package uk.ac.standrews.cs.shabdiz.testing.junit;

import java.util.NoSuchElementException;
import java.util.Properties;
import org.junit.internal.runners.model.EachTestNotifier;
import org.junit.runner.Result;
import org.junit.runner.Runner;
import org.junit.runner.notification.Failure;
import org.junit.runner.notification.RunNotifier;
import org.junit.runners.Parameterized;
import uk.ac.standrews.cs.shabdiz.host.Host;
import uk.ac.standrews.cs.shabdiz.host.SSHHost;
import uk.ac.standrews.cs.shabdiz.host.SSHPublicKeyCredentials;
import uk.ac.standrews.cs.shabdiz.host.exec.AgentBasedJavaProcessBuilder;
import uk.ac.standrews.cs.shabdiz.util.Duration;
import uk.ac.standrews.cs.shabdiz.util.Input;

public class Parallelized extends Parameterized {

    static final String TEST_PARAM_INDEX = "test.param.index";
    static Host host;
    private final Integer target_parameter_index;

    /** Coppied from super constructor: Only called reflectively. Do not use programmatically. */
    public Parallelized(final Class<?> test_class) throws Throwable {

        super(test_class);
        final String index_as_string = System.getProperty(TEST_PARAM_INDEX);
        target_parameter_index = index_as_string != null ? Integer.valueOf(index_as_string) : null;
    }

    @Override
    protected void runChild(final Runner runner, final RunNotifier notifier) {

        final int index = getRunnerIndex(runner);
        if (target_parameter_index != null) {
            if (target_parameter_index == index) {
                super.runChild(runner, notifier);
            }
        }
        else {

            final EachTestNotifier eachNotifier = new EachTestNotifier(notifier, runner.getDescription());
            final AgentBasedJavaProcessBuilder builder = new AgentBasedJavaProcessBuilder();
            builder.setMainClass(JUnitBootstrapCore.class);
            //            builder.addCurrentJVMClasspath();
            builder.addMavenDependency("uk.ac.standrews.cs", "shabdiz-evaluation", "1.0-SNAPSHOT");
            builder.addJVMArgument("-D" + TEST_PARAM_INDEX + '=' + index);
            builder.addJVMArgument("-Xmx1024m");
            builder.addJVMArgument("-XX:MaxPermSize=256m");
            builder.setWorkingDirectory("/home/masih/shabdiz_experiments/results");
            //            builder.setWorkingDirectory("/home/masih/shabdiz_wd");
            //            builder.setWorkingDirectory(System.getProperty("user.dir", "."));
            //            builder.setDeleteWorkingDirectoryOnExit(true);
            //            Host host = null;
            try {
                if (host == null) {
                    host = new SSHHost("blub.cs.st-andrews.ac.uk", SSHPublicKeyCredentials.getDefaultRSACredentials(Input.readPassword("local RSA key")));
                    //                    host = new LocalHost();
                }

                Process test_process = null;

                final Properties properties;
                try {
                    eachNotifier.fireTestStarted();
                    test_process = builder.start(host, getTestClass().getJavaClass().getName());
                    properties = JUnitBootstrapCore.readProperties(JUnitBootstrapCore.class, test_process, Duration.MAX_DURATION);
                }
                finally {
                    if (test_process != null) {
                        test_process.destroy();
                    }
                }

                final Result result = JUnitBootstrapCore.getResultProperty(properties);
                if (result.wasSuccessful()) {
                    eachNotifier.fireTestFinished();
                }
                else {
                    for (Failure failure : result.getFailures()) {
                        eachNotifier.addFailure(failure.getException());
                    }
                }

            }
            catch (Exception e) {
                eachNotifier.addFailure(e);
            }
        }
    }

    private int getRunnerIndex(final Runner runner) {

        int i = 0;
        for (Runner child : getChildren()) {
            if (runner.equals(child)) { return i; }
            i++;
        }
        throw new NoSuchElementException("No matching runner");
    }

    public static @interface Parallelization {

        String[] mavenArtifacts() default {};

        String[] URLs() default {};

        String[] files() default {};

        String[] remoteFiles() default {};

        String[] JVMArguments() default {};

        int forkCount() default 0;

        int threadCount() default 1;

        String workingDirectory() default "";

    }
}
