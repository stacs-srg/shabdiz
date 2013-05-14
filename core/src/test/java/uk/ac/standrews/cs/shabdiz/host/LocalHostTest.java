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

package uk.ac.standrews.cs.shabdiz.host;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import uk.ac.standrews.cs.shabdiz.host.exec.Commands;
import uk.ac.standrews.cs.shabdiz.platform.LocalPlatform;
import uk.ac.standrews.cs.shabdiz.util.ProcessUtil;

import java.io.File;
import java.net.InetAddress;

import static org.junit.Assert.*;

public class LocalHostTest {

    private LocalHost local_host;

    @Before
    public void setUp() throws Exception {
        local_host = new LocalHost();
    }

    @After
    public void tearDown() throws Exception {
        local_host.close();
    }

    @Test
    public void testUploadSingleFile() throws Exception {

    }

    @Test
    public void testDownloadSingleFile() throws Exception {

    }

    @Test
    public void testUploadMultipleFiles() throws Exception {

    }

    @Test
    public void testDownloadMultipleFile() throws Exception {

    }

    @Test
    public void testExecuteWithWorkingDirectory() throws Exception {
        final File root = new File(new File(".").getCanonicalPath()).getParentFile();
        final Process pwd_process = local_host.execute("../", Commands.CURRENT_WORKING_DIRECTORY.get(local_host.getPlatform()));
        final String pwd_process_output = ProcessUtil.awaitNormalTerminationAndGetOutput(pwd_process);
        assertEquals(root.getAbsolutePath(), pwd_process_output);
    }

    @Test
    public void testExecuteWithoutWorkingDirectory() throws Exception {
        final File root = new File(".");
        final Process pwd_process = local_host.execute(Commands.CURRENT_WORKING_DIRECTORY.get(local_host.getPlatform()));
        final String pwd_process_output = ProcessUtil.awaitNormalTerminationAndGetOutput(pwd_process);
        assertEquals(root.getCanonicalPath(), pwd_process_output);
    }

    @Test
    public void testGetPlatform() throws Exception {

        assertEquals(LocalPlatform.getInstance(), local_host.getPlatform());
    }

    @Test
    public void testGetAddress() throws Exception {

        assertEquals(InetAddress.getLocalHost(), local_host.getAddress());
    }

    @Test
    public void testGetName() throws Exception {
        assertEquals(InetAddress.getLocalHost().getHostName(), local_host.getName());
    }

    @Test
    public void testIsLocal() throws Exception {
        assertTrue(local_host.isLocal());
    }
}
