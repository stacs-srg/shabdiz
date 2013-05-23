package uk.ac.standrews.cs.shabdiz;

import org.junit.Assert;
import uk.ac.standrews.cs.shabdiz.util.AttributeKey;

public class MockApplicationManager implements ApplicationManager {

    private static final Object MOCK_APPLICATION_REFERENCE = new Object();
    private static final AttributeKey<Boolean> KILLED_ATTRIBUTE = new AttributeKey<Boolean>();
    private volatile ApplicationState probe_state_result = ApplicationState.UNKNOWN;
    private boolean throw_exception_on_kill;

    @Override
    public ApplicationState probeState(final ApplicationDescriptor descriptor) {

        return isKilled(descriptor) ? ApplicationState.KILLED : isDeployed(descriptor) ? ApplicationState.DEPLOYED : probe_state_result;
    }

    private boolean isDeployed(final ApplicationDescriptor descriptor) {
        return MOCK_APPLICATION_REFERENCE.equals(descriptor.getApplicationReference());
    }

    private Boolean isKilled(final ApplicationDescriptor descriptor) {

        final Boolean killed = descriptor.getAttribute(KILLED_ATTRIBUTE);
        return killed != null && killed;
    }

    @Override
    public Object deploy(final ApplicationDescriptor descriptor) throws Exception {
        return MOCK_APPLICATION_REFERENCE;
    }

    @Override
    public void kill(final ApplicationDescriptor descriptor) throws Exception {
        if (!throw_exception_on_kill) {
            descriptor.setAttribute(KILLED_ATTRIBUTE, true);
        }
        else { throw new Exception(); }
    }

    public void setThrowExceptionOnKill(final boolean enabled) {
        throw_exception_on_kill = enabled;
    }

    public void assertDeployed(final ApplicationDescriptor descriptor) {
        Assert.assertEquals(MOCK_APPLICATION_REFERENCE, descriptor.getApplicationReference());
    }

    public void assertKilled(final ApplicationDescriptor descriptor) {
        Assert.assertTrue(isKilled(descriptor));
    }

    public void assertNotKilled(final ApplicationDescriptor descriptor) {
        Assert.assertFalse(isKilled(descriptor));
    }

    void setProbeStateResult(ApplicationState probe_state_result) {

        this.probe_state_result = probe_state_result;
    }
}