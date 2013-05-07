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

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import uk.ac.standrews.cs.shabdiz.ApplicationNetwork;
import uk.ac.standrews.cs.shabdiz.example.util.LogNewAndOldPropertyListener;

/**
 * A network for checking the availability of number of remotely running web services.
 * 
 * @author Masih Hajiarabderkani (mh638@st-andrews.ac.uk)
 */
public class URLPingerNetwork extends ApplicationNetwork {

    private static final long serialVersionUID = 7479140206927524085L;
    private static final LogNewAndOldPropertyListener PRINT_LISTENER = new LogNewAndOldPropertyListener();

    /** Instantiates a new URL pinger network. */
    public URLPingerNetwork() {

        super("URL Pinger Network");
    }

    /**
     * Instantiates a new {@link URLPingerNetwork} that periodically pings a number of web services.
     * 
     * @param args the arguments are ignored.
     * @throws MalformedURLException if a given URL is invalid
     */
    public static void main(final String[] args) throws MalformedURLException {

        final URLPingerNetwork network = new URLPingerNetwork();
        final List<URL> targets = new ArrayList<URL>();
        targets.add(new URL("http://www.google.co.uk"));
        targets.add(new URL("http://www.cs.st-andrews.ac.uk"));
        targets.add(new URL("http://www.bbc.co.uk/"));
        targets.add(new URL("http://quicksilver.hg.cs.st-andrews.ac.uk"));
        configureUrlPingerNetwork(network, targets);
    }

    private static void configureUrlPingerNetwork(final URLPingerNetwork network, final Collection<URL> targets) throws MalformedURLException {

        for (final URL url : targets) {
            final URLPingerDescriptor descriptor = createUrlPingerDescriptor(url);
            network.add(descriptor);
        }
    }

    private static URLPingerDescriptor createUrlPingerDescriptor(final URL url) throws MalformedURLException {

        final URLPingerDescriptor descriptor = new URLPingerDescriptor(url);
        descriptor.addStateChangeListener(PRINT_LISTENER);
        return descriptor;
    }
}
