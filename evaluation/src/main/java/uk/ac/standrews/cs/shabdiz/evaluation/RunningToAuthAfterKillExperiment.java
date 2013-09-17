package uk.ac.standrews.cs.shabdiz.evaluation;

import java.io.IOException;
import java.util.concurrent.TimeUnit;
import javax.inject.Provider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.standrews.cs.shabdiz.ApplicationState;
import uk.ac.standrews.cs.shabdiz.host.Host;

/**
 * Unknwon -> Auth -> Running -> Auth
 *
 * @author Masih Hajiarabderkani (mh638@st-andrews.ac.uk)
 */
public class RunningToAuthAfterKillExperiment extends Experiment {

    private static final Logger LOGGER = LoggerFactory.getLogger(RunningToAuthAfterKillExperiment.class);
    private static final String TIME_TO_REACH_AUTH_FROM_RUNNING = "time_to_reach_auth_from_running";

    public RunningToAuthAfterKillExperiment(int network_size, final Provider<Host> host_provider, ExperimentManager manager, boolean cold) throws IOException {

        super(network_size, host_provider, manager, cold);
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
