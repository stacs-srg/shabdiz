package uk.ac.standrews.cs.shabdiz.evaluation;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import javax.inject.Provider;
import org.junit.runners.Parameterized;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.standrews.cs.shabdiz.ApplicationDescriptor;
import uk.ac.standrews.cs.shabdiz.ApplicationState;
import uk.ac.standrews.cs.shabdiz.host.Host;
import uk.ac.standrews.cs.shabdiz.util.Combinations;
import uk.ac.standrews.cs.shabdiz.util.Duration;

import static uk.ac.standrews.cs.shabdiz.ApplicationState.AUTH;
import static uk.ac.standrews.cs.shabdiz.ApplicationState.RUNNING;
import static uk.ac.standrews.cs.shabdiz.evaluation.Constants.ALL_KILL_PORTIONS;
import static uk.ac.standrews.cs.shabdiz.evaluation.Constants.ALL_MANAGERS;
import static uk.ac.standrews.cs.shabdiz.evaluation.Constants.ALL_MANAGERS_FILE_COLD;
import static uk.ac.standrews.cs.shabdiz.evaluation.Constants.ALL_NETWORK_SIZES;
import static uk.ac.standrews.cs.shabdiz.evaluation.Constants.BLUB_HOST_PROVIDER;
import static uk.ac.standrews.cs.shabdiz.evaluation.Constants.CHORD_ECHO_HELLO_WORLD_FILE_WARM_MANAGERS;
import static uk.ac.standrews.cs.shabdiz.evaluation.Constants.CONCURRENT_SCANNER_THREAD_POOL_SIZE_MAX;
import static uk.ac.standrews.cs.shabdiz.evaluation.Constants.KILL_PORTION_50;
import static uk.ac.standrews.cs.shabdiz.evaluation.Constants.KILL_PORTION_PROPERTY;
import static uk.ac.standrews.cs.shabdiz.evaluation.Constants.KILL_PORTION_RANDOM_SEED;
import static uk.ac.standrews.cs.shabdiz.evaluation.Constants.NETWORK_SIZE_48;
import static uk.ac.standrews.cs.shabdiz.evaluation.Constants.REPETITIONS;
import static uk.ac.standrews.cs.shabdiz.evaluation.Constants.SCANNER_INTERVAL_1_SECOND;
import static uk.ac.standrews.cs.shabdiz.evaluation.Constants.SCANNER_TIMEOUT_5_MINUTE;
import static uk.ac.standrews.cs.shabdiz.evaluation.Constants.SCHEDULER_THREAD_POOL_SIZE_10;
import static uk.ac.standrews.cs.shabdiz.evaluation.Constants.TIME_TO_REACH_AUTH_AFTER_KILL_DURATION;
import static uk.ac.standrews.cs.shabdiz.evaluation.Constants.TIME_TO_REACH_AUTH_AFTER_KILL_START;
import static uk.ac.standrews.cs.shabdiz.evaluation.Constants.TIME_TO_REACH_AUTH_DURATION;
import static uk.ac.standrews.cs.shabdiz.evaluation.Constants.TIME_TO_REACH_AUTH_START;
import static uk.ac.standrews.cs.shabdiz.evaluation.Constants.TIME_TO_REACH_RUNNING_AFTER_KILL_DURATION;
import static uk.ac.standrews.cs.shabdiz.evaluation.Constants.TIME_TO_REACH_RUNNING_AFTER_KILL_START;
import static uk.ac.standrews.cs.shabdiz.evaluation.Constants.TIME_TO_REACH_RUNNING_DURATION;
import static uk.ac.standrews.cs.shabdiz.evaluation.Constants.TIME_TO_REACH_RUNNING_START;

/**
 * Investigates how long it takes for a network to reach {@link ApplicationState#RUNNING} state after a portion of application instances are killed.
 * For a given network size, a host provider, a manager and a kill portion:
 * - Adds all hosts to a network
 * - enables status scanner
 * - awaits {@link ApplicationState#AUTH} state
 * - enables auto-deploy
 * - awaits {@link ApplicationState#RUNNING} state
 * - disables auto-deploy
 * - kills a portion of network
 * - re-enables auto-deploy
 * - awaits {@link ApplicationState#RUNNING} state
 * - shuts down the network
 *
 * @author Masih Hajiarabderkani (mh638@st-andrews.ac.uk)
 */
public class ResurrectionExperiment extends Experiment {

    private static final Logger LOGGER = LoggerFactory.getLogger(ResurrectionExperiment.class);
    private static final long RANDOM_SEED = 4546468;
    protected final int kill_portion;
    private final Random random;

    public ResurrectionExperiment(final int network_size, final Provider<Host> host_provider, ExperimentManager manager, final int kill_portion, Duration scanner_interval, Duration scanner_timeout, int scheduler_thread_pool_size, final int concurrent_scanner_thread_pool_size) {

        super(network_size, host_provider, manager, scanner_interval, scanner_timeout, scheduler_thread_pool_size, concurrent_scanner_thread_pool_size);
        validateKillPortion(kill_portion);
        this.kill_portion = kill_portion;
        random = new Random(RANDOM_SEED);
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
        final List<Object[]> network_size_effect = Combinations.generateArgumentCombinations(new Object[][]{
                ALL_NETWORK_SIZES, BLUB_HOST_PROVIDER, CHORD_ECHO_HELLO_WORLD_FILE_WARM_MANAGERS, KILL_PORTION_50,
                SCANNER_INTERVAL_1_SECOND, SCANNER_TIMEOUT_5_MINUTE, SCHEDULER_THREAD_POOL_SIZE_10, CONCURRENT_SCANNER_THREAD_POOL_SIZE_MAX});

        final List<Object[]> application_size_effect = Combinations.generateArgumentCombinations(new Object[][]{
                NETWORK_SIZE_48, BLUB_HOST_PROVIDER, ALL_MANAGERS_FILE_COLD, KILL_PORTION_50,
                SCANNER_INTERVAL_1_SECOND, SCANNER_TIMEOUT_5_MINUTE, SCHEDULER_THREAD_POOL_SIZE_10, CONCURRENT_SCANNER_THREAD_POOL_SIZE_MAX});

        final List<Object[]> deployment_strategy_effect = Combinations.generateArgumentCombinations(new Object[][]{
                NETWORK_SIZE_48, BLUB_HOST_PROVIDER, ALL_MANAGERS, KILL_PORTION_50,
                SCANNER_INTERVAL_1_SECOND, SCANNER_TIMEOUT_5_MINUTE, SCHEDULER_THREAD_POOL_SIZE_10, CONCURRENT_SCANNER_THREAD_POOL_SIZE_MAX});

        final List<Object[]> kill_portion_effect = Combinations.generateArgumentCombinations(new Object[][]{
                NETWORK_SIZE_48, BLUB_HOST_PROVIDER, CHORD_ECHO_HELLO_WORLD_FILE_WARM_MANAGERS, ALL_KILL_PORTIONS,
                SCANNER_INTERVAL_1_SECOND, SCANNER_TIMEOUT_5_MINUTE, SCHEDULER_THREAD_POOL_SIZE_10, CONCURRENT_SCANNER_THREAD_POOL_SIZE_MAX});
        //@formatter:on

        unique_parameters.addAll(network_size_effect);
        unique_parameters.addAll(application_size_effect);
        unique_parameters.addAll(deployment_strategy_effect);
        unique_parameters.addAll(kill_portion_effect);

        final List<Object[]> parameters_with_repetitions = new ArrayList<Object[]>();
        for (int i = 0; i < REPETITIONS; i++) {
            parameters_with_repetitions.addAll(unique_parameters);
        }
        return parameters_with_repetitions;
    }

    @Override
    public void setUp() throws Exception {

        super.setUp();
        setProperty(KILL_PORTION_PROPERTY, kill_portion);
        setProperty(KILL_PORTION_RANDOM_SEED, RANDOM_SEED);
    }

    @Override
    public void doExperiment() throws Exception {

        LOGGER.info("enabling status scanner");
        network.setStatusScannerEnabled(true);

        timeUniformNetworkState(TIME_TO_REACH_AUTH_START, TIME_TO_REACH_AUTH_DURATION, AUTH);

        LOGGER.info("enabling auto deploy");
        network.setAutoDeployEnabled(true);

        timeUniformNetworkState(TIME_TO_REACH_RUNNING_START, TIME_TO_REACH_RUNNING_DURATION, RUNNING);

        afterDeploy();

        if (kill_portion > 0) {
            LOGGER.info("disabling auto deploy");
            network.setAutoDeployEnabled(false);

            LOGGER.info("killing {}% of network", kill_portion);
            final List<ApplicationDescriptor> killed_instances = killPortionOfNetwork();

            LOGGER.info("re-enabling auto deploy");
            network.setAutoDeployEnabled(true);

            timeUniformNetworkState(TIME_TO_REACH_RUNNING_AFTER_KILL_START, TIME_TO_REACH_RUNNING_AFTER_KILL_DURATION, RUNNING);

            afterResurrection(killed_instances);
        }

        LOGGER.info("disabling auto deploy prior to kill all");
        network.setAutoDeployEnabled(false);

        LOGGER.info("enabling auto kill");
        network.setAutoKillEnabled(true);

        timeUniformNetworkState(TIME_TO_REACH_AUTH_AFTER_KILL_START, TIME_TO_REACH_AUTH_AFTER_KILL_DURATION, AUTH);
    }

    protected void afterResurrection(final List<ApplicationDescriptor> killed_instances) throws Exception {

        assert killed_instances != null;
    }

    protected void afterDeploy() throws Exception {

    }

    private static void validateKillPortion(final float kill_portion) {

        if (kill_portion < 0 || kill_portion > 100) { throw new IllegalArgumentException("kill portion must be between 0.0 (exclusive) to 1.0 (inclusive)"); }
    }

    protected List<ApplicationDescriptor> killPortionOfNetwork() throws Exception {

        final int kill_count = getNumberOfKillCandidates();
        LOGGER.info("killing {} out of {}...", kill_count, network_size);

        final List<ApplicationDescriptor> descriptors_list = new ArrayList<ApplicationDescriptor>(network.getApplicationDescriptors());
        final List<ApplicationDescriptor> killed_descriptors = new ArrayList<ApplicationDescriptor>();
        final int descriptors_count = descriptors_list.size();

        final ExecutorService executor = Executors.newCachedThreadPool();
        try {
            final List<Future> awaits = new ArrayList<Future>();
            for (int i = 0; i < kill_count; i++) {

                final int kill_candidate_index = random.nextInt(descriptors_count - i);
                final ApplicationDescriptor kill_candidate = descriptors_list.remove(kill_candidate_index);
                final Future<Void> future_kill = executor.submit(new Callable<Void>() {

                    @Override
                    public Void call() throws Exception {

                        kill(kill_candidate);
                        killed_descriptors.add(kill_candidate);
                        LOGGER.debug("killed {}", kill_candidate);
                        return null;
                    }
                });
                awaits.add(future_kill);
                for (Future future : awaits) {
                    future.get();
                }
            }
        }
        finally {
            executor.shutdownNow();
        }
        return killed_descriptors;
    }

    protected void kill(final ApplicationDescriptor kill_candidate) throws Exception {

        network.kill(kill_candidate);
    }

    private int getNumberOfKillCandidates() {

        final int kill_count = network_size * kill_portion / 100;
        if (kill_count == 0) {
            LOGGER.warn("the number of instances to kill is zero for network size of {} and kill portion of {}", network_size, kill_portion);
        }
        return kill_count;
    }
}
