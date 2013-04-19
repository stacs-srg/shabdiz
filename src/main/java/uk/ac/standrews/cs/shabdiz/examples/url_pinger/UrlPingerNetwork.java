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
package uk.ac.standrews.cs.shabdiz.examples.url_pinger;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import uk.ac.standrews.cs.shabdiz.ApplicationNetwork;
import uk.ac.standrews.cs.shabdiz.examples.PrintNewAndOldPropertyListener;

public class UrlPingerNetwork extends ApplicationNetwork {

    private static final PrintNewAndOldPropertyListener PRINT_LISTENER = new PrintNewAndOldPropertyListener();

    public UrlPingerNetwork() {

        super("URL Pinger Network");
    }

    public static void main(final String[] args) throws MalformedURLException {

        final UrlPingerNetwork network = new UrlPingerNetwork();
        final List<URL> targets = new ArrayList<URL>();
        targets.add(new URL("http://www.google.co.uk"));
        targets.add(new URL("http://www.cs.st-andrews.ac.uk"));
        targets.add(new URL("http://www.bbc.co.uk/news/"));
        targets.add(new URL("http://quicksilver.hg.cs.st-andrews.ac.uk"));
        configureUrlPingerNetwork(network, targets);
    }

    private static void configureUrlPingerNetwork(final UrlPingerNetwork network, final Collection<URL> targets) throws MalformedURLException {

        for (final URL url : targets) {
            final UrlPingerDescriptor descriptor = createUrlPingerDescriptor(url);
            network.add(descriptor);
        }
    }

    private static UrlPingerDescriptor createUrlPingerDescriptor(final URL url) throws MalformedURLException {

        final UrlPingerDescriptor descriptor = new UrlPingerDescriptor(url);
        descriptor.addStateChangeListener(PRINT_LISTENER);
        return descriptor;
    }
}
