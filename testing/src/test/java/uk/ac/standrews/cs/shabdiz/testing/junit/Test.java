package uk.ac.standrews.cs.shabdiz.testing.junit;

import java.lang.management.ManagementFactory;
import org.junit.runner.RunWith;

/** @author Masih Hajiarabderkani (mh638@st-andrews.ac.uk) */
//@RunWith(Parameterized.class)
@RunWith(Parallelized.class)
public class Test {

    private String a;
    private String b;

    //    public Test(String a, String b) {
    //
    //        this.a = a;
    //        this.b = b;
    //    }
    //
    //    @Parameterized.Parameters(name = "{index}, a: {0}, b: {1}")
    //    public static Collection<Object[]> data() {
    //
    //        return Combinations.generateArgumentCombinations(new Object[][]{{"a", "b", "c"}, {"1", "2", "3"}});
    //    }

    @org.junit.Test
    public void testName() throws Exception {

        System.out.println(ManagementFactory.getRuntimeMXBean().getName());
        System.out.println(a + " -> " + b);
        System.out.println();
    }
}
