package uk.ac.standrews.cs.shabdiz.host.exec;

import java.io.File;
import java.io.IOException;
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
import org.eclipse.aether.connector.basic.BasicRepositoryConnectorFactory;
import org.eclipse.aether.graph.Dependency;
import org.eclipse.aether.graph.DependencyNode;
import org.eclipse.aether.graph.DependencyVisitor;
import org.eclipse.aether.impl.DefaultServiceLocator;
import org.eclipse.aether.repository.LocalRepository;
import org.eclipse.aether.repository.RemoteRepository;
import org.eclipse.aether.resolution.DependencyRequest;
import org.eclipse.aether.resolution.DependencyResolutionException;
import org.eclipse.aether.spi.connector.RepositoryConnectorFactory;
import org.eclipse.aether.spi.connector.transport.TransporterFactory;
import org.eclipse.aether.transport.file.FileTransporterFactory;
import org.eclipse.aether.transport.http.HttpTransporterFactory;
import uk.ac.standrews.cs.shabdiz.util.URLUtils;

/**
 * Resolves Maven artifact dependencies into a list of Jar files.
 * By default uses Maven central repository and the St Andrews School of Computer Science Maven repository. 
 * 
 * @author Masih Hajiarabderkani (mh638@st-andrews.ac.uk)
 */
public class MavenDependencyResolver {

    static final File SHABDIZ_REPOSITORY_HOME = new File(Bootstrap.LOCAL_SHABDIZ_HOME, "repository");
    private static final String COLON = ":";
    private static final String DEFAULT_REPOSITORY_TYPE = "default";
    private static final ServiceLocatorErrorHandler DEFAULT_ERROR_HANDLER = new ServiceLocatorErrorHandler();
    private static final RemoteRepository MAVEN_CENTRAL_REPOSITORY = new RemoteRepository.Builder("central", "default", "http://central.maven.org/maven2/").build();
    private static final RemoteRepository ST_ANDREWS_CS_MAVEN_REPOSITORY = new RemoteRepository.Builder("uk.ac.standrews.cs.maven.repository", "default", "https://maven.cs.st-andrews.ac.uk/").build();
    private static final RepositorySystem REPOSITORY_SYSTEM = createRepositorySystem();
    private static final int URL_PING_TIMEOUT_MILLIS = 15000;
    private final List<RemoteRepository> repositories;
    private final RepositorySystemSession session;

    /**
     * Instantiates a new Maven dependency resolver using the default shabdiz repository home.
     */
    public MavenDependencyResolver() {

        this(SHABDIZ_REPOSITORY_HOME);
    }

    /**
     * Instantiates a new Maven dependency resolver at the given {@code repository_home}.
     *
     * @param repository_home the local repository home
     */
    public MavenDependencyResolver(File repository_home) {

        session = createRepositorySystemSession(REPOSITORY_SYSTEM, repository_home);
        repositories = new ArrayList<RemoteRepository>();
        addDefaultRepositories();
    }

    /**
     * Constructs Maven artifact coordinates from group ID, artifact ID, version and maven artifact classifier.
     *
     * @param group_id the group ID
     * @param artifact_id the artifact ID
     * @param version the artifact version
     * @param classifier the artifact classifier
     * @return the Maven artifact coordinates
     */
    public static String toCoordinate(final String group_id, final String artifact_id, final String version, String classifier) {

        final StringBuilder builder = new StringBuilder();
        builder.append(group_id).append(COLON).append(artifact_id);
        if (classifier != null) {
            builder.append(COLON).append("jar").append(COLON).append(classifier);
        }
        builder.append(COLON).append(version);
        return builder.toString();
    }

    /**
     * Constructs Maven artifact coordinates from group ID, artifact ID and version.
     *
     * @param group_id the group ID
     * @param artifact_id the artifact ID
     * @param version the artifact version
     * @return the Maven artifact coordinates
     */
    public static String toCoordinate(final String group_id, final String artifact_id, final String version) {

        return toCoordinate(group_id, artifact_id, version, null);
    }

    /**
     * Resolves dependencies of a given Maven artifact coordinates as a list of local Jar files.
     *
     * @param artifact_coordinates the artifact coordinates
     * @return the dependency jar files of the given artifact coordinate
     * @throws DependencyCollectionException if unable to collect dependencies
     * @throws DependencyResolutionException if unable to resolve dependencies
     */
    public List<File> resolve(String artifact_coordinates) throws DependencyCollectionException, DependencyResolutionException {

        return resolve(new DefaultArtifact(artifact_coordinates));
    }

    /**
     * Resolves dependencies of a given Maven artifact as a list of local Jar files.
     *
     * @param artifact the artifact
     * @return the dependency jar files of the given artifact
     * @throws DependencyCollectionException if unable to collect dependencies
     * @throws DependencyResolutionException if unable to resolve dependencies
     */
    public List<File> resolve(final Artifact artifact) throws DependencyCollectionException, DependencyResolutionException {

        final List<DependencyNode> nodes = resolveAsDependencyNode(artifact);
        return getFiles(nodes);
    }

    /**
     * Resolves dependencies of a given Maven artifact as a list of remote URLs to Jar files.
     *
     * @param artifact the artifact
     * @return the dependency jar files of the given artifact as remote URLs
     * @throws DependencyCollectionException if unable to collect dependencies
     * @throws DependencyResolutionException if unable to resolve dependencies
     */
    public List<URL> resolveAsRemoteURLs(final Artifact artifact) throws DependencyCollectionException, DependencyResolutionException, IOException {

        final List<DependencyNode> nodes = resolveAsDependencyNode(artifact);
        return getRemoteURLs(nodes);
    }

    /**
     * Adds the given URL to the lis of Maven repositories used for dependency resolution.
     *
     * @param url the url
     * @return whether the url was added successfully
     * @throws MalformedURLException if the url is malformed
     */
    public boolean addRepository(final String url) throws MalformedURLException {

        return addRepository(new URL(url));
    }

    /**
     * Adds the given URL to the lis of Maven repositories used for dependency resolution.
     *
     * @param url the url
     * @return whether the url was added successfully
     * @throws MalformedURLException if the url is malformed
     */
    public boolean addRepository(final URL url) {

        return addRepository(toRemoteRepository(url));
    }

    /**
     * Adds the given repository to the lis of Maven repositories used for dependency resolution.
     *
     * @param repository the repository to add
     * @return whether the repository was added successfully
     */
    public boolean addRepository(final RemoteRepository repository) {

        return repositories.add(repository);
    }

    private List<DependencyNode> resolveAsDependencyNode(final Artifact artifact) throws DependencyCollectionException, DependencyResolutionException {

        final CollectResult collect_result = collectDependencies(artifact, repositories);
        final DependencyNode root_dependency = collect_result.getRoot();
        final DependencyRequest dep_request = new DependencyRequest();
        final DependencyNodeCollector node_collector = new DependencyNodeCollector();
        dep_request.setRoot(root_dependency);
        REPOSITORY_SYSTEM.resolveDependencies(session, dep_request);
        root_dependency.accept(node_collector);
        return node_collector.nodes;
    }

    private List<URL> getRemoteURLs(final List<DependencyNode> nodes) throws IOException {

        final List<URL> urls = new ArrayList<URL>();
        for (DependencyNode node : nodes) {

            final Artifact artifact = node.getArtifact();
            boolean added = false;

            for (RemoteRepository repository : node.getRepositories()) {
                if (!added) {
                    final URL url = constructRemoteURL(artifact, repository);
                    added |= URLUtils.ping(url, URL_PING_TIMEOUT_MILLIS) && urls.add(url);
                }
            }

            if (!added) { throw new IOException("unable to resolve the remote ural of artifact " + artifact); }
        }
        return urls;
    }

    private List<File> getFiles(final List<DependencyNode> nodes) {

        final List<File> files = new ArrayList<File>();
        for (DependencyNode node : nodes) {
            files.add(node.getArtifact().getFile());
        }
        return files;
    }

    private void addDefaultRepositories() {

        addRepository(MAVEN_CENTRAL_REPOSITORY);
        addRepository(ST_ANDREWS_CS_MAVEN_REPOSITORY);
    }

    private CollectResult collectDependencies(final Artifact artifact, final List<RemoteRepository> repositories) throws DependencyCollectionException {

        final CollectResult collectResult;
        final CollectRequest collectRequest = new CollectRequest();
        final Dependency root_dependency = new Dependency(artifact, "");
        collectRequest.setRoot(root_dependency);
        collectRequest.setRepositories(repositories);
        collectResult = REPOSITORY_SYSTEM.collectDependencies(session, collectRequest);
        return collectResult;
    }

    private static RemoteRepository toRemoteRepository(final URL url) {

        return new RemoteRepository.Builder(url.getHost(), DEFAULT_REPOSITORY_TYPE, url.toString()).build();
    }

    private static DefaultRepositorySystemSession createRepositorySystemSession(final RepositorySystem system, final File repository_home) {

        final DefaultRepositorySystemSession session = MavenRepositorySystemUtils.newSession();
        final LocalRepository local_repository = new LocalRepository(repository_home);
        session.setLocalRepositoryManager(system.newLocalRepositoryManager(session, local_repository));
        return session;
    }

    private static RepositorySystem createRepositorySystem() {

        final DefaultServiceLocator locator = MavenRepositorySystemUtils.newServiceLocator();
        locator.addService(RepositoryConnectorFactory.class, BasicRepositoryConnectorFactory.class);
        locator.addService(TransporterFactory.class, HttpTransporterFactory.class);
        locator.addService(TransporterFactory.class, FileTransporterFactory.class);
        locator.setErrorHandler(DEFAULT_ERROR_HANDLER);

        return locator.getService(RepositorySystem.class);
    }

    private URL constructRemoteURL(final Artifact artifact, final RemoteRepository repository) throws MalformedURLException {

        final StringBuilder url_as_string = new StringBuilder();
        final String artifact_id = artifact.getArtifactId();
        final String repo_url = repository.getUrl();
        url_as_string.append(repo_url.endsWith("/") ? repo_url : repo_url + "/");
        url_as_string.append(artifact.getGroupId().replaceAll("\\.", "/"));
        url_as_string.append("/");
        url_as_string.append(artifact_id);
        url_as_string.append("/");
        url_as_string.append(artifact.getBaseVersion());
        url_as_string.append("/");
        url_as_string.append(artifact_id);
        url_as_string.append("-");
        url_as_string.append(artifact.getVersion());
        url_as_string.append(".");
        url_as_string.append(artifact.getExtension());

        return new URL(url_as_string.toString());
    }

    static class ServiceLocatorErrorHandler extends DefaultServiceLocator.ErrorHandler {

        @Override
        public void serviceCreationFailed(Class<?> type, Class<?> impl, Throwable exception) {

            //TODO do decent logging into /tmp/shabdiz/log/error.log
            exception.printStackTrace();
        }
    }

    static final class FileCollector implements DependencyVisitor {

        private final List<File> files;

        FileCollector() {

            files = new ArrayList<File>();
        }

        public boolean visitEnter(final DependencyNode node) {

            final Artifact artifact = node.getArtifact();
            final File artifact_file = artifact.getFile();
            return files.add(artifact_file);
        }

        public boolean visitLeave(final DependencyNode node) {

            return true;
        }
    }

    static final class DependencyNodeCollector implements DependencyVisitor {

        private final List<DependencyNode> nodes;

        DependencyNodeCollector() {

            nodes = new ArrayList<DependencyNode>();
        }

        public boolean visitEnter(final DependencyNode node) {

            return nodes.add(node);
        }

        public boolean visitLeave(final DependencyNode node) {

            return true;
        }
    }
}
