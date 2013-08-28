package uk.ac.standrews.cs.shabdiz.evaluation;

import java.io.IOException;
import java.util.Set;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.mashti.gauge.Timer;
import uk.ac.standrews.cs.shabdiz.ApplicationNetwork;
import uk.ac.standrews.cs.shabdiz.ApplicationState;
import uk.ac.standrews.cs.shabdiz.example.echo.EchoNetwork;
import uk.ac.standrews.cs.shabdiz.host.Host;

/** @author Masih Hajiarabderkani (mh638@st-andrews.ac.uk) */
@RunWith(value = Parameterized.class)
public class UnknownToAuthExperiment extends Experiment {

    protected final Timer state_timer;
    private final EchoNetwork network;
    private final int network_size;

    public UnknownToAuthExperiment(int network_size) throws IOException {

        super(network_size);
        this.network_size = network_size;
        state_timer = new Timer();
        network = new EchoNetwork();
    }

    @Override
    public void setUp() throws Exception {

        registerMetric("state_timer", state_timer);
        disableAllNetworkScanners();
        populateNetwork();
        super.setUp();
    }

    @Override
    public void doExperiment() throws InterruptedException {

        final Timer.Time time = state_timer.time();
        getNetwork().setStatusScannerEnabled(true);
        getNetwork().awaitAnyOfStates(ApplicationState.AUTH);
        time.stop();
    }

    @Override
    protected ApplicationNetwork getNetwork() {

        return network;
    }

    protected void populateNetwork() throws IOException {

        final Set<Host> hosts = BlubUtils.getHosts(network_size);
        network.addAll(hosts);
    }

    protected void disableAllNetworkScanners() {

        network.setAutoKillEnabled(false);
        network.setAutoRemoveEnabled(false);
        network.setAutoDeployEnabled(false);
        network.setStatusScannerEnabled(false);
        network.setScanEnabled(false);
    }
}
