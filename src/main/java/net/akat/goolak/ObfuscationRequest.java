package net.akat.goolak;

import java.util.Set;
import net.akat.goolak.antixray.AntiXRayConfig;
import net.akat.goolak.antixray.BlockPos;
import org.bukkit.Chunk;
import org.bukkit.entity.Player;

public record ObfuscationRequest(Player player, Chunk chunk, AntiXRayConfig config, Set<BlockPos> alreadyMasked) {
}
