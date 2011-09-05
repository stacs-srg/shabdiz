/*
 * shabdiz Library
 * Copyright (C) 2011 Distributed Systems Architecture Research Group
 * <http://www-systems.cs.st-andrews.ac.uk/>
 *
 * This file is part of shabdiz, a variation of the Chord protocol
 * <http://pdos.csail.mit.edu/chord/>, where each node strives to maintain
 * a list of all the nodes in the overlay in order to provide one-hop
 * routing.
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
 * For more information, see <http://beast.cs.st-andrews.ac.uk:8080/hudson/job/shabdiz/>.
 */
package uk.ac.standrews.cs.shabdiz.worker.rpc;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.net.InetSocketAddress;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import org.json.JSONException;
import org.json.JSONWriter;

import uk.ac.standrews.cs.nds.rpc.DeserializationException;
import uk.ac.standrews.cs.nds.rpc.stream.JSONReader;
import uk.ac.standrews.cs.nds.rpc.stream.Marshaller;
import uk.ac.standrews.cs.shabdiz.interfaces.IFutureRemoteReference;
import uk.ac.standrews.cs.shabdiz.interfaces.IJobRemote;
import uk.ac.standrews.cs.shabdiz.worker.FutureRemoteReference;

/**
 * The Class McJobRemoteMarshaller.
 * 
 * @author Masih Hajiarabderkani (mh638@st-andrews.ac.uk)
 */
public class WorkerRemoteMarshaller extends Marshaller {

    private static final String JOB_ID_KEY = "jobid";
    private static final String ADDRESS_KEY = "address";

    //TODO improve exception serialisation/deserialisation
    @Override
    public void serializeException(final Exception e, final JSONWriter writer) throws JSONException {

        try {
            serializeSerializable(e, writer);
        }
        catch (final IOException e1) {
            new JSONException("unable to serialise exception as Java Serializable : " + e1.getMessage());
        }
    }

    @Override
    public Exception deserializeException(final JSONReader reader) {

        try {
            return (Exception) deserializeSerializable(reader);
        }
        catch (final DeserializationException e) {
            return new RuntimeException("could not instantiate serialized exception ", e);
        }
    }

    /**
     * Serialises a {@link Serializable} using {@link ObjectOutputStream}.
     *
     * @param object the object to serialise
     * @param writer the writer
     * @throws JSONException if JSON related error occurs
     * @throws IOException Signals that an I/O exception has occurred
     */
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
            catch (final Exception e) {
                e.printStackTrace();
            }
            finally {
                closeSilently(object_output_stream);
            }
        }
    }

    /**
     * Deserialises a {@link Serializable}.
     *
     * @param reader the reader to read the serialised from
     * @return the deserialised object
     * @throws DeserializationException if unable to deserialise
     */
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

    /**
     * Deserialises a {@link Serializable} object and casts it to the given generic type.
     *
     * @param <Result> the type of result
     * @param reader the reader to read the serialised from
     * @return the deserialised object
     * @throws DeserializationException if unable to deserialise
     */
    @SuppressWarnings("unchecked")
    public <Result extends Serializable> Result deserializeResult(final JSONReader reader) throws DeserializationException {

        final Serializable deserialized_serializable = deserializeSerializable(reader);
        try {
            return (Result) deserialized_serializable;
        }
        catch (final ClassCastException e) {
            throw new DeserializationException(e);
        }
    }

    /**
     * Serialises remote job.
     *
     * @param remote_job the remote job to serialise
     * @param writer the writer
     * @throws JSONException if a JSON related error occurs
     * @throws IOException Signals that an I/O exception has occurred.
     */
    public void serializeRemoteJob(final IJobRemote<?> remote_job, final JSONWriter writer) throws JSONException, IOException {

        serializeSerializable(remote_job, writer);
    }

    /**
     * Deserialises a {@link IJobRemote}.
     *
     * @param reader the reader to read the serialised job from
     * @return the deserialised remote job
     * @throws DeserializationException if unable to deserialise
     */
    @SuppressWarnings("unchecked")
    public IJobRemote<?> deserializeRemoteJob(final JSONReader reader) throws DeserializationException {

        try {
            return (IJobRemote<? extends Serializable>) deserializeSerializable(reader);
        }
        catch (final Exception e) {
            throw new DeserializationException(e);
        }
    }

    /**
     * Serialises future remote reference.
     *
     * @param <Result> the generic type
     * @param future_remote the future_remote
     * @param writer the writer
     * @throws JSONException if a JSON related error occurs
     */
    public <Result extends Serializable> void serializeFutureRemoteReference(final IFutureRemoteReference<Result> future_remote, final JSONWriter writer) throws JSONException {

        if (future_remote == null) {
            writer.value(null);
        }
        else {
            writer.object();

            writer.key(JOB_ID_KEY);
            serializeUUID(future_remote.getId(), writer);

            writer.key(ADDRESS_KEY);
            serializeInetSocketAddress(future_remote.getAddress(), writer);

            writer.endObject();
        }
    }

    /**
     * Deserialises a {@link FutureRemoteReference}.
     *
     * @param <Result> the type of result which is pending in the future remote reference
     * @param reader the reader to read the serialised from
     * @return the deserialised object
     * @throws DeserializationException if unable to deserialise
     */
    public <Result extends Serializable> FutureRemoteReference<Result> deserializeFutureRemoteReference(final JSONReader reader) throws DeserializationException {

        try {
            if (reader.checkNull()) { return null; }

            reader.object();

            reader.key(JOB_ID_KEY);
            final UUID job_id = deserializeUUID(reader);

            reader.key(ADDRESS_KEY);
            final InetSocketAddress proxy_address = deserializeInetSocketAddress(reader);

            reader.endObject();

            return new FutureRemoteReference<Result>(job_id, proxy_address);
        }
        catch (final Exception e) {
            throw new DeserializationException(e);
        }
    }

    /**
     * Serialises a {@link TimeUnit} and.
     *
     * @param unit the unit
     * @param writer the writer
     * @throws JSONException the jSON exception
     */
    public void serializeTimeUnit(final TimeUnit unit, final JSONWriter writer) throws JSONException {

        if (unit == null) {
            writer.value(null);
        }
        else {
            writer.value(unit.name());
        }
    }

    /**
     * Deserialises a {@link TimeUnit}.
     *
     * @param reader the reader to read the serialised from
     * @return the deserialised object
     * @throws DeserializationException if unable to deserialise
     */
    public TimeUnit deserializeTimeUnit(final JSONReader reader) throws DeserializationException {

        try {
            if (reader.checkNull()) { return null; }

            return TimeUnit.valueOf(reader.stringValue());
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
