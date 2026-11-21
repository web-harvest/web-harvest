package org.webharvest.server.validation;

import org.testng.Assert;
import org.testng.annotations.Test;

/**
 * Verifies root-level text validation without rejecting plugin body content.
 */
public class SchemaValidatorTextContentTest {

    private final SchemaValidator validator = new SchemaValidator();

    @Test
    public void rejectsNonWhitespaceTextDirectlyUnderConfigRoot() {
        ValidationResult result = validator.validate("<config>unexpected text</config>");

        Assert.assertTrue(
                result.getErrors().stream().anyMatch(error -> "INVALID_CONTENT".equals(error.getType())),
                "Non-whitespace text directly under config must be reported as INVALID_CONTENT");
    }

    @Test
    public void ignoresWhitespaceOnlyTextDirectlyUnderConfigRoot() {
        ValidationResult result = validator.validate("<config> \n\t </config>");

        Assert.assertFalse(
                result.getErrors().stream().anyMatch(error -> "INVALID_CONTENT".equals(error.getType())),
                "Whitespace-only text directly under config must be ignored");
    }

    @Test
    public void allowsBodyTextInsideBodyContentPlugin() {
        ValidationResult result = validator.validate(
                "<config><set-var name=\"message\">body text</set-var></config>");

        Assert.assertFalse(
                result.getErrors().stream().anyMatch(error -> "INVALID_CONTENT".equals(error.getType())),
                "Text inside a body-content plugin must not be treated as root-level text");
    }
}
