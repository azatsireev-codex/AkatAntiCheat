package net.akat.goolak.platform;

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
      if (tryCancelViaScheduledTaskInterface(taskHandle)) {
        return;
      }

      Method cancel = taskHandle.getClass().getDeclaredMethod("cancel");
      cancel.setAccessible(true);
      cancel.invoke(taskHandle);
    } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException | ClassNotFoundException
        exception) {
      throw new IllegalStateException("Failed to cancel Folia task", exception);
    }
  }

  private boolean tryCancelViaScheduledTaskInterface(Object taskHandle)
      throws NoSuchMethodException, InvocationTargetException, IllegalAccessException, ClassNotFoundException {
    Class<?> scheduledTaskType = Class.forName("io.papermc.paper.threadedregions.scheduler.ScheduledTask");
    if (!scheduledTaskType.isInstance(taskHandle)) {
      return false;
    }

    Method cancel = scheduledTaskType.getMethod("cancel");
    cancel.invoke(taskHandle);
    return true;
  }

  public void executePlayerTask(Player player, Runnable runnable) {
    if (!this.folia) {
      runnable.run();
      return;
    }

    try {
      Object scheduler = player.getClass().getMethod("getScheduler").invoke(player);
      if (tryInvokePlayerRun(scheduler, runnable)) {
        return;
      }
      if (tryInvokePlayerExecute(scheduler, runnable)) {
        return;
      }
      throw new NoSuchMethodException("No compatible Folia player scheduler method was found");
    } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException exception) {
      throw new IllegalStateException("Failed to schedule Folia player task", exception);
    }
  }


  public void executePlayerTaskLater(Player player, Runnable runnable, long delayTicks) {
    if (delayTicks <= 0) {
      this.executePlayerTask(player, runnable);
      return;
    }

    if (!this.folia) {
      Bukkit.getScheduler().runTaskLater(this.plugin, runnable, delayTicks);
      return;
    }

    try {
      Object scheduler = player.getClass().getMethod("getScheduler").invoke(player);
      if (tryInvokePlayerRun(scheduler, runnable, delayTicks)) {
        return;
      }
      if (tryInvokePlayerExecute(scheduler, runnable, delayTicks)) {
        return;
      }
      throw new NoSuchMethodException("No compatible Folia player scheduler delayed method was found");
    } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException exception) {
      throw new IllegalStateException("Failed to schedule delayed Folia player task", exception);
    }
  }

  @SuppressWarnings("unchecked")
  private boolean tryInvokePlayerRun(Object scheduler, Runnable runnable)
      throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
    try {
      Method run = scheduler.getClass().getMethod("run", org.bukkit.plugin.Plugin.class, Consumer.class,
          Runnable.class, long.class);
      run.invoke(scheduler, this.plugin, (Consumer<Object>) task -> runnable.run(), null, 0L);
      return true;
    } catch (NoSuchMethodException ignored) {
      // Folia API variant without delay argument.
    }

    try {
      Method run = scheduler.getClass().getMethod("run", org.bukkit.plugin.Plugin.class, Consumer.class,
          Runnable.class);
      run.invoke(scheduler, this.plugin, (Consumer<Object>) task -> runnable.run(), null);
      return true;
    } catch (NoSuchMethodException ignored) {
      return false;
    }
  }

  private boolean tryInvokePlayerExecute(Object scheduler, Runnable runnable)
      throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
    try {
      Method execute = scheduler.getClass().getMethod("execute", org.bukkit.plugin.Plugin.class, Runnable.class,
          Runnable.class, long.class);
      execute.invoke(scheduler, this.plugin, runnable, null, 0L);
      return true;
    } catch (NoSuchMethodException ignored) {
      // Fall through to check other signatures.
    }

    try {
      Method execute = scheduler.getClass().getMethod("execute", org.bukkit.plugin.Plugin.class, Runnable.class,
          Runnable.class);
      execute.invoke(scheduler, this.plugin, runnable, null);
      return true;
    } catch (NoSuchMethodException ignored) {
      return false;
    }
  }


  @SuppressWarnings("unchecked")
  private boolean tryInvokePlayerRun(Object scheduler, Runnable runnable, long delayTicks)
      throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
    try {
      Method run = scheduler.getClass().getMethod("run", org.bukkit.plugin.Plugin.class, Consumer.class,
          Runnable.class, long.class);
      run.invoke(scheduler, this.plugin, (Consumer<Object>) task -> runnable.run(), null, delayTicks);
      return true;
    } catch (NoSuchMethodException ignored) {
      // Fall through to check other signatures.
    }

    return false;
  }

  private boolean tryInvokePlayerExecute(Object scheduler, Runnable runnable, long delayTicks)
      throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
    try {
      Method execute = scheduler.getClass().getMethod("execute", org.bukkit.plugin.Plugin.class, Runnable.class,
          Runnable.class, long.class);
      execute.invoke(scheduler, this.plugin, runnable, null, delayTicks);
      return true;
    } catch (NoSuchMethodException ignored) {
      return false;
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
