package net.imprex.goolak.platform;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.function.Consumer;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

public final class TaskDispatcher {

  private final JavaPlugin plugin;
  private final boolean folia;

  public TaskDispatcher(JavaPlugin plugin) {
    this.plugin = plugin;
    this.folia = hasMethod(Bukkit.class, "getGlobalRegionScheduler");
  }

  public Object runRepeatingTask(Runnable runnable, long delayTicks, long periodTicks) {
    if (!this.folia) {
      return Bukkit.getScheduler().scheduleSyncRepeatingTask(this.plugin, runnable, delayTicks, periodTicks);
    }

    try {
      Object scheduler = Bukkit.class.getMethod("getGlobalRegionScheduler").invoke(null);
      Method runAtFixedRate = scheduler.getClass()
          .getMethod("runAtFixedRate", org.bukkit.plugin.Plugin.class, Consumer.class, long.class, long.class);
      return runAtFixedRate.invoke(scheduler, this.plugin, (Consumer<Object>) task -> runnable.run(), delayTicks,
          periodTicks);
    } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException exception) {
      throw new IllegalStateException("Failed to schedule Folia task", exception);
    }
  }

  public void cancelTask(Object taskHandle) {
    if (taskHandle == null) {
      return;
    }

    if (taskHandle instanceof Integer taskId) {
      Bukkit.getScheduler().cancelTask(taskId);
      return;
    }

    try {
      taskHandle.getClass().getMethod("cancel").invoke(taskHandle);
    } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException exception) {
      throw new IllegalStateException("Failed to cancel Folia task", exception);
    }
  }

  public void executePlayerTask(Player player, Runnable runnable) {
    if (!this.folia) {
      runnable.run();
      return;
    }

    try {
      Object scheduler = player.getClass().getMethod("getScheduler").invoke(player);
      Method run = scheduler.getClass().getMethod("run", org.bukkit.plugin.Plugin.class, Consumer.class,
          Runnable.class, long.class);
      run.invoke(scheduler, this.plugin, (Consumer<Object>) task -> runnable.run(), null, 0L);
    } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException exception) {
      throw new IllegalStateException("Failed to schedule Folia player task", exception);
    }
  }

  private static boolean hasMethod(Class<?> type, String methodName) {
    for (Method method : type.getMethods()) {
      if (method.getName().equals(methodName)) {
        return true;
      }
    }
    return false;
  }
}
