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
package uk.ac.standrews.cs.shabdiz.host;

import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.util.Collection;

import uk.ac.standrews.cs.shabdiz.api.Host;
import uk.ac.standrews.cs.shabdiz.api.Platform;

public abstract class HostWrapper implements Host {

    private final Host unwrapped_host;

    protected HostWrapper(final Host host) {

        this.unwrapped_host = host;
    }

    protected Host getUnwrappedHost() {

        return unwrapped_host;
    }

    @Override
    public void upload(final Collection<File> sources, final String destination) throws IOException {

        unwrapped_host.upload(sources, destination);
    }

    @Override
    public void upload(final File source, final String destination) throws IOException {

        unwrapped_host.upload(source, destination);
    }

    @Override
    public void close() throws IOException {

        unwrapped_host.close();
    }

    @Override
    public boolean isLocal() {

        return unwrapped_host.isLocal();
    }

    @Override
    public Platform getPlatform() throws IOException {

        return unwrapped_host.getPlatform();
    }

    @Override
    public InetAddress getAddress() {

        return unwrapped_host.getAddress();
    }

    @Override
    public Process execute(final String... command) throws IOException {

        return unwrapped_host.execute(command);
    }

    @Override
    public void download(final String source, final File destination) throws IOException {

        unwrapped_host.download(source, destination);
    }
}
