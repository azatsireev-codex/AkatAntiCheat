package net.akat.goolak.antixray;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletionException;
import java.util.UUID;
import net.akat.goolak.ObfuscationResult;
import net.akat.goolak.ObfuscationSystem;
import net.akat.goolak.platform.BlockChangeSender;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Player;

public final class AntiXRayService {

  private final BlockChangeSender blockChangeSender;
  private final ObfuscationSystem obfuscationSystem;
  private final Map<UUID, Set<BlockPos>> activeMasks = new HashMap<>();

  private AntiXRayConfig config;

  public AntiXRayService(BlockChangeSender blockChangeSender, AntiXRayConfig config, ObfuscationSystem obfuscationSystem) {
    this.blockChangeSender = blockChangeSender;
    this.config = config;
    this.obfuscationSystem = obfuscationSystem;
  }

  public void updateConfig(AntiXRayConfig config) {
    this.config = config;

    if (!this.config.enabled()) {
      for (Player player : Bukkit.getOnlinePlayers()) {
        this.clearPlayer(player);
      }
      this.activeMasks.clear();
    }
  }

  public void start() {
    // Chunk-level mode: no repeating full world scan.
  }

  public void stop() {
    for (Player player : Bukkit.getOnlinePlayers()) {
      this.clearPlayer(player);
    }

    this.activeMasks.clear();
    this.obfuscationSystem.shutdown();
  }

  public void clearPlayer(Player player) {
    Set<BlockPos> oldMask = this.activeMasks.remove(player.getUniqueId());
    if (oldMask == null || oldMask.isEmpty()) {
      return;
    }

    for (BlockPos pos : oldMask) {
      this.restoreBlock(player, pos);
    }
  }

  public void handleChunkLoad(Player player, Chunk chunk) {
    if (!this.config.enabled()) {
      return;
    }

    Set<BlockPos> playerMask = this.activeMasks.computeIfAbsent(player.getUniqueId(), key -> new java.util.HashSet<>());
    this.obfuscationSystem.obfuscate(player, chunk, this.config, playerMask)
        .thenAccept(result -> this.applyObfuscation(player, chunk.getWorld(), playerMask, result))
        .exceptionally(throwable -> {
          Throwable cause = throwable instanceof CompletionException && throwable.getCause() != null
              ? throwable.getCause() : throwable;
          Bukkit.getLogger().warning("[GOOLak] Failed to obfuscate chunk " + chunk.getX() + "," + chunk.getZ()
              + " for player " + player.getName() + ": " + cause.getMessage());
          return null;
        });
  }

  public void handleChunkUnload(Player player, int chunkX, int chunkZ) {
    Set<BlockPos> mask = this.activeMasks.get(player.getUniqueId());
    if (mask == null || mask.isEmpty()) {
      return;
    }

    int restoresLeft = this.config.maxRestoresPerScan();
    Iterator<BlockPos> iterator = mask.iterator();
    while (iterator.hasNext() && restoresLeft > 0) {
      BlockPos pos = iterator.next();
      if ((pos.x() >> 4) == chunkX && (pos.z() >> 4) == chunkZ) {
        this.restoreBlock(player, pos);
        iterator.remove();
        restoresLeft--;
      }
    }

    if (mask.isEmpty()) {
      this.activeMasks.remove(player.getUniqueId());
    }
  }

  public void refreshBlockForAllPlayers(Block block) {
    BlockPos pos = new BlockPos(block.getX(), block.getY(), block.getZ());
    BlockData realData = block.getBlockData();

    for (Player player : Bukkit.getOnlinePlayers()) {
      Set<BlockPos> mask = this.activeMasks.get(player.getUniqueId());
      if (mask == null || !mask.remove(pos)) {
        continue;
      }
      this.blockChangeSender.sendBlockChange(player, block.getLocation(), realData);
      if (mask.isEmpty()) {
        this.activeMasks.remove(player.getUniqueId());
      }
    }
  }

  private void applyObfuscation(Player player, World world, Set<BlockPos> playerMask, ObfuscationResult result) {
    if (result.isEmpty()) {
      return;
    }

    for (Map.Entry<BlockPos, BlockData> entry : result.replacements().entrySet()) {
      BlockPos pos = entry.getKey();
      if (playerMask.add(pos)) {
        this.blockChangeSender.sendBlockChange(player, new Location(world, pos.x(), pos.y(), pos.z()), entry.getValue());
      }
    }
  }

  private void restoreBlock(Player player, BlockPos pos) {
    World world = player.getWorld();
    BlockData blockData = world.getBlockAt(pos.x(), pos.y(), pos.z()).getBlockData();
    this.blockChangeSender.sendBlockChange(player, new Location(world, pos.x(), pos.y(), pos.z()), blockData);
  }
}
