package Genetic;

import java.util.ArrayList;

/**
 * An implementation of Executable interface for test purposes.
 * @author Nikita Kuzmin
 */

public class ExecutableTest implements Executable{
    private String output;

    ExecutableTest(String output){
        this.output = output;
    }

    String getOutput(){
        return this.output;
    }
    void setOutput(String output){
        this.output = output;
    }


    @Override
    public int functional1(ArrayList<String> node) {
        return 1;
    }

    @Override
    public int functional2(ArrayList<String> node) {
        return 3;
    }

    @Override
    public int functional3(ArrayList<String> node) {
        return 0;
    }

    @Override
    public int functional4(ArrayList<String> node) {
        return 0;
    }

    @Override
    public int functional5(ArrayList<String> node) {
        return 0;
    }

    @Override
    public int terminal1() {
        this.output = "t1";
        return 0;
    }

    @Override
    public int terminal2() {
        this.output = "t2";
        return 0;
    }

    @Override
    public int terminal3() {
        this.output = "t3";
        return 0;
    }

    @Override
    public int terminal4() {
        return 0;
    }

    @Override
    public int terminal5() {
        return 0;
    }

    @Override
    public int terminal6() {
        return 0;
    }

    @Override
    public int terminal7() {
        return 0;
    }

    @Override
    public int terminal8() {
        return 0;
    }

    @Override
    public int terminal9() {
        return 0;
    }

    @Override
    public int terminal10() {
        return 0;
    }

    @Override
    public int terminal11() {
        return 0;
    }

    @Override
    public int terminal12() {
        return 0;
    }

    @Override
    public int terminal13() {
        return 0;
    }

    @Override
    public int terminal14() {
        return 0;
    }
}
