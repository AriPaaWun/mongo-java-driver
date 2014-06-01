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
 * Holder for a BSON type DBPointer(0x0c). It's deprecated in BSON Specification and present here because of compatibility reasons.
 *
 * @since 3.0
 */
public class DBPointer extends BsonValue implements Serializable {
    private static final long serialVersionUID = -5105961452917374359L;

    private final String namespace;
    private final ObjectId id;

    public DBPointer(final String namespace, final ObjectId id) {
        if (namespace == null) {
            throw new IllegalArgumentException("namespace can not be null");
        }
        if (id == null) {
            throw new IllegalArgumentException("id can not be null");
        }
        this.namespace = namespace;
        this.id = id;
    }

    @Override
    public BsonType getBsonType() {
        return BsonType.DB_POINTER;
    }

    public String getNamespace() {
        return namespace;
    }

    public ObjectId getId() {
        return id;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        DBPointer dbPointer = (DBPointer) o;

        if (!id.equals(dbPointer.id)) {
            return false;
        }
        if (!namespace.equals(dbPointer.namespace)) {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode() {
        int result = namespace.hashCode();
        result = 31 * result + id.hashCode();
        return result;
    }
}
