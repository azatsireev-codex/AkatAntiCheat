package net.akat.goolak.antixray;

import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import org.bukkit.Material;
import org.bukkit.block.data.BlockData;
import org.bukkit.configuration.file.FileConfiguration;

public final class AntiXRayConfig {

  private final boolean enabled;
  private final int scanIntervalTicks;
  private final int chunkRadius;
  private final boolean useClientViewDistance;
  private final int minY;
  private final int maxY;
  private final double viewConeDegrees;
  private final int maxReplacementsPerScan;
  private final int maxRestoresPerScan;
  private final Material replacementMaterial;
  private final BlockData replacementBlockData;
  private final Set<Material> hiddenMaterials;

  private AntiXRayConfig(boolean enabled, int scanIntervalTicks, int chunkRadius, boolean useClientViewDistance,
      int minY, int maxY, double viewConeDegrees, int maxReplacementsPerScan, int maxRestoresPerScan,
      Material replacementMaterial, Set<Material> hiddenMaterials) {
    this.enabled = enabled;
    this.scanIntervalTicks = scanIntervalTicks;
    this.chunkRadius = chunkRadius;
    this.useClientViewDistance = useClientViewDistance;
    this.minY = minY;
    this.maxY = maxY;
    this.viewConeDegrees = viewConeDegrees;
    this.maxReplacementsPerScan = maxReplacementsPerScan;
    this.maxRestoresPerScan = maxRestoresPerScan;
    this.replacementMaterial = replacementMaterial;
    this.replacementBlockData = replacementMaterial.createBlockData();
    this.hiddenMaterials = hiddenMaterials;
  }

  public static AntiXRayConfig from(FileConfiguration config) {
    boolean enabled = config.getBoolean("enabled", true);
    int scanIntervalTicks = Math.max(1, config.getInt("scanIntervalTicks", 20));
    int chunkRadius = Math.max(0, config.getInt("chunkRadius", 1));

    boolean useClientViewDistance = config.getBoolean("useClientViewDistance", true);

    int configuredMinY = config.getInt("minY", -64);
    int configuredMaxY = config.getInt("maxY", 64);
    int minY = Math.min(configuredMinY, configuredMaxY);
    int maxY = Math.max(configuredMinY, configuredMaxY);

    double viewConeDegrees = Math.min(180d, Math.max(1d, config.getDouble("viewConeDegrees", 100d)));
    int maxReplacementsPerScan = Math.max(1, config.getInt("maxReplacementsPerScan", 20000));
    int maxRestoresPerScan = Math.max(1, config.getInt("maxRestoresPerScan", 20000));

    Material replacement = Material.matchMaterial(config.getString("replacementMaterial", "STONE"));
    if (replacement == null || !replacement.isBlock()) {
      replacement = Material.STONE;
    }

    Set<Material> hiddenMaterials = EnumSet.noneOf(Material.class);
    List<String> values = config.getStringList("hiddenMaterials");
    for (String value : values) {
      Material material = Material.matchMaterial(value);
      if (material != null && material.isBlock()) {
        hiddenMaterials.add(material);
      }
    }

    if (hiddenMaterials.isEmpty()) {
      hiddenMaterials.add(Material.DIAMOND_ORE);
      hiddenMaterials.add(Material.DEEPSLATE_DIAMOND_ORE);
    }

    return new AntiXRayConfig(enabled, scanIntervalTicks, chunkRadius, useClientViewDistance, minY, maxY,
        viewConeDegrees, maxReplacementsPerScan, maxRestoresPerScan, replacement, hiddenMaterials);
  }

  public boolean enabled() { return this.enabled; }
  public int scanIntervalTicks() { return this.scanIntervalTicks; }
  public int chunkRadius() { return this.chunkRadius; }
  public boolean useClientViewDistance() { return this.useClientViewDistance; }
  public int minY() { return this.minY; }
  public int maxY() { return this.maxY; }
  public double viewConeDegrees() { return this.viewConeDegrees; }
  public int maxReplacementsPerScan() { return this.maxReplacementsPerScan; }
  public int maxRestoresPerScan() { return this.maxRestoresPerScan; }
  public Material replacementMaterial() { return this.replacementMaterial; }
  public BlockData replacementBlockData() { return this.replacementBlockData; }
  public Set<Material> hiddenMaterials() { return this.hiddenMaterials; }
}
