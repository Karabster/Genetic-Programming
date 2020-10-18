package Genetic;

import java.util.ArrayList;

/**
 * This interface has to be implemented to give the object with the methods of this class to the static class Generation.
 *
 * The fitness function has to calculate the standardised fitness, which is the difference between maximum fitness and
 * raw fitness.
 *
 * Functional methods have to return an int specifying which option the program has to go for, starting with 1.
 * Terminal methods have to return a value, as these values might be used if the evaluation is called from a functional node
 *
 * @author Nikita Kuzmin
 *
 */

public interface Executable {

    int functional1(ArrayList<String> node);
    int functional2(ArrayList<String> node);
    int functional3(ArrayList<String> node);
    int functional4(ArrayList<String> node);
    int functional5(ArrayList<String> node);
    int terminal1();
    int terminal2();
    int terminal3();
    int terminal4();
    int terminal5();
    int terminal6();
    int terminal7();
    int terminal8();
    int terminal9();
    int terminal10();
    int terminal11();
    int terminal12();
    int terminal13();
    int terminal14();

}
