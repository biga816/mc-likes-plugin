package dev.example.likes.util;

import net.kyori.adventure.translation.GlobalTranslator;

import java.text.MessageFormat;
import java.util.Locale;

/**
 * A locale-bound translator for a single player.
 * <p>
 * Obtained via {@link MessageFactory#translatorFor(org.bukkit.entity.Player)}.
 * The player's locale is fixed at construction time, so callers do not need
 * to pass a {@link Locale} on every call.
 * </p>
 * <p>
 * Intended for contexts where Adventure cannot resolve
 * {@link net.kyori.adventure.text.Component#translatable(String)} automatically
 * (e.g. book NBT pages). For normal chat messages, continue using
 * {@link MessageFactory#info}/{@link MessageFactory#error} etc. which rely on
 * Adventure's per-packet resolution.
 * </p>
 */
public class PlayerTranslator {

    private final Locale locale;

    PlayerTranslator(Locale locale) {
        this.locale = locale;
    }

    /**
     * Returns the locale bound to this translator.
     *
     * @return the player's locale
     */
    public Locale locale() {
        return locale;
    }

    /**
     * Resolves a translation key for this player's locale.
     * Falls back to the key itself if no translation is registered.
     *
     * @param key the translation key (e.g. {@code "likes.book.ranking.title"})
     * @return the resolved string
     */
    public String translate(String key) {
        MessageFormat format = GlobalTranslator.translator().translate(key, locale);
        return format != null ? format.format(null) : key;
    }

    /**
     * Resolves a translation key with format arguments.
     *
     * @param key  the translation key
     * @param args arguments substituted into {@code {0}}, {@code {1}}, …
     * @return the resolved string
     */
    public String translate(String key, Object... args) {
        MessageFormat format = GlobalTranslator.translator().translate(key, locale);
        return format != null ? format.format(args) : key;
    }
}
