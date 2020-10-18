
package Genetic;



import java.util.ArrayList;
import java.util.Collections;
import java.util.Random;

/**
 *This class provides the library with creation of new generations of the decision trees
 *
 * @author Nikita Kuzmin
 */
public class CrossOver {

    protected static ArrayList<Tree> generation(){
        ArrayList<Tree> result = new ArrayList<>();
        int pointer = 0;
        Tree[] parents = new Tree[2];
        Random r = new Random();
        double randomProb;
        result.add(new Tree(Generation.getBestTree()));

        while(result.size() < Generation.getTrees().size() * 0.8) {
            for (int i = 0; i < 2; i++) {

                randomProb = (double) r.nextInt(1000) + 0;

                for (Tree tree : Generation.getTrees()) {
                    if (tree.getProbability()*1000 > randomProb) {
                        parents[i] = Generation.getTrees().get(pointer);
                        break;
                    }
                    pointer++;
                }
                pointer = 0;
            }

            Tree[] crossedParents = crossOver(parents);

            Collections.addAll(result, crossedParents);
        }
        while(result.size() < Generation.getTrees().size()) {
            randomProb = (double) r.nextInt(1000) + 0;
            for(Tree tree : Generation.getTrees()){
                if(tree.getProbability()*1000 >randomProb){
                    result.add(tree);
                    break;
                }
            }

        }



        return result;
    }

    private static Tree[] crossOver(Tree[] parents){
        Tree [] children = new Tree[2];
        ArrayList<String> father = LispParser.getArray(parents[0].getTree());
        ArrayList<String> mother = LispParser.getArray(parents[1].getTree());
        int motherNodeStart = getNode(mother);
        int fatherNodeStart = getNode(father);
        int motherNodeEnd = getEnd(mother, motherNodeStart);
        int fatherNodeEnd = getEnd(father, fatherNodeStart);
        ArrayList<String> newFather = new ArrayList<>();
        ArrayList<String> newMother = new ArrayList<>();



        if(fatherNodeStart == 0){
            for(int i = 0; i < 1; i++){
                newFather.add(father.get(i));
            }
        }else{
            for(int i = 0; i < fatherNodeStart; i++){
                newFather.add(father.get(i));
            }
        }

        if(motherNodeStart == 0){
            for(int j = 1; j< motherNodeEnd; j++){
                newFather.add(mother.get(j));
            }
        }else{
            for(int j = motherNodeStart ; j <= motherNodeEnd; j++){
                newFather.add(mother.get(j));
            }
        }

        if(fatherNodeStart == 0){
            newFather.add(father.get(fatherNodeEnd));
        }else {
            for (int k = fatherNodeEnd + 1; k < father.size(); k++) {
                newFather.add(father.get(k));
            }
        }



        if(motherNodeStart == 0){
            for(int l = 0; l < 1; l++){
                newMother.add(mother.get(l));
            }

        }else{
            for(int l = 0; l < motherNodeStart; l++){
                newMother.add(mother.get(l));
            }

        }

        if(fatherNodeStart == 0){
            for(int p = 1; p < fatherNodeEnd; p++){
                newMother.add(father.get(p));

            }
        }else{
            for(int p = fatherNodeStart; p <= fatherNodeEnd; p++){
                newMother.add(father.get(p));

            }
        }
        if(motherNodeStart == 0){
            newMother.add(mother.get(motherNodeEnd));
        }else {
            for (int m = motherNodeEnd + 1; m < mother.size(); m++) {
                newMother.add(mother.get(m));
            }
        }

        if(depth(newMother) > 17 || depth(newFather) > 17){
            System.out.println("Too long");
            children = crossOver(parents);
        }

        children[0] = new Tree(stringMaker(newFather));
        children[1] = new Tree(stringMaker(newMother));


        return children;
    }

    private static String stringMaker(ArrayList<String> input){
        String output = "";
        for(String s : input){
            output = output.concat(s + " ");
        }
        return output.substring(0, output.length() - 1);
    }



    private static int depth(ArrayList<String> tree){
        int depth = 0;
        int temp = 0;

        for(String s : tree){
            if(s.equals("(")){
                temp ++;
            }else if(s.equals(")")){
                temp --;
            }
            if(depth < temp){
                depth = temp;
            }
        }

        return depth;
    }

    private static int getEnd(ArrayList<String> tree, int start){
        int pointer = start;
        int counter = 0;
        if(tree.get(pointer + 1).equals("(")) {
            pointer += 1;
            for (int i = start + 1; i < tree.size(); i++) {
                if (tree.get(i).equals("(")) {
                    counter++;
                } else if (tree.get(i).equals(")")) {
                    counter--;
                }
                if (counter == 0) {
                    break;
                }
                pointer++;
            }
        }
        return pointer;
    }
    private static int getNode(ArrayList<String> tree){
        Random r = new Random();

        int numberOfNodes = 0;
        int pointer = 0;
        for(String s : tree){
            if(!s.equals("(") & !s.equals(")")){
                numberOfNodes ++;
            }
        }

        int nodeNumber = r.nextInt(numberOfNodes) + 1;
        numberOfNodes = 0;
        for(String s : tree){
            if(!s.equals("(") & !s.equals(")")){
                numberOfNodes ++;
            }if(numberOfNodes == nodeNumber){
                break;
            }
            pointer ++;
        }
        return pointer;
    }

}
