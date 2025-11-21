package org.webharvest.server.examples;

import org.testng.Assert;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;
import org.webharvest.server.validation.SchemaValidator;
import org.webharvest.server.validation.ValidationResult;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Ensures bundled XML examples are cleanly formatted (no BOM or stray characters before &lt;?xml...&gt;).
 * This guards against the "Content is not allowed in prolog" parser failure seen during validation.
 */
public class ExampleValidationTest {

    private static final Path EXAMPLES_DIR = Paths.get("src/main/resources/webapp/examples");
    private final SchemaValidator schemaValidator = new SchemaValidator();

    @DataProvider(name = "exampleFiles")
    public Object[][] exampleFiles() throws IOException {
        try {
            List<Path> files = Files.list(EXAMPLES_DIR)
                    .filter(path -> Files.isRegularFile(path) && path.toString().endsWith(".xml"))
                    .collect(Collectors.toList());

            Object[][] data = new Object[files.size()][1];
            for (int i = 0; i < files.size(); i++) {
                data[i][0] = files.get(i).getFileName().toString();
            }
            return data;
        } catch (IOException ex) {
            throw new IOException("Failed to enumerate example XML files in " + EXAMPLES_DIR.toAbsolutePath(), ex);
        }
    }

    @Test(dataProvider = "exampleFiles")
    public void examplesShouldStartWithXmlDeclaration(String filename) throws Exception {
        Path file = EXAMPLES_DIR.resolve(filename);
        byte[] bytes = Files.readAllBytes(file);
        Assert.assertTrue(bytes.length > 0, filename + " should not be empty");
        Assert.assertEquals(bytes[0], (byte) '<', filename + " contains data before '<'");

        String xmlContent = new String(bytes, StandardCharsets.UTF_8);
        Assert.assertTrue(xmlContent.startsWith("<?xml"),
                "Example " + filename + " must start with XML declaration");
    }

    @Test(dataProvider = "exampleFiles")
    public void examplesShouldNotTriggerXmlParseErrors(String filename) throws Exception {
        Path file = EXAMPLES_DIR.resolve(filename);
        String xmlContent = Files.readString(file, StandardCharsets.UTF_8);
        ValidationResult result = schemaValidator.validate(xmlContent);
        boolean hasXmlParseError = result.getErrors().stream()
                .anyMatch(error -> "XML_PARSE".equals(error.getType()));
        if (hasXmlParseError) {
            Assert.fail("XML parse error detected for " + filename + ": " + result.getErrors());
        }
    }
}

