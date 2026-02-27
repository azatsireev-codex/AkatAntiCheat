package net.imprex.goolak;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

final class GOOLakCommand implements CommandExecutor {

  private final GOOLakPlugin plugin;

  GOOLakCommand(GOOLakPlugin plugin) {
    this.plugin = plugin;
  }

  @Override
  public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
    if (args.length == 1 && args[0].equalsIgnoreCase("reload")) {
      this.plugin.reloadPluginConfig();
      sender.sendMessage("§aGOOLak config reloaded.");
      return true;
    }

    sender.sendMessage("§eUsage: /" + label + " reload");
    return true;
  }
}
