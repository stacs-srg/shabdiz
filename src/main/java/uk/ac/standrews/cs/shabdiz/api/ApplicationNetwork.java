package uk.ac.standrews.cs.shabdiz.api;

public interface ApplicationNetwork<T extends ApplicationDescriptor> extends Network<T> {

    String getApplicationName();

    void awaitAnyState(State... states) throws InterruptedException;

    boolean addScanner(Scanner scanner);

    boolean removeScanner(Scanner scanner);

    void setScanEnabled(boolean enabled);

    //    boolean isScanEnabled();

    //FIXME auto deploy and auto kill
}
