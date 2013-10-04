package uk.ac.standrews.cs.shabdiz;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import uk.ac.standrews.cs.shabdiz.host.Host;
import uk.ac.standrews.cs.shabdiz.host.LocalHost;
import uk.ac.standrews.cs.shabdiz.util.Duration;

/**
 * Tests {@link ApplicationNetwork}.
 *
 * @author Masih Hajiarabderkani (mh638@st-andrews.ac.uk)
 */
public class ApplicationNetworkTest {

    public static final int AWAIT_STATE_TEST_TIMEOUT = 10000;
    private static final int TEST_NETWORK_SIZE = 10;
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
    public void testGetApplicationName() throws Exception {

        Assert.assertEquals(MockApplicationNetwork.NAME, mock_network.getApplicationName());
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
            Assert.assertEquals(null, added_descriptor.getApplicationReference());
        }
    }

    @Test
    public void testKillAllOnHost() throws Exception {

        final Host local_host = new LocalHost();
        final List<ApplicationDescriptor> local_application_descriptors = new ArrayList<ApplicationDescriptor>();
        for (int i = 0; i < TEST_NETWORK_SIZE; i++) {
            final ApplicationDescriptor descriptor = new ApplicationDescriptor(local_host, mock_network.manager);
            Assert.assertTrue(mock_network.add(descriptor));
            local_application_descriptors.add(descriptor);
        }

        final int expected_network_size = TEST_NETWORK_SIZE * 2;
        Assert.assertEquals(expected_network_size, mock_network.size());
        mock_network.killAllOnHost(local_host);
        Assert.assertEquals(expected_network_size, mock_network.size());

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

    @Test
    //(timeout = AWAIT_STATE_TEST_TIMEOUT)
    public void testAwaitAnyOfStates() throws Exception {

        ApplicationState target_state = ApplicationState.LAUNCHED;
        mock_network.manager.setProbeStateResult(target_state);

        mock_network.awaitAnyOfStates(target_state);
    }

    @Test
    public void testAddScanner() throws Exception {

        final TestScanner test_scanner = new TestScanner();

        Assert.assertTrue(mock_network.addScanner(test_scanner));
        Assert.assertTrue(mock_network.scheduled_scanners.containsKey(test_scanner));
        Assert.assertFalse(test_scanner.awaitScan(5, TimeUnit.SECONDS));
        test_scanner.setEnabled(true);
        Assert.assertTrue(test_scanner.awaitScan(5, TimeUnit.SECONDS));
        Assert.assertFalse(mock_network.addScanner(test_scanner));
    }

    @Test
    public void testRemoveScanner() throws Exception {

        final TestScanner test_scanner = new TestScanner();
        test_scanner.setEnabled(true);
        Assert.assertFalse(mock_network.removeScanner(test_scanner));
        Assert.assertTrue(mock_network.addScanner(test_scanner));

        final ScheduledFuture<?> scheduled_scanner = mock_network.scheduled_scanners.get(test_scanner);
        Assert.assertTrue(mock_network.removeScanner(test_scanner));
        Assert.assertFalse(mock_network.scheduled_scanners.containsKey(test_scanner));
        Assert.assertTrue(scheduled_scanner.isDone());
    }

    @Test
    public void testSetScanEnabled() throws Exception {

        final TestScanner test_scanner = new TestScanner();
        Assert.assertTrue(mock_network.addScanner(test_scanner));

        mock_network.setScanEnabled(true);
        Assert.assertTrue(test_scanner.isEnabled());
        Assert.assertTrue(mock_network.auto_deploy_scanner.isEnabled());
        Assert.assertTrue(mock_network.auto_kill_scanner.isEnabled());
        Assert.assertTrue(mock_network.auto_remove_scanner.isEnabled());
        Assert.assertTrue(mock_network.status_scanner.isEnabled());

        mock_network.setScanEnabled(false);
        Assert.assertFalse(test_scanner.isEnabled());
        Assert.assertFalse(mock_network.auto_deploy_scanner.isEnabled());
        Assert.assertFalse(mock_network.auto_kill_scanner.isEnabled());
        Assert.assertFalse(mock_network.auto_remove_scanner.isEnabled());
        Assert.assertFalse(mock_network.status_scanner.isEnabled());

        Assert.assertTrue(mock_network.removeScanner(test_scanner));
        mock_network.setScanEnabled(true);
        Assert.assertFalse(test_scanner.isEnabled());
        Assert.assertTrue(mock_network.auto_deploy_scanner.isEnabled());
        Assert.assertTrue(mock_network.auto_kill_scanner.isEnabled());
        Assert.assertTrue(mock_network.auto_remove_scanner.isEnabled());
        Assert.assertTrue(mock_network.status_scanner.isEnabled());

    }

    @Test
    public void testSetAutoKillEnabled() throws Exception {

        mock_network.setAutoKillEnabled(true);
        Assert.assertTrue(mock_network.auto_kill_scanner.isEnabled());

        mock_network.setAutoKillEnabled(false);
        Assert.assertFalse(mock_network.auto_kill_scanner.isEnabled());

    }

    @Test
    public void testSetAutoDeployEnabled() throws Exception {

        mock_network.setAutoDeployEnabled(true);
        Assert.assertTrue(mock_network.auto_deploy_scanner.isEnabled());

        mock_network.setAutoDeployEnabled(false);
        Assert.assertFalse(mock_network.auto_deploy_scanner.isEnabled());
    }

    @Test
    public void testSetAutoRemoveEnabled() throws Exception {

        mock_network.setAutoRemoveEnabled(true);
        Assert.assertTrue(mock_network.auto_remove_scanner.isEnabled());

        mock_network.setAutoRemoveEnabled(false);
        Assert.assertFalse(mock_network.auto_remove_scanner.isEnabled());
    }

    @Test
    public void testSetStatusScannerEnabled() throws Exception {

        mock_network.setStatusScannerEnabled(true);
        Assert.assertTrue(mock_network.status_scanner.isEnabled());

        mock_network.setStatusScannerEnabled(false);
        Assert.assertFalse(mock_network.status_scanner.isEnabled());
    }

    @Test
    public void testShutdown() throws Exception {

        mock_network.shutdown();
        for (ScheduledFuture<?> scheduled_scanner : mock_network.scheduled_scanners.values()) {
            Assert.assertTrue(scheduled_scanner.isDone());
        }

        Assert.assertEquals(0, mock_network.size());
    }

    @Test
    public void testAddAndContains() throws Exception {

        ApplicationDescriptor descriptor = mock_network.createApplicationDescriptor();
        Assert.assertTrue(mock_network.add(descriptor));
        Assert.assertTrue(mock_network.application_descriptors.contains(descriptor));
        Assert.assertFalse(mock_network.add(descriptor));
    }

    @Test
    public void testRemove() throws Exception {

        ApplicationDescriptor descriptor = mock_network.createApplicationDescriptor();
        Assert.assertFalse(mock_network.remove(descriptor));
        Assert.assertFalse(mock_network.application_descriptors.contains(descriptor));
        Assert.assertTrue(mock_network.add(descriptor));
        Assert.assertTrue(mock_network.remove(descriptor));
        Assert.assertFalse(mock_network.application_descriptors.contains(descriptor));
    }

    @Test
    public void testFirst() throws Exception {

        Assert.assertEquals(mock_network.application_descriptors.first(), mock_network.first());
    }

    @Test
    public void testSize() throws Exception {

        Assert.assertEquals(TEST_NETWORK_SIZE, mock_network.size());
    }

    @Test
    public void testKillAll() throws Exception {

        mock_network.killAll();
        mock_network.assertAllKilled();
    }

    private static class TestScanner implements Scanner {

        private static final Duration CYCLE_DELAY = new Duration(1, TimeUnit.SECONDS);
        private static final Duration CYCLE_TIMEOUT = CYCLE_DELAY;
        private final CountDownLatch scan_latch = new CountDownLatch(1);
        private boolean enabled;

        @Override
        public void scan(final ApplicationNetwork network) {

            scan_latch.countDown();
        }

        @Override
        public Duration getCycleDelay() {

            return CYCLE_DELAY;
        }

        @Override
        public Duration getScanTimeout() {

            return CYCLE_TIMEOUT;
        }

        protected boolean awaitScan(final long timeout, final TimeUnit unit) throws InterruptedException {

            return scan_latch.await(timeout, unit);
        }

        @Override
        public void setEnabled(final boolean enabled) {

            this.enabled = enabled;
        }

        @Override
        public boolean isEnabled() {

            return enabled;
        }

    }
}
