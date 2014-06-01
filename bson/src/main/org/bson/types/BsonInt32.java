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

package org.bson.types;

import org.bson.BsonType;

import java.io.Serializable;

/**
 * A representation of the BSON Int32 type.
 *
 * @since 3.0
 */
public final class BsonInt32 extends BsonNumber implements Comparable<BsonInt32>, Serializable {
    private static final long serialVersionUID = 2215506922933899945L;

    private final int value;

    /**
     * Construct a new instance with the given value.
     *
     * @param value the value
     */
    public BsonInt32(final int value) {
        this.value = value;
    }

    @Override
    public int compareTo(final BsonInt32 o) {
        return Integer.compare(value, o.value);
    }

    @Override
    public BsonType getBsonType() {
        return BsonType.INT32;
    }

    /**
     * Gets the integer value.
     *
     * @return the value
     */
    public int getValue() {
        return value;
    }

    @Override
    public int intValue() {
        return value;
    }

    @Override
    public long longValue() {
        return value;
    }

    @Override
    public double doubleValue() {
        return value;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        BsonInt32 bsonInt32 = (BsonInt32) o;

        if (value != bsonInt32.value) {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode() {
        return value;
    }

    @Override
    public String toString() {
        return "BsonInt32{"
               + "value=" + value
               + '}';
    }
}
