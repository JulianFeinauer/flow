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
 *
 */

package com.vaadin.flow.server.frontend;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import elemental.json.JsonObject;

import static com.vaadin.flow.server.Constants.PACKAGE_JSON;
import static com.vaadin.flow.server.frontend.FrontendUtils.FLOW_IMPORTS_FILE;
import static com.vaadin.flow.server.frontend.FrontendUtils.WEBPACK_CONFIG;
import static com.vaadin.flow.server.frontend.FrontendUtils.getBaseDir;

public class NodeUpdatePackagesTest extends NodeUpdateTestUtil {

    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    private TaskUpdatePackages packageUpdater;
    private TaskUpdateWebpack webpackUpdater;
    private TaskCreatePackageJson packageCreator;
    private File packageJson;
    private File webpackConfig;

    @Before
    public void setup() throws Exception {
        System.setProperty("user.dir", temporaryFolder.getRoot().getPath());

        File baseDir = new File(getBaseDir());

        NodeUpdateTestUtil.createStubNode(true, true);

        packageCreator = new TaskCreatePackageJson(baseDir,
                new File(baseDir, "node_modules"));

        packageUpdater = new TaskUpdatePackages(getClassFinder(), baseDir,
                new File(baseDir, "node_modules"), true);



        webpackUpdater = new TaskUpdateWebpack(baseDir, baseDir, WEBPACK_CONFIG,
                new File(baseDir, FLOW_IMPORTS_FILE));

        packageJson = new File(baseDir, PACKAGE_JSON);
        webpackConfig = new File(baseDir, WEBPACK_CONFIG);
    }

    @Test
    public void should_CreatePackageJson() throws Exception {
        Assert.assertFalse(packageJson.exists());
        packageCreator.execute();
        Assert.assertTrue(packageJson.exists());
    }

    @Test
    public void should_CreateWebpackConfig() throws Exception {
        Assert.assertFalse(webpackConfig.exists());
        webpackUpdater.execute();
        assertWebpackConfigContent();
    }

    @Test
    public void should_not_ModifyPackageJson_WhenAlreadyExists() throws Exception {
        packageCreator.execute();
        Assert.assertTrue(packageCreator.modified);

        packageCreator.execute();
        Assert.assertFalse(packageCreator.modified);
    }


    @Test
    public void should_AddNewDependencies() throws Exception {
        packageCreator.execute();
        packageUpdater.execute();
        Assert.assertTrue(packageCreator.modified);
        Assert.assertTrue(packageUpdater.modified);
        assertPackageJsonContent();
    }

    private void assertPackageJsonContent() throws IOException {
        JsonObject packageJsonObject = packageUpdater.getPackageJson();

        JsonObject dependencies = packageJsonObject.getObject("dependencies");

        Assert.assertTrue("Missing @vaadin/vaadin-button package",
                dependencies.hasKey("@vaadin/vaadin-button"));
        Assert.assertTrue("Missing @webcomponents/webcomponentsjs package",
                dependencies.hasKey("@webcomponents/webcomponentsjs"));
        Assert.assertTrue("Missing @polymer/iron-icon package",
                dependencies.hasKey("@polymer/iron-icon"));

        JsonObject devDependencies = packageJsonObject
                .getObject("devDependencies");

        Assert.assertTrue("Missing webpack dev package",
                devDependencies.hasKey("webpack"));
        Assert.assertTrue("Missing webpack-cli dev package",
                devDependencies.hasKey("webpack-cli"));
        Assert.assertTrue("Missing webpack-dev-server dev package",
                devDependencies.hasKey("webpack-dev-server"));
        Assert.assertTrue(
                "Missing webpack-babel-multi-target-plugin dev package",
                devDependencies.hasKey("webpack-babel-multi-target-plugin"));
        Assert.assertTrue("Missing copy-webpack-plugin dev package",
                devDependencies.hasKey("copy-webpack-plugin"));
    }

    private void assertWebpackConfigContent() throws IOException {
        List<String> webpackContents = Files.lines(webpackConfig.toPath()).collect(Collectors.toList());

        Assert.assertFalse(
                "webpack config should not contain Windows path separators",
                webpackContents.contains("\\\\"));

        verifyNoAbsolutePathsPresent(webpackContents);
    }

    private void verifyNoAbsolutePathsPresent(List<String> webpackContents) {
        List<String> wrongLines = webpackContents.stream()
            // check the lines with slashes only
            .filter(line -> line.contains("/"))
            // trim the whitespaces
            .map(line -> line.replaceAll("\\s", ""))
            // check the equals ( a=something ) and object declarations ( {a: something} )
            .map(line -> {
                    int equalsSignPosition = line.indexOf("=");
                    int jsonPropertySignPosition = line.indexOf(":");
                    if (equalsSignPosition > 0) {
                        return line.substring(equalsSignPosition + 1);
                    } else if (jsonPropertySignPosition > 0) {
                        return line.substring(jsonPropertySignPosition + 1);
                    } else {
                        return null;
                    }
                })
            .filter(Objects::nonNull)
            // take the lines with strings only and trim the string start
            .filter(line -> line.startsWith("'") || line.startsWith("\"") || line.startsWith("`"))
            .map(line -> line.substring(1))
            .filter(line -> line.startsWith("/"))
            .collect(Collectors.toList());

        Assert.assertTrue(String.format(
                "Expected to have no lines that have a string starting with a slash in assignment. Incorrect lines: '%s'",
                wrongLines),
                wrongLines.isEmpty());
    }
}