package dev.example.likes;

import org.bukkit.plugin.java.JavaPlugin;

public class LikesPlugin extends JavaPlugin {
  @Override
  public void onEnable() {
    saveDefaultConfig();
    getLogger().info("Likes enabled!");
  }

  @Override
  public void onDisable() {
    getLogger().info("Likes disabled!");
  }
}