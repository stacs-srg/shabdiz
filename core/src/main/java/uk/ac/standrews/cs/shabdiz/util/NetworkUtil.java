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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.*;
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
    private static final Logger LOGGER = LoggerFactory.getLogger(NetworkUtil.class);

    /** Prevent instantiation of utility class. */
    private NetworkUtil() {

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

        if (host_name.equals("")) {
            if (port == UNDEFINED_PORT) {
                return getLocalIPv4InetSocketAddress(default_port);
            }
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
     * Extracts a port number from a string of the form "[host][:][port]". If
     * the port part is empty {@link #UNDEFINED_PORT} is returned.
     *
     * @param host_and_port a string of the form "host:port", "host", "host:" or ":port"
     * @return the port number
     */
    public static int extractPortNumber(final String host_and_port) {

        try {
            return Integer.parseInt(extractPortNumberAsString(host_and_port));
        } catch (final NumberFormatException e) {
            return UNDEFINED_PORT;
        }
    }

    /**
     * Extracts a port number as a string from a string of the form "[host][:][port]". If
     * the port part is empty, the string representation of {@link #UNDEFINED_PORT} is returned.
     *
     * @param host_and_port a string of the form "host:port", "host", "host:" or ":port"
     * @return the port number as a string
     */
    public static String extractPortNumberAsString(final String host_and_port) {

        if (host_and_port == null) {
            return String.valueOf(UNDEFINED_PORT);
        }

        final int separator_index = host_and_port.indexOf(":");

        // Check for "<host>", "<host>:" and ":"
        if (separator_index == -1 || separator_index == host_and_port.length() - 1) {
            return String.valueOf(UNDEFINED_PORT);
        }

        return host_and_port.substring(separator_index + 1);
    }

    /**
     * Extracts a host name from a string of the form "[host][:][port]". If the
     * host part is empty, the empty string is returned.
     *
     * @param host_and_port a string of the form "host:port", "host", "host:", ":port"
     * @return the host name
     */
    public static String extractHostName(final String host_and_port) {

        if (host_and_port == null) {
            return "";
        }

        final int separator_index = host_and_port.indexOf(":");

        if (separator_index != -1) {
            return host_and_port.substring(0, separator_index);
        }
        return host_and_port; // No port was specified.
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
        if (default_local_address instanceof Inet4Address && !default_local_address.isLoopbackAddress()) {
            return default_local_address;
        }

        // Otherwise, look for an IPv4 address among the other interfaces.
        InetAddress loopback_address = null;

        try {
            final Enumeration<NetworkInterface> interfaces_enumeration = NetworkInterface.getNetworkInterfaces();

            if (interfaces_enumeration != null) {
                for (final NetworkInterface network_interface : Collections.list(interfaces_enumeration)) {
                    for (final InetAddress address : Collections.list(network_interface.getInetAddresses())) {
                        if (address instanceof Inet4Address) {
                            // Found an IPv4 address. Return it if it's not loopback, otherwise remember it.
                            if (!address.isLoopbackAddress()) {
                                return address;
                            }
                            loopback_address = address;
                        }
                    }
                }
            }

            // Haven't found any non-loopback IPv4 address, so return loopback if available.
            if (loopback_address != null) {
                return loopback_address;
            }
        } catch (final SocketException e) {
            LOGGER.trace("ignoring error", e);
        }

        throw new UnknownHostException("local host has no interface with an IPv4 address");
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
            } catch (final SocketException e) {
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
     * Constructs an {@link InetSocketAddress address} from an String representation of an address that is produced by {@link InetSocketAddress#toString()}.
     *
     * @param address_in_string the address in string
     * @return the address
     * @throws UnknownHostException if host is unknown
     * @see InetSocketAddress#toString()
     */
    public static InetSocketAddress getAddressFromString(final String address_in_string) throws UnknownHostException {

        if (address_in_string == null || address_in_string.equals("null")) {
            return null;
        }
        final String[] components = address_in_string.split(":", -1);
        final String host = components[0];
        final int port = Integer.parseInt(components[1]);
        final String name = getName(host);
        final byte[] address_bytes = getBytes(host);
        final InetAddress addr = name.equals("") ? InetAddress.getByAddress(address_bytes) : InetAddress.getByAddress(name, address_bytes);
        return new InetSocketAddress(addr, port);
    }

    //---------------------------------------------------------

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

    private static String getName(final String host) {

        final String[] name_address = host.split("/", -1);
        return name_address[0];
    }
}
