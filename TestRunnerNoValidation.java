import org.webharvest.definition.FileConfigSource;
import org.webharvest.definition.SAXConfigParser;
import org.webharvest.definition.XMLConfig;
import org.webharvest.utils.XmlUtil;

import java.io.File;

public class TestRunnerNoValidation {
    public static void main(String[] args) {
        if (args.length != 1) {
            System.err.println("Usage: java TestRunnerNoValidation <config-file>");
            System.exit(1);
        }
        
        String configFile = args[0];
        System.out.println("Running configuration: " + configFile);
        
        try {
            // Create configuration without schema validation
            FileConfigSource configSource = new FileConfigSource(new File(configFile));
            
            // Create a custom SAX parser without schema validation
            javax.xml.parsers.SAXParserFactory factory = javax.xml.parsers.SAXParserFactory.newInstance();
            factory.setValidating(false);
            factory.setNamespaceAware(true);
            
            // Parse the XML manually without schema validation
            org.xml.sax.helpers.DefaultHandler handler = new org.xml.sax.helpers.DefaultHandler() {
                @Override
                public void startElement(String uri, String localName, String qName, org.xml.sax.Attributes attributes) {
                    System.out.println("Element: " + qName + " (uri: " + uri + ", localName: " + localName + ")");
                }
            };
            
            factory.newSAXParser().parse(new File(configFile), handler);
            
            System.out.println("XML parsed successfully without schema validation!");
            
        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }
}

