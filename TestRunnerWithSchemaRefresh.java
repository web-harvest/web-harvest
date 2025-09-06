import org.webharvest.definition.FileConfigSource;
import org.webharvest.definition.SAXConfigParser;
import org.webharvest.definition.XMLConfig;
import org.webharvest.definition.validation.SchemaComponentFactory;
import org.webharvest.definition.validation.SchemaResolver;

import java.io.File;

public class TestRunnerWithSchemaRefresh {
    public static void main(String[] args) {
        if (args.length != 1) {
            System.err.println("Usage: java TestRunnerWithSchemaRefresh <config-file>");
            System.exit(1);
        }
        
        String configFile = args[0];
        System.out.println("Running configuration: " + configFile);
        
        try {
            // Force schema refresh before parsing
            System.out.println("Refreshing schema resolver...");
            SchemaResolver schemaResolver = SchemaComponentFactory.getSchemaResolver();
            schemaResolver.refresh();
            
            // Create configuration
            FileConfigSource configSource = new FileConfigSource(new File(configFile));
            SAXConfigParser parser = new SAXConfigParser();
            XMLConfig config = new XMLConfig(configSource, parser);
            
            // Load configuration
            config.reload();
            
            System.out.println("Configuration loaded successfully!");
            System.out.println("Version: " + config.getVersion());
            
        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }
}
