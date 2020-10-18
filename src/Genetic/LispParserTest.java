package Genetic;
/**
 * This small test class provides testing of the Parser which converts the String trees into ArrayLists for execution
 * @author Nikita Kuzmin
 */

import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class LispParserTest {
    String tree;
    LispParser lp;
    ArrayList<String> list;
    @Before
        public void setUp() {
        tree = "( 2f1 ( t1 ( t1 t2 ) t2 ( t2 t1 ) )";
        lp = new LispParser("( 2f1 ( t1 ( t1 t2 ) t2 ( t2 t1 ) )");
        list = new ArrayList<>();
        list.add("(");
        list.add("2f1");
        list.add("(");
        list.add("t1");
        list.add("(");
        list.add("t1");
        list.add("t2");
        list.add(")");
        list.add("t2");
        list.add("(");
        list.add("t2");
        list.add("t1");
        list.add(")");
        list.add(")");
    }

    @Test
    public void test1(){
        assertEquals(list, lp.getArray());
        assertEquals(list, LispParser.getArray(tree));
    }
}
