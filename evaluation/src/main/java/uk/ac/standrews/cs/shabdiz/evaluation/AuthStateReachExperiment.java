package uk.ac.standrews.cs.shabdiz.evaluation;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
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
public class AuthStateReachExperiment extends Experiment {

    protected final Timer state_timer;
    private final EchoNetwork network;
    private final int network_size;

    public AuthStateReachExperiment(int network_size) throws IOException {

        super(network_size);
        this.network_size = network_size;
        state_timer = new Timer();
        network = new EchoNetwork();
    }

    @Parameterized.Parameters
    public static Collection<Object[]> data() {

        final List<Object[]> parameters = new ArrayList<Object[]>();
        parameters.add(new Object[]{10});
        parameters.add(new Object[]{20});
        parameters.add(new Object[]{30});
        parameters.add(new Object[]{40});
        parameters.add(new Object[]{48});
        return parameters;
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
        getNetwork().awaitAnyOfStates(getNetworkTargetState());
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
    }

    protected ApplicationState getNetworkTargetState() {

        return ApplicationState.AUTH;
    }
}
