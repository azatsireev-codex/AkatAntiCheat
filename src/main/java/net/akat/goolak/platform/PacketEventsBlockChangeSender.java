package net.akat.goolak.platform;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.logging.Level;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

public final class PacketEventsBlockChangeSender implements BlockChangeSender {

  private final JavaPlugin plugin;
  private final Method getApiMethod;
  private final Method getPlayerManagerMethod;
  private final Method sendPacketMethod;
  private final Constructor<?> packetConstructor;
  private final Constructor<?> blockPositionConstructor;
  private final Method wrappedStateByStringMethod;

  private PacketEventsBlockChangeSender(JavaPlugin plugin, Method getApiMethod, Method getPlayerManagerMethod,
      Method sendPacketMethod, Constructor<?> packetConstructor, Constructor<?> blockPositionConstructor,
      Method wrappedStateByStringMethod) {
    this.plugin = plugin;
    this.getApiMethod = getApiMethod;
    this.getPlayerManagerMethod = getPlayerManagerMethod;
    this.sendPacketMethod = sendPacketMethod;
    this.packetConstructor = packetConstructor;
    this.blockPositionConstructor = blockPositionConstructor;
    this.wrappedStateByStringMethod = wrappedStateByStringMethod;
  }

  public static BlockChangeSender create(JavaPlugin plugin) {
    try {
      Plugin packetEventsPlugin = Bukkit.getPluginManager().getPlugin("packetevents");
      if (packetEventsPlugin == null || !packetEventsPlugin.isEnabled()) {
        plugin.getLogger().warning("PacketEvents is not installed/enabled. Falling back to Bukkit sendBlockChange.");
        return new BukkitBlockChangeSender();
      }

      Class<?> packetEventsClass = Class.forName("com.github.retrooper.packetevents.PacketEvents");
      Method getApiMethod = packetEventsClass.getMethod("getAPI");

      Object api = invokeMethod(getApiMethod, null);
      Method getPlayerManagerMethod = api.getClass().getMethod("getPlayerManager");
      Object playerManager = invokeMethod(getPlayerManagerMethod, api);
      Method sendPacketMethod = findSendPacketMethod(playerManager.getClass());

      Class<?> blockChangePacketClass =
          Class.forName("com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerBlockChange");
      Class<?> wrappedStateClass =
          Class.forName("com.github.retrooper.packetevents.protocol.world.states.WrappedBlockState");
      Method wrappedStateByStringMethod = wrappedStateClass.getMethod("getByString", String.class);

      Constructor<?> blockPositionConstructor = findBlockPositionConstructor();
      Constructor<?> packetConstructor = blockChangePacketClass.getConstructor(blockPositionConstructor.getDeclaringClass(),
          wrappedStateClass);

      plugin.getLogger().info("PacketEvents block-change sender enabled.");
      return new PacketEventsBlockChangeSender(plugin, getApiMethod, getPlayerManagerMethod, sendPacketMethod,
          packetConstructor, blockPositionConstructor, wrappedStateByStringMethod);
    } catch (ClassNotFoundException | NoSuchMethodException | IllegalAccessException | InvocationTargetException
        exception) {
      plugin.getLogger().log(Level.WARNING,
          "Failed to initialize PacketEvents sender. Falling back to Bukkit sendBlockChange.", exception);
      return new BukkitBlockChangeSender();
    }
  }

  @Override
  public void sendBlockChange(Player player, Location location, BlockData blockData) {
    try {
      Object api = invokeMethod(this.getApiMethod, null);
      Object playerManager = invokeMethod(this.getPlayerManagerMethod, api);

      Object blockPosition = this.blockPositionConstructor.newInstance(location.getBlockX(), location.getBlockY(),
          location.getBlockZ());
      String blockStateName = normalizeStateName(blockData.getAsString(false));
      Object wrappedState = invokeMethod(this.wrappedStateByStringMethod, null, blockStateName);
      Object packet = this.packetConstructor.newInstance(blockPosition, wrappedState);

      invokeMethod(this.sendPacketMethod, playerManager, player, packet);
    } catch (InstantiationException | IllegalAccessException | InvocationTargetException exception) {
      this.plugin.getLogger().log(Level.FINE,
          "PacketEvents send failed for " + player.getName() + ". Falling back to Bukkit sendBlockChange.", exception);
      player.sendBlockChange(location, blockData);
    }
  }



  private static Method findSendPacketMethod(Class<?> playerManagerClass) throws NoSuchMethodException {
    for (Method method : playerManagerClass.getMethods()) {
      if (!method.getName().equals("sendPacket") || method.getParameterCount() != 2) {
        continue;
      }

      Class<?> firstParameter = method.getParameterTypes()[0];
      if (firstParameter.isAssignableFrom(Player.class) || Player.class.isAssignableFrom(firstParameter)
          || firstParameter == Object.class) {
        return method;
      }
    }

    for (Method method : playerManagerClass.getDeclaredMethods()) {
      if (!method.getName().equals("sendPacket") || method.getParameterCount() != 2) {
        continue;
      }
      return method;
    }

    throw new NoSuchMethodException("No compatible sendPacket method found on " + playerManagerClass.getName());
  }

  private static Object invokeMethod(Method method, Object instance, Object... args)
      throws InvocationTargetException, IllegalAccessException {
    try {
      return method.invoke(instance, args);
    } catch (IllegalAccessException exception) {
      method.setAccessible(true);
      return method.invoke(instance, args);
    }
  }

  private static Constructor<?> findBlockPositionConstructor() throws ClassNotFoundException, NoSuchMethodException {
    try {
      Class<?> blockPositionClass = Class.forName("com.github.retrooper.packetevents.protocol.world.BlockPosition");
      return blockPositionClass.getConstructor(int.class, int.class, int.class);
    } catch (ClassNotFoundException exception) {
      Class<?> vector3iClass = Class.forName("com.github.retrooper.packetevents.util.Vector3i");
      return vector3iClass.getConstructor(int.class, int.class, int.class);
    }
  }

  private static String normalizeStateName(String data) {
    if (data.startsWith("minecraft:")) {
      return data;
    }
    return "minecraft:" + data;
  }

  private static final class BukkitBlockChangeSender implements BlockChangeSender {

    @Override
    public void sendBlockChange(Player player, Location location, BlockData blockData) {
      player.sendBlockChange(location, blockData);
    }
  }
}
