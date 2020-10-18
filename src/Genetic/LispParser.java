package Genetic;

import java.util.ArrayList;
import java.util.Arrays;

/**
 * This class allow the user to turn String scripts into ArrayList, that can be executed by LispExecutor class
 * @author Nikita Kuzmin
 */

class LispParser {
    private String input;

    protected LispParser(String input){
        this.input = input;
    }

    protected String getInput() {
        return input;
    }

    protected ArrayList<String> getArray(){
        String[] splittedArray = getInput().split(" ");
        ArrayList<String> methodArray = new ArrayList<>(Arrays.asList(splittedArray));
        return methodArray;
    }

    /**
     * This method turns Lisp String output, form the genetic programming engine into an ArrayList that can be executed
     * @param input String created by the genetic programming engine
     * @return Arraylist that can be executed by the LispExecutor
     */
    static ArrayList<String> getArray(String input){
        String[] splittedArray = input.split(" ");
        ArrayList<String> methodArray = new ArrayList<>(Arrays.asList(splittedArray));
        return methodArray;
    }

}
