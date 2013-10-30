package uk.ac.standrews.cs.shabdiz.evaluation;

import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import javax.inject.Provider;
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
import static uk.ac.standrews.cs.shabdiz.evaluation.Constants.CONCURRENT_SCANNER_THREAD_POOL_SIZE_MAX;
import static uk.ac.standrews.cs.shabdiz.evaluation.Constants.KILL_PORTION_50;
import static uk.ac.standrews.cs.shabdiz.evaluation.Constants.NETWORK_SIZE_48;
import static uk.ac.standrews.cs.shabdiz.evaluation.Constants.REPETITIONS;
import static uk.ac.standrews.cs.shabdiz.evaluation.Constants.SCANNER_INTERVAL_1_SECOND;
import static uk.ac.standrews.cs.shabdiz.evaluation.Constants.SCANNER_TIMEOUT_1_MINUTE;
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
    private final List<IChordRemoteReference> joined_nodes = new ArrayList<IChordRemoteReference>();
    private final Random random;

    public ChordResurrectionExperiment(final int network_size, final Provider<Host> host_provider, ExperimentManager manager, final int kill_portion, Duration scanner_interval, Duration scanner_timeout, int scheduler_pool_size, int concurrent_scanner_pool_size) {

        super(network_size, host_provider, manager, kill_portion, scanner_interval, scanner_timeout, scheduler_pool_size, concurrent_scanner_pool_size);
        ring_size_scanner = new ChordRingSizeScanner();
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
                ALL_SCANNER_INTERVALS, SCANNER_TIMEOUT_1_MINUTE, SCHEDULER_THREAD_POOL_SIZE_10, CONCURRENT_SCANNER_THREAD_POOL_SIZE_MAX});

        final List<Object[]> concurrent_scanner_pool_size_effect = Combinations.generateArgumentCombinations(new Object[][]{
                NETWORK_SIZE_48, BLUB_HOST_PROVIDER, CHORD_MANAGER_FILE_WARM, KILL_PORTION_50,
                SCANNER_INTERVAL_1_SECOND, SCANNER_TIMEOUT_1_MINUTE, SCHEDULER_THREAD_POOL_SIZE_10, ALL_CONCURRENT_SCANNER_THREAD_POOL_SIZES});
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
        assembleRing(killed_instances);

        LOGGER.info("awaiting stabilized ring after killing portion of network...");
        timeRingStabilization(TIME_TO_REACH_STABILIZED_RING_AFTER_KILL_START, TIME_TO_REACH_STABILIZED_RING_AFTER_KILL_DURATION);

    }

    @Override
    protected void kill(final ApplicationDescriptor kill_candidate) throws Exception {

        super.kill(kill_candidate);
        synchronized (joined_nodes) {
            joined_nodes.remove(kill_candidate.getApplicationReference());
        }
    }

    private void assembleRing(final Iterable<ApplicationDescriptor> descriptors) throws Exception {

        final List<ListenableFuture<Void>> future_joins = new ArrayList<ListenableFuture<Void>>();
        final ListeningExecutorService executor = MoreExecutors.listeningDecorator(Executors.newCachedThreadPool());
        try {

            for (ApplicationDescriptor descriptor : descriptors) {
                final IChordRemoteReference node = descriptor.getApplicationReference();

                final ListenableFuture<Void> future_join = executor.submit(new Callable<Void>() {

                    @Override
                    public Void call() throws Exception {

                        try {
                            final IChordRemoteReference known_node = joinWithRetry(node);
                            LOGGER.debug("node {}({}) successfully joined {}({})", node.getCachedKey(), node.getCachedAddress(), known_node.getCachedKey(), known_node.getCachedAddress());
                        }
                        catch (final Exception e) {
                            LOGGER.error("node {}({}) failed to complete join within timeout", node.getCachedKey(), node.getCachedAddress());
                            LOGGER.error("failed join caused by", e);
                            throw e;
                        }
                        return null; // Void task
                    }
                });
                future_joins.add(future_join);
            }
            Futures.allAsList(future_joins).get();
        }
        finally {
            executor.shutdownNow();
            for (ListenableFuture<Void> deployment : future_joins) {
                deployment.cancel(true);
            }
        }
    }

    private IChordRemoteReference joinWithRetry(final IChordRemoteReference joiner) throws Exception {

        return TimeoutExecutorService.retry(new Callable<IChordRemoteReference>() {

            @Override
            public IChordRemoteReference call() throws Exception {

                final IChordRemoteReference known_node = getRandomJoinedNode(joiner);
                joiner.getRemote().join(known_node);
                if (!known_node.equals(joiner)) {
                    synchronized (joined_nodes) {
                        joined_nodes.add(joiner);
                    }
                }
                return known_node;
            }
        }, JOIN_TIMEOUT, JOIN_RETRY_INTERVAL);
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

    private void timeRingStabilization(final String start_property, final String duration_property) throws InterruptedException {

        LOGGER.info("awaiting ring stabilisation...");
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
