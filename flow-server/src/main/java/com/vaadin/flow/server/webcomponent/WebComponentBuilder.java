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

import java.security.InvalidParameterException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.WebComponentExporter;
import com.vaadin.flow.component.webcomponent.InstanceConfigurator;
import com.vaadin.flow.component.webcomponent.PropertyConfiguration;
import com.vaadin.flow.component.webcomponent.WebComponentBinding;
import com.vaadin.flow.component.webcomponent.WebComponentConfiguration;
import com.vaadin.flow.component.webcomponent.WebComponentDefinition;
import com.vaadin.flow.di.Instantiator;
import com.vaadin.flow.internal.ReflectTools;

import elemental.json.JsonValue;

public class WebComponentBuilder<C extends Component>
        implements WebComponentDefinition<C>, WebComponentConfiguration<C> {
    // TODO: get rid of these and json (de)serialize on the spot
    private static final List<Class> SUPPORTED_TYPES = Arrays.asList(
            Boolean.class, String.class, Integer.class, Double.class,
            JsonValue.class);

    private Class<C> componentClass = null;
    private WebComponentExporter<C> exporter;
    private InstanceConfigurator<C> instanceConfigurator;
    private Map<String, PropertyConfigurationImp<C, ?>> propertyConfigurationMap =
            new HashMap<>();

    public WebComponentBuilder(WebComponentExporter<C> exporter) {
        if (exporter.tag() == null) {
            throw new InvalidParameterException(String.format(
                    "WebComponentExporter %s gave a null tag. Please provide " +
                            "a non-null tag for the web component!",
                    exporter.getClass().getCanonicalName()));
        }

        this.exporter = exporter;
        this.exporter.define(this);
    }

    @Override
    public <P> PropertyConfiguration<C, P> addProperty(
            String name, Class<P> type, P defaultValue) {
        Objects.requireNonNull(name, "Parameter 'name' cannot be null!");
        Objects.requireNonNull(type, "Parameter 'type' cannot be null!");

        if (!isSupportedType(type)) {
            throw new UnsupportedPropertyType(String.format("PropertyConfiguration " +
                    "cannot handle type %s. Use any of %s instead.",
                    type.getCanonicalName(),
                    SUPPORTED_TYPES.stream().map(Class::getSimpleName)
                            .collect(Collectors.joining(", "))));
        }

        PropertyConfigurationImp<C, P> configurationImp =
                new PropertyConfigurationImp<>(getComponentClass(), name,
                        type, defaultValue);

        propertyConfigurationMap.put(name, configurationImp);

        return configurationImp;
    }

    @Override
    public void setInstanceConfigurator(InstanceConfigurator<C> configurator) {
        this.instanceConfigurator = configurator;
    }

    @Override
    public <P> PropertyConfiguration<C, List<P>> addListProperty(String name, Class<P> entryClass) {
        return null;
    }

    @Override
    public <P> PropertyConfiguration<C, List<P>> addListProperty(String name, List<P> defaultValue) {
        return null;
    }

    public String getWebComponentTag() {
        return exporter.tag();
    }

    @Override
    public Class<?> getPropertyType(String propertyName) {
        if (propertyConfigurationMap.containsKey(propertyName)) {
            return propertyConfigurationMap.get(propertyName).getPropertyData().getType();
        } else {
            // TODO: or should we throw here?
            return null;
        }
    }

    @Override
    public Set<PropertyData2<?>> getPropertyDataSet() {
        return propertyConfigurationMap.values().stream()
                .map(PropertyConfigurationImp::getPropertyData)
                .collect(Collectors.toSet());
    }

    @Override
    public WebComponentBinding<C> createBinding(Instantiator instantiator) {
        Objects.requireNonNull(instantiator, "Parameter 'instantiator' must not" +
                " be null!");

        C componentReference = instantiator.getOrCreate(this.getComponentClass());

        // TODO: real IWebComponent impl
        if (instanceConfigurator != null) {
            instanceConfigurator.accept(new DummyWebComponentInterfacer<>(),
                    componentReference);
        }

        Set<PropertyBinding<?>> propertyBindings =
                propertyConfigurationMap.values().stream()
                        .map(pc -> new PropertyBinding<>(
                                pc.getPropertyData(),
                                v -> pc.getOnChangeHandler()
                                        .accept(componentReference, v)))
                        .collect(Collectors.toSet());

        WebComponentBindingImpl<C> binding =
                new WebComponentBindingImpl<>(componentReference,
                        propertyBindings);

        binding.updateProperties();

        return binding;
    }

    @Override
    public Class<C> getComponentClass() {
        // TODO: if this works, exporter.getComponentClass() can be removed
        if (componentClass == null) {
            componentClass = (Class<C>)ReflectTools.getGenericInterfaceType(
                    exporter.getClass(), WebComponentExporter.class);
        }
        return componentClass;
    }

    @Override
    public Class<WebComponentExporter<C>> getExporterClass() {
        return (Class<WebComponentExporter<C>>) exporter.getClass();
    }

    @Override
    public boolean hasProperty(String propertyName) {
        return propertyConfigurationMap.containsKey(propertyName);
    }

    private static boolean isSupportedType(Class clazz) {
        return SUPPORTED_TYPES.contains(clazz);
    }
}
