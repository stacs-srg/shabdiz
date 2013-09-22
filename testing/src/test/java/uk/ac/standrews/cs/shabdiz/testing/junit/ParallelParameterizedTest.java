package uk.ac.standrews.cs.shabdiz.testing.junit;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import uk.ac.standrews.cs.shabdiz.host.Host;
import uk.ac.standrews.cs.shabdiz.host.LocalHost;
import uk.ac.standrews.cs.shabdiz.util.Combinations;

import static org.junit.Assert.assertNotNull;

/** @author Masih Hajiarabderkani (mh638@st-andrews.ac.uk) */
@RunWith(ParallelParameterized.class)
//@RunWith(Parameterized.class)
@ParallelParameterized.Parallelization(threadCount = 4, hostProvider = "local")
public class ParallelParameterizedTest {

    private final String a;
    private final String b;

    public ParallelParameterizedTest(String a, String b) {

        this.a = a;
        this.b = b;
    }

    @Parameterized.Parameters(name = "{index}, a: {0}, b: {1}")
    public static Collection<Object[]> data() {

        return Combinations.generateArgumentCombinations(new Object[][]{{"a", "b"}, {"1", "2"}});
    }

    @ParallelParameterized.HostProvider(name = "local")
    public static Collection<Host> getHosts() throws IOException {

        List<Host> hosts = new ArrayList<Host>();
        final LocalHost localHost = new LocalHost();
        hosts.add(localHost);
        return hosts;

    }

    @Test
    public void testArgs() throws Exception {

        assertNotNull(a);
        assertNotNull(b);
    }

    @Test
    public void testArgs2() throws Exception {

        assertNotNull(a);
        assertNotNull(b);
    }
}
