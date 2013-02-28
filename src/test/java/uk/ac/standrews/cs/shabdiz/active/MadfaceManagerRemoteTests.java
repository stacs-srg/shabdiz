package uk.ac.standrews.cs.shabdiz.active;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.IOException;
import java.net.UnknownHostException;
import java.util.concurrent.Semaphore;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;

import uk.ac.standrews.cs.shabdiz.api.State;
import uk.ac.standrews.cs.shabdiz.credentials.SSHPasswordCredential;
import uk.ac.standrews.cs.shabdiz.zold.HostDescriptor;


/**
 * Tests requiring authentication, not intended to be run automatically.
 * 
 * @author Graham Kirby (graham.kirby@st-andrews.ac.uk)
 */
public class MadfaceManagerRemoteTests extends MadfaceManagerTestBase {

    private static final long TEST_TIMEOUT = 100000;
    private static HostDescriptor host_descriptor;

    /**
     * Initializes a host descriptor interactively.
     * 
     * @throws IOException if an error occurs
     */
    @BeforeClass
    public static void setupHost() throws IOException {

        host_descriptor = new HostDescriptor(true);
        host_descriptor.port(55123);
    }

    @Before
    @Override
    public void setup() throws Exception {

        super.setup();
        configureManager();
    }

    /**
     * Adds an invalid host and tests that the host state is eventually INVALID.
     * 
     * @throws Exception if the test fails
     */
    @Test(expected = UnknownHostException.class, timeout = TEST_TIMEOUT)
    public void addInvalidHost() throws Exception {

        HostDescriptor host_descriptor = null;
        try {
            host_descriptor = new HostDescriptor("abc.def.ghi", new SSHPasswordCredential("dummy", "dummy".toCharArray()));
        }
        finally {
            if (host_descriptor != null) {
                host_descriptor.shutdown();
            }
        }
    }

    /**
     * Adds a valid host with invalid credentials and tests that the host state is eventually NO_AUTH.
     * 
     * @throws Exception if the test fails
     */
    @Test(timeout = TEST_TIMEOUT)
    public void addHostWithInvalidCredentials() throws Exception {

        final HostDescriptor host_descriptor = new HostDescriptor("localhost", new SSHPasswordCredential("dummy", "dummy".toCharArray()));
        host_descriptor.port(55123);
        manager.add(host_descriptor);
        manager.waitForAllToReachState(State.NO_AUTH);
    }

    /**
     * Adds a valid host with valid credentials and tests that the host state is eventually AUTH.
     * 
     * @throws Exception if the test fails
     */
    @Test(timeout = TEST_TIMEOUT)
    public void addHostWithValidCredentials() throws Exception {

        manager.add(host_descriptor);

        manager.waitForAllToReachState(State.AUTH);
    }

    /**
     * Deploys to a valid host and tests that the host state is eventually RUNNING.
     * 
     * @throws Exception if the test fails
     */
    @Test(timeout = TEST_TIMEOUT)
    public void deploy() throws Exception {

        manager.add(host_descriptor);
        manager.deploy(host_descriptor);

        manager.waitForAllToReachState(State.RUNNING);
    }

    /**
     * Adds a valid host, sets auto-deploy and tests that the host state is eventually RUNNING.
     * 
     * @throws Exception if the test fails
     */
    @Test(timeout = TEST_TIMEOUT)
    public void autoDeploy() throws Exception {

        manager.add(host_descriptor);
        manager.setAutoDeploy(true);

        manager.waitForAllToReachState(State.RUNNING);
    }

    /**
     * Deploys to a valid host, then kills and tests that the host state is eventually AUTH.
     * 
     * @throws Exception if the test fails
     */
    @Test(timeout = TEST_TIMEOUT)
    public void kill() throws Exception {

        manager.add(host_descriptor);

        manager.deploy(host_descriptor);
        manager.waitForAllToReachState(State.RUNNING);

        manager.kill(host_descriptor, false);
        manager.waitForAllToReachState(State.AUTH);
    }

    /**
     * Deploys to a valid host, sets auto-kill and tests that the host state is eventually AUTH.
     * 
     * @throws Exception if the test fails
     */
    @Test(timeout = TEST_TIMEOUT)
    public void autoKill() throws Exception {

        manager.add(host_descriptor);

        manager.deploy(host_descriptor);
        manager.waitForAllToReachState(State.RUNNING);

        manager.setAutoKill(true);
        manager.waitForAllToReachState(State.AUTH);
    }

    /**
     * Deploys to a valid host, then kills and tests that the host state is eventually not RUNNING.
     * 
     * @throws Exception if the test fails
     */
    @Test(timeout = TEST_TIMEOUT)
    public void waitForNotRunning() throws Exception {

        manager.add(host_descriptor);

        manager.deploy(host_descriptor);
        manager.waitForAllToReachState(State.RUNNING);

        manager.kill(host_descriptor, false);
        manager.waitForAllToReachStateThatIsNot(State.RUNNING);
    }

    /**
     * Deploys to a valid host, then tests that a status callback is eventually received.
     * 
     * @throws Exception if the test fails
     */
    @Test(timeout = TEST_TIMEOUT)
    public void statusCallback() throws Exception {

        final Semaphore sync = new Semaphore(0);
        final String host_name = host_descriptor.getHost();

        manager.add(host_descriptor);
        host_descriptor.addPropertyChangeListener(HostDescriptor.HOST_STATE_PROPERTY_NAME, new PropertyChangeListener() {

            @Override
            public void propertyChange(final PropertyChangeEvent evt) {

                final Object source = evt.getSource();
                if (HostDescriptor.class.isInstance(source)) {
                    final HostDescriptor host_descriptor = HostDescriptor.class.cast(source);
                    if (host_descriptor.getHost().equals(host_name) && host_descriptor.getHostState() == State.RUNNING) {
                        sync.release();
                    }
                }
            }
        });

        manager.deploy(host_descriptor);
        sync.acquire();
    }

    /**
     * Deploys to a valid host, then tests that an attribute callback is eventually received.
     * 
     * @throws Exception if the test fails
     */
    @Test(timeout = TEST_TIMEOUT)
    @Ignore
    public void attributeCallback() throws Exception {

        final Semaphore sync = new Semaphore(0);
        final String host_name = host_descriptor.getHost();

        //        manager.add(host_descriptor);
        //        manager.addAttributesCallback(new AttributesCallback() {
        //
        //            @Override
        //            public void attributesChange(final HostDescriptor host_descriptor) {
        //
        //                if (host_descriptor.getHost().equals(host_name) && host_descriptor.getAttributes().containsKey(TestAppManager.ATTRIBUTE_NAME)) {
        //                    sync.release();
        //                }
        //            }
        //        });
        //
        //        manager.deploy(host_descriptor);
        //        sync.acquire();
    }
}
