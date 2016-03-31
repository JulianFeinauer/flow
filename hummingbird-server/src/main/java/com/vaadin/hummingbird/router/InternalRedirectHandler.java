/*
 * Copyright 2000-2016 Vaadin Ltd.
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
package com.vaadin.hummingbird.router;

import com.vaadin.ui.UI;

/**
 * Handles navigation by redirecting the user to some location in the
 * application.
 *
 * @since
 * @author Vaadin Ltd
 */
public class InternalRedirectHandler implements NavigationHandler {
    private Location target;

    /**
     * Creates a new redirect handler for the provided location.
     *
     * @param target
     *            the target of the redirect, not <code>null</code>
     */
    public InternalRedirectHandler(Location target) {
        assert target != null;
        this.target = target;
    }

    @Override
    public void handle(NavigationEvent event) {
        UI ui = event.getUI();
        Router router = event.getSource();

        ui.getPage().getHistory().replaceState(null, target.getPath());

        router.navigate(ui, target);
    }
}
