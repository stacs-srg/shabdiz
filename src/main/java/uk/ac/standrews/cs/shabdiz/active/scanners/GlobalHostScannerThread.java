/***************************************************************************
 *                                                                         *
 * remote_management Library                                               *
 * Copyright (C) 2010 Distributed Systems Architecture Research Group      *
 * University of St Andrews, Scotland                                      *
 * http://www-systems.cs.st-andrews.ac.uk/                                 *
 *                                                                         *
 * This file is part of remote_management, a package providing             *
 * functionality for remotely managing a specified application.            *
 *                                                                         *
 * remote_management is free software: you can redistribute it and/or      *
 * modify it under the terms of the GNU General Public License as          *
 * published by the Free Software Foundation, either version 3 of the      *
 * License, or (at your option) any later version.                         *
 *                                                                         *
 * remote_management is distributed in the hope that it will be useful,    *
 * but WITHOUT ANY WARRANTY; without even the implied warranty of          *
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the           *
 * GNU General Public License for more details.                            *
 *                                                                         *
 * You should have received a copy of the GNU General Public License       *
 * along with remote_management.  If not, see                              *
 * <http://www.gnu.org/licenses/>.                                         *
 *                                                                         *
 ***************************************************************************/
package uk.ac.standrews.cs.shabdiz.active.scanners;

import java.util.concurrent.Callable;
import java.util.concurrent.TimeoutException;

import uk.ac.standrews.cs.nds.util.Diagnostic;
import uk.ac.standrews.cs.nds.util.DiagnosticLevel;
import uk.ac.standrews.cs.nds.util.Duration;
import uk.ac.standrews.cs.nds.util.Timing;
import uk.ac.standrews.cs.shabdiz.active.MadfaceManager;
import uk.ac.standrews.cs.shabdiz.active.interfaces.GlobalHostScanner;

/**
 * Thread that continually monitors the status of the hosts in a given list,
 * the contents of which may vary dynamically.
 *
 * @author Graham Kirby (graham.kirby@st-andrews.ac.uk)
 */
public class GlobalHostScannerThread extends HostScannerThread {

    private final GlobalHostScanner scanner;

    // -------------------------------------------------------------------------------------------------------

    /**
     * Creates a monitor for the manager.
     *
     * @param manager the manager
     * @param scanner an application-specific scanner
     */
    public GlobalHostScannerThread(final MadfaceManager manager, final GlobalHostScanner scanner) {

        super(scanner);
        host_state_list = manager.getHostDescriptors();
        this.scanner = scanner;
    }

    // -------------------------------------------------------------------------------------------------------

    @Override
    public void run() {

        final Callable<Void> check_all_hosts = new Callable<Void>() {

            @Override
            public Void call() throws Exception {

                scanner.check(host_state_list);
                scanner.cycleFinished();
                return null;
            }
        };

        try {
            Timing.repeat(check_all_hosts, Duration.MAX_DURATION, scanner.getMinCycleTime(), false, DiagnosticLevel.FULL);
        }
        catch (final InterruptedException e) {
            Diagnostic.trace(DiagnosticLevel.FULL, "scanner: " + scanner.getName() + " interrupted");
        }
        catch (final TimeoutException e) {
            Diagnostic.trace("scanner: " + scanner.getName() + " timed out unexpectedly");
        }
        catch (final Exception e) {
            throw new IllegalStateException("Unexpected checked exception", e);
        }
    }
}
