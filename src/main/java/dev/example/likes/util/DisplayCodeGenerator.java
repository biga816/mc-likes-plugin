package dev.example.likes.util;

import dev.example.likes.database.BroadcastRepository;

import java.security.SecureRandom;
import java.sql.SQLException;
import java.util.logging.Logger;

/**
 * Utility for generating 4-character display codes using a human-friendly
 * Base32-style character set that avoids visually similar characters.
 * <p>
 * Charset: {@code 23456789ABCDEFGHJKLMNPQRSTUVWXYZ} (excludes 0, 1, I, O)
 * </p>
 */
public class DisplayCodeGenerator {

    private static final Logger log = Logger.getLogger(DisplayCodeGenerator.class.getName());
    private static final String CHARSET = "23456789ABCDEFGHJKLMNPQRSTUVWXYZ";
    private static final int CODE_LENGTH = 4;
    private static final int MAX_RETRIES = 10;
    private static final int RECENT_WINDOW = 100;
    private static final SecureRandom RANDOM = new SecureRandom();

    private final BroadcastRepository broadcastRepository;

    /**
     * Constructs a DisplayCodeGenerator with uniqueness checking against recent broadcasts.
     *
     * @param broadcastRepository the broadcast repository used for collision detection
     */
    public DisplayCodeGenerator(BroadcastRepository broadcastRepository) {
        this.broadcastRepository = broadcastRepository;
    }

    /**
     * Generates a single 4-character display code without collision checking.
     *
     * @return a 4-character code from the human-friendly charset
     */
    public static String generate() {
        char[] code = new char[CODE_LENGTH];
        for (int i = 0; i < CODE_LENGTH; i++) {
            code[i] = CHARSET.charAt(RANDOM.nextInt(CHARSET.length()));
        }
        return new String(code);
    }

    /**
     * Generates a 4-character display code that does not collide with any of
     * the most recent {@value RECENT_WINDOW} broadcasts in the database.
     * <p>
     * Retries up to {@value MAX_RETRIES} times; throws if all attempts collide.
     * </p>
     *
     * @return a display code unique within the recent window
     * @throws SQLException if the maximum retry count is exceeded or a DB operation fails
     */
    public String generateUnique() throws SQLException {
        for (int attempt = 1; attempt <= MAX_RETRIES; attempt++) {
            String code = generate();
            if (!broadcastRepository.existsInRecentByDisplayCode(code, RECENT_WINDOW)) {
                return code;
            }
            log.fine("displayCode collision on attempt " + attempt + ", retrying...");
        }
        throw new SQLException("Failed to generate unique displayCode after " + MAX_RETRIES + " attempts");
    }
}
