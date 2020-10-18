package pacman;

import Genetic.Generation;
import Genetic.LispStatements;
import pacman.controllers.Controller;
import pacman.controllers.SimpleController;
import pacman.controllers.examples.Legacy;
import pacman.game.Constants;

import java.util.ArrayList;
import java.util.EnumMap;

/**
 * This executor provides for the showcase of training of a simple controller against Legacy ghosts in a 4 maze game
 * @author Nikita Kuzmin
 */
public class ExecutorOne {
    public static void main(String [] args){
        int generation = 0;
        int sum = 0;
        Executor exec = new Executor();

        SimpleController pacman = new SimpleController();
        Controller<EnumMap<Constants.GHOST, Constants.MOVE>> ghosts = new Legacy();

		/*
		int numTrials=1;
		exec.runExperiment(pacman, ghosts, numTrials);
		*/
		/*
		int delay = 18;
		boolean visual = true;
		exec.runGame(pacman, ghosts, visual, delay);
		*/

        //Generation.pointerUpdate();

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


        //To run this code, the controller should call LispExecutor.executeBest(), in order to run just one specific tree
/*
        int delay = 18;
        boolean visual = true;
        sum = exec.runGame(pacman, ghosts,visual, delay);
        System.out.println(sum);
*/

    }
}
