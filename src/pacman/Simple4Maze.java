package pacman;

import Genetic.LispStatements;
import pacman.controllers.Controller;
import pacman.controllers.SimpleController;
import pacman.controllers.examples.Legacy;
import pacman.game.Constants;

import java.util.ArrayList;
import java.util.EnumMap;
/**
 * This class provides with the playback of the best game from 4 maze experiment against Legacy ghosts, using simple methods
 * @author Nikita Kuzmin
 */
public class Simple4Maze {
    public static void main(String [] args){
        Executor exec = new Executor();

        SimpleController pacman = new SimpleController();
        Controller<EnumMap<Constants.GHOST, Constants.MOVE>> ghosts = new Legacy();



        LispStatements ls = new LispStatements();
        ArrayList<String> functionSet = new ArrayList<>();
        functionSet.add("2ifblue");
        functionSet.add("4ifle");
        ArrayList<String> terminalSet = new ArrayList<>();
        terminalSet.add("distToPill");
        terminalSet.add("distToPower");
        terminalSet.add("distGhost");
        terminalSet.add("distTo2ndGhost");
        terminalSet.add("distToEdibleGhost");
        terminalSet.add("distToNonEdibleGhost");
        terminalSet.add("moveToFood");
        terminalSet.add("moveToPower");
        terminalSet.add("moveToEdibleGhost");
        terminalSet.add("moveFromNonEdibleGhost");
        terminalSet.add("moveFromPower");
        try {
            ls.setFunctionalSet(functionSet);
        }
        catch (Exception ignored){

        }
        ls.setTerminalSet(terminalSet);
        try {
            Genetic.Executor.setUp(pacman, ls, 8);
        }catch (Exception ignored){

        }

        boolean visual=true;
        String fileName="results/replay6.txt";
/*
        int bestScore = 0;
        int result = 0;
        for(int i = 0; i < 1000 ; i++) {
            result = exec.runGameTimedRecorded(pacman, ghosts, visual, fileName, bestScore);
            if(result > bestScore){
                bestScore = result;
            }
            System.out.println(result);
        }
        visual = true;
*/
        exec.replayGame(fileName,visual);
    }
}
