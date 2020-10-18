package Genetic;

import java.io.FileWriter;
import java.util.ArrayList;
import java.util.Collections;

public class Generation {
    private static ArrayList<Tree> trees;
    private static Tree bestTree;
    private static double bestRating;
    private static int pointer;
    private static Executable sets;
    private static LispStatements statements;


    static void createGeneration(ArrayList<String> newTrees, Executable newSets, LispStatements newStatements) {
        trees = new ArrayList<>();
        for(String s : newTrees){
            trees.add(new Tree(s));
        }
        Collections.shuffle(trees);
        bestTree = new Tree("");
        bestRating = Integer.MAX_VALUE;
        pointer = 0;
        sets = newSets;
        statements = newStatements;

    }
    public static boolean isGenFinished(){
        return pointer != trees.size();
    }

    public static void pointerUpdate() throws IllegalArgumentException{
        if(trees.get(pointer).getFitness() == Integer.MAX_VALUE){
            throw new IllegalArgumentException("The fitness for previous tree was not set");
        }
        pointer++;
    }
    static void pointerReset(){
        pointer = 0;
    }



    static void setPointer(int pointer) {
        Generation.pointer = pointer;
    }

    static void setSets(Executable sets) {
        Generation.sets = sets;
    }

    static void setStatements(LispStatements statements) {
        Generation.statements = statements;
    }

    static int getPointer() {
        return pointer;
    }

    static Executable getSets() {
        return sets;
    }

    static LispStatements getStatements() {
        return statements;
    }



    static ArrayList<Tree> getTrees() {
        return trees;
    }



    static void setTrees(ArrayList<Tree> newTrees) {
        trees = newTrees;
    }


    public static String getBestTree() {
        return bestTree.getTree();
    }

    public static double getBestRating() {
        return bestRating;
    }


    static void findBest(){
        double newBestRating = bestRating;
        int i = 0;
        int index = 0;
        for(Tree tree : trees){
            if(tree.getFitness() < bestRating){
                newBestRating = tree.getFitness();
                index = i;
            }
            i++;
        }
        if(bestRating > newBestRating){
            bestRating = newBestRating;
            bestTree = trees.get(index);
        }
    }

    static void calcProbability(){
        double sum = 0;
        double cumulative = 0;

        for(Tree tree : trees){
            sum += tree.getFitness();
        }

        for(Tree tree : trees){
            tree.setProbability(tree.getFitness()/sum);
        }

        Collections.sort(trees);

        for(Tree tree : trees){
            cumulative +=tree.getProbability();
            tree.setProbability(cumulative);
        }


    }


    static void adjustedFitness(){

        for(Tree tree : trees){
            tree.setFitness((double) 1/(1 + tree.getFitness()));
        }

    }

    public static void setFitness(double fitness){
        trees.get(pointer).setFitness(fitness);
    }

    static void storeBest(){
        try {
            FileWriter fw = new FileWriter("Results.txt");
            fw.write(bestTree.getTree() + "\n" +  bestRating + "\n\n\n\n\n");
        }catch (Exception e){
            System.out.println("Could not write to a file");
        }
    }

    static void storeAvg(){
        try {
            double avg = 0;
            for(Tree tree : trees){
                avg += tree.getFitness();
            }
            avg = avg/trees.size();
            FileWriter fw = new FileWriter("Results.txt");
            fw.write("This Generation's avg standardised fitness is: " + avg);
        }catch (Exception e){
            System.out.println("Could not write to a file");
        }
    }

}
