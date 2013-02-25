/***************************************************************************
 *                                                                         *
 * nds Library                                                             *
 * Copyright (C) 2005-2011 Distributed Systems Architecture Research Group *
 * University of St Andrews, Scotland                                      *
 * http://www-systems.cs.st-andrews.ac.uk/                                 *
 *                                                                         *
 * This file is part of nds, a package of utility classes.                 *
 *                                                                         *
 * nds is free software: you can redistribute it and/or modify             *
 * it under the terms of the GNU General Public License as published by    *
 * the Free Software Foundation, either version 3 of the License, or       *
 * (at your option) any later version.                                     *
 *                                                                         *
 * nds is distributed in the hope that it will be useful,                  *
 * but WITHOUT ANY WARRANTY; without even the implied warranty of          *
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the           *
 * GNU General Public License for more details.                            *
 *                                                                         *
 * You should have received a copy of the GNU General Public License       *
 * along with nds.  If not, see <http://www.gnu.org/licenses/>.            *
 *                                                                         *
 ***************************************************************************/
package uk.ac.standrews.cs.shabdiz.active;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.concurrent.TimeUnit;

import uk.ac.standrews.cs.nds.rpc.app.nostream.TestProxy;
import uk.ac.standrews.cs.nds.rpc.app.nostream.TestServer;
import uk.ac.standrews.cs.nds.rpc.interfaces.IPingable;
import uk.ac.standrews.cs.nds.util.Duration;
import uk.ac.standrews.cs.shabdiz.active.AbstractApplicationManager;
import uk.ac.standrews.cs.shabdiz.active.HostDescriptor;
import uk.ac.standrews.cs.shabdiz.active.interfaces.AttributesCallback;
import uk.ac.standrews.cs.shabdiz.active.interfaces.GlobalHostScanner;
import uk.ac.standrews.cs.shabdiz.active.interfaces.SingleHostScanner;
import uk.ac.standrews.cs.shabdiz.active.scanners.Scanner;
import uk.ac.standrews.cs.shabdiz.impl.RemoteJavaProcessBuilder;

/**
 * Manager for test application.
 * 
 * @author Graham Kirby (graham.kirby@st-andrews.ac.uk)
 */
public class TestAppManager extends AbstractApplicationManager {

    private static final Duration TIMEOUT = new Duration(10, TimeUnit.SECONDS);
    private static final int THREADS = 1;

    private static class DummySingleHostScanner extends Scanner implements SingleHostScanner {

        public DummySingleHostScanner() {

            super(null, new Duration(), THREADS, TIMEOUT, "dummy single", true);
        }

        @Override
        public String getName() {

            return "dummy single";
        }

        @Override
        public String getAttributeName() {

            return "test attribute";
        }

        @Override
        public String getToggleLabel() {

            return null;
        }

        @Override
        public void check(final HostDescriptor host_descriptor, final Set<AttributesCallback> attribute_callbacks) throws Exception {

        }
    }

    private static class TestSingleHostScanner extends Scanner implements SingleHostScanner {

        private int call_count = 0;

        public TestSingleHostScanner() {

            super(null, new Duration(), THREADS, TIMEOUT, "single", true);
        }

        // -------------------------------------------------------------------------------------------------------

        @Override
        public String getName() {

            return "single";
        }

        @Override
        public String getAttributeName() {

            return "test attribute";
        }

        @Override
        public void check(final HostDescriptor host_descriptor, final Set<AttributesCallback> attribute_callbacks) {

            // Only change attribute after a number of calls.
            call_count++;

            if (call_count > 2) {

                final Map<String, String> attribute_map = host_descriptor.getAttributes();

                attribute_map.put("test attribute", "test value");

                for (final AttributesCallback callback : attribute_callbacks) {
                    callback.attributesChange(host_descriptor);
                }
            }
        }

        @Override
        public String getToggleLabel() {

            return "single host scanner";
        }

        @Override
        public String toString() {

            return "TestScanner";
        }
    }

    private static class DummyGlobalHostScanner extends Scanner implements GlobalHostScanner {

        public DummyGlobalHostScanner() {

            super(null, new Duration(), THREADS, TIMEOUT, "dummy global", true);
        }

        @Override
        public String getName() {

            return "dummy global";
        }

        @Override
        public String getToggleLabel() {

            return null;
        }

        @Override
        public void check(final SortedSet<HostDescriptor> host_descriptors) {

        }
    }

    private static class TestGlobalHostScanner extends Scanner implements GlobalHostScanner {

        public TestGlobalHostScanner() {

            super(null, new Duration(), THREADS, TIMEOUT, "global", true);
        }

        @Override
        public String getName() {

            return "global";
        }

        @Override
        public String getToggleLabel() {

            return "global host scanner";
        }

        @Override
        public void check(final SortedSet<HostDescriptor> host_descriptors) {

        }
    }

    /**
     * The application name.
     */
    public static final String APPLICATION_NAME = "test-application";

    /**
     * The application-specific attribute name.
     */
    public static final String ATTRIBUTE_NAME = "test-attribute";

    public TestAppManager() {

        this(false);
    }

    public TestAppManager(final boolean use_dummy_scanners) {

        getSingleScanners().add(use_dummy_scanners ? new DummySingleHostScanner() : new TestSingleHostScanner());
        getGlobalScanners().add(use_dummy_scanners ? new DummyGlobalHostScanner() : new TestGlobalHostScanner());
    }

    @Override
    public String getApplicationName() {

        return APPLICATION_NAME;
    }

    @Override
    public void deployApplication(final HostDescriptor host_descriptor) throws Exception {

        synchronized (host_descriptor) {

            if (host_descriptor.getNumberOfProcesses() == 0) {
                final List<String> arg_list = new ArrayList<String>();

                arg_list.add("-p" + host_descriptor.getPort());

                final RemoteJavaProcessBuilder java_process_builder = new RemoteJavaProcessBuilder(TestServer.class);
                java_process_builder.addCommandLineArguments(arg_list);
                java_process_builder.addCurrentJVMClasspath();

                final Process java_process = java_process_builder.start(host_descriptor.getManagedHost());
                host_descriptor.process(java_process);
            }
        }
    }

    @Override
    public void establishApplicationReference(final HostDescriptor host_descriptor) throws Exception {

        final IPingable remote_reference = new TestProxy(host_descriptor.getInetSocketAddress());

        // Check that the remote application can be contacted.
        remote_reference.ping();

        host_descriptor.applicationReference(remote_reference);
    }

    @Override
    protected String guessFragmentOfApplicationProcessName(final HostDescriptor host_descriptor) {

        return null;
    }
}
