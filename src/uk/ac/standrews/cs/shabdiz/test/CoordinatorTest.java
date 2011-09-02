package uk.ac.standrews.cs.shabdiz.test;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;
import java.util.SortedSet;

import junit.framework.Assert;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import uk.ac.standrews.cs.nds.madface.HostDescriptor;
import uk.ac.standrews.cs.nds.madface.URL;
import uk.ac.standrews.cs.nds.util.Diagnostic;
import uk.ac.standrews.cs.nds.util.DiagnosticLevel;
import uk.ac.standrews.cs.shabdiz.coordinator.Coordinator;
import uk.ac.standrews.cs.shabdiz.interfaces.IFutureRemoteReference;
import uk.ac.standrews.cs.shabdiz.interfaces.IRemoteJob;
import uk.ac.standrews.cs.shabdiz.interfaces.IWorker;
import uk.ac.standrews.cs.shabdiz.worker.servers.WorkerNodeServer;

/**
 * The Class CoordinatorTest.
 * 
 * @author Masih Hajiarabderkani (mh638@st-andrews.ac.uk)
 */
public class CoordinatorTest {

    private static final int[] WORKER_NETWORK_SIZE = {1, 2, 3, 4, 5, 6, 7, 8, 9};

    /** The Constant HELLO. */
    public static final String HELLO = "hello";
    private static final Set<URL> APPLICATION_LIB_URLS = new HashSet<URL>();
    static {
        try {
            APPLICATION_LIB_URLS.add(new URL("http://beast.cs.st-andrews.ac.uk:8080/hudson/job/hudson_tools/lastSuccessfulBuild/artifact/lib/junit-4.8.2.jar"));
            APPLICATION_LIB_URLS.add(new URL("http://beast.cs.st-andrews.ac.uk:8080/hudson/job/trombone/lastSuccessfulBuild/artifact/lib/json.jar"));
            APPLICATION_LIB_URLS.add(new URL("http://beast.cs.st-andrews.ac.uk:8080/hudson/job/trombone/lastSuccessfulBuild/artifact/lib/mindterm.jar"));
        }
        catch (final IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Sets the up the test.
     *
     * @throws Exception the exception
     */
    @Before
    public void setUp() throws Exception {

        Diagnostic.setLevel(DiagnosticLevel.NONE);

        // Kill any lingering Shabdiz Worker processes.
        final HostDescriptor local_host_descriptor = new HostDescriptor();
        local_host_descriptor.getProcessManager().killMatchingProcesses(WorkerNodeServer.class.getSimpleName());
        local_host_descriptor.shutdown();
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

        for (final int size : WORKER_NETWORK_SIZE) {

            System.out.println(">>> Worker network size : " + size);

            final Coordinator coordinator = new Coordinator(APPLICATION_LIB_URLS);
            System.out.println(" deploying workers");
            final SortedSet<IWorker> workers = deployWorkers(coordinator, size);
            System.out.println("done deploying");
            for (final IWorker worker : workers) {
                final IFutureRemoteReference<String> future_reference = worker.submit(new IRemoteJobImplementation());

                Assert.assertEquals(future_reference.getRemote().get(), HELLO);

                worker.shutdown();
            }

            coordinator.shutdown();
            System.out.println(">>> Done");
        }
    }

    private static SortedSet<IWorker> deployWorkers(final Coordinator coordinator, final int size) throws Exception {

        for (int i = 0; i < size; i++) {

            coordinator.addHost(new HostDescriptor());
        }

        return coordinator.deployWorkersOnHosts();
    }
}

final class IRemoteJobImplementation implements IRemoteJob<String> {

    private static final transient long serialVersionUID = -8715065957655698996L;

    @Override
    public String call() throws Exception {

        return CoordinatorTest.HELLO;
    }
}
