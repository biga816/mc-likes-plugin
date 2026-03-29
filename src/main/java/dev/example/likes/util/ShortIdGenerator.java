package dev.example.likes.util;

import dev.example.likes.database.BroadcastRepository;

import java.sql.SQLException;
import java.util.UUID;
import java.util.logging.Logger;

/**
 * Utility for generating 8-character Base36 (0-9, a-z) short IDs.
 * <p>
 * The static method generates one ID without collision checking;
 * the instance method generates one with DB-backed uniqueness verification.
 * </p>
 */
public class ShortIdGenerator {

    private static final Logger log = Logger.getLogger(ShortIdGenerator.class.getName());
    private static final int ID_LENGTH = 8;
    private static final int MAX_RETRIES = 5;

    private final BroadcastRepository broadcastRepository;

    /**
     * Constructs a ShortIdGenerator with DB-backed uniqueness checking.
     *
     * @param broadcastRepository the broadcast repository used for collision
     *                            detection
     */
    public ShortIdGenerator(BroadcastRepository broadcastRepository) {
        this.broadcastRepository = broadcastRepository;
    }

    /**
     * Generates a single 8-character Base36 ID without collision checking.
     * <p>
     * XORs the most and least significant bits of a {@link UUID#randomUUID()},
     * masks to a non-negative value, then converts to a Base36 string.
     * Left-pads with zeros if shorter than 8 characters; truncates to 8 if longer.
     * </p>
     *
     * @return an 8-character Base36 ID
     */
    public static String generate() {
        UUID uuid = UUID.randomUUID();
        // Math.abs(Long.MIN_VALUE) stays negative; use bitwise AND to guarantee
        // non-negative
        long hash = (uuid.getMostSignificantBits() ^ uuid.getLeastSignificantBits()) & Long.MAX_VALUE;
        String base36 = Long.toString(hash, 36);
        // Normalise to exactly 8 characters (left-pad if short, truncate if long)
        if (base36.length() < ID_LENGTH) {
            return "0".repeat(ID_LENGTH - base36.length()) + base36;
        }
        return base36.substring(0, ID_LENGTH);
    }

    /**
     * Generates an 8-character Base36 ID that is unique in the database.
     * <p>
     * Retries up to {@value MAX_RETRIES} times; throws if all attempts collide.
     * </p>
     *
     * @return a DB-unique 8-character Base36 ID
     * @throws SQLException if the maximum retry count is exceeded or a DB operation
     *                      fails
     */
    public String generateUnique() throws SQLException {
        for (int attempt = 1; attempt <= MAX_RETRIES; attempt++) {
            String id = generate();
            if (!broadcastRepository.existsByShortId(id)) {
                return id;
            }
            log.fine("shortId collision on attempt " + attempt + ", retrying...");
        }
        throw new SQLException("Failed to generate unique shortId after " + MAX_RETRIES + " attempts");
    }
}
