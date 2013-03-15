/*
 * This file is part of Shabdiz.
 * 
 * Shabdiz is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * Shabdiz is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with Shabdiz.  If not, see <http://www.gnu.org/licenses/>.
 */
package uk.ac.standrews.cs.shabdiz.active;

import java.util.HashSet;
import java.util.Set;

import org.junit.Test;

import uk.ac.standrews.cs.shabdiz.api.Platform;
import uk.ac.standrews.cs.shabdiz.process.RemoteJavaProcessBuilder;
import uk.ac.standrews.cs.shabdiz.util.URL;
import uk.ac.standrews.cs.shabdiz.zold.ClassPath;
import uk.ac.standrews.cs.shabdiz.zold.HostDescriptor;

/**
 * Various tests of local and remote process invocation, not intended to be run automatically.
 * They could be refactored to reduce code duplication, but it seems useful to keep them as self-contained examples.
 * 
 * @author Graham Kirby (graham.kirby@st-andrews.ac.uk)
 */
public class ProcessManagerTests {

    /**
     * Runs the 'uname' process locally.
     * 
     * @throws Exception if the test fails
     */
    @Test
    public void runLocalProcess() throws Exception {

        final Process p = new HostDescriptor().getManagedHost().execute("uname -a");

        p.waitFor();
        p.destroy();
    }

    /**
     * Runs a Java process locally.
     * 
     * @throws Exception if the test fails
     */
    @Test
    public void runLocalJavaProcessShortLived() throws Exception {

        final RemoteJavaProcessBuilder process_builder = new RemoteJavaProcessBuilder(TestClassShortLived.class);
        process_builder.addCurrentJVMClasspath();

        final Process p = process_builder.start(new HostDescriptor().getManagedHost());

        p.waitFor();
        p.destroy();
    }

    /**
     * Runs a long-running Java process locally. The process will be left running after the test.
     * 
     * @throws Exception if the test fails
     */
    @Test
    public void runLocalJavaProcessLongLived() throws Exception {

        final RemoteJavaProcessBuilder process_builder = new RemoteJavaProcessBuilder(TestClassLongLived.class);
        process_builder.addCurrentJVMClasspath();

        process_builder.start(new HostDescriptor().getManagedHost());
    }

    /**
     * Gets the platform of a remote machine.
     * 
     * @throws Exception if the test fails
     */
    @Test
    public void getRemotePlatform() throws Exception {

        final HostDescriptor host_descriptor = new HostDescriptor(true);
        final Platform platform = host_descriptor.getPlatform();

        System.out.println("platform: " + platform);
    }

    /**
     * Runs the 'uname' process remotely, using username/password authentication.
     * 
     * @throws Exception if the test fails
     */
    @Test
    public void runRemoteProcessPassword() throws Exception {

        runRemoteProcess(true);
    }

    /**
     * Runs the 'uname' process remotely, using username/public key authentication.
     * 
     * @throws Exception if the test fails
     */
    @Test
    public void runRemoteProcessPublicKey() throws Exception {

        runRemoteProcess(false);
    }

    /**
     * Runs a Java process remotely, using username/password authentication, assuming nds.jar is already installed remotely.
     * 
     * @throws Exception if the test fails
     */
    @Test
    public void runRemoteJavaProcessPassword() throws Exception {

        runRemoteJavaProcess(true);
    }

    /**
     * Runs a Java process remotely, using username/public key authentication, assuming nds.jar is already installed remotely.
     * 
     * @throws Exception if the test fails
     */
    @Test
    public void runRemoteJavaProcessPublicKey() throws Exception {

        runRemoteJavaProcess(false);
    }

    /**
     * Runs a Java process remotely, using username/password authentication, and installing libraries dynamically.
     * 
     * @throws Exception if the test fails
     */
    @Test
    public void runRemoteJavaProcessPasswordLibraryInstallation() throws Exception {

        runRemoteJavaProcessLibraryInstallation(true);
    }

    /**
     * Runs a Java process remotely, using username/public key authentication, and installing libraries dynamically.
     * 
     * @throws Exception if the test fails
     */
    @Test
    public void runRemoteJavaProcessPublicKeyLibraryInstallation() throws Exception {

        runRemoteJavaProcessLibraryInstallation(false);
    }

    /**
     * Runs a long-running Java process remotely, using username/password authentication, and installing libraries dynamically.
     * This test should complete and the remote process should be left running.
     * 
     * @throws Exception if the test fails
     */
    @Test
    public void runRemoteJavaProcessLongLived() throws Exception {

        final Set<URL> urls = new HashSet<URL>();
        urls.add(new URL("https://builds.cs.st-andrews.ac.uk/job/nds/lastStableBuild/artifact/bin/nds.jar"));
        final HostDescriptor host_descriptor = new HostDescriptor(true).applicationURLs(urls);

        //        final ProcessDescriptor java_process_descriptor = new JavaProcessDescriptor().classToBeInvoked(TestClassLongLived.class);
        //        host_descriptor.getProcessManager().runProcess(java_process_descriptor);
    }

    /**
     * Kills off a remote process using public key authentication.
     * 
     * @throws Exception if the test fails
     */
    @Test
    public void killRemoteProcess() throws Exception {

        final HostDescriptor host_descriptor = new HostDescriptor(true);
        host_descriptor.killMatchingProcesses("TestClassLongLived");
        host_descriptor.clearTempFiles();
    }

    // -------------------------------------------------------------------------------------------------------

    private void runRemoteProcess(final boolean use_password) throws Exception {

        //        final HostDescriptor host_descriptor = new HostDescriptor(use_password);
        //        final ProcessDescriptor process_descriptor = new ProcessDescriptor().command("uname -a");
        //        final Process p = host_descriptor.getProcessManager().runProcess(process_descriptor);
        //        p.waitFor();
        //        p.destroy();
    }

    private void runRemoteJavaProcess(final boolean use_password) throws Exception {

        final HostDescriptor host_descriptor = new HostDescriptor(use_password);
        final ClassPath class_path = new ClassPath("~" + host_descriptor.getCredentials().getUsername() + "/nds.jar");
        //        host_descriptor.classPath(class_path);
        //        final ProcessDescriptor java_process_descriptor = new JavaProcessDescriptor().classToBeInvoked(TestClassShortLived.class);
        //        final Process p = host_descriptor.getProcessManager().runProcess(java_process_descriptor);
        //
        //        p.waitFor();
        //        p.destroy();
    }

    private void runRemoteJavaProcessLibraryInstallation(final boolean use_password) throws Exception {

        final Set<URL> urls = new HashSet<URL>();
        urls.add(new URL("https://builds.cs.st-andrews.ac.uk/job/nds/lastStableBuild/artifact/bin/nds.jar"));
        final HostDescriptor host_descriptor = new HostDescriptor(use_password).applicationURLs(urls);
        //        final ProcessDescriptor java_process_descriptor = new JavaProcessDescriptor().classToBeInvoked(TestClassShortLived.class);
        //        final Process p = host_descriptor.getProcessManager().runProcess(java_process_descriptor);
        //
        //        p.waitFor();
        //        p.destroy();
    }
}
