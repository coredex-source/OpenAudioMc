package com.craftmend.openaudiomc.spigot.services.scheduling;

import com.craftmend.openaudiomc.OpenAudioMc;
import com.craftmend.openaudiomc.generic.logging.OpenAudioLogger;
import com.craftmend.openaudiomc.generic.platform.interfaces.TaskService;
import com.craftmend.openaudiomc.spigot.OpenAudioMcSpigot;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public class SpigotTaskService implements TaskService {

    private final ConcurrentHashMap<Integer, ScheduledFuture<?>> taskMap = new ConcurrentHashMap<>();
    private final ScheduledExecutorService executor = Executors.newScheduledThreadPool(4);
    private int taskIdCounter = 0;

    private int getNextTaskId() {
        return ++taskIdCounter;
    }

    @Override
    public int scheduleAsyncRepeatingTask(Runnable runnable, int delayUntilFirst, int tickInterval) {
        if (OpenAudioMc.getInstance().isDisabled()) {
            runnable.run();
            return -1;
        }

        int taskId = getNextTaskId();
        ScheduledFuture<?> future = executor.scheduleAtFixedRate(
            runnable,
            delayUntilFirst * 50L, // Convert ticks to milliseconds
            tickInterval * 50L,
            TimeUnit.MILLISECONDS
        );
        taskMap.put(taskId, future);
        return taskId;
    }

    @Override
    public int scheduleSyncRepeatingTask(Runnable runnable, int delayUntilFirst, int tickInterval) {
        if (OpenAudioMc.getInstance().isDisabled()) {
            runnable.run();
            return -1;
        }

        // Try to use Folia's global region scheduler first
        try {
            Object scheduler = Class.forName("io.papermc.paper.threadedregions.scheduler.GlobalRegionScheduler")
                .getMethod("get")
                .invoke(null);
            
            Object task = scheduler.getClass()
                .getMethod("runAtFixedRate", Class.forName("org.bukkit.plugin.Plugin"), Runnable.class, long.class, long.class)
                .invoke(scheduler, OpenAudioMcSpigot.getInstance(), runnable, delayUntilFirst, tickInterval);
            
            int taskId = getNextTaskId();
            return taskId;
        } catch (Exception e) {
            // Fallback to async execution using our executor service
            return scheduleAsyncRepeatingTask(runnable, delayUntilFirst, tickInterval);
        }
    }

    @Override
    public int schduleSyncDelayedTask(Runnable runnable, int delay) {
        if (OpenAudioMc.getInstance().isDisabled()) {
            runnable.run();
            return -1;
        }

        // Try to use Folia's global region scheduler first
        try {
            Object scheduler = Class.forName("io.papermc.paper.threadedregions.scheduler.GlobalRegionScheduler")
                .getMethod("get")
                .invoke(null);
            
            Object task = scheduler.getClass()
                .getMethod("run", Class.forName("org.bukkit.plugin.Plugin"), Runnable.class, long.class)
                .invoke(scheduler, OpenAudioMcSpigot.getInstance(), runnable, delay);
            
            int taskId = getNextTaskId();
            return taskId;
        } catch (Exception e) {
            // Fallback to async execution using our executor service
            int taskId = getNextTaskId();
            ScheduledFuture<?> future = executor.schedule(
                runnable,
                delay * 50L, // Convert ticks to milliseconds
                TimeUnit.MILLISECONDS
            );
            taskMap.put(taskId, future);
            return taskId;
        }
    }

    @Override
    public void cancelRepeatingTask(int id) {
        ScheduledFuture<?> future = taskMap.remove(id);
        if (future != null) {
            future.cancel(false);
        }
    }

    @Override
    public void cancelTask(int id) {
        // Alias for cancelRepeatingTask since we handle all tasks the same way
        cancelRepeatingTask(id);
    }

    @Override
    public int scheduleAsyncDelayedTask(Runnable runnable, int delay) {
        if (OpenAudioMc.getInstance().isDisabled()) {
            runnable.run();
            return -1;
        }

        int taskId = getNextTaskId();
        ScheduledFuture<?> future = executor.schedule(
            runnable,
            delay * 50L, // Convert ticks to milliseconds
            TimeUnit.MILLISECONDS
        );
        taskMap.put(taskId, future);
        return taskId;
    }

    @Override
    public void runAsync(Runnable runnable) {
        if (OpenAudioMc.getInstance().isDisabled()) {
            notifyRunner();
            runnable.run();
            return;
        }

        executor.execute(runnable);
    }

    @Override
    public void runSync(Runnable runnable) {
        if (OpenAudioMc.getInstance().isDisabled()) {
            notifyRunner();
            runnable.run();
            return;
        }

        // Try to use Folia's global region scheduler for immediate execution
        try {
            Object scheduler = Class.forName("io.papermc.paper.threadedregions.scheduler.GlobalRegionScheduler")
                .getMethod("get")
                .invoke(null);
            
            scheduler.getClass()
                .getMethod("execute", Class.forName("org.bukkit.plugin.Plugin"), Runnable.class)
                .invoke(scheduler, OpenAudioMcSpigot.getInstance(), runnable);
        } catch (Exception e) {
            // Fallback to direct execution if global region scheduler fails
            runnable.run();
        }
    }

    /**
     * Shutdown the task service and cleanup resources
     */
    public void shutdown() {
        if (executor != null) {
            executor.shutdown();
        }
        taskMap.clear();
    }
}
