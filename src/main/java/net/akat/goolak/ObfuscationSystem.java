package net.akat.goolak;

import java.util.Set;
import java.util.concurrent.CompletableFuture;
import net.akat.goolak.antixray.AntiXRayConfig;
import net.akat.goolak.antixray.BlockPos;
import org.bukkit.Chunk;
import org.bukkit.entity.Player;

public final class ObfuscationSystem {

  private final ObfuscationTaskDispatcher dispatcher;
  private final ObfuscationTaskWorker worker;

  public ObfuscationSystem() {
    this.dispatcher = new ObfuscationTaskDispatcher();
    this.worker = new ObfuscationTaskWorker(new ObfuscationProcessor());
  }

  public CompletableFuture<ObfuscationResult> obfuscate(Player player, Chunk chunk, AntiXRayConfig config,
      Set<BlockPos> alreadyMasked) {
    ObfuscationRequest request = new ObfuscationRequest(player, chunk, config, alreadyMasked);
    ObfuscationTask task = new ObfuscationTask(request);
    return this.dispatcher.dispatch(task, this.worker);
  }

  public void shutdown() {
    this.dispatcher.shutdown();
  }
}
