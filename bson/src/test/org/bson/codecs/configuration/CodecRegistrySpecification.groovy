/*
 * Copyright (c) 2008-2014 MongoDB, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.bson.codecs.configuration

import org.bson.BsonBinaryReader
import org.bson.BsonBinaryWriter
import org.bson.BsonReader
import org.bson.BsonType
import org.bson.BsonWriter
import org.bson.ByteBufNIO
import org.bson.codecs.Codec
import org.bson.codecs.MinKeyCodec
import org.bson.io.BasicInputBuffer
import org.bson.io.BasicOutputBuffer
import org.bson.types.MinKey
import spock.lang.Specification

import java.nio.ByteBuffer

import static java.util.Arrays.asList

class CodecRegistrySpecification extends Specification {

    def 'get should throw for unregistered codec'() {
        when:
        new RootCodecRegistry([]).get(MinKey) == null

        then:
        thrown(CodecConfigurationException)
    }

    def 'get should return registered codec'() {
        given:
        def minKeyCodec = new MinKeyCodec()
        def registry = new RootCodecRegistry([new SimpleCodecSource(minKeyCodec)])

        expect:
        registry.get(MinKey) is minKeyCodec
    }

    def 'get should return the codec from the first source that has one'() {
        given:
        def minKeyCodec1 = new MinKeyCodec()
        def minKeyCodec2 = new MinKeyCodec()
        def registry = new RootCodecRegistry([new SimpleCodecSource(minKeyCodec1), new SimpleCodecSource(minKeyCodec2)])

        expect:
        registry.get(MinKey) is minKeyCodec1
    }

    def 'should handle cycles'() {
        given:
        def registry = new RootCodecRegistry([new ClassModelCodecSource()])

        when:
        Codec<Top> topCodec = registry.get(Top)

        then:
        topCodec instanceof TopCodec

        when:
        def top = new Top('Bob',
                          new Top('Jim', null, null),
                          new Nested('George', new Top('Joe', null, null)))
        def writer = new BsonBinaryWriter(new BasicOutputBuffer(), false)
        topCodec.encode(writer, top)
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        writer.getBuffer().pipe(os);
        writer.close()

        then:
        topCodec.decode(new BsonBinaryReader(new BasicInputBuffer(new ByteBufNIO(ByteBuffer.wrap(os.toByteArray()))), false)) == top
    }

    def 'should throw CodecConfigurationException when a codec requires another codec that can not be found'() {
        given:
        def registry = new RootCodecRegistry([new ClassModelCodecSource([Top])]);

        when:
        registry.get(Top)

        then:
        thrown(CodecConfigurationException)
    }
}

class SimpleCodecSource implements CodecSource {

    private final Codec<?> codec

    SimpleCodecSource(final Codec<?> codec) {
        this.codec = codec
    }

    @Override
    def <T> Codec<T> get(final Class<T> clazz, final CodecRegistry registry) {
        if (clazz == codec.getEncoderClass()) {
            return codec
        } else {
            return null
        }
    }
}

class ClassModelCodecSource implements CodecSource {

    private final List<Class<?>> supportedClasses

    ClassModelCodecSource() {
        this(asList(Top.class, Nested.class))
    }

    ClassModelCodecSource(List<Class<?>> supportedClasses) {
        this.supportedClasses = supportedClasses
    }

    @Override
    def <T> Codec<T> get(final Class<T> clazz, final CodecRegistry registry) {
        if (!supportedClasses.contains(clazz)) {
            return null;
        } else if (clazz == Top) {
            return new TopCodec(registry)
        } else if (clazz == Nested) {
            return new NestedCodec(registry)
        } else {
            return null
        }
    }
}

class TopCodec implements Codec<Top> {
    Codec<TopCodec> codecForOther
    Codec<Nested> codecForNested

    TopCodec(final CodecRegistry registry) {
        codecForOther = registry.get(Top)
        codecForNested = registry.get(Nested)
    }

    @Override
    void encode(final BsonWriter writer, final Top top) {
        if (top == null) {
            writer.writeNull()
            return
        }

        writer.writeStartDocument()
        writer.writeString('name', top.getName())
        writer.writeName('other')
        codecForOther.encode(writer, top.getOther())
        writer.writeName('nested')
        codecForNested.encode(writer, top.getNested())
        writer.writeEndDocument()
    }

    @Override
    Class<Top> getEncoderClass() {
        return Top
    }

    @Override
    def Top decode(final BsonReader reader) {
        reader.readStartDocument()
        reader.readName()
        def name = reader.readString()
        def other = null
        def nested = null

        def type = reader.readBSONType()

        reader.readName()
        if (type == BsonType.NULL) {
            reader.readNull()
        } else {
            other = codecForOther.decode(reader)
        }

        reader.readName('nested')
        if (type == BsonType.NULL) {
            reader.readNull()
        } else {
            nested = codecForNested.decode(reader)
        }
        reader.readEndDocument()
        return new Top(name, other, nested);
    }
}

class NestedCodec implements Codec<Nested> {
    Codec<TopCodec> codecForTop

    NestedCodec(final CodecRegistry registry) {
        codecForTop = registry.get(Top)
    }

    @Override
    void encode(final BsonWriter writer, final Nested nested) {
        if (nested == null) {
            writer.writeNull()
            return
        }

        writer.writeStartDocument()
        writer.writeString('name', nested.getName())
        writer.writeName('top')
        codecForTop.encode(writer, nested.getTop())
        writer.writeEndDocument()
    }

    @Override
    Class<Top> getEncoderClass() {
        return Top
    }

    @Override
    def Nested decode(final BsonReader reader) {
        reader.readStartDocument()
        reader.readName()
        def name = reader.readString()
        def type = reader.readBSONType()
        reader.readName()
        def top = null
        if (type == BsonType.NULL) {
            reader.readNull()
        } else {
            top = codecForTop.decode(reader)
        }
        reader.readEndDocument()
        return new Nested(name, top);
    }
}

class Top {
    private String name
    private Top other
    private Nested nested

    Top(final String name, final Top other, final Nested nested) {
        this.name = name
        this.other = other
        this.nested = nested
    }

    String getName() {
        return name
    }

    Top getOther() {
        return other
    }

    Nested getNested() {
        return nested
    }

    boolean equals(final o) {
        if (this.is(o)) {
            return true
        }
        if (getClass() != o.class) {
            return false
        }

        Top top = (Top) o

        if (name != top.name) {
            return false
        }
        if (nested != top.nested) {
            return false
        }
        if (other != top.other) {
            return false
        }

        return true
    }

    int hashCode() {
        int result
        result = name.hashCode()
        result = 31 * result + (other != null ? other.hashCode() : 0)
        result = 31 * result + (nested != null ? nested.hashCode() : 0)
        return result
    }
}

class Nested {
    private String name
    private Top top

    Nested(final String name, final Top top) {
        this.name = name
        this.top = top
    }

    String getName() {
        return name
    }

    Top getTop() {
        return top
    }

    boolean equals(final o) {
        if (this.is(o)) {
            return true
        }
        if (getClass() != o.class) {
            return false
        }

        Nested nested = (Nested) o

        if (name != nested.name) {
            return false
        }
        if (top != nested.top) {
            return false
        }

        return true
    }

    int hashCode() {
        int result
        result = name.hashCode()
        result = 31 * result + (top != null ? top.hashCode() : 0)
        return result
    }
}

