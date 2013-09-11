package uk.ac.standrews.cs.shabdiz.host.exec;

import java.io.InputStream;
import java.util.Properties;
import org.apache.commons.io.IOUtils;
import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** @author Masih Hajiarabderkani (mh638@st-andrews.ac.uk) */
public class BootstrapTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(BootstrapTest.class);

    @Test
    public void testDeploy() throws Exception {

        //        final AgentBasedJavaProcessBuilder builder = new AgentBasedJavaProcessBuilder();
        //        builder.addMavenDependency("uk.ac.standrews.cs", "shabdiz-job", "1.0-SNAPSHOT");
        //        builder.addFile(new File("/Users/masih/Desktop/a.jar"));
        //        builder.setMainClass("uk.ac.standrews.cs.shabdiz.job.WorkerMain");
        //        builder.start(new LocalHost(), ":0", "abc:1111");
    }

    @Test
    public void testPropertiesScanner() throws Exception {

        final String id = "uk.ac.standrews.cs.shabdiz.job.WorkerMain.properties:";
        final InputStream in = IOUtils.toInputStream(id + "kjhfkjhafadfgkasdhjfgkadsjhgfaksdhfgjf\n" + id + "{worker.remote.address=138.251.195.136%3A54816, pid=33306}\n" +
                "asdasdasasdsasasd");

        final Properties properties = Bootstrap.newProcessOutputScannerTask(in, id).call();

        LOGGER.info("Properties: ", properties);
        Assert.assertEquals(String.valueOf(33306), properties.getProperty("pid"));
        Assert.assertEquals("138.251.195.136:54816", properties.getProperty("worker.remote.address"));

    }
}
