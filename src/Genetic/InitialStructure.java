package Genetic;
/**
 * This class provides the library with the creation of the first generation of trees that are going to be executed
 * The class has both grow and full tree methods and takes in maximum depth from the user
 * @author Nikita Kuzmin
 */

import java.util.ArrayList;
import java.util.Random;

class InitialStructure {

    static ArrayList<String> initialiseTrees(LispStatements statements, int depth){
        ArrayList<String> trees = new ArrayList<>();
        for(int i = 2; i <= depth; i++){
            for(int j = 0; j < 100; j++) {
                trees.add(growTree(statements, i));
                trees.add(fullTree(statements, i));
            }
        }


        return trees;
    }

    private static String growTree(LispStatements statements, int depth){
        ArrayList<String> combinedSet = new ArrayList<>();
        combinedSet.addAll(statements.getFunctionalSet());

        ArrayList<String> termSet = statements.getTerminalSet();
        combinedSet.addAll(termSet);

        return genGrowTree(statements, depth - 1, combinedSet, "( ") + ")";
    }

    private static String genGrowTree(LispStatements statements, int depth, ArrayList<String> combined, String output){
        String tree = output;
        if (tree.equals("( ")){
            String func = statements.getFunctionalSet().get(getRandomInt(statements.getFunctionalSet())) + " ( ";
            tree = tree.concat(func);
            for(int i =0; i < Character.getNumericValue(func.charAt(0)); i++) {
                tree = genGrowTree(statements, depth - 1, combined, tree);
            }
            tree = tree.concat(") ");
            return tree;

        }if(depth != 0){
            String rand = combined.get(getRandomInt(combined));
            if(Character.isDigit(rand.charAt(0))){
                tree = tree.concat(rand + " ( ");

                for(int i =0; i < Character.getNumericValue(rand.charAt(0)); i++) {
                    tree = genGrowTree(statements, depth - 1, combined, tree);
                }
                tree = tree.concat(") ");

            }else{
                tree = tree.concat(rand + " ");
            }
        }else{
            String term = statements.getTerminalSet().get(getRandomInt(statements.getTerminalSet())) + " ";
            tree = tree.concat(term);
        }
        return tree;
    }

    private static int getRandomInt(ArrayList<String> array){
        Random r = new Random();
        return r.nextInt(array.size());
    }

    private static String fullTree(LispStatements statements, int depth){
        return genFullTree(statements, depth - 1, "( ") + ")";
    }

    private static String genFullTree(LispStatements statements, int depth, String output){
        String tree = output;
        if(depth != 0){
            String rand = statements.getFunctionalSet().get(getRandomInt(statements.getFunctionalSet()));
            if(Character.isDigit(rand.charAt(0))){
                tree = tree.concat(rand + " ( ");

                for(int i =0; i < Character.getNumericValue(rand.charAt(0)); i++) {
                    tree = genFullTree(statements, depth - 1, tree);
                }
                tree = tree.concat(") ");

            }else{
                tree = tree.concat(rand + " ");
            }
        }else{
            String term = statements.getTerminalSet().get(getRandomInt(statements.getTerminalSet())) + " ";
            tree = tree.concat(term);
        }
        return tree;
    }


}
