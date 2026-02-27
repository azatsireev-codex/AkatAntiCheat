package net.imprex.goolak;

import java.util.Objects;
import net.imprex.goolak.antixray.AntiXRayConfig;
import net.imprex.goolak.antixray.AntiXRayListener;
import net.imprex.goolak.antixray.AntiXRayService;
import net.imprex.goolak.command.GOOLakCommand;
import net.imprex.goolak.platform.TaskDispatcher;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;

public final class GOOLakPlugin extends JavaPlugin {

  private AntiXRayService antiXRayService;

  @Override
  public void onEnable() {
    this.saveDefaultConfig();

    TaskDispatcher taskDispatcher = new TaskDispatcher(this);
    AntiXRayConfig config = AntiXRayConfig.from(this.getConfig());
    this.antiXRayService = new AntiXRayService(taskDispatcher, config);
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
