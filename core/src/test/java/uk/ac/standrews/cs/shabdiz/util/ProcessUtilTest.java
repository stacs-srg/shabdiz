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
package uk.ac.standrews.cs.shabdiz.util;

import static org.junit.Assert.fail;

import java.io.IOException;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import uk.ac.standrews.cs.shabdiz.host.Host;
import uk.ac.standrews.cs.shabdiz.host.LocalHost;
import uk.ac.standrews.cs.shabdiz.host.exec.Commands;
import uk.ac.standrews.cs.shabdiz.platform.Platform;

/**
 * Tests {@link ProcessUtil}.
 * 
 * @author Masih Hajiarabderkani (mh638@st-andrews.ac.uk)
 */
public class ProcessUtilTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(ProcessUtilTest.class);
    private Host local_host;

    /**
     * Instantiates a new local host.
     * 
     * @throws Exception if host instantiation fails
     */
    @Before
    public void setUp() throws Exception {

        local_host = new LocalHost();
    }

    /**
     * Closes the instantiated host in {@link #setUp()}.
     * 
     * @throws Exception if host closure fails
     */
    @After
    public void tearDown() throws Exception {

        local_host.close();
    }

    /**
     * Executes an invalid command and expects {@link IOException} to be thrown while waiting for the execution to terminate.
     * 
     * @throws IOException if command cannot be executed on host
     * @throws InterruptedException if interrupted while waiting for the command execution to complete
     */
    @Test
    public void testWaitForSuccessWithInvalidCommand() throws IOException, InterruptedException {

        final Process process = local_host.execute("the command that is not.");
        try {
            ProcessUtil.waitForNormalTerminationAndGetOutput(process);
            fail("expected exception");
        }
        catch (final IOException e) {
            LOGGER.debug("expected exception occured ", e);
        }
    }

    /**
     * Executes number of valid commands and checks their output for correctness.
     * 
     * @throws IOException if command cannot be executed on host
     * @throws InterruptedException if interrupted while waiting for the command execution to complete
     */
    @Test
    public void testWaitForSuccessWithVaildCommand() throws IOException, InterruptedException {

        final Platform local_platform = local_host.getPlatform();
        LOGGER.info("Platform {}", local_platform);
        final String test_message = "testing successful command execution";
        final Process username_process = local_host.execute(Commands.ECHO.get(local_platform, test_message));
        Assert.assertEquals(test_message, ProcessUtil.waitForNormalTerminationAndGetOutput(username_process));
    }
}
