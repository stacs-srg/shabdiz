package uk.ac.standrews.cs.shabdiz.evaluation;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.TimeUnit;
import javax.inject.Provider;
import org.junit.runners.Parameterized;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.standrews.cs.shabdiz.ApplicationState;
import uk.ac.standrews.cs.shabdiz.host.Host;
import uk.ac.standrews.cs.shabdiz.util.Combinations;

/**
 * Unknwon -> Auth -> Running -> Auth
 *
 * @author Masih Hajiarabderkani (mh638@st-andrews.ac.uk)
 */
public class RunningToAuthAfterKillExperiment extends Experiment {

    static final ExperimentManager[] ECHO_APPLICATION_MANAGERS = {EchoManager.FILE_BASED, EchoManager.URL_BASED, EchoManager.MAVEN_BASED};
    private static final Logger LOGGER = LoggerFactory.getLogger(RunningToAuthAfterKillExperiment.class);
    private static final String TIME_TO_REACH_AUTH_FROM_RUNNING = "time_to_reach_auth_from_running";

    public RunningToAuthAfterKillExperiment(int network_size, final Provider<Host> host_provider, ExperimentManager manager, boolean cold) throws IOException {

        super(network_size, host_provider, manager, cold);
    }

    @Parameterized.Parameters(name = "{index}: network_size: {0}, host_provider: {1}, manager: {2}, cold: {3}")
    public static Collection<Object[]> getParameters() {

        final List<Object[]> parameters = new ArrayList<Object[]>();
        final List<Object[]> combinations = Combinations.generateArgumentCombinations(new Object[][]{NETWORK_SIZES, HOST_PROVIDERS, ECHO_APPLICATION_MANAGERS, HOT_COLD});
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

        LOGGER.info("enabling auto deply");
        network.setAutoDeployEnabled(true);
        LOGGER.info("awaiting RUNNING state...");
        final long time_to_reach_running = timeUniformNetworkStateInNanos(ApplicationState.RUNNING);
        setProperty(TIME_TO_REACH_RUNNING, String.valueOf(time_to_reach_running));
        LOGGER.info("reached RUNNING state in {} seconds", TimeUnit.SECONDS.convert(time_to_reach_running, TimeUnit.NANOSECONDS));

        LOGGER.info("disabling auto deploy");
        network.setAutoDeployEnabled(false);
        LOGGER.info("killing all application instances");
        network.killAll();
        LOGGER.info("awaiting RUNNING sate after kill...");
        final long time_to_reach_auth_from_running = timeUniformNetworkStateInNanos(ApplicationState.AUTH);
        setProperty(TIME_TO_REACH_AUTH_FROM_RUNNING, String.valueOf(time_to_reach_auth_from_running));
        LOGGER.info("reached AUTH state after kill all in {} seconds", TimeUnit.SECONDS.convert(time_to_reach_auth_from_running, TimeUnit.NANOSECONDS));
    }
}
