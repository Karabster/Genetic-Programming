package pacman;

import Genetic.Generation;
import pacman.controllers.Controller;
import pacman.controllers.GeneticController;

import pacman.controllers.examples.*;
import pacman.game.Game;
import pacman.game.GameView;

import Genetic.*;

import java.io.*;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.Random;

import static pacman.game.Constants.*;

/**
 * This class may be used to execute the game in timed or un-timed modes, with or without
 * visuals. Competitors should implement their controllers in game.entries.ghosts and 
 * game.entries.pacman respectively. The skeleton classes are already provided. The package
 * structure should not be changed (although you may create sub-packages in these packages).
 *
 */
@SuppressWarnings("unused")
public class Executor
{	
	/**
	 * The main method contains the main testing setup of the high complexity controller vs Legacy ghosts in 4 mazes
	 * In the comments a method to create create a replay of the best game of the best controller and play it back
	 * The game shall be recorded into a file, which if does not exists will be created by the system
	 * @author Nikita Kuzmin
	 *
	 * @param args the command line arguments
	 */
	public static void main(String[] args)
	{	int generation = 0;
		int sum = 0;
		Executor exec=new Executor();
		
		GeneticController pacman = new GeneticController();
		Controller<EnumMap<GHOST,MOVE>> ghosts = new Legacy();


		LispStatements ls = new LispStatements();
		ArrayList<String> functionSet = new ArrayList<>();
		functionSet.add("2ifblue");
		functionSet.add("4ifle");
		functionSet.add("2ifindanger");
		functionSet.add("2ifpowerpillscleared");
		functionSet.add("2istopowersafe");
		ArrayList<String> terminalSet = new ArrayList<>();
		terminalSet.add("distToPill");
		terminalSet.add("distToPower");
		terminalSet.add("distGhost");
		terminalSet.add("distTo2ndGhost");
		terminalSet.add("distTo3rdGhost");
		terminalSet.add("distToEdibleGhost");
		terminalSet.add("distToNonEdibleGhost");
		terminalSet.add("moveToFood");
		terminalSet.add("moveToPower");
		terminalSet.add("moveToEdibleGhost");
		terminalSet.add("moveFromNonEdibleGhost");
		terminalSet.add("moveFromPower");
		terminalSet.add("runaway");
		try {
			ls.setFunctionalSet(functionSet);
		}catch (Exception ignored){

		}
		ls.setTerminalSet(terminalSet);
		try {
			Genetic.Executor.setUp(pacman, ls, 8);
		}catch (Exception ignored){

		}

		double average = 0;

		while (generation < 51) {
			while (Generation.isGenFinished()) {

					for(int i = 0; i < 10; i++) {
						int delay = 0;
						boolean visual = false;
						sum += exec.runGame(pacman, ghosts, visual, delay);
					}

					Generation.setFitness(50000 - (sum/10));
					System.out.println(sum/10);


					sum = 0;

				Generation.pointerUpdate();
			}
			System.out.println("Generation " + generation);
			Genetic.Executor.newGen();
			System.out.println(Generation.getBestRating());
			generation ++;
		}

		System.out.println(Generation.getBestTree());
		System.out.println(Generation.getBestRating());



/*
		boolean visual=false;
		String fileName="results/replay10.txt";

		int bestScore = 0;
		int result = 0;
		int average = 0;
		for(int i = 0; i < 1000 ; i++) {
			result = exec.runGameTimedRecorded(pacman, ghosts, visual, fileName, bestScore);
			average +=result;
			if(result > bestScore){
				bestScore = result;
			}
			System.out.println(result);
		}
		visual = true;
		System.out.println(average/1000.0);
		exec.replayGame(fileName,visual);
		*/


	}
	
    /**
     * For running multiple games without visuals. This is useful to get a good idea of how well a controller plays
     * against a chosen opponent: the random nature of the game means that performance can vary from game to game. 
     * Running many games and looking at the average score (and standard deviation/error) helps to get a better
     * idea of how well the controller is likely to do in the competition.
     *
     * @param pacManController The Pac-Man controller
     * @param ghostController The Ghosts controller
     * @param trials The number of trials to be executed
     */
    public ArrayList<Double> runExperiment(Controller<MOVE> pacManController,Controller<EnumMap<GHOST,MOVE>> ghostController,int trials)
    {
    	double avgScore=0;
    	double avgTime=0;
    	double maxScore = 0;
    	
    	Random rnd=new Random(0);
		Game game;
		
		for(int i=0;i<trials;i++)
		{
			game=new Game(rnd.nextLong());
			int lastLevel = 0;
			
			while(!game.gameOver())
			{				
		        game.advanceGame(pacManController.getMove(game.copy(),System.currentTimeMillis()+DELAY),
		        		ghostController.getMove(game.copy(),System.currentTimeMillis()+DELAY));
		        lastLevel = game.getCurrentLevel();
			}
			if(game.getScore() > maxScore){
				maxScore = game.getScore();
			}
			
			avgScore+=game.getScore();
			avgTime+=game.getTotalTime();
			//System.out.println(i+"\t"+game.getScore());
		}
		
		ArrayList<Double> ret = new ArrayList<>(3);
		ret.add(avgScore/trials);
		ret.add(avgTime/trials);
		ret.add(maxScore);
		
		System.out.println(avgScore/trials);
		System.out.println("This is the max Score: " + maxScore);
		return ret;
    }
	
	/**
	 * Run a game in asynchronous mode: the game waits until a move is returned. In order to slow thing down in case
	 * the controllers return very quickly, a time limit can be used. If fasted gameplay is required, this delay
	 * should be put as 0.
	 *
	 * @param pacManController The Pac-Man controller
	 * @param ghostController The Ghosts controller
	 * @param visual Indicates whether or not to use visuals
	 * @param delay The delay between time-steps
	 */
	public int runGame(Controller<MOVE> pacManController,Controller<EnumMap<GHOST,MOVE>> ghostController,boolean visual,int delay)
	{
		Game game=new Game(0);

		GameView gv=null;
		
		if(visual)
			gv=new GameView(game).showGame();
		
		int lastLevel = 0;
		
		while(!game.gameOver())
		{				
	        game.advanceGame(pacManController.getMove(game.copy(),-1),ghostController.getMove(game.copy(),-1));
	        lastLevel = game.getCurrentLevel();
	        
	        try{Thread.sleep(delay);}catch(Exception e){}
	        
	        if(visual)
	        	gv.repaint();
		}

		return game.getScore();
	}
	


    
	/**
	 * Run a game in asynchronous mode and recorded.
	 *
     * @param pacManController The Pac-Man controller
     * @param ghostController The Ghosts controller
     * @param visual Whether to run the game with visuals
	 * @param fileName The file name of the file that saves the replay
	 */
	public int runGameTimedRecorded(Controller<MOVE> pacManController,Controller<EnumMap<GHOST,MOVE>> ghostController,boolean visual,String fileName, int bestScore)
	{
		StringBuilder replay=new StringBuilder();

		Game game=new Game(0);
		int lastLevel = 0;

		GameView gv=null;

		if(visual)
		{
			gv=new GameView(game).showGame();

		}


		while(!game.gameOver())
		{
			game.advanceGame(pacManController.getMove(game.copy(), -1),ghostController.getMove(game.copy(), -1));
			lastLevel = game.getCurrentLevel();

			try
			{
				Thread.sleep(0);
			}
			catch(InterruptedException e)
			{
				e.printStackTrace();
			}



	        if(visual)
	        	gv.repaint();

	        replay.append(game.getGameState()+"\n");
		}

		pacManController.terminate();
		ghostController.terminate();
		if(game.getScore() > bestScore) {
			saveToFile(replay.toString(), fileName, false);
		}
		return game.getScore();
	}
	
	/**
	 * Replay a previously saved game.
	 *
	 * @param fileName The file name of the game to be played
	 * @param visual Indicates whether or not to use visuals
	 */
	public void replayGame(String fileName,boolean visual)
	{
		ArrayList<String> timeSteps=loadReplay(fileName);
		
		Game game=new Game(0);
		
		GameView gv=null;
		
		if(visual)
			gv=new GameView(game).showGame();
		
		for(int j=0;j<timeSteps.size();j++)
		{			
			game.setGameState(timeSteps.get(j));

			try
			{
				Thread.sleep(18);
			}
			catch(InterruptedException e)
			{
				e.printStackTrace();
			}
	        if(visual)
	        	gv.repaint();
		}
	}
	
	//save file for replays
    public static void saveToFile(String data,String name,boolean append)
    {
        try 
        {
            FileOutputStream outS=new FileOutputStream(name,append);
            PrintWriter pw=new PrintWriter(outS);

            pw.println(data);
            pw.flush();
            outS.close();

        } 
        catch (IOException e)
        {
            System.out.println("Could not save data!");	
        }
    }  

    //load a replay
    private static ArrayList<String> loadReplay(String fileName)
	{
    	ArrayList<String> replay=new ArrayList<String>();
		
        try(BufferedReader br=new BufferedReader(new InputStreamReader(new FileInputStream(fileName))))
        {
            String input=br.readLine();		
            
            while(input!=null)
            {
            	if(!input.equals(""))
            		replay.add(input);

            	input=br.readLine();	
            }
        }
        catch(IOException ioe)
        {
            ioe.printStackTrace();
        }
        
        return replay;
	}
}