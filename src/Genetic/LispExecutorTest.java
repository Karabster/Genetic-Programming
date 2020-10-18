package Genetic;
/**
 * This class provides niche case testing of the library execution mechanism
 * Most of the test rely on the mistakes that potential user can make, thus the error checking mechanisms are tested
 * @author Nikita Kuzmin
 */

import org.junit.Before;
import org.junit.Test;
import java.util.ArrayList;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

public class LispExecutorTest {
    private ExecutableTest exec;
    private LispStatements ls;
    private Tree tree;
    private ArrayList<Tree> listTree;
    private ArrayList<String> fs, ts, fs1;

    @Before
    public void setUp(){
        exec = new ExecutableTest("");
        tree = new Tree("( 2f1 ( 3f2 ( t1 t2 t3 ) 2f1 ( t2 t1 ) )");
        listTree = new ArrayList<>();
        listTree.add(tree);
        Generation.setTrees(listTree);
        Generation.setPointer(0);
        ls = new LispStatements();
        fs = new ArrayList<>();
        ts = new ArrayList<>();
        fs1 = new ArrayList<>();
        fs.add("2f1");
        fs.add("3f2");
        ts.add("t1");
        ts.add("t2");
        ts.add("t3");
        fs1.add("2f1");
        fs1.add("f2");

        ls.setTerminalSet(ts);
        try {
            ls.setFunctionalSet(fs);
        }catch (Exception e){
            System.out.println("fail");
        }
        Generation.setStatements(ls);
        Generation.setSets(exec);

}

    @Test
    public void exceptionTest(){


        try {
            ls.setFunctionalSet(fs1);
            fail("Expected an IllegalArgumentException to be thrown");
        } catch (IllegalArgumentException anIllegalArgumentException) {
            assertEquals(anIllegalArgumentException.getMessage(),"Functional method names should include an integer");
        } catch (Exception e) {
            e.printStackTrace();
        }
        try {
            Executor.newGen();

        } catch (IllegalArgumentException anIllegalArgumentException) {
            assertEquals(anIllegalArgumentException.getMessage(),"Generation is not finished executing");
        } catch (Exception e) {
            e.printStackTrace();
        }


    }

    @Test
    public void exceptionTest1(){


        try {
            Executor.setUp(exec,ls, 8);

        } catch (IllegalArgumentException anIllegalArgumentException) {
            assertEquals(anIllegalArgumentException.getMessage(),"Terminal or Functional set is empty");
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    @Test
    public void test1(){

        LispExecutor.execute();
        boolean expected = Generation.isGenFinished();
        assertEquals(true, expected);
        assertEquals("t3", exec.getOutput());

        try {
            Generation.pointerUpdate();
            fail("Expected an IllegalArgumentException to be thrown");
        } catch (IllegalArgumentException anIllegalArgumentException) {
            assertEquals(anIllegalArgumentException.getMessage(),"The fitness for previous tree was not set");
        } catch (Exception e) {
            e.printStackTrace();
        }
        Generation.setFitness(20.0);
        Generation.pointerUpdate();
        expected = Generation.isGenFinished();
        assertEquals(false, expected);
        exec.setOutput("");
        LispExecutor.executeBest(tree.getTree());
        assertEquals("t3", exec.getOutput());
    }
}
