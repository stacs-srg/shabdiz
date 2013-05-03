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
package uk.ac.standrews.cs.shabdiz.host;

import java.io.File;
import java.io.IOException;

import org.json.JSONException;

import uk.ac.standrews.cs.nds.rpc.nostream.json.JSONObject;
import uk.ac.standrews.cs.nds.util.Input;

/**
 * Factory for {@link SSHPasswordCredentials}, {@link SSHPublicKeyCredentials} and utility methods for JSON serialisation and deserialisation of {@link SSHCredentials credentials}.
 * 
 * @author Masih Hajiarabderkani (mh638@st-andrews.ac.uk)
 */
public final class Credentials {

    private static final String USERNAME_KEY = "user_name";
    private static final String PASSWORD_KEY = "password";
    private static final String PRIVATE_KEY_FILE_KEY = "key_file";
    private static final String PASSPHRASE_KEY = "key_passphrase";

    private Credentials() {

    }

    /**
     * Gets the current user from system properties.
     * 
     * @return the current user
     * @see System#getProperty(String)
     */
    public static String getCurrentUser() {

        return System.getProperty("user.name");
    }

    /**
     * Initialises a credentials by prompting the user for information.
     * 
     * @param use_password whether the credentials is {@link SSHPasswordCredentials}
     * @return an instance of {@link SSHPasswordCredentials} if {@code use_password} is {@code true}, an instance of {@link SSHPublicKeyCredentials} otherwise
     * @throws IOException Signals that an I/O exception has occurred.
     */
    public static SSHCredentials newSSHCredential(final boolean use_password) throws IOException {

        final String username = Input.readLine("enter username: ");
        return use_password ? newSSHPasswordCredential(username) : newSSHPublicKeyCredential(username);
    }

    private static SSHCredentials newSSHPublicKeyCredential(final String username) throws IOException {

        final File private_key = new File(Input.readLine("enter full path to the private key"));
        final char[] passphrase = Input.readPassword("enter private key passphrase:");
        return new SSHPublicKeyCredentials(username, private_key, passphrase);
    }

    private static SSHPasswordCredentials newSSHPasswordCredential(final String username) {

        final char[] password = Input.readPassword("enter password:");
        return new SSHPasswordCredentials(username, password);
    }

    /**
     * Serialises the given {@link SSHCredentials SSH credential} to {@link JSONObject JSON}.
     * 
     * @param credential the credential to serialise
     * @return the serialised SSH credentials
     * @throws JSONException if the given {@code credential} type is unknown
     * @see #fromJSONObject(JSONObject)
     */
    public static JSONObject toJSONObject(final SSHCredentials credential) throws JSONException {

        final JSONObject serialized_credentials;
        if (credential == null) {
            serialized_credentials = JSONObject.NULL;
        }
        else if (SSHPasswordCredentials.class.isInstance(credential)) {
            serialized_credentials = toJSONObject(SSHPasswordCredentials.class.cast(credential));
        }
        else if (SSHPublicKeyCredentials.class.isInstance(credential)) {
            serialized_credentials = toJSONObject(SSHPublicKeyCredentials.class.cast(credential));
        }
        else {
            throw new JSONException("unable to serialize credentials to JSON object; unknown credential type");
        }

        return serialized_credentials;
    }

    /**
     * Deserialises a {@link JSONObject} to either a {@link SSHPasswordCredentials} or a {@link SSHPublicKeyCredentials}.
     * 
     * @param serialized_credential the serialised credentials
     * @return the deserialised credentials
     * @throws JSONException if an error occurs during deserialisation, or the given serialisation format is unknown
     * @see #toJSONObject(SSHCredentials)
     * @see #toJSONObject(SSHPasswordCredentials)
     * @see #toJSONObject(SSHPublicKeyCredentials)
     */
    public static SSHCredentials fromJSONObject(final JSONObject serialized_credential) throws JSONException {

        final String username = serialized_credential.getString(USERNAME_KEY);
        if (isSerializedSSHPasswordCredential(serialized_credential)) {
            return deserializeSSHPasswordCredential(serialized_credential, username);
        }
        else if (isSerializedSSHPublicKeyCredential(serialized_credential)) {
            return deserialiseSSHPublicKeyCredential(serialized_credential, username);
        }
        else {
            throw new JSONException("Unknown serialized credentials type");
        }
    }

    private static SSHCredentials deserialiseSSHPublicKeyCredential(final JSONObject serialized_credential, final String username) throws JSONException {

        final String passphrase = serialized_credential.getString(PASSPHRASE_KEY);
        final String file_path = serialized_credential.getString(PRIVATE_KEY_FILE_KEY);
        final File private_key_file = file_path != null ? new File(file_path) : null;
        return new SSHPublicKeyCredentials(username, private_key_file, passphrase.toCharArray());
    }

    private static SSHCredentials deserializeSSHPasswordCredential(final JSONObject serialized_credential, final String username) throws JSONException {

        final String password = serialized_credential.getString(PASSWORD_KEY);
        return new SSHPasswordCredentials(username, password.toCharArray());
    }

    private static boolean isSerializedSSHPasswordCredential(final JSONObject serialized_credential) {

        return serialized_credential.has(USERNAME_KEY) && serialized_credential.has(PASSWORD_KEY) && !serialized_credential.has(PASSPHRASE_KEY) && !serialized_credential.has(PRIVATE_KEY_FILE_KEY);
    }

    private static boolean isSerializedSSHPublicKeyCredential(final JSONObject serialized_credential) {

        return serialized_credential.has(USERNAME_KEY) && !serialized_credential.has(PASSWORD_KEY) && serialized_credential.has(PASSPHRASE_KEY) && serialized_credential.has(PRIVATE_KEY_FILE_KEY);
    }
}
