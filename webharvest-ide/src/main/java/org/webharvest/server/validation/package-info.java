/**
 * Server-side configuration validation for WebHarvest IDE.
 * 
 * <p>This package provides schema-based validation for XML configurations,
 * checking attributes, plugin existence, and context-specific requirements
 * before configuration execution.</p>
 * 
 * <h2>Main Components</h2>
 * <ul>
 *   <li>{@link org.webharvest.server.validation.SchemaValidator} - Core validator that checks XML against plugin schemas</li>
 *   <li>{@link org.webharvest.server.validation.ValidationResult} - Validation outcome wrapper</li>
 *   <li>{@link org.webharvest.server.validation.ValidationError} - Individual validation error details</li>
 * </ul>
 * 
 * <h2>Validation Features</h2>
 * <ul>
 *   <li><strong>Attribute Validation:</strong> Checks unknown, required, and optional attributes</li>
 *   <li><strong>Plugin Existence:</strong> Verifies all plugins are registered</li>
 *   <li><strong>Context Validation:</strong> Ensures context-specific plugins (e.g., http-header) appear in correct parents</li>
 *   <li><strong>Schema Access:</strong> Retrieves schemas via plugin metadata</li>
 * </ul>
 * 
 * <h2>Usage Example</h2>
 * <pre>{@code
 * SchemaValidator validator = new SchemaValidator();
 * ValidationResult result = validator.validate(xmlContent);
 * 
 * if (!result.isValid()) {
 *     for (ValidationError error : result.getErrors()) {
 *         System.err.println(error.getMessage());
 *     }
 * }
 * }</pre>
 * 
 * <h2>API Integration</h2>
 * <p>Validation is exposed via REST API endpoint:</p>
 * <pre>POST /api/config/validate</pre>
 * 
 * <p>Returns JSON with validation status and detailed error list.</p>
 * 
 * @since 2.3.1
 * @see org.webharvest.server.api.ConfigServlet
 * @see org.webharvest.plugin.PluginConfigurationSchema
 */
package org.webharvest.server.validation;






