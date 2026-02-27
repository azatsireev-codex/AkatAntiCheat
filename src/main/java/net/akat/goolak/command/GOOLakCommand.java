package net.akat.goolak.command;

import net.akat.goolak.GOOLakPlugin;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

public final class GOOLakCommand implements CommandExecutor {

  private final GOOLakPlugin plugin;

  public GOOLakCommand(GOOLakPlugin plugin) {
    this.plugin = plugin;
  }

  @Override
  public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
    if (args.length == 1 && args[0].equalsIgnoreCase("reload")) {
      this.plugin.reloadPluginConfig();
      sender.sendMessage("§aGOOLak AntiXRay config reloaded.");
      return true;
    }

    sender.sendMessage("§eUsage: /" + label + " reload");
    return true;
  }
}
