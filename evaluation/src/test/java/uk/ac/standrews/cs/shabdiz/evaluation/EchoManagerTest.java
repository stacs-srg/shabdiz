package uk.ac.standrews.cs.shabdiz.evaluation;

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

    @Test(timeout = 10000)
    public void testMavenBasedCold() throws Exception {

        final EchoManager manager = new EchoManager.MavenBasedCold();
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
