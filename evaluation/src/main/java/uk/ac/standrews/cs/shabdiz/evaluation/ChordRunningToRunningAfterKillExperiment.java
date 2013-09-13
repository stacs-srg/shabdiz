package uk.ac.standrews.cs.shabdiz.evaluation;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import javax.inject.Provider;
import org.junit.runners.Parameterized;
import org.mashti.gauge.Gauge;
import org.mashti.gauge.Timer;
import uk.ac.standrews.cs.nds.rpc.RPCException;
import uk.ac.standrews.cs.shabdiz.ApplicationDescriptor;
import uk.ac.standrews.cs.shabdiz.ApplicationManager;
import uk.ac.standrews.cs.shabdiz.ApplicationState;
import uk.ac.standrews.cs.shabdiz.evaluation.util.ChordRingSizeScanner;
import uk.ac.standrews.cs.shabdiz.host.Host;
import uk.ac.standrews.cs.shabdiz.util.Combinations;
import uk.ac.standrews.cs.stachord.interfaces.IChordRemoteReference;

/**
 * Unknwon -> Auth -> Running -> Kill a portion -> Running
 *
 * @author Masih Hajiarabderkani (mh638@st-andrews.ac.uk)
 */
public class ChordRunningToRunningAfterKillExperiment extends RunningToRunningAfterKillExperiment {

    private static final String TIME_TO_REACH_STABILIZED_RING = "time_to_reach_stabilized_ring";
    private static final String TIME_TO_REACH_STABILIZED_RING_AFTER_KILL = "time_to_reach_stabilized_ring_after_kill";
    private static final ChordManager[] CHORD_APPLICATION_MANAGERS = {};
    private final ChordRingSizeScanner ring_size_scanner;
    private final RingSizeGauge ring_size_gauge;

    public ChordRunningToRunningAfterKillExperiment(final int network_size, final Provider<Host> host_provider, final ApplicationManager manager, final float kill_portion) throws IOException {

        super(network_size, host_provider, manager, kill_portion);
        ring_size_scanner = new ChordRingSizeScanner();
        ring_size_gauge = new RingSizeGauge();
    }

    @Parameterized.Parameters(name = "{index}: network_size: {0}, host_provider: {1}, chord_manager: {2}, kill_portion: {3}")
    public static Collection<Object[]> getParameters() {

        final List<Object[]> parameters = new ArrayList<Object[]>();
        final List<Object[]> combinations = Combinations.generateArgumentCombinations(new Object[][]{NETWORK_SIZES, HOST_PROVIDERS, CHORD_APPLICATION_MANAGERS, KILL_PORTIONS});
        for (int i = 0; i < REPETITIONS; i++) {
            parameters.addAll(combinations);
        }
        return parameters;
    }

    @Override
    public void setUp() throws Exception {

        registerMetric("ring_size_gauge", ring_size_gauge);
        network.addScanner(ring_size_scanner);
        super.setUp();
    }

    @Override
    public void doExperiment() throws Exception {

        network.setStatusScannerEnabled(true);
        final long time_to_reach_auth = timeUniformNetworkStateInNanos(ApplicationState.AUTH);
        setProperty(TIME_TO_REACH_AUTH, String.valueOf(time_to_reach_auth));

        network.setAutoDeployEnabled(true);
        ring_size_scanner.setEnabled(true);
        final long time_to_reach_running = timeUniformNetworkStateInNanos(ApplicationState.RUNNING);
        setProperty(TIME_TO_REACH_RUNNING, String.valueOf(time_to_reach_running));

        assembleRing();
        final long time_to_reach_stabilized_ring = timeRingStabilization();
        setProperty(TIME_TO_REACH_STABILIZED_RING, String.valueOf(time_to_reach_stabilized_ring));

        network.setAutoDeployEnabled(false);
        killPortionOfNetwork();
        network.setAutoDeployEnabled(true);
        final long time_to_reach_running_after_kill = timeUniformNetworkStateInNanos(ApplicationState.RUNNING);
        setProperty(TIME_TO_REACH_RUNNING_AFTER_KILL, String.valueOf(time_to_reach_running_after_kill));

        final long time_to_reach_stabilized_ring_after_kill = timeRingStabilization();
        setProperty(TIME_TO_REACH_STABILIZED_RING_AFTER_KILL, String.valueOf(time_to_reach_stabilized_ring_after_kill));

    }

    private void assembleRing() throws RPCException {

        IChordRemoteReference known_node = null;
        for (ApplicationDescriptor descriptor : network) {

            final IChordRemoteReference reference = descriptor.getApplicationReference();
            if (known_node == null) {
                known_node = reference;
            }
            else {
                reference.getRemote().join(known_node);
            }
        }
    }

    private long timeRingStabilization() throws InterruptedException {

        final CountDownLatch stabilization_latch = new CountDownLatch(1);
        final PropertyChangeListener latched_ring_size_change_listener = new PropertyChangeListener() {

            @Override
            public synchronized void propertyChange(final PropertyChangeEvent event) {

                final Integer new_ring_size = (Integer) event.getNewValue();
                if (new_ring_size.equals(network_size)) {
                    stabilization_latch.countDown();
                }
            }
        };
        ring_size_scanner.addRingSizeChangeListener(latched_ring_size_change_listener);

        final Timer.Time time = timer.time();
        stabilization_latch.await();
        return time.stop();
    }

    private final class RingSizeGauge implements Gauge<Integer> {

        @Override
        public Integer get() {

            return ring_size_scanner.getLastStableRingSize();
        }
    }
}
