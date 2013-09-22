package uk.ac.standrews.cs.shabdiz.host.exec;

import uk.ac.standrews.cs.shabdiz.platform.Platform;

/**
 * Provides common functionality for {@link HostProcessBuilder process builders} that start a Java process.
 * This class is not thread-safe.
 *
 * @author Masih Hajiarabderkani (mh638@st-andrews.ac.uk)
 */
public abstract class JavaProcessBuilder implements HostProcessBuilder {

    // TODO make a single Java process builder that accepts file, URL and maven artifacts as dependencies.

    protected static final String SPACE = " ";
    protected static final String JAVA = "java";
    protected static final String BIN = "bin";
    private final StringBuffer jvm_arguments;
    private volatile String main_class;
    private volatile String working_directory;

    protected JavaProcessBuilder() {

        jvm_arguments = new StringBuffer();
    }

    /**
     * Adds the given {@code argument} to this builder's JVM arguments followed by a {@code whitespace}.
     *
     * @param argument the argument to add to the collection of this builder's JVM arguments
     */
    public void addJVMArgument(final String argument) {

        jvm_arguments.append(argument.trim()).append(SPACE);
    }

    /**
     * Gets this builders JVM arguments.
     *
     * @return this builders JVM arguments
     */
    public String getJVMArguments() {

        return jvm_arguments.toString().trim();
    }

    /**
     * Set this builder's JVM arguments to the given {@code replacement_arguments} discarding the previously set JVM arguments.
     *
     * @param arguments the arguments to replace the previous JVM arguments
     */
    public void setJVMArguments(final String... arguments) {

        jvm_arguments.setLength(0);
        for (String argument : arguments) {
            addJVMArgument(argument);
        }
    }

    /**
     * Gets the name of the class that is used as the main class of the process, which are started by this process builder.
     *
     * @return the name of the class that is used as the main class of the process, which are started by this process builder
     */
    public String getMainClass() {

        return main_class;
    }

    /**
     * Sets the fully qualified class  on which the {@code main} method is invoked.
     *
     * @param main_class the fully qualified class on which the {@code main} method is invoked
     */
    public void setMainClass(final Class<?> main_class) {

        this.main_class = main_class.getName();
    }

    /**
     * Sets the fully qualified class name on which the {@code main} method is invoked.
     *
     * @param main_class the fully qualified class name on which the {@code main} method is invoked
     */
    public void setMainClass(final String main_class) {

        this.main_class = main_class.trim();
    }

    /**
     * Gets the working directory of the Java process that are started by this builder.
     * A {@code null} working directory indicates that the working directory is not specified and the default host working directory is used.
     *
     * @return the working directory of the Java process that are started by this builder, or {@code null} if not specified
     */
    public String getWorkingDirectory() {

        return working_directory;
    }

    /**
     * Sets the working directory of the Java process that are started by this builder.
     * if the working directory is {@code null} the default host working directory is used.
     *
     * @param working_directory the new working directory
     */
    public void setWorkingDirectory(final String working_directory) {

        this.working_directory = working_directory;
    }

    protected void appendJavaBinPath(final StringBuilder command, final Platform platform) {

        final String java_home = platform.getJavaHomeDirectory();
        if (java_home != null) {

            final char separator = platform.getSeparator();
            command.append(java_home);
            command.append(BIN);
            command.append(separator);
        }
        command.append(JAVA);
        command.append(SPACE);
    }

    protected void appendJVMArguments(final StringBuilder command) {

        if (jvm_arguments.length() > 0) {
            command.append(getJVMArguments());
            command.append(SPACE);
        }
    }

    protected void appendCommandLineArguments(final StringBuilder command, final Platform platform, final String[] parameters) {

        if (parameters != null && parameters.length > 0) {
            for (String argument : parameters) {
                command.append(platform.quote(argument));
                command.append(SPACE);
            }
        }
    }

    protected void appendMainClass(final StringBuilder command) {

        command.append(getMainClass());
        command.append(SPACE);
    }
}
