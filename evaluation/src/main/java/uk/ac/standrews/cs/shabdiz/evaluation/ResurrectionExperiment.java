package uk.ac.standrews.cs.shabdiz.evaluation;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Random;
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

import static uk.ac.standrews.cs.shabdiz.ApplicationState.AUTH;
import static uk.ac.standrews.cs.shabdiz.ApplicationState.RUNNING;

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

    public static final String KILL_PORTION = "kill_portion";
    static final String TIME_TO_REACH_RUNNING_AFTER_KILL = "time_to_reach_running_after_kill";
    static final Float[] KILL_PORTIONS = {0.1F, 0.3F, 0.5F, 0.7F, 0.9F};
    private static final Logger LOGGER = LoggerFactory.getLogger(ResurrectionExperiment.class);
    private static final ExperimentManager[] ECHO_APPLICATION_MANAGERS = {EchoManager.FILE_BASED_WARM, EchoManager.FILE_BASED_COLD, EchoManager.URL_BASED, EchoManager.MAVEN_BASED_WARM, EchoManager.MAVEN_BASED_COLD};
    private static final long RANDOM_SEED = 0x455fa4;
    protected final float kill_portion;
    private final Random random;

    public ResurrectionExperiment(final int network_size, final Provider<Host> host_provider, ExperimentManager manager, final float kill_portion) {

        super(network_size, host_provider, manager);
        if (kill_portion <= 0 || kill_portion > 1) { throw new IllegalArgumentException("kill portion must be between 0.0 (excusive) to 1.0 (inclusive)"); }
        this.kill_portion = kill_portion;
        random = new Random(RANDOM_SEED);
        setProperty(KILL_PORTION, kill_portion);
    }

    @Parameterized.Parameters(name = "network_size_{0}__on_{1}__{2}__kill_portion_{3}")
    public static Collection<Object[]> getParameters() {

        final List<Object[]> parameters = new ArrayList<Object[]>();
        final List<Object[]> combinations = Combinations.generateArgumentCombinations(new Object[][] {NETWORK_SIZES, BLUB_HOST_PROVIDER, ECHO_APPLICATION_MANAGERS, KILL_PORTIONS});
        for (int i = 0; i < REPETITIONS; i++) {
            parameters.addAll(combinations);
        }
        return parameters;
    }

    @Override
    public void doExperiment() throws Exception {

        LOGGER.info("enabling status scanner");
        network.setStatusScannerEnabled(true);

        LOGGER.info("awaiting AUTH state...");
        network.awaitAnyOfStates(AUTH);

        LOGGER.info("enabling auto deploy");
        network.setAutoDeployEnabled(true);

        LOGGER.info("awaiting RUNNING state...");
        network.awaitAnyOfStates(RUNNING);

        LOGGER.info("disabling auto deploy");
        network.setAutoDeployEnabled(false);

        LOGGER.info("killing {} portion of network", kill_portion);
        killPortionOfNetwork();

        LOGGER.info("re-enabling auto deploy");
        network.setAutoDeployEnabled(true);

        LOGGER.info("awaiting RUNNING state after killing a portion of network...");
        final long time_to_reach_running_after_kill = timeUniformNetworkStateInNanos(RUNNING);
        setProperty(TIME_TO_REACH_RUNNING_AFTER_KILL, String.valueOf(time_to_reach_running_after_kill));
        LOGGER.info("reached RUNNING state after killing {} portion of network in {} seconds", kill_portion, nanosToSeconds(time_to_reach_running_after_kill));
    }

    @Override
    protected String constructName() {

        return super.constructName() + '_' + kill_portion;
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
                        //                        kill_candidate.awaitAnyOfStates(AUTH);
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

        final int kill_count = (int) (network_size * kill_portion);
        if (kill_count == 0) {
            LOGGER.warn("the number of instances to kill is zero for network size of {} and kill portion of {}", network_size, kill_portion);
        }
        return kill_count;
    }
}
