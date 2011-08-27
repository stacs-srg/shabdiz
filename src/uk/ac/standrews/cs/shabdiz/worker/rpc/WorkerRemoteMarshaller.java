package uk.ac.standrews.cs.shabdiz.worker.rpc;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;

import org.json.JSONException;
import org.json.JSONWriter;

import uk.ac.standrews.cs.shabdiz.interfaces.IRemoteJob;
import uk.ac.standrews.cs.nds.rpc.DeserializationException;
import uk.ac.standrews.cs.nds.rpc.stream.JSONReader;
import uk.ac.standrews.cs.nds.rpc.stream.Marshaller;

/**
 * The Class McJobRemoteMarshaller.
 * 
 * @author Masih Hajiarabderkani (mh638@st-andrews.ac.uk)
 */
public class WorkerRemoteMarshaller extends Marshaller {

    public void serializeSerializable(final Serializable object, final JSONWriter writer) throws JSONException, IOException {

        if (object == null) {
            writer.value(null);
        }
        else {

            ObjectOutputStream object_output_stream = null;

            try {
                final ByteArrayOutputStream bytes_output_stream = new ByteArrayOutputStream();
                object_output_stream = new ObjectOutputStream(bytes_output_stream);
                object_output_stream.writeObject(object);

                final byte[] object_as_bytes = bytes_output_stream.toByteArray();
                serializeBytes(object_as_bytes, writer);
            }
            finally {
                closeSilently(object_output_stream);
            }
        }
    }

    public Serializable deserializeSerializable(final JSONReader reader) throws DeserializationException {

        ObjectInputStream object_input_stream = null;

        try {
            if (reader.checkNull()) { return null; }

            final byte[] serialized_object_as_bytes = deserializeBytes(reader);
            final ByteArrayInputStream bytes_input_stream = new ByteArrayInputStream(serialized_object_as_bytes);
            object_input_stream = new ObjectInputStream(bytes_input_stream);

            final Serializable deserialized_object = (Serializable) object_input_stream.readObject();

            return deserialized_object;
        }
        catch (final Exception e) {
            throw new DeserializationException(e);
        }
        finally {
            closeSilently(object_input_stream);
        }
    }

    public void serializeRemoteJob(final IRemoteJob<?> remote_job, final JSONWriter writer) throws JSONException, IOException {

        serializeSerializable(remote_job, writer);
    }

    @SuppressWarnings("unchecked")
    public IRemoteJob<?> deserializeRemoteJob(final JSONReader reader) throws DeserializationException {

        try {
            return (IRemoteJob<? extends Serializable>) deserializeSerializable(reader);
        }
        catch (final Exception e) {
            throw new DeserializationException(e);
        }
    }

    // -------------------------------------------------------------------------------------------------------------------------------

    private void closeSilently(final Closeable closable) {

        if (closable == null) { return; }

        try {
            closable.close();
        }
        catch (final IOException e) {
            // Keep Calm and Carry On
        }
    }
}
