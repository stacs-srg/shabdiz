package uk.ac.standrews.cs.nds.madface;

import java.io.IOException;
import java.util.concurrent.Semaphore;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import uk.ac.standrews.cs.nds.madface.interfaces.IAttributesCallback;
import uk.ac.standrews.cs.nds.madface.interfaces.IHostStatusCallback;

/**
 * Tests requiring authentication, not intended to be run automatically.
 *
 * @author Graham Kirby (graham.kirby@st-andrews.ac.uk)
 */
public class MadfaceManagerRemoteTests extends MadfaceManagerTestBase {

    private static final long TEST_TIMEOUT = 30000;
    private static HostDescriptor host_descriptor;

    /**
     * Initializes a host descriptor interactively.
     * @throws IOException if an error occurs
     */
    @BeforeClass
    public static void setupHost() throws IOException {

        host_descriptor = new HostDescriptor(true);
    }

    @Before
    @Override
    public void setup() throws Exception {

        super.setup();
        configureManager();
    }

    /**
     * Adds an invalid host and tests that the host state is eventually INVALID.
     * @throws Exception if the test fails
     */
    @Test(timeout = TEST_TIMEOUT)
    public void addInvalidHost() throws Exception {

        final HostDescriptor host_descriptor = new HostDescriptor("abc.def.ghi");
        manager.add(host_descriptor);

        manager.waitForAllToReachState(HostState.INVALID);
    }

    /**
     * Adds a valid host with invalid credentials and tests that the host state is eventually NO_AUTH.
     * @throws Exception if the test fails
     */
    @Test(timeout = TEST_TIMEOUT)
    public void addHostWithInvalidCredentials() throws Exception {

        final HostDescriptor host_descriptor = new HostDescriptor(true);
        host_descriptor.credentials(new Credentials().user("dummy").password("dummy"));
        manager.add(host_descriptor);

        manager.waitForAllToReachState(HostState.NO_AUTH);
    }

    /**
     * Adds a valid host with valid credentials and tests that the host state is eventually AUTH.
     * @throws Exception if the test fails
     */
    @Test(timeout = TEST_TIMEOUT)
    public void addHostWithValidCredentials() throws Exception {

        manager.add(host_descriptor);

        manager.waitForAllToReachState(HostState.AUTH);
    }

    /**
     * Deploys to a valid host and tests that the host state is eventually RUNNING.
     * @throws Exception if the test fails
     */
    @Test(timeout = TEST_TIMEOUT)
    public void deploy() throws Exception {

        manager.add(host_descriptor);
        manager.deploy(host_descriptor);

        manager.waitForAllToReachState(HostState.RUNNING);
    }

    /**
     * Adds a valid host, sets auto-deploy and tests that the host state is eventually RUNNING.
     * @throws Exception if the test fails
     */
    @Test(timeout = TEST_TIMEOUT)
    public void autoDeploy() throws Exception {

        manager.add(host_descriptor);
        manager.setAutoDeploy(true);

        manager.waitForAllToReachState(HostState.RUNNING);
    }

    /**
     * Deploys to a valid host, then kills and tests that the host state is eventually AUTH.
     * @throws Exception if the test fails
     */
    @Test(timeout = TEST_TIMEOUT)
    public void kill() throws Exception {

        manager.add(host_descriptor);

        manager.deploy(host_descriptor);
        manager.waitForAllToReachState(HostState.RUNNING);

        manager.kill(host_descriptor, false);
        manager.waitForAllToReachState(HostState.AUTH);
    }

    /**
     * Deploys to a valid host, sets auto-kill and tests that the host state is eventually AUTH.
     * @throws Exception if the test fails
     */
    @Test(timeout = TEST_TIMEOUT)
    public void autoKill() throws Exception {

        manager.add(host_descriptor);

        manager.deploy(host_descriptor);
        manager.waitForAllToReachState(HostState.RUNNING);

        manager.setAutoKill(true);
        manager.waitForAllToReachState(HostState.AUTH);
    }

    /**
     * Deploys to a valid host, then kills and tests that the host state is eventually not RUNNING.
     * @throws Exception if the test fails
     */
    @Test(timeout = TEST_TIMEOUT)
    public void waitForNotRunning() throws Exception {

        manager.add(host_descriptor);

        manager.deploy(host_descriptor);
        manager.waitForAllToReachState(HostState.RUNNING);

        manager.kill(host_descriptor, false);
        manager.waitForAllToReachStateThatIsNot(HostState.RUNNING);
    }

    /**
     * Deploys to a valid host, then tests that a status callback is eventually received.
     * @throws Exception if the test fails
     */
    @Test(timeout = TEST_TIMEOUT)
    public void statusCallback() throws Exception {

        final Semaphore sync = new Semaphore(0);
        final String host_name = host_descriptor.getHost();

        manager.add(host_descriptor);
        manager.addHostStatusCallback(new IHostStatusCallback() {

            @Override
            public void hostStatusChange(final HostDescriptor host_descriptor, final HostState original_state) {

                if (host_descriptor.getHost().equals(host_name) && host_descriptor.getHostState() == HostState.RUNNING) {
                    sync.release();
                }
            }
        });

        manager.deploy(host_descriptor);
        sync.acquire();
    }

    /**
     * Deploys to a valid host, then tests that an attribute callback is eventually received.
     * @throws Exception if the test fails
     */
    @Test(timeout = TEST_TIMEOUT)
    public void attributeCallback() throws Exception {

        final Semaphore sync = new Semaphore(0);
        final String host_name = host_descriptor.getHost();

        manager.add(host_descriptor);
        manager.addAttributesCallback(new IAttributesCallback() {

            @Override
            public void attributesChange(final HostDescriptor host_descriptor) {

                if (host_descriptor.getHost().equals(host_name) && host_descriptor.getAttributes().containsKey(TestAppManager.ATTRIBUTE_NAME)) {
                    sync.release();
                }
            }
        });

        manager.deploy(host_descriptor);
        sync.acquire();
    }
}
