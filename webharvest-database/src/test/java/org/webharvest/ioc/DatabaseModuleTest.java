package org.webharvest.ioc;

import java.util.List;

import com.google.inject.Binding;
import com.google.inject.Key;
import com.google.inject.name.Names;
import com.google.inject.spi.Elements;

import org.testng.annotations.Test;
import org.webharvest.runtime.database.ConnectionFactory;

import static org.testng.Assert.assertTrue;

/** Verifies the public Guice bindings contributed by the optional Database module. */
public class DatabaseModuleTest {

    @Test
    public void exposesDefaultStandaloneAndNamedJndiFactories() {
        List<com.google.inject.spi.Element> elements = Elements.getElements(new DatabaseModule());
        assertTrue(hasBinding(elements, Key.get(ConnectionFactory.class)),
                "The default connection factory binding must be present");
        assertTrue(hasBinding(elements,
                Key.get(ConnectionFactory.class, Names.named("standalone"))),
                "The named standalone binding must be present");
        assertTrue(hasBinding(elements,
                Key.get(ConnectionFactory.class, Names.named("jndi"))),
                "The named JNDI binding must be present");
    }

    private boolean hasBinding(List<com.google.inject.spi.Element> elements, Key<?> key) {
        for (com.google.inject.spi.Element element : elements) {
            if (element instanceof Binding && ((Binding<?>) element).getKey().equals(key)) {
                return true;
            }
        }
        return false;
    }
}
