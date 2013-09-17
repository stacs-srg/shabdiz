package uk.ac.standrews.cs.shabdiz.evaluation;

import java.io.IOException;
import java.util.concurrent.TimeUnit;
import javax.inject.Provider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.standrews.cs.shabdiz.ApplicationDescriptor;
import uk.ac.standrews.cs.shabdiz.ApplicationState;
import uk.ac.standrews.cs.shabdiz.host.Host;

/**
 * Unknwon -> Auth -> Running -> RESET_STATE -> Running
 *
 * @author Masih Hajiarabderkani (mh638@st-andrews.ac.uk)
 */
public class RunningToRunningAfterResetExperiment extends Experiment {

    private static final Logger LOGGER = LoggerFactory.getLogger(RunningToRunningAfterResetExperiment.class);
    private static final String TIME_TO_REACH_RUNNING_AFTER_RESET = "time_to_reach_running_after_reset";

    public RunningToRunningAfterResetExperiment(final int network_size, final Provider<Host> host_provider, ExperimentManager manager, boolean cold) throws IOException {

        super(network_size, host_provider, manager, cold);
    }

    @Override
    public void doExperiment() throws InterruptedException {

        LOGGER.info("enabling status scanner");
        network.setStatusScannerEnabled(true);
        LOGGER.info("awaiting AUTH state");
        final long time_to_reach_auth = timeUniformNetworkStateInNanos(ApplicationState.AUTH);
        setProperty(TIME_TO_REACH_AUTH, String.valueOf(time_to_reach_auth));
        LOGGER.info("reached AUTH state in {} seconds", TimeUnit.SECONDS.convert(time_to_reach_auth, TimeUnit.NANOSECONDS));

        LOGGER.info("enabling auto deply");
        network.setAutoDeployEnabled(true);
        final long time_to_reach_running = timeUniformNetworkStateInNanos(ApplicationState.RUNNING);
        setProperty(TIME_TO_REACH_RUNNING, String.valueOf(time_to_reach_running));
        LOGGER.info("reached RUNNING state in {} seconds", TimeUnit.SECONDS.convert(time_to_reach_running, TimeUnit.NANOSECONDS));

        LOGGER.info("disabling all network scanners");
        disableAllNetworkScanners();
        LOGGER.info("resetting all application descriptor states to UNKNOWN");
        resetApplicationDescriptorsStatusTo(ApplicationState.UNKNOWN);
        LOGGER.info("re-enabling status scanner");
        network.setStatusScannerEnabled(true);
        LOGGER.info("awaiting RUNNING state after reset");
        final long time_to_reach_running_after_reset = timeUniformNetworkStateInNanos(ApplicationState.RUNNING);
        setProperty(TIME_TO_REACH_RUNNING_AFTER_RESET, String.valueOf(time_to_reach_running_after_reset));
        LOGGER.info("reached RUNNING state after reset in {} seconds", TimeUnit.SECONDS.convert(time_to_reach_running_after_reset, TimeUnit.NANOSECONDS));
    }

    private void resetApplicationDescriptorsStatusTo(final ApplicationState status) {

        for (ApplicationDescriptor descriptor : getNetwork()) {
            descriptor.setApplicationState(status);
        }
    }
}
