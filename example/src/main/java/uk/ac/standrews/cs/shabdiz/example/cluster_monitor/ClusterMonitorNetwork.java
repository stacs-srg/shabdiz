package uk.ac.standrews.cs.shabdiz.example.cluster_monitor;

import uk.ac.standrews.cs.shabdiz.ApplicationDescriptor;
import uk.ac.standrews.cs.shabdiz.ApplicationNetwork;
import uk.ac.standrews.cs.shabdiz.host.Host;

public class ClusterMonitorNetwork extends ApplicationNetwork {

    private final ClusterMonitorManager manager;

    public ClusterMonitorNetwork() {

        super("cluster monitor network");
        manager = new ClusterMonitorManager();
    }

    boolean add(final Host host) {

        final ApplicationDescriptor descriptor = new ApplicationDescriptor(host, manager);
        return add(descriptor);
    }

}
