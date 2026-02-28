package net.akat.goolak.platform;

import org.bukkit.Location;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Player;

public interface BlockChangeSender {

  void sendBlockChange(Player player, Location location, BlockData blockData);
}
