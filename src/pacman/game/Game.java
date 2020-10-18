package pacman.game;

import pacman.game.Constants.*;
import pacman.game.internal.*;
import pacman.game.util.GameInfo;

import java.util.BitSet;
import java.util.EnumMap;
import java.util.Map.Entry;
import java.util.Random;

import static pacman.game.Constants.*;

/**
 * The implementation of Ms Pac-Man. This class contains the game engine and all methods required to
 * query the state of the game. First, the mazes are loaded once only as they are immutable. The game
 * then proceeds to initialise all variables using default values. The game class also provides numerous
 * methods to extract the game state as a string (used for replays and for communication via pipes during
 * the competition) and to create copies. Care has been taken to implement the game efficiently to ensure
 * that copies can be created quickly.
 * 
 * The game has a central update method called advanceGame which takes a move for Ms Pac-Man and up to
 * 4 moves for the ghosts. It then updates the positions of all characters, check whether pills or power
 * pills have been eaten and updates the game state accordingly.
 * 
 * All other methods are to access the gamestate and to compute numerous aspects such as directions to taken
 * given a target or a shortest path from a to b. All shortest path distances from any node to any other node
 * are pre-computed and loaded from file. This makes these methods more efficient. Note about the ghosts: ghosts
 * are not allowed to reverse. Hence it is not possible to simply look up the shortest path distance. Instead,
 * one can approximate this greedily or use A* to compute it properly. The former is somewhat quicker and has
 * a low error rate. The latter takes a bit longer but is absolutely accurate. We use the pre-computed shortest
 * path distances as admissable heuristic so it is very efficient.
 */
public final class Game
{
	//pills stored as bitsets for efficient copying
	private BitSet pills, powerPills;
	//all the game's variables
	private int mazeIndex, levelCount, currentLevelTime, totalTime, score, ghostEatMultiplier, timeOfLastGlobalReversal;	
	private boolean gameOver, pacmanWasEaten, pillWasEaten, powerPillWasEaten;
	private EnumMap<GHOST,Boolean> ghostsEaten;
	//the data relating to pacman and the ghosts are stored in respective data structures for clarity
	private PacMan pacman;
	private EnumMap<GHOST, Ghost> ghosts;
	private GameInfo gi;

	//Game information methods
	public GameInfo getGi() {
		return gi;
	}

	public void setGi(GameInfo gi) {
		this.gi = gi;
	}

	//mazes are only loaded once since they don't change over time
	private static Maze[] mazes=new Maze[NUM_MAZES];;
	
	private Maze currentMaze;
	
	static 
	{
		for(int i=0;i<mazes.length;i++)
			mazes[i]=new Maze(i);
	}
	
	public static PathsCache[] caches=new PathsCache[NUM_MAZES];
	
	static 
	{
		for(int i=0;i<mazes.length;i++)
		{
			caches[i]=new PathsCache(i);
		}
	}
	
	private Random rnd;
	private long seed;	
	
	/////////////////////////////////////////////////////////////////////////////
	///////////////////  Constructors and initialisers  /////////////////////////
	/////////////////////////////////////////////////////////////////////////////
	
	/**
	 * Instantiates a new game. The seed is used to initialise the pseudo-random
	 * number generator. This way, a game may be replicated exactly by using identical
	 * seeds. Note: in the competition, the games received from the game server are
	 * using different seeds. Otherwise global reversal events would be predictable.
	 *
	 * @param seed The seed for the pseudo-random number generator
	 */
	public Game(long seed)
	{		
		this.gi = new GameInfo(); //Manual inicialization for non evolutionary executions (test games..)
		this.seed=seed;
		rnd=new Random(seed);
		
		_init(0);
	}
	
	public void initGi(){
		this.gi = new GameInfo();
	}
	
	/**
	 * Initiates a new game specifying the maze to start with.
	 * 
	 * @param seed Seed used for the pseudo-random numbers
	 * @param initialMaze The maze to start the game with
	 */
	public Game(long seed,int initialMaze)
	{						
		this.seed=seed;
		rnd=new Random(seed);
		
		_init(initialMaze);		
	}
	
	/**
	 * Empty constructor used by the copy method.
	 */
	private Game(){}
	
	/**
	 * _init.
	 *
	 * @param initialMaze the initial maze
	 */
	private void _init(int initialMaze)
	{
		mazeIndex=initialMaze;
		score=currentLevelTime=levelCount=totalTime=0;
		ghostEatMultiplier=1;
		gameOver=false;
		timeOfLastGlobalReversal=-1;		
		pacmanWasEaten=false;
		pillWasEaten=false;
		powerPillWasEaten=false;
		
		ghostsEaten=new EnumMap<GHOST,Boolean>(GHOST.class);
		
		for(GHOST ghost : GHOST.values())
			ghostsEaten.put(ghost,false);
		
		_setPills(currentMaze=mazes[mazeIndex]);
		_initGhosts();
		
		pacman=new PacMan(currentMaze.initialPacManNodeIndex, MOVE.LEFT,NUM_LIVES,false);
	}
	
	/**
	 * _new level reset.
	 */
	private void _newLevelReset()
	{
		mazeIndex=++mazeIndex%NUM_MAZES;
		levelCount++;
		currentMaze=mazes[mazeIndex];
		
		currentLevelTime=0;
		ghostEatMultiplier=1;
			
		_setPills(currentMaze);
		_levelReset();
	}
	
	public int getLevelCount() {
		return levelCount;
	}

	/**
	 * _level reset.
	 */
	private void _levelReset()
	{
		ghostEatMultiplier=1;
		
		_initGhosts();
		
		pacman.currentNodeIndex=currentMaze.initialPacManNodeIndex;
		pacman.lastMoveMade= MOVE.LEFT;
	}
	
	/**
	 * _set pills.
	 *
	 * @param maze the maze
	 */
	private void _setPills(Maze maze)
	{
		pills=new BitSet(currentMaze.pillIndices.length);
		pills.set(0,currentMaze.pillIndices.length);
		powerPills=new BitSet(currentMaze.powerPillIndices.length);
		powerPills.set(0,currentMaze.powerPillIndices.length);
	}
	
	/**
	 * _init ghosts.
	 */
	private void _initGhosts()
	{
		ghosts=new EnumMap<GHOST, Ghost>(GHOST.class);
					
		for(GHOST ghostType : GHOST.values())
			ghosts.put(ghostType,new Ghost(ghostType,currentMaze.lairNodeIndex,0,
					(int)(ghostType.initialLairTime*(Math.pow(LAIR_REDUCTION,levelCount%LEVEL_RESET_REDUCTION))), MOVE.NEUTRAL));
	}

	/**
	 * Gets the game state as a string: all variables are written to a string in a pre-determined
	 * order. The string may later be used to recreate a game state using the setGameState() method.
	 *
	 * Variables not included: enableGlobalReversals
	 *
	 * @return The game state as a string
	 */
	public String getGameState()
	{
		StringBuilder sb=new StringBuilder();

		sb.append(mazeIndex+","+totalTime+","+score+","+currentLevelTime+","+levelCount+","
				+pacman.currentNodeIndex+","+pacman.lastMoveMade+","+pacman.numberOfLivesRemaining+","+pacman.hasReceivedExtraLife+",");

		for(Ghost ghost : ghosts.values())
			sb.append(ghost.currentNodeIndex+","+ghost.edibleTime+","+ghost.lairTime+","+ghost.lastMoveMade+",");

		for(int i=0;i<currentMaze.pillIndices.length;i++)
			if(pills.get(i))
				sb.append("1");
			else
				sb.append("0");

		sb.append(",");
		
		for(int i=0;i<currentMaze.powerPillIndices.length;i++)
			if(powerPills.get(i))
				sb.append("1");
			else
				sb.append("0");
		
		sb.append(",");		
		sb.append(timeOfLastGlobalReversal);
		sb.append(",");		
		sb.append(pacmanWasEaten);
		sb.append(",");	
		
		for(GHOST ghost : GHOST.values())
		{
			sb.append(ghostsEaten.get(ghost));
			sb.append(",");
		}
		
		sb.append(pillWasEaten);
		sb.append(",");
		sb.append(powerPillWasEaten);		
		
		return sb.toString();
	}
	
	/**
	 * Sets the game state from a string: the inverse of getGameState(). It reconstructs
	 * all the game's variables from the string.
	 *
	 * @param gameState The game state represented as a string
	 */
	public void setGameState(String gameState)
	{	
		String[] values=gameState.split(",");
		
		int index=0;
		
		mazeIndex=Integer.parseInt(values[index++]);
		totalTime=Integer.parseInt(values[index++]);
		score=Integer.parseInt(values[index++]);
		currentLevelTime=Integer.parseInt(values[index++]);
		levelCount=Integer.parseInt(values[index++]);
		
		pacman=new PacMan(Integer.parseInt(values[index++]), MOVE.valueOf(values[index++]),
				Integer.parseInt(values[index++]),Boolean.parseBoolean(values[index++]));
		
		ghosts=new EnumMap<GHOST, Ghost>(GHOST.class);
		
		for(GHOST ghostType : GHOST.values())
			ghosts.put(ghostType,new Ghost(ghostType,Integer.parseInt(values[index++]),Integer.parseInt(values[index++]),
					Integer.parseInt(values[index++]), MOVE.valueOf(values[index++])));
			
		_setPills(currentMaze=mazes[mazeIndex]);
		
		for(int i=0;i<values[index].length();i++)
			if(values[index].charAt(i)=='1')
				pills.set(i);
			else
				pills.clear(i);
		
		index++;
		
		for(int i=0;i<values[index].length();i++)
			if(values[index].charAt(i)=='1')
				powerPills.set(i);		
			else
				powerPills.clear(i);
		
		timeOfLastGlobalReversal=Integer.parseInt(values[++index]);			
		pacmanWasEaten=Boolean.parseBoolean(values[++index]);
		
		ghostsEaten=new EnumMap<GHOST,Boolean>(GHOST.class);
		
		for(GHOST ghost : GHOST.values())
			ghostsEaten.put(ghost,Boolean.parseBoolean(values[++index]));
		
		pillWasEaten=Boolean.parseBoolean(values[++index]);
		powerPillWasEaten=Boolean.parseBoolean(values[++index]);
	}
	
	/**
	 * Returns an exact copy of the game. This may be used for forward searches
	 * such as minimax. The copying is relatively efficient.
	 *
	 * @return the game
	 */
	public Game copy()
	{
		Game copy=new Game();
		
		copy.initGi();
		copy.seed=seed;
		copy.rnd=new Random(seed);
		copy.currentMaze=currentMaze;		
		copy.pills=(BitSet)pills.clone();
		copy.powerPills=(BitSet)powerPills.clone();		
		copy.mazeIndex=mazeIndex;
		copy.levelCount=levelCount;
		copy.currentLevelTime=currentLevelTime;		
		copy.totalTime=totalTime;
		copy.score=score;
		copy.ghostEatMultiplier=ghostEatMultiplier;
		copy.gameOver=gameOver;
		copy.timeOfLastGlobalReversal=timeOfLastGlobalReversal;		
		copy.pacmanWasEaten=pacmanWasEaten;
		copy.pillWasEaten=pillWasEaten;
		copy.powerPillWasEaten=powerPillWasEaten;		
		copy.pacman=pacman.copy();
		
		copy.ghostsEaten=new EnumMap<GHOST,Boolean>(GHOST.class);
		copy.ghosts=new EnumMap<GHOST, Ghost>(GHOST.class);
		
		for(GHOST ghostType : GHOST.values())
		{
			copy.ghosts.put(ghostType,ghosts.get(ghostType).copy());
			copy.ghostsEaten.put(ghostType,ghostsEaten.get(ghostType));
		}
			
		return copy;	
	}

	/////////////////////////////////////////////////////////////////////////////
	///////////////////////////  Game-engine   //////////////////////////////////
	/////////////////////////////////////////////////////////////////////////////

	/**
	 * Central method that advances the game state using the moves supplied by
	 * the controllers. It first updates Ms Pac-Man, then the ghosts and then
	 * the general game logic.
	 *
	 * @param pacManMove The move supplied by the Ms Pac-Man controller
	 * @param ghostMoves The moves supplied by the ghosts controller
	 */	
	public void advanceGame(MOVE pacManMove, EnumMap<GHOST, MOVE> ghostMoves)
	{		
		updatePacMan(pacManMove);
		updateGhosts(ghostMoves);	
		updateGame();
		
		//Game info updates
		if (powerPillWasEaten)
			gi.increasePowerPillsEaten();
		
		if(pillWasEaten)
			gi.increasePillsEaten();
	}
	
	public void advanceGameWithoutReverse(MOVE pacManMove, EnumMap<GHOST, MOVE> ghostMoves)
	{		
		updatePacMan(pacManMove);
		updateGhostsWithoutReverse(ghostMoves);
		updateGame();
	}
	
	public void advanceGameWithForcedReverse(MOVE pacManMove, EnumMap<GHOST, MOVE> ghostMoves)
	{		
		updatePacMan(pacManMove);
		updateGhostsWithForcedReverse(ghostMoves);
		updateGame();
	}
	
	public void advanceGameWithPowerPillReverseOnly(MOVE pacManMove, EnumMap<GHOST, MOVE> ghostMoves)
	{		
		updatePacMan(pacManMove);
		
		if(powerPillWasEaten)
			updateGhostsWithForcedReverse(ghostMoves);
		else
			updateGhostsWithoutReverse(ghostMoves);
		
		updateGame();
	}
		
	/**
	 * Updates the state of Ms Pac-Man given the move returned by the controller.
	 *
	 * @param pacManMove The move supplied by the Ms Pac-Man controller
	 */
	public void updatePacMan(MOVE pacManMove)
	{
		_updatePacMan(pacManMove);					//move pac-man		
		_eatPill();									//eat a pill
		_eatPowerPill();							//eat a power pill
	}
	
	/**
	 * Updates the states of the ghosts given the moves returned by the controller.
	 *
	 * @param ghostMoves The moves supplied by the ghosts controller
	 */
	public void updateGhosts(EnumMap<GHOST, MOVE> ghostMoves)
	{
		ghostMoves=_completeGhostMoves(ghostMoves);
		
		if(!_reverseGhosts(ghostMoves,false))
			_updateGhosts(ghostMoves);
	}
	
	public void updateGhostsWithoutReverse(EnumMap<GHOST, MOVE> ghostMoves)
	{
		ghostMoves=_completeGhostMoves(ghostMoves);
		_updateGhosts(ghostMoves);
	}
	
	public void updateGhostsWithForcedReverse(EnumMap<GHOST, MOVE> ghostMoves)
	{
		ghostMoves=_completeGhostMoves(ghostMoves);
		_reverseGhosts(ghostMoves,true);
	}
	
	/**
	 * Updates the game once the individual characters have been updated: check if anyone
	 * can eat anyone else. Then update the lair times and check if Ms Pac-Man should be
	 * awarded the extra live. Then update the time and see if the level or game is over.
	 */
	public void updateGame()
	{
		_feast();									//ghosts eat pac-man or vice versa		
		_updateLairTimes();
		_updatePacManExtraLife();

		totalTime++;
		currentLevelTime++;
		
		_checkLevelState();							//check if level/game is over
	}
	
	/**
	 * This method is for specific purposes such as searching a tree in a specific manner. It has to be used cautiously as it might
	 * create an unstable game state and may cause the game to crash.
	 * 
	 * @param feast Whether or not to enable feasting
	 * @param updateLairTimes Whether or not to update the lair times
	 * @param updateExtraLife Whether or not to update the extra life
	 * @param updateTotalTime Whether or not to update the total time
	 * @param updateLevelTime Whether or not to update the level time
	 */
	public void updateGame(boolean feast,boolean updateLairTimes,boolean updateExtraLife,boolean updateTotalTime,boolean updateLevelTime)
	{						
		if(feast) 			_feast();				//ghosts eat pac-man or vice versa		
		if(updateLairTimes) _updateLairTimes();
		if(updateExtraLife) _updatePacManExtraLife();

		if(updateTotalTime) totalTime++;
		if(updateLevelTime) currentLevelTime++;
		
		_checkLevelState();							//check if level/game is over
	}
	
	/**
	 * _update lair times.
	 */
	private void _updateLairTimes()
	{
		for(Ghost ghost : ghosts.values())
			if(ghost.lairTime>0)
				if(--ghost.lairTime==0)
					ghost.currentNodeIndex=currentMaze.initialGhostNodeIndex;
	}
	
	/**
	 * _update pac man extra life.
	 */
	private void _updatePacManExtraLife()
	{
		if(!pacman.hasReceivedExtraLife && score>=EXTRA_LIFE_SCORE)	//award 1 extra life at 10000 points
		{
			pacman.hasReceivedExtraLife=true;
			pacman.numberOfLivesRemaining++;
		}
	}
	
	/**
	 * _update pac man.
	 *
	 * @param move the move
	 */
	private void _updatePacMan(MOVE move)
	{
		pacman.lastMoveMade=_correctPacManDir(move);		
		pacman.currentNodeIndex=pacman.lastMoveMade == MOVE.NEUTRAL ? pacman.currentNodeIndex :
			currentMaze.graph[pacman.currentNodeIndex].neighbourhood.get(pacman.lastMoveMade);
	}

	/**
	 * _correct pac man dir.
	 *
	 * @param direction the direction
	 * @return the mOVE
	 */
	private MOVE _correctPacManDir(MOVE direction)
	{
		Node node=currentMaze.graph[pacman.currentNodeIndex];
		
		//direction is correct, return it
		if(node.neighbourhood.containsKey(direction))
			return direction;
		else
		{
			//try to use previous direction (i.e., continue in the same direction)
			if(node.neighbourhood.containsKey(pacman.lastMoveMade))
				return pacman.lastMoveMade;
			//else stay put
			else
				return MOVE.NEUTRAL;
		}
	}

	/**
	 * _update ghosts.
	 *
	 * @param moves the moves
	 */
	private void _updateGhosts(EnumMap<GHOST, MOVE> moves)
	{
		for(Entry<GHOST, MOVE> entry : moves.entrySet())
		{
			Ghost ghost=ghosts.get(entry.getKey());

			if(ghost.lairTime==0)
			{
				if(ghost.edibleTime==0 || ghost.edibleTime%GHOST_SPEED_REDUCTION!=0)
				{
					ghost.lastMoveMade=_checkGhostDir(ghost,entry.getValue());					
					moves.put(entry.getKey(), ghost.lastMoveMade);					
					ghost.currentNodeIndex=currentMaze.graph[ghost.currentNodeIndex].neighbourhood.get(ghost.lastMoveMade);
				}
			}
		}
	}
	
	private EnumMap<GHOST, MOVE> _completeGhostMoves(EnumMap<GHOST, MOVE> moves)
	{
		if(moves==null)
		{
			moves=new EnumMap<GHOST, MOVE>(GHOST.class);
			
			for(GHOST ghostType : GHOST.values())
				moves.put(ghostType,ghosts.get(ghostType).lastMoveMade);
		}
		
		if(moves.size()<NUM_GHOSTS)
			for(GHOST ghostType : GHOST.values())
				if(!moves.containsKey(ghostType))
					moves.put(ghostType, MOVE.NEUTRAL);
		
		return moves;
	}
	
	/**
	 * _check ghost dir.
	 *
	 * @param ghost the ghost
	 * @param direction the direction
	 * @return the mOVE
	 */
	private MOVE _checkGhostDir(Ghost ghost, MOVE direction)
	{
		//Gets the neighbours of the node with the node that would correspond to reverse removed
		Node node=currentMaze.graph[ghost.currentNodeIndex];
		
		//The direction is possible and not opposite to the previous direction of that ghost
		if(node.neighbourhood.containsKey(direction) && direction!=ghost.lastMoveMade.opposite())
			return direction;
		else
		{
			if(node.neighbourhood.containsKey(ghost.lastMoveMade))
				return ghost.lastMoveMade;
			else
			{
				MOVE[] moves=node.allPossibleMoves.get(ghost.lastMoveMade);
				return moves[rnd.nextInt(moves.length)];				
			}
		}
	}
			
	/**
	 * _eat pill.
	 */
	private void _eatPill()
	{
		pillWasEaten=false;
		
		int pillIndex=currentMaze.graph[pacman.currentNodeIndex].pillIndex;

		if(pillIndex>=0 && pills.get(pillIndex))
		{
			score+=PILL;
			pills.clear(pillIndex);
			pillWasEaten=true;
		}
	}
	
	/**
	 * _eat power pill.
	 */
	private void _eatPowerPill()
	{
		powerPillWasEaten=false;	
		
		int powerPillIndex=currentMaze.graph[pacman.currentNodeIndex].powerPillIndex;
		
		if(powerPillIndex>=0 && powerPills.get(powerPillIndex))
		{
			score+=POWER_PILL;
			ghostEatMultiplier=1;
			powerPills.clear(powerPillIndex);
			
			int newEdibleTime=(int)(EDIBLE_TIME*(Math.pow(EDIBLE_TIME_REDUCTION,levelCount%LEVEL_RESET_REDUCTION)));
			
			for(Ghost ghost : ghosts.values())
				if(ghost.lairTime==0)
					ghost.edibleTime=newEdibleTime;
				else
					ghost.edibleTime=0;
			
			powerPillWasEaten=true;
		}
	}
	
	private boolean _reverseGhosts(EnumMap<GHOST, MOVE> moves, boolean force)
	{
		boolean reversed=false;		
		boolean globalReverse=false;
			
		if(Math.random()<GHOST_REVERSAL)
			globalReverse=true;
		
		for(Entry<GHOST, MOVE> entry : moves.entrySet())
		{
			Ghost ghost=ghosts.get(entry.getKey());
		
			if(currentLevelTime>1 && ghost.lairTime==0 && ghost.lastMoveMade!= MOVE.NEUTRAL)
			{
				if(force || (powerPillWasEaten || globalReverse))
				{
					ghost.lastMoveMade=ghost.lastMoveMade.opposite();
					ghost.currentNodeIndex=currentMaze.graph[ghost.currentNodeIndex].neighbourhood.get(ghost.lastMoveMade);
					reversed=true;
					timeOfLastGlobalReversal = totalTime;
				}
			}
		}
		
		return reversed;
	}

	/**
	 * _feast.
	 */
	private void _feast()
	{		
		pacmanWasEaten=false;
		
		for(GHOST ghost : GHOST.values())
			ghostsEaten.put(ghost,false);
		
		for(Ghost ghost : ghosts.values())
		{
			int distance=getShortestPathDistance(pacman.currentNodeIndex, ghost.currentNodeIndex);
			
			if(distance<=EAT_DISTANCE && distance!=-1)
			{
				if(ghost.edibleTime>0)									//pac-man eats ghost
				{
					//game info update
					gi.increaseGhostsEaten();
					
					score+=GHOST_EAT_SCORE*ghostEatMultiplier;
					ghostEatMultiplier*=2;
					ghost.edibleTime=0;					
					ghost.lairTime=(int)(COMMON_LAIR_TIME*(Math.pow(LAIR_REDUCTION,levelCount%LEVEL_RESET_REDUCTION)));					
					ghost.currentNodeIndex=currentMaze.lairNodeIndex;
					ghost.lastMoveMade= MOVE.NEUTRAL;
					
					ghostsEaten.put(ghost.type, true);
				}
				else													//ghost eats pac-man
				{
					pacman.numberOfLivesRemaining--;
					pacmanWasEaten=true;
					
					if(pacman.numberOfLivesRemaining<=0)
						gameOver=true;
					else
						_levelReset();
					
					return;
				}
			}
		}
		
		for(Ghost ghost : ghosts.values())
			if(ghost.edibleTime>0)
				ghost.edibleTime--;
	}
	
	/**
	 * _check level state.
	 */
	private void _checkLevelState()
	{
		//put a cap on the total time a game can be played for
		if(totalTime+1>MAX_TIME)
		{
			gameOver=true;
			score+=pacman.numberOfLivesRemaining*AWARD_LIFE_LEFT;
		}
		//if all pills have been eaten or the time is up...
		else if((pills.isEmpty() && powerPills.isEmpty()) || currentLevelTime>=LEVEL_LIMIT)
			_newLevelReset();
	}

	/////////////////////////////////////////////////////////////////////////////
	///////////////////  Query Methods (return only)  ///////////////////////////
	/////////////////////////////////////////////////////////////////////////////

	/**
	 * Returns whether pacman was eaten in the last time step
	 * 
	 * @return whether Ms Pac-Man was eaten.
	 */
	public boolean wasPacManEaten()
	{
		return pacmanWasEaten;
	}
	
	/**
	 * Returns whether a ghost was eaten in the last time step
	 * 
	 * @return whether a ghost was eaten.
	 */
	public boolean wasGhostEaten(GHOST ghost)
	{
		return ghostsEaten.get(ghost);
	}
	
	public int getNumGhostsEaten()
	{
		int count=0;
		
		for(GHOST ghost : GHOST.values())
			if(ghostsEaten.get(ghost))
				count++;
		
		return count;
	}
	
	/**
	 * Returns whether a pill was eaten in the last time step
	 * 
	 * @return whether a pill was eaten.
	 */
	public boolean wasPillEaten()
	{
		return pillWasEaten;
	}
	
	/**
	 * Returns whether a power pill was eaten in the last time step
	 * 
	 * @return whether a power pill was eaten.
	 */
	public boolean wasPowerPillEaten()
	{
		return powerPillWasEaten;
	}
		
	/**
	 * Returns the time when the last global reversal event took place.
	 * 
	 * @return time the last global reversal event took place (not including power pill reversals)
	 */
	public int getTimeOfLastGlobalReversal()
	{
		return timeOfLastGlobalReversal;
	}
	
	/**
	 * Checks whether the game is over or not: all lives are lost or 16 levels have been 
	 * played. The variable is set by the methods _feast() and _checkLevelState().
	 *
	 * @return true, if successful
	 */
	public boolean gameOver()
	{
		return gameOver;
	}
	
	/**
	 * Returns the current maze of the game.
	 * 
	 * @return The current maze.
	 */
	public Maze getCurrentMaze()
	{
		return currentMaze;
	}
	
	/**
	 * Returns the x coordinate of the specified node.
	 *
	 * @param nodeIndex the node index
	 * @return the node x cood
	 */
	public int getNodeXCood(int nodeIndex)
	{
		return currentMaze.graph[nodeIndex].x;
	}
	
	/**
	 * Returns the y coordinate of the specified node.
	 *
	 * @param nodeIndex The node index
	 * @return The node's y coordinate
	 */
	public int getNodeYCood(int nodeIndex)
	{
		return currentMaze.graph[nodeIndex].y;
	}
	
	/**
	 * Gets the index of the current maze.
	 *
	 * @return The maze index
	 */
	public int getMazeIndex()
	{
		return mazeIndex;
	}
	
	/**
	 * Returns the current level.
	 *
	 * @return The current level
	 */
	public int getCurrentLevel()
	{
		return levelCount;
	}
	
	/**
	 * Returns the number of nodes in the current maze.
	 * 
	 * @return number of nodes in the current maze.
	 */
	public int getNumberOfNodes()
	{
		return currentMaze.graph.length;
	}
	 
	/**
	 * Returns the current value awarded for eating a ghost.
	 * 
	 * @return the current value awarded for eating a ghost.
	 */
	public int getGhostCurrentEdibleScore()
	{
		return GHOST_EAT_SCORE*ghostEatMultiplier;
	}
	 
	/**
	 * Returns the node index where ghosts start in the maze once leaving
	 * the lair.
	 * 
	 * @return the node index where ghosts start after leaving the lair.
	 */
	public int getGhostInitialNodeIndex()
	{
		return currentMaze.initialGhostNodeIndex;
	}
	
	/**
	 * Whether the pill specified is still there or has been eaten.
	 *
	 * @param pillIndex The pill index
	 * @return true, if is pill still available
	 */
	public boolean isPillStillAvailable(int pillIndex)
	{
		return pills.get(pillIndex);
	}
	
	/**
	 * Whether the power pill specified is still there or has been eaten.
	 *
	 * @param powerPillIndex The power pill index
	 * @return true, if is power pill still available
	 */
	public boolean isPowerPillStillAvailable(int powerPillIndex)
	{
		return powerPills.get(powerPillIndex);
	}
	
	/**
	 * Returns the pill index of the node specified. This can be -1 if there
	 * is no pill at the specified node.
	 * 
	 * @param nodeIndex The Index of the node.
	 * @return a number corresponding to the pill index (or -1 if node has no pill)
	 */
	public int getPillIndex(int nodeIndex)
	{
		return currentMaze.graph[nodeIndex].pillIndex;
	}
	
	/**
	 * Returns the power pill index of the node specified. This can be -1 if there
	 * is no power pill at the specified node.
	 * 
	 * @param nodeIndex The Index of the node.
	 * @return a number corresponding to the power pill index (or -1 if node has no pill)
	 */
	public int getPowerPillIndex(int nodeIndex)
	{
		return currentMaze.graph[nodeIndex].powerPillIndex;		
	}
	
	/**
	 * Returns the array of node indices that are junctions (3 or more neighbours).
	 * 
	 * @return the junction indices
	 */
	public int[] getJunctionIndices()
	{
		return currentMaze.junctionIndices;		
	}
	
	/**
	 * Returns the indices to all the nodes that have pills.
	 *
	 * @return the pill indices
	 */
	public int[] getPillIndices()
	{
		return currentMaze.pillIndices;
	}
	
	/**
	 * Returns the indices to all the nodes that have power pills.
	 *
	 * @return the power pill indices
	 */
	public int[] getPowerPillIndices()
	{
		return currentMaze.powerPillIndices;
	}
	
	/**
	 * Current node index of Ms Pac-Man.
	 *
	 * @return the pacman current node index
	 */
	public int getPacmanCurrentNodeIndex()
	{
		return pacman.currentNodeIndex;
	}
	
	/**
	 * Current node index of Ms Pac-Man.
	 *
	 * @return the pacman last move made
	 */
	public MOVE getPacmanLastMoveMade()
	{
		return pacman.lastMoveMade;
	}
	
	/**
	 * Lives that remain for Ms Pac-Man.
	 *
	 * @return the number of lives remaining
	 */
	public int getPacmanNumberOfLivesRemaining()
	{
		return pacman.numberOfLivesRemaining;
	}
	
	/**
	 * Current node at which the specified ghost resides.
	 *
	 * @param ghostType the ghost type
	 * @return the ghost current node index
	 */
	public int getGhostCurrentNodeIndex(GHOST ghostType)
	{
		return ghosts.get(ghostType).currentNodeIndex;
	}

	/**
	 * Current direction of the specified ghost.
	 *
	 * @param ghostType the ghost type
	 * @return the ghost last move made
	 */
	public MOVE getGhostLastMoveMade(GHOST ghostType)
	{
		return ghosts.get(ghostType).lastMoveMade;
	}
	
	/**
	 * Returns the edible time for the specified ghost.
	 *
	 * @param ghostType the ghost type
	 * @return the ghost edible time
	 */
	public int getGhostEdibleTime(GHOST ghostType)
	{
		return ghosts.get(ghostType).edibleTime;
	}
	
	/**
	 * Find the nearest edible ghost.
	 * 
	 * @param fromIndex current pacman location
	 * @return Ghost or null if there is none edible
	 */
	public Ghost getClosestEdibleGhost(int fromIndex) {
		int minDistance = Integer.MAX_VALUE;
		Ghost minGhost = null;

		for (Ghost ghost : this.ghosts.values()) {
			if (ghost.isEdible()) {
				int distance = this.getShortestPathDistance(fromIndex, ghost.currentNodeIndex);

				if (distance < minDistance) {
					minDistance = distance;
					minGhost = ghost;
				}
			}
		}

		return minGhost;
	}
	
	/**
	 * Find the nearest edible in time ghost.
	 * 
	 * @param fromIndex current pacman location
	 * @return Ghost or null if there is none edible
	 */
	public Ghost getClosestReachableEdibleGhost(int fromIndex) {
		int minDistance = Integer.MAX_VALUE;
		Ghost minGhost = null;

		for (Ghost ghost : this.ghosts.values()) {
			if (ghost.isEdible()) {
				int distance = this.getShortestPathDistance(fromIndex, ghost.currentNodeIndex);
				int movesToReach = distance*(1+((Constants.GHOST_SPEED_REDUCTION-1)/ Constants.GHOST_SPEED_REDUCTION));
				
				if (distance < minDistance && movesToReach <= ghost.edibleTime) {
					minDistance = distance;
					minGhost = ghost;
				}
			}
		}

		return minGhost;
	}
	
	/**
	 * Find the nearest non-edible ghost.
	 * 
	 * @param fromIndex current pacman location
	 * @return Ghost or null if the ghosts are still in the lair
	 */
	public Ghost getClosestNonEdibleGhost(int fromIndex) {
		int minDistance = Integer.MAX_VALUE;
		Ghost minGhost = null;

		for (Ghost ghost : this.ghosts.values()) {
			if (!ghost.isEdible() && !ghost.isInLair()) {
				int distance = this.getShortestPathDistance(fromIndex, ghost.currentNodeIndex);

				if (distance < minDistance) {
					minDistance = distance;
					minGhost = ghost;
				}
			}
		}

		return minGhost;
	}
	
	/**
	 * Distance to the nearest non-edible ghost.
	 * 
	 * @param fromIndex current pacman location
	 * @return The distance from the closest non edible ghost to pacman, having in mind the ghosts can't reverse
	 */
	public int getDistanceToClosestNonEdibleGhost(int fromIndex) {
		int minDistance = Integer.MAX_VALUE;

		for (Ghost ghost : this.ghosts.values()) {
			if (!ghost.isEdible() && !ghost.isInLair()) {
				int distance = this.getShortestPathDistance(ghost.currentNodeIndex, fromIndex);

				if (distance < minDistance) {
					minDistance = distance;
				}
			}
		}

		return minDistance;
	}
	
	/**
	 * Returns the distance to the nearest non-edible ghost not considering distances from directions opposing the last move made.
	 * 
	 * @param fromIndex current pacman location
	 * @return Ghost or null if the ghosts are still in the lair
	 */
	public int getDistanceToClosestNonEdibleGhost(int fromIndex, MOVE lastMoveMade) {
		int minDistance = Integer.MAX_VALUE;

		for (Ghost ghost : this.ghosts.values()) {
			if (!ghost.isEdible() && !ghost.isInLair()) {
				int distance = this.getShortestPathDistance(fromIndex, ghost.currentNodeIndex, lastMoveMade);

				if (distance < minDistance) {
					minDistance = distance;
				}
			}
		}

		return minDistance;
	}
	
	/**
	 * Distance to the nearest non-edible ghost.
	 * 
	 * @param fromIndex current pacman location
	 * @return Ghost or null if the ghosts are still in the lair
	 */
	public int getDistanceToClosestEdibleGhost(int fromIndex) {
		int minDistance = Integer.MAX_VALUE;

		for (Ghost ghost : this.ghosts.values()) {
			if (ghost.isEdible() && !ghost.isInLair()) {
				int distance = this.getShortestPathDistance(fromIndex, ghost.currentNodeIndex);

				if (distance < minDistance) {
					minDistance = distance;
				}
			}
		}

		return minDistance;
	}
	
	/**
	 * Returns the distance to the nearest non-edible ghost not considering distances from directions opposing the last move made.
	 * 
	 * @param fromIndex current pacman location
	 * @return Ghost or null if the ghosts are still in the lair
	 */
	public int getDistanceToClosestEdibleGhost(int fromIndex, MOVE lastMoveMade) {
		int minDistance = Integer.MAX_VALUE;

		for (Ghost ghost : this.ghosts.values()) {
			if (ghost.isEdible() && !ghost.isInLair()) {
				int distance = this.getShortestPathDistance(fromIndex, ghost.currentNodeIndex, lastMoveMade);

				if (distance < minDistance) {
					minDistance = distance;
				}
			}
		}

		return minDistance;
	}
	
	/**
	 * Find the nearest ghost, edible or not.
	 * 
	 * @param fromIndex current pacman location
	 * @return Ghost or null if the ghosts are still in the lair
	 */
	public Ghost getClosestGhost(int fromIndex) {
		int minDistance = Integer.MAX_VALUE;
		Ghost minGhost = null;

		for (Ghost ghost : this.ghosts.values()) {
			int distance = this.getShortestPathDistance(fromIndex, ghost.currentNodeIndex);

			if (distance < minDistance) {
				minDistance = distance;
				minGhost = ghost;
			}
		}

		return minGhost;
	}
	
	/**
	 * Handy method which makes use of getShortestPathDistance to check
	 * if a ghost is closer than a certain distance to pacman.
	 * 
	 * @param fromIndex pacman
	 * @param toIndex ghost
	 * @param distance distance to check
	 * @return
	 */
	public boolean closerThan(int fromIndex, int toIndex, int distance) {
		return this.getShortestPathDistance(fromIndex, toIndex) < distance;
	}
	
	/**
	 * Simpler check to see if a ghost is edible.
	 *
	 * @param ghostType the ghost type
	 * @return true, if is ghost edible
	 */
	public boolean isGhostEdible(GHOST ghostType)
	{
		return ghosts.get(ghostType).edibleTime>0;
	}

	/**
	 * Returns the score of the game.
	 *
	 * @return the score
	 */
	public int getScore()
	{
		return score;
	}
	
	/**
	 * Returns the time of the current level (important with respect to LEVEL_LIMIT).
	 *
	 * @return the current level time
	 */
	public int getCurrentLevelTime()
	{
		return currentLevelTime;
	}
	
	/**
	 * Total time the game has been played for (at most LEVEL_LIMIT*MAX_LEVELS).
	 *
	 * @return the total time
	 */
	public int getTotalTime()
	{
		return totalTime;
	}
	
	/**
	 * Total number of pills in the mazes[gs.curMaze]
	 *
	 * @return the number of pills
	 */
	public int getNumberOfPills()
	{
		return currentMaze.pillIndices.length;
	}
	
	/**
	 * Total number of power pills in the mazes[gs.curMaze]
	 *
	 * @return the number of power pills
	 */
	public int getNumberOfPowerPills()
	{
		return currentMaze.powerPillIndices.length;
	}
	
	/**
	 * Total number of pills in the mazes[gs.curMaze]
	 *
	 * @return the number of active pills
	 */
	public int getNumberOfActivePills()
	{
		return pills.cardinality();
	}
	
	/**
	 * Total number of power pills in the mazes[gs.curMaze]
	 *
	 * @return the number of active power pills
	 */
	public int getNumberOfActivePowerPills()
	{
		return powerPills.cardinality();
	}
	
	public int getClosestPill(int currentPos) {
		return this.getClosestNodeIndexFromNodeIndex(currentPos, this.getActivePillsIndices(), DM.PATH);
	}
	
	public int getClosestPowerPill(int currentPos) {
		return this.getClosestNodeIndexFromNodeIndex(currentPos, this.getActivePowerPillsIndices(), DM.PATH);
	}
	
	public int getClosestPillOrPowerPill(int currentPos) {
		int[] activePills=this.getActivePillsIndices();
		int[] activePowerPills=this.getActivePowerPillsIndices();
		int[] targetNodeIndices = new int[activePills.length + activePowerPills.length];
		
		System.arraycopy(activePills, 0, targetNodeIndices, 0, activePills.length);
		System.arraycopy(activePowerPills, 0, targetNodeIndices, activePills.length, activePowerPills.length);
		
		return this.getClosestNodeIndexFromNodeIndex(currentPos, targetNodeIndices, DM.PATH);
	}
	
	/**
	 * Time left that the specified ghost will spend in the lair.
	 *
	 * @param ghostType the ghost type
	 * @return the ghost lair time
	 */
	public int getGhostLairTime(GHOST ghostType)
	{
		return ghosts.get(ghostType).lairTime;
	}
	
	/**
	 * returns the indices of all active pills in the mazes[gs.curMaze]
	 *
	 * @return the active pills indices
	 */
	public int[] getActivePillsIndices()
	{
		int[] indices=new int[pills.cardinality()];
		
		int index=0;
		
		for(int i=0;i<currentMaze.pillIndices.length;i++)
			if(pills.get(i))
				indices[index++]=currentMaze.pillIndices[i];		
			
		return indices;
	}
	
	/**
	 * returns the indices of all active power pills in the mazes[gs.curMaze]
	 *
	 * @return the active power pills indices
	 */
	public int[] getActivePowerPillsIndices()
	{
		int[] indices=new int[powerPills.cardinality()];
		
		int index=0;
		
		for(int i=0;i<currentMaze.powerPillIndices.length;i++)
			if(powerPills.get(i))
				indices[index++]=currentMaze.powerPillIndices[i];		
			
		return indices;
	}

	/**
	 * If in lair (getLairTime(-)>0) or if not at junction.
	 *
	 * @param ghostType the ghost type
	 * @return true, if successful
	 */
	public boolean doesGhostRequireAction(GHOST ghostType)
	{
		//inlcude neutral here for the unique case where the ghost just left the lair
		return ((isJunction(ghosts.get(ghostType).currentNodeIndex) || (ghosts.get(ghostType).lastMoveMade== MOVE.NEUTRAL) && ghosts.get(ghostType).currentNodeIndex==currentMaze.initialGhostNodeIndex)
				&& (ghosts.get(ghostType).edibleTime==0 || ghosts.get(ghostType).edibleTime%GHOST_SPEED_REDUCTION!=0));
	}
	
	/**
	 * Checks if the node specified by the nodeIndex is a junction.
	 *
	 * @param nodeIndex the node index
	 * @return true, if is junction
	 */
	public boolean isJunction(int nodeIndex)
	{
		return currentMaze.graph[nodeIndex].numNeighbouringNodes>2;
	}
	
	/**
	 * Gets the possible moves from the node index specified.
	 *
	 * @param nodeIndex The current node index
	 * @return The set of possible moves
	 */
	public MOVE[] getPossibleMoves(int nodeIndex)
	{
		return currentMaze.graph[nodeIndex].allPossibleMoves.get(MOVE.NEUTRAL);
	}
	
	/**
	 * Gets the possible moves except the one that corresponds to the reverse of the move supplied.
	 *
	 * @param nodeIndex The current node index
	 * @param lastModeMade The last mode made (possible moves will exclude the reverse)
	 * @return The set of possible moves
	 */
	public MOVE[] getPossibleMoves(int nodeIndex, MOVE lastModeMade)
	{
		return currentMaze.graph[nodeIndex].allPossibleMoves.get(lastModeMade);
	}
	
	/**
	 * Gets the neighbouring nodes from the current node index.
	 *
	 * @param nodeIndex The current node index
	 * @return The set of neighbouring nodes
	 */
	public int[] getNeighbouringNodes(int nodeIndex)
	{
		return currentMaze.graph[nodeIndex].allNeighbouringNodes.get(MOVE.NEUTRAL);
	}
	
	/**
	 * Gets the neighbouring nodes from the current node index excluding the node 
	 * that corresponds to the opposite of the last move made which is given as an argument.
	 *
	 * @param nodeIndex The current node index
	 * @param lastModeMade The last mode made
	 * @return The set of neighbouring nodes except the one that is opposite of the last move made
	 */
	public int[] getNeighbouringNodes(int nodeIndex, MOVE lastModeMade)
	{
		return currentMaze.graph[nodeIndex].allNeighbouringNodes.get(lastModeMade);
	}
	
   /**
    * Given a node index and a move to be made, it returns the node index the move takes one to.
    * If there is no neighbour in that direction, the method returns -1.
    * 
    * @param nodeIndex The current node index
	* @param moveToBeMade The move to be made
	* @return The node index of the node the move takes one to
    */
    public int getNeighbour(int nodeIndex, MOVE moveToBeMade)
    {
    	Integer neighbour=currentMaze.graph[nodeIndex].neighbourhood.get(moveToBeMade);
    	
    	return neighbour==null ? -1 : neighbour;
    }
    	
	/**
	 * Method that returns the direction to take given a node index and an index of a neighbouring
	 * node. Returns null if the neighbour is invalid.
	 *
	 * @param currentNodeIndex The current node index.
	 * @param neighbourNodeIndex The direct neighbour (node index) of the current node.
	 * @return the move to make to reach direct neighbour
	 */
	public MOVE getMoveToMakeToReachDirectNeighbour(int currentNodeIndex, int neighbourNodeIndex)
	{
		for(MOVE move : MOVE.values())
		{
			if(currentMaze.graph[currentNodeIndex].neighbourhood.containsKey(move) 
					&& currentMaze.graph[currentNodeIndex].neighbourhood.get(move)==neighbourNodeIndex)
			{
				return move;
			}
		}
		
		return null;
	}
	
	/////////////////////////////////////////////////////////////////////////////
	///////////////////  Helper Methods (computational)  ////////////////////////
	/////////////////////////////////////////////////////////////////////////////

	/**
	 * Returns the PATH distance from any node to any other node.
	 *
	 * @param fromNodeIndex the from node index
	 * @param toNodeIndex the to node index
	 * @return the shortest path distance
	 */
	public int getShortestPathDistance(int fromNodeIndex,int toNodeIndex)
	{
		if(fromNodeIndex==toNodeIndex)
			return 0;
		else if(fromNodeIndex == -1 || toNodeIndex == -1)
			return Integer.MAX_VALUE;
		else if(fromNodeIndex<toNodeIndex)
			return currentMaze.shortestPathDistances[((toNodeIndex*(toNodeIndex+1))/2)+fromNodeIndex];
		else
			return currentMaze.shortestPathDistances[((fromNodeIndex*(fromNodeIndex+1))/2)+toNodeIndex];
	}
	
	/**
	 * Returns the EUCLEDIAN distance between two nodes in the current mazes[gs.curMaze].
	 *
	 * @param fromNodeIndex the from node index
	 * @param toNodeIndex the to node index
	 * @return the euclidean distance
	 */
	public double getEuclideanDistance(int fromNodeIndex,int toNodeIndex)
	{
		return Math.sqrt(Math.pow(currentMaze.graph[fromNodeIndex].x-currentMaze.graph[toNodeIndex].x,2)+Math.pow(currentMaze.graph[fromNodeIndex].y-currentMaze.graph[toNodeIndex].y,2));
	}
	
	/**
	 * Returns the MANHATTAN distance between two nodes in the current mazes[gs.curMaze].
	 *
	 * @param fromNodeIndex the from node index
	 * @param toNodeIndex the to node index
	 * @return the manhattan distance
	 */
	public int getManhattanDistance(int fromNodeIndex,int toNodeIndex)
	{
		return (int)(Math.abs(currentMaze.graph[fromNodeIndex].x-currentMaze.graph[toNodeIndex].x)+Math.abs(currentMaze.graph[fromNodeIndex].y-currentMaze.graph[toNodeIndex].y));
	}
	
	/**
	 * Gets the distance.
	 *
	 * @param fromNodeIndex the from node index
	 * @param toNodeIndex the to node index
	 * @param distanceMeasure the distance measure
	 * @return the distance
	 */
	public double getDistance(int fromNodeIndex, int toNodeIndex, DM distanceMeasure)
	{
		switch(distanceMeasure)
		{
			case PATH: return getShortestPathDistance(fromNodeIndex,toNodeIndex);
			case EUCLID: return getEuclideanDistance(fromNodeIndex,toNodeIndex);
			case MANHATTAN: return getManhattanDistance(fromNodeIndex,toNodeIndex);
		}
		
		return -1;
	}
	
	/**
	 * Returns the distance between two nodes taking reversals into account.
	 *
	 * @param fromNodeIndex the index of the originating node
	 * @param toNodeIndex the index of the target node
	 * @param lastMoveMade the last move made
	 * @param distanceMeasure the distance measure to be used
	 * @return the distance between two nodes.
	 */
	public double getDistance(int fromNodeIndex, int toNodeIndex, MOVE lastMoveMade, DM distanceMeasure)
	{
		switch(distanceMeasure)
		{
			case PATH: return getShortestPathDistance(fromNodeIndex,toNodeIndex,lastMoveMade);
			case EUCLID: return getEuclideanDistance(fromNodeIndex,toNodeIndex);
			case MANHATTAN: return getManhattanDistance(fromNodeIndex,toNodeIndex);
		}
		
		return -1;
	}
	
	/**
	 * Gets the closest node index from node index.
	 *
	 * @param fromNodeIndex the from node index
	 * @param targetNodeIndices the target node indices
	 * @param distanceMeasure the distance measure
	 * @return the closest node index from node index
	 */
	public int getClosestNodeIndexFromNodeIndex(int fromNodeIndex, int[] targetNodeIndices, DM distanceMeasure)
	{
		double minDistance=Integer.MAX_VALUE;
		int target=-1;
		
		for(int i=0;i<targetNodeIndices.length;i++)
		{				
			double distance=0;
			
			distance=getDistance(targetNodeIndices[i],fromNodeIndex,distanceMeasure);
					
			if(distance<minDistance)
			{
				minDistance=distance;
				target=targetNodeIndices[i];	
			}
		}
		
		return target;
	}

	/**
	 * Gets the farthest node index from node index.
	 *
	 * @param fromNodeIndex the from node index
	 * @param targetNodeIndices the target node indices
	 * @param distanceMeasure the distance measure
	 * @return the farthest node index from node index
	 */
	public int getFarthestNodeIndexFromNodeIndex(int fromNodeIndex, int[] targetNodeIndices, DM distanceMeasure)
	{
		double maxDistance=Integer.MIN_VALUE;
		int target=-1;
		
		for(int i=0;i<targetNodeIndices.length;i++)
		{				
			double distance=0;
			
			distance=getDistance(targetNodeIndices[i],fromNodeIndex,distanceMeasure);
					
			if(distance>maxDistance)
			{
				maxDistance=distance;
				target=targetNodeIndices[i];	
			}
		}
		
		return target;
	}

	/**
	 * Gets the next move towards target.
	 *
	 * @param fromNodeIndex the from node index
	 * @param toNodeIndex the to node index
	 * @param distanceMeasure the distance measure
	 * @return the next move towards target
	 */
	public MOVE getNextMoveTowardsTarget(int fromNodeIndex, int toNodeIndex, DM distanceMeasure)
	{
		MOVE move=null;

		double minDistance=Integer.MAX_VALUE;

		for(Entry<MOVE,Integer> entry : currentMaze.graph[fromNodeIndex].neighbourhood.entrySet())
		{
			double distance=getDistance(entry.getValue(),toNodeIndex,distanceMeasure);
								
			if(distance<minDistance)
			{
				minDistance=distance;
				move=entry.getKey();	
			}
		}
		
		return move;
	}
	
	/**
	 * Gets the next move away from target.
	 *
	 * @param fromNodeIndex the from node index
	 * @param toNodeIndex the to node index
	 * @param distanceMeasure the distance measure
	 * @return the next move away from target
	 */
	public MOVE
	getNextMoveAwayFromTarget(int fromNodeIndex, int toNodeIndex, DM distanceMeasure)
	{
		MOVE move=null;

		double maxDistance=Integer.MIN_VALUE;

		for(Entry<MOVE,Integer> entry : currentMaze.graph[fromNodeIndex].neighbourhood.entrySet())
		{
			double distance=getDistance(entry.getValue(),toNodeIndex,distanceMeasure);
								
			if(distance>maxDistance)
			{
				maxDistance=distance;
				move=entry.getKey();	
			}
		}
		
		return move;
	}
	
	/**
	 * UPGRADED, Gets the next move away from target.
	 *
	 * @param fromNodeIndex the from node index
	 * @param toNodeIndex the to node index
	 * @param distanceMeasure the distance measure
	 * @return the next move away from target
	 */
	public MOVE getNextMoveAwayFromTargetUpgraded(int fromNodeIndex, int toNodeIndex, DM distanceMeasure)
	{
		MOVE move=null;

		double maxDistance=Integer.MIN_VALUE;
		
		if(isJunction(fromNodeIndex)) {
			MOVE[] possibleMoves = getPossibleMoves(fromNodeIndex);
			int newNodeIndex;
			//Ghost newClosestNonEdibleGhost;
			double newDistance;
			
			for (MOVE possibleM : possibleMoves) {
				newNodeIndex = currentMaze.graph[fromNodeIndex].neighbourhood.get(possibleM);
				newDistance = getDistanceToClosestNonEdibleGhost(newNodeIndex, possibleM);
				
				if (newDistance > maxDistance) {
					maxDistance = newDistance;
					move = possibleM;
				}
			}
			
			/*for (MOVE possibleM : possibleMoves) {
				newNodeIndex = currentMaze.graph[fromNodeIndex].neighbourhood.get(possibleM);
				newClosestNonEdibleGhost = getClosestNonEdibleGhost(newNodeIndex);
				newDistance = getDistance(newNodeIndex, newClosestNonEdibleGhost.currentNodeIndex, distanceMeasure);
				
				if (newDistance > maxDistance) {
					maxDistance = newDistance;
					move = possibleM;
				}
			}*/
		}
		else {
			for(Entry<MOVE,Integer> entry : currentMaze.graph[fromNodeIndex].neighbourhood.entrySet())
			{
				double distance=getDistance(entry.getValue(),toNodeIndex,distanceMeasure);
									
				if(distance>maxDistance)
				{
					maxDistance=distance;
					move=entry.getKey();	
				}
			}
		}
		
		return move;
	}
	
	/**
	 * Gets the approximate next move towards target not considering directions opposing the last move made.
	 *
	 * @param fromNodeIndex The node index from which to move (i.e., current position)
	 * @param toNodeIndex The target node index
	 * @param lastMoveMade The last move made
	 * @param distanceMeasure The distance measure required (Manhattan, Euclidean or Straight line)
	 * @return The approximate next move towards target (chosen greedily)
	 */
	public MOVE getApproximateNextMoveTowardsTarget(int fromNodeIndex, int toNodeIndex, MOVE lastMoveMade, DM distanceMeasure)
	{
		MOVE move=null;

		double minDistance=Integer.MAX_VALUE;

		for(Entry<MOVE,Integer> entry : currentMaze.graph[fromNodeIndex].allNeighbourhoods.get(lastMoveMade).entrySet())
		{
			double distance=getDistance(entry.getValue(),toNodeIndex,distanceMeasure);
								
			if(distance<minDistance)
			{
				minDistance=distance;
				move=entry.getKey();	
			}
		}
		
		return move;
	}
	
	/**
	 * Gets the approximate next move away from a target not considering directions opposing the last move made.
	 *
	 * @param fromNodeIndex The node index from which to move (i.e., current position)
	 * @param toNodeIndex The target node index
	 * @param lastMoveMade The last move made
	 * @param distanceMeasure The distance measure required (Manhattan, Euclidean or Straight line)
	 * @return The approximate next move towards target (chosen greedily)
	 */
	public MOVE getApproximateNextMoveAwayFromTarget(int fromNodeIndex, int toNodeIndex, MOVE lastMoveMade, DM distanceMeasure)
	{
		MOVE move=null;

		double maxDistance=Integer.MIN_VALUE;

		for(Entry<MOVE,Integer> entry : currentMaze.graph[fromNodeIndex].allNeighbourhoods.get(lastMoveMade).entrySet())
		{
			double distance=getDistance(entry.getValue(),toNodeIndex,distanceMeasure);
								
			if(distance>maxDistance)
			{
				maxDistance=distance;
				move=entry.getKey();	
			}
		}
		
		return move;
	}
	
	/**
	 * Gets the exact next move towards target taking into account reversals. This uses the pre-computed paths.
	 *
	 * @param fromNodeIndex The node index from which to move (i.e., current position)
	 * @param toNodeIndex The target node index
	 * @param lastMoveMade The last move made
	 * @param distanceMeasure the distance measure to be used
	 * @return the next move towards target
	 */
	public MOVE getNextMoveTowardsTarget(int fromNodeIndex, int toNodeIndex, MOVE lastMoveMade, DM distanceMeasure)
	{
		MOVE move=null;

		double minDistance=Integer.MAX_VALUE;

		for(Entry<MOVE,Integer> entry : currentMaze.graph[fromNodeIndex].allNeighbourhoods.get(lastMoveMade).entrySet())
		{
			double distance=getDistance(entry.getValue(),toNodeIndex,lastMoveMade,distanceMeasure);
								
			if(distance<minDistance)
			{
				minDistance=distance;
				move=entry.getKey();	
			}
		}
		
		return move;
	}
	
	/**
	 * Gets the exact next move away from target taking into account reversals. This uses the pre-computed paths.
	 *
	 * @param fromNodeIndex The node index from which to move (i.e., current position)
	 * @param toNodeIndex The target node index
	 * @param lastMoveMade The last move made
	 * @param distanceMeasure the distance measure to be used
	 * @return the next move away from target
	 */

	public MOVE getNextMoveAwayFromTarget(int fromNodeIndex, int toNodeIndex, MOVE lastMoveMade, DM distanceMeasure)
	{
		MOVE move=null;

		double maxDistance=Integer.MIN_VALUE;

		for(Entry<MOVE,Integer> entry : currentMaze.graph[fromNodeIndex].allNeighbourhoods.get(lastMoveMade).entrySet())
		{
			double distance=getDistance(entry.getValue(),toNodeIndex,lastMoveMade,distanceMeasure);
								
			if(distance>maxDistance)
			{
				maxDistance=distance;
				move=entry.getKey();	
			}
		}
		
		return move;
	}



	/**
	 * Gets the A* path considering previous moves made (i.e., opposing actions are ignored)
	 *
	 * @param fromNodeIndex The node index from which to move (i.e., current position)
	 * @param toNodeIndex The target node index
	 * @param lastMoveMade The last move made
	 * @return The A* path
	 * 
	 * @deprecated use getShortestPath() instead.
	 */
	@Deprecated
	public int[] getAStarPath(int fromNodeIndex, int toNodeIndex, MOVE lastMoveMade)
	{
		return getShortestPath(fromNodeIndex,toNodeIndex,lastMoveMade);
	}
	
	/**
	 * Gets the shortest path from node A to node B as specified by their indices.
	 *
	 * @param fromNodeIndex The node index from where to start (i.e., current position)
	 * @param toNodeIndex The target node index
	 * @return the shortest path from start to target
	 */
	public int[] getShortestPath(int fromNodeIndex,int toNodeIndex)
	{
		return caches[mazeIndex].getPathFromA2B(fromNodeIndex,toNodeIndex);
	}
	
	/**
	 * Gets the approximate shortest path taking into account the last move made (i.e., no reversals).
	 * This is approximate only as the path is computed greedily. A more accurate path can be obtained
	 * using A* which is slightly more costly.
	 *
	 * @param fromNodeIndex The node index from where to start (i.e., current position)
	 * @param toNodeIndex The target node index
	 * @param lastMoveMade The last move made
	 * @return the shortest path from start to target
	 * 
	 * @deprecated use getShortestPath() instead.
	 */
	@Deprecated
	public int[] getApproximateShortestPath(int fromNodeIndex, int toNodeIndex, MOVE lastMoveMade)
	{
		return getShortestPath(fromNodeIndex,toNodeIndex,lastMoveMade);
	}
	
	/**
	 * Gets the shortest path taking into account the last move made (i.e., no reversals).
	 * This is approximate only as the path is computed greedily. A more accurate path can be obtained
	 * using A* which is slightly more costly.
	 *
	 * @param fromNodeIndex The node index from where to start (i.e., current position)
	 * @param toNodeIndex The target node index
	 * @param lastMoveMade The last move made
	 * @return the shortest path from start to target
	 */
	public int[] getShortestPath(int fromNodeIndex, int toNodeIndex, MOVE lastMoveMade)
	{
		if(currentMaze.graph[fromNodeIndex].neighbourhood.size()==0)//lair
			return new int[0];

		return caches[mazeIndex].getPathFromA2B(fromNodeIndex,toNodeIndex,lastMoveMade);
	}
	
	/**
	 * Similar to getApproximateShortestPath but returns the distance of the path only. It is slightly
	 * more efficient.
	 *  
	 * @param fromNodeIndex The node index from where to start (i.e., current position)
	 * @param toNodeIndex The target node index
	 * @param lastMoveMade The last move made
	 * @return the exact distance of the path
	 * 
	 * @deprecated use getShortestPathDistance() instead.
	 */
	@Deprecated
	public int getApproximateShortestPathDistance(int fromNodeIndex, int toNodeIndex, MOVE lastMoveMade)
	{
		return getShortestPathDistance(fromNodeIndex,toNodeIndex,lastMoveMade);
	}
	
	/**
	 * Similar to getShortestPath but returns the distance of the path only. It is slightly
	 * more efficient.
	 *  
	 * @param fromNodeIndex The node index from where to start (i.e., current position)
	 * @param toNodeIndex The target node index
	 * @param lastMoveMade The last move made
	 * @return the exact distance of the path
	 */
	public int getShortestPathDistance(int fromNodeIndex, int toNodeIndex, MOVE lastMoveMade)
	{
		if(currentMaze.graph[fromNodeIndex].neighbourhood.size()==0)//lair
			return 0;

		return caches[mazeIndex].getPathDistanceFromA2B(fromNodeIndex,toNodeIndex,lastMoveMade);
	}	
	
	/**
	 * Returns the distance to the closest powerpill in a given direction
	 */
	public int getDistToClosestPowerPill4d(int pacmanNode, MOVE direction){
		int dist;
		int mindist=Integer.MAX_VALUE;
		
		if(currentMaze.graph[pacmanNode].neighbourhood.size()==0)//lair
			return 0;
		
		int [] PPIndices = getPowerPillIndices();
		
		for(int i = 0; i < PPIndices.length; i++){
			if(isPowerPillStillAvailable(PPIndices[i])){
				dist = getShortestPathDistance(pacmanNode, PPIndices[i], direction);
				if (dist < mindist)
					mindist = dist;
			}
		}
		
		return mindist;
	}
	
	/**
	 * Returns the distance to the closest powerpill in any direction
	 */
	public int getDistToClosestPowerPill(int pacmanNode){
		int dist;
		int mindist=Integer.MAX_VALUE;
		int [] PPIndices = getPowerPillIndices();
		
		for(int i = 0; i < PPIndices.length; i++){
			if(isPowerPillStillAvailable(PPIndices[i])){
				dist = getShortestPathDistance(pacmanNode, PPIndices[i]);
				if (dist < mindist)
					mindist = dist;
			}
		}
		
		return mindist;
	}
	
	/**
	 * Returns the distance to the closest pill in a given direction
	 */
	public int getDistToClosestPill4d(int pacmanNode, MOVE direction){
		int dist;
		int mindist=Integer.MAX_VALUE;
		
		if(currentMaze.graph[pacmanNode].neighbourhood.size()==0)//lair
			return 0;
		
		int [] PIndices = getPillIndices();
		
		for(int i = 0; i < PIndices.length; i++){
			if(isPillStillAvailable(PIndices[i])){
				dist = getShortestPathDistance(pacmanNode, PIndices[i], direction);
				if (dist < mindist)
					mindist = dist;
			}
		}
		
		return mindist;
	}
	
	/**
	 * Returns the distance to the closest pill in any direction
	 */
	public int getDistToClosestPill(int pacmanNode){
		int mindist=Integer.MAX_VALUE;
		
		if(currentMaze.graph[pacmanNode].neighbourhood.size()==0)//lair
			return 0;
		
		int down = getDistToClosestPill4d(pacmanNode, MOVE.DOWN);
		int up = getDistToClosestPill4d(pacmanNode, MOVE.UP);
		int left = getDistToClosestPill4d(pacmanNode, MOVE.LEFT);
		int right = getDistToClosestPill4d(pacmanNode, MOVE.RIGHT);
		
		if(down < mindist)
			mindist = down;
		if(up < mindist)
			mindist = up;
		if(left < mindist)
			mindist = left;
		if(right < mindist)
			mindist = right;
		
		return mindist;
	}
	
	/**
	 * Returns the direction towards the closest edible ghost (NEUTRAL if no edible ghosts avaiable)
	 */
	public MOVE getDirectionTowardsClosestEdibleGhost(int pacmanNode){
		MOVE ret = MOVE.NEUTRAL;
		Ghost g = getClosestEdibleGhost(pacmanNode);
		if (g != null)
			ret = getNextMoveTowardsTarget(pacmanNode, g.currentNodeIndex, DM.PATH);
		
		return ret;
	}
	
	/**
	 * Returns true/false depending if a movement is possible in a given direction
	 */
	public boolean isDirectionTakeable(int pacmanNode, MOVE direction){
		boolean ret = false;
		MOVE[] movs = getPossibleMoves(pacmanNode);
		for(int i = 0; i < movs.length; i++){
			if(direction == movs[i]){
				ret = true;
				break;
			}
		}
		return ret;
	}
	
	/**
	 * Returns the movement opposed to the direction of the closest non edible ghost
	 */
	public MOVE getDirectionAwayFromClosestNonEdibleGhost(int pacmanNode){
		MOVE ret = MOVE.NEUTRAL;
		Ghost g = getClosestNonEdibleGhost(pacmanNode);
		if (g != null)
			ret = getNextMoveAwayFromTargetUpgraded(pacmanNode, g.currentNodeIndex, DM.PATH);
		
		return ret;
	}
	
	/**
	 * Returns the movement towards the closest PowerPill
	 */
	public MOVE getDirectionTowardsClosestPowerPill(int pacmanNode){
		MOVE ret = MOVE.NEUTRAL;
		int cpp = getClosestPowerPill(pacmanNode);
		if (cpp != -1) 
			ret = getNextMoveTowardsTarget(pacmanNode, cpp, DM.PATH);
		
		return ret;
	}
	
	/**
	 * Returns the movement towards the closest Pill
	 */
	public MOVE getDirectionTowardsClosestPill(int pacmanNode){
		MOVE ret = MOVE.NEUTRAL;
		int cpp = getClosestPill(pacmanNode);
		if (cpp != -1) 
			ret = getNextMoveTowardsTarget(pacmanNode, cpp, DM.PATH);
		
		return ret;
	}
	
	/**
	 * Returns the index of the closest junction to the Pacman
	 */
	@Deprecated
	public int getClosestJunction(int pacmanLocation, MOVE lastMoveMade)
	{
		int[] junctionIndices = getJunctionIndices();
		
		int minJunctionIndex = junctionIndices[0];
		int minDistance = getShortestPathDistance(pacmanLocation, minJunctionIndex, lastMoveMade);
		for(int i = 1; i < junctionIndices.length; i++)
		{
			int junctionDist = getShortestPathDistance(pacmanLocation, junctionIndices[i], lastMoveMade);
			if(junctionDist < minDistance)
			{
				minJunctionIndex = junctionIndices[i];
				minDistance = junctionDist;
			}
		}
		
		return minJunctionIndex;
	}
	
	/**
	 * Returns the index of the closest junction to Pacman following a direction
	 * if pacmanLocation is a junction, returns the next junction
	 * if the immediate first node is a wall, returns -1
	 * the algorithm doesn't get stuck on corners
	 */
	public int getClosestJunctionUpgraded(int pacmanLocation, MOVE direction) {
		int oldNode = pacmanLocation;
		int currentNode = getNeighbour(pacmanLocation, direction);
		if (currentNode == -1)
			return -1;
		
		
		while(!isJunction(currentNode)) {
			int[] newNodes = getNeighbouringNodes(currentNode);
			if(newNodes[0] == oldNode) {
				oldNode = currentNode;
				currentNode = newNodes[1];
			}
			else {
				oldNode = currentNode;
				currentNode = newNodes[0];
			}
		}
		
		return currentNode;
	}
	
	/**
	 * Returns the number of exits of the closest junction to Pacman
	 */
	public int getClosestJunctionExitsNumber(int pacmanLocation, MOVE direction)
	{
		int closestJunctionIndex = getClosestJunctionUpgraded(pacmanLocation, direction);
		if(closestJunctionIndex == -1)
			return 0;
		
		return currentMaze.graph[closestJunctionIndex].numNeighbouringNodes;
	}
	
	/**
	 * Returns the distance of Pacman to the closest intersection
	 * Same as getClosestJunction() function but returning the distance
	 */
	public int getDistanceToClosestJunction(int pacmanLocation, MOVE direction)
	{
		int oldNode = pacmanLocation;
		int currentNode = getNeighbour(pacmanLocation, direction);
		if (currentNode == -1)
			return Integer.MAX_VALUE;
		
		int distance = 1;
		while(!isJunction(currentNode)) {
			int[] newNodes = getNeighbouringNodes(currentNode);
			if(newNodes[0] == oldNode) {
				oldNode = currentNode;
				currentNode = newNodes[1];
			}
			else {
				oldNode = currentNode;
				currentNode = newNodes[0];
			}
			distance++;
		}
		
		return distance;
	}
	
	/**
	 * Returns the distance of the closest non-edible ghost to the closest junction to Pacman
	 */
	public int getClosestNonEdibleGhostDistanceToClosestJunction(int pacmanLocation, MOVE direction) {
		int junctionIndex = getClosestJunctionUpgraded(pacmanLocation, direction);
		if (junctionIndex == -1)
			return -1;
		
		int minDistance = Integer.MAX_VALUE;
		for (Ghost ghost : this.ghosts.values()) {
			if (!ghost.isEdible() && !ghost.isInLair())	{
				int distance = this.getShortestPathDistance(ghost.currentNodeIndex, junctionIndex, ghost.lastMoveMade);
	
				if (distance < minDistance)
					minDistance = distance;
			}
		}
		
		return minDistance;
	}
	
	/**
	 * Returns the distance of the closest edible ghost to the closest junction to Pacman
	 */
	public int getClosestEdibleGhostDistanceToClosestJunction(int pacmanLocation, MOVE direction) {
		int junctionIndex = getClosestJunctionUpgraded(pacmanLocation, direction);
		
		if (junctionIndex == -1)
			return Integer.MAX_VALUE;
		
		int minDistance = Integer.MAX_VALUE;
		for (Ghost ghost : this.ghosts.values()) {
			if (ghost.isEdible() && !ghost.isInLair())	{
				int distance = this.getShortestPathDistance(junctionIndex, ghost.currentNodeIndex);
	
				if (distance < minDistance)
					minDistance = distance;
			}
		}
		
		return minDistance;
	}
	
	/**
	 * Returns true if Pacman can reach safely the closest junction
	 * Can be optimized 
	 */
	
	public boolean isJunctionSafe(int pacmanLocation, MOVE direction) {
		if (getNeighbour(pacmanLocation, direction) == -1)
			return false;
		
		int ghostDistanceToJunction = getClosestNonEdibleGhostDistanceToClosestJunction(pacmanLocation, direction); //Not the real distance (as it doesnt uses ghost last move, but it doesnt needs to be
		int pacmanDistanceToJunction = getDistanceToClosestJunction(pacmanLocation, direction);
		if (ghostDistanceToJunction > pacmanDistanceToJunction)	//Pacman is closer to the junction than any ghost
			return true;
		
		//check if any ghost is between junction and Pacman
		for (Ghost ghost : this.ghosts.values()) {
			if (!ghost.isEdible() && !ghost.isInLair())	{
				int distanceGhostToPacman = this.getShortestPathDistance(ghost.currentNodeIndex, pacmanLocation, ghost.lastMoveMade);	//check if the ghost is going towards or away from pacman
	
				if (pacmanDistanceToJunction <= distanceGhostToPacman)	//ghost is between junction and Pacman and facing Pacman
					return false;
			}
		}
	
		return true;
		
	}
	
	/**
	 * Returns the geometric mean of the distance between Pacman and all the non-edible ghosts
	 * Ghosts in the lair are also taken into account
	 */
	public int getGeometricMeanDistanceToNonEdibleGhosts(int pacmanLocation)
	{
		double productOfDistances = 1.0f;
		
		int number = 0;
		for (Ghost ghost : this.ghosts.values())
		{
			if (!ghost.isEdible() && !ghost.isInLair())
			{
				productOfDistances *= this.getShortestPathDistance(pacmanLocation, ghost.currentNodeIndex);
				number++;
			}
		}
		
		if (number == 0)			//if no ghosts
			return Integer.MAX_VALUE;
		

		Double result = Math.pow(productOfDistances, 1/number); //counting only edible and out of lair ghosts
		
		return result.intValue();
	}
	
	/**
	 * Returns the geometric mean of the Pacman distance to all the edible ghosts
	 */
	public int getGeometricMeanDistanceToEdibleGhosts(int pacmanLocation)
	{
		double productOfDistances = 1.0f;
		
		int number = 0;
		for (Ghost ghost : this.ghosts.values())
		{
			if (ghost.isEdible() && !ghost.isInLair())
			{
				productOfDistances *= this.getShortestPathDistance(pacmanLocation, ghost.currentNodeIndex);
				number++;
			}
		}
		
		if (number == 0)			//if no ghosts
			return Integer.MAX_VALUE;
		
		Double result = Math.pow(productOfDistances, 1/number); //counting only edible and out of lair ghosts
		
		return result.intValue();
	}
	
	/**
	 * direction moves modified to avoid jam situations
	 * returns direction on junctions
	 * returns direction on tunnels if direction is a possible move
	 * returns last move on tunnels if direction it's not a possible move
	 */
	public MOVE getSmartmove(int pacmanLocation, MOVE direction) {
		
		if (isJunction(pacmanLocation))		//returns direction on junctions
			return direction;
		else if (getNeighbour(pacmanLocation, direction) != -1)		//returns direction on tunnels if direction is a possible move
			return direction;
		else {		//returns last move on tunnels if direction it's not a possible move
			if (getNeighbour(pacmanLocation, getPacmanLastMoveMade()) != -1)		// 'I' tunnel
				return getPacmanLastMoveMade();
			else if (getNeighbour(pacmanLocation, getPacmanLastMoveMade().L90()) != -1)	// 'L' tunnel 90º left
				return getPacmanLastMoveMade().L90();
			else if (getNeighbour(pacmanLocation, getPacmanLastMoveMade().R90()) != -1)	// 'L' tunnel 90º right
				return getPacmanLastMoveMade().R90();
			else
				return MOVE.NEUTRAL;	//unknown situation
		}
		
	}
	
	//***********************FUNCIONES DE ALTO NIVEL************************
	//USANDO:
	/*
	 * Una gramatica con 10,20,30,40 como distancias
	 * Distancia a fantasma comible mas cercano
	 * Distancia a fantasma no comible mas cercano
	 * Huir
	 * Atacar
	 * Farmear
	 */

	
	/**Mirar powerpill mas cercana, su distancia de pacman a ella ppd
	 *Si las distancias son mayores desde todos los fantasmas a la PP que la de pacman, ir a por la PP
	 *Si no, huir en la direccion por la que se tarde mas en llegar a un fantasma
	 */
	public MOVE huir(){
		
		boolean go2pp = true;
		
		int closestpp = getClosestPowerPill(getPacmanCurrentNodeIndex());

		if (closestpp < 0) {
			Ghost nonEdibleGhost = getClosestNonEdibleGhost(getPacmanCurrentNodeIndex());
			if (nonEdibleGhost != null)
				return getNextMoveAwayFromTargetUpgraded(getPacmanCurrentNodeIndex(), getClosestNonEdibleGhost(getPacmanCurrentNodeIndex()).currentNodeIndex, DM.PATH);
			else
				return MOVE.NEUTRAL;
		}
		
		double distpmpp = getDistance(getPacmanCurrentNodeIndex(), closestpp, DM.PATH); //returns double with different DMs
		
		for (Ghost ghost : this.ghosts.values())
		{
			if (!ghost.isEdible() && !ghost.isInLair())
			{
				if((int)getDistance(closestpp, ghost.currentNodeIndex, DM.PATH) < distpmpp)
					go2pp = false;
			}
		}
		
		if(go2pp)
			return getNextMoveTowardsTarget(getPacmanCurrentNodeIndex(), closestpp, DM.PATH);
		else{
			return getNextMoveAwayFromTargetUpgraded(getPacmanCurrentNodeIndex(), getClosestNonEdibleGhost(getPacmanCurrentNodeIndex()).currentNodeIndex, DM.PATH);
		}		
		
	}
	
	/**
	 *Mirar si se puede llegar al fantasma comible mas cercano
	 *Antes de que pueda llegar ningun fantasma no comible a el
	 *Si tal, ir a comerselo, si no, llamada a farmear
	 */
	public MOVE atacar(){
		
		boolean atk = true;
		Ghost g = getClosestEdibleGhost(getPacmanCurrentNodeIndex());
		if (g == null)
			return MOVE.NEUTRAL;
		int closestg = g.currentNodeIndex;

		double distpmg = getDistance(getPacmanCurrentNodeIndex(), closestg, DM.PATH);
		
		for (Ghost ghost : this.ghosts.values())
		{
			if (!ghost.isEdible() && !ghost.isInLair())
			{
				if(getDistance(closestg, ghost.currentNodeIndex, DM.PATH) < distpmg)
					atk = false;
			}
		}
		
		if(atk)
			return getNextMoveTowardsTarget(getPacmanCurrentNodeIndex(), closestg, DM.PATH);
		else{
			return MOVE.NEUTRAL;
		}		
	}
	
	/**
	 *Mirar si se puede llegar al fantasma comible mas cercano
	 *Antes de que pueda llegar ningun fantasma no comible a el
	 *Si tal, ir a comerselo, si no, llamada a farmear
	 */
	public MOVE atacarCheta(){
		
		boolean atk = true;
		Ghost g = getClosestEdibleGhost(getPacmanCurrentNodeIndex());
		if (g == null)
			return farmear();
		int closestg = g.currentNodeIndex;

		double distpmg = getDistance(getPacmanCurrentNodeIndex(), closestg, DM.PATH);
		
		for (Ghost ghost : this.ghosts.values())
		{
			if (!ghost.isEdible() && !ghost.isInLair())
			{
				if(getDistance(closestg, ghost.currentNodeIndex, DM.PATH) < distpmg)
					atk = false;
			}
		}
		
		if(atk)
			return getNextMoveTowardsTarget(getPacmanCurrentNodeIndex(), closestg, DM.PATH);
		else{
			return farmear();
		}		
	}
	
	/**
	 * Ir a por pill mas cercana a la que no llegen antes los fantasmas antes, si no puede llegar a ninguna se aleja de ellos
	 */
	public MOVE farmear(){
		
		boolean go2p = true;
		
		int closestp = getClosestPill(getPacmanCurrentNodeIndex());
		if (closestp < 0) {
			int closestpp = getClosestPowerPill(getPacmanCurrentNodeIndex());
			return getNextMoveTowardsTarget(getPacmanCurrentNodeIndex(), closestpp, DM.PATH);
		}

		double distpmp = getDistance(getPacmanCurrentNodeIndex(), closestp, DM.PATH);
		
		for (Ghost ghost : this.ghosts.values())
		{
			if (!ghost.isEdible() && !ghost.isInLair())
			{
				if(getDistance(closestp, ghost.currentNodeIndex, DM.PATH) < distpmp)
					go2p = false;
			}
		}
		
		if(go2p)
			return getNextMoveTowardsTarget(getPacmanCurrentNodeIndex(), closestp, DM.PATH);
		else{
			return MOVE.NEUTRAL;
		}		
	}
	
	/**
	 * Ir a por pill mas cercana a la que no llegen antes los fantasmas antes, si no puede llegar a ninguna se aleja de ellos
	 */
	public MOVE farmearCheta(){
		
		boolean go2p = true;
		
		int closestp = getClosestPill(getPacmanCurrentNodeIndex());
		if (closestp < 0) {
			int closestpp = getClosestPowerPill(getPacmanCurrentNodeIndex());
			return getNextMoveTowardsTarget(getPacmanCurrentNodeIndex(), closestpp, DM.PATH);
		}

		double distpmp = getDistance(getPacmanCurrentNodeIndex(), closestp, DM.PATH);
		
		for (Ghost ghost : this.ghosts.values())
		{
			if (!ghost.isEdible() && !ghost.isInLair())
			{
				if(getDistance(closestp, ghost.currentNodeIndex, DM.PATH) < distpmp)
					go2p = false;
			}
		}
		
		if(go2p)
			return getNextMoveTowardsTarget(getPacmanCurrentNodeIndex(), closestp, DM.PATH);
		else{
			return getNextMoveAwayFromTargetUpgraded(getPacmanCurrentNodeIndex(), getClosestNonEdibleGhost(getPacmanCurrentNodeIndex()).currentNodeIndex, DM.PATH);
		}		
	}
	
}