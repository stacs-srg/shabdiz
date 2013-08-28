package uk.ac.standrews.cs.shabdiz.evaluation;

import java.io.IOException;
import org.mashti.gauge.Timer;
import uk.ac.standrews.cs.shabdiz.ApplicationState;

/** @author Masih Hajiarabderkani (mh638@st-andrews.ac.uk) */
public class RunningToAuthExperiment extends UnknownToAuthExperiment {

    public RunningToAuthExperiment(final int network_size) throws IOException {

        super(network_size);
    }

    @Override
    public void doExperiment() throws InterruptedException {

        final Timer.Time time_to_reach_running = state_timer.time();
        getNetwork().setStatusScannerEnabled(true);
        getNetwork().setAutoDeployEnabled(true);
        getNetwork().awaitAnyOfStates(ApplicationState.RUNNING);
        time_to_reach_running.stop();

        final Timer.Time time_to_reach_auth = state_timer.time();
        getNetwork().setAutoDeployEnabled(false);
        getNetwork().setAutoKillEnabled(true);
        getNetwork().awaitAnyOfStates(ApplicationState.RUNNING);
        time_to_reach_auth.stop();
    }
}
