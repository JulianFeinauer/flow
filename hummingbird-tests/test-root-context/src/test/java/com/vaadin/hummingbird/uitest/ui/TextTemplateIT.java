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
package com.vaadin.hummingbird.uitest.ui;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;

import com.vaadin.hummingbird.testutil.PhantomJSTest;

/**
 * @author Vaadin Ltd
 *
 */
public class TextTemplateIT extends PhantomJSTest {

    @Before
    public void setUp() {
        open();
    }

    @Test
    public void initialText() {
        WebElement textDiv = findElement(By.id("text"));

        Assert.assertEquals("Foo", textDiv.getText());
    }

    @Test
    public void updateText() {
        WebElement button = findElement(By.cssSelector("button"));
        button.click();

        WebElement textDiv = findElement(By.id("text"));

        Assert.assertEquals("Bar", textDiv.getText());
    }
}