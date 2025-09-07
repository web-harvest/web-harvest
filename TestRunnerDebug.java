import org.webharvest.definition.FileConfigSource;
import org.webharvest.definition.SAXConfigParser;
import org.webharvest.definition.XMLConfig;
import org.webharvest.definition.validation.SchemaComponentFactory;
import org.webharvest.definition.validation.SchemaResolver;
import org.webharvest.definition.validation.SchemaSource;

import java.io.File;
import java.util.Set;

public class TestRunnerDebug {
    public static void main(String[] args) {
        if (args.length != 1) {
            System.err.println("Usage: java TestRunnerDebug <config-file>");
            System.exit(1);
        }
        
        String configFile = args[0];
        System.out.println("Running configuration: " + configFile);
        
        try {
            // Get schema resolver and debug what schemas are loaded
            SchemaResolver schemaResolver = SchemaComponentFactory.getSchemaResolver();
            System.out.println("Schema resolver: " + schemaResolver.getClass().getName());
            
            // Try to get schema sources (this might not be accessible)
            try {
                java.lang.reflect.Field field = schemaResolver.getClass().getDeclaredField("schemaSources");
                field.setAccessible(true);
                Set<SchemaSource> schemaSources = (Set<SchemaSource>) field.get(schemaResolver);
                System.out.println("Number of schema sources: " + schemaSources.size());
                for (SchemaSource source : schemaSources) {
                    System.out.println("Schema source: " + source.getSource().getSystemId());
                }
            } catch (Exception e) {
                System.out.println("Could not access schema sources: " + e.getMessage());
            }
            
            // Force schema refresh
            System.out.println("Refreshing schema resolver...");
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


