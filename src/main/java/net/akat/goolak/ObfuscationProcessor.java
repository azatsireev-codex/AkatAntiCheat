package net.akat.goolak;

import java.util.HashMap;
import java.util.Map;
import net.akat.goolak.antixray.BlockPos;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;

public final class ObfuscationProcessor {

  public ObfuscationResult process(ObfuscationTask task) {
    World world = task.chunk().getWorld();
    int worldMinY = world.getMinHeight();
    int worldMaxY = world.getMaxHeight() - 1;
    int minY = Math.max(worldMinY, task.config().minY());
    int maxY = Math.min(worldMaxY, task.config().maxY());

    int baseX = task.chunk().getX() << 4;
    int baseZ = task.chunk().getZ() << 4;
    int budget = task.config().maxReplacementsPerScan();
    BlockData replacement = task.config().replacementBlockData();

    Map<BlockPos, BlockData> replacements = new HashMap<>();

    for (int y = minY; y <= maxY && budget > 0; y++) {
      for (int localZ = 0; localZ < 16 && budget > 0; localZ++) {
        for (int localX = 0; localX < 16 && budget > 0; localX++) {
          int x = baseX + localX;
          int z = baseZ + localZ;
          Block block = world.getBlockAt(x, y, z);
          Material material = block.getType();
          if (!task.config().hiddenMaterials().contains(material)) {
            continue;
          }

          BlockPos pos = new BlockPos(x, y, z);
          if (task.alreadyMasked().contains(pos)) {
            continue;
          }

          if (!this.shouldObfuscate(world, x, y, z, minY, maxY)) {
            continue;
          }

          replacements.put(pos, replacement);
          budget--;
        }
      }
    }

    return new ObfuscationResult(replacements);
  }

  private boolean shouldObfuscate(World world, int x, int y, int z, int minY, int maxY) {
    return this.isOccluding(world, x, y + 1, z, minY, maxY)
        && this.isOccluding(world, x, y - 1, z, minY, maxY)
        && this.isOccluding(world, x + 1, y, z, minY, maxY)
        && this.isOccluding(world, x - 1, y, z, minY, maxY)
        && this.isOccluding(world, x, y, z + 1, minY, maxY)
        && this.isOccluding(world, x, y, z - 1, minY, maxY);
  }

  private boolean isOccluding(World world, int x, int y, int z, int minY, int maxY) {
    if (y < minY || y > maxY) {
      return false;
    }
    return world.getBlockAt(x, y, z).getType().isOccluding();
  }
}
