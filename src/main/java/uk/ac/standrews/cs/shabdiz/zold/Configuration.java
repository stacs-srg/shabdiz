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
package uk.ac.standrews.cs.shabdiz.zold;

import java.util.ArrayList;
import java.util.concurrent.TimeUnit;

import org.json.JSONException;

import uk.ac.standrews.cs.nds.rpc.nostream.json.JSONArray;
import uk.ac.standrews.cs.nds.rpc.nostream.json.JSONValue;
import uk.ac.standrews.cs.nds.util.Duration;
import uk.ac.standrews.cs.shabdiz.zold.scanners.StatusScanner;

public final class Configuration implements Cloneable {

    private final ArrayList<ParameterValue> parameter_values;

    public static final String STATE_WAIT_DELAY_KEY = "state_wait_delay";
    public static final String SCANNER_MIN_CYCLE_TIME_KEY = "scanner_min_cycle_time";
    public static final String SSH_CHECK_THREAD_TIMEOUT_KEY = "ssh_check_thread_timeout";
    /** The default minimum scanner cycle time. */
    public static final Duration DEFAULT_SCANNER_MIN_CYCLE_TIME = new Duration(2, TimeUnit.SECONDS);

    /** The default delay between host state checks. */
    public static final Duration DEFAULT_STATE_WAIT_DELAY = new Duration(1, TimeUnit.SECONDS);
    public static final Configuration DEFAULT_CONFIGURATION;

    static {
        DEFAULT_CONFIGURATION = new Configuration();
        DEFAULT_CONFIGURATION.addParameter(SSH_CHECK_THREAD_TIMEOUT_KEY, (int) StatusScanner.DEFAULT_SSH_CHECK_TIMEOUT.getLength(TimeUnit.SECONDS));
        DEFAULT_CONFIGURATION.addParameter(SCANNER_MIN_CYCLE_TIME_KEY, (int) DEFAULT_SCANNER_MIN_CYCLE_TIME.getLength(TimeUnit.SECONDS));
        DEFAULT_CONFIGURATION.addParameter(STATE_WAIT_DELAY_KEY, (int) DEFAULT_STATE_WAIT_DELAY.getLength(TimeUnit.SECONDS));
    }

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

    public Configuration addParameter(final String name, final int value) {

        return addParameter(new ParameterValue(name, value));
    }

    public Configuration addParameter(final ParameterValue parameter_value) {

        parameter_values.add(parameter_value);
        return this;
    }

    public ArrayList<ParameterValue> getValues() {

        return parameter_values;
    }

    public JSONValue serialize() {

        final JSONArray array = new JSONArray();

        for (final ParameterValue parameter_value : parameter_values) {
            array.put(parameter_value.serialize());
        }

        return array;
    }
}
