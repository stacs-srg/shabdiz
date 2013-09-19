package uk.ac.standrews.cs.shabdiz.evaluation.util;

import java.io.IOException;
import javax.inject.Provider;
import uk.ac.standrews.cs.shabdiz.host.Host;
import uk.ac.standrews.cs.shabdiz.host.LocalHost;

/** @author Masih Hajiarabderkani (mh638@st-andrews.ac.uk) */
public class LocalHostProvider implements Provider<Host> {

    @Override
    public Host get() {

        try {
            return new LocalHost();
        }
        catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public String toString() {

        return "localhost";
    }
}
