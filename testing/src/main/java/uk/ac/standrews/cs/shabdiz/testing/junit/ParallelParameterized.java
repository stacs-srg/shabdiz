package uk.ac.standrews.cs.shabdiz.testing.junit;

import java.io.File;
import java.lang.annotation.Annotation;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Properties;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.junit.Test;
import org.junit.internal.runners.model.EachTestNotifier;
import org.junit.runner.Description;
import org.junit.runner.Result;
import org.junit.runner.Runner;
import org.junit.runner.notification.Failure;
import org.junit.runner.notification.RunNotifier;
import org.junit.runners.Parameterized;
import org.junit.runners.model.FrameworkMethod;
import org.junit.runners.model.MultipleFailureException;
import org.junit.runners.model.RunnerScheduler;
import uk.ac.standrews.cs.shabdiz.host.Host;
import uk.ac.standrews.cs.shabdiz.host.exec.AgentBasedJavaProcessBuilder;
import uk.ac.standrews.cs.shabdiz.util.Duration;

public class ParallelParameterized extends Parameterized {

    static final String TEST_PARAM_INDEX = "test.param.index";
    private final Integer parameter_index;
    private final AgentBasedJavaProcessBuilder process_builder;
    private volatile int next_host_index;
    private List<Host> hosts;
    private ExecutorService executor_service;

    /** Coppied from super constructor: Only called reflectively. Do not use programmatically. */
    public ParallelParameterized(final Class<?> test_class) throws Throwable {

        super(test_class);
        final String index_as_string = System.getProperty(TEST_PARAM_INDEX);
        parameter_index = index_as_string != null ? Integer.valueOf(index_as_string) : null;
        final Parallelization annotation = getParallelizationAnnotation();
        process_builder = new AgentBasedJavaProcessBuilder();
        configure(annotation);
        setScheduler(new RunnerScheduler() {

            @Override
            public void schedule(final Runnable childStatement) {

                executor_service.execute(childStatement);

            }

            @Override
            public void finished() {

                executor_service.shutdown();
                try {
                    executor_service.awaitTermination(10, TimeUnit.MINUTES);
                }
                catch (InterruptedException exc) {
                    throw new RuntimeException(exc);
                }

            }
        });
    }

    private void configure(final Parallelization annotation) throws Throwable {

        process_builder.setMainClass(JUnitBootstrapCore.class);

        if (annotation != null) {

            if (!isParameterIndexSpecified()) {
                final String[] maven_artifacts = annotation.mavenArtifacts();
                for (String maven_artifact : maven_artifacts) {
                    process_builder.addMavenDependency(maven_artifact);
                }

                final String[] files = annotation.files();
                for (String file : files) {
                    process_builder.addFile(new File(file));
                }

                final String[] urls = annotation.urls();
                for (String url : urls) {
                    process_builder.addURL(new URL(url));
                }

                final String[] remote_files = annotation.remoteFiles();
                for (String remote_file : remote_files) {
                    process_builder.addRemoteFile(remote_file);
                }
                final String[] jvm_args = annotation.jvmArguments();
                for (String arg : jvm_args) {
                    process_builder.addJVMArgument(arg);
                }

                if (annotation.addCurrentJvmClasspath()) {
                    process_builder.addCurrentJVMClasspath();
                }

                process_builder.setDeleteWorkingDirectoryOnExit(annotation.deleteWorkingDirectoryOnExit());

                final String working_directory = annotation.workingDirectory();
                if (!working_directory.isEmpty()) {
                    process_builder.setWorkingDirectory(working_directory);
                }
                final String host_provider_name = annotation.hostProvider();
                if (!host_provider_name.isEmpty()) {
                    hosts = new ArrayList<Host>(getHostProviderByName(host_provider_name));
                }
            }
            int thread_count = annotation.threadCount();
            if (thread_count > 0) {
                executor_service = Executors.newFixedThreadPool(thread_count);
            }
            else {
                executor_service = Executors.newCachedThreadPool();
            }
        }
    }

    private Collection<Host> getHostProviderByName(final String host_provider_name) throws Throwable {

        final FrameworkMethod host_provider = getHostProviderMethodByName(host_provider_name);
        validateHostProviderMethod(host_provider);
        return (Collection<Host>) host_provider.invokeExplosively(null);
    }

    private void validateHostProviderMethod(final FrameworkMethod host_provider) throws Exception {

        if (host_provider == null || !host_provider.isStatic() || !host_provider.isPublic()) { throw new Exception("HostProvider method must be public and static and must return Collection<Host>"); }
    }

    private FrameworkMethod getHostProviderMethodByName(final String host_provider_name) {

        for (FrameworkMethod provider : getTestClass().getAnnotatedMethods(HostProvider.class)) {
            if (provider.getAnnotation(HostProvider.class).name().equals(host_provider_name)) { return provider; }
        }

        return null;
    }

    private Parallelization getParallelizationAnnotation() {

        final Annotation[] annotations = getTestClass().getAnnotations();
        for (Annotation annotation : annotations) {
            if (annotation.annotationType().equals(Parallelization.class)) { return (Parallelization) annotation; }
        }
        return null;
    }

    @Override
    protected void runChild(final Runner runner, final RunNotifier notifier) {

        final int index = getRunnerIndex(runner);
        if (isParameterIndexSpecified()) {
            if (parameter_index == index) {
                super.runChild(runner, notifier);
            }
        }
        else if (hosts == null) {
            super.runChild(runner, notifier);
        }
        else {
            final Description description = describeChild(runner);
            final EachTestNotifier eachNotifier = new EachTestNotifier(notifier, description);
            try {
                final Host host = nextHost();
                Process test_process = null;
                final String test_class_name = getTestClass().getJavaClass().getName();
                final String parameter_index = String.valueOf(index);
                final Properties properties;
                eachNotifier.fireTestStarted();
                try {
                    test_process = process_builder.start(host, test_class_name, parameter_index);
                    properties = JUnitBootstrapCore.readProperties(JUnitBootstrapCore.class, test_process, Duration.MAX_DURATION);
                }
                finally {
                    if (test_process != null) {
                        test_process.destroy();
                    }
                }

                final List<Description> descriptions = getMethodDescriptions(runner);
                for (Description description1 : descriptions) {
                    notifier.fireTestStarted(description1);
                }

                final Result result = JUnitBootstrapCore.getResultProperty(properties);
                if (result.wasSuccessful()) {

                    eachNotifier.fireTestFinished();
                }

                final List<Throwable> failure_exceptions = new ArrayList<Throwable>();
                for (Failure failure : result.getFailures()) {

                    failure_exceptions.add(failure.getException());
                    notifier.fireTestFailure(failure);
                    descriptions.remove(failure.getDescription());
                }

                if (!failure_exceptions.isEmpty()) {
                    eachNotifier.addFailure(new MultipleFailureException(failure_exceptions));
                }
                for (Description description1 : descriptions) {
                    notifier.fireTestFinished(description1);
                }
            }
            catch (Exception e) {
                eachNotifier.addFailure(e);
            }
        }
    }

    private List<Description> getMethodDescriptions(final Runner runner) {

        final String displayName = runner.getDescription().getDisplayName();
        final List<FrameworkMethod> methods = getTestClass().getAnnotatedMethods(Test.class);
        List<Description> descriptions = new ArrayList<Description>();
        for (FrameworkMethod method : methods) {
            final Description description = Description.createTestDescription(getTestClass().getJavaClass(), method.getName() + displayName, method.getAnnotations());
            descriptions.add(description);
        }

        return descriptions;
    }

    private Host nextHost() {

        if (hosts == null) { return null; }

        synchronized (this) {

            final Host host = hosts.get(next_host_index);
            next_host_index++;
            if (next_host_index > hosts.size() - 1) {
                next_host_index = 0;
            }
            return host;
        }
    }

    private boolean isParameterIndexSpecified() {

        return parameter_index != null;
    }

    private int getRunnerIndex(final Runner runner) {

        int i = 0;
        for (Runner child : getChildren()) {
            if (runner.equals(child)) { return i; }
            i++;
        }
        throw new NoSuchElementException("No matching runner");
    }

    /** Specifies a collection of {@link Host hosts} on which to strat JVMs. */
    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.METHOD)
    public @interface HostProvider {

        String name();
    }

    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.TYPE)
    @Inherited
    public @interface Parallelization {

        String[] mavenArtifacts() default {};

        String[] urls() default {};

        String[] files() default {};

        String[] remoteFiles() default {};

        boolean addCurrentJvmClasspath() default true;

        boolean deleteWorkingDirectoryOnExit() default false;

        String[] jvmArguments() default {};

        String workingDirectory() default "";

        int threadCount() default 1;

        String hostProvider() default "";

    }
}
