package net.akat.goolak.antixray;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import net.akat.goolak.platform.BlockChangeSender;
import net.akat.goolak.platform.TaskDispatcher;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

public final class AntiXRayService {

  private final TaskDispatcher taskDispatcher;
  private final BlockChangeSender blockChangeSender;
  private final Map<UUID, Set<BlockPos>> activeMasks = new HashMap<>();

  private AntiXRayConfig config;
  private Object scanTaskHandle;

  public AntiXRayService(TaskDispatcher taskDispatcher, BlockChangeSender blockChangeSender, AntiXRayConfig config) {
    this.taskDispatcher = taskDispatcher;
    this.blockChangeSender = blockChangeSender;
    this.config = config;
  }

  public void updateConfig(AntiXRayConfig config) {
    this.config = config;
    this.restartTask();
  }

  public void start() {
    this.restartTask();
  }

  public void stop() {
    this.taskDispatcher.cancelTask(this.scanTaskHandle);
    this.scanTaskHandle = null;

    for (Player player : Bukkit.getOnlinePlayers()) {
      this.clearPlayer(player);
    }

    this.activeMasks.clear();
  }

  public void clearPlayer(Player player) {
    this.taskDispatcher.executePlayerTask(player, () -> this.clearPlayerInternal(player));
  }

  private void clearPlayerInternal(Player player) {
    Set<BlockPos> oldMask = this.activeMasks.remove(player.getUniqueId());
    if (oldMask == null || oldMask.isEmpty()) {
      return;
    }

    for (BlockPos pos : oldMask) {
      this.restoreBlock(player, pos);
    }
  }

  private void restartTask() {
    this.taskDispatcher.cancelTask(this.scanTaskHandle);
    this.scanTaskHandle = null;

    if (!this.config.enabled()) {
      return;
    }

    this.scanTaskHandle = this.taskDispatcher.runRepeatingTask(this::scanPlayers, 20L, this.config.scanIntervalTicks());
  }

  private void scanPlayers() {
    for (Player player : Bukkit.getOnlinePlayers()) {
      if (!player.isOnline() || player.isDead()) {
        continue;
      }
      this.taskDispatcher.executePlayerTask(player, () -> this.scanPlayer(player));
    }
  }

  private void scanPlayer(Player player) {
    World world = player.getWorld();
    int worldMinY = world.getMinHeight();
    int worldMaxY = world.getMaxHeight() - 1;
    int minY = Math.max(worldMinY, this.config.minY());
    int maxY = Math.min(worldMaxY, this.config.maxY());

    Location location = player.getLocation();
    int centerChunkX = location.getBlockX() >> 4;
    int centerChunkZ = location.getBlockZ() >> 4;

    Set<BlockPos> newMask = new HashSet<>();
    int replacementsLeft = this.config.maxReplacementsPerScan();

    for (int chunkZ = centerChunkZ - this.config.chunkRadius(); chunkZ <= centerChunkZ
        + this.config.chunkRadius() && replacementsLeft > 0; chunkZ++) {
      for (int chunkX = centerChunkX - this.config.chunkRadius(); chunkX <= centerChunkX
          + this.config.chunkRadius() && replacementsLeft > 0; chunkX++) {
        if (!world.isChunkLoaded(chunkX, chunkZ)) {
          continue;
        }

        Chunk chunk = world.getChunkAt(chunkX, chunkZ);
        replacementsLeft -= this.maskChunk(player, chunk, minY, maxY, newMask, replacementsLeft);
      }
    }

    Set<BlockPos> oldMask = this.activeMasks.get(player.getUniqueId());
    if (oldMask != null && !oldMask.isEmpty()) {
      int restoresLeft = this.config.maxRestoresPerScan();
      for (BlockPos oldPos : oldMask) {
        if (newMask.contains(oldPos)) {
          continue;
        }

        if (restoresLeft > 0) {
          this.restoreBlock(player, oldPos);
          restoresLeft--;
        } else {
          newMask.add(oldPos);
        }
      }
    }

    this.activeMasks.put(player.getUniqueId(), newMask);
  }

  private int maskChunk(Player player, Chunk chunk, int minY, int maxY, Set<BlockPos> targetMask, int budget) {
    int replaced = 0;
    int baseX = chunk.getX() << 4;
    int baseZ = chunk.getZ() << 4;

    for (int y = minY; y <= maxY && replaced < budget; y++) {
      for (int localZ = 0; localZ < 16 && replaced < budget; localZ++) {
        for (int localX = 0; localX < 16 && replaced < budget; localX++) {
          replaced += this.tryMaskBlock(player, baseX + localX, y, baseZ + localZ, targetMask, budget - replaced);
        }
      }
    }

    return replaced;
  }

  private int tryMaskBlock(Player player, int x, int y, int z, Set<BlockPos> targetMask, int budget) {
    if (budget <= 0) {
      return 0;
    }

    World world = player.getWorld();
    Block block = world.getBlockAt(x, y, z);
    Material material = block.getType();
    if (!this.config.hiddenMaterials().contains(material)) {
      return 0;
    }

    if (!isEnclosed(block)) {
      return 0;
    }

    if (isInsideViewCone(player, x + 0.5, y + 0.5, z + 0.5, this.config.viewConeDegrees())) {
      return 0;
    }

    BlockPos pos = new BlockPos(x, y, z);
    this.blockChangeSender.sendBlockChange(player, new Location(world, x, y, z),
        this.config.replacementMaterial().createBlockData());
    targetMask.add(pos);
    return 1;
  }

  private void restoreBlock(Player player, BlockPos pos) {
    World world = player.getWorld();
    BlockData blockData = world.getBlockAt(pos.x(), pos.y(), pos.z()).getBlockData();
    this.blockChangeSender.sendBlockChange(player, new Location(world, pos.x(), pos.y(), pos.z()), blockData);
  }

  static boolean isEnclosed(Block block) {
    return isOccluding(block.getRelative(1, 0, 0))
        && isOccluding(block.getRelative(-1, 0, 0))
        && isOccluding(block.getRelative(0, 1, 0))
        && isOccluding(block.getRelative(0, -1, 0))
        && isOccluding(block.getRelative(0, 0, 1))
        && isOccluding(block.getRelative(0, 0, -1));
  }

  static boolean isOccluding(Block block) {
    Material material = block.getType();
    return material.isOccluding() && material.isSolid();
  }

  static boolean isInsideViewCone(Player player, double x, double y, double z, double viewConeDegrees) {
    Location eye = player.getEyeLocation();

    Vector toBlock = new Vector(x - eye.getX(), y - eye.getY(), z - eye.getZ());
    if (toBlock.lengthSquared() < 0.01) {
      return true;
    }

    Vector direction = eye.getDirection();
    double dot = direction.normalize().dot(toBlock.normalize());

    double cosHalfFov = Math.cos(Math.toRadians(viewConeDegrees / 2d));
    return dot >= cosHalfFov;
  }
}
