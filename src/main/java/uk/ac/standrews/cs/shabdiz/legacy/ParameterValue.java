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
package uk.ac.standrews.cs.shabdiz.legacy;

import org.json.JSONException;

import uk.ac.standrews.cs.nds.rpc.nostream.json.JSONObject;
import uk.ac.standrews.cs.nds.rpc.nostream.json.JSONValue;

public class ParameterValue {

    String name;
    private final int value;

    public ParameterValue(final String name, final int value) {

        this.name = name;
        this.value = value;
    }

    public ParameterValue(final JSONObject object) throws JSONException {

        this(object.getString("name"), object.getInt("value"));
    }

    public String getParameterName() {

        return name;
    }

    public int getValue() {

        return value;
    }

    @Override
    public boolean equals(final Object obj) {

        if (!(obj instanceof ParameterValue)) { return false; }
        final ParameterValue other = (ParameterValue) obj;

        return name.equals(other.name) && value == other.value;
    }

    @Override
    public int hashCode() {

        return name.hashCode() * 31 + value;
    }

    @Override
    public String toString() {

        return name + "=" + value;
    }

    public JSONValue serialize() {

        final JSONObject object = new JSONObject();

        object.put("name", name);
        object.put("value", value);

        return object;
    }
}
