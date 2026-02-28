package net.akat.goolak.antixray;

import io.papermc.paper.event.packet.PlayerChunkLoadEvent;
import io.papermc.paper.event.packet.PlayerChunkUnloadEvent;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public final class AntiXRayListener implements Listener {

  private final AntiXRayService antiXRayService;

  public AntiXRayListener(AntiXRayService antiXRayService) {
    this.antiXRayService = antiXRayService;
  }

  @EventHandler
  public void onChunkLoad(PlayerChunkLoadEvent event) {
    this.antiXRayService.handleChunkLoad(event.getPlayer(), event.getChunk());
  }

  @EventHandler
  public void onChunkUnload(PlayerChunkUnloadEvent event) {
    this.antiXRayService.handleChunkUnload(event.getPlayer(), event.getChunk().getX(), event.getChunk().getZ());
  }

  @EventHandler
  public void onQuit(PlayerQuitEvent event) {
    this.antiXRayService.clearPlayer(event.getPlayer());
  }

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void onBlockBreak(BlockBreakEvent event) {
    this.antiXRayService.refreshBlockForAllPlayers(event.getBlock());
  }

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void onBlockPlace(BlockPlaceEvent event) {
    this.antiXRayService.refreshBlockForAllPlayers(event.getBlockPlaced());
  }
}
