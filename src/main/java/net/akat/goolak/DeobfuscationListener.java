package net.akat.goolak;

import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;

public final class DeobfuscationListener implements Listener {

  private final DeobfuscationWorker worker;

  public DeobfuscationListener(DeobfuscationWorker worker) {
    this.worker = worker;
  }

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void onBlockBreak(BlockBreakEvent event) {
    this.worker.deobfuscateAround(event.getBlock());
  }

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void onBlockPlace(BlockPlaceEvent event) {
    this.worker.deobfuscateAround(event.getBlockPlaced());
  }
}
