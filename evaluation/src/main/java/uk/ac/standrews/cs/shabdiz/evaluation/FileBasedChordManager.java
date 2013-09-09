package uk.ac.standrews.cs.shabdiz.evaluation;

import uk.ac.standrews.cs.shabdiz.host.exec.FileBasedJavaProcessBuilder;
import uk.ac.standrews.cs.stachord.servers.NodeServer;

/** @author Masih Hajiarabderkani (mh638@st-andrews.ac.uk) */
public class FileBasedChordManager extends ChordManager {

    private static final FileBasedJavaProcessBuilder FILE_BASED_JAVA_PROCESS_BUILDER;

    static {
        FILE_BASED_JAVA_PROCESS_BUILDER = new FileBasedJavaProcessBuilder(NodeServer.class);

    }

    public FileBasedChordManager() {

        super(FILE_BASED_JAVA_PROCESS_BUILDER);
    }
}
