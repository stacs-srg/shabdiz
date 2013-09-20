package uk.ac.standrews.cs.shabdiz.evaluation;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Random;
import java.util.concurrent.TimeUnit;
import javax.inject.Provider;
import org.junit.runners.Parameterized;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.standrews.cs.shabdiz.ApplicationDescriptor;
import uk.ac.standrews.cs.shabdiz.ApplicationState;
import uk.ac.standrews.cs.shabdiz.host.Host;
import uk.ac.standrews.cs.shabdiz.util.Combinations;

/**
 * Unknwon -> Auth -> Running -> Kill a portion -> Running
 *
 * @author Masih Hajiarabderkani (mh638@st-andrews.ac.uk)
 */
public class RunningToRunningAfterKillExperiment extends Experiment {

    static final String TIME_TO_REACH_RUNNING_AFTER_KILL = "time_to_reach_running_after_kill";
    static final Float[] KILL_PORTIONS = {0.1F, 0.3F, 0.5F, 0.7F, 0.9F};
    private static final Logger LOGGER = LoggerFactory.getLogger(RunningToRunningAfterKillExperiment.class);
    private static final long RANDOM_SEED = 0x455fa4;
    protected final float kill_portion;
    private final Random random;

    public RunningToRunningAfterKillExperiment(final int network_size, final Provider<Host> host_provider, ExperimentManager manager, boolean cold, final float kill_portion) throws IOException {

        super(network_size, host_provider, manager, cold);
        if (kill_portion <= 0 || kill_portion > 1) { throw new IllegalArgumentException("kill portion must be between 0.0 (excusive) to 1.0 (inclusive)"); }
        this.kill_portion = kill_portion;
        random = new Random(RANDOM_SEED);
    }

    @Parameterized.Parameters(name = "{index}: network_size: {0}, host_provider: {1}, manager: {2}, cold: {3}, kill_portion: {4}")
    public static Collection<Object[]> getParameters() {

        final List<Object[]> parameters = new ArrayList<Object[]>();
        final List<Object[]> combinations = Combinations.generateArgumentCombinations(new Object[][]{NETWORK_SIZES, HOST_PROVIDERS, APPLICATION_MANAGERS, HOT_COLD, KILL_PORTIONS});
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
        final long time_to_reach_auth = timeUniformNetworkStateInNanos(ApplicationState.AUTH);
        setProperty(TIME_TO_REACH_AUTH, String.valueOf(time_to_reach_auth));
        LOGGER.info("reached AUTH state in {} seconds", TimeUnit.SECONDS.convert(time_to_reach_auth, TimeUnit.NANOSECONDS));

        LOGGER.info("enabling auto deploy");
        network.setAutoDeployEnabled(true);
        LOGGER.info("awaiting RUNNING state...");
        final long time_to_reach_running = timeUniformNetworkStateInNanos(ApplicationState.RUNNING);
        setProperty(TIME_TO_REACH_RUNNING, String.valueOf(time_to_reach_running));
        LOGGER.info("reached RUNNING state in {} seconds", TimeUnit.SECONDS.convert(time_to_reach_running, TimeUnit.NANOSECONDS));

        LOGGER.info("disabling auto deploy");
        network.setAutoDeployEnabled(false);
        LOGGER.info("killing {} portion of network", kill_portion);
        killPortionOfNetwork();

        LOGGER.info("re-enabling auto deploy");
        network.setAutoDeployEnabled(true);
        LOGGER.info("awaiting RUNNING state after killing portion of network...");
        final long time_to_reach_running_after_kill = timeUniformNetworkStateInNanos(ApplicationState.RUNNING);
        setProperty(TIME_TO_REACH_RUNNING_AFTER_KILL, String.valueOf(time_to_reach_running_after_kill));
        LOGGER.info("reached RUNNING state after killing {} portion of network in {} seconds", kill_portion, TimeUnit.SECONDS.convert(time_to_reach_running, TimeUnit.NANOSECONDS));
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
        for (int i = 0; i < kill_count; i++) {

            final int kill_candidate_index = random.nextInt(descriptors_count - i);
            final ApplicationDescriptor kill_candidate = descriptors_list.remove(kill_candidate_index);
            kill(kill_candidate);
            kill_candidate.awaitAnyOfStates(ApplicationState.AUTH);
            killed_descriptors.add(kill_candidate);
            LOGGER.debug("killed {}", kill_candidate);
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
