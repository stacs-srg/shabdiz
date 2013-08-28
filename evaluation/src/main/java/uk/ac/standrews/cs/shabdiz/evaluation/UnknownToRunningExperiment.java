package uk.ac.standrews.cs.shabdiz.evaluation;

import java.io.IOException;
import org.mashti.gauge.Timer;
import uk.ac.standrews.cs.shabdiz.ApplicationState;

/** @author Masih Hajiarabderkani (mh638@st-andrews.ac.uk) */
public class UnknownToRunningExperiment extends UnknownToAuthExperiment {

    public UnknownToRunningExperiment(final int network_size) throws IOException {

        super(network_size);
    }

    @Override
    public void doExperiment() throws InterruptedException {

        final Timer.Time time = state_timer.time();
        getNetwork().setStatusScannerEnabled(true);
        getNetwork().setAutoDeployEnabled(true);
        getNetwork().awaitAnyOfStates(ApplicationState.RUNNING);
        time.stop();
    }
}
