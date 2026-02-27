package net.akat.goolak.antixray;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;

public final class AntiXRayListener implements Listener {

  private final AntiXRayService antiXRayService;

  public AntiXRayListener(AntiXRayService antiXRayService) {
    this.antiXRayService = antiXRayService;
  }

  @EventHandler
  public void onQuit(PlayerQuitEvent event) {
    this.antiXRayService.clearPlayer(event.getPlayer());
  }
}
