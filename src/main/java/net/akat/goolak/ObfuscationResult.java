package net.akat.goolak;

import java.util.Map;
import net.akat.goolak.antixray.BlockPos;
import org.bukkit.block.data.BlockData;

public record ObfuscationResult(Map<BlockPos, BlockData> replacements) {

  public boolean isEmpty() {
    return this.replacements.isEmpty();
  }
}
