package net.imprex.goolak;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;

final class GOOLakListener implements Listener {

  private final GOOLakService service;

  GOOLakListener(GOOLakService service) {
    this.service = service;
  }

  @EventHandler
  public void onQuit(PlayerQuitEvent event) {
    this.service.clearPlayer(event.getPlayer());
  }
}
