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

import java.util.concurrent.atomic.AtomicLong;

import uk.ac.standrews.cs.shabdiz.ApplicationDescriptor;

/**
 * Presents the key to an attribute that may be stored in an {@link ApplicationDescriptor}.
 *
 * @param <Value> the type the value that is represented by this key
 * @author Masih Hajiarabderkani (mh638@st-andrews.ac.uk)
 * @see ApplicationDescriptor#getAttribute(AttributeKey)
 * @see ApplicationDescriptor#setAttribute(AttributeKey, Object)
 */
public final class AttributeKey<Value> implements Comparable<AttributeKey<?>> {

    private static final AtomicLong NEXT_ID = new AtomicLong();
    private final Long id;

    /** Instantiates a new attribute key. */
    public AttributeKey() {

        id = NEXT_ID.getAndIncrement();
    }

    @Override
    public int compareTo(final AttributeKey<?> other) {

        return other.id.compareTo(id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }

    @Override
    public boolean equals(final Object other) {
        if (this == other) { return true; }
        if (!(other instanceof AttributeKey)) { return false; }
        final AttributeKey that = (AttributeKey) other;
        return id.equals(that.id);
    }
}
