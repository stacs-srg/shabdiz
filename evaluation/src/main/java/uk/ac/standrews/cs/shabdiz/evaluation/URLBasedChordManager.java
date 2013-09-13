package uk.ac.standrews.cs.shabdiz.evaluation;

import java.net.URL;
import java.util.List;
import org.eclipse.aether.artifact.DefaultArtifact;
import uk.ac.standrews.cs.shabdiz.example.util.Constants;
import uk.ac.standrews.cs.shabdiz.host.exec.MavenDependencyResolver;

/** @author Masih Hajiarabderkani (mh638@st-andrews.ac.uk) */
public class URLBasedChordManager extends ChordManager {

    private final MavenDependencyResolver resolver;

    public URLBasedChordManager() throws Exception {

        resolver = new MavenDependencyResolver();
    }

    @Override
    protected void configure() throws Exception {

        super.configure();

        final String stachord_coordinates = MavenDependencyResolver.toCoordinate(Constants.CS_GROUP_ID, "stachord", "2.0-SNAPSHOT");
        final List<URL> dependenlcy_urls = resolver.resolveAsRemoteURLs(new DefaultArtifact(stachord_coordinates));
        for (URL url : dependenlcy_urls) {
            process_builder.addURL(url);
        }
    }
}
