package uk.ac.standrews.cs.shabdiz.host.exec;

import java.io.File;
import java.net.URL;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.TimeUnit;
import org.apache.commons.io.FileUtils;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import uk.ac.standrews.cs.shabdiz.host.LocalHost;

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

        final LocalHost host = new LocalHost();
        while (true) {
            final long now = System.nanoTime();
            final List<URL> resolve = maven_dependency_resolver.resolveAsRemoteURLs(new DefaultArtifact("uk.ac.standrews.cs:hello_world_64m:1.0-SNAPSHOT"));
            System.out.println(TimeUnit.SECONDS.convert(System.nanoTime() - now, TimeUnit.NANOSECONDS));
            System.out.println(Arrays.toString(resolve.toArray()));
            AgentBasedJavaProcessBuilder.clearCachedFilesOnHost(host);
            maven_dependency_resolver = new MavenDependencyResolver();
        }
    }

    @Test
    public void testResolveByArtifact() throws Exception {

    }

    @Test
    public void testAddRepositoryAsString() throws Exception {

    }

    @Test
    public void testAddRepositoryAsURL() throws Exception {

        String[] ss = {
                        //                "org.eclipse.aether:aether-connector-file:0.9.0.M2",
                        //                "org.eclipse.aether:aether-connector-asynchttpclient:0.9.0.M2",

                        //                "org.eclipse.aether:aether-api:0.9.0.M3",
                        //                       "org.eclipse.aether:aether-util:0.9.0.M3",
                        "org.eclipse.aether:aether-impl:0.9.0.M3", "org.eclipse.aether:aether-connector-basic:0.9.0.M3", "org.eclipse.aether:aether-transport-file:0.9.0.M3", "org.eclipse.aether:aether-transport-http:0.9.0.M3", "org.apache.maven:maven-aether-provider:3.1.1"

        };

        Set<URL> strings = new TreeSet<URL>(new Comparator<URL>() {

            @Override
            public int compare(final URL o1, final URL o2) {

                return o1.toExternalForm().compareTo(o2.toExternalForm().toString());
            }
        });

        for (String s : ss) {
            final List<URL> urls = maven_dependency_resolver.resolveAsRemoteURLs(new DefaultArtifact(s));
            strings.addAll(urls);
            System.out.println();
            System.out.println(s);
            for (URL u : urls) {
                System.out.println(u.toExternalForm());
            }
            System.out.println();
            System.out.println();
        }

        File tt = new File("/Users/masih/Desktop/tt");
        int i = 1;
        for (URL u : strings) {

            System.out.println(u.toExternalForm());
            FileUtils.copyURLToFile(u, new File(tt, String.valueOf(i++)));
        }
    }
}
