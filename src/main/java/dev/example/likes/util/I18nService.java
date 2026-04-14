package dev.example.likes.util;

import net.kyori.adventure.key.Key;
import net.kyori.adventure.translation.GlobalTranslator;
import net.kyori.adventure.translation.TranslationRegistry;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.PropertyResourceBundle;
import java.util.ResourceBundle;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Manages Adventure's {@link TranslationRegistry} for the Likes plugin.
 * <p>
 * Loads {@code .properties} files from the plugin JAR as UTF-8,
 * registers them with a {@link TranslationRegistry}, and adds the registry
 * to {@link GlobalTranslator} so that Paper/Adventure automatically resolves
 * translations per each player's client locale.
 * </p>
 */
public class I18nService {

    private static final Logger log = Logger.getLogger(I18nService.class.getName());
    private static final Key REGISTRY_KEY = Key.key("likes", "translations");

    private TranslationRegistry registry;

    /**
     * Initialises the translation registry and registers it with
     * {@link GlobalTranslator}.
     * Call this once during plugin startup.
     *
     * @param classLoader the plugin's class loader used to locate resource files
     */
    public void initialize(ClassLoader classLoader) {
        registry = TranslationRegistry.create(REGISTRY_KEY);
        registry.defaultLocale(Locale.US);

        // Load all three .properties files via UTF-8 streams to support non-ASCII
        // characters.
        loadAndRegister(classLoader, Locale.US, "messages_en_US.properties");
        loadAndRegister(classLoader, Locale.JAPAN, "messages_ja_JP.properties");
        // Register ROOT as an extra safety fallback (same content as en_US).
        loadAndRegister(classLoader, Locale.ROOT, "messages_en_US.properties");

        GlobalTranslator.translator().addSource(registry);
    }

    /**
     * Removes the registry from {@link GlobalTranslator}.
     * Call this during plugin shutdown.
     */
    public void close() {
        if (registry != null) {
            GlobalTranslator.translator().removeSource(registry);
            registry = null;
        }
    }

    // -------------------------------------------------------------------------
    // Internal helpers
    // -------------------------------------------------------------------------

    private void loadAndRegister(ClassLoader classLoader, Locale locale, String filename) {
        try (InputStream is = classLoader.getResourceAsStream(filename)) {
            if (is == null) {
                log.warning("Translation file not found: " + filename);
                return;
            }
            ResourceBundle bundle = new PropertyResourceBundle(
                    new InputStreamReader(is, StandardCharsets.UTF_8));
            registry.registerAll(locale, bundle, false);
        } catch (IOException e) {
            log.log(Level.WARNING, "Failed to load translation file: " + filename, e);
        }
    }
}
