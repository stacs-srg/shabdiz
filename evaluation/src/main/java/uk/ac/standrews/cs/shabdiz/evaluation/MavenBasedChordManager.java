package uk.ac.standrews.cs.shabdiz.evaluation;

import uk.ac.standrews.cs.shabdiz.host.exec.MavenManagedJavaProcessBuilder;
import uk.ac.standrews.cs.stachord.servers.NodeServer;

/** @author Masih Hajiarabderkani (mh638@st-andrews.ac.uk) */
public class MavenBasedChordManager extends ChordManager {

    private static final MavenManagedJavaProcessBuilder process_builder;

    static {
        process_builder = new MavenManagedJavaProcessBuilder();
        process_builder.addMavenDependency("uk.ac.standrews.cs", "stachord", "2.0-SNAPSHOT");
        process_builder.setMainClass(NodeServer.class);
    }

    public MavenBasedChordManager() {

        super(process_builder);
    }
}
