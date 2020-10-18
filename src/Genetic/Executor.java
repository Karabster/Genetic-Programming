package Genetic;



/**
 * This class allows the user to set up the generation that can be executed by the library as well as call for the new
 * generation to be built
 *
 * @author Nikita Kuzmin
 */

public class Executor {
    /**
     * This method starts generation of genetic programming trees
     * @param sets  Object containing terminal, functional and other methods that will be included in the scripts
     * @param statements Object containing String descriptions of the methods that can be included
     * @throws IllegalArgumentException if either the terminal statements or functional statements are empty
     */
    public static void setUp(Executable sets, LispStatements statements, int depth) throws IllegalArgumentException{
        if(statements.getFunctionalSet().isEmpty() || statements.getTerminalSet().isEmpty()){
            throw new IllegalArgumentException("Terminal or Functional set is empty");
        }
        Generation.createGeneration(InitialStructure.initialiseTrees(statements, depth), sets, statements);
        System.out.println("Generation Created");
    }

    /**
     *
     */
    public static void newGen() throws IllegalArgumentException{
        if(!Generation.isGenFinished()){
            throw new IllegalArgumentException("Generation is not finished executing");
        }


        Generation.findBest();
        Generation.storeBest();
        Generation.storeAvg();
        Generation.adjustedFitness();
        Generation.calcProbability();
        Generation.setTrees(CrossOver.generation());
        Generation.pointerReset();

    }
}
