package uk.ac.standrews.cs.shabdiz.evaluation;

import edu.emory.mathcs.backport.java.util.Collections;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
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

import static uk.ac.standrews.cs.shabdiz.ApplicationState.AUTH;
import static uk.ac.standrews.cs.shabdiz.ApplicationState.RUNNING;

/**
 * Investigates how long it takes for a chord network to reach {@link ApplicationState#RUNNING} state and a stabilized ring after a portion of application instances are killed.
 *
 * For a given network size, a host provider, a manager and a kill portion:
 * - Adds all hosts to a network
 * - enables status scanner
 * - awaits {@link ApplicationState#AUTH}
 * - enables auto-deploy
 * - awaits {@link ApplicationState#RUNNING} state
 * - enables ring-size scanner
 * - assembles chord ring
 * - awaits stabilized ring
 * - disables auto-deploy
 * - kills a portion of network
 * - re-enables auto-deploy
 * - awaits {@link ApplicationState#RUNNING} state
 * - re-assemples ring
 * - awaits re-stabilized ring
 * - shuts down the network
 *
 * @author Masih Hajiarabderkani (mh638@st-andrews.ac.uk)
 */
public class ChordResurrectionExperiment extends ResurrectionExperiment {

    static final Boolean[] HOT_COLD = {Boolean.FALSE, Boolean.TRUE};
    private static final Logger LOGGER = LoggerFactory.getLogger(ChordResurrectionExperiment.class);
    private static final String TIME_TO_REACH_STABILIZED_RING = "time_to_reach_stabilized_ring";
    private static final String TIME_TO_REACH_STABILIZED_RING_AFTER_KILL = "time_to_reach_stabilized_ring_after_kill";
    private static final ChordManager[] CHORD_APPLICATION_MANAGERS = {ChordManager.FILE_BASED_COLD, ChordManager.FILE_BASED_WARM, ChordManager.URL_BASED, ChordManager.MAVEN_BASED_COLD, ChordManager.MAVEN_BASED_WARM};
    private static final Duration JOIN_TIMEOUT = new Duration(1, TimeUnit.MINUTES);
    private static final int SEED = 78354;
    private final ChordRingSizeScanner ring_size_scanner;
    private final RingSizeGauge ring_size_gauge;
    private final List<IChordRemoteReference> joined_nodes = Collections.synchronizedList(new ArrayList<IChordRemoteReference>());
    private final Random random;

    public ChordResurrectionExperiment(final int network_size, final Provider<Host> host_provider, ExperimentManager manager, final float kill_portion) {

        super(network_size, host_provider, manager, kill_portion);
        ring_size_scanner = new ChordRingSizeScanner();
        ring_size_gauge = new RingSizeGauge();
        random = new Random(SEED);
    }

    @Parameterized.Parameters(name = "{index}__size_{0}__on_{1}__{2}__kill_portion_{3}")
    public static Collection<Object[]> getParameters() {

        final List<Object[]> parameters = new ArrayList<Object[]>();
        final List<Object[]> combinations = Combinations.generateArgumentCombinations(new Object[][]{NETWORK_SIZES, BLUB_HOST_PROVIDER, CHORD_APPLICATION_MANAGERS, KILL_PORTIONS});
        for (int i = 0; i < REPETITIONS; i++) {
            parameters.addAll(combinations);
        }
        return parameters;
    }

    @Override
    public void setUp() throws Exception {

        registerMetric("ring_size_gauge", ring_size_gauge);
        network.addScanner(ring_size_scanner);
        setProperty("join_timeout", JOIN_TIMEOUT);
        super.setUp();
    }

    @Override
    public void doExperiment() throws Exception {

        LOGGER.info("enabling status scanner");
        network.setStatusScannerEnabled(true);

        LOGGER.info("awaiting AUTH state");
        network.awaitAnyOfStates(AUTH);

        LOGGER.info("enabling auto deploy");
        network.setAutoDeployEnabled(true);

        LOGGER.info("awaiting RUNNING state");
        network.awaitAnyOfStates(RUNNING);

        LOGGER.info("enabling ring size scanner");
        ring_size_scanner.setEnabled(true);

        LOGGER.info("assembing Chord ring");
        assembleRing();

        LOGGER.info("awaiting stabilized ring");
        final long time_to_reach_stabilized_ring = timeRingStabilization();
        setProperty(TIME_TO_REACH_STABILIZED_RING, String.valueOf(time_to_reach_stabilized_ring));
        LOGGER.info("reached stabilized ring in {} seconds", nanosToSeconds(time_to_reach_stabilized_ring));

        LOGGER.info("disabling auto deploy");
        network.setAutoDeployEnabled(false);

        LOGGER.info("killing {} portion of network", kill_portion);
        final List<ApplicationDescriptor> killed_descriptors = killPortionOfNetwork();

        LOGGER.info("re-enabling auto deploy");
        network.setAutoDeployEnabled(true);

        LOGGER.info("awaiting RUNNING state after killing portion of network...");
        final long time_to_reach_running_after_kill = timeUniformNetworkStateInNanos(RUNNING);
        setProperty(TIME_TO_REACH_RUNNING_AFTER_KILL, String.valueOf(time_to_reach_running_after_kill));
        LOGGER.info("reached RUNNING state after killing {} portion of network in {} seconds", kill_portion, nanosToSeconds(time_to_reach_running_after_kill));

        LOGGER.info("re-assembing Chord ring");
        assembleRing(killed_descriptors);

        LOGGER.info("awaiting stabilized ring after killing portion of network...");
        final long time_to_reach_stabilized_ring_after_kill = timeRingStabilization();
        setProperty(TIME_TO_REACH_STABILIZED_RING_AFTER_KILL, String.valueOf(time_to_reach_stabilized_ring_after_kill));
        LOGGER.info("reached stabilized ring in {} seconds after killing portion of network", nanosToSeconds(time_to_reach_stabilized_ring_after_kill));

    }

    @Override
    protected synchronized void kill(final ApplicationDescriptor kill_candidate) throws Exception {

        super.kill(kill_candidate);
        joined_nodes.remove(kill_candidate.getApplicationReference());
    }

    private void assembleRing(final Iterable<ApplicationDescriptor> descriptors) throws Exception {

        for (ApplicationDescriptor descriptor : descriptors) {
            final IChordRemoteReference node = descriptor.getApplicationReference();
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
                        synchronized (joined_nodes) {
                            joined_nodes.add(joiner);
                        }
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

    private IChordRemoteReference getRandomJoinedNode(IChordRemoteReference joiner) {

        synchronized (joined_nodes) {
            final IChordRemoteReference joined_node;
            if (joined_nodes.isEmpty()) {
                joined_nodes.add(joiner);
                joined_node = joiner;
            }
            else {
                int candidate_index = random.nextInt(joined_nodes.size());
                joined_node = joined_nodes.get(candidate_index);
                assert joined_node != null;
            }
            return joined_node;
        }
    }

    private void assembleRing() throws Exception {

        assembleRing(network);
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

        final Timer.Time time = time();
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
