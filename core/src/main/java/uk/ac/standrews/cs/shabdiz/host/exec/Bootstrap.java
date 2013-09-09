package uk.ac.standrews.cs.shabdiz.host.exec;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.instrument.Instrumentation;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.jar.Attributes;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;

/** @author Masih Hajiarabderkani (mh638@st-andrews.ac.uk) */
public abstract class Bootstrap {

    static final String SHABDIZ_HOME_NAME = "shabdiz";
    static final String BOOTSTRAP_HOME_NAME = ".bootstrap";
    static final String TEMP_HOME_NAME = "tmp";
    static final File LOCAL_SHABDIZ_HOME = new File(System.getProperty("java.io.tmpdir"), SHABDIZ_HOME_NAME);
    static final File LOCAL_BOOTSTRAP_HOME = new File(LOCAL_SHABDIZ_HOME, BOOTSTRAP_HOME_NAME);
    static final File LOCAL_SHABDIZ_TMP_HOME = new File(LOCAL_SHABDIZ_HOME, TEMP_HOME_NAME);
    static final String BOOTSTRAP_JAR_NAME = "bootstrap.jar";
    private static final File BOOTSTRAP_JAR = new File(LOCAL_BOOTSTRAP_HOME, BOOTSTRAP_JAR_NAME);
    private static final String MVN_CENTRAL = "http://repo1.maven.org/maven2/";
    private static final String SEPARATOR = "\t";
    private static final String CONFIG_FILE_ATTRIBUTES_NAME = "Shabdiz";
    private static final Attributes.Name CLASSPATH_FILES = new Attributes.Name("Class-Path-Files");
    private static final Attributes.Name CLASSPATH_URLS = new Attributes.Name("Class-Path-URLs");
    private static final Attributes.Name MAVEN_REPOSITORIES = new Attributes.Name("Maven-Repositories");
    private static final Attributes.Name MAVEN_ARTIFACTS = new Attributes.Name("Maven-Atifacts");
    private static final Attributes.Name BOOTSTRAP_CLASS_KEY = new Attributes.Name("Application-Bootstrap-Class");
    private static final Attributes.Name PREMAIN_CLASS = new Attributes.Name("Premain-Class");
    private static final String WORKING_DIRECTORY = System.getProperty("user.dir");
    private static MavenDependencyResolver maven_dependency_resolver;
    private static String application_bootstrap_class_name;

    public static void main(String[] args) throws Exception {

        final Class<?> application_bootstrap_class = Class.forName(application_bootstrap_class_name);
        if (Bootstrap.class.isAssignableFrom(application_bootstrap_class)) {

            final Bootstrap bootstrap = (Bootstrap) application_bootstrap_class.newInstance();
            bootstrap.deploy(args);
        }
        else {
            application_bootstrap_class.getMethod("main", String[].class).invoke(null, new Object[]{args});
        }
        //TODO print properties
    }

    protected abstract void deploy(String... args);

    public static void premain(String path_to_config_file, Instrumentation instrumentation) throws Exception {

        if (path_to_config_file == null) { throw new IllegalArgumentException("unspecified config file in bootstrap agent"); }

        final FileInputStream in = new FileInputStream(path_to_config_file);
        final BootstrapConfiguration configuration = BootstrapConfiguration.read(in);

        if (configuration.hasMavenArtifact()) {
            initMavenDependencyResolver(instrumentation);
            addMavenRepositories(configuration.maven_repositories);
            loadMavenArtifacts(instrumentation, configuration.maven_artifacts);
        }
        loadClassPathFiles(instrumentation, configuration.files);
        loadClassPathUrls(instrumentation, configuration.urls);
        application_bootstrap_class_name = configuration.application_bootstrap_class_name;
    }

    private static void loadClassPathFiles(final Instrumentation instrumentation, final Set<String> files) throws IOException {

        for (String file : files) {
            loadClassPathFile(instrumentation, file);
        }
    }

    private static void loadClassPathFile(final Instrumentation instrumentation, final String file) throws IOException {

        loadClassPathJAR(instrumentation, new JarFile(file));
    }

    synchronized static File getBootstrapJar(boolean force_reconstruction) throws IOException {

        if (force_reconstruction || !BOOTSTRAP_JAR.isFile()) {
            reconstructBootstrapJar();
        }
        return BOOTSTRAP_JAR;

    }

    private static void reconstructBootstrapJar() throws IOException {

        final Manifest manifest = new Manifest();
        final Attributes main_attributes = manifest.getMainAttributes();
        main_attributes.put(Attributes.Name.MANIFEST_VERSION, "1.0");
        main_attributes.put(Attributes.Name.MAIN_CLASS, Bootstrap.class.getName());
        main_attributes.put(Attributes.Name.CLASS_PATH, ".:*");
        main_attributes.put(PREMAIN_CLASS, Bootstrap.class.getName());
        FileUtils.forceMkdir(LOCAL_BOOTSTRAP_HOME);
        final JarOutputStream jar_stream = new JarOutputStream(new FileOutputStream(BOOTSTRAP_JAR), manifest);

        try {
            addClassToJar(MavenDependencyResolver.class, jar_stream);
            addClassToJar(MavenDependencyResolver.URLCollector.class, jar_stream);
            addClassToJar(Bootstrap.class, jar_stream);
            addClassToJar(BootstrapConfiguration.class, jar_stream);
        }
        finally {
            jar_stream.flush();
            jar_stream.close();
        }
    }

    protected static void addClassToJar(final Class<?> type, final JarOutputStream jar) throws IOException {

        final String resource_path = getResourcePath(type);
        final JarEntry entry = new JarEntry(resource_path);
        final InputStream resource_stream = getResurceInputStream(type, resource_path);
        jar.putNextEntry(entry);
        IOUtils.copy(resource_stream, jar);
        jar.closeEntry();
    }

    private static String getResourcePath(final Class<?> type) {

        return type.getName().replaceAll("\\.", "/") + ".class";
    }

    private static InputStream getResurceInputStream(final Class<?> type, final String resource_path) throws IOException {

        final ClassLoader class_loader = Thread.currentThread().getContextClassLoader();
        final InputStream resource_stream = class_loader.getResourceAsStream(resource_path);
        if (resource_stream != null) { return resource_stream; }
        throw new IOException("unable to locate resource " + type);
    }

    private static void loadMavenArtifacts(final Instrumentation instrumentation, final Set<String> maven_artifacts) throws Exception {

        for (String artifact : maven_artifacts) {
            final List<URL> resolved_urls = maven_dependency_resolver.resolve(artifact);
            loadClassPathUrls(instrumentation, resolved_urls);
        }
    }

    private static void loadClassPathUrls(final Instrumentation instrumentation, final Collection<URL> urls) throws URISyntaxException, IOException {

        for (URL url : urls) {
            loadClassPathURL(instrumentation, url);
        }
    }

    private static synchronized void initMavenDependencyResolver(Instrumentation instrumentation) {

        if (maven_dependency_resolver != null) { return; }
        loadEclipseAetherDependencies(instrumentation);
        maven_dependency_resolver = new MavenDependencyResolver();
    }

    private static synchronized void loadEclipseAetherDependencies(final Instrumentation instrumentation) {

        assert maven_dependency_resolver == null;
        try {
            loadClassPathURL(instrumentation, new URL(MVN_CENTRAL + "org/eclipse/aether/aether-api/0.9.0.M2/aether-api-0.9.0.M2.jar"));
            loadClassPathURL(instrumentation, new URL(MVN_CENTRAL + "org/eclipse/aether/aether-impl/0.9.0.M2/aether-impl-0.9.0.M2.jar"));
            loadClassPathURL(instrumentation, new URL(MVN_CENTRAL + "org/eclipse/aether/aether-util/0.9.0.M2/aether-util-0.9.0.M2.jar"));
            loadClassPathURL(instrumentation, new URL(MVN_CENTRAL + "org/eclipse/aether/aether-connector-file/0.9.0.M2/aether-connector-file-0.9.0.M2.jar"));
            loadClassPathURL(instrumentation, new URL(MVN_CENTRAL + "org/eclipse/aether/aether-connector-asynchttpclient/0.9.0.M2/aether-connector-asynchttpclient-0.9.0.M2.jar"));
            loadClassPathURL(instrumentation, new URL(MVN_CENTRAL + "org/eclipse/aether/aether-spi/0.9.0.M2/aether-spi-0.9.0.M2.jar"));
            loadClassPathURL(instrumentation, new URL(MVN_CENTRAL + "com/ning/async-http-client/1.7.6/async-http-client-1.7.6.jar"));
            loadClassPathURL(instrumentation, new URL(MVN_CENTRAL + "io/netty/netty/3.4.4.Final/netty-3.4.4.Final.jar"));
            loadClassPathURL(instrumentation, new URL(MVN_CENTRAL + "org/slf4j/slf4j-api/1.7.5/slf4j-api-1.7.5.jar"));
            loadClassPathURL(instrumentation, new URL(MVN_CENTRAL + "io/tesla/maven/maven-aether-provider/3.1.0/maven-aether-provider-3.1.0.jar"));
            loadClassPathURL(instrumentation, new URL(MVN_CENTRAL + "io/tesla/maven/maven-model/3.1.0/maven-model-3.1.0.jar"));
            loadClassPathURL(instrumentation, new URL(MVN_CENTRAL + "io/tesla/maven/maven-model-builder/3.1.0/maven-model-builder-3.1.0.jar"));
            loadClassPathURL(instrumentation, new URL(MVN_CENTRAL + "org/codehaus/plexus/plexus-interpolation/1.16/plexus-interpolation-1.16.jar"));
            loadClassPathURL(instrumentation, new URL(MVN_CENTRAL + "io/tesla/maven/maven-repository-metadata/3.1.0/maven-repository-metadata-3.1.0.jar"));
            loadClassPathURL(instrumentation, new URL(MVN_CENTRAL + "org/eclipse/sisu/org.eclipse.sisu.plexus/0.0.0.M2a/org.eclipse.sisu.plexus-0.0.0.M2a.jar"));
            loadClassPathURL(instrumentation, new URL(MVN_CENTRAL + "javax/enterprise/cdi-api/1.0/cdi-api-1.0.jar"));
            loadClassPathURL(instrumentation, new URL(MVN_CENTRAL + "javax/annotation/jsr250-api/1.0/jsr250-api-1.0.jar"));
            loadClassPathURL(instrumentation, new URL(MVN_CENTRAL + "javax/inject/javax.inject/1/javax.inject-1.jar"));
            loadClassPathURL(instrumentation, new URL(MVN_CENTRAL + "com/google/guava/guava/10.0.1/guava-10.0.1.jar"));
            loadClassPathURL(instrumentation, new URL(MVN_CENTRAL + "com/google/code/findbugs/jsr305/1.3.9/jsr305-1.3.9.jar"));
            loadClassPathURL(instrumentation, new URL(MVN_CENTRAL + "org/sonatype/sisu/sisu-guice/3.1.0/sisu-guice-3.1.0-no_aop.jar"));
            loadClassPathURL(instrumentation, new URL(MVN_CENTRAL + "aopalliance/aopalliance/1.0/aopalliance-1.0.jar"));
            loadClassPathURL(instrumentation, new URL(MVN_CENTRAL + "org/eclipse/sisu/org.eclipse.sisu.inject/0.0.0.M2a/org.eclipse.sisu.inject-0.0.0.M2a.jar"));
            loadClassPathURL(instrumentation, new URL(MVN_CENTRAL + "asm/asm/3.3.1/asm-3.3.1.jar"));
            loadClassPathURL(instrumentation, new URL(MVN_CENTRAL + "org/codehaus/plexus/plexus-classworlds/2.4/plexus-classworlds-2.4.jar"));
            loadClassPathURL(instrumentation, new URL(MVN_CENTRAL + "org/codehaus/plexus/plexus-component-annotations/1.5.5/plexus-component-annotations-1.5.5.jar"));
            loadClassPathURL(instrumentation, new URL(MVN_CENTRAL + "org/codehaus/plexus/plexus-utils/3.0.10/plexus-utils-3.0.10.jar"));
        }
        catch (final Exception e) {
            throw new RuntimeException("failed to load eclipse eather dependencies", e);
        }
    }

    private static void loadClassPathURL(Instrumentation instrumentation, URL url) throws URISyntaxException, IOException {

        final File url_as_file;
        if (!isFile(url)) {
            url_as_file = copyUrlToWorkingDirectory(url);
        }
        else {
            url_as_file = new File(url.toURI());
        }

        final JarFile url_as_jar = new JarFile(url_as_file);
        loadClassPathJAR(instrumentation, url_as_jar);
    }

    private static File copyUrlToWorkingDirectory(final URL url) throws IOException {

        final File url_as_file;
        final String url_file = url.getFile();
        final String url_file_name = url_file.substring(url_file.lastIndexOf('/') + 1);
        url_as_file = new File(WORKING_DIRECTORY, url_file_name + ".jar");
        ReadableByteChannel byte_channel = null;
        FileOutputStream out = null;
        try {
            byte_channel = Channels.newChannel(url.openStream());
            out = new FileOutputStream(url_as_file);
            out.getChannel().transferFrom(byte_channel, 0, Long.MAX_VALUE);
            out.close();
        }
        finally {
            if (byte_channel != null) {
                byte_channel.close();
            }
            if (out != null) {
                out.close();
            }
        }
        return url_as_file;
    }

    private static void loadClassPathJAR(final Instrumentation instrumentation, final JarFile jar) {

        instrumentation.appendToSystemClassLoaderSearch(jar);
    }

    private static void addMavenRepositories(final Set<URL> repositories) {

        for (URL repository : repositories) {
            maven_dependency_resolver.addRepository(repository);
        }
    }

    private static boolean isFile(final URL url) {

        return url.getProtocol().equals("file:");
    }

    static class BootstrapConfiguration {

        private final Set<String> files = new HashSet<String>();
        private final Set<URL> urls = new HashSet<URL>();
        private final Set<URL> maven_repositories = new HashSet<URL>();
        private final Set<String> maven_artifacts = new HashSet<String>();
        private volatile String application_bootstrap_class_name;

        void write(OutputStream out) throws IOException {

            final Manifest manifest = toManifest();
            manifest.write(out);
        }

        private Manifest toManifest() {

            final Manifest manifest = new Manifest();
            final Attributes attributes = new Attributes();

            attributes.put(BOOTSTRAP_CLASS_KEY, application_bootstrap_class_name);
            attributes.put(CLASSPATH_FILES, toString(files, SEPARATOR));
            attributes.put(CLASSPATH_URLS, toString(urls, SEPARATOR));
            attributes.put(MAVEN_REPOSITORIES, toString(maven_repositories, SEPARATOR));
            attributes.put(MAVEN_ARTIFACTS, toString(maven_artifacts, SEPARATOR));
            manifest.getEntries().put(CONFIG_FILE_ATTRIBUTES_NAME, attributes);
            return manifest;
        }

        private static String toString(Collection<?> collection, String delimiter) {

            final StringBuilder string_builder = new StringBuilder();
            for (Object element : collection) {
                string_builder.append(element);
                string_builder.append(delimiter);
            }

            return string_builder.toString().trim();
        }

        static BootstrapConfiguration read(InputStream in) throws IOException {

            final Manifest manifest = new Manifest(new BufferedInputStream(in));
            return fromManifest(manifest);
        }

        private static BootstrapConfiguration fromManifest(Manifest manifest) throws MalformedURLException {

            final BootstrapConfiguration configuration = new BootstrapConfiguration();
            final Attributes attributes = manifest.getAttributes(CONFIG_FILE_ATTRIBUTES_NAME);
            final String bootstrap = attributes.get(BOOTSTRAP_CLASS_KEY).toString();

            configuration.setApplicationBootstrapClassName(bootstrap);

            final String[] files = attributes.get(CLASSPATH_FILES).toString().split(SEPARATOR);
            for (String file : files) {
                if (!file.trim().isEmpty()) {
                    configuration.addClassPathFile(file);
                }
            }
            final String[] urls = attributes.get(CLASSPATH_URLS).toString().split(SEPARATOR);
            for (String url : urls) {
                if (!url.trim().isEmpty()) {
                    configuration.addClassPathURL(new URL(url));
                }
            }

            final String[] repos = attributes.get(MAVEN_REPOSITORIES).toString().split(SEPARATOR);
            for (String repo : repos) {
                if (!repo.trim().isEmpty()) {
                    configuration.addMavenRepository(new URL(repo));
                }
            }

            final String[] mvns = attributes.get(MAVEN_ARTIFACTS).toString().split(SEPARATOR);
            for (String artifact_coordinate : mvns) {
                if (!artifact_coordinate.trim().isEmpty()) {
                    configuration.addMavenArtifact(artifact_coordinate);
                }
            }

            return configuration;
        }

        void setApplicationBootstrapClass(Class<?> bootstrap_class) {

            application_bootstrap_class_name = bootstrap_class.getName();
        }

        boolean addClassPathURL(URL url) {

            return urls.add(url);
        }

        boolean addMavenArtifact(String artifact_coordinate) {

            return maven_artifacts.add(artifact_coordinate);
        }

        boolean addMavenRepository(URL url) {

            return maven_repositories.add(url);
        }

        boolean addClassPathFile(String path) {

            return files.add(path);
        }

        void setApplicationBootstrapClassName(String class_name) {

            application_bootstrap_class_name = class_name;
        }

        boolean hasMavenArtifact() {

            return !maven_artifacts.isEmpty();
        }
    }
}
