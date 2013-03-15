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
package uk.ac.standrews.cs.shabdiz.util;

import java.util.ArrayList;
import java.util.List;

/**
 * Provides host address pattern parsing.
 *
 * @author Graham Kirby (graham.kirby@st-andrews.ac.uk)
 */
public final class Patterns {

    private static final int MAX_BYTE_VALUE = 255;

    /**
     * Prevent instantiation of utility class.
     */
    private Patterns() {

    }

    /**
     * <p>Resolves patterns of two forms into lists of host addresses.</p>
     * 
     * <p>The first form represents an address range by the start and end addresses separated by the string " - ". The non-common parts of the start and end addresses
     * must be parseable as integers; the resulting list enumerates all the intermediate addresses and includes the specified start and end addresses. For example,
     * the pattern "compute-0-3 - compute-0-735" yields a list of 733 addresses including "compute-0-3", "compute-0-4", ..., "compute-0-734", "compute-0-735".</p>
     * 
     * <p>The second form represents a range of 256 IPv4 addresses by a dotted-quad wild-carded in the least significant quad. For example, the pattern "138.251.195.*"
     * yields a list of 256 addresses including "138.251.195.0", "138.251.195.1", ..., "138.251.195.254", "138.251.195.255".</p>
     * 
     * @param host_pattern a host pattern
     * @return a list of host addresses matching the pattern, or the original pattern if the pattern cannot be parsed
     */
    public static List<String> resolveHostPattern(final String host_pattern) {

        return resolveHostPattern(host_pattern, Integer.MAX_VALUE);
    }

    /**
     * <p>Resolves patterns of two forms into lists of host addresses.</p>
     * 
     * <p>The first form represents an address range by the start and end addresses separated by the string " - ". The non-common parts of the start and end addresses
     * must be parseable as integers; the resulting list enumerates all the intermediate addresses and includes the specified start and end addresses. For example,
     * the pattern "compute-0-3 - compute-0-735" yields a list of 733 addresses including "compute-0-3", "compute-0-4", ..., "compute-0-734", "compute-0-735".</p>
     * 
     * <p>The second form represents a range of 256 IPv4 addresses by a dotted-quad wild-carded in the least significant quad. For example, the pattern "138.251.195.*"
     * yields a list of 256 addresses including "138.251.195.0", "138.251.195.1", ..., "138.251.195.254", "138.251.195.255".</p>
     * 
     * @param host_pattern a host pattern
     * @param range_limit the maximum number of hosts to be returned
     * @return a list of host addresses matching the pattern, or the original pattern if the pattern cannot be parsed
     */
    public static List<String> resolveHostPattern(final String host_pattern, final int range_limit) {

        final List<String> hosts = new ArrayList<String>();

        tryArbitraryRange(host_pattern, hosts, range_limit);

        if (hosts.size() == 0) {
            tryDottedQuad(host_pattern, hosts);
        }

        if (hosts.size() == 0) {
            hosts.add(host_pattern); // No pattern detected so treat as a single host.
        }

        return hosts;
    }

    /**
     * Tries to parse pattern representing an address range by the start and end addresses separated by the string " - ", adding addresses to the
     * given list if successful.
     * 
     * @param host_pattern a host pattern
     * @param hosts a list into which the hosts are inserted
     * @param range_limit the maximum number of hosts to be inserted
     */
    private static void tryArbitraryRange(final String host_pattern, final List<String> hosts, final int range_limit) {

        final String[] split = host_pattern.split(" - ");

        if (split.length == 2) {

            final String address1 = split[0];
            final String address2 = split[1];

            final String prefix = getCommonNonNumericPrefix(address1, address2);

            final String suffix1 = address1.substring(prefix.length());
            final String suffix2 = address2.substring(prefix.length());

            addHostsInRange(prefix, suffix1, suffix2, hosts, range_limit);
        }
    }

    /**
     * Enumerates the host addresses formed by concatenating the given prefix with all the suffixes lying between the given
     * suffixes, if the suffixes are both parseable as integers.
     * 
     * @param prefix an arbitrary prefix
     * @param suffix1 the suffix denoting the lower end of the range
     * @param suffix2 the suffix denoting the higher end of the range
     * @param hosts a list into which the hosts are inserted
     * @param range_limit the maximum number of hosts to be inserted
     */
    private static void addHostsInRange(final String prefix, final String suffix1, final String suffix2, final List<String> hosts, final int range_limit) {

        try {
            final int start_index = Integer.parseInt(suffix1);
            final int end_index = Integer.parseInt(suffix2);

            if (start_index >= 0) {

                // Allow for integer overflow where range_limit is set to MAX_VALUE.
                final int end = Math.min(end_index, Math.max(start_index + range_limit - 1, range_limit));

                for (int index = start_index; index <= end; index++) {
                    // Try to preserve any leading zeros in the address suffixes.
                    hosts.add(prefix + padWithLeadingZeros(index, suffix1.length()));
                }
            }
        }
        catch (final NumberFormatException e) {
            // Ignore.
        }
    }

    private static String padWithLeadingZeros(final int index, final int length) {

        String result = String.valueOf(index);
        final int number_of_zeros_required = length - result.length();

        for (int i = 0; i < number_of_zeros_required; i++) {
            result = "0" + result;
        }
        return result;
    }

    /**
     * Tries to parse pattern representing a range of 256 IPv4 addresses by a dotted-quad wild-carded in the least significant quad.
     * 
     * @param host_pattern a host pattern
     * @param hosts a list into which the hosts are inserted
     */
    private static void tryDottedQuad(final String host_pattern, final List<String> hosts) {

        if (validWildCardedDottedQuadFormat(host_pattern)) {

            final String base = host_pattern.substring(0, host_pattern.length() - 1);

            for (int index = 0; index <= MAX_BYTE_VALUE; index++) {
                hosts.add(base + index);
            }
        }
    }

    /**
     * Tests whether the given pattern contains a valid IPv4 address range wild-carded in the least significant quad.
     * 
     * @param host_pattern a host pattern
     * @return true if the pattern is valid
     */
    private static boolean validWildCardedDottedQuadFormat(final String host_pattern) {

        final String[] quads = host_pattern.split("\\.");

        final int number_of_quads = 4;
        if (quads.length != number_of_quads) { return false; }
        for (int i = 0; i < number_of_quads - 1; i++) {
            if (!isValidQuadValue(quads[i])) { return false; }
        }

        return quads[number_of_quads - 1].equals("*");
    }

    /**
     * Tests whether the given string represents a valid IPv4 quad value (i.e. in the range 0-255).
     * 
     * @param s a possible quad value
     * @return true if the quad is valid
     */
    private static boolean isPositiveInt(final String s, final int max) {

        try {
            final int value = Integer.parseInt(s);
            return value >= 0 && value <= max;
        }
        catch (final NumberFormatException e) {
            return false;
        }
    }

    /**
     * Tests whether the given string represents a valid IPv4 quad value (i.e. in the range 0-255).
     * 
     * @param quad a possible quad value
     * @return true if the quad is valid
     */
    private static boolean isValidQuadValue(final String quad) {

        return isPositiveInt(quad, MAX_BYTE_VALUE);
    }

    /**
     * Extracts the common non-numeric prefix shared by the given strings.
     * 
     * @param s1 a string
     * @param s2 a string
     * @return the common prefix
     */
    private static String getCommonNonNumericPrefix(final String s1, final String s2) {

        int index = 0;
        final int min_length = Math.min(s1.length(), s2.length());

        // Advance while corresponding characters are the same.
        while (index < min_length && s1.charAt(index) == s2.charAt(index)) {
            index++;
        }

        // Discount those that are digits.
        while (index > 0 && isDigit(s1.charAt(index - 1))) {
            index--;
        }

        return s1.substring(0, index);
    }

    private static boolean isDigit(final char c) {

        return c >= '0' && c <= '9';
    }
}
