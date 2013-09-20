package uk.ac.standrews.cs.shabdiz.evaluation;

import edu.emory.mathcs.backport.java.util.Collections;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Random;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import javax.inject.Provider;
import org.junit.runners.Parameterized;
import org.mashti.gauge.Gauge;
import org.mashti.gauge.Timer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.standrews.cs.nds.rpc.RPCException;
import uk.ac.standrews.cs.shabdiz.ApplicationDescriptor;
import uk.ac.standrews.cs.shabdiz.ApplicationState;
import uk.ac.standrews.cs.shabdiz.evaluation.util.ChordRingSizeScanner;
import uk.ac.standrews.cs.shabdiz.host.Host;
import uk.ac.standrews.cs.shabdiz.util.Combinations;
import uk.ac.standrews.cs.shabdiz.util.Duration;
import uk.ac.standrews.cs.shabdiz.util.TimeoutExecutorService;
import uk.ac.standrews.cs.stachord.interfaces.IChordRemoteReference;

/**
 * Unknwon -> Auth -> Running -> Kill a portion -> Running
 *
 * @author Masih Hajiarabderkani (mh638@st-andrews.ac.uk)
 */
public class ChordRunningToRunningAfterKillExperiment extends RunningToRunningAfterKillExperiment {

    private static final Logger LOGGER = LoggerFactory.getLogger(ChordRunningToRunningAfterKillExperiment.class);
    private static final String TIME_TO_REACH_STABILIZED_RING = "time_to_reach_stabilized_ring";
    private static final String TIME_TO_REACH_STABILIZED_RING_AFTER_KILL = "time_to_reach_stabilized_ring_after_kill";
    private static final ChordManager[] CHORD_APPLICATION_MANAGERS = {ChordManager.FILE_BASED, ChordManager.URL_BASED, ChordManager.MAVEN_BASED};
    private static final Duration JOIN_TIMEOUT = new Duration(5, TimeUnit.SECONDS);
    private static final int SEED = 78354;
    private final ChordRingSizeScanner ring_size_scanner;
    private final RingSizeGauge ring_size_gauge;
    private final List<IChordRemoteReference> joined_nodes = Collections.synchronizedList(new ArrayList<IChordRemoteReference>());
    private final Random random;

    public ChordRunningToRunningAfterKillExperiment(final int network_size, final Provider<Host> host_provider, ExperimentManager manager, boolean cold, final float kill_portion) throws IOException {

        super(network_size, host_provider, manager, cold, kill_portion);
        ring_size_scanner = new ChordRingSizeScanner();
        ring_size_gauge = new RingSizeGauge();
        random = new Random(SEED);
    }

    @Parameterized.Parameters(name = "{index}: network_size: {0}, host_provider: {1}, chord_manager: {2}, cold: {3}, kill_portion: {4}")
    public static Collection<Object[]> getParameters() {

        final List<Object[]> parameters = new ArrayList<Object[]>();
        final List<Object[]> combinations = Combinations.generateArgumentCombinations(new Object[][]{NETWORK_SIZES, HOST_PROVIDERS, CHORD_APPLICATION_MANAGERS, HOT_COLD, KILL_PORTIONS});
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

        LOGGER.info("enabling status scanner");
        network.setStatusScannerEnabled(true);
        LOGGER.info("awaiting AUTH state");
        final long time_to_reach_auth = timeUniformNetworkStateInNanos(ApplicationState.AUTH);
        setProperty(TIME_TO_REACH_AUTH, String.valueOf(time_to_reach_auth));
        LOGGER.info("reached AUTH state in {} seconds", TimeUnit.SECONDS.convert(time_to_reach_auth, TimeUnit.NANOSECONDS));

        LOGGER.info("enabling auto deploy");
        network.setAutoDeployEnabled(true);
        LOGGER.info("enabling ring size scanner");
        ring_size_scanner.setEnabled(true);
        LOGGER.info("awaiting RUNNING state");
        final long time_to_reach_running = timeUniformNetworkStateInNanos(ApplicationState.RUNNING);
        setProperty(TIME_TO_REACH_RUNNING, String.valueOf(time_to_reach_running));
        LOGGER.info("reached RUNNING state in {} seconds", TimeUnit.SECONDS.convert(time_to_reach_running, TimeUnit.NANOSECONDS));

        LOGGER.info("assembing Chord ring");
        assembleRing();
        LOGGER.info("awaiting stabilized ring");
        final long time_to_reach_stabilized_ring = timeRingStabilization();
        setProperty(TIME_TO_REACH_STABILIZED_RING, String.valueOf(time_to_reach_stabilized_ring));
        LOGGER.info("reached stabilized ring in {} seconds", TimeUnit.SECONDS.convert(time_to_reach_stabilized_ring, TimeUnit.NANOSECONDS));

        LOGGER.info("disabling auto deploy");
        network.setAutoDeployEnabled(false);
        LOGGER.info("killing {} portion of network", kill_portion);
        final List<ApplicationDescriptor> killed_descriptors = killPortionOfNetwork();

        LOGGER.info("re-enabling auto deploy");
        network.setAutoDeployEnabled(true);
        final long time_to_reach_running_after_kill = timeUniformNetworkStateInNanos(ApplicationState.RUNNING);
        LOGGER.info("awaiting RUNNING state after killing portion of network...");
        setProperty(TIME_TO_REACH_RUNNING_AFTER_KILL, String.valueOf(time_to_reach_running_after_kill));
        LOGGER.info("reached RUNNING state after killing {} portion of network in {} seconds", kill_portion, TimeUnit.SECONDS.convert(time_to_reach_running, TimeUnit.NANOSECONDS));

        LOGGER.info("re-assembing Chord ring");
        reassembleRing(killed_descriptors);

        LOGGER.info("awaiting stabilized ring after killing portion of network...");
        final long time_to_reach_stabilized_ring_after_kill = timeRingStabilization();
        setProperty(TIME_TO_REACH_STABILIZED_RING_AFTER_KILL, String.valueOf(time_to_reach_stabilized_ring_after_kill));
        LOGGER.info("reached stabilized ring in {} seconds after killing portion of network", TimeUnit.SECONDS.convert(time_to_reach_stabilized_ring_after_kill, TimeUnit.NANOSECONDS));

    }

    @Override
    protected synchronized void kill(final ApplicationDescriptor kill_candidate) throws Exception {

        super.kill(kill_candidate);
        joined_nodes.remove(kill_candidate.getApplicationReference());
    }

    private void reassembleRing(final List<ApplicationDescriptor> killed_descriptors) throws Exception {

        for (ApplicationDescriptor killed_descriptor : killed_descriptors) {
            final IChordRemoteReference node = killed_descriptor.getApplicationReference();
            joinWithTimeout(node);
        }
    }

    private void joinWithTimeout(final IChordRemoteReference joiner) throws Exception {

        TimeoutExecutorService.awaitCompletion(new Callable<Void>() {

            @Override
            public Void call() throws Exception {

                boolean successful = false;
                while (!Thread.currentThread().isInterrupted() && !successful) {
                    try {
                        IChordRemoteReference known_node = getRandomJoinedNode(joiner);
                        joiner.getRemote().join(known_node);
                        successful = true;
                    }
                    catch (RPCException e) {
                        successful = false;
                    }
                }
                return null;
            }
        }, JOIN_TIMEOUT);
    }

    private synchronized IChordRemoteReference getRandomJoinedNode(IChordRemoteReference joiner) {

        final IChordRemoteReference joined_node;
        if (joined_nodes.isEmpty()) {
            joined_nodes.add(joiner);
            joined_node = joiner;
        }
        else {
            int candidate_index = random.nextInt(joined_nodes.size());
            joined_node = joined_nodes.get(candidate_index);
        }
        return joined_node;
    }

    private void assembleRing() throws Exception {

        for (ApplicationDescriptor descriptor : network) {

            final IChordRemoteReference reference = descriptor.getApplicationReference();
            joinWithTimeout(reference);
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
