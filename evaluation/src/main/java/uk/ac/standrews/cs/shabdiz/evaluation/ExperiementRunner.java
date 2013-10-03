package uk.ac.standrews.cs.shabdiz.evaluation;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import org.apache.commons.io.FileUtils;
import org.junit.Test;
import org.junit.internal.runners.model.EachTestNotifier;
import org.junit.runner.Description;
import org.junit.runner.JUnitCore;
import org.junit.runner.Result;
import org.junit.runner.Runner;
import org.junit.runner.notification.Failure;
import org.junit.runner.notification.RunNotifier;
import org.junit.runners.Parameterized;
import org.junit.runners.model.FrameworkMethod;
import org.junit.runners.model.MultipleFailureException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.standrews.cs.shabdiz.host.LocalHost;
import uk.ac.standrews.cs.shabdiz.testing.junit.JUnitBootstrapCore;
import uk.ac.standrews.cs.shabdiz.testing.junit.ParameterizedRange;
import uk.ac.standrews.cs.shabdiz.util.Duration;
import uk.ac.standrews.cs.shabdiz.util.ProcessUtil;

/** @author Masih Hajiarabderkani (mh638@st-andrews.ac.uk) */
public class ExperiementRunner extends Parameterized {

    public static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy_MM_dd__HH_mm_ss_SSS");
    public static final String REPETITIONS_HOME_NAME = "repetitions";
    public static final String RESULT_PROPERTY_KEY = "RESULT";
    public static final Duration TEST_OUTPUT_TIMEOUT = new Duration(30, TimeUnit.MINUTES);
    private static final Logger LOGGER = LoggerFactory.getLogger(ExperiementRunner.class);
    private static final File RESULTS_HOME = new File("results");
    private final LocalHost local_host;
    private final String class_path;

    public ExperiementRunner(final Class<?> klass) throws Throwable {

        super(klass);

        local_host = new LocalHost();
        final File tmp_dir_file = new File(FileUtils.getTempDirectory(), UUID.randomUUID().toString());
        FileUtils.forceMkdir(tmp_dir_file);
        String tmp_dir_path = tmp_dir_file.getAbsolutePath();
        if (!tmp_dir_path.endsWith(File.separator)) {
            tmp_dir_path += File.separator;
        }
        class_path = "-cp " + tmp_dir_path + File.pathSeparator + tmp_dir_path + "* ";

        for (String classpath_entry : System.getProperty("java.class.path").split(File.pathSeparator)) {
            if (!classpath_entry.isEmpty()) {
                final File classphath_file = new File(classpath_entry);
                if (classphath_file.isDirectory()) {
                    for (String sub_cp : classphath_file.list()) {
                        local_host.upload(new File(classphath_file, sub_cp), tmp_dir_path);
                    }
                }
                else {
                    local_host.upload(classphath_file, tmp_dir_path);
                }
            }
        }

        Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {

            @Override
            public void run() {

                FileUtils.deleteQuietly(tmp_dir_file);
            }
        }));
    }

    public static void main(String[] args) throws IOException {

        Result result;
        try {
            final String test_class_name = args[0];
            final Class<?> test_class = Class.forName(test_class_name);
            final ParameterizedRange runner = new ParameterizedRange(test_class, Integer.parseInt(args[1]), Integer.parseInt(args[2]));
            final JUnitCore core = new JUnitCore();
            result = core.run(runner);
        }
        catch (Throwable e) {
            LOGGER.error("failed to run test with arguments {}" + Arrays.toString(args), e);
            result = new Result();
            final Description description = Description.createSuiteDescription(e.getMessage());
            final Failure failure = new Failure(description, e);
            result.getFailures().add(failure);
        }

        final String result_in_base64;
        try {
            result_in_base64 = JUnitBootstrapCore.serializeAsBase64(result);
            ProcessUtil.printKeyValue(System.out, RESULT_PROPERTY_KEY, result_in_base64);
        }
        catch (IOException e) {
            LOGGER.error("failed to encode result to base64", e);
            throw e;
        }
    }

    @Override
    protected void runChild(final Runner runner, final RunNotifier notifier) {

        final int index = getRunnerIndex(runner);
        final File working_directory = constructWorkingDirectoryByDescription(runner.getDescription());
        final Description description = describeChild(runner);
        try {
            final EachTestNotifier eachNotifier = new EachTestNotifier(notifier, description);
            FileUtils.forceMkdir(working_directory);
            final String command = "java " + class_path + getClass().getName() + " " + getTestClass().getName() + " " + index + " " + (index + 1);
            LOGGER.info("running command {}", command);
            final Process test_process = local_host.execute(working_directory.getAbsolutePath(), command);
            final String result_in_base64 = ProcessUtil.scanProcessOutput(test_process, RESULT_PROPERTY_KEY, TEST_OUTPUT_TIMEOUT);

            eachNotifier.fireTestStarted();
            final List<Description> descriptions = getMethodDescriptions(runner);
            for (Description description1 : descriptions) {
                notifier.fireTestStarted(description1);
            }

            final Result result = JUnitBootstrapCore.deserializeAsBase64(result_in_base64);
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
            notifier.fireTestFailure(new Failure(description, e));
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

    private synchronized File constructWorkingDirectoryByDescription(final Description description) {

        final File class_name = new File(RESULTS_HOME, getTestClass().getJavaClass().getSimpleName());
        final File display_name = new File(class_name, description.getDisplayName());
        final File repetitions = new File(display_name, REPETITIONS_HOME_NAME);
        final File working_directory = new File(repetitions, DATE_FORMAT.format(new Date()));
        return working_directory.exists() ? constructWorkingDirectoryByDescription(description) : working_directory;
    }

    private int getRunnerIndex(Runner runner) {

        int i = 0;
        for (Runner child : getChildren()) {
            if (runner.equals(child)) { return i; }
            i++;
        }
        throw new NoSuchElementException("No matching runner " + runner);
    }
}
