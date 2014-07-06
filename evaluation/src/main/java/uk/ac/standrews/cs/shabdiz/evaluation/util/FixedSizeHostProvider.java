package uk.ac.standrews.cs.shabdiz.evaluation.util;

import java.util.function.Supplier;
import uk.ac.standrews.cs.shabdiz.host.Host;

/** @author Masih Hajiarabderkani (mh638@st-andrews.ac.uk) */
public abstract class FixedSizeHostProvider implements Supplier<Host> {

    private final int size;

    public FixedSizeHostProvider(int size) {

        validateSize(size);
        this.size = size;
    }

    public int getSize() {

        return size;
    }

    protected void validateSize(final int size) {

        if (size < 1) { throw new IllegalArgumentException("size must be at least 1"); }
    }
}
