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

import java.io.InputStream;
import java.io.OutputStream;

public class ProcessWrapper extends Process {

    private final Process unwrapped_process;

    public ProcessWrapper(final Process unwrapped_process) {

        this.unwrapped_process = unwrapped_process;
    }

    @Override
    public OutputStream getOutputStream() {

        return unwrapped_process.getOutputStream();
    }

    @Override
    public InputStream getInputStream() {

        return unwrapped_process.getInputStream();
    }

    @Override
    public InputStream getErrorStream() {

        return unwrapped_process.getErrorStream();
    }

    @Override
    public int waitFor() throws InterruptedException {

        return unwrapped_process.waitFor();
    }

    @Override
    public int exitValue() {

        return unwrapped_process.exitValue();
    }

    @Override
    public void destroy() {

        unwrapped_process.destroy();
    }

    public Process getUnwrappedProcess() {

        return unwrapped_process;
    }

}
