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
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.OpenOption;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.io.FileUtils;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import static com.vaadin.flow.server.frontend.FrontendUtils.WEBPACK_PREFIX_ALIAS;

public class NodeUpdateImportsTest extends NodeUpdateTestUtil {

    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    private File importsFile;
    private File nodeModulesPath;
    private File frontendDirectory;
    private File generatedFrontendDirectory;
    private TaskUpdateImports node;

    @Before
    public void setup() throws Exception {

        File tmpRoot = temporaryFolder.getRoot();
        importsFile = new File(tmpRoot, "flow-imports.js");
        nodeModulesPath = new File(tmpRoot, "node_modules");
        frontendDirectory = new File(tmpRoot, "frontend");
        generatedFrontendDirectory = new File(tmpRoot, "target/frontend");

        node = new TaskUpdateImports(getClassFinder(), frontendDirectory,
                generatedFrontendDirectory, importsFile, tmpRoot,
                nodeModulesPath, true);

        Assert.assertTrue(getFlowPackage().mkdirs());

        createExpectedImports(frontendDirectory, nodeModulesPath);
    }

    @Test
    public void should_ThrowException_WhenImportsDoNotExist() {
        deleteExpectedImports(frontendDirectory, nodeModulesPath);

        boolean exceptionNotThrown = true;
        try {
            node.execute();
        } catch (IllegalStateException expected) {
            exceptionNotThrown = false;
            String exceptionMessage = expected.getMessage();
            Assert.assertTrue(
                    exceptionMessage.contains(importsFile.getAbsolutePath()));

            String content = null;
            try {
                content = FileUtils.readFileToString(importsFile,
                        Charset.defaultCharset());
            } catch (IOException e) {
            }

            String innerMessage = expected.getCause().getMessage();
            Assert.assertTrue(
                    innerMessage + " is missing "
                            + nodeModulesPath.getAbsolutePath()
                            + "\n While imports file is " + content + "\n",
                    innerMessage.contains(nodeModulesPath.getAbsolutePath()));

            List<String> expectedImports = new ArrayList<>(
                    getExpectedImports());
            expectedImports.remove("@vaadin/flow-frontend/ExampleConnector.js");

            for (String expectedImport : expectedImports) {
                String normalizedImport = expectedImport.startsWith("./")
                        ? expectedImport.substring(2)
                        : expectedImport;
                Assert.assertTrue(
                        innerMessage + " is missing " + expectedImport
                                + "\n While imports file is " + content + "\n",
                        innerMessage.contains(normalizedImport));
            }
        }

        if (exceptionNotThrown) {
            Assert.fail(
                    "Expected an exception to be thrown when no imported files exist");
        }
    }

    @Test
    public void should_UpdateMainJsFile() throws Exception {
        Assert.assertFalse(importsFile.exists());

        List<String> expectedLines = new ArrayList<>(Arrays.asList(
                "const div = document.createElement('div');",
                "div.innerHTML = '<custom-style><style include=\"lumo-color lumo-typography\"></style></custom-style>';",
                "document.head.insertBefore(div.firstElementChild, document.head.firstChild);",
                "document.body.setAttribute('theme', 'dark');"));
        expectedLines.addAll(getExpectedImports());

        node.execute();

        assertContainsImports(true, expectedLines.toArray(new String[0]));

        Assert.assertTrue(getFlowPackage().exists());
        Assert.assertTrue(new File(getFlowPackage(), "ExampleConnector.js")
                .exists());
    }

    @Test
    public void shouldNot_UpdateJsFile_when_NoChanges() throws Exception {
        node.execute();
        long timestamp1 = importsFile.lastModified();

        // need to sleep because timestamp is in seconds
        sleep(1000);
        node.execute();
        long timestamp2 = importsFile.lastModified();

        Assert.assertEquals(timestamp1, timestamp2);
    }

    @Test
    public void should_ContainLumoThemeFiles() throws Exception {
        node.execute();

        assertContainsImports(true, "@vaadin/vaadin-lumo-styles/color.js",
                "@vaadin/vaadin-lumo-styles/typography.js",
                "@vaadin/vaadin-lumo-styles/sizing.js",
                "@vaadin/vaadin-lumo-styles/spacing.js",
                "@vaadin/vaadin-lumo-styles/style.js",
                "@vaadin/vaadin-lumo-styles/icons.js");
    }

    @Test
    public void should_AddImports() throws Exception {
        node.execute();
        removeImports("@vaadin/vaadin-lumo-styles/sizing.js",
                "./local-p2-template.js");
        assertContainsImports(false, "@vaadin/vaadin-lumo-styles/sizing.js",
                "./local-p2-template.js");

        node.execute();
        assertContainsImports(true, "@vaadin/vaadin-lumo-styles/sizing.js",
                "./local-p2-template.js");
    }

    @Test
    public void should_removeImports() throws Exception {
        node.execute();
        addImports("./added-import.js");
        assertContainsImports(true, "./added-import.js");

        node.execute();
        assertContainsImports(false, "./added-import.js");
    }

    @Test
    public void should_AddRemove_Imports() throws Exception {
        node.execute();

        removeImports("@vaadin/vaadin-lumo-styles/sizing.js",
                "./local-p2-template.js");
        addImports("./added-import.js");

        assertContainsImports(false, "@vaadin/vaadin-lumo-styles/sizing.js",
                "./local-p2-template.js");
        assertContainsImports(true, "./added-import.js");

        node.execute();

        assertContainsImports(true, "@vaadin/vaadin-lumo-styles/sizing.js",
                "./local-p2-template.js");
        assertContainsImports(false, "./added-import.js");
    }

    private void assertContainsImports(boolean contains, String... imports)
            throws IOException {
        String content = FileUtils.readFileToString(importsFile,
                Charset.defaultCharset());
        for (String importString : imports) {
                if (contains) {
                    Assert.assertTrue(
                        importString + " not found in:\n" + content,
                        content.contains(addWebpackPrefix(importString)));
                } else {
                    Assert.assertFalse(
                        importString + " not found in:\n" + content,
                        content.contains(addWebpackPrefix(importString)));
                }
            }

    }

    private String addWebpackPrefix(String s) {
        if (s.startsWith("./")) {
            return WEBPACK_PREFIX_ALIAS + s.substring(2);
        }
        return s;
    }

    private void removeImports(String... imports) throws IOException {
        List<String> importsList = Arrays.asList(imports);

        List<String> current = FileUtils.readLines(importsFile,
                Charset.defaultCharset());

        Set<String> removed = current.stream()
                .filter(line -> importsList.stream().map(this::addWebpackPrefix).anyMatch(line::contains))
                .collect(Collectors.toSet());

        current.removeAll(removed);

        String content = String.join("\n", current);

        replaceJsFile(content + "\n");
    }

    private void addImports(String... imports) throws IOException {
        String content = Arrays.stream(imports).map(this::addWebpackPrefix).map(s -> "import '" + s + "';")
                .collect(Collectors.joining("\n"));

        replaceJsFile(content + "\n", StandardOpenOption.APPEND);
    }

    private void replaceJsFile(String content, OpenOption... options)
            throws IOException {
        Files.write(Paths.get(importsFile.toURI()),
                content.getBytes(StandardCharsets.UTF_8), options);
    }

    File getFlowPackage() {
        return FrontendUtils.getFlowPackage(nodeModulesPath);
    }

}