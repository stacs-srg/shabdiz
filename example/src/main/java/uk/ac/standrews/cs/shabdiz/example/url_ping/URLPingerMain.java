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
package uk.ac.standrews.cs.shabdiz.example.url_ping;

import java.net.MalformedURLException;
import java.net.URL;

import uk.ac.standrews.cs.shabdiz.ApplicationDescriptor;
import uk.ac.standrews.cs.shabdiz.ApplicationNetwork;
import uk.ac.standrews.cs.shabdiz.example.util.LogNewAndOldPropertyListener;

/**
 * The entry point to the URL pinger example.
 * Constructs a network for checking the availability of a number of remotely running web services.
 * This is an example of an application network that has minimal access to a remotely running application instance.
 *
 * @author Masih Hajiarabderkani (mh638@st-andrews.ac.uk)
 */
public class URLPingerMain {

    private static final LogNewAndOldPropertyListener PRINT_LISTENER = new LogNewAndOldPropertyListener();
    private final URLPingManager manager;
    private final ApplicationNetwork network;

    URLPingerMain() {

        network = new ApplicationNetwork("URL Ping Network");
        manager = new URLPingManager();
    }

    /**
     * Instantiates a new {@link URLPingerMain} that periodically pings a number of web services.
     *
     * @param args the arguments are ignored
     * @throws MalformedURLException if a sample URL is invalid
     */
    public static void main(final String[] args) throws MalformedURLException {

        final URLPingerMain url_pinger = new URLPingerMain();
        url_pinger.add(new URL("http://www.google.co.uk"));
        url_pinger.add(new URL("http://www.cs.st-andrews.ac.uk"));
        url_pinger.add(new URL("http://www.bbc.co.uk/"));
    }

    boolean add(final URL url) throws MalformedURLException {

        final ApplicationDescriptor descriptor = new ApplicationDescriptor(manager);
        manager.setTarget(descriptor, url);
        descriptor.addStateChangeListener(PRINT_LISTENER);
        return network.add(descriptor);
    }
}
