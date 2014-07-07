package uk.ac.standrews.cs.shabdiz;

import java.util.concurrent.TimeUnit;
import org.junit.Assert;
import uk.ac.standrews.cs.shabdiz.util.Duration;

public class MockApplicationNetwork extends ApplicationNetwork {

    static final String NAME = "Mock Network";
    static final int DEFAULT_SIZE = 10;
    final MockApplicationManager manager;

    MockApplicationNetwork() {

        super(NAME, new Duration(5, TimeUnit.SECONDS), new Duration(1, TimeUnit.MINUTES), 50, 50);
        manager = new MockApplicationManager();
    }

    void populate() {
        populate(DEFAULT_SIZE);
    }

    void populate(final int size) {
        for (int i = 0; i < size; i++) {
            add(createApplicationDescriptor());
        }
    }

    ApplicationDescriptor createApplicationDescriptor() {

        return new ApplicationDescriptor(manager);
    }

    void assertAllDeployed() {
        for (ApplicationDescriptor descriptor : this) {
            manager.assertDeployed(descriptor);
        }
    }

    void assertAllNotDeployed() {
        for (ApplicationDescriptor descriptor : this) {
            manager.assertNotDeployed(descriptor);
        }
    }

    void assertAllKilled() {
        for (ApplicationDescriptor descriptor : this) {
            manager.assertKilled(descriptor);
        }
    }

    void assertAllNotKilled() {
        for (ApplicationDescriptor descriptor : this) {
            manager.assertNotKilled(descriptor);
        }
    }

    void assertAllInState(final ApplicationState expected_state) {

        for (ApplicationDescriptor descriptor : this) {
            Assert.assertEquals(expected_state, descriptor.getApplicationState());
        }
    }

    void assertEmptiness() {
        Assert.assertTrue(application_descriptors.isEmpty());
    }
}
