package net.akat.goolak;

import java.util.Set;
import net.akat.goolak.antixray.AntiXRayConfig;
import net.akat.goolak.antixray.BlockPos;
import org.bukkit.Chunk;
import org.bukkit.entity.Player;

public record ObfuscationTask(ObfuscationRequest request) {

  public Player player() {
    return this.request.player();
  }

  public Chunk chunk() {
    return this.request.chunk();
  }

  public AntiXRayConfig config() {
    return this.request.config();
  }

  public Set<BlockPos> alreadyMasked() {
    return this.request.alreadyMasked();
  }
}
