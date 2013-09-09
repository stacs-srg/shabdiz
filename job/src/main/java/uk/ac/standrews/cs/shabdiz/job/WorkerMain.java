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
package uk.ac.standrews.cs.shabdiz.job;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.rmi.AlreadyBoundException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeoutException;
import java.util.logging.Level;
import java.util.logging.Logger;
import uk.ac.standrews.cs.shabdiz.host.exec.Bootstrap;
import uk.ac.standrews.cs.shabdiz.util.CommandLineArgs;
import uk.ac.standrews.cs.shabdiz.util.NetworkUtil;

/**
 * The entry point to start up a new worker.
 *
 * @author Masih Hajiarabderkani (mh638@st-andrews.ac.uk)
 */
public class WorkerMain extends Bootstrap {

    static final String WORKER_REMOTE_ADDRESS_KEY = "worker.remote.address";
    private static final Logger LOGGER = Logger.getLogger(WorkerMain.class.getName());
    private static final String LOCAL_ADDRESS_KEY = "-s";
    private static final String CALLBACK_ADDRESS_KEY = "-c";
    private InetSocketAddress local_address;
    private InetSocketAddress launcher_callback_address;

    /**
     * The following command line parameters are available:
     * <dl>
     * <dt>-shost:port (required)</dt>
     * <dd>Specifies the local address and port at which the worker service should be made available.</dd>
     * <dt>-chost:port (required)</dt>
     * <dd>Specifies the address and port of an existing launcher callback server.</dd>
     * </dl>
     *
     * @param args see above
     * @throws UnknownHostException if a node cannot be created using the given local address
     * @throws AlreadyBoundException if another node is already bound in the registry
     * @throws InterruptedException the interrupted exception
     * @throws TimeoutException the timeout exception
     */
    @Override
    protected void deploy(final String... args) throws UnknownHostException, InterruptedException, TimeoutException, AlreadyBoundException {

        final WorkerMain worker_main = new WorkerMain();
        worker_main.configure(args);
        try {
            final DefaultWorkerRemote worker = worker_main.createNode();
            setProperty(WORKER_REMOTE_ADDRESS_KEY, String.valueOf(worker.getAddress()));
            LOGGER.info("Started Shabdiz worker at " + worker.getAddress());
        }
        catch (final IOException e) {
            LOGGER.log(Level.SEVERE, "Couldn't start Shabdiz worker at " + worker_main.local_address, e);
        }
    }

    /**
     * Creates a new worker node.
     *
     * @return the created worker node
     * @throws IOException Signals that an I/O exception has occurred.
     * @throws AlreadyBoundException the already bound exception
     * @throws InterruptedException the interrupted exception
     * @throws TimeoutException the timeout exception
     */
    DefaultWorkerRemote createNode() throws IOException, AlreadyBoundException, InterruptedException, TimeoutException {

        return new DefaultWorkerRemote(local_address, launcher_callback_address);
    }

    private void configure(String... args) throws UnknownHostException {

        final Map<String, String> arguments = CommandLineArgs.parseCommandLineArgs(args);
        configureLocalAddress(arguments);
        configureLauncherAddress(arguments);
    }

    private void configureLauncherAddress(final Map<String, String> arguments) throws UnknownHostException {

        final String known_address_parameter = arguments.get(CALLBACK_ADDRESS_KEY);
        if (known_address_parameter == null) {
            usage();
        }
        launcher_callback_address = NetworkUtil.extractInetSocketAddress(known_address_parameter, 0);
    }

    private static void usage() {

        System.err.println("Usage: -Chost:port [-Shost:port]");
    }

    private void configureLocalAddress(final Map<String, String> arguments) throws UnknownHostException {

        final String local_address_parameter = arguments.get(LOCAL_ADDRESS_KEY);
        final int port = local_address_parameter != null ? NetworkUtil.extractPortNumber(local_address_parameter) : 0;
        local_address = NetworkUtil.getLocalIPv4InetSocketAddress(port);
    }

    static String[] constructCommandLineArguments(final InetSocketAddress callback_address, final Integer port) {

        final List<String> arguments = new ArrayList<String>();
        arguments.add(CALLBACK_ADDRESS_KEY + NetworkUtil.formatHostAddress(callback_address));

        if (port != null) {
            arguments.add(LOCAL_ADDRESS_KEY + NetworkUtil.formatHostAddress("", port));
        }
        return arguments.toArray(new String[arguments.size()]);
    }
}
