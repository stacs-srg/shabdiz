package uk.ac.standrews.cs.shabdiz.util;

import java.io.File;
import java.io.IOException;

import org.json.JSONException;

import uk.ac.standrews.cs.nds.rpc.nostream.json.JSONObject;
import uk.ac.standrews.cs.nds.util.Input;
import uk.ac.standrews.cs.shabdiz.active.Credentials;
import uk.ac.standrews.cs.shabdiz.active.PasswordCredentials;
import uk.ac.standrews.cs.shabdiz.active.PublicKeyCredentials;
import uk.ac.standrews.cs.shabdiz.active.exceptions.InvalidCredentialsException;

public final class CredentialsUtil {

    /** The JSON key for the user name. */
    public static final String USERNAME_KEY = "user_name";

    /** The JSON key for the password. */
    public static final String PASSWORD_KEY = "password";

    /** The JSON key for the private key file path. */
    public static final String PRIVATE_KEY_FILE_KEY = "key_file";

    /** The JSON key for the private key passphrase. */
    public static final String PASSPHRASE_KEY = "key_passphrase";

    private CredentialsUtil() {

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
     * @param use_password whether the credentials is {@link PasswordCredentials}
     * @return an instance of {@link PasswordCredentials} if {@code use_password} is {@code true}, an instance of {@link PublicKeyCredentials} otherwise
     * @throws IOException Signals that an I/O exception has occurred.
     */
    public static Credentials initCredentials(final boolean use_password) throws IOException {

        final String username = Input.readLine("enter username: ");
        final Credentials credentials;

        if (use_password) {
            final char[] password = Input.readPassword("enter password:");
            credentials = new PasswordCredentials(username, password);
        }
        else {
            final char[] passphrase = Input.readPassword("enter passphrase:");
            final File private_key = new File(Input.readLine("enter path to private key"));
            credentials = new PublicKeyCredentials(username, private_key, passphrase);
        }

        return credentials;
    }

    public static JSONObject toJSONObject(final Credentials credentials) throws JSONException {

        final JSONObject serialized_credentials;
        if (credentials == null) {
            serialized_credentials = JSONObject.NULL;
        }
        else if (PasswordCredentials.class.isInstance(credentials)) {
            serialized_credentials = toJSONObject(PasswordCredentials.class.cast(credentials));
        }
        else if (PublicKeyCredentials.class.isInstance(credentials)) {
            serialized_credentials = toJSONObject(PublicKeyCredentials.class.cast(credentials));
        }
        else {
            throw new JSONException("unable to serialize credentials to JSON object; unknown credential type");
        }

        return serialized_credentials;
    }

    /**
     * Serializes a {@link PasswordCredentials} instance to a {@link JSONObject}.
     * 
     * @return the serialized password credentials
     */
    public static JSONObject toJSONObject(final PasswordCredentials credentials) {

        final JSONObject serialized_credentials = new JSONObject();
        serialized_credentials.put(USERNAME_KEY, credentials.getUsername());
        serialized_credentials.put(PASSWORD_KEY, new String(credentials.getPassword()));
        return serialized_credentials;
    }

    /**
     * Serializes a {@link PublicKeyCredentials} instance to a {@link JSONObject}.
     * 
     * @return the serialized public key credentials
     */
    public static JSONObject toJSONObject(final PublicKeyCredentials credentials) {

        final JSONObject serialized_credentials = new JSONObject();
        serialized_credentials.put(USERNAME_KEY, credentials.getUsername());
        serialized_credentials.put(PASSPHRASE_KEY, new String(credentials.getPassword()));
        serialized_credentials.put(PRIVATE_KEY_FILE_KEY, credentials.getPrivateKey().getAbsolutePath());
        return serialized_credentials;
    }

    /**
     * Deserializes a {@link JSONObject} to either {@link PasswordCredentials} or {@link PublicKeyCredentials}.
     * 
     * @param serialized_credentials the serialized credentials
     * @return the deserialized credentials
     * @throws InvalidCredentialsException if an error occurs during deserialization, or the given serialization format is unknown
     */
    public static Credentials fromJSONObject(final JSONObject serialized_credentials) throws InvalidCredentialsException {

        final Credentials credentials;
        try {
            final String username = serialized_credentials.getString(USERNAME_KEY);
            if (isSerializedPasswordCredentials(serialized_credentials)) {
                final String password = serialized_credentials.getString(PASSWORD_KEY);
                credentials = new PasswordCredentials(username, password.toCharArray());
            }
            else if (isSerializedPublicKeyCredentials(serialized_credentials)) {
                final String passphrase = serialized_credentials.getString(PASSPHRASE_KEY);
                final String file_path = serialized_credentials.getString(PRIVATE_KEY_FILE_KEY);
                final File private_key_file = file_path != null ? new File(file_path) : null;
                credentials = new PublicKeyCredentials(username, private_key_file, passphrase.toCharArray());
            }
            else {
                throw new InvalidCredentialsException("Unknown serialized credentials type");
            }
        }
        catch (final JSONException e) {
            throw new InvalidCredentialsException("Unable to parse serialized credentials", e);
        }

        return credentials;
    }

    private static boolean isSerializedPasswordCredentials(final JSONObject serialized_credentials) {

        return serialized_credentials.has(USERNAME_KEY) && serialized_credentials.has(PASSWORD_KEY) && !serialized_credentials.has(PASSPHRASE_KEY) && !serialized_credentials.has(PRIVATE_KEY_FILE_KEY);
    }

    private static boolean isSerializedPublicKeyCredentials(final JSONObject serialized_credentials) {

        return serialized_credentials.has(USERNAME_KEY) && !serialized_credentials.has(PASSWORD_KEY) && serialized_credentials.has(PASSPHRASE_KEY) && serialized_credentials.has(PRIVATE_KEY_FILE_KEY);
    }
}
