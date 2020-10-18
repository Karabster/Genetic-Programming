package Genetic;


import java.util.ArrayList;
import java.util.Random;

/**
 * This class provides the user with executing decision trees created by the library
 * main functionality includes executing trees stored in the static class Generation
 * as well as executing a single tree, which should be inputted as a String by the user
 * Also this class allows the user to evaluate the children nodes of any functional node by using evaluate method
 *
 * @author Nikita Kuzmin
 */

public class LispExecutor {

    /**
     * This method exevutes the tree from a static class Generation, which is pointed at the Generation's pointer parameter
     * This method runs through the decision tree only once
     */
     public static void execute(){
        ArrayList<String> input = LispParser.getArray(Generation.getTrees().get(Generation.getPointer()).getTree());
        LispStatements statements = Generation.getStatements();

        int pointer = 1;
            while(true) {
                if (statements.getFunctionalSet().contains(input.get(pointer))) {
                    pointer = functionCheck(input.get(pointer), Generation.getSets(), statements, pointer, input);
                } else if (statements.getTerminalSet().contains(input.get(pointer))) {
                    terminalCheck(input.get(pointer),Generation.getSets(),statements);
                    break;
                }else{
                    break;
                }
            }
     }

    /**
     * This class allows the user to run their own trees
     * Generation class should still have information about the method names and the Executable class in order for this
     * method to operate
     * @param tree String representation of the tree to be run
     */
    public static void executeBest(String tree){
        ArrayList<String> input = LispParser.getArray(tree);
        LispStatements statements = Generation.getStatements();

        int pointer = 1;
        while(true) {
            if (statements.getFunctionalSet().contains(input.get(pointer))) {
                pointer = functionCheck(input.get(pointer), Generation.getSets(), statements, pointer, input);
            } else if (statements.getTerminalSet().contains(input.get(pointer))) {
                terminalCheck(input.get(pointer),Generation.getSets(),statements);
                break;
            }else{
                break;
            }
        }
    }

    /**
     * This class allows the user to evaluate children nodes from within functional method
     * @param node1 the 1st node for the comparison
     * @param node2 the 2nd node from the comparison
     * @param parentNode ArrayList representing the parent node analysed. This node is given to every functional method
     * @return returns 1 if the 1st node is higher in value and 0 otherwise. If both nodes are the same the decision returned is random
     */
     public static int evaluate(int node1, int node2, ArrayList<String> parentNode){
         int result;
         int node1Score = 0;
         int node2Score = 0;
         int node1pointer = 2;
         int node2pointer = 2;
         Random r = new Random();
         LispStatements statements = Generation.getStatements();
         if(node1 == 1){
             node1pointer = 2;
         }else{
             for(int i = 1; i < node1; i++){
                 node1pointer = getNext(parentNode, node1pointer);
             }
         }

         if(node2 == 1){
             node2pointer = 2;
         }else{
             for(int j = 1; j < node2; j++){
                 node2pointer = getNext(parentNode, node2pointer);
             }
         }



         while(true) {
             if (statements.getFunctionalSet().contains(parentNode.get(node1pointer))) {
                 node1pointer = functionCheck(parentNode.get(node1pointer), Generation.getSets(), statements, node1pointer, parentNode);
             } else if (statements.getTerminalSet().contains(parentNode.get(node1pointer))) {
                 node1Score = terminalCheck(parentNode.get(node1pointer),Generation.getSets(),statements);
                 break;
             }else{
                 break;
             }
         }
         while(true) {
             if (statements.getFunctionalSet().contains(parentNode.get(node2pointer))) {
                 node2pointer = functionCheck(parentNode.get(node2pointer), Generation.getSets(), statements, node2pointer, parentNode);
             } else if (statements.getTerminalSet().contains(parentNode.get(node2pointer))) {
                 node2Score = terminalCheck(parentNode.get(node2pointer),Generation.getSets(),statements);
                 break;
             }else{
                 break;
             }
         }

         if(node1Score < node2Score){
             result = 0;
         }else if(node1Score > node2Score){
             result = 1;
         }else {
             result = r.nextInt(2);
         }
         return result;
     }



    private static int functionCheck(String input,Executable sets, LispStatements statements, int pointer,
                                     ArrayList<String> array){
        int pointerUpdate = 0;
        int functionalStatement = 0;
        int currentPointer = pointer + 2;
        for(String statement : statements.getFunctionalSet()){
            if(input.equals(statement)){
                break;
            }

            functionalStatement += 1;
        }

        switch (functionalStatement){
            case 0:
                pointerUpdate += sets.functional1(getNode(pointer, array));
                break;
            case 1:
                pointerUpdate += sets.functional2(getNode(pointer, array));
                break;
            case 2:
                pointerUpdate += sets.functional3(getNode(pointer, array));
                break;
            case 3:
                pointerUpdate += sets.functional4(getNode(pointer, array));
                break;
            case 4:
                pointerUpdate += sets.functional5(getNode(pointer, array));
                break;
        }
        for(int i = 1; i < pointerUpdate; i++){
            currentPointer = getNext(array, currentPointer);
        }
        pointerUpdate = currentPointer;
        return pointerUpdate;
    }

    private static int getNext(ArrayList<String> input, int pointer){
        int index = pointer;
        int equaliser = 0;
        if(input.get(index + 1).equals("(")){
            equaliser += 1;
            for(int i = pointer + 2; i < input.size(); i++ ){
                if(equaliser == 0){
                    index = i;
                    break;
                }
                else if(input.get(i).equals("(")){
                    equaliser +=1;
                }else if(input.get(i).equals(")")){
                    equaliser -=1;
                    if(equaliser == 0){
                        index = i;
                        break;
                    }
                }
            }
        }else{
            index += 1;
        }
        return index;
    }

    private static ArrayList<String> getNode(int pointer, ArrayList<String> array){
         ArrayList<String> node = new ArrayList<>();
         for(int i = pointer; i <= getNext(array, pointer); i++){
             node.add(array.get(i));
         }
         return node;
    }

    private static int terminalCheck(String input, Executable sets, LispStatements statements){
         int result = 0;
        int terminalStatement = 0;
        for(String statement : statements.getTerminalSet()){
            if(input.equals(statement)){
                break;
            }
            terminalStatement +=1;
        }
        switch (terminalStatement){
            case 0:
                result = sets.terminal1();
                break;
            case 1:
                result = sets.terminal2();
                break;
            case 2:
                result = sets.terminal3();
                break;
            case 3:
                result = sets.terminal4();
                break;
            case 4:
                result =  sets.terminal5();
                break;
            case 5:
                result =  sets.terminal6();
                break;
            case 6:
                result =  sets.terminal7();
                break;
            case 7:
                result =  sets.terminal8();
                break;
            case 8:
                result =  sets.terminal9();
                break;
            case 9:
                result =  sets.terminal10();
                break;
            case 10:
                result =  sets.terminal11();
                break;
            case 11:
                result =  sets.terminal12();
                break;
            case 12:
                result =  sets.terminal13();
                break;
            case 13:
                result =  sets.terminal14();
                break;
        }
        return result;

    }


}
