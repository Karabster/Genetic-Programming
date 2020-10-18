package Genetic;

/**
 * This class provides the library an easy way to store the information about the trees that it is working with
 * This includes the String LISP script of the tree, as well as its fitness and probability for the custom probability
 * function
 * @author Nikita Kuzmin
 */



public class Tree implements Comparable<Tree>{
    private String tree;
    private double fitness;
    private double probability;

    Tree(String tree){
        this.tree = tree;
        fitness = Integer.MAX_VALUE;
        probability = 0;
    }

    protected void setTree(String tree) {
        this.tree = tree;
    }

    void setFitness(double fitness) {
        this.fitness = fitness;
    }

    void setProbability(double probability) {
        this.probability = probability;
    }

    String getTree() {
        return tree;
    }

    double getFitness() {
        return fitness;
    }

    double getProbability() {
        return probability;
    }


    @Override
    public int compareTo(Tree o) {
        if(this.fitness < o.getFitness()) {
            return -1;
        } else if (this.fitness > o.getFitness()) {
            return 1;
        } else {
            return 0;
        }
    }

}
