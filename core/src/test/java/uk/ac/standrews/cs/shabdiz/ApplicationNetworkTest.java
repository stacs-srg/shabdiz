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
import uk.ac.standrews.cs.shabdiz.util.AttributeKey;
import uk.ac.standrews.cs.shabdiz.util.Duration;

/**
 * Tests {@link ApplicationNetwork}.
 *
 * @author Masih Hajiarabderkani (mh638@st-andrews.ac.uk)
 */
public class ApplicationNetworkTest {

    public static final int AWAIT_STATE_TEST_TIMEOUT = 10000;
    private static final String TEST_APPLICATION_NAME = "Test Application";
    private static final int TEST_NETWORK_SIZE = 10;
    private static final AttributeKey<Boolean> KILLED_ATTRIBUTE = new AttributeKey<Boolean>();
    private static final Object MOCK_APPLICATION_REFERENCE = new Object();
    private static final TestApplicationManager TEST_APPLICATION_MANAGER = new TestApplicationManager();
    private ApplicationNetwork network;

    @Before
    public void setUp() throws Exception {

        network = new ApplicationNetwork(TEST_APPLICATION_NAME);
        for (int i = 0; i < TEST_NETWORK_SIZE; i++) {
            network.add(createTestApplicationDescriptor());
        }
    }

    private static ApplicationDescriptor createTestApplicationDescriptor() {

        return new ApplicationDescriptor(TEST_APPLICATION_MANAGER);
    }

    @After
    public void tearDown() throws Exception {

        network.shutdown();
    }

    @Test
    public void testGetApplicationName() throws Exception {

        Assert.assertEquals(TEST_APPLICATION_NAME, network.getApplicationName());
    }

    @Test
    public void testDeployAll() throws Exception {
        network.deployAll();
        for (ApplicationDescriptor descriptor : network) {
            Assert.assertEquals(MOCK_APPLICATION_REFERENCE, descriptor.getApplicationReference());
        }
    }

    @Test
    public void testDeploy() throws Exception {

        final ApplicationDescriptor descriptor = createTestApplicationDescriptor();
        network.deploy(descriptor);
        Assert.assertEquals(MOCK_APPLICATION_REFERENCE, descriptor.getApplicationReference());

        for (ApplicationDescriptor added_descriptor : network) {
            Assert.assertEquals(null, added_descriptor.getApplicationReference());
        }
    }

    @Test
    public void testKillAllOnHost() throws Exception {

        final Host local_host = new LocalHost();
        final List<ApplicationDescriptor> local_application_descriptors = new ArrayList<ApplicationDescriptor>();
        for (int i = 0; i < TEST_NETWORK_SIZE; i++) {
            final ApplicationDescriptor descriptor = new ApplicationDescriptor(local_host, TEST_APPLICATION_MANAGER);
            Assert.assertTrue(network.add(descriptor));
            local_application_descriptors.add(descriptor);
        }

        final int expected_network_size = TEST_NETWORK_SIZE * 2;
        Assert.assertEquals(expected_network_size, network.size());
        network.killAllOnHost(local_host);
        Assert.assertEquals(expected_network_size, network.size());
        for (final ApplicationDescriptor descriptor : network) {
            if (local_application_descriptors.contains(descriptor)) {
                Assert.assertTrue(descriptor.getAttribute(KILLED_ATTRIBUTE));
            }
            else {
                Assert.assertNull(descriptor.getAttribute(KILLED_ATTRIBUTE));
            }
        }
    }

    @Test
    public void testKill() throws Exception {
        ApplicationDescriptor descriptor = createTestApplicationDescriptor();
        network.kill(descriptor);
        Assert.assertTrue(descriptor.getAttribute(KILLED_ATTRIBUTE));

        for (ApplicationDescriptor added_descriptor : network) {
            Assert.assertNull(added_descriptor.getAttribute(KILLED_ATTRIBUTE));
        }
    }

    @Test(timeout = AWAIT_STATE_TEST_TIMEOUT)
    public void testAwaitAnyOfStates() throws Exception {

        ApplicationState target_state = ApplicationState.LAUNCHED;
        TEST_APPLICATION_MANAGER.setProbeStateResult(target_state);

        network.awaitAnyOfStates(target_state);
    }

    @Test
    public void testAddScanner() throws Exception {

        final TestScanner test_scanner = new TestScanner();

        Assert.assertTrue(network.addScanner(test_scanner));
        Assert.assertTrue(network.scheduled_scanners.containsKey(test_scanner));
        Assert.assertFalse(test_scanner.awaitScan(5, TimeUnit.SECONDS));
        test_scanner.setEnabled(true);
        Assert.assertTrue(test_scanner.awaitScan(5, TimeUnit.SECONDS));
        Assert.assertFalse(network.addScanner(test_scanner));
    }

    @Test
    public void testRemoveScanner() throws Exception {

        final TestScanner test_scanner = new TestScanner();
        test_scanner.setEnabled(true);
        Assert.assertFalse(network.removeScanner(test_scanner));
        Assert.assertTrue(network.addScanner(test_scanner));

        final ScheduledFuture<?> scheduled_scanner = network.scheduled_scanners.get(test_scanner);
        Assert.assertTrue(network.removeScanner(test_scanner));
        Assert.assertFalse(network.scheduled_scanners.containsKey(test_scanner));
        Assert.assertTrue(scheduled_scanner.isDone());
    }

    @Test
    public void testSetScanEnabled() throws Exception {

        final TestScanner test_scanner = new TestScanner();
        Assert.assertTrue(network.addScanner(test_scanner));

        network.setScanEnabled(true);
        Assert.assertTrue(test_scanner.isEnabled());
        Assert.assertTrue(network.auto_deploy_scanner.isEnabled());
        Assert.assertTrue(network.auto_kill_scanner.isEnabled());
        Assert.assertTrue(network.auto_remove_scanner.isEnabled());
        Assert.assertTrue(network.status_scanner.isEnabled());

        network.setScanEnabled(false);
        Assert.assertFalse(test_scanner.isEnabled());
        Assert.assertFalse(network.auto_deploy_scanner.isEnabled());
        Assert.assertFalse(network.auto_kill_scanner.isEnabled());
        Assert.assertFalse(network.auto_remove_scanner.isEnabled());
        Assert.assertFalse(network.status_scanner.isEnabled());

        Assert.assertTrue(network.removeScanner(test_scanner));
        network.setScanEnabled(true);
        Assert.assertFalse(test_scanner.isEnabled());
        Assert.assertTrue(network.auto_deploy_scanner.isEnabled());
        Assert.assertTrue(network.auto_kill_scanner.isEnabled());
        Assert.assertTrue(network.auto_remove_scanner.isEnabled());
        Assert.assertTrue(network.status_scanner.isEnabled());

    }

    @Test
    public void testSetAutoKillEnabled() throws Exception {
        network.setAutoKillEnabled(true);
        Assert.assertTrue(network.auto_kill_scanner.isEnabled());

        network.setAutoKillEnabled(false);
        Assert.assertFalse(network.auto_kill_scanner.isEnabled());

    }

    @Test
    public void testSetAutoDeployEnabled() throws Exception {
        network.setAutoDeployEnabled(true);
        Assert.assertTrue(network.auto_deploy_scanner.isEnabled());

        network.setAutoDeployEnabled(false);
        Assert.assertFalse(network.auto_deploy_scanner.isEnabled());
    }

    @Test
    public void testSetAutoRemoveEnabled() throws Exception {
        network.setAutoRemoveEnabled(true);
        Assert.assertTrue(network.auto_remove_scanner.isEnabled());

        network.setAutoRemoveEnabled(false);
        Assert.assertFalse(network.auto_remove_scanner.isEnabled());
    }

    @Test
    public void testSetStatusScannerEnabled() throws Exception {
        network.setStatusScannerEnabled(true);
        Assert.assertTrue(network.status_scanner.isEnabled());

        network.setStatusScannerEnabled(false);
        Assert.assertFalse(network.status_scanner.isEnabled());
    }

    @Test
    public void testShutdown() throws Exception {

        network.shutdown();
        for (ScheduledFuture<?> scheduled_scanner : network.scheduled_scanners.values()) {
            Assert.assertTrue(scheduled_scanner.isDone());
        }

        Assert.assertEquals(0, network.size());
    }

    @Test
    public void testAddAndContains() throws Exception {

        ApplicationDescriptor descriptor = createTestApplicationDescriptor();
        Assert.assertTrue(network.add(descriptor));
        Assert.assertTrue(network.application_descriptors.contains(descriptor));
        Assert.assertFalse(network.add(descriptor));
    }

    @Test
    public void testRemove() throws Exception {
        ApplicationDescriptor descriptor = createTestApplicationDescriptor();
        Assert.assertFalse(network.remove(descriptor));
        Assert.assertFalse(network.application_descriptors.contains(descriptor));
        Assert.assertTrue(network.add(descriptor));
        Assert.assertTrue(network.remove(descriptor));
        Assert.assertFalse(network.application_descriptors.contains(descriptor));
    }

    @Test
    public void testFirst() throws Exception {

        Assert.assertEquals(network.application_descriptors.first(), network.first());
    }

    @Test
    public void testSize() throws Exception {

        Assert.assertEquals(TEST_NETWORK_SIZE, network.size());
    }

    @Test
    public void testKillAll() throws Exception {
        network.killAll();
        for (ApplicationDescriptor descriptor : network) {
            Assert.assertTrue(descriptor.getAttribute(KILLED_ATTRIBUTE));
        }
    }

    private static class TestApplicationManager implements ApplicationManager {

        private volatile ApplicationState probe_state_result = ApplicationState.UNKNOWN;

        @Override
        public ApplicationState probeState(final ApplicationDescriptor descriptor) {
            return probe_state_result;
        }

        @Override
        public Object deploy(final ApplicationDescriptor descriptor) throws Exception {
            return MOCK_APPLICATION_REFERENCE;
        }

        @Override
        public void kill(final ApplicationDescriptor descriptor) throws Exception {
            descriptor.setAttribute(KILLED_ATTRIBUTE, true);
        }

        void setProbeStateResult(ApplicationState probe_state_result) {

            this.probe_state_result = probe_state_result;
        }
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
