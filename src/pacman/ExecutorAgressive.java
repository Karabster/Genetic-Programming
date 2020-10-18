package pacman;

import Genetic.Generation;
import Genetic.LispStatements;
import pacman.controllers.Controller;
import pacman.controllers.GeneticController;
import pacman.controllers.examples.AggressiveGhosts;
import pacman.game.Constants;

import java.util.ArrayList;
import java.util.EnumMap;

/**
 * This class was built as a representation of the same setup in the class Executor, but with Aggressive ghosts being
 * the opponent.
 * This class will start training if initiated.
 * @author Nikita Kuzmin
 */
public class ExecutorAgressive {
    public static void main(String [] args) {
        int generation = 0;
        int sum = 0;
        Executor exec = new Executor();

        GeneticController pacman = new GeneticController();
        Controller<EnumMap<Constants.GHOST, Constants.MOVE>> ghosts = new AggressiveGhosts();



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

                for (int i = 0; i < 10; i++) {
                    int delay = 0;
                    boolean visual = false;
                    sum += exec.runGame(pacman, ghosts, visual, delay);
                }

                Generation.setFitness(50000 - (sum / 10));
                System.out.println(sum / 10);

                sum = 0;

                Generation.pointerUpdate();
            }
            System.out.println("Generation " + generation);
            Genetic.Executor.newGen();
            System.out.println(Generation.getBestRating());
            generation++;
        }


        System.out.println(Generation.getBestTree());
        System.out.println(Generation.getBestRating());
    }

}
