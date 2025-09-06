package org.webharvest.definition;

import static org.easymock.EasyMock.*;
import static org.testng.AssertJUnit.*;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;

import org.testng.annotations.*;
import org.unitils.UnitilsTestNG;
import org.unitils.easymock.EasyMockUnitils;
import org.unitils.easymock.annotation.RegularMock;
import org.webharvest.WHConstants;

public class XMLConfigTest extends UnitilsTestNG {

    private static final String BASIC_CONFIG_SKELETON = "" +
        "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n\n" +
        "<config xmlns=\"" + WHConstants.XMLNS_CORE + "\"\n" +
        "\t\txmlns:var=\"" + WHConstants.XMLNS_VAR + "\"\n" +
        "\t\txmlns:p=\"" + WHConstants.XMLNS_PARAM + "\">\n" +
        "\t\n" +
        "</config>";

    private XMLConfig config;

    private Reader mockReader;

    @BeforeMethod
    public void setUp() {
        mockReader = new StringReader(BASIC_CONFIG_SKELETON);
        // Create a mock ConfigParser for testing
        ConfigParser mockConfigParser = createMock(ConfigParser.class);
        // Create a mock ConfigSource for testing
        ConfigSource mockSource = createMock(ConfigSource.class);
        config = new XMLConfig(mockSource, mockConfigParser);
    }

    @AfterMethod
    public void tearDown() {
        config = null;
        mockReader = null;
    }

    @Test(expectedExceptions=IllegalArgumentException.class)
    public void testConstructorWithoutSource() {
        new XMLConfig(null, null);
    }

    @Test(expectedExceptions=IllegalStateException.class)
    public void testGetElementDefWithoutLoadedConfiguration() {
        config.getElementDef();
    }

    @Test
    public void testGetConfigSource() {
        // Create a mock ConfigSource for testing
        ConfigSource mockSource = createMock(ConfigSource.class);
        ConfigParser mockParser = createMock(ConfigParser.class);
        XMLConfig testConfig = new XMLConfig(mockSource, mockParser);
        assertSame(testConfig.getConfigSource(), mockSource);
    }

    @Test
    public void testLoad() throws IOException {
        // Mock the ConfigParser to return a valid element definition
        ConfigParser mockParser = createMock(ConfigParser.class);
        IElementDef mockElementDef = createMock(IElementDef.class);
        ConfigSource mockSource = createMock(ConfigSource.class);
        expect(mockParser.parse(mockSource)).andReturn(mockElementDef);
        
        // Replay the parser mock
        replay(mockParser);
        
        // Create a new config with the mocked parser
        XMLConfig testConfig = new XMLConfig(mockSource, mockParser);
        testConfig.reload();
        
        final IElementDef def = testConfig.getElementDef();
        assertNotNull(def);
        
        // Verify that the parser was called
        verify(mockParser);
    }

}
