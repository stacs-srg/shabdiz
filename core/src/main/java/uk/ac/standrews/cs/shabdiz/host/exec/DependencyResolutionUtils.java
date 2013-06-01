package uk.ac.standrews.cs.shabdiz.host.exec;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.apache.maven.repository.internal.MavenRepositorySystemUtils;
import org.eclipse.aether.DefaultRepositorySystemSession;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.eclipse.aether.collection.CollectRequest;
import org.eclipse.aether.collection.CollectResult;
import org.eclipse.aether.collection.DependencyCollectionException;
import org.eclipse.aether.connector.async.AsyncRepositoryConnectorFactory;
import org.eclipse.aether.connector.file.FileRepositoryConnectorFactory;
import org.eclipse.aether.graph.Dependency;
import org.eclipse.aether.graph.DependencyNode;
import org.eclipse.aether.graph.DependencyVisitor;
import org.eclipse.aether.impl.DefaultServiceLocator;
import org.eclipse.aether.repository.LocalRepository;
import org.eclipse.aether.repository.RemoteRepository;
import org.eclipse.aether.resolution.ArtifactResolutionException;
import org.eclipse.aether.resolution.DependencyRequest;
import org.eclipse.aether.resolution.DependencyResolutionException;
import org.eclipse.aether.resolution.VersionResolutionException;
import org.eclipse.aether.spi.connector.RepositoryConnectorFactory;

class DependencyResolutionUtils {

    static final RemoteRepository MAVEN_CENTRAL_REPOSITORY = new RemoteRepository.Builder("central", "default", "http://repo1.maven.org/maven2/").build();
    static final RemoteRepository ST_ANDREWS_CS_MAVEN_REPOSITORY = new RemoteRepository.Builder("uk.ac.standrews.cs.maven.repository", "default", "http://maven.cs.st-andrews.ac.uk/").build();

    static final File REPOSITORY_HOME = new File(JavaBootstrap.LOCAL_SHABDIZ_HOME, "repository");
    static final RepositorySystem REPOSITORY_SYSTEM = createRepositorySystem();
    static final RepositorySystemSession REPOSITORY_SYSTEM_SESSION = createRepositorySystemSession(REPOSITORY_SYSTEM, REPOSITORY_HOME);

    static List<URL> resolveDependencies(String artifact_coordinates, RemoteRepository... repositories) throws DependencyCollectionException, DependencyResolutionException, ArtifactResolutionException, VersionResolutionException {

        return resolveDependencies(new DefaultArtifact(artifact_coordinates), repositories);
    }

    static List<URL> resolveDependencies(Artifact artifact, RemoteRepository... repositories) throws DependencyCollectionException, DependencyResolutionException, ArtifactResolutionException, VersionResolutionException {

        final CollectResult collect_result = collectDependencies(artifact, Arrays.asList(repositories));
        final DependencyNode root_dependency = collect_result.getRoot();
        final DependencyRequest dep_request = new DependencyRequest();
        dep_request.setRoot(root_dependency);
        REPOSITORY_SYSTEM.resolveDependencies(REPOSITORY_SYSTEM_SESSION, dep_request);

        final URLCollector url_collector = new URLCollector();
        root_dependency.accept(url_collector);

        return url_collector.urls;
    }

    static List<URL> resolveDependencies(String artifact_coordinates) throws Exception {

        return resolveDependencies(artifact_coordinates, MAVEN_CENTRAL_REPOSITORY, ST_ANDREWS_CS_MAVEN_REPOSITORY);
    }

    private static DefaultRepositorySystemSession createRepositorySystemSession(RepositorySystem system, File repository_home) {

        final DefaultRepositorySystemSession session = MavenRepositorySystemUtils.newSession();
        final LocalRepository localRepo = new LocalRepository(repository_home);
        session.setLocalRepositoryManager(system.newLocalRepositoryManager(session, localRepo));
        return session;
    }

    private static CollectResult collectDependencies(final Artifact artifact, final List<RemoteRepository> repositories) throws DependencyCollectionException {
        final CollectResult collectResult;
        final CollectRequest collectRequest = new CollectRequest();
        final Dependency root_dependency = new Dependency(artifact, "");
        collectRequest.setRoot(root_dependency);
        collectRequest.setRepositories(repositories);
        collectResult = REPOSITORY_SYSTEM.collectDependencies(REPOSITORY_SYSTEM_SESSION, collectRequest);
        return collectResult;
    }

    private static RepositorySystem createRepositorySystem() {
        DefaultServiceLocator locator = MavenRepositorySystemUtils.newServiceLocator();
        locator.addService(RepositoryConnectorFactory.class, FileRepositoryConnectorFactory.class);
        locator.addService(RepositoryConnectorFactory.class, AsyncRepositoryConnectorFactory.class);

        return locator.getService(RepositorySystem.class);
    }

    static class URLCollector implements DependencyVisitor {

        private final List<URL> urls;

        URLCollector() {
            urls = new ArrayList<URL>();
        }

        public boolean visitEnter(DependencyNode node) {

            final File artifact_file = node.getArtifact().getFile();
            return urls.add(toURL(artifact_file));
        }

        public boolean visitLeave(DependencyNode node) {
            return true;
        }

        private URL toURL(final File artifact_file) {
            try {
                return artifact_file.toURI().toURL();
            }
            catch (MalformedURLException e) {
                throw new RuntimeException(e);
            }
        }
    }
}

