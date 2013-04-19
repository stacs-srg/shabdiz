/*
 * Copyright 2013 University of St Andrews School of Computer Science
 * 
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

import org.junit.Assert;
import org.junit.Test;

/**
 * Various tests of local and remote process invocation, not intended to be run automatically.
 * They could be refactored to reduce code duplication, but it seems useful to keep them as self-contained examples.
 */
public class RemoteJavaProcessBuilderTests {

    /**
     * Runs the 'uname' process locally.
     * 
     * @throws Exception if the test fails
     */
    @Test
    public void runLocalProcess() throws Exception {

        Assert.fail("unimplemented ");
    }

    /**
     * Runs a Java process locally.
     * 
     * @throws Exception if the test fails
     */
    @Test
    public void runLocalJavaProcessShortLived() throws Exception {

        Assert.fail("unimplemented ");
    }

    /**
     * Runs a long-running Java process locally. The process will be left running after the test.
     * 
     * @throws Exception if the test fails
     */
    @Test
    public void runLocalJavaProcessLongLived() throws Exception {

        Assert.fail("unimplemented ");
    }

    /**
     * Gets the platform of a remote machine.
     * 
     * @throws Exception if the test fails
     */
    @Test
    public void getRemotePlatform() throws Exception {

        Assert.fail("unimplemented ");
    }

    /**
     * Runs the 'uname' process remotely, using username/password authentication.
     * 
     * @throws Exception if the test fails
     */
    @Test
    public void runRemoteProcessPassword() throws Exception {

        Assert.fail("unimplemented ");
    }

    /**
     * Runs the 'uname' process remotely, using username/public key authentication.
     * 
     * @throws Exception if the test fails
     */
    @Test
    public void runRemoteProcessPublicKey() throws Exception {

        Assert.fail("unimplemented ");
    }

    /**
     * Runs a Java process remotely, using username/password authentication, assuming nds.jar is already installed remotely.
     * 
     * @throws Exception if the test fails
     */
    @Test
    public void runRemoteJavaProcessPassword() throws Exception {

        Assert.fail("unimplemented ");
    }

    /**
     * Runs a Java process remotely, using username/public key authentication, assuming nds.jar is already installed remotely.
     * 
     * @throws Exception if the test fails
     */
    @Test
    public void runRemoteJavaProcessPublicKey() throws Exception {

        Assert.fail("unimplemented ");
    }

    /**
     * Runs a Java process remotely, using username/password authentication, and installing libraries dynamically.
     * 
     * @throws Exception if the test fails
     */
    @Test
    public void runRemoteJavaProcessPasswordLibraryInstallation() throws Exception {

        Assert.fail("unimplemented ");
    }

    /**
     * Runs a Java process remotely, using username/public key authentication, and installing libraries dynamically.
     * 
     * @throws Exception if the test fails
     */
    @Test
    public void runRemoteJavaProcessPublicKeyLibraryInstallation() throws Exception {

        Assert.fail("unimplemented ");
    }

    /**
     * Runs a long-running Java process remotely, using username/password authentication, and installing libraries dynamically.
     * This test should complete and the remote process should be left running.
     * 
     * @throws Exception if the test fails
     */
    @Test
    public void runRemoteJavaProcessLongLived() throws Exception {

        Assert.fail("unimplemented ");
    }

    /**
     * Kills off a remote process using public key authentication.
     * 
     * @throws Exception if the test fails
     */
    @Test
    public void killRemoteProcess() throws Exception {

        Assert.fail("unimplemented ");
    }

}
