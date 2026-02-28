package net.akat.goolak;

import java.util.Objects;
import net.akat.goolak.antixray.AntiXRayConfig;
import net.akat.goolak.antixray.AntiXRayListener;
import net.akat.goolak.antixray.AntiXRayService;
import net.akat.goolak.command.GOOLakCommand;
import net.akat.goolak.platform.BlockChangeSender;
import net.akat.goolak.platform.PacketEventsBlockChangeSender;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;

public final class GOOLakPlugin extends JavaPlugin {

  private AntiXRayService antiXRayService;

  @Override
  public void onEnable() {
    this.saveDefaultConfig();

    BlockChangeSender blockChangeSender = PacketEventsBlockChangeSender.create(this);
    AntiXRayConfig config = AntiXRayConfig.from(this.getConfig());
    this.antiXRayService = new AntiXRayService(blockChangeSender, config);
    this.antiXRayService.start();

    PluginCommand command = Objects.requireNonNull(this.getCommand("goolak"), "Missing command goolak");
    command.setExecutor(new GOOLakCommand(this));

    this.getServer().getPluginManager().registerEvents(new AntiXRayListener(this.antiXRayService), this);

    this.getLogger().info("GOOLak enabled. AntiXRay module is active.");
  }

  @Override
  public void onDisable() {
    if (this.antiXRayService != null) {
      this.antiXRayService.stop();
      this.antiXRayService = null;
    }
  }

  public void reloadPluginConfig() {
    this.reloadConfig();
    AntiXRayConfig config = AntiXRayConfig.from(this.getConfig());
    this.antiXRayService.updateConfig(config);
  }
}
