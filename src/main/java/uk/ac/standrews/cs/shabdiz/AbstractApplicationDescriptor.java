/*
 * shabdiz Library
 * Copyright (C) 2013 Networks and Distributed Systems Research Group
 * <http://www.cs.st-andrews.ac.uk/research/nds>
 *
 * shabdiz is a free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * shabdiz is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with shabdiz.  If not, see <http://www.gnu.org/licenses/>.
 *
 * For more information, see <https://builds.cs.st-andrews.ac.uk/job/shabdiz/>.
 */
package uk.ac.standrews.cs.shabdiz;

import java.io.IOException;
import java.util.Comparator;
import java.util.Iterator;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.atomic.AtomicLong;

import uk.ac.standrews.cs.shabdiz.api.DeployHook;
import uk.ac.standrews.cs.shabdiz.api.Host;
import uk.ac.standrews.cs.shabdiz.host.HostWrapper;

public abstract class AbstractApplicationDescriptor extends AbstractProbeHook implements DeployHook, Comparable<AbstractApplicationDescriptor> {

    private static final AtomicLong NEXT_ID = new AtomicLong();
    private final Long id; // used to resolve ties when comparing
    private final Host host;
    final ConcurrentSkipListSet<Process> processes;

    public AbstractApplicationDescriptor(final Host host) {

        id = generateId();
        processes = new ConcurrentSkipListSet<Process>(new ProcessHashcodeComparator());
        this.host = new HostWrapper(host) {

            @Override
            public Process execute(final String... command) throws IOException {

                final Process process = getUnwrappedHost().execute(command);
                processes.add(process);
                return process;
            }
        };
    }

    @Override
    public Host getHost() {

        return host;
    }

    public ConcurrentSkipListSet<Process> getProcesses() {

        return processes;
    }

    protected boolean addProcess(final Process process) {

        return processes.add(process);
    }

    @Override
    public void kill() {

        final Iterator<Process> process_iterator = processes.iterator();
        while (process_iterator.hasNext()) {
            final Process process = process_iterator.next();
            process.destroy();
        }
    }

    @Override
    public int compareTo(final AbstractApplicationDescriptor other) {

        final int host_name_comparison = host.getAddress().getHostName().compareTo(other.host.getAddress().getHostName());
        return host_name_comparison != 0 ? host_name_comparison : id.compareTo(other.id);
    }

    private static Long generateId() {

        return NEXT_ID.getAndIncrement();
    }

    private static final class ProcessHashcodeComparator implements Comparator<Process> {

        @Override
        public int compare(final Process o1, final Process o2) {

            return Integer.valueOf(o1.hashCode()).compareTo(o2.hashCode());
        }
    }
}
