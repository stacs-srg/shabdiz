package uk.ac.standrews.cs.shabdiz.host.exec;

import java.util.Arrays;

/** @author Masih Hajiarabderkani (mh638@st-andrews.ac.uk) */
public class MockBootstrap extends Bootstrap {

    @Override
    protected void deploy(final String... args) {

        System.out.println("ARGS: " + Arrays.toString(args));

    }

}
