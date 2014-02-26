package uk.ac.standrews.cs.shabdiz.host.exec;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import java.util.concurrent.Executors;
import org.apache.commons.io.IOUtils;
import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.standrews.cs.shabdiz.host.LocalHost;

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

    @Test
    public void testJavaProcess() throws Exception {

        final LocalHost host = new LocalHost();
        AgentBasedJavaProcessBuilder.clearCachedFilesOnHost(host);
        AgentBasedJavaProcessBuilder builder = new AgentBasedJavaProcessBuilder();
        builder.addMavenDependency("ch.qos.logback:logback-classic:1.1.1");
        builder.addMavenDependency("ch.qos.logback:logback-core:1.1.1");
        builder.setMainClassName("aaa");
        final Process start = builder.start(host);
        Executors.newSingleThreadExecutor().submit(new Runnable() {

            @Override
            public void run() {

                try {
                    final InputStream stream = start.getInputStream();
                    int read = stream.read();
                    while(read != -1){
                        System.out.print((char) read);
                        read = stream.read();
                    }
                }
                catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }) ;
        Executors.newSingleThreadExecutor().submit(new Runnable() {

            @Override
            public void run() {

                try {
                    final InputStream stream = start.getErrorStream();
                    int read = stream.read();
                    while(read != -1){
                        System.out.print((char) read);
                        read = stream.read(); 
                    }
                }
                catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }).get();
        

    }
}
