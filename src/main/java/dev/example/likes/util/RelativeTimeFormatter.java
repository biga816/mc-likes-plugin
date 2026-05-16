package dev.example.likes.util;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Map;

/**
 * Utility for formatting a timestamp as a human-readable relative or absolute
 * time string.
 *
 * <p>
 * Thresholds:
 * </p>
 * <ul>
 * <li>0–59 s → "just now"</li>
 * <li>1–59 m → "{n}m ago"</li>
 * <li>1–23 h → "{n}h ago"</li>
 * <li>1–7 d → "{n}d ago"</li>
 * <li>8 d or more → absolute date (yyyy-MM-dd / yyyy/MM/dd)</li>
 * </ul>
 *
 * <p>
 * Locale selection is handled by the supplied {@link PlayerTranslator}.
 * </p>
 */
public final class RelativeTimeFormatter {

    private static final long SECONDS_PER_MINUTE = 60L;
    private static final long SECONDS_PER_HOUR = 3600L;
    private static final long SECONDS_PER_DAY = 86400L;
    /** Threshold (in seconds) above which absolute date is shown (8 days). */
    private static final long ABSOLUTE_THRESHOLD_SECONDS = 8L * SECONDS_PER_DAY;

    /**
     * Add an entry here when a new language needs a different absolute-date
     * pattern.
     */
    private static final Map<String, DateTimeFormatter> DATE_FORMAT_BY_LANGUAGE = Map.of(
            "en", DateTimeFormatter.ofPattern("yyyy-MM-dd"),
            "ja", DateTimeFormatter.ofPattern("yyyy/MM/dd"));

    private RelativeTimeFormatter() {
    }

    /**
     * Formats a past timestamp as a relative or absolute time string using the
     * given locale-bound translator.
     *
     * @param createdAtMillis epoch-millisecond timestamp of the event
     * @param tr              locale-bound translator for the viewing player
     * @return a human-readable time string
     */
    public static String format(long createdAtMillis, PlayerTranslator tr) {
        long diffMs = System.currentTimeMillis() - createdAtMillis;
        long diffSec = Math.max(0L, diffMs / 1000L);

        if (diffSec < SECONDS_PER_MINUTE) {
            return tr.translate("likes.time.just_now");
        } else if (diffSec < SECONDS_PER_HOUR) {
            long mins = diffSec / SECONDS_PER_MINUTE;
            return tr.translate("likes.time.minutes_ago", mins);
        } else if (diffSec < SECONDS_PER_DAY) {
            long hours = diffSec / SECONDS_PER_HOUR;
            return tr.translate("likes.time.hours_ago", hours);
        } else if (diffSec < ABSOLUTE_THRESHOLD_SECONDS) {
            long days = diffSec / SECONDS_PER_DAY;
            return tr.translate("likes.time.days_ago", days);
        } else {
            // 8 days or more → absolute date
            String lang = tr.locale().getLanguage();
            DateTimeFormatter dtf = DATE_FORMAT_BY_LANGUAGE.getOrDefault(
                    lang, DATE_FORMAT_BY_LANGUAGE.get("en"));
            return dtf.format(
                    Instant.ofEpochMilli(createdAtMillis)
                            .atZone(ZoneId.systemDefault())
                            .toLocalDate());
        }
    }
}
