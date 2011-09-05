package uk.ac.standrews.cs.shabdiz.test.integrity;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.concurrent.ExecutionException;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import uk.ac.standrews.cs.nds.madface.HostDescriptor;
import uk.ac.standrews.cs.nds.madface.URL;
import uk.ac.standrews.cs.nds.rpc.RPCException;
import uk.ac.standrews.cs.nds.util.Diagnostic;
import uk.ac.standrews.cs.nds.util.DiagnosticLevel;
import uk.ac.standrews.cs.shabdiz.coordinator.Coordinator;
import uk.ac.standrews.cs.shabdiz.interfaces.IFutureRemoteReference;
import uk.ac.standrews.cs.shabdiz.interfaces.IJobRemote;
import uk.ac.standrews.cs.shabdiz.interfaces.IWorkerRemote;
import uk.ac.standrews.cs.shabdiz.worker.servers.WorkerNodeServer;

/**
 * Tests whether a result/exception return by a job is working.
 * 
 * @author Masih Hajiarabderkani (mh638@st-andrews.ac.uk)
 */
public class ResultReturnTest {

    private static final int[] WORKER_NETWORK_SIZE = {1};
    static final String HELLO = "hello";

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
     * Test.
     *
     * @throws Exception the exception
     */
    @Test
    public void sayHelloTest() throws Exception {

        for (final int size : WORKER_NETWORK_SIZE) {

            System.out.println(">>> Worker network size : " + size);

            final Coordinator coordinator = new Coordinator(APPLICATION_LIB_URLS);
            System.out.println(" deploying workers");
            final SortedSet<IWorkerRemote> workers = deployWorkers(coordinator, size);
            System.out.println("done deploying workers");
            for (final IWorkerRemote worker : workers) {
                final IFutureRemoteReference<String> future_reference = worker.submit(new SayHelloRemoteJob());

                Assert.assertEquals(future_reference.getRemote().get(), HELLO);

                try {
                    worker.shutdown();
                }
                catch (final RPCException e) {
                    // ignore
                }
            }

            coordinator.shutdown();
            System.out.println(">>> Done");
        }
    }

    /**
     * Test.
     *
     * @throws Exception the exception
     */
    @Test
    public void throwExeptionTest() throws Exception {

        for (final int size : WORKER_NETWORK_SIZE) {

            System.out.println(">>> Worker network size : " + size);

            final Coordinator coordinator = new Coordinator(APPLICATION_LIB_URLS);
            System.out.println(" deploying workers");
            final SortedSet<IWorkerRemote> workers = deployWorkers(coordinator, size);
            System.out.println("done deploying workers");
            for (final IWorkerRemote worker : workers) {
                final IFutureRemoteReference<String> future_reference = worker.submit(new NullPointerExceptionRemoteJob());
                try {
                    future_reference.getRemote().get();
                }
                catch (final ExecutionException e) {
                    Assert.assertTrue(e.getCause() instanceof NullPointerException && e.getCause().getMessage().equals("test"));
                }

                try {
                    worker.shutdown();
                }
                catch (final RPCException e) {
                    // ignore
                }
            }

            coordinator.shutdown();
            System.out.println(">>> Done");
        }
    }

    private static SortedSet<IWorkerRemote> deployWorkers(final Coordinator coordinator, final int size) throws Exception {

        final SortedSet<IWorkerRemote> deployed_workers = new TreeSet<IWorkerRemote>();
        for (int i = 0; i < size; i++) {

            deployed_workers.add(coordinator.deployWorkerOnHost(new HostDescriptor()));
        }

        return deployed_workers;
    }
}

final class SayHelloRemoteJob implements IJobRemote<String> {

    private static final transient long serialVersionUID = -8715065957655698996L;

    @Override
    public String call() throws Exception {

        return ResultReturnTest.HELLO;
    }
}

final class NullPointerExceptionRemoteJob implements IJobRemote<String> {

    private static final long serialVersionUID = 9089082845434872396L;

    @Override
    public String call() throws Exception {

        throw new NullPointerException("test");
    }
}
