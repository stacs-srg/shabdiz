package uk.ac.standrews.cs.shabdiz.evaluation;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import uk.ac.standrews.cs.shabdiz.ApplicationDescriptor;
import uk.ac.standrews.cs.shabdiz.ApplicationNetwork;
import uk.ac.standrews.cs.shabdiz.ApplicationState;
import uk.ac.standrews.cs.shabdiz.host.Host;
import uk.ac.standrews.cs.shabdiz.host.LocalHost;
import uk.ac.standrews.cs.test.category.Ignore;

/** @author Masih Hajiarabderkani (mh638@st-andrews.ac.uk) */
@Category(Ignore.class)
public class EchoManagerTest {

    private EchoManager manager;

    @Before
    public void setUp() throws Exception {

        manager = new EchoManager.MavenBasedCold();
    }

    @After
    public void tearDown() throws Exception {

    }

    @Test
    public void testMavenBasedCold() throws Exception {

        Host host = new LocalHost();

        ApplicationDescriptor descriptor = new ApplicationDescriptor(host, manager);

        ApplicationNetwork network = new ApplicationNetwork("test");
        network.add(descriptor);

        manager.configure(network);

        network.deployAll();
        network.awaitAnyOfStates(ApplicationState.RUNNING);
        network.killAll();

    }
}
