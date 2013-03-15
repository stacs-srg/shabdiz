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
package uk.ac.standrews.cs.shabdiz.jobs;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;

import uk.ac.standrews.cs.jetson.util.CloseableUtil;
import uk.ac.standrews.cs.shabdiz.api.JobRemote;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.BeanDescription;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializationConfig;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.module.SimpleSerializers;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import com.fasterxml.jackson.databind.type.ClassKey;

class WorkerModule extends SimpleModule {

    public static final class JsonSerializableDeserializer extends StdDeserializer<Serializable> {

        protected JsonSerializableDeserializer() {

            super(Serializable.class);
        }

        @Override
        public Serializable deserialize(final JsonParser parser, final DeserializationContext context) throws IOException, JsonProcessingException {

            ObjectInputStream object_input_stream = null;

            try {
                final byte[] object_as_bytes = parser.getBinaryValue();
                if (object_as_bytes == null) { return null; }
                final ByteArrayInputStream bytes_input_stream = new ByteArrayInputStream(object_as_bytes);
                object_input_stream = new ObjectInputStream(bytes_input_stream);

                final Serializable deserialized_object = (Serializable) object_input_stream.readObject();

                return deserialized_object;
            }
            catch (final ClassNotFoundException e) {
                throw new JsonMappingException("unable to deserialize Java object", e);
            }
            finally {
                CloseableUtil.closeQuietly(object_input_stream);
            }
        }
    }

    public static final class JsonSerializableSerializer extends StdSerializer<Serializable> {

        protected JsonSerializableSerializer() {

            super(Serializable.class);
        }

        @Override
        public void serialize(final Serializable object, final JsonGenerator generator, final SerializerProvider provider) throws IOException, JsonProcessingException {

            if (isEmpty(object)) {
                generator.writeNull();
            }
            else {

                ObjectOutputStream object_output_stream = null;

                try {
                    final ByteArrayOutputStream bytes_output_stream = new ByteArrayOutputStream();
                    object_output_stream = new ObjectOutputStream(bytes_output_stream);
                    object_output_stream.writeObject(object);

                    final byte[] object_as_bytes = bytes_output_stream.toByteArray();
                    generator.writeBinary(object_as_bytes);
                }
                finally {
                    CloseableUtil.closeQuietly(object_output_stream);
                }
            }
        }
    }

    public WorkerModule() {

        super("Shabdiz Worker Module");

        addDeserializer(Serializable.class, new JsonSerializableDeserializer());
        setMixInAnnotation(JobRemote.class, SerializableMixIn.class);
        setMixInAnnotation(Throwable.class, SerializableMixIn.class);
        final ShabdizWorkerSerializers serializers = new ShabdizWorkerSerializers();
        serializers.addSerializer(Serializable.class, new JsonSerializableSerializer());
        setSerializers(serializers);
    }

    public class ShabdizWorkerSerializers extends SimpleSerializers {

        private static final long serialVersionUID = 679723185864882365L;

        @Override
        public JsonSerializer<?> findSerializer(final SerializationConfig config, final JavaType type, final BeanDescription beanDesc) {

            final Class<?> cls = type.getRawClass();
            final ClassKey key = new ClassKey(cls);

            if (cls.isInterface()) {
                if (_interfaceMappings != null) { return _interfaceMappings.get(key); }
            }
            else {
                if (_classMappings != null) { return _classMappings.get(key); }
            }
            return null;
        }
    }

    @JsonSerialize(using = JsonSerializableSerializer.class)
    @JsonDeserialize(using = JsonSerializableDeserializer.class)
    abstract class SerializableMixIn {

    }
}
