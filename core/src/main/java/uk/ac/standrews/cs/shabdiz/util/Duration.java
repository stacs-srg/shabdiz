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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Representation of a duration in time.
 *
 * @author Graham Kirby (graham.kirby@st-andrews.ac.uk)
 */
public class Duration implements Comparable<Duration>, Serializable {

    /** The maximum duration that can be represented in {@link TimeUnit#DAYS}. */
    public static final Duration MAX_DURATION = new Duration(Long.MAX_VALUE, TimeUnit.DAYS);
    /** The zero duration in {@link TimeUnit#NANOSECONDS}. */
    public static final Duration ZERO = new Duration(0, TimeUnit.NANOSECONDS);
    /** An XML tag for the duration unit. */
    public static final String UNIT_TAG = "unit";
    /** An XML tag for the duration length. */
    public static final String LENGTH_TAG = "length";
    private static final long serialVersionUID = 2022340212281366237L;
    private static final Logger LOGGER = LoggerFactory.getLogger(Duration.class);
    private static final Map<TimeUnit, String> UNIT_ABBREVIATIONS;

    static {
        UNIT_ABBREVIATIONS = new HashMap<TimeUnit, String>();

        UNIT_ABBREVIATIONS.put(TimeUnit.NANOSECONDS, "ns");
        UNIT_ABBREVIATIONS.put(TimeUnit.MICROSECONDS, "micros");
        UNIT_ABBREVIATIONS.put(TimeUnit.MILLISECONDS, "ms");
        UNIT_ABBREVIATIONS.put(TimeUnit.SECONDS, "s");
        UNIT_ABBREVIATIONS.put(TimeUnit.MINUTES, "min");
        UNIT_ABBREVIATIONS.put(TimeUnit.HOURS, "hrs");
        UNIT_ABBREVIATIONS.put(TimeUnit.DAYS, "days");
    }

    // -------------------------------------------------------------------------------------------------------
    private final long length;
    private final TimeUnit unit;

    // -------------------------------------------------------------------------------------------------------

    /** Initializes a new duration of zero length. */
    public Duration() {

        this(0, TimeUnit.MILLISECONDS);
    }

    /**
     * Initializes a duration.
     *
     * @param length the length of the duration
     * @param unit the time unit of the specified length
     */
    public Duration(final long length, final TimeUnit unit) {

        this.length = length;
        this.unit = unit;
    }

    // -------------------------------------------------------------------------------------------------------

    /**
     * Returns a new duration representing the difference between a specified time instant in the past, and the current time in nanoseconds.
     *
     * @param start the past instant represented in nanoseconds as specified for {@link java.lang.System#nanoTime()}
     * @return a new duration representing the difference between the specified time instant and the current time
     */
    public static Duration elapsedNano(final long start) {

        return elapsed(start, System.nanoTime(), TimeUnit.NANOSECONDS);
    }

    /**
     * Returns a new duration representing the difference between two specified time instants represented in the given <code>unit</code>.
     *
     * @param start the first instant
     * @param end the second instant
     * @param unit the unit
     * @return a new duration representing the difference between the specified time instants
     */
    public static Duration elapsed(final long start, final long end, final TimeUnit unit) {

        return new Duration(end - start, unit);
    }

    /**
     * Returns a new duration representing the difference between time zero, as specified for {@link java.lang.System#currentTimeMillis()}, and the current time.
     *
     * @return a new duration representing the difference between time zero and the current time
     */
    public static Duration elapsed() {

        return elapsed(0);
    }

    /**
     * Returns a new duration representing the difference between a specified time instant in the past, and the current time.
     *
     * @param start the past instant represented in milliseconds as specified for {@link java.lang.System#currentTimeMillis()}
     * @return a new duration representing the difference between the specified time instant and the current time
     */
    public static Duration elapsed(final long start) {

        return elapsed(start, System.currentTimeMillis());
    }

    /**
     * Returns a new duration representing the difference between two specified time instants represented in milliseconds.
     *
     * @param start the first instant represented in milliseconds
     * @param end the second instant represented in milliseconds
     * @return a new duration representing the difference between the specified time instants
     */
    public static Duration elapsed(final long start, final long end) {

        return elapsed(start, end, TimeUnit.MILLISECONDS);
    }

    /**
     * Returns a new duration representing the difference between two specified durations. There is no requirement for the durations' time units to be the same.
     *
     * @param start the first duration
     * @param end the second duration
     * @return a new duration representing the difference between the specified durations
     */
    public static Duration elapsed(final Duration start, final Duration end) {

        return elapsed(start.getLength(TimeUnit.MILLISECONDS), end.getLength(TimeUnit.MILLISECONDS));
    }

    /**
     * Returns the length of the duration, expressed in the given time unit.
     *
     * @param other_time_unit the other time unit
     * @return the length of the duration expressed in the given time unit
     */
    public long getLength(final TimeUnit other_time_unit) {

        return other_time_unit.convert(length, unit);
    }

    /**
     * Returns a new duration representing the difference between a specified duration, and the duration from time zero, as specified for {@link java.lang.System#currentTimeMillis()}, and the current time.
     *
     * @param start the duration
     * @return a new duration representing the difference between the specified duration and the current time
     */
    public static Duration elapsed(final Duration start) {

        return elapsed(start.getLength(TimeUnit.MILLISECONDS));
    }

    // -------------------------------------------------------------------------------------------------------

    /**
     * Returns a new duration representing the longer of the specified durations.
     *
     * @param first the first duration
     * @param second the second duration
     * @return a new duration representing the longer of the specified durations
     */
    public static Duration max(final Duration first, final Duration second) {

        return new Duration(Math.max(first.length, second.getLength(first.unit)), first.unit);
    }

    /**
     * Returns a new duration obtained by deserializing the given string. The string may be in the format returned by {@link #toString()}, for example "3 min", "23 ms",
     * or in a similar format using full unit names, for example "3 MINUTES", "23 MILLISECONDS".
     *
     * @param s a string representing a duration
     * @return a new duration obtained by deserializing the given string
     * @throws IllegalArgumentException if the string is not in a recognized format
     */
    public static Duration valueOf(final String s) {

        final int index_of_space = s.indexOf(" ");
        final long length = Long.valueOf(s.substring(0, index_of_space));
        final TimeUnit unit = expand(s.substring(index_of_space + 1));

        return new Duration(length, unit);
    }

    private static TimeUnit expand(final String unit_as_string) {

        for (final TimeUnit unit : UNIT_ABBREVIATIONS.keySet()) {

            // Try abbreviated form.
            if (UNIT_ABBREVIATIONS.get(unit).equals(unit_as_string)) {
                return unit;
            }

            // Try expanded form.
            try {
                return TimeUnit.valueOf(unit_as_string);
            } catch (final IllegalArgumentException e) {
                // Ignore and try next unit.
                LOGGER.trace("error occurred while trying expanded form", e);
            }
        }
        throw new IllegalArgumentException();
    }

    /**
     * Returns the length of the duration.
     *
     * @return the length of the duration
     */
    public long getLength() {

        return length;
    }

    /**
     * Returns the time unit of the duration.
     *
     * @return the time unit of the duration
     */
    public TimeUnit getTimeUnit() {

        return unit;
    }

    /**
     * Causes the current thread to sleep for the time represented by this duration.
     *
     * @throws InterruptedException if interrupted while sleeping
     */
    public void sleep() throws InterruptedException {

        unit.sleep(length);
    }

    /**
     * Returns a new duration representing this duration divided by the given divisor.
     *
     * @param divisor the divisor
     * @return a new duration representing this duration divided by the given divisor
     */
    public Duration dividedBy(final long divisor) {

        return new Duration(length / divisor, unit);
    }

    /**
     * Returns a new duration representing this duration multiplied by the given multiplier.
     *
     * @param multiplier the multiplier
     * @return a new duration representing this duration multiplied by the given multiplier
     */
    public Duration times(final long multiplier) {

        return new Duration(length * multiplier, unit);
    }

    /**
     * Returns a new duration representing the mod of this duration and the given duration.
     *
     * @param other the other duration
     * @return a new duration representing the mod of this duration and the given duration
     */
    public Duration mod(final Duration other) {

        return new Duration(length % other.getLength(unit), unit);
    }

    /**
     * Returns a new duration representing the sum of this duration and the given duration.
     *
     * @param other the other duration
     * @return a new duration representing the sum of this duration and the given duration
     */
    public Duration add(final Duration other) {

        return new Duration(length + other.getLength(unit), unit);
    }

    /**
     * Returns a new duration representing the difference between this duration and the given duration.
     *
     * @param other the other duration
     * @return a new duration representing the result of subtracting the other duration from this duration
     */
    public Duration subtract(final Duration other) {

        return new Duration(length - other.getLength(unit), unit);
    }

    /**
     * Returns a new duration representing the difference between this duration and the given duration in the given time unit.
     *
     * @param other the other duration
     * @param unit the new time unit
     * @return a new duration representing the result of subtracting the other duration from this duration
     */
    public Duration subtract(final Duration other, final TimeUnit unit) {

        return new Duration(getLength(unit) - other.getLength(unit), unit);
    }

    // -------------------------------------------------------------------------------------------------------

    /**
     * Returns a duration representing this duration expressed in the given time unit.
     *
     * @param other_time_unit the other time unit
     * @return a duration representing this duration expressed in the given time unit
     */
    public Duration convertTo(final TimeUnit other_time_unit) {

        if (unit.equals(other_time_unit)) {
            return this;
        }
        return new Duration(getLength(other_time_unit), other_time_unit);
    }

    // -------------------------------------------------------------------------------------------------------

    /**
     * Checks whether this duration exceeds the {@code other} duration.
     *
     * @param other the other duration
     * @return true, if and only if {@code other} duration is shorter than or equal to this duration
     */
    public boolean exceeds(final Duration other) {

        return compareTo(other) > 0;
    }

    @Override
    public int compareTo(final Duration other) {

        final long other_in_same_units = unit.convert(other.length, other.unit);
        return length > other_in_same_units ? 1 : length == other_in_same_units ? 0 : -1;
    }

    @Override
    public int hashCode() {

        return (int) length * unit.ordinal();
    }

    @Override
    public boolean equals(final Object other) {

        return other instanceof Duration && compareTo((Duration) other) == 0;
    }

    @Override
    public String toString() {

        return toString(length, unit);
    }

    private static String toString(final long length, final TimeUnit unit) {

        return length + " " + abbreviate(unit);
    }

    private static String abbreviate(final TimeUnit time_unit) {

        return UNIT_ABBREVIATIONS.get(time_unit);
    }

    /**
     * Returns a string representation of this duration with the largest time unit this duration can be converted to where <code>length > 0</code>.
     * Returns {@link #toString()} if no such time unit exists excluding this duration's time unit, or <code>length = 0</code>.
     *
     * @return a string representation of this duration with largest time unit this duration can be converted to where <code>length > 0</code>
     */
    public String toStringAsLargestTimeUnit() {

        return toStringAsLargestTimeUnit(length, unit);
    }

    /**
     * Returns a string representation of this duration with the largest time unit this duration can be converted to where <code>length > 0</code>.
     * Returns {@link #toString()} if no such time unit exists excluding this duration's time unit, or <code>length = 0</code>.
     *
     * @param length the length
     * @param unit the unit
     * @return a string representation of this duration with largest time unit this duration can be converted to where <code>length > 0</code>
     */
    public static String toStringAsLargestTimeUnit(final long length, final TimeUnit unit) {

        if (length > 0 && !unit.equals(TimeUnit.DAYS)) {
            final TimeUnit[] units = TimeUnit.values();
            int i = units.length - 1;
            TimeUnit new_unit = units[i];
            // Find the largest TimeUnit that can present this duration in a non-zero length. The unit is always bigger than or equal to the current unit.
            while (!unit.equals(new_unit)) {
                final long length_in_new_unit = new_unit.convert(length, unit);
                if (length_in_new_unit != 0) {
                    return toString(length_in_new_unit, new_unit);
                }
                new_unit = units[i--];
            }
        }
        return toString(length, unit);
    }
}
