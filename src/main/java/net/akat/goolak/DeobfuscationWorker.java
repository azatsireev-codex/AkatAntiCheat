package net.akat.goolak;

import net.akat.goolak.antixray.AntiXRayService;
import org.bukkit.block.Block;

public final class DeobfuscationWorker {

  private static final int[][] NEIGHBOR_OFFSETS = {
      {0, 0, 0},
      {1, 0, 0},
      {-1, 0, 0},
      {0, 1, 0},
      {0, -1, 0},
      {0, 0, 1},
      {0, 0, -1}
  };

  private final AntiXRayService antiXRayService;

  public DeobfuscationWorker(AntiXRayService antiXRayService) {
    this.antiXRayService = antiXRayService;
  }

  public void deobfuscateAround(Block origin) {
    for (int[] offset : NEIGHBOR_OFFSETS) {
      this.antiXRayService.refreshBlockForAllPlayers(origin.getRelative(offset[0], offset[1], offset[2]));
    }
  }
}
