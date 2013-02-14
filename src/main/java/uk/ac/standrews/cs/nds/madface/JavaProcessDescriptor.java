package uk.ac.standrews.cs.nds.madface;

import java.util.List;

public class JavaProcessDescriptor extends ProcessDescriptor {

    private volatile List<String> jvm_params;
    private volatile Class<?> class_to_be_invoked;

    public List<String> getJVMParams() {

        return jvm_params;
    }

    public Class<?> getClassToBeInvoked() {

        return class_to_be_invoked;
    }

    public JavaProcessDescriptor classToBeInvoked(final Class<?> class_to_be_invoked) {

        this.class_to_be_invoked = class_to_be_invoked;
        return this;
    }

    public JavaProcessDescriptor jvmParams(final List<String> jvm_params) {

        this.jvm_params = jvm_params;
        return this;
    }
}
