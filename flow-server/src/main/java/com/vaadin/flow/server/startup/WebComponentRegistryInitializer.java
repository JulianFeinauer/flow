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
package com.vaadin.flow.server.startup;

import javax.servlet.ServletContainerInitializer;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.annotation.HandlesTypes;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.WebComponentExporter;
import com.vaadin.flow.internal.CustomElementNameValidator;
import com.vaadin.flow.server.InvalidCustomElementNameException;
import com.vaadin.flow.server.webcomponent.WebComponentBuilder;
import com.vaadin.flow.server.webcomponent.WebComponentBuilderRegistry;

/**
 *
 * Servlet initializer for collecting all classes that extend
 * {@link WebComponentExporter} on startup.
 *
 * @author Vaadin Ltd.
 * @since
 */
@HandlesTypes({ WebComponentExporter.class })
public class WebComponentRegistryInitializer
        implements ServletContainerInitializer {

    @Override
    public void onStartup(Set<Class<?>> set, ServletContext servletContext)
            throws ServletException {
        WebComponentBuilderRegistry instance = WebComponentBuilderRegistry
                .getInstance(servletContext);
        if (set == null || set.isEmpty()) {
            instance.setBuilders(Collections.emptySet());
            return;
        }
        Set<Class<?>> exporterClasses = set.stream()
                .filter(WebComponentExporter.class::isAssignableFrom)
                .filter(aClass -> !aClass.isInterface())
                .collect(Collectors.toSet());
        Set<WebComponentExporter<? extends Component>> exporters;

        exporters = constructExporters(exporterClasses);
        validateDistinct(exporters);
        validateComponentName(exporters);

        Set<WebComponentBuilder<? extends Component>> builders =
                constructBuilders(exporters);

        instance.setBuilders(builders);
    }

    private Set<WebComponentBuilder<? extends Component>> constructBuilders(Set<WebComponentExporter<? extends Component>> exporters) {
        return exporters.stream()
                .map(exporter -> {
                    try {
                        return new WebComponentBuilder<>(exporter);
                    } catch (IllegalArgumentException e) {
                        throw new IllegalArgumentException(String.format(
                                "Invalid exporter '%s': %s",
                                exporter.getClass().getCanonicalName(),
                                e.getMessage()));
                    }
                }).collect(Collectors.toSet());
    }

    private Set<WebComponentExporter<? extends Component>> constructExporters(
            Set<Class<?>> exporterClasses) {

        Set<WebComponentExporter<? extends Component>> set = new HashSet<>();
        WebComponentExporter<? extends Component> webComponentExporter;

        for (Class<?> exporterClass : exporterClasses) {
            try {
                webComponentExporter =
                        (WebComponentExporter<? extends Component>)
                                exporterClass.newInstance();
            } catch (InstantiationException | IllegalAccessException e) {
                throw new RuntimeException(String.format("Could not construct %s " +
                        "from %s!", WebComponentExporter.class.getName(),
                        exporterClass.getCanonicalName()), e);
            }
            set.add(webComponentExporter);
        }

        return set;
    }

    /**
     * Validate that all web component names are valid custom element names.
     *
     * @param exporterSet
     *         set of web components to validate
     */
    protected void validateComponentName(
            Set<WebComponentExporter<? extends Component>> exporterSet) {
        for (WebComponentExporter<? extends Component> exporter : exporterSet) {
            String tag = exporter.tag();
            if (!CustomElementNameValidator.isCustomElementName(tag)) {
                String msg = String
                        .format("Tag name '%s' given by '%s' is not a valid " +
                                        "custom element name.",
                                tag, exporter.getClass().getCanonicalName());
                throw new InvalidCustomElementNameException(msg);
            }
        }
    }

    /**
     * Validate that we have exactly one web component exporter per tag name.
     *
     * @param exporterSet
     *         set of web components to validate
     */
    protected void validateDistinct(
            Set<WebComponentExporter<? extends Component>> exporterSet) {
        long count = exporterSet.stream().map(WebComponentExporter::tag)
                .distinct().count();
        if (exporterSet.size() != count) {
            Map<String, Class<?>> items = new HashMap<>();
            for (WebComponentExporter<? extends Component> exporter : exporterSet) {
                String tag = exporter.tag();
                if (items.containsKey(tag)) {
                    String message = String.format(
                            "Found two %s classes '%s' and '%s' for the tag " +
                                    "name '%s'. Tag must be unique.",
                            WebComponentExporter.class.getSimpleName(),
                            items.get(tag).getCanonicalName(),
                            exporter.getClass().getCanonicalName(),
                            tag);
                    throw new IllegalArgumentException(message);
                }
                items.put(tag, exporter.getClass());
            }
        }
    }
}
