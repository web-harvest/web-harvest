package org.webharvest.runtime;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertTrue;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import org.webharvest.definition.Config;
import org.webharvest.definition.ConfigSource;
import org.webharvest.definition.CorePluginRegistry;
import org.webharvest.definition.PluginConfigurationTree;
import org.webharvest.plugin.DefaultPluginConfiguration;
import org.webharvest.plugin.PluginConfiguration;
import org.webharvest.plugin.PluginConfigurationSchema;
import org.webharvest.plugin.PluginMetadata;
import org.webharvest.plugin.annotations.CorePlugin;
import org.webharvest.plugin.exceptions.PluginValidationException;
import org.webharvest.plugin.AbstractPlugin;
import org.webharvest.runtime.scripting.ScriptingLanguage;
import org.webharvest.runtime.variables.EmptyVariable;
import org.webharvest.runtime.variables.NodeVariable;
import org.webharvest.runtime.variables.Variable;

import com.google.common.eventbus.EventBus;

public class ScraperRootExecutionTest {

    private Scraper scraper;

    @BeforeMethod
    public void setUp() {
        CorePluginRegistry.getInstance().clear();
        CorePluginRegistry.getInstance().registerMapping(null, TestChildPlugin.class, "test-child");
        scraper = new Scraper(new EventBus());
    }

    @AfterMethod
    public void tearDown() {
        CorePluginRegistry.getInstance().clear();
        TestChildPlugin.EXECUTION_COUNT.set(0);
    }

    @Test
    public void rootConfigurationSetsRuntimeAndExecutesChildren() {
        DefaultPluginConfiguration rootConfig = new DefaultPluginConfiguration(
            "config", "http://org.webharvest/schema/2.1/core", "config",
            Map.of("charset", "ISO-8859-1", "scriptlang", "groovy"));

        DefaultPluginConfiguration childConfig = new DefaultPluginConfiguration(
            "test-child", "urn:test", "test-child");
        childConfig.setChildren(Collections.emptyList());

        PluginConfigurationTree childTree = new PluginConfigurationTree(childConfig, "test-child", TestChildPlugin.class);
        PluginConfigurationTree rootTree = new PluginConfigurationTree(rootConfig, "config", null);
        rootTree.addChild(childTree);
        rootConfig.setChildren(List.of(childConfig));

        ScraperContext context = new ScraperContext();
        context.setConfig(new StubConfig(rootTree));
        context.setEventBus(new EventBus());

        scraper.execute(context);

        assertEquals(TestChildPlugin.EXECUTION_COUNT.get(), 1, "Child plugin should execute once");
        assertEquals(context.getCharset(), "ISO-8859-1", "Charset should be applied from root configuration");
        assertEquals(context.getScriptingLanguage(), ScriptingLanguage.GROOVY,
            "Scripting language should be applied from root configuration");
        assertTrue(context.containsVar("configResult"), "Root execution should expose configResult variable");
        assertNotNull(context.getVar("testExecuted"), "Child plugin should set variable in context");
    }

    private static final class StubConfig implements Config {

        private final PluginConfigurationTree tree;

        private StubConfig(PluginConfigurationTree tree) {
            this.tree = tree;
        }

        @Override
        public Version getVersion() {
            return Version.v2;
        }

        @Override
        public ConfigSource getConfigSource() {
            return null;
        }

        @Override
        public PluginConfigurationTree getConfigurationTree() {
            return tree;
        }

        @Override
        public void reload() {
            // no-op for tests
        }
    }

    @CorePlugin(elementName = "test-child", description = "Test child plugin", version = "1.0")
    public static class TestChildPlugin extends AbstractPlugin {

        private static final AtomicInteger EXECUTION_COUNT = new AtomicInteger();

        private static final PluginMetadata METADATA = new PluginMetadata() {
            @Override
            public String getId() {
                return "test-child";
            }

            @Override
            public String getName() {
                return "Test Child";
            }

            @Override
            public String getVersion() {
                return "1.0";
            }

            @Override
            public String getDescription() {
                return "Test child plugin";
            }

            @Override
            public String getAuthor() {
                return "Test";
            }

            @Override
            public String getCategory() {
                return "test";
            }

            @Override
            public String[] getTags() {
                return new String[] {"test"};
            }

            @Override
            public String[] getDependencies() {
                return new String[0];
            }

            @Override
            public String getClassName() {
                return TestChildPlugin.class.getName();
            }

            @Override
            public boolean isCorePlugin() {
                return true;
            }

            @Override
            public String getElementName() {
                return "test-child";
            }

            @Override
            public String getNamespace() {
                return "urn:test";
            }

            @Override
            public PluginConfigurationSchema getConfigurationSchema() {
                return new PluginConfigurationSchema() {
                    @Override
                    public AttributeDefinition[] getAttributeDefinitions() {
                        return new AttributeDefinition[0];
                    }

                    @Override
                    public AttributeDefinition getAttribute(String name) {
                        return null;
                    }

                    @Override
                    public String[] getRequiredAttributes() {
                        return new String[0];
                    }

                    @Override
                    public String[] getOptionalAttributes() {
                        return new String[0];
                    }

                    @Override
                    public boolean allowsBody() {
                        return false;
                    }

                    @Override
                    public String[] getRequiredSubElements() {
                        return new String[0];
                    }

                    @Override
                    public String[] getAllowedSubElements() {
                        return new String[0];
                    }

                    @Override
                    public String[] getRequiredParents() {
                        return new String[0];
                    }

                    @Override
                    public String getVersion() {
                        return "1.0";
                    }

                    @Override
                    public void validate(PluginConfiguration configuration) throws PluginValidationException {
                        // nothing to validate
                    }
                };
            }
        };

        @Override
        public PluginMetadata getMetadata() {
            return METADATA;
        }

        @Override
        protected void doInitialize(PluginConfiguration configuration) {
            // no initialization required
        }

        @Override
        protected void doValidateConfiguration(PluginConfiguration configuration) {
            // no validation required
        }

        @Override
        protected Variable doExecute(DynamicScopeContext context) {
            EXECUTION_COUNT.incrementAndGet();
            context.setLocalVar("testExecuted", new NodeVariable("done"));
            return EmptyVariable.INSTANCE;
        }
    }
}

