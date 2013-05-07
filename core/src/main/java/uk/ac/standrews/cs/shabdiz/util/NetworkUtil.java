/***************************************************************************
 *                                                                         *
 * nds Library                                                             *
 * Copyright (C) 2005-2010 Distributed Systems Architecture Research Group *
 * University of St Andrews, Scotland                                      *
 * http://www-systems.cs.st-andrews.ac.uk/                                 *
 *                                                                         *
 * This file is part of nds, a package of utility classes.                 *
 *                                                                         *
 * nds is free software: you can redistribute it and/or modify             *
 * it under the terms of the GNU General Public License as published by    *
 * the Free Software Foundation, either version 3 of the License, or       *
 * (at your option) any later version.                                     *
 *                                                                         *
 * nds is distributed in the hope that it will be useful,                  *
 * but WITHOUT ANY WARRANTY; without even the implied warranty of          *
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the           *
 * GNU General Public License for more details.                            *
 *                                                                         *
 * You should have received a copy of the GNU General Public License       *
 * along with nds.  If not, see <http://www.gnu.org/licenses/>.            *
 *                                                                         *
 ***************************************************************************/
package uk.ac.standrews.cs.shabdiz.util;

import java.io.IOException;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.NetworkInterface;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.Collections;
import java.util.Enumeration;

/**
 * Various network utilities.
 * 
 * @author Stuart Norcross (stuart@cs.st-andrews.ac.uk)
 * @author Masih Hajiarabderkani (mh638@st-andrews.ac.uk)
 */
public final class NetworkUtil {

    /** The maximum number that can be specified as port number. */
    public static final int MAX_PORT_NUMBER = 0xFFFF;

    /** The undefined port. */
    public static final int UNDEFINED_PORT = -1;

    /** Prevent instantiation of utility class. */
    private NetworkUtil() {

    }

    /**
     * Returns the first non-loopback IPv4 address (Inet4Address) that can be found for an interface on the local host.
     * This method should be used in place of InetAddress.getLocalHost(), which may return an Inet6Address object
     * corresponding to the IPv6 address of a local interface. The bind operation is not supported by this
     * address family.
     * 
     * @return the first IPv4 address found
     * @throws UnknownHostException if no IPv4 address can be found
     */
    public static InetAddress getLocalIPv4Address() throws UnknownHostException {

        final InetAddress default_local_address = InetAddress.getLocalHost();

        // Return the default local address if it's an IPv4 address and isn't the loopback address. This will work in most cases.
        if (default_local_address instanceof Inet4Address && !default_local_address.isLoopbackAddress()) { return default_local_address; }

        // Otherwise, look for an IPv4 address among the other interfaces.
        InetAddress loopback_address = null;

        try {
            final Enumeration<NetworkInterface> interfaces_enumeration = NetworkInterface.getNetworkInterfaces();

            if (interfaces_enumeration != null) {
                for (final NetworkInterface network_interface : Collections.list(interfaces_enumeration)) {
                    for (final InetAddress address : Collections.list(network_interface.getInetAddresses())) {
                        if (address instanceof Inet4Address) {
                            // Found an IPv4 address. Return it if it's not loopback, otherwise remember it.
                            if (!address.isLoopbackAddress()) { return address; }
                            loopback_address = address;
                        }
                    }
                }
            }

            // Haven't found any non-loopback IPv4 address, so return loopback if available.
            if (loopback_address != null) { return loopback_address; }
        }
        catch (final SocketException e) {
            // Ignore.
        }

        throw new UnknownHostException("local host has no interface with an IPv4 address");
    }

    /**
     * Returns an InetSocketAddress corresponding to a local non-loopback IPv4 address.
     * 
     * @param port the port
     * @return the corresponding local address
     * @throws UnknownHostException if no IPv4 address can be found
     */
    public static InetSocketAddress getLocalIPv4InetSocketAddress(final int port) throws UnknownHostException {

        return new InetSocketAddress(getLocalIPv4Address(), port);
    }

    /**
     * Extracts an InetSocketAddress from a string of the form "host:port". If the host part is empty, the local
     * loopback address is used. If the port part is empty, the specified default port is used.
     * 
     * @param host_and_port a string of the form "host:port"
     * @param default_port the default port to be used if the port is not specified
     * @return a corresponding InetSocketAddress
     * @throws UnknownHostException if the specified host cannot be resolved
     */
    public static InetSocketAddress extractInetSocketAddress(final String host_and_port, final int default_port) throws UnknownHostException {

        final String host_name = extractHostName(host_and_port);
        final int port = extractPortNumber(host_and_port);

        if (host_name == "") {
            if (port == UNDEFINED_PORT) { return getLocalIPv4InetSocketAddress(default_port); }
            return getLocalIPv4InetSocketAddress(port);
        }
        else if (port == UNDEFINED_PORT) {
            return getInetSocketAddress(host_name, default_port);
        }
        else {
            return getInetSocketAddress(host_name, port);
        }
    }

    /**
     * Creates an InetSocketAddress for a given host and port.
     * 
     * @param host the host
     * @param port the port
     * @return a corresponding InetSocketAddress
     * @throws UnknownHostException if the specified host cannot be resolved
     */
    public static InetSocketAddress getInetSocketAddress(final InetAddress host, final int port) throws UnknownHostException {

        return new InetSocketAddress(host, port);
    }

    /**
     * Creates an InetSocketAddress for a given host and port.
     * 
     * @param host_name the host
     * @param port the port
     * @return a corresponding InetSocketAddress
     * @throws UnknownHostException if the specified host cannot be resolved
     */
    public static InetSocketAddress getInetSocketAddress(final String host_name, final int port) throws UnknownHostException {

        return getInetSocketAddress(InetAddress.getByName(host_name), port);
    }

    /**
     * Extracts a host name from a string of the form "[host][:][port]". If the
     * host part is empty, the empty string is returned.
     * 
     * @param host_and_port a string of the form "host:port", "host", "host:", ":port"
     * @return the host name
     */
    public static String extractHostName(final String host_and_port) {

        if (host_and_port == null) { return ""; }

        final int separator_index = host_and_port.indexOf(":");

        if (separator_index != -1) { return host_and_port.substring(0, separator_index); }
        return host_and_port; // No port was specified.
    }

    /**
     * Extracts a port number as a string from a string of the form "[host][:][port]". If
     * the port part is empty, the string representation of {@link #UNDEFINED_PORT} is returned.
     * 
     * @param host_and_port a string of the form "host:port", "host", "host:" or ":port"
     * @return the port number as a string
     */
    public static String extractPortNumberAsString(final String host_and_port) {

        if (host_and_port == null) { return String.valueOf(UNDEFINED_PORT); }

        final int separator_index = host_and_port.indexOf(":");

        // Check for "<host>", "<host>:" and ":"
        if (separator_index == -1 || separator_index == host_and_port.length() - 1) { return String.valueOf(UNDEFINED_PORT); }

        return host_and_port.substring(separator_index + 1);
    }

    /**
     * Extracts a port number from a string of the form "[host][:][port]". If
     * the port part is empty {@link #UNDEFINED_PORT} is returned.
     * 
     * @param host_and_port a string of the form "host:port", "host", "host:" or ":port"
     * @return the port number
     */
    public static int extractPortNumber(final String host_and_port) {

        try {
            return Integer.parseInt(extractPortNumberAsString(host_and_port));
        }
        catch (final NumberFormatException e) {
            return UNDEFINED_PORT;
        }
    }

    /**
     * Tests whether a given address is a valid local address.
     * 
     * @param address an address
     * @return true if the address is a valid local address
     */
    public static boolean isValidLocalAddress(final InetAddress address) {

        boolean local = address.isAnyLocalAddress() || address.isLoopbackAddress();
        if (!local) {
            try {
                local = NetworkInterface.getByInetAddress(address) != null;
            }
            catch (final SocketException e) {
                local = false;
            }
        }
        return local;
    }

    /**
     * Returns a description of a given host address.
     * 
     * @param address an address
     * @return a description of the address
     */
    public static String formatHostAddress(final InetSocketAddress address) {

        if (address != null) {

            final String host = address.getAddress().getHostAddress();
            final int port = address.getPort();

            return formatHostAddress(host, port);
        }
        return null;
    }

    /**
     * Returns a description of a given host address.
     * 
     * @param host an IP address
     * @param port a port
     * @return a description of the address
     */
    public static String formatHostAddress(final String host, final int port) {

        return host + ":" + port;
    }

    /**
     * Returns a description of a given socket.
     * 
     * @param socket a socket
     * @return a description of the socket's address
     */
    public static String formatHostAddress(final Socket socket) {

        return formatHostAddress(socket.getInetAddress(), socket.getPort());
    }

    /**
     * Returns a description of a given host address.
     * 
     * @param address an address
     * @param port a port
     * @return a description of the address
     */
    public static String formatHostAddress(final InetAddress address, final int port) {

        return formatHostAddress(address.getHostAddress(), port);
    }

    /**
     * Alternative to {@link ServerSocket} constructors that calls {@link ServerSocket#setReuseAddress(boolean)} to enable reuse of the fixed
     * local port even when there is a previous connection to that port in the timeout state.
     * 
     * @param local_port the local port
     * @return a socket bound to the given local port
     * @throws IOException if the new socket can't be connected to
     */
    public static ServerSocket makeReusableServerSocket(final int local_port) throws IOException {

        final ServerSocket socket = new ServerSocket();
        socket.setReuseAddress(true);

        socket.bind(new InetSocketAddress(local_port));

        return socket;
    }

    /**
     * Alternative to {@link ServerSocket} constructors that calls {@link ServerSocket#setReuseAddress(boolean)} to enable reuse of the fixed
     * local port even when there is a previous connection to that port in the timeout state.
     * 
     * @param local_address the address to which the socket should be bound
     * @param local_port the local port
     * @return a socket bound to the given local port
     * @throws IOException if the new socket can't be connected to
     */
    public static ServerSocket makeReusableServerSocket(final InetAddress local_address, final int local_port) throws IOException {

        final ServerSocket socket = new ServerSocket();
        socket.setReuseAddress(true);

        socket.bind(new InetSocketAddress(local_address, local_port));

        return socket;
    }

    /**
     * finds a free TCP port on the local machine.
     * 
     * @return a free port on the local machine
     * @throws IOException if unable to check for free port
     */
    public static synchronized int findFreeLocalTCPPort() throws IOException {

        ServerSocket server_socket = null;
        try {

            server_socket = makeReusableServerSocket(0);
            return server_socket.getLocalPort();
        }
        finally {

            try {
                server_socket.close();
            }
            catch (final IOException e) {
                // ignore
            }
        }
    }

    /**
     * Constructs an {@link InetSocketAddress address} from an String representation of an address that is produced by {@link InetSocketAddress#toString()}.
     * 
     * @param address_in_string the address in string
     * @return the address
     * @throws UnknownHostException if host is unknown
     * @see InetSocketAddress#toString()
     */
    public static InetSocketAddress getAddressFromString(final String address_in_string) throws UnknownHostException {

        final String serialized_inet_socket_address = address_in_string;
        if (serialized_inet_socket_address == null || serialized_inet_socket_address.equals("null")) { return null; }
        final String[] components = serialized_inet_socket_address.split(":", -1);
        final String host = components[0];
        final int port = Integer.parseInt(components[1]);
        final String name = getName(host);
        final byte[] address_bytes = getBytes(host);
        final InetAddress addr = name.equals("") ? InetAddress.getByAddress(address_bytes) : InetAddress.getByAddress(name, address_bytes);
        return new InetSocketAddress(addr, port);
    }

    //---------------------------------------------------------

    private static String getName(final String host) {

        final String[] name_address = host.split("/", -1);
        return name_address[0];
    }

    private static byte[] getBytes(final String host) {

        final String[] name_address = host.split("/", -1);
        final String[] byte_strings = name_address[1].split("\\.", -1);
        final byte[] bytes = new byte[byte_strings.length];

        for (int i = 0; i < byte_strings.length; i++) {
            final Integer j = Integer.valueOf(byte_strings[i]);
            bytes[i] = j.byteValue();
        }

        return bytes;
    }
}
