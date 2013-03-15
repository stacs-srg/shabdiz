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
package uk.ac.standrews.cs.shabdiz.jobs;

import java.util.concurrent.atomic.AtomicReference;

import uk.ac.standrews.cs.shabdiz.DefaultApplicationDescriptor;
import uk.ac.standrews.cs.shabdiz.api.Host;
import uk.ac.standrews.cs.shabdiz.api.Worker;

public class RemoteWorkerDescriptor extends DefaultApplicationDescriptor {

    private final AtomicReference<Worker> application_reference;

    public RemoteWorkerDescriptor(final Host host, final WorkerManager worker_manager) {

        super(host, worker_manager);
        application_reference = new AtomicReference<Worker>();
    }

    public void setApplicationReference(final Worker worker) {

        application_reference.set(worker);
    }

    public Worker getApplicationReference() {

        return application_reference.get();
    }

}
