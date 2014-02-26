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
    public void testPropertiesScanner() throws Exception {

        final String id = "uk.ac.standrews.cs.shabdiz.job.WorkerMain";
        final InputStream in = IOUtils.toInputStream("SLF4J: Failed to load class \"org.slf4j.impl.StaticLoggerBinder\".\n" + "SLF4J: Defaulting to no-operation (NOP) logger implementation\n" + "SLF4J: See http://www.slf4j.org/codes.html#StaticLoggerBinder for further details.\n" + id
                        + "{worker.remote.address=138.251.195.136%3A54816, pid=33306}\n" + "SLF4J: Failed to load class \"org.slf4j.impl.StaticLoggerBinder\".\n" + "SLF4J: Defaulting to no-operation (NOP) logger implementation\n"
                        + "SLF4J: See http://www.slf4j.org/codes.html#StaticLoggerBinder for further details.\n");

        final Properties properties = Bootstrap.newProcessOutputScannerTask(in, id).call();

        LOGGER.info("Properties: ", properties);
        Assert.assertEquals(String.valueOf(33306), properties.getProperty("pid"));
        Assert.assertEquals("138.251.195.136:54816", properties.getProperty("worker.remote.address"));

    }

        
}
