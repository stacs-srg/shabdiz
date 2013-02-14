package uk.ac.standrews.cs.nds.madface;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;

import org.json.JSONException;

import uk.ac.standrews.cs.nds.madface.exceptions.InvalidCredentialsException;
import uk.ac.standrews.cs.nds.rpc.nostream.json.JSONObject;
import uk.ac.standrews.cs.nds.rpc.nostream.json.JSONValue;
import uk.ac.standrews.cs.nds.util.Input;

import com.mindbright.ssh2.SSH2Exception;
import com.mindbright.ssh2.SSH2SimpleClient;
import com.mindbright.ssh2.SSH2Transport;

/**
 * Represents a set of authentication credentials, catering for the various permutations of current user or named user;
 * private key or password; encrypted or unencrypted private key; and default or specified private key location.
 *
 * @author Graham Kirby (graham.kirby@st-andrews.ac.uk)
 */
public class Credentials {

    /**
     * The JSON key for the user name.
     */
    public static final String USER_NAME_KEY = "user_name";

    /**
     * The JSON key for the password.
     */
    public static final String PASSWORD_KEY = "password";

    /**
     * The JSON key for the private key file path.
     */
    public static final String KEY_FILE_KEY = "key_file";

    /**
     * The JSON key for the private key passphrase.
     */
    public static final String KEY_PASSPHRASE_KEY = "key_passphrase";

    /**
     * The name of the private key file.
     */
    public static final String PRIVATE_KEY_NAME = "id_rsa";

    /**
     * The directory containing the private key file.
     */
    public static final String SSH_DIR = ".ssh";

    // -------------------------------------------------------------------------------------------------------

    private volatile String user_name = null;
    private volatile String password = null;
    private volatile File key_file = null;
    private volatile String key_passphrase = null;

    // -------------------------------------------------------------------------------------------------------

    /**
     * Creates credentials for the current user using default private key location.
     */
    public Credentials() {

        currentUser();
        privateKey();
    }

    /**
     * Creates credentials from a serialized representation as a JSON string. The representation must contain a string user name with the key {@link #USER_NAME_KEY}, plus either a string
     * password with the key {@link #PASSWORD_KEY}, or a string private key file path with the key {@link #KEY_FILE_KEY} plus a string private key passphrase with the key {@link #KEY_PASSPHRASE_KEY}.
     *
     * @param serialized_credentials the serialized credentials
     * @throws InvalidCredentialsException if the representation is invalid
     */
    public Credentials(final JSONObject serialized_credentials) throws InvalidCredentialsException {

        init(serialized_credentials);
    }

    /**
    * Creates credentials from a serialized representation as a JSON string. The representation must contain a string user name with the key {@link #USER_NAME_KEY}, plus either a string
    * password with the key {@link #PASSWORD_KEY}, or a string private key file path with the key {@link #KEY_FILE_KEY} plus a string private key passphrase with the key {@link #KEY_PASSPHRASE_KEY}.
    *
    * @param serialized_credentials the serialized credentials
    * @throws InvalidCredentialsException if the representation is invalid
    */
    public Credentials(final String serialized_credentials) throws InvalidCredentialsException {

        try {
            init(new JSONObject(serialized_credentials));
        }
        catch (final JSONException e) {
            throw new InvalidCredentialsException("invalid credentials: " + serialized_credentials);
        }
    }

    public Credentials(final boolean use_password) throws IOException {

        this();

        final BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));

        System.out.print("Enter remote username: ");
        final String user_name = reader.readLine();
        user(user_name);

        if (use_password) {
            password(readPassword());
        }
        else {
            keyPassphrase(readPassphrase());
        }
    }

    // -------------------------------------------------------------------------------------------------------

    /*
     * The setter methods are written in the style returning 'this', to enable a set of attributes to be set succinctly.
     *
     * For example: new Credentials().user("john").privateKey()
     */

    /**
     * Sets the user to the current user as defined by the system property "user.name".
     *
     * @return this object
     */
    public Credentials currentUser() {

        user_name = getCurrentUser();
        return this;
    }

    /**
     * Sets the user.
     *
     * @param user_name a user name
     * @return this object
     */
    public Credentials user(final String user_name) {

        this.user_name = user_name;
        return this;
    }

    /**
     * Sets authentication to private key using an unencrypted key file in the default location.
     *
     * @return this object
     */
    public Credentials privateKey() {

        return privateKey(getDefaultKeyFile());
    }

    /**
     * Sets authentication to private key using the specified unencrypted key file.
     *
     * @param key_file the key file
     * @return this object
     */
    public Credentials privateKey(final File key_file) {

        this.key_file = key_file;
        password = null;
        return this;
    }

    /**
     * Sets the private key passphrase.
     *
     * @param key_passphrase the key file
     * @return this object
     */
    public Credentials keyPassphrase(final String key_passphrase) {

        this.key_passphrase = key_passphrase;
        return this;
    }

    /**
     * Sets authentication to use the specified password.
     *
     * @param password the password
     * @return this object
     */
    public Credentials password(final String password) {

        this.password = password;
        key_file = null;
        key_passphrase = null;
        return this;
    }

    /**
     * Returns the user name.
     *
     * @return the user name
     */
    public String getUser() {

        return user_name;
    }

    /**
     * Serializes the credentials to a JSON string.
     *
     * @return the serialized credentials
     */
    public JSONValue serialize() {

        final JSONObject object = new JSONObject();

        object.put(USER_NAME_KEY, user_name);
        object.put(PASSWORD_KEY, password);
        object.put(KEY_PASSPHRASE_KEY, key_passphrase);
        object.put(KEY_FILE_KEY, key_file != null ? key_file.getAbsolutePath() : null);

        return object;
    }

    @Override
    public String toString() {

        final StringBuilder builder = new StringBuilder();

        builder.append("user: " + user_name + "\n");
        builder.append("password: " + password + "\n");
        builder.append("passphrase: " + key_passphrase + "\n");
        builder.append("key file: " + key_file + "\n");

        return builder.toString();
    }

    // -------------------------------------------------------------------------------------------------------

    SSH2SimpleClient makeSSHClient(final SSH2Transport transport) throws SSH2Exception, IOException {

        if (password != null) { return new SSH2SimpleClient(transport, user_name, password); }
        return new SSH2SimpleClient(transport, user_name, key_file.getAbsolutePath(), key_passphrase);
    }

    // -------------------------------------------------------------------------------------------------------

    private static File getDefaultKeyFile() {

        return new File(new File(System.getProperty("user.home"), SSH_DIR), PRIVATE_KEY_NAME);
    }

    private static String getCurrentUser() {

        return System.getProperty("user.name");
    }

    private static String readPassword() {

        return Input.readMaskedLine("Enter remote password: ");
    }

    private static String readPassphrase() {

        return Input.readMaskedLine("Enter SSH passphrase: ");
    }

    private void validate() throws InvalidCredentialsException {

        if (user_name == null) { throw new InvalidCredentialsException("no user name"); }

        if (password == null) {
            if (key_passphrase == null) { throw new InvalidCredentialsException("no password or private key passphrase"); }
            if (key_file == null) { throw new InvalidCredentialsException("no password or private key file"); }
        }
    }

    private void init(final JSONObject serialized_credentials) throws InvalidCredentialsException {

        try {
            user_name = serialized_credentials.getString(USER_NAME_KEY);
            password = serialized_credentials.getString(PASSWORD_KEY);
            key_passphrase = serialized_credentials.getString(KEY_PASSPHRASE_KEY);

            final String file_path = serialized_credentials.getString(KEY_FILE_KEY);
            key_file = file_path != null ? new File(file_path) : null;

            validate();
        }
        catch (final JSONException e) {
            throw new InvalidCredentialsException("invalid credentials: " + serialized_credentials);
        }
    }

    /**
     * Create the serialized JSON string needed for {@link #Credentials(String)}.
     * @param username
     * @param keyFileLocation
     * @param keyPassphrase
     * @return String version of a JSON object.
     */
    public static String constructJSONString(final String username, final String keyFileLocation, final String keyPassphrase) {

        final String credentials = "{ \"" + USER_NAME_KEY + "\" : \"" + username + "\", \"" + KEY_FILE_KEY + "\" : \"" + keyFileLocation + "\", \"" + KEY_PASSPHRASE_KEY + "\" : \"" + keyPassphrase + "\"};";

        return credentials;
    }
}
