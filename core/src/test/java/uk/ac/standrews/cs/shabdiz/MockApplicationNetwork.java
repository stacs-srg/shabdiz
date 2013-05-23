package uk.ac.standrews.cs.shabdiz;

public class MockApplicationNetwork extends ApplicationNetwork {

    static final String NAME = "Mock Network";
    static final int DEFAULT_SIZE = 10;
    final MockApplicationManager manager;

    MockApplicationNetwork() {

        super(NAME);
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

    void assertAllKilled() {
        for (ApplicationDescriptor descriptor : this) {
            manager.assertKilled(descriptor);
        }
    }
}