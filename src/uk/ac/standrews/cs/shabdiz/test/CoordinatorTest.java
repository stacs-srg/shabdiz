package uk.ac.standrews.cs.shabdiz.test;

import java.util.HashSet;
import java.util.Set;
import java.util.SortedSet;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import uk.ac.standrews.cs.nds.madface.HostDescriptor;
import uk.ac.standrews.cs.nds.madface.URL;
import uk.ac.standrews.cs.shabdiz.coordinator.Coordinator;
import uk.ac.standrews.cs.shabdiz.interfaces.IWorker;

/**
 * The Class CoordinatorTest.
 * 
 * @author Masih Hajiarabderkani (mh638@st-andrews.ac.uk)
 */
public class CoordinatorTest {

    /**
     * Sets the up the test.
     *
     * @throws Exception the exception
     */
    @Before
    public void setUp() throws Exception {

    }

    /**
     * Tears down the test.
     *
     * @throws Exception the exception
     */
    @After
    public void tearDown() throws Exception {

    }

    /**
     * Test.
     *
     * @throws Exception the exception
     */
    @Test
    public void test() throws Exception {

        final Set<URL> application_lib_urls = new HashSet<URL>();
        application_lib_urls.add(new URL("http://beast.cs.st-andrews.ac.uk:8080/hudson/job/hudson_tools/lastSuccessfulBuild/artifact/lib/junit-4.8.2.jar"));
        application_lib_urls.add(new URL("http://beast.cs.st-andrews.ac.uk:8080/hudson/job/trombone/lastSuccessfulBuild/artifact/lib/json.jar"));
        application_lib_urls.add(new URL("http://beast.cs.st-andrews.ac.uk:8080/hudson/job/trombone/lastSuccessfulBuild/artifact/lib/mindterm.jar"));

        final Coordinator coordinator = new Coordinator(application_lib_urls, false);

        coordinator.addHost(new HostDescriptor());
        coordinator.addHost(new HostDescriptor());
        coordinator.addHost(new HostDescriptor());

        final SortedSet<IWorker> deployed_workers = coordinator.deployWorkersOnHosts();

    }
}
