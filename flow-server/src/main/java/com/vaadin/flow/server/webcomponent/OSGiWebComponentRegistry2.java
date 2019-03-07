/*
 * Copyright 2000-2019 Vaadin Ltd.
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

import java.util.Set;

import com.vaadin.flow.component.Component;

/**
 * Data collector component for collecting web components in an OSGi
 * environment.
 */
public class OSGiWebComponentRegistry2 extends WebComponentRegistry2 {

    @Override
    public boolean setWebComponentBuilders(
            Set<WebComponentBuilder<? extends Component>> components) {

        updateRegistry(components);
        return true;
    }
}
