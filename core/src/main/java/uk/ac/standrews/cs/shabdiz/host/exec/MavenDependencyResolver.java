package uk.ac.standrews.cs.shabdiz.host.exec;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
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
import org.eclipse.aether.resolution.DependencyRequest;
import org.eclipse.aether.resolution.DependencyResolutionException;
import org.eclipse.aether.spi.connector.RepositoryConnectorFactory;

class MavenDependencyResolver {

    static final File REPOSITORY_HOME = new File(Bootstrap.LOCAL_SHABDIZ_HOME, "repository");
    private static final String DEFAULT_REPOSITORY_TYPE = "default";
    private static final RemoteRepository MAVEN_CENTRAL_REPOSITORY = new RemoteRepository.Builder("central", "default", "http://repo1.maven.org/maven2/").build();
    private static final RemoteRepository ST_ANDREWS_CS_MAVEN_REPOSITORY = new RemoteRepository.Builder("uk.ac.standrews.cs.maven.repository", "default", "http://maven.cs.st-andrews.ac.uk/").build();
    private static final RepositorySystem REPOSITORY_SYSTEM = createRepositorySystem();
    private static final RepositorySystemSession REPOSITORY_SYSTEM_SESSION = createRepositorySystemSession(REPOSITORY_SYSTEM, REPOSITORY_HOME);
    private final List<RemoteRepository> repositories;

    MavenDependencyResolver() {

        repositories = new ArrayList<RemoteRepository>();
        addDefaultRepositories();
    }

    private void addDefaultRepositories() {

        addRepository(MAVEN_CENTRAL_REPOSITORY);
        addRepository(ST_ANDREWS_CS_MAVEN_REPOSITORY);
    }

    private boolean addRepository(final RemoteRepository repository) {

        return repositories.add(repository);
    }

    List<URL> resolve(String artifact_coordinates) throws DependencyCollectionException, DependencyResolutionException {

        return resolve(new DefaultArtifact(artifact_coordinates));
    }

    List<URL> resolve(final Artifact artifact) throws DependencyCollectionException, DependencyResolutionException {

        final CollectResult collect_result = collectDependencies(artifact, repositories);
        final DependencyNode root_dependency = collect_result.getRoot();
        final DependencyRequest dep_request = new DependencyRequest();
        final URLCollector url_collector = new URLCollector();
        dep_request.setRoot(root_dependency);
        REPOSITORY_SYSTEM.resolveDependencies(REPOSITORY_SYSTEM_SESSION, dep_request);
        root_dependency.accept(url_collector);

        return url_collector.urls;
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

    boolean addRepository(final String url) throws MalformedURLException {

        return addRepository(new URL(url));
    }

    boolean addRepository(final URL url) {

        return addRepository(toRemoteRepository(url));
    }

    private static RemoteRepository toRemoteRepository(final URL url) {

        return new RemoteRepository.Builder(url.getHost(), DEFAULT_REPOSITORY_TYPE, url.toString()).build();
    }

    private static DefaultRepositorySystemSession createRepositorySystemSession(final RepositorySystem system, final File repository_home) {

        final DefaultRepositorySystemSession session = MavenRepositorySystemUtils.newSession();
        final LocalRepository localRepo = new LocalRepository(repository_home);
        session.setLocalRepositoryManager(system.newLocalRepositoryManager(session, localRepo));
        return session;
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

        public boolean visitEnter(final DependencyNode node) {

            final File artifact_file = node.getArtifact().getFile();
            return urls.add(toURL(artifact_file));
        }

        private URL toURL(final File artifact_file) {

            try {
                return artifact_file.toURI().toURL();
            }
            catch (MalformedURLException e) {
                throw new RuntimeException(e);
            }
        }

        public boolean visitLeave(final DependencyNode node) {

            return true;
        }
    }
}
