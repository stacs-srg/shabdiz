package uk.ac.standrews.cs.shabdiz.evaluation;

import uk.ac.standrews.cs.shabdiz.example.util.Constants;

/** @author Masih Hajiarabderkani (mh638@st-andrews.ac.uk) */
public class MavenBasedChordManager extends ChordManager {

    @Override
    protected void configure() {

        super.configure();
        process_builder.addMavenDependency(Constants.CS_GROUP_ID, "stachord", "2.0-SNAPSHOT");
        process_builder.addMavenDependency(Constants.CS_GROUP_ID, "stachord", "2.0-SNAPSHOT", "tests");
    }
}
