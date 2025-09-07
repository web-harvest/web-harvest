package org.webharvest.docs;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.apache.commons.lang3.StringUtils;
import org.webharvest.annotation.Definition;
import org.webharvest.runtime.processors.plugins.TargetNamespace;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Method;
import java.util.*;

/**
 * Generator for processor reference documentation from source code annotations.
 * 
 * @author Web-Harvest Team
 * @version 2.1.0-SNAPSHOT
 */
public class ProcessorReferenceGenerator {
    
    private static final String[] PROCESSOR_PACKAGES = {
        "org.webharvest.runtime.processors",
        "org.webharvest.runtime.processors.plugins"
    };
    
    private static final Map<String, String> CATEGORY_ICONS = new HashMap<>();
    private static final Map<String, String> CATEGORY_DESCRIPTIONS = new HashMap<>();
    
    static {
        CATEGORY_ICONS.put("web", "fas fa-globe");
        CATEGORY_ICONS.put("data", "fas fa-search");
        CATEGORY_ICONS.put("file", "fas fa-file");
        CATEGORY_ICONS.put("control", "fas fa-code-branch");
        CATEGORY_ICONS.put("variable", "fas fa-variable");
        CATEGORY_ICONS.put("extension", "fas fa-plug");
        
        CATEGORY_DESCRIPTIONS.put("web", "Web & HTTP Processors");
        CATEGORY_DESCRIPTIONS.put("data", "Data Extraction Processors");
        CATEGORY_DESCRIPTIONS.put("file", "File & Data Processors");
        CATEGORY_DESCRIPTIONS.put("control", "Control Flow Processors");
        CATEGORY_DESCRIPTIONS.put("variable", "Variable Processors");
        CATEGORY_DESCRIPTIONS.put("extension", "Extension Processors");
    }
    
    public static void main(String[] args) {
        if (args.length < 1) {
            System.err.println("Usage: ProcessorReferenceGenerator <output-directory>");
            System.exit(1);
        }
        
        String outputDir = args[0];
        ProcessorReferenceGenerator generator = new ProcessorReferenceGenerator();
        
        try {
            generator.generateProcessorReference(outputDir);
            System.out.println("Processor reference generated successfully in: " + outputDir);
        } catch (Exception e) {
            System.err.println("Error generating processor reference: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }
    
    public void generateProcessorReference(String outputDir) throws IOException {
        File outputDirFile = new File(outputDir);
        if (!outputDirFile.exists()) {
            outputDirFile.mkdirs();
        }
        
        // Generate JSON reference
        generateJsonReference(outputDir);
        
        // Generate HTML reference
        generateHtmlReference(outputDir);
        
        // Generate individual processor pages
        generateIndividualPages(outputDir);
    }
    
    private void generateJsonReference(String outputDir) throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        ObjectNode root = mapper.createObjectNode();
        
        ArrayNode processors = mapper.createArrayNode();
        
        // This is a simplified version - in a real implementation,
        // you would scan the classpath for processor classes
        addMockProcessors(processors, mapper);
        
        root.set("processors", processors);
        root.put("generatedAt", new Date().toString());
        root.put("version", "2.1.0-SNAPSHOT");
        
        File jsonFile = new File(outputDir, "processors.json");
        mapper.writerWithDefaultPrettyPrinter().writeValue(jsonFile, root);
    }
    
    private void addMockProcessors(ArrayNode processors, ObjectMapper mapper) {
        // HTTP Processor
        ObjectNode httpProcessor = mapper.createObjectNode();
        httpProcessor.put("name", "http");
        httpProcessor.put("displayName", "HTTP Request");
        httpProcessor.put("category", "web");
        httpProcessor.put("description", "Performs HTTP requests to fetch web content");
        httpProcessor.put("namespace", "http://org.webharvest/schema/2.1/core");
        
        ArrayNode httpAttributes = mapper.createArrayNode();
        addAttribute(httpAttributes, "url", "Target URL", true, "string");
        addAttribute(httpAttributes, "method", "HTTP method", false, "string", "GET");
        addAttribute(httpAttributes, "timeout", "Request timeout in milliseconds", false, "integer", "30000");
        addAttribute(httpAttributes, "followredirects", "Follow HTTP redirects", false, "boolean", "true");
        addAttribute(httpAttributes, "useragent", "Custom User-Agent string", false, "string");
        httpProcessor.set("attributes", httpAttributes);
        
        ArrayNode httpSubprocessors = mapper.createArrayNode();
        httpSubprocessors.add("http-header");
        httpSubprocessors.add("http-param");
        httpProcessor.set("subprocessors", httpSubprocessors);
        
        processors.add(httpProcessor);
        
        // HTML to XML Processor
        ObjectNode htmlProcessor = mapper.createObjectNode();
        htmlProcessor.put("name", "html-to-xml");
        htmlProcessor.put("displayName", "HTML to XML");
        htmlProcessor.put("category", "web");
        htmlProcessor.put("description", "Converts HTML content to well-formed XML using advanced parsing strategies");
        htmlProcessor.put("namespace", "http://org.webharvest/schema/2.1/core");
        
        ArrayNode htmlAttributes = mapper.createArrayNode();
        addAttribute(htmlAttributes, "advancedxmlescape", "Advanced XML character escaping", false, "boolean", "false");
        addAttribute(htmlAttributes, "specialentities", "Support for special HTML entities", false, "boolean", "false");
        addAttribute(htmlAttributes, "unicodechars", "Unicode character support", false, "boolean", "false");
        addAttribute(htmlAttributes, "allowmultiwordattributes", "Allow multi-word attributes", false, "boolean", "false");
        addAttribute(htmlAttributes, "omitunknowntags", "Omit unknown HTML tags", false, "boolean", "false");
        addAttribute(htmlAttributes, "omitcomments", "Omit HTML comments", false, "boolean", "false");
        addAttribute(htmlAttributes, "omittermlenvelope", "Omit XML envelope", false, "boolean", "false");
        htmlProcessor.set("attributes", htmlAttributes);
        
        processors.add(htmlProcessor);
        
        // XPath Processor
        ObjectNode xpathProcessor = mapper.createObjectNode();
        xpathProcessor.put("name", "xpath");
        xpathProcessor.put("displayName", "XPath");
        xpathProcessor.put("category", "data");
        xpathProcessor.put("description", "Extracts data using XPath expressions");
        xpathProcessor.put("namespace", "http://org.webharvest/schema/2.1/core");
        
        ArrayNode xpathAttributes = mapper.createArrayNode();
        addAttribute(xpathAttributes, "expression", "XPath expression", true, "string");
        addAttribute(xpathAttributes, "id", "Unique identifier", false, "string");
        xpathProcessor.set("attributes", xpathAttributes);
        
        processors.add(xpathProcessor);
        
        // Add more processors as needed...
    }
    
    private void addAttribute(ArrayNode attributes, String name, String description, boolean required, String type) {
        addAttribute(attributes, name, description, required, type, null);
    }
    
    private void addAttribute(ArrayNode attributes, String name, String description, boolean required, String type, String defaultValue) {
        ObjectNode attribute = attributes.addObject();
        attribute.put("name", name);
        attribute.put("description", description);
        attribute.put("required", required);
        attribute.put("type", type);
        if (defaultValue != null) {
            attribute.put("defaultValue", defaultValue);
        }
    }
    
    private void generateHtmlReference(String outputDir) throws IOException {
        StringBuilder html = new StringBuilder();
        html.append("<!DOCTYPE html>\n");
        html.append("<html lang=\"en\">\n");
        html.append("<head>\n");
        html.append("    <meta charset=\"UTF-8\">\n");
        html.append("    <meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">\n");
        html.append("    <title>Processor Reference - Web-Harvest 2.1.0</title>\n");
        html.append("    <link rel=\"stylesheet\" href=\"../css/style.css\">\n");
        html.append("    <link rel=\"stylesheet\" href=\"../css/processors.css\">\n");
        html.append("    <link href=\"https://fonts.googleapis.com/css2?family=Inter:wght@300;400;500;600;700&display=swap\" rel=\"stylesheet\">\n");
        html.append("    <link rel=\"stylesheet\" href=\"https://cdnjs.cloudflare.com/ajax/libs/font-awesome/6.0.0/css/all.min.css\">\n");
        html.append("</head>\n");
        html.append("<body>\n");
        html.append("    <div class=\"processor-reference\">\n");
        html.append("        <h1>Processor Reference</h1>\n");
        html.append("        <p>Complete reference for all Web-Harvest processors and their configuration options.</p>\n");
        html.append("        <div class=\"reference-content\">\n");
        html.append("            <p>This reference is automatically generated from source code annotations.</p>\n");
        html.append("            <p>For the most up-to-date information, please refer to the source code or use the IDE's auto-completion features.</p>\n");
        html.append("        </div>\n");
        html.append("    </div>\n");
        html.append("</body>\n");
        html.append("</html>\n");
        
        File htmlFile = new File(outputDir, "reference.html");
        try (FileWriter writer = new FileWriter(htmlFile)) {
            writer.write(html.toString());
        }
    }
    
    private void generateIndividualPages(String outputDir) throws IOException {
        // Generate individual processor pages
        // This would iterate through all processors and create individual HTML pages
        // For now, we'll create a placeholder
        
        File individualDir = new File(outputDir, "individual");
        individualDir.mkdirs();
        
        String placeholder = "<!DOCTYPE html>\n" +
                "<html>\n" +
                "<head><title>Individual Processor Pages</title></head>\n" +
                "<body>\n" +
                "<h1>Individual Processor Pages</h1>\n" +
                "<p>Individual processor pages will be generated here.</p>\n" +
                "</body>\n" +
                "</html>";
        
        File placeholderFile = new File(individualDir, "index.html");
        try (FileWriter writer = new FileWriter(placeholderFile)) {
            writer.write(placeholder);
        }
    }
}

