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
