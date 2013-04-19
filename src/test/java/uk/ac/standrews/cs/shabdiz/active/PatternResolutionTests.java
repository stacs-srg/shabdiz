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

import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import java.util.List;

import org.junit.Test;

import uk.ac.standrews.cs.shabdiz.util.Patterns;

/**
 * Tests for pattern resolution logic.
 * 
 * @author Graham Kirby (graham.kirby@st-andrews.ac.uk)
 */
public class PatternResolutionTests {

    private static final int MAX_BYTE_VALUE = 255;

    /**
     * Tests small arbitrary range.
     */
    @Test
    public void arbitraryRangeSmall() {

        final int first = 0;
        final int last = 5;

        final List<String> hosts = Patterns.resolveHostPattern(makeRangePattern("compute-0", first, last));

        assertEquals(last - first + 1, hosts.size());
        assertTrue(hosts.contains("compute-0-0"));
        assertTrue(hosts.contains("compute-0-1"));
        assertTrue(hosts.contains("compute-0-2"));
        assertTrue(hosts.contains("compute-0-3"));
        assertTrue(hosts.contains("compute-0-4"));
        assertTrue(hosts.contains("compute-0-5"));
    }

    /**
     * Tests another small arbitrary range.
     */
    @Test
    public void arbitraryRangeSmall2() {

        final List<String> hosts = Patterns.resolveHostPattern("mac1-007 - mac1-011");

        assertThat(hosts.size(), is(equalTo(5)));

        assertThat(hosts.contains("mac1-007"), is(true));
        assertThat(hosts.contains("mac1-008"), is(true));
        assertThat(hosts.contains("mac1-009"), is(true));
        assertThat(hosts.contains("mac1-010"), is(true));
        assertThat(hosts.contains("mac1-011"), is(true));
    }

    /**
     * Tests medium arbitrary range, for example "test-1 - test-100".
     */
    @Test
    public void arbitraryRangeMedium() {

        final int first = 1;
        final int last = 100;

        final List<String> hosts = Patterns.resolveHostPattern(makeRangePattern("test", first, last));

        assertEquals(last - first + 1, hosts.size());
        assertTrue(hosts.contains("test-50"));
    }

    /**
     * Tests large arbitrary range.
     */
    @Test
    public void arbitraryRangeLarge() {

        final int first = 3;
        final int last = 478;

        final List<String> hosts = Patterns.resolveHostPattern(makeRangePattern("test", first, last));

        assertEquals(last - first + 1, hosts.size());
        assertTrue(hosts.contains("test-139"));
    }

    /**
     * Tests large arbitrary range subject to range limit.
     */
    @Test
    public void arbitraryRangeTooLarge() {

        final int first = 1;
        final int last = 10000;
        final int range_limit = 500;

        final List<String> hosts = Patterns.resolveHostPattern(makeRangePattern("test", first, last), range_limit);

        assertEquals(range_limit, hosts.size());
    }

    /**
     * Tests various invalid ranges.
     */
    @Test
    public void arbitraryRangeInvalid() {

        resolvesToSingleHost("abcdef - abc");
        resolvesToSingleHost("abcdef - ");
        resolvesToSingleHost("abcdef - xyz");
        resolvesToSingleHost("abcdef-1 - abcdef5");
        resolvesToSingleHost(" - ");
        resolvesToSingleHost("test-3 - test-");
        resolvesToSingleHost("test- - test-3");
        resolvesToSingleHost("test-478 - test-3");
    }

    /**
     * Tests elaboration of a subnet.
     */
    @Test
    public void dottedQuad1() {

        final List<String> hosts = Patterns.resolveHostPattern("138.251.195.*");

        assertEquals(MAX_BYTE_VALUE + 1, hosts.size());
        assertTrue(hosts.contains("138.251.195.0"));
        assertTrue(hosts.contains("138.251.195.255"));
        assertTrue(hosts.contains("138.251.195.173"));
    }

    /**
     * Tests elaboration of a subnet.
     */
    @Test
    public void dottedQuad2() {

        final List<String> hosts = Patterns.resolveHostPattern("1.0.1.*");

        assertEquals(MAX_BYTE_VALUE + 1, hosts.size());
        assertTrue(hosts.contains("1.0.1.0"));
        assertTrue(hosts.contains("1.0.1.255"));
        assertTrue(hosts.contains("1.0.1.173"));
    }

    /**
     * Tests various invalid subnet ranges.
     */
    @Test
    public void dottedQuadInvalid() {

        resolvesToSingleHost("138.251.195.206");
        resolvesToSingleHost("138.251.256.*");
        resolvesToSingleHost("138.251.*.255");
        resolvesToSingleHost("138.251.256.**");
        resolvesToSingleHost("138.251.-1.*");
        resolvesToSingleHost("138.251.*.*");
        resolvesToSingleHost("138.251.195.206.*");
        resolvesToSingleHost("abc.def.ghi.*");
    }

    // -------------------------------------------------------------------------------------------------------

    private void resolvesToSingleHost(final String pattern) {

        final List<String> hosts = Patterns.resolveHostPattern(pattern);

        assertEquals(1, hosts.size());
        assertTrue(hosts.contains(pattern));
    }

    private String makeRangePattern(final String root, final int first, final int last) {

        return root + "-" + first + " - " + root + "-" + last;
    }
}
