package uk.ac.standrews.cs.shabdiz.evaluation;

import uk.ac.standrews.cs.shabdiz.example.util.Constants;

/** @author Masih Hajiarabderkani (mh638@st-andrews.ac.uk) */
public class MavenBasedChordManager extends ChordManager {

    public MavenBasedChordManager() throws Exception {

    }

    @Override
    protected void configure() throws Exception {

        super.configure();
        process_builder.addMavenDependency(Constants.CS_GROUP_ID, "stachord", "2.0-SNAPSHOT");
    }
}
