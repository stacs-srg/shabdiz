package uk.ac.standrews.cs.nds.madface;

import java.io.File;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import uk.ac.standrews.cs.nds.madface.interfaces.IStreamProcessor;
import uk.ac.standrews.cs.nds.util.Duration;
import uk.ac.standrews.cs.nds.util.TimeoutExecutor;

/**
 * Holds details of a process to be executed. The method {@link #shutdown()} should be called before disposing of an instance, to avoid thread leakage.
 *
 * @author Graham Kirby (graham.kirby@st-andrews.ac.uk)
 */
public class ProcessDescriptor {

    private static final Duration DEFAULT_PROCESS_TIMEOUT = new Duration(10, TimeUnit.SECONDS);
    private static final List<String> EMPTY_ARG_LIST = new ArrayList<String>();

    private volatile OutputStream output_stream;
    private volatile OutputStream error_stream;
    private volatile IStreamProcessor output_processor;
    private volatile IStreamProcessor error_processor;
    private volatile TimeoutExecutor timeout_executor;
    private volatile String command;
    private volatile String label;
    private volatile List<String> args;
    private volatile List<File> delete_on_exit;

    public OutputStream getOutputStream() {

        return output_stream == null ? System.out : output_stream;
    }

    public IStreamProcessor getOutputProcessor() {

        return output_processor;
    }

    public IStreamProcessor getErrorProcessor() {

        return error_processor;
    }

    public OutputStream getErrorStream() {

        return error_stream == null ? System.err : error_stream;
    }

    public synchronized TimeoutExecutor getExecutor() {

        if (timeout_executor == null) {
            timeout_executor = makeDefaultTimeoutExecutor();
        }
        return timeout_executor;
    }

    public void shutdown() {

        if (timeout_executor != null) {
            timeout_executor.shutdown();
        }
    }

    public ProcessDescriptor executor(final TimeoutExecutor timeout_executor) {

        this.timeout_executor = timeout_executor;
        return this;
    }

    public ProcessDescriptor command(final String command) {

        this.command = command;
        return this;
    }

    public ProcessDescriptor label(final String label) {

        this.label = label;
        return this;
    }

    public ProcessDescriptor outputStream(final OutputStream output_stream) {

        this.output_stream = output_stream;
        return this;
    }

    public ProcessDescriptor errorStream(final OutputStream error_stream) {

        this.error_stream = error_stream;
        return this;
    }

    public String getCommand() {

        return command;
    }

    public String getLabel() {

        return label == null ? command : label;
    }

    public ProcessDescriptor outputProcessor(final IStreamProcessor output_processor) {

        this.output_processor = output_processor;
        return this;
    }

    public ProcessDescriptor errorProcessor(final IStreamProcessor error_processor) {

        this.error_processor = error_processor;
        return this;
    }

    public List<String> getArgs() {

        return args == null ? EMPTY_ARG_LIST : args;
    }

    public synchronized List<File> getFilesDeletedOnExit() {

        if (delete_on_exit == null) {
            delete_on_exit = new ArrayList<File>();
        }
        return delete_on_exit;
    }

    public ProcessDescriptor args(final List<String> args) {

        this.args = args;
        return this;
    }

    public ProcessDescriptor args(final String arg) {

        final List<String> new_args = new ArrayList<String>();
        new_args.add(arg);
        return args(new_args);
    }

    public synchronized ProcessDescriptor deleteOnExit(final File file) {

        if (delete_on_exit == null) {
            delete_on_exit = new ArrayList<File>();
        }
        delete_on_exit.add(file);
        return this;
    }

    private TimeoutExecutor makeDefaultTimeoutExecutor() {

        return TimeoutExecutor.makeTimeoutExecutor(1, DEFAULT_PROCESS_TIMEOUT, true, true, "ProcessDescriptor");
    }
}
