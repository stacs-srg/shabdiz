package uk.ac.standrews.cs.shabdiz;

import org.junit.Assert;
import uk.ac.standrews.cs.shabdiz.util.AttributeKey;

public class MockApplicationManager implements ApplicationManager {

    private static final Object MOCK_APPLICATION_REFERENCE = new Object();
    private static final AttributeKey<Boolean> KILLED_ATTRIBUTE = new AttributeKey<Boolean>();
    private volatile ApplicationState probe_state_result = ApplicationState.UNKNOWN;

    @Override
    public ApplicationState probeState(final ApplicationDescriptor descriptor) {

        return isKilled(descriptor) ? ApplicationState.KILLED : isDeployed(descriptor) ? ApplicationState.DEPLOYED : probe_state_result;
    }

    @Override
    public Object deploy(final ApplicationDescriptor descriptor) throws Exception {
        return MOCK_APPLICATION_REFERENCE;
    }

    @Override
    public void kill(final ApplicationDescriptor descriptor) throws Exception {
        descriptor.setAttribute(KILLED_ATTRIBUTE, true);
    }

    private Boolean isKilled(final ApplicationDescriptor descriptor) {

        final Boolean killed = descriptor.getAttribute(KILLED_ATTRIBUTE);
        return killed != null && killed;
    }

    private boolean isDeployed(final ApplicationDescriptor descriptor) {
        return MOCK_APPLICATION_REFERENCE.equals(descriptor.getApplicationReference());
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