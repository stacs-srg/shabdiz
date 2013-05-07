package uk.ac.standrews.cs.shabdiz.example.cluster_monitor;

import uk.ac.standrews.cs.shabdiz.AbstractApplicationManager;
import uk.ac.standrews.cs.shabdiz.ApplicationDescriptor;

public class ClusterMonitorManager extends AbstractApplicationManager {

    @Override
    public Object deploy(final ApplicationDescriptor descriptor) throws Exception {

        throw new UnsupportedOperationException("no applciation-specific deployment is supported");
    }

    @Override
    protected void attemptApplicationCall(final ApplicationDescriptor descriptor) throws Exception {

        throw new UnsupportedOperationException("no applciation-specific call is supported");
    }
}
