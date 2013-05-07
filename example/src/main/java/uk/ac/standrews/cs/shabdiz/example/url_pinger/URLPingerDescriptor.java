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
package uk.ac.standrews.cs.shabdiz.example.url_pinger;

import java.net.URL;

import uk.ac.standrews.cs.shabdiz.ApplicationDescriptor;

class URLPingerDescriptor extends ApplicationDescriptor {

    private static final URLPingerManager URL_PINGER_MANAGER = new URLPingerManager();

    private final URL target;

    public URLPingerDescriptor(final URL target) {

        super(null, URL_PINGER_MANAGER);
        validateTarget(target);
        this.target = target;
    }

    private void validateTarget(final URL target) {

        if (!target.getProtocol().toLowerCase().equals("http")) { throw new IllegalArgumentException("HTTP urls only"); }
    }

    public URL getTarget() {

        return target;
    }

    @Override
    public String toString() {

        return target.toString();
    }
}
