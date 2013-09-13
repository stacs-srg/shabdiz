package uk.ac.standrews.cs.shabdiz.host.exec;

import java.net.URL;
import java.util.List;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/** @author Masih Hajiarabderkani (mh638@st-andrews.ac.uk) */
public class MavenDependencyResolverTest {

    MavenDependencyResolver maven_dependency_resolver;

    @Before
    public void setUp() throws Exception {

        maven_dependency_resolver = new MavenDependencyResolver();
    }

    @After
    public void tearDown() throws Exception {

    }

    @Test
    public void testResolveByCoordinate() throws Exception {

        final List<URL> resolve = maven_dependency_resolver.resolve("uk.ac.standrews.cs:stachord:2.0-SNAPSHOT");
    }

    @Test
    public void testResolveByArtifact() throws Exception {

    }

    @Test
    public void testAddRepositoryAsString() throws Exception {

    }

    @Test
    public void testAddRepositoryAsURL() throws Exception {

    }
}
