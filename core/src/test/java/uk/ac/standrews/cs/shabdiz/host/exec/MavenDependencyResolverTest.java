package uk.ac.standrews.cs.shabdiz.host.exec;

import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

/** @author Masih Hajiarabderkani (mh638@st-andrews.ac.uk) */
public class MavenDependencyResolverTest {

    MavenDependencyResolver maven_dependency_resolver;

    @Before
    public void setUp() throws Exception {

        maven_dependency_resolver = new MavenDependencyResolver();
    }

    @Test
    public void testResolveByCoordinate() throws Exception {

        final List<URL> resolve = maven_dependency_resolver.resolveAsRemoteURLs(new DefaultArtifact("uk.ac.standrews.cs.sample_applications:hello_world_64m:1.0"));
        for (URL url : resolve) {
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            try {
                connection.setRequestMethod("HEAD");
                assertEquals(200, connection.getResponseCode());
            }
            finally {
                connection.disconnect();
            }
        }
    }

}
