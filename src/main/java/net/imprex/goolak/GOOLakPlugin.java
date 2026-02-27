package net.imprex.goolak;

import java.util.Objects;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;

public final class GOOLakPlugin extends JavaPlugin {

  private GOOLakService service;

  @Override
  public void onEnable() {
    this.saveDefaultConfig();

    GOOLakConfig config = GOOLakConfig.from(this.getConfig());
    this.service = new GOOLakService(this, config);
    this.service.start();

    PluginCommand command = Objects.requireNonNull(this.getCommand("goolak"), "Missing command goolak");
    command.setExecutor(new GOOLakCommand(this));

    this.getServer().getPluginManager().registerEvents(new GOOLakListener(this.service), this);

    this.getLogger().info("GOOLak enabled. Boundary-only masking is active.");
  }

  @Override
  public void onDisable() {
    if (this.service != null) {
      this.service.stop();
      this.service = null;
    }
  }

  void reloadPluginConfig() {
    this.reloadConfig();
    GOOLakConfig config = GOOLakConfig.from(this.getConfig());
    this.service.updateConfig(config);
  }
}
