package net.akat.goolak;

import io.papermc.paper.event.packet.PlayerChunkLoadEvent;
import io.papermc.paper.event.packet.PlayerChunkUnloadEvent;
import net.akat.goolak.antixray.AntiXRayService;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;

public final class ObfuscationListener implements Listener {

  private final AntiXRayService antiXRayService;

  public ObfuscationListener(AntiXRayService antiXRayService) {
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
}
