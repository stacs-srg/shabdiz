package uk.ac.standrews.cs.shabdiz.evaluation;

import java.io.File;
import java.util.List;
import uk.ac.standrews.cs.shabdiz.example.util.Constants;
import uk.ac.standrews.cs.shabdiz.host.exec.MavenDependencyResolver;

/** @author Masih Hajiarabderkani (mh638@st-andrews.ac.uk) */
public class FileBasedChordManager extends ChordManager {

    private final MavenDependencyResolver resolver;

    public FileBasedChordManager() throws Exception {

        resolver = new MavenDependencyResolver();
    }

    @Override
    protected void configure() throws Exception {

        super.configure();

        final String stachord_coordinates = MavenDependencyResolver.toCoordinate(Constants.CS_GROUP_ID, "stachord", "2.0-SNAPSHOT");
        final List<File> dependenlcy_files = resolver.resolve(stachord_coordinates);
        for (File file : dependenlcy_files) {
            process_builder.addFile(file);
        }
    }
}
