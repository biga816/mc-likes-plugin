package dev.example.likes.service;

import org.bukkit.configuration.file.FileConfiguration;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

/**
 * Service that manages per-pair (sender→target) cooldowns in memory.
 * Cooldowns reset on server restart by design.
 */
public class CooldownService {
    private static final Logger log = Logger.getLogger(CooldownService.class.getName());

    /** Key: "senderUuid:targetUuid", Value: cooldown expiry timestamp in ms */
    private final ConcurrentHashMap<String, Long> cooldownMap = new ConcurrentHashMap<>();
    private final int cooldownSeconds;

    /**
     * Constructs a CooldownService.
     *
     * @param config plugin configuration; reads cooldown duration from
     *               {@code limits.pairCooldownSeconds}.
     *               Defaults to 60 seconds.
     */
    public CooldownService(FileConfiguration config) {
        this.cooldownSeconds = config.getInt("limits.pairCooldownSeconds", 60);
        log.fine("CooldownService initialized: pairCooldownSeconds=" + cooldownSeconds);
    }

    /**
     * Returns whether the given sender→target pair is currently on cooldown.
     *
     * @param sender the sender's UUID
     * @param target the target player's UUID
     * @return true if the pair is on cooldown
     */
    public boolean isOnCooldown(UUID sender, UUID target) {
        String k = key(sender, target);
        Long expiry = cooldownMap.get(k);
        if (expiry == null) {
            return false;
        }
        if (System.currentTimeMillis() < expiry) {
            return true;
        }
        // Expired — remove the entry
        cooldownMap.remove(k);
        return false;
    }

    /**
     * Sets the cooldown for the given sender→target pair.
     * Should be called immediately after {@link #isOnCooldown(UUID, UUID)}.
     *
     * @param sender the sender's UUID
     * @param target the target player's UUID
     */
    public void setCooldown(UUID sender, UUID target) {
        long expiry = System.currentTimeMillis() + (cooldownSeconds * 1000L);
        cooldownMap.put(key(sender, target), expiry);
    }

    /**
     * Returns the remaining cooldown seconds for the given sender→target pair.
     * Returns 0 if the pair is not on cooldown.
     *
     * @param sender the sender's UUID
     * @param target the target player's UUID
     * @return remaining cooldown in seconds, or 0 if not on cooldown
     */
    public long getRemainingSeconds(UUID sender, UUID target) {
        Long expiry = cooldownMap.get(key(sender, target));
        if (expiry == null) {
            return 0L;
        }
        long remaining = (expiry - System.currentTimeMillis()) / 1000L;
        return Math.max(0L, remaining);
    }

    private String key(UUID sender, UUID target) {
        return sender + ":" + target;
    }
}
