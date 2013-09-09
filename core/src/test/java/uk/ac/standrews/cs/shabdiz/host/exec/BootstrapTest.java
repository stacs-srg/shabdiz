package uk.ac.standrews.cs.shabdiz.host.exec;

import java.io.File;
import org.junit.Test;
import uk.ac.standrews.cs.shabdiz.host.LocalHost;

/** @author Masih Hajiarabderkani (mh638@st-andrews.ac.uk) */
public class BootstrapTest {

    @Test
    public void testDeploy() throws Exception {

        final AgentBasedJavaProcessBuilder builder = new AgentBasedJavaProcessBuilder();
        builder.addMavenDependency("uk.ac.standrews.cs", "shabdiz-job", "1.0-SNAPSHOT");
        builder.addFile(new File("/Users/masih/Desktop/a.jar"));
        builder.setMainClass("uk.ac.standrews.cs.shabdiz.job.WorkerMain");
        builder.start(new LocalHost(), ":0", "abc:1111");
    }
}
