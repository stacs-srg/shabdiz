package uk.ac.standrews.cs.shabdiz.testing.junit;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import org.codehaus.plexus.util.Base64;
import org.junit.runner.Description;
import org.junit.runner.JUnitCore;
import org.junit.runner.Result;
import org.junit.runner.notification.Failure;
import uk.ac.standrews.cs.shabdiz.host.exec.Bootstrap;

/** @author Masih Hajiarabderkani (mh638@st-andrews.ac.uk) */
public class JUnitBootstrapCore extends Bootstrap {

    static final String TEST_RESULT = "result";
    private static final Charset UTF8 = Charset.forName("UTF-8");
    private final List<Failure> failures;
    private final List<Class<?>> test_classes;

    public JUnitBootstrapCore() {

        test_classes = new ArrayList<Class<?>>();
        failures = new ArrayList<Failure>();
    }

    public static Result deserializeAsBase64(final String result_as_string) throws IOException, ClassNotFoundException {

        final byte[] bytes = Base64.decodeBase64(result_as_string.getBytes(UTF8));
        ObjectInputStream ois = null;

        try {
            ois = new ObjectInputStream(new ByteArrayInputStream(bytes));
            return (Result) ois.readObject();
        }
        finally {
            if (ois != null) {
                ois.close();
            }
        }
    }

    public static String serializeAsBase64(final Result result) throws IOException {

        final ByteArrayOutputStream bos = new ByteArrayOutputStream();
        ObjectOutputStream oos = null;
        try {
            oos = new ObjectOutputStream(bos);
            oos.writeObject(result);
            oos.flush();
            final byte[] bytes = bos.toByteArray();
            return new String(Base64.encodeBase64(bytes), UTF8);
        }
        finally {
            if (oos != null) {
                oos.close();
            }
        }
    }

    @Override
    protected void deploy(final String... args) throws Exception {

        configure(args);
        final JUnitCore core = new JUnitCore();
        final Result result = core.run(test_classes.toArray(new Class[test_classes.size()]));
        for (Failure failure : failures) {
            result.getFailures().add(failure);
        }

        setProperty(TEST_RESULT, serializeAsBase64(result));
    }

    static Result getResultProperty(Properties properties) throws IOException, ClassNotFoundException {

        final String result_as_string = properties.getProperty(TEST_RESULT);
        return deserializeAsBase64(result_as_string);
    }

    private void configure(String... args) {

        if (args.length < 2) {
            Failure failure = new Failure(Description.TEST_MECHANISM, new Exception("expected parameterized_test_class_name and parameter_index as command line arguments"));
            failures.add(failure);
        }
        else {

            final String test_class_name = args[0];
            final String parameter_index = args[1];
            System.setProperty(ParallelParameterized.TEST_PARAM_INDEX, parameter_index);
            try {
                test_classes.add(Class.forName(test_class_name));
            }
            catch (final ClassNotFoundException e) {
                Description description = Description.createSuiteDescription(test_class_name);
                Failure failure = new Failure(description, e);
                failures.add(failure);
            }
        }
    }
}
