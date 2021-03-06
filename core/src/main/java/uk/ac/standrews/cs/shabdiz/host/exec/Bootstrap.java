package uk.ac.standrews.cs.shabdiz.host.exec;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.lang.instrument.Instrumentation;
import java.lang.management.ManagementFactory;
import java.lang.management.RuntimeMXBean;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;
import java.util.jar.Attributes;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import uk.ac.standrews.cs.shabdiz.util.Duration;
import uk.ac.standrews.cs.shabdiz.util.TimeoutExecutorService;
import uk.ac.standrews.cs.shabdiz.util.URLUtils;

/**
 * A Java agent that is capable of adding jar to the JVM class path at runtime.
 * The jar files may be specified as Maven artifacts, URLs and local files in a configuration file called {@code bootstrap.config}.
 * The bootstrap configuration file must be available in the class path for this class to work.
 * This class automatically deletes the temporary directory at which it finds the bootstrap configuration file.
 * Note by: this class is not designed to be used independently to resolve classpath entries.
 * To start a Java process using this bootstrap mechanism please use {@link AgentBasedJavaProcessBuilder}
 * If a sub class of this class overrides the {@code main} method, the {@link #printProperties()} method must be called at the end of main method.
 *
 * @author Masih Hajiarabderkani (mh638@st-andrews.ac.uk)
 * @see AgentBasedJavaProcessBuilder
 */
public abstract class Bootstrap {

    /** The key of PID property. */
    public static final String PID_PROPERTY_KEY = "pid";
    protected static final char NEW_LINE = '\n';
    protected static final char LINE_FEED = '\r';
    static final String SHABDIZ_HOME_NAME = "shabdiz";
    static final String BOOTSTRAP_HOME_NAME = ".bootstrap";
    static final String TEMP_HOME_NAME = "tmp";
    static final File LOCAL_SHABDIZ_HOME = new File(System.getProperty("java.io.tmpdir"), SHABDIZ_HOME_NAME);
    static final File LOCAL_BOOTSTRAP_HOME = new File(LOCAL_SHABDIZ_HOME, BOOTSTRAP_HOME_NAME);
    static final File LOCAL_SHABDIZ_TMP_HOME = new File(LOCAL_SHABDIZ_HOME, TEMP_HOME_NAME);
    static final String BOOTSTRAP_JAR_NAME = "bootstrap.jar";
    private static final String PROCESS_OUTPUT_ENCODING = "UTF-8";
    private static final File BOOTSTRAP_JAR = new File(LOCAL_BOOTSTRAP_HOME, BOOTSTRAP_JAR_NAME);
    private static final String MVN_CENTRAL = "http://central.maven.org/maven2/";
    private static final String SEPARATOR = "\t";
    private static final String CONFIG_FILE_ATTRIBUTES_NAME = "Shabdiz";
    private static final Attributes.Name DELETE_WD_ON_EXIT = new Attributes.Name("Delete-Working-Directory-On-Exit");
    private static final Attributes.Name CLASSPATH_FILES = new Attributes.Name("Class-Path-Files");
    private static final Attributes.Name CLASSPATH_URLS = new Attributes.Name("Class-Path-URLs");
    private static final Attributes.Name MAVEN_REPOSITORIES = new Attributes.Name("Maven-Repositories");
    private static final Attributes.Name MAVEN_ARTIFACTS = new Attributes.Name("Maven-Artifacts");
    private static final Attributes.Name BOOTSTRAP_CLASS_KEY = new Attributes.Name("Application-Bootstrap-Class");
    private static final Attributes.Name PREMAIN_CLASS = new Attributes.Name("Premain-Class");
    private static final Attributes.Name FILES_TO_DELETE_ON_EXIT = new Attributes.Name("Delete-On-Exit");
    private static final File WORKING_DIRECTORY = new File(System.getProperty("user.dir"));
    private static final String FILE_PROTOCOL = "file";
    private static final Pattern KEY_VALUE_PATTERN = Pattern.compile("(.?[^=]+)=(.?[^=,]+)(,\\s)?");
    private static MavenDependencyResolver maven_dependency_resolver;
    private static String application_bootstrap_class_name;
    private static File tmp_dir;
    private final Properties properties;

    protected Bootstrap() {

        properties = new Properties();

        setDefaultProperties();
    }

    public static Integer getPIDProperty(final Properties properties) {

        final String pid_as_string = properties.getProperty(PID_PROPERTY_KEY);
        return pid_as_string != null ? Integer.parseInt(pid_as_string) : null;
    }

    public static void main(final String[] args) throws Exception {

        final Class<?> application_bootstrap_class = Class.forName(application_bootstrap_class_name);
        if (Bootstrap.class.isAssignableFrom(application_bootstrap_class)) {

            final Bootstrap bootstrap = (Bootstrap) application_bootstrap_class.newInstance();
            bootstrap.deploy(args);
            bootstrap.printProperties();
        }
        else {
            application_bootstrap_class.getMethod("main", String[].class).invoke(null, new Object[] {args});
        }
    }

    public static void premain(final String args, final Instrumentation instrumentation) throws Exception {

        final BootstrapConfiguration configuration = getConfigurationFromFile();
        loadMavenArtifacts(instrumentation, configuration);
        loadClassPathFiles(instrumentation, configuration.files);
        loadClassPathUrlsAsString(instrumentation, configuration.urls);
        loadShutdownHooks(configuration);
        loadApplicationBootstrapClassName(configuration);
    }

    public static String readLine(final InputStream in) throws IOException {

        //TODO tidy this up.
        // jdk scanner is evil.
        int next_byte;
        final StringBuilder builder = new StringBuilder();
        while ((next_byte = in.read()) != -1) {

            final char next_char = (char) next_byte;
            if (next_char == NEW_LINE || next_char == LINE_FEED) { //TODO investigate whether echo prints \n at the end on all platforms. is line separator platform dependant?
                break;
            }
            builder.append(next_char);
        }
        if (next_byte == -1) { throw new IOException("eof"); }
        return builder.toString();
    }

    public static Properties readProperties(final Class<?> bootstrap_class, final Process process, final Duration timeout) throws ExecutionException, InterruptedException, TimeoutException {

        return readProperties(bootstrap_class, process, timeout, false);
    }

    public static Properties readProperties(final Class<?> bootstrap_class, final Process process, final Duration timeout, final boolean print_lines) throws ExecutionException, InterruptedException, TimeoutException {

        final String properties_id = getPropertiesID(bootstrap_class);
        final Callable<Properties> scan_task = newProcessOutputScannerTask(process.getInputStream(), properties_id, print_lines);
        return TimeoutExecutorService.awaitCompletion(scan_task, timeout.getLength(), timeout.getTimeUnit());
    }

    private static BootstrapConfiguration getConfigurationFromFile() throws IOException, URISyntaxException {

        tmp_dir = new File(ClassLoader.getSystemResource("bootstrap.config").toURI()).getParentFile();
        final InputStream in = ClassLoader.getSystemResourceAsStream("bootstrap.config");
        final BootstrapConfiguration configuration = BootstrapConfiguration.read(in);
        in.close();
        if (configuration.delete_working_directory_on_exit) {
            configuration.addFileToDeleteOnExit(tmp_dir.getAbsolutePath());
        }
        return configuration;
    }

    private static void loadMavenArtifacts(final Instrumentation instrumentation, final BootstrapConfiguration configuration) throws Exception {

        if (configuration.hasMavenArtifact()) {
            initMavenDependencyResolver(instrumentation);
            addMavenRepositories(configuration.maven_repositories);
            resolveAndLoadMavenArtifacts(instrumentation, configuration.maven_artifacts);
        }
    }

    private static void loadApplicationBootstrapClassName(final BootstrapConfiguration configuration) {

        application_bootstrap_class_name = configuration.application_bootstrap_class_name;
    }

    private static void loadShutdownHooks(final BootstrapConfiguration configuration) {

        final List<File> files_to_delete = new ArrayList<File>();
        if (configuration.delete_working_directory_on_exit) {
            files_to_delete.add(WORKING_DIRECTORY);
        }
        for (final String file : configuration.delete_on_exit) {
            files_to_delete.add(new File(file));
        }

        if (!files_to_delete.isEmpty()) {
            final FileDeletionHook file_deletion_hook = new FileDeletionHook(files_to_delete);
            Runtime.getRuntime().addShutdownHook(file_deletion_hook);
        }
    }

    private void setDefaultProperties() {

        setProperty(PID_PROPERTY_KEY, getPIDFromRuntimeMXBeanName());
    }

    protected Object setProperty(final Object key, final Object value) {

        return setProperty(String.valueOf(key), String.valueOf(value));
    }

    protected Object setProperty(final String key, final String value) {

        try {
            return properties.setProperty(URLEncoder.encode(key, PROCESS_OUTPUT_ENCODING), URLEncoder.encode(value, PROCESS_OUTPUT_ENCODING));
        }
        catch (UnsupportedEncodingException e) {
            throw new RuntimeException("failed to encode property", e);
        }
    }

    /**
     * Attempts to get a PID from a given runtime MXBean name.
     * The expected format is {@code <pid>@<machine_name>}.
     * Returns {@code null} if the given MXBean name does not match the above pattern.
     *
     * @return the pid from the given name or {@code null} if the name does not match the expected pattern
     * @see RuntimeMXBean#getName()
     */
    private static Integer getPIDFromRuntimeMXBeanName() {

        final String runtime_mxbean_name = ManagementFactory.getRuntimeMXBean().getName();
        Integer pid = null;
        final int index_of_at = runtime_mxbean_name.indexOf("@");
        if (index_of_at != -1) {
            pid = Integer.parseInt(runtime_mxbean_name.substring(0, index_of_at));
        }
        return pid;
    }

    protected abstract void deploy(String... args) throws Exception;

    protected void printProperties() {

        final String properties_as_string = getPropertiesAsString();
        System.out.println(properties_as_string);
        System.out.flush();
    }

    private String getPropertiesAsString() {

        final StringBuilder properties_as_string = new StringBuilder();
        properties_as_string.append(getPropertiesID(getClass()));
        properties_as_string.append(properties);
        return properties_as_string.toString();
    }

    private static String getPropertiesID(final Class<?> bootstrap_class) {

        return bootstrap_class.getName();
    }

    static Callable<Properties> newProcessOutputScannerTask(final InputStream in, final String properties_id) {

        return newProcessOutputScannerTask(in, properties_id, false);
    }

    static Callable<Properties> newProcessOutputScannerTask(final InputStream in, final String properties_id, final boolean print_lines) {

        return new Callable<Properties>() {

            @Override
            public Properties call() throws Exception {

                final Pattern pattern = Pattern.compile(Pattern.quote(properties_id) + "\\{(.*)?\\}");
                while (!Thread.currentThread().isInterrupted()) {
                    final String line = readLine(in);
                    final Matcher matcher = pattern.matcher(line);
                    if (matcher.matches()) {
                        final String key_values = matcher.group(1);
                        return parseProperties(key_values);
                    }
                    if (print_lines) {
                        System.out.println(line);
                    }
                }
                throw new InterruptedException();
            }

            private Properties parseProperties(final String key_values) throws UnsupportedEncodingException {

                final Properties properties;
                if (key_values != null) {
                    properties = new Properties();
                    final Matcher key_value_matcher = KEY_VALUE_PATTERN.matcher(key_values);
                    while (key_value_matcher.find()) {
                        final String key = URLDecoder.decode(key_value_matcher.group(1), PROCESS_OUTPUT_ENCODING);
                        final String value = URLDecoder.decode(key_value_matcher.group(2), PROCESS_OUTPUT_ENCODING);
                        properties.setProperty(key, value);
                    }
                }
                else {
                    properties = null;
                }
                return properties;
            }
        };
    }

    private static void loadClassPathFiles(final Instrumentation instrumentation, final Set<String> files) throws IOException {

        for (final String file : files) {
            loadClassPathFile(instrumentation, file);
        }
    }

    private static void loadClassPathFile(final Instrumentation instrumentation, final String file) throws IOException {

        loadClassPathJAR(instrumentation, new JarFile(file));
    }

    static synchronized File getBootstrapJar(final boolean force_reconstruction) throws IOException {

        if (force_reconstruction || !BOOTSTRAP_JAR.isFile()) {
            reconstructBootstrapJar();
        }
        assert BOOTSTRAP_JAR.isFile();
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
            addClassToJar(MavenDependencyResolver.FileCollector.class, jar_stream);
            addClassToJar(MavenDependencyResolver.DependencyNodeCollector.class, jar_stream);
            addClassToJar(MavenDependencyResolver.ServiceLocatorErrorHandler.class, jar_stream);
            addClassToJar(Bootstrap.class, jar_stream);
            addClassToJar(BootstrapConfiguration.class, jar_stream);
            addClassToJar(Duration.class, jar_stream);
            addClassToJar(URLUtils.class, jar_stream);
            addClassToJar(FileDeletionHook.class, jar_stream);
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

    private static void resolveAndLoadMavenArtifacts(final Instrumentation instrumentation, final Set<String> maven_artifacts) throws Exception {

        for (final String artifact : maven_artifacts) {
            final List<File> resolved_urls = maven_dependency_resolver.resolve(artifact);
            loadClassPathFiles(instrumentation, resolved_urls);
        }
    }

    private static void loadClassPathUrlsAsString(final Instrumentation instrumentation, final Collection<String> urls) throws URISyntaxException, IOException {

        for (final String url : urls) {
            loadClassPathURL(instrumentation, new URL(url));
        }
    }

    private static void loadClassPathFiles(final Instrumentation instrumentation, final Collection<File> files) throws IOException {

        for (final File file : files) {
            loadClassPathFile(instrumentation, file);
        }
    }

    private static synchronized void initMavenDependencyResolver(final Instrumentation instrumentation) {

        if (maven_dependency_resolver != null) { return; }
        loadEclipseAetherDependencies(instrumentation);
        maven_dependency_resolver = new MavenDependencyResolver();
    }

    private static synchronized void loadEclipseAetherDependencies(final Instrumentation instrumentation) {

        assert maven_dependency_resolver == null;

        try {
            loadBootstrapClassPathURL(instrumentation, new URL(MVN_CENTRAL + "org/apache/httpcomponents/httpclient/4.2.5/httpclient-4.2.5.jar"));
            loadBootstrapClassPathURL(instrumentation, new URL(MVN_CENTRAL + "org/apache/httpcomponents/httpcore/4.2.5/httpcore-4.2.5.jar"));
            loadBootstrapClassPathURL(instrumentation, new URL(MVN_CENTRAL + "org/apache/maven/maven-aether-provider/3.1.1/maven-aether-provider-3.1.1.jar"));
            loadBootstrapClassPathURL(instrumentation, new URL(MVN_CENTRAL + "org/apache/maven/maven-model-builder/3.1.1/maven-model-builder-3.1.1.jar"));
            loadBootstrapClassPathURL(instrumentation, new URL(MVN_CENTRAL + "org/apache/maven/maven-model/3.1.1/maven-model-3.1.1.jar"));
            loadBootstrapClassPathURL(instrumentation, new URL(MVN_CENTRAL + "org/apache/maven/maven-repository-metadata/3.1.1/maven-repository-metadata-3.1.1.jar"));
            loadBootstrapClassPathURL(instrumentation, new URL(MVN_CENTRAL + "org/codehaus/plexus/plexus-interpolation/1.19/plexus-interpolation-1.19.jar"));
            loadBootstrapClassPathURL(instrumentation, new URL(MVN_CENTRAL + "org/codehaus/plexus/plexus-utils/3.0.15/plexus-utils-3.0.15.jar"));
            loadBootstrapClassPathURL(instrumentation, new URL(MVN_CENTRAL + "org/eclipse/aether/aether-api/0.9.0.M3/aether-api-0.9.0.M3.jar"));
            loadBootstrapClassPathURL(instrumentation, new URL(MVN_CENTRAL + "org/eclipse/aether/aether-spi/0.9.0.M3/aether-spi-0.9.0.M3.jar"));
            loadBootstrapClassPathURL(instrumentation, new URL(MVN_CENTRAL + "org/eclipse/aether/aether-impl/0.9.0.M3/aether-impl-0.9.0.M3.jar"));
            loadBootstrapClassPathURL(instrumentation, new URL(MVN_CENTRAL + "org/eclipse/aether/aether-util/0.9.0.M3/aether-util-0.9.0.M3.jar"));
            loadBootstrapClassPathURL(instrumentation, new URL(MVN_CENTRAL + "org/eclipse/aether/aether-connector-basic/0.9.0.M3/aether-connector-basic-0.9.0.M3.jar"));
            loadBootstrapClassPathURL(instrumentation, new URL(MVN_CENTRAL + "org/eclipse/aether/aether-transport-file/0.9.0.M3/aether-transport-file-0.9.0.M3.jar"));
            loadBootstrapClassPathURL(instrumentation, new URL(MVN_CENTRAL + "org/eclipse/aether/aether-transport-http/0.9.0.M3/aether-transport-http-0.9.0.M3.jar"));
            loadBootstrapClassPathURL(instrumentation, new URL(MVN_CENTRAL + "org/slf4j/slf4j-api/1.7.6/slf4j-api-1.7.6.jar"));
            loadBootstrapClassPathURL(instrumentation, new URL(MVN_CENTRAL + "org/slf4j/jcl-over-slf4j/1.7.6/jcl-over-slf4j-1.7.6.jar"));
            loadBootstrapClassPathURL(instrumentation, new URL(MVN_CENTRAL + "ch/qos/logback/logback-core/1.1.1/logback-core-1.1.1.jar"));
            loadBootstrapClassPathURL(instrumentation, new URL(MVN_CENTRAL + "ch/qos/logback/logback-classic/1.1.1/logback-classic-1.1.1.jar"));
        }
        catch (final Exception e) {
            throw new RuntimeException("failed to load eclipse aether dependencies", e);
        }
    }

    private static void loadBootstrapClassPathURL(final Instrumentation instrumentation, final URL url) throws IOException {

        final File cache = new File(LOCAL_BOOTSTRAP_HOME, getFileName(url));
        if (!cache.isFile()) {
            copyUrlToFile(url, cache);
        }
        loadClassPathFile(instrumentation, cache);
    }

    private static void loadClassPathJAR(final Instrumentation instrumentation, final JarFile jar) {

        instrumentation.appendToSystemClassLoaderSearch(jar);
    }

    private static String getFileName(final URL url) {

        final String url_file = url.getFile();
        return url_file.substring(url_file.lastIndexOf('/') + 1);
    }

    private static File copyUrlToFile(final URL url, final File destination) throws IOException {

        ReadableByteChannel byte_channel = null;
        FileOutputStream out = null;
        try {
            byte_channel = Channels.newChannel(url.openStream());
            out = new FileOutputStream(destination);
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
        return destination;
    }

    private static void loadClassPathURL(final Instrumentation instrumentation, final URL url) throws URISyntaxException, IOException {

        final File url_as_file;
        if (!isFile(url)) {
            url_as_file = copyUrlToTmpDirectory(url);
        }
        else {
            url_as_file = new File(url.toURI());
        }

        loadClassPathFile(instrumentation, url_as_file);
    }

    private static void loadClassPathFile(final Instrumentation instrumentation, final File file) throws IOException {

        final JarFile jar = new JarFile(file);
        loadClassPathJAR(instrumentation, jar);
    }

    private static File copyUrlToTmpDirectory(final URL url) throws IOException {

        return copyUrlToFile(url, new File(tmp_dir, getFileName(url)));
    }

    private static void addMavenRepositories(final Set<String> repositories) throws MalformedURLException {

        for (final String repository : repositories) {
            maven_dependency_resolver.addRepository(new URL(repository));
        }
    }

    private static boolean isFile(final URL url) {

        return url.getProtocol().equals(FILE_PROTOCOL);
    }

    static class BootstrapConfiguration {

        private final Set<String> files = new HashSet<String>();
        private final Set<String> urls = new HashSet<String>();
        private final Set<String> maven_repositories = new HashSet<String>();
        private final Set<String> maven_artifacts = new HashSet<String>();
        private final Set<String> delete_on_exit = new HashSet<String>();
        private volatile String application_bootstrap_class_name;
        private boolean delete_working_directory_on_exit;

        void setDeleteWorkingDirectoryOnExit(final boolean enabled) {

            delete_working_directory_on_exit = enabled;
        }

        void write(final OutputStream out) throws IOException {

            final Manifest manifest = toManifest();
            manifest.write(out);
        }

        private Manifest toManifest() {

            final Manifest manifest = new Manifest();
            final Attributes attributes = new Attributes();

            attributes.put(BOOTSTRAP_CLASS_KEY, application_bootstrap_class_name);
            attributes.put(CLASSPATH_FILES, toString(files));
            attributes.put(CLASSPATH_URLS, toString(urls));
            attributes.put(MAVEN_REPOSITORIES, toString(maven_repositories));
            attributes.put(MAVEN_ARTIFACTS, toString(maven_artifacts));
            attributes.put(FILES_TO_DELETE_ON_EXIT, toString(delete_on_exit));
            attributes.put(DELETE_WD_ON_EXIT, String.valueOf(delete_working_directory_on_exit));
            manifest.getEntries().put(CONFIG_FILE_ATTRIBUTES_NAME, attributes);
            return manifest;
        }

        private static String toString(final Collection<?> collection) {

            final StringBuilder string_builder = new StringBuilder();
            for (final Object element : collection) {
                string_builder.append(element);
                string_builder.append(SEPARATOR);
            }

            return string_builder.toString().trim();
        }

        static BootstrapConfiguration read(final InputStream in) throws IOException {

            final Manifest manifest = new Manifest(new BufferedInputStream(in));
            return fromManifest(manifest);
        }

        private static BootstrapConfiguration fromManifest(final Manifest manifest) {

            final BootstrapConfiguration configuration = new BootstrapConfiguration();
            final Attributes attributes = manifest.getAttributes(CONFIG_FILE_ATTRIBUTES_NAME);
            final String bootstrap = attributes.get(BOOTSTRAP_CLASS_KEY).toString();

            configuration.setApplicationBootstrapClassName(bootstrap);

            final String[] files = attributes.get(CLASSPATH_FILES).toString().split(SEPARATOR);
            for (final String file : files) {
                if (!file.trim().isEmpty()) {
                    configuration.addClassPathFile(file);
                }
            }
            final String[] urls = attributes.get(CLASSPATH_URLS).toString().split(SEPARATOR);
            for (final String url : urls) {
                if (!url.trim().isEmpty()) {
                    configuration.urls.add(url);
                }
            }

            final String[] repos = attributes.get(MAVEN_REPOSITORIES).toString().split(SEPARATOR);
            for (final String repo : repos) {
                if (!repo.trim().isEmpty()) {
                    configuration.maven_repositories.add(repo);
                }
            }

            final String[] mvns = attributes.get(MAVEN_ARTIFACTS).toString().split(SEPARATOR);
            for (final String artifact_coordinate : mvns) {
                if (!artifact_coordinate.trim().isEmpty()) {
                    configuration.addMavenArtifact(artifact_coordinate);
                }
            }

            final String[] delete_on_exit = attributes.get(FILES_TO_DELETE_ON_EXIT).toString().split(SEPARATOR);
            for (final String file : delete_on_exit) {
                if (!file.trim().isEmpty()) {
                    configuration.addFileToDeleteOnExit(file);
                }
            }

            final Boolean delete_wd_on_exit = Boolean.valueOf(attributes.get(DELETE_WD_ON_EXIT).toString());
            configuration.setDeleteWorkingDirectoryOnExit(delete_wd_on_exit);

            return configuration;
        }

        boolean addMavenArtifact(final String artifact_coordinate) {

            return maven_artifacts.add(artifact_coordinate);
        }

        boolean addClassPathFile(final String path) {

            return files.add(path);
        }

        void setApplicationBootstrapClassName(final String class_name) {

            application_bootstrap_class_name = class_name;
        }

        boolean addMavenRepository(final URL url) {

            return maven_repositories.add(url.toExternalForm());
        }

        boolean addClassPathURL(final URL url) {

            return urls.add(url.toExternalForm());
        }

        boolean addFileToDeleteOnExit(final String path) {

            return delete_on_exit.add(path);
        }

        void setApplicationBootstrapClass(final Class<?> bootstrap_class) {

            application_bootstrap_class_name = bootstrap_class.getName();
        }

        boolean hasMavenArtifact() {

            return !maven_artifacts.isEmpty();
        }
    }

    private static final class FileDeletionHook extends Thread {

        private final List<File> files;

        private FileDeletionHook(final List<File> files) {

            this.files = files;
        }

        @Override
        public void run() {

            for (final File file : files) {
                deleteRecursively(file);
            }
        }

        void deleteRecursively(final File file) {

            if (file.exists()) {
                if (file.isDirectory()) {
                    final File[] sub_files = file.listFiles();
                    if (sub_files != null) {
                        for (final File sub_file : sub_files) {
                            deleteRecursively(sub_file);
                        }
                    }
                }
                if (!file.delete()) {
                    System.err.println("Failed to delete file: " + file);
                }
            }
        }
    }
}
