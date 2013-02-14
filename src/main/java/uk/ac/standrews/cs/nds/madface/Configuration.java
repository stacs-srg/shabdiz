package uk.ac.standrews.cs.nds.madface;

import java.util.ArrayList;

import org.json.JSONException;

import uk.ac.standrews.cs.nds.rpc.nostream.json.JSONArray;
import uk.ac.standrews.cs.nds.rpc.nostream.json.JSONValue;

public final class Configuration implements Cloneable {

    private final ArrayList<ParameterValue> parameter_values;

    public Configuration() {

        parameter_values = new ArrayList<ParameterValue>();
    }

    public Configuration(final ArrayList<ParameterValue> parameter_values) {

        this.parameter_values = parameter_values;
    }

    public Configuration(final JSONArray serialized_configuration) throws JSONException {

        this();
        for (int i = 0; i < serialized_configuration.length(); i++) {
            parameter_values.add(new ParameterValue(serialized_configuration.getJSONObject(i)));
        }
    }

    public void appendToBuilder(final StringBuilder builder) {

        for (final ParameterValue parameter_value : parameter_values) {
            final int length = builder.length();
            if (length > 0 && builder.charAt(length - 1) != '\n') {
                builder.append(", ");
            }
            builder.append(parameter_value);
        }
    }

    @Override
    public String toString() {

        final StringBuilder builder = new StringBuilder();
        appendToBuilder(builder);

        return builder.toString();
    }

    @Override
    @SuppressWarnings("unchecked")
    public Object clone() {

        return new Configuration((ArrayList<ParameterValue>) parameter_values.clone());
    }

    @Override
    public boolean equals(final Object obj) {

        if (!(obj instanceof Configuration)) { return false; }
        final Configuration other = (Configuration) obj;

        if (parameter_values.size() != other.parameter_values.size()) { return false; }

        for (final ParameterValue parameter : parameter_values) {
            if (!other.parameter_values.contains(parameter)) { return false; }
        }
        return true;
    }

    @Override
    public int hashCode() {

        int hash = 1;
        for (final ParameterValue parameter : parameter_values) {
            hash = hash * 31 + parameter.hashCode();
        }
        return hash;
    }

    public Configuration addParameter(final ParameterValue parameter_value) {

        parameter_values.add(parameter_value);
        return this;
    }

    public ArrayList<ParameterValue> getValues() {

        return parameter_values;
    }

    /**
     * Serializes the configuration.
     *
     * @return the serialized configuration
     */
    public JSONValue serialize() {

        final JSONArray array = new JSONArray();

        for (final ParameterValue parameter_value : parameter_values) {
            array.put(parameter_value.serialize());
        }

        return array;
    }
}
