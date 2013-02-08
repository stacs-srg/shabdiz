package uk.ac.standrews.cs.shabdiz.impl;

import java.io.IOException;

public interface RemoteProcessBuilder {

    Process start(Host host) throws IOException, InterruptedException;

}
