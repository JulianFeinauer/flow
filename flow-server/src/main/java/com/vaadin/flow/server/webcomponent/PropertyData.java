/*
 * Copyright 2000-2018 Vaadin Ltd.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package com.vaadin.flow.server.webcomponent;

import java.io.Serializable;
import java.util.Objects;

/**
 * Value object containing information of a WebComponent property field.
 */
public final class PropertyData<P extends Serializable> implements Serializable {
    private final String name;
    private final Class<P> type;
    private final P defaultValue;
    private final boolean readOnly;

    /**
     * @param name          name of the property
     * @param type          type of the property value
     * @param readOnly      is the property read-only (on the client-side)
     * @param defaultValue  default value for the property
     */
    public PropertyData(String name, Class<P> type, boolean readOnly,
                        P defaultValue) {
        Objects.requireNonNull(name, "Parameter 'name' must not be null!");
        Objects.requireNonNull(type, "Parameter 'type' must not be null!");
        this.name = name;
        this.type = type;
        this.readOnly = readOnly;
        this.defaultValue = defaultValue;
    }

    /**
     * Copy-constructor, which allows for changing the {@code readOnly} flag.
     *
     * @param data      base property data
     * @param readOnly  new read-only value
     */
    public PropertyData(PropertyData<P> data, boolean readOnly) {
        this(data.name, data.type, readOnly, data.defaultValue);
    }

    /**
     * Getter for the property name.
     *
     * @return property name
     */
    public String getName() {
        return name;
    }

    /**
     * Getter for the property value class type.
     *
     * @return value class type
     */
    public Class<P> getType() {
        return type;
    }

    /**
     * Getter for the initial value if given.
     *
     * @return initial value or {@code null} if none given
     */
    public P getDefaultValue() {
        return defaultValue;
    }

    /**
     * Checks if the property is a read-only value.
     *
     * @return is read-only
     */
    public boolean isReadOnly() {
        return readOnly;
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, type);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof PropertyData) {
            PropertyData other = (PropertyData) obj;
            return name.equals(other.name) && type.equals(other.type);
        }
        return false;
    }
}
