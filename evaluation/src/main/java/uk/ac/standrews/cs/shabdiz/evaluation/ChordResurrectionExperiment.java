package uk.ac.standrews.cs.shabdiz.evaluation;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;
import org.junit.runners.Parameterized;
import org.mashti.gauge.Gauge;
import org.mashti.gauge.Timer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.standrews.cs.shabdiz.ApplicationDescriptor;
import uk.ac.standrews.cs.shabdiz.ApplicationState;
import uk.ac.standrews.cs.shabdiz.evaluation.util.ChordRingSizeScanner;
import uk.ac.standrews.cs.shabdiz.host.Host;
import uk.ac.standrews.cs.shabdiz.util.Combinations;
import uk.ac.standrews.cs.shabdiz.util.Duration;
import uk.ac.standrews.cs.shabdiz.util.TimeoutExecutorService;
import uk.ac.standrews.cs.stachord.interfaces.IChordRemoteReference;

import static uk.ac.standrews.cs.shabdiz.evaluation.Constants.ALL_CONCURRENT_SCANNER_THREAD_POOL_SIZES;
import static uk.ac.standrews.cs.shabdiz.evaluation.Constants.ALL_SCANNER_INTERVALS;
import static uk.ac.standrews.cs.shabdiz.evaluation.Constants.BLUB_HOST_PROVIDER;
import static uk.ac.standrews.cs.shabdiz.evaluation.Constants.CHORD_JOIN_RANDOM_SEED;
import static uk.ac.standrews.cs.shabdiz.evaluation.Constants.CHORD_JOIN_RETRY_INTERVAL;
import static uk.ac.standrews.cs.shabdiz.evaluation.Constants.CHORD_JOIN_TIMEOUT;
import static uk.ac.standrews.cs.shabdiz.evaluation.Constants.CHORD_MANAGER_FILE_WARM;
import static uk.ac.standrews.cs.shabdiz.evaluation.Constants.CONCURRENT_SCANNER_THREAD_POOL_SIZE_5_AND_MAX;
import static uk.ac.standrews.cs.shabdiz.evaluation.Constants.KILL_PORTION_50;
import static uk.ac.standrews.cs.shabdiz.evaluation.Constants.NETWORK_SIZE_48;
import static uk.ac.standrews.cs.shabdiz.evaluation.Constants.REPETITIONS;
import static uk.ac.standrews.cs.shabdiz.evaluation.Constants.SCANNER_INTERVAL_1_SECOND;
import static uk.ac.standrews.cs.shabdiz.evaluation.Constants.SCANNER_TIMEOUT_5_MINUTE;
import static uk.ac.standrews.cs.shabdiz.evaluation.Constants.SCHEDULER_THREAD_POOL_SIZE_10;
import static uk.ac.standrews.cs.shabdiz.evaluation.Constants.TIME_TO_REACH_STABILIZED_RING_AFTER_KILL_DURATION;
import static uk.ac.standrews.cs.shabdiz.evaluation.Constants.TIME_TO_REACH_STABILIZED_RING_AFTER_KILL_START;
import static uk.ac.standrews.cs.shabdiz.evaluation.Constants.TIME_TO_REACH_STABILIZED_RING_DURATION;
import static uk.ac.standrews.cs.shabdiz.evaluation.Constants.TIME_TO_REACH_STABILIZED_RING_START;

/**
 * Investigates how long it takes for a chord network to reach {@link ApplicationState#RUNNING} state and a stabilized ring after a portion of application instances are killed.
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
 * - awaits re-stabilized ring
 * - shuts down the network
 *
 * @author Masih Hajiarabderkani (mh638@st-andrews.ac.uk)
 */
public class ChordResurrectionExperiment extends ResurrectionExperiment {

    private static final Logger LOGGER = LoggerFactory.getLogger(ChordResurrectionExperiment.class);
    private static final Duration JOIN_TIMEOUT = new Duration(5, TimeUnit.MINUTES);
    private static final Duration JOIN_RETRY_INTERVAL = new Duration(5, TimeUnit.SECONDS);
    private static final int JOIN_RANDOM_SEED = 78354;
    private final ChordRingSizeScanner ring_size_scanner;
    private final RingSizeGauge ring_size_gauge;
    private final Set<ApplicationDescriptor> joined_nodes = new HashSet<ApplicationDescriptor>();
    private final Random random;

    public ChordResurrectionExperiment(final int network_size, final Supplier<Host> host_provider, ExperimentManager manager, final int kill_portion, Duration scanner_interval, Duration scanner_timeout, int scheduler_pool_size, int concurrent_scanner_pool_size) {

        super(network_size, host_provider, manager, kill_portion, scanner_interval, scanner_timeout, scheduler_pool_size, concurrent_scanner_pool_size);
        ring_size_scanner = new ChordRingSizeScanner(scanner_interval, scanner_timeout);
        ring_size_gauge = new RingSizeGauge();
        random = new Random(JOIN_RANDOM_SEED);
    }

    @Parameterized.Parameters(name = "network_{0}_{1}_{2}_kill_{3}_interval_{4}_timeout_{5}_sch_pool_{6}_conc_pool_{7}")
    public static Collection<Object[]> getParameters() {

        final Set<Object[]> unique_parameters = new TreeSet<Object[]>(new Comparator<Object[]>() {

            @Override
            public int compare(final Object[] o1, final Object[] o2) {

                return Arrays.toString(o1).compareTo(Arrays.toString(o2));
            }
        });
        //@formatter:off
        final List<Object[]> scanner_interval_effect = Combinations.generateArgumentCombinations(new Object[][]{
                NETWORK_SIZE_48, BLUB_HOST_PROVIDER, CHORD_MANAGER_FILE_WARM, KILL_PORTION_50,
                ALL_SCANNER_INTERVALS, SCANNER_TIMEOUT_5_MINUTE, SCHEDULER_THREAD_POOL_SIZE_10, CONCURRENT_SCANNER_THREAD_POOL_SIZE_5_AND_MAX
        });

        final List<Object[]> concurrent_scanner_pool_size_effect = Combinations.generateArgumentCombinations(new Object[][]{
                NETWORK_SIZE_48, BLUB_HOST_PROVIDER, CHORD_MANAGER_FILE_WARM, KILL_PORTION_50,
                SCANNER_INTERVAL_1_SECOND, SCANNER_TIMEOUT_5_MINUTE, SCHEDULER_THREAD_POOL_SIZE_10, ALL_CONCURRENT_SCANNER_THREAD_POOL_SIZES});
        //@formatter:on

        unique_parameters.addAll(scanner_interval_effect);
        unique_parameters.addAll(concurrent_scanner_pool_size_effect);

        final List<Object[]> parameters_with_repetitions = new ArrayList<Object[]>();
        for (int i = 0; i < REPETITIONS; i++) {
            parameters_with_repetitions.addAll(unique_parameters);
        }
        return parameters_with_repetitions;
    }

    @Override
    public void setUp() throws Exception {

        registerMetric("ring_size_gauge", ring_size_gauge);
        network.addScanner(ring_size_scanner);
        setProperty(CHORD_JOIN_TIMEOUT, JOIN_TIMEOUT);
        setProperty(CHORD_JOIN_RETRY_INTERVAL, JOIN_RETRY_INTERVAL);
        setProperty(CHORD_JOIN_RANDOM_SEED, JOIN_RANDOM_SEED);

        super.setUp();
    }

    @Override
    protected void afterDeploy() throws Exception {

        super.afterDeploy();

        LOGGER.info("enabling ring size scanner");
        ring_size_scanner.setEnabled(true);

        LOGGER.info("assembling Chord ring");
        assembleRing();

        timeRingStabilization(TIME_TO_REACH_STABILIZED_RING_START, TIME_TO_REACH_STABILIZED_RING_DURATION);
    }

    @Override
    protected void afterResurrection(final List<ApplicationDescriptor> killed_instances) throws Exception {

        super.afterResurrection(killed_instances);
        LOGGER.info("re-assembling Chord ring by re-joining {} instances", killed_instances.size());
        reAssembleRing(killed_instances);

        LOGGER.info("awaiting stabilized ring after killing portion of network...");
        timeRingStabilization(TIME_TO_REACH_STABILIZED_RING_AFTER_KILL_START, TIME_TO_REACH_STABILIZED_RING_AFTER_KILL_DURATION);

    }

    private void reAssembleRing(final List<ApplicationDescriptor> killed_instances) throws Exception {

        //TODO investigate why ring does not stabilize if assembly is only done for killed instances
        //        assembleRing(killed_instances);
        // For now we just assemble the whole network again
        joined_nodes.clear();
        assembleRing();
    }

    @Override
    protected void kill(final ApplicationDescriptor kill_candidate) throws Exception {

        super.kill(kill_candidate);
        synchronized (this) {
            joined_nodes.remove(kill_candidate);
        }
    }

    private void assembleRing(final Iterable<ApplicationDescriptor> descriptors) throws Exception {

        final List<Future<Void>> future_joins = new ArrayList<Future<Void>>();
        final ExecutorService executor = Executors.newCachedThreadPool();
        try {

            for (final ApplicationDescriptor descriptor : descriptors) {

                final Future<Void> future_join = executor.submit(() -> {

                    try {
                        final ApplicationDescriptor known_node = joinWithRetry(descriptor);
                        LOGGER.debug("node {} successfully joined {}", descriptor, known_node);
                    }
                    catch (final Exception e) {
                        LOGGER.error("node {} failed to complete join within timeout", descriptor);
                        LOGGER.error("failed join caused by", e);
                        throw e;
                    }
                    return null; // Void task
                });
                future_joins.add(future_join);
            }

            for (Future<Void> future_join : future_joins) {
                future_join.get();
            }
        }
        finally {
            executor.shutdownNow();
            for (Future<Void> deployment : future_joins) {
                deployment.cancel(true);
            }
        }
    }

    private ApplicationDescriptor joinWithRetry(final ApplicationDescriptor joiner_descriptor) throws Exception {

        return TimeoutExecutorService.retry(new Callable<ApplicationDescriptor>() {

            @Override
            public ApplicationDescriptor call() throws Exception {

                final ApplicationDescriptor known_node = getRandomJoinedNode(joiner_descriptor);
                final IChordRemoteReference joiner = joiner_descriptor.getApplicationReference();
                final IChordRemoteReference joinee = known_node.getApplicationReference();
                joiner.getRemote().join(joinee);
                synchronized (this) {
                    if (!joined_nodes.contains(joiner_descriptor)) {
                        joined_nodes.add(joiner_descriptor);
                    }
                }
                return known_node;
            }
        }, JOIN_TIMEOUT, JOIN_RETRY_INTERVAL);

    }

    private synchronized ApplicationDescriptor getRandomJoinedNode(ApplicationDescriptor joiner) {

        ApplicationDescriptor joined_node = null;
        if (joined_nodes.isEmpty()) {
            joined_nodes.add(joiner);
            joined_node = joiner;
        }
        else {
            final int size = joined_nodes.size();
            final int candidate_index = random.nextInt(size);
            final Iterator<ApplicationDescriptor> iterator = joined_nodes.iterator();
            int index = 0;
            while (iterator.hasNext()) {
                final ApplicationDescriptor candidate = iterator.next();
                if (index == candidate_index) {
                    joined_node = candidate;
                }
                index++;
            }
        }

        if (joined_node == null) {
            LOGGER.warn("null join node; possible concurrency bug");
        }

        return joined_node;
    }

    private void assembleRing() throws Exception {

        assembleRing(network);
    }

    private void timeRingStabilization(final String start_property, final String duration_property) throws InterruptedException {

        LOGGER.info("awaiting ring stabilisation...");
        final CountDownLatch stabilization_latch = new CountDownLatch(1);
        final PropertyChangeListener latched_ring_size_change_listener = new PropertyChangeListener() {

            @Override
            public synchronized void propertyChange(final PropertyChangeEvent event) {

                final Integer new_ring_size = (Integer) event.getNewValue();
                LOGGER.info("ring size changed from {} to {}", event.getOldValue(), new_ring_size);
                if (new_ring_size.equals(network_size)) {
                    stabilization_latch.countDown();
                }
            }
        };

        ring_size_scanner.addRingSizeChangeListener(latched_ring_size_change_listener);

        final Timer.Time time = time();
        stabilization_latch.await();
        final long duration = time.stop();
        final long start = time.getStartTimeInNanos();

        ring_size_scanner.removeRingSizeChangeListener(latched_ring_size_change_listener);

        setProperty(start_property, start);
        setProperty(duration_property, duration);
        LOGGER.info("reached stabilized ring of size {} in {} seconds", network_size, toSeconds(duration));
    }

    private final class RingSizeGauge implements Gauge<Integer> {

        @Override
        public Integer get() {

            return ring_size_scanner.getLastStableRingSize();
        }
    }
}
