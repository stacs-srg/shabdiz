package uk.ac.standrews.cs.shabdiz.testing.junit;

import java.util.List;
import org.junit.runner.Runner;
import org.junit.runners.Parameterized;

/** @author Masih Hajiarabderkani (mh638@st-andrews.ac.uk) */
public class ParameterizedRange extends Parameterized {

    //TODO implement annotation-based configuration for the first constructor

    private final List<Runner> sub_runners;

    public ParameterizedRange(final Class<?> klass) throws Throwable {

        super(klass);
        sub_runners = super.getChildren();
    }

    public ParameterizedRange(final Class<?> klass, int from_index, int to_index) throws Throwable {

        super(klass);
        sub_runners = super.getChildren().subList(from_index, to_index);
    }

    @Override
    protected List<Runner> getChildren() {

        return sub_runners;
    }
}
