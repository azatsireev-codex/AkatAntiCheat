package net.akat.goolak.antixray;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import net.akat.goolak.platform.BlockChangeSender;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Player;

public final class AntiXRayService {

  private final BlockChangeSender blockChangeSender;
  private final Map<UUID, Set<BlockPos>> activeMasks = new HashMap<>();

  private AntiXRayConfig config;

  public AntiXRayService(BlockChangeSender blockChangeSender, AntiXRayConfig config) {
    this.blockChangeSender = blockChangeSender;
    this.config = config;
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

    this.maskChunk(player, chunk);
  }

  public void handleChunkUnload(Player player, int chunkX, int chunkZ) {
    Set<BlockPos> mask = this.activeMasks.get(player.getUniqueId());
    if (mask == null || mask.isEmpty()) {
      return;
    }

    Iterator<BlockPos> iterator = mask.iterator();
    while (iterator.hasNext()) {
      BlockPos pos = iterator.next();
      if ((pos.x() >> 4) == chunkX && (pos.z() >> 4) == chunkZ) {
        this.restoreBlock(player, pos);
        iterator.remove();
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

  private void maskChunk(Player player, Chunk chunk) {
    World world = chunk.getWorld();
    int worldMinY = world.getMinHeight();
    int worldMaxY = world.getMaxHeight() - 1;
    int minY = Math.max(worldMinY, this.config.minY());
    int maxY = Math.min(worldMaxY, this.config.maxY());

    int budget = this.config.maxReplacementsPerScan();
    Set<BlockPos> playerMask = this.activeMasks.computeIfAbsent(player.getUniqueId(), key -> new HashSet<>());
    int baseX = chunk.getX() << 4;
    int baseZ = chunk.getZ() << 4;

    for (int y = minY; y <= maxY && budget > 0; y++) {
      for (int localZ = 0; localZ < 16 && budget > 0; localZ++) {
        for (int localX = 0; localX < 16 && budget > 0; localX++) {
          int x = baseX + localX;
          int z = baseZ + localZ;
          if (this.tryMaskBlock(player, world, x, y, z, playerMask)) {
            budget--;
          }
        }
      }
    }
  }

  private boolean tryMaskBlock(Player player, World world, int x, int y, int z, Set<BlockPos> targetMask) {
    Material material = world.getBlockAt(x, y, z).getType();
    if (!this.config.hiddenMaterials().contains(material)) {
      return false;
    }

    BlockPos pos = new BlockPos(x, y, z);
    if (!targetMask.add(pos)) {
      return false;
    }

    this.blockChangeSender.sendBlockChange(player, new Location(world, x, y, z), this.config.replacementBlockData());
    return true;
  }

  private void restoreBlock(Player player, BlockPos pos) {
    World world = player.getWorld();
    BlockData blockData = world.getBlockAt(pos.x(), pos.y(), pos.z()).getBlockData();
    this.blockChangeSender.sendBlockChange(player, new Location(world, pos.x(), pos.y(), pos.z()), blockData);
  }
}
