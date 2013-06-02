package uk.ac.standrews.cs.shabdiz.host.exec;

import java.io.File;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.List;

class JavaBootstrap {

    public static final String MAVEN_DEPENDENCY_RESOLVER_CLASS_NAME = "uk.ac.standrews.cs.shabdiz.host.exec.MavenDependencyResolver";
    static final String SHABDIZ_HOME_NAME = "shabdiz";
    static final String BOOTSTRAP_HOME_NAME = ".bootstrap";
    static final File LOCAL_SHABDIZ_HOME = new File(System.getProperty("java.io.tmpdir"), SHABDIZ_HOME_NAME);
    static final File LOCAL_BOOTSTRAP_HOME = new File(LOCAL_SHABDIZ_HOME, BOOTSTRAP_HOME_NAME);
    private static final String MVN_CENTRAL = "http://repo1.maven.org/maven2/";
    private static final String COMMONS_IO_JAR = "commons-io-2.4.jar";
    private static final File COMMONS_IO_JAR_FILE = new File(LOCAL_BOOTSTRAP_HOME, COMMONS_IO_JAR);
    private static final String COMMONS_IO_ON_MAVEN_CENTRAL = MVN_CENTRAL + "commons-io/commons-io/2.4/" + COMMONS_IO_JAR;
    private static final String[] AETHER_DEPENDENCIES_ON_MAVEN_CENTRAL = {
            MVN_CENTRAL + "org/eclipse/aether/aether-api/0.9.0.M2/aether-api-0.9.0.M2.jar",
            MVN_CENTRAL + "org/eclipse/aether/aether-util/0.9.0.M2/aether-util-0.9.0.M2.jar",
            MVN_CENTRAL + "org/eclipse/aether/aether-impl/0.9.0.M2/aether-impl-0.9.0.M2.jar",
            MVN_CENTRAL + "org/eclipse/aether/aether-spi/0.9.0.M2/aether-spi-0.9.0.M2.jar",
            MVN_CENTRAL + "org/eclipse/aether/aether-connector-file/0.9.0.M2/aether-connector-file-0.9.0.M2.jar",
            MVN_CENTRAL + "org/eclipse/aether/aether-connector-asynchttpclient/0.9.0.M2/aether-connector-asynchttpclient-0.9.0.M2.jar",
            MVN_CENTRAL + "com/ning/async-http-client/1.7.6/async-http-client-1.7.6.jar",
            MVN_CENTRAL + "io/netty/netty/3.4.4.Final/netty-3.4.4.Final.jar",
            MVN_CENTRAL + "org/slf4j/slf4j-api/1.7.5/slf4j-api-1.7.5.jar",
            MVN_CENTRAL + "io/tesla/maven/maven-aether-provider/3.1.0/maven-aether-provider-3.1.0.jar",
            MVN_CENTRAL + "io/tesla/maven/maven-model/3.1.0/maven-model-3.1.0.jar",
            MVN_CENTRAL + "io/tesla/maven/maven-model-builder/3.1.0/maven-model-builder-3.1.0.jar",
            MVN_CENTRAL + "org/codehaus/plexus/plexus-interpolation/1.16/plexus-interpolation-1.16.jar",
            MVN_CENTRAL + "io/tesla/maven/maven-repository-metadata/3.1.0/maven-repository-metadata-3.1.0.jar",
            MVN_CENTRAL + "org/eclipse/sisu/org.eclipse.sisu.plexus/0.0.0.M2a/org.eclipse.sisu.plexus-0.0.0.M2a.jar",
            MVN_CENTRAL + "javax/enterprise/cdi-api/1.0/cdi-api-1.0.jar",
            MVN_CENTRAL + "javax/annotation/jsr250-api/1.0/jsr250-api-1.0.jar",
            MVN_CENTRAL + "javax/inject/javax.inject/1/javax.inject-1.jar",
            MVN_CENTRAL + "com/google/guava/guava/10.0.1/guava-10.0.1.jar",
            MVN_CENTRAL + "com/google/code/findbugs/jsr305/1.3.9/jsr305-1.3.9.jar",
            MVN_CENTRAL + "org/sonatype/sisu/sisu-guice/3.1.0/sisu-guice-3.1.0-no_aop.jar",
            MVN_CENTRAL + "aopalliance/aopalliance/1.0/aopalliance-1.0.jar",
            MVN_CENTRAL + "org/eclipse/sisu/org.eclipse.sisu.inject/0.0.0.M2a/org.eclipse.sisu.inject-0.0.0.M2a.jar",
            MVN_CENTRAL + "asm/asm/3.3.1/asm-3.3.1.jar",
            MVN_CENTRAL + "org/codehaus/plexus/plexus-classworlds/2.4/plexus-classworlds-2.4.jar",
            MVN_CENTRAL + "org/codehaus/plexus/plexus-component-annotations/1.5.5/plexus-component-annotations-1.5.5.jar",
            MVN_CENTRAL + "org/codehaus/plexus/plexus-utils/3.0.10/plexus-utils-3.0.10.jar"
    };
    private final String main_class;
    private final String[] repositories;
    private final String[] artifacts;
    private final String[] args;
    private final ClassLoader loader;

    JavaBootstrap(String[] repositories, String[] artifacts, String main_class, String[] args) throws ClassNotFoundException, MalformedURLException, InvocationTargetException, IllegalAccessException, NoSuchMethodException, URISyntaxException {

        this.repositories = repositories;
        this.artifacts = artifacts;
        this.main_class = main_class;
        this.args = args;
        loader = resolveSelfDependencies();
    }

    public static void main(String[] args) throws Exception {

        String[] repositories = toArray(args[0]);
        String[] artifacts = toArray(args[1]);
        String target_main_class = args[2];
        String[] target_main_args = toArray(args[3]);
        final JavaBootstrap bootstrap = new JavaBootstrap(repositories, artifacts, target_main_class, target_main_args);
        bootstrap.start();
    }

    public void start() throws Exception {

        final Class<?> maven_dependency_resolver_class = loader.loadClass(MAVEN_DEPENDENCY_RESOLVER_CLASS_NAME);
        final Constructor<?> constructor = maven_dependency_resolver_class.getDeclaredConstructor();
        constructor.setAccessible(true);
        final Object maven_dependency_resolver = constructor.newInstance();

        addRepositories(maven_dependency_resolver_class, maven_dependency_resolver);
        addArtifacts(maven_dependency_resolver_class, maven_dependency_resolver);

        final ClassLoader target_loader = resolve(maven_dependency_resolver_class, maven_dependency_resolver);
        loadAndInvokeTargetMainMethod(target_loader);
    }

    private void loadAndInvokeTargetMainMethod(final ClassLoader target_loader) throws IllegalAccessException, InvocationTargetException, NoSuchMethodException, ClassNotFoundException {

        target_loader.loadClass(main_class).getMethod("main", String[].class).invoke(null, (Object) args);
    }

    private void addRepositories(final Class<?> maven_dependency_resolver_class, final Object maven_dependency_resolver) throws NoSuchMethodException, IllegalAccessException, InvocationTargetException {

        if (repositories != null && repositories.length > 0) {
            final Method add_repository = maven_dependency_resolver_class.getDeclaredMethod("addRepository", String.class);
            add_repository.setAccessible(true);
            for (String repository : repositories) {
                add_repository.invoke(maven_dependency_resolver, repository);
            }
        }
    }

    private void addArtifacts(final Class<?> maven_dependency_resolver_class, final Object maven_dependency_resolver) throws NoSuchMethodException, IllegalAccessException, InvocationTargetException {

        if (artifacts != null && artifacts.length > 0) {
            final Method add_artifact = maven_dependency_resolver_class.getDeclaredMethod("addArtifact", String.class);
            add_artifact.setAccessible(true);
            for (String artifact : artifacts) {
                add_artifact.invoke(maven_dependency_resolver, artifact);
            }
        }
    }

    private ClassLoader resolve(final Class<?> maven_dependency_resolver_class, final Object maven_dependency_resolver) throws NoSuchMethodException, IllegalAccessException, InvocationTargetException {

        final Method resolve = maven_dependency_resolver_class.getDeclaredMethod("resolve");
        resolve.setAccessible(true);
        return (ClassLoader) resolve.invoke(maven_dependency_resolver);
    }

    static File getBootstrapHome(String tmp_dir) {
        return new File(getShabdizHome(tmp_dir), BOOTSTRAP_HOME_NAME);
    }

    static File getShabdizHome(final String tmp_dir) {
        return new File(tmp_dir, SHABDIZ_HOME_NAME);
    }

    private static URLClassLoader resolveSelfDependencies() throws MalformedURLException, ClassNotFoundException, NoSuchMethodException, IllegalAccessException, InvocationTargetException, URISyntaxException {
        final URL commons_io_url = getCommonsIOURL();
        final URLClassLoader url_class_loader = URLClassLoader.newInstance(new URL[] { commons_io_url }, ClassLoader.getSystemClassLoader());
        final URL[] selfDependencies = getSelfDependenciesRemoteURLs();
        final Class<?> target_main = url_class_loader.loadClass("org.apache.commons.io.FileUtils");
        final Method copyURLToFile = target_main.getMethod("copyURLToFile", URL.class, File.class);
        for (URL dependency : selfDependencies) {
            final File destination = new File(LOCAL_BOOTSTRAP_HOME, getJarFileName(dependency));
            if (!destination.isFile()) {
                copyURLToFile.invoke(null, dependency, destination);
            }
        }
        return URLClassLoader.newInstance(toURLs(LOCAL_BOOTSTRAP_HOME.listFiles()), null);
    }

    private static URL getCommonsIOURL() throws MalformedURLException {

        return COMMONS_IO_JAR_FILE.isFile() ? toURL(COMMONS_IO_JAR_FILE) : new URL(COMMONS_IO_ON_MAVEN_CENTRAL);
    }

    private static String getJarFileName(final URL url) {

        final String url_file = url.getFile();
        return url_file.substring(url_file.lastIndexOf('/'));
    }

    private static URL[] getSelfDependenciesRemoteURLs() throws MalformedURLException {

        final List<URL> self_dependencies = new ArrayList<URL>();
        self_dependencies.add(new URL(COMMONS_IO_ON_MAVEN_CENTRAL));
        for (String aether_dependency : AETHER_DEPENDENCIES_ON_MAVEN_CENTRAL) {
            self_dependencies.add(new URL(aether_dependency));
        }
        self_dependencies.add(JavaBootstrap.class.getProtectionDomain().getCodeSource().getLocation());
        return self_dependencies.toArray(new URL[self_dependencies.size()]);
    }

    private static URL[] toURLs(File... files) throws MalformedURLException {
        URL[] urls = null;
        if (files != null) {
            int files_count = files.length;
            urls = new URL[files_count];

            for (int i = 0; i < files_count; i++) {
                urls[i] = toURL(files[i]);
            }
        }
        return urls;
    }

    private static URL toURL(File file) throws MalformedURLException {
        return file.toURI().toURL();
    }

    private static String[] toArray(final String value) {
        if (value != null) {
            final String tidied_value = value.trim().replace("[", "").replace("]", "");
            return !tidied_value.equals("") ? tidied_value.split(", ") : null;
        }

        return null;
    }
}
