package uk.ac.standrews.cs.shabdiz;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import uk.ac.standrews.cs.shabdiz.host.Host;
import uk.ac.standrews.cs.shabdiz.host.LocalHost;
import uk.ac.standrews.cs.shabdiz.util.Duration;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * Tests {@link ApplicationNetwork}.
 *
 * @author Masih Hajiarabderkani (mh638@st-andrews.ac.uk)
 */
public class ApplicationNetworkTest {

    public static final int AWAIT_STATE_TEST_TIMEOUT = 10000;
    private static final int TEST_NETWORK_SIZE = 10;
    public static final int NUMBER_OF_DEFAULT_SCANNERS = 4;
    private MockApplicationNetwork mock_network;

    @Before
    public void setUp() throws Exception {

        mock_network = new MockApplicationNetwork();
        mock_network.populate(TEST_NETWORK_SIZE);
    }

    @After
    public void tearDown() throws Exception {

        mock_network.shutdown();
    }

    @Test
    public void testScheduledScanners() throws Exception {

        final HashMap<Scanner, ScheduledFuture<?>> scheduled_scanners = mock_network.scheduled_scanners;
        assertEquals(NUMBER_OF_DEFAULT_SCANNERS, scheduled_scanners.size());

        assertNull(scheduled_scanners.get(mock_network.auto_kill_scanner));
        assertNull(scheduled_scanners.get(mock_network.auto_deploy_scanner));
        assertNull(scheduled_scanners.get(mock_network.auto_remove_scanner));
        assertNotNull(scheduled_scanners.get(mock_network.status_scanner));
        assertFalse(scheduled_scanners.get(mock_network.status_scanner).isDone());

        mock_network.setScanEnabled(true);
        assertNotNull(scheduled_scanners.get(mock_network.auto_kill_scanner));
        assertFalse(scheduled_scanners.get(mock_network.auto_kill_scanner).isDone());

        assertNotNull(scheduled_scanners.get(mock_network.auto_deploy_scanner));
        assertFalse(scheduled_scanners.get(mock_network.auto_deploy_scanner).isDone());

        assertNotNull(scheduled_scanners.get(mock_network.auto_remove_scanner));
        assertFalse(scheduled_scanners.get(mock_network.auto_remove_scanner).isDone());

        assertNotNull(scheduled_scanners.get(mock_network.status_scanner));
        assertFalse(scheduled_scanners.get(mock_network.status_scanner).isDone());

        mock_network.setScanEnabled(false);
        assertNotNull(scheduled_scanners.get(mock_network.auto_kill_scanner));
        assertTrue(scheduled_scanners.get(mock_network.auto_kill_scanner).isDone());

        assertNotNull(scheduled_scanners.get(mock_network.auto_deploy_scanner));
        assertTrue(scheduled_scanners.get(mock_network.auto_deploy_scanner).isDone());

        assertNotNull(scheduled_scanners.get(mock_network.auto_remove_scanner));
        assertTrue(scheduled_scanners.get(mock_network.auto_remove_scanner).isDone());

        assertNotNull(scheduled_scanners.get(mock_network.status_scanner));
        assertTrue(scheduled_scanners.get(mock_network.status_scanner).isDone());

        final TestScanner test_scanner = new TestScanner();
        assertTrue(mock_network.addScanner(test_scanner));
        assertNull(scheduled_scanners.get(test_scanner));

        test_scanner.setEnabled(false);
        assertNull(scheduled_scanners.get(test_scanner));

        test_scanner.setEnabled(true);
        assertNotNull(scheduled_scanners.get(test_scanner));
        assertFalse(scheduled_scanners.get(test_scanner).isDone());

        test_scanner.setEnabled(false);
        assertNotNull(scheduled_scanners.get(test_scanner));
        assertTrue(scheduled_scanners.get(test_scanner).isDone());

        assertFalse(mock_network.addScanner(test_scanner));
    }

    @Test
    public void testGetApplicationName() throws Exception {

        assertEquals(MockApplicationNetwork.NAME, mock_network.getApplicationName());
    }

    @Test
    public void testDeployAll() throws Exception {

        mock_network.deployAll();
        mock_network.assertAllDeployed();
    }

    @Test
    public void testDeploy() throws Exception {

        final ApplicationDescriptor descriptor = mock_network.createApplicationDescriptor();
        mock_network.deploy(descriptor);
        mock_network.manager.assertDeployed(descriptor);

        for (ApplicationDescriptor added_descriptor : mock_network) {
            assertEquals(null, added_descriptor.getApplicationReference());
        }
    }

    @Test
    public void testKillAllOnHost() throws Exception {

        final Host local_host = new LocalHost();
        final List<ApplicationDescriptor> local_application_descriptors = new ArrayList<ApplicationDescriptor>();
        for (int i = 0; i < TEST_NETWORK_SIZE; i++) {
            final ApplicationDescriptor descriptor = new ApplicationDescriptor(local_host, mock_network.manager);
            assertTrue(mock_network.add(descriptor));
            local_application_descriptors.add(descriptor);
        }

        final int expected_network_size = TEST_NETWORK_SIZE * 2;
        assertEquals(expected_network_size, mock_network.size());
        mock_network.killAllOnHost(local_host);
        assertEquals(expected_network_size, mock_network.size());

        for (final ApplicationDescriptor descriptor : mock_network) {
            if (local_application_descriptors.contains(descriptor)) {
                mock_network.manager.assertKilled(descriptor);
            }
            else {
                mock_network.manager.assertNotKilled(descriptor);
            }
        }
    }

    @Test
    public void testKill() throws Exception {

        ApplicationDescriptor descriptor = mock_network.createApplicationDescriptor();
        mock_network.kill(descriptor);
        mock_network.manager.assertKilled(descriptor);

        for (ApplicationDescriptor added_descriptor : mock_network) {
            mock_network.manager.assertNotKilled(added_descriptor);
        }
    }

    @Test(timeout = AWAIT_STATE_TEST_TIMEOUT)
    public void testAwaitAnyOfStates() throws Exception {

        ApplicationState target_state = ApplicationState.LAUNCHED;
        mock_network.manager.setProbeStateResult(target_state);

        mock_network.awaitAnyOfStates(target_state);
    }

    @Test
    public void testAddScanner() throws Exception {

        final TestScanner test_scanner = new TestScanner();

        assertTrue(mock_network.addScanner(test_scanner));
        assertTrue(mock_network.scheduled_scanners.containsKey(test_scanner));
        assertFalse(test_scanner.awaitScan(5, TimeUnit.SECONDS));
        test_scanner.setEnabled(true);
        assertTrue(test_scanner.awaitScan(5, TimeUnit.SECONDS));
        assertFalse(mock_network.addScanner(test_scanner));
    }

    @Test
    public void testRemoveScanner() throws Exception {

        final TestScanner test_scanner = new TestScanner();
        test_scanner.setEnabled(true);
        assertFalse(mock_network.removeScanner(test_scanner));
        assertTrue(mock_network.addScanner(test_scanner));

        final ScheduledFuture<?> scheduled_scanner = mock_network.scheduled_scanners.get(test_scanner);
        assertTrue(mock_network.removeScanner(test_scanner));
        assertFalse(mock_network.scheduled_scanners.containsKey(test_scanner));
        assertTrue(scheduled_scanner.isDone());
    }

    @Test
    public void testSetScanEnabled() throws Exception {

        final TestScanner test_scanner = new TestScanner();
        assertTrue(mock_network.addScanner(test_scanner));

        mock_network.setScanEnabled(true);
        assertTrue(test_scanner.isEnabled());
        assertTrue(mock_network.auto_deploy_scanner.isEnabled());
        assertTrue(mock_network.auto_kill_scanner.isEnabled());
        assertTrue(mock_network.auto_remove_scanner.isEnabled());
        assertTrue(mock_network.status_scanner.isEnabled());

        mock_network.setScanEnabled(false);
        assertFalse(test_scanner.isEnabled());
        assertFalse(mock_network.auto_deploy_scanner.isEnabled());
        assertFalse(mock_network.auto_kill_scanner.isEnabled());
        assertFalse(mock_network.auto_remove_scanner.isEnabled());
        assertFalse(mock_network.status_scanner.isEnabled());

        assertTrue(mock_network.removeScanner(test_scanner));
        mock_network.setScanEnabled(true);
        assertFalse(test_scanner.isEnabled());
        assertTrue(mock_network.auto_deploy_scanner.isEnabled());
        assertTrue(mock_network.auto_kill_scanner.isEnabled());
        assertTrue(mock_network.auto_remove_scanner.isEnabled());
        assertTrue(mock_network.status_scanner.isEnabled());

    }

    @Test
    public void testSetAutoKillEnabled() throws Exception {

        mock_network.setAutoKillEnabled(true);
        assertTrue(mock_network.auto_kill_scanner.isEnabled());

        mock_network.setAutoKillEnabled(false);
        assertFalse(mock_network.auto_kill_scanner.isEnabled());

    }

    @Test
    public void testSetAutoDeployEnabled() throws Exception {

        mock_network.setAutoDeployEnabled(true);
        assertTrue(mock_network.auto_deploy_scanner.isEnabled());

        mock_network.setAutoDeployEnabled(false);
        assertFalse(mock_network.auto_deploy_scanner.isEnabled());
    }

    @Test
    public void testSetAutoRemoveEnabled() throws Exception {

        mock_network.setAutoRemoveEnabled(true);
        assertTrue(mock_network.auto_remove_scanner.isEnabled());

        mock_network.setAutoRemoveEnabled(false);
        assertFalse(mock_network.auto_remove_scanner.isEnabled());
    }

    @Test
    public void testSetStatusScannerEnabled() throws Exception {

        mock_network.setStatusScannerEnabled(true);
        assertTrue(mock_network.status_scanner.isEnabled());

        mock_network.setStatusScannerEnabled(false);
        assertFalse(mock_network.status_scanner.isEnabled());
    }

    @Test
    public void testShutdown() throws Exception {

        mock_network.shutdown();
        for (ScheduledFuture<?> scheduled_scanner : mock_network.scheduled_scanners.values()) {
            if (scheduled_scanner != null) {
                assertTrue(scheduled_scanner.isDone());
            }
        }

        assertEquals(0, mock_network.size());
    }

    @Test
    public void testAddAndContains() throws Exception {

        ApplicationDescriptor descriptor = mock_network.createApplicationDescriptor();
        assertTrue(mock_network.add(descriptor));
        assertTrue(mock_network.application_descriptors.contains(descriptor));
        assertFalse(mock_network.add(descriptor));
    }

    @Test
    public void testRemove() throws Exception {

        ApplicationDescriptor descriptor = mock_network.createApplicationDescriptor();
        assertFalse(mock_network.remove(descriptor));
        assertFalse(mock_network.application_descriptors.contains(descriptor));
        assertTrue(mock_network.add(descriptor));
        assertTrue(mock_network.remove(descriptor));
        assertFalse(mock_network.application_descriptors.contains(descriptor));
    }

    @Test
    public void testFirst() throws Exception {

        assertEquals(mock_network.application_descriptors.first(), mock_network.first());
    }

    @Test
    public void testSize() throws Exception {

        assertEquals(TEST_NETWORK_SIZE, mock_network.size());
    }

    @Test
    public void testKillAll() throws Exception {

        mock_network.killAll();
        mock_network.assertAllKilled();
    }

    private static class TestScanner extends Scanner {

        private static final Duration CYCLE_DELAY = new Duration(1, TimeUnit.SECONDS);
        private static final Duration CYCLE_TIMEOUT = CYCLE_DELAY;
        private final CountDownLatch scan_latch = new CountDownLatch(1);

        protected TestScanner() {

            super(CYCLE_DELAY, CYCLE_TIMEOUT);
        }

        @Override
        public void scan(final ApplicationNetwork network) {

            scan_latch.countDown();
        }

        protected boolean awaitScan(final long timeout, final TimeUnit unit) throws InterruptedException {

            return scan_latch.await(timeout, unit);
        }
    }
}
