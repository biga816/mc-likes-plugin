package dev.example.likes.service;

import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Displays particle effects on like/reaction success.
 * All methods must be called on the server main thread.
 */
public class LikeEffectService {

    private static final Logger log = Logger.getLogger(LikeEffectService.class.getName());

    private static final double SPREAD = 0.3;
    private static final double Y_OFFSET = 2.5;

    private final FileConfiguration config;

    public LikeEffectService(FileConfiguration config) {
        this.config = config;
    }

    /** Called after a direct like succeeds. */
    public void showDirectLikeEffect(Player sender, Player target) {
        if (!isEnabled())
            return;
        showEffect(sender, Particle.HEART, 1);
        showEffect(target, Particle.HEART, 1);
        showEffect(target, Particle.FIREWORK, 10);
    }

    /** Called after a reaction like succeeds. target may be null if offline. */
    public void showReactionEffect(Player reactor, Player target) {
        if (!isEnabled())
            return;

        showEffect(reactor, Particle.HEART, 1);

        if (target != null) {
            showEffect(target, Particle.HEART, 3);
            showEffect(target, Particle.FIREWORK, 10);
        }
    }

    // ── Internal helpers ─────────────────────────────────────────────────────

    private void showEffect(Player player, Particle particle, int count) {
        if (!player.isOnline())
            return;
        try {
            Location loc = player.getLocation().add(0, Y_OFFSET, 0);
            player.getWorld().spawnParticle(particle, loc, count, SPREAD, SPREAD, SPREAD, 0.0);
        } catch (Exception e) {
            log.log(Level.WARNING, "Failed to show particle effect for " + player.getName(), e);
        }
    }

    private boolean isEnabled() {
        return config.getBoolean("effects.enabled", true);
    }
}
