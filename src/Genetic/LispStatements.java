package Genetic;
import java.util.ArrayList;

/**
 * This class has to implemented by the user in order for the engine to create Strings of scripts that are then executable
 *
 * Every string relates to a functional or terminal method in an Executable object.
 *
 * Thus the first String in terminal set relates to terminal1 in the Executable object.
 *
 * The functional sets ahve to named with a number prefix, to indicate number of outcomes it has i.e.:
 *
 * 3ifblue means that there are 3 outcomes from this functional method
 * @author Nikita Kuzmin
 */

public class LispStatements {

    private ArrayList<String> terminalSet;
    private ArrayList<String> functionalSet;

    /**
     * Empty constructor allows you to fill the sets afterwards
     */
    public LispStatements(){
        terminalSet = new ArrayList<String>();
        functionalSet = new ArrayList<String>();
    }

    /**
     * Constructor for class LispStatements
     * @param terminalSet   Set of strings describing terminal methods in the Executable object
     * @param functionalSet Set of strings describing functional methods in the Executable object
     */
    public LispStatements(ArrayList<String> terminalSet, ArrayList<String> functionalSet){
        this.functionalSet = functionalSet;
        this.terminalSet = terminalSet;
    }

    ArrayList<String> getTerminalSet() {
        return terminalSet;
    }

    /**
     * Allows to set the terminal set of method names after using the empty constructor
     * @param terminalSet set of method names of terminal methods
     */
    public void setTerminalSet(ArrayList<String> terminalSet) {

        this.terminalSet = terminalSet;
    }

    ArrayList<String> getFunctionalSet() {
        return functionalSet;
    }

    /**
     * Allows to set the functional set of method names after using the empty constructor
     * @param functionalSet set of method names of functional methods
     * @throws Exception Illegal argument exception if the functional method names do not have a number as their first character
     *
     */
    public void setFunctionalSet(ArrayList<String> functionalSet) throws Exception {
        for(String func : functionalSet){
            if(!Character.isDigit(func.charAt(0))){
                throw new IllegalArgumentException("Functional method names should include an integer");
            }
        }
        this.functionalSet = functionalSet;
    }
}
