package com.craftmend.openaudiomc.spigot.services.utils;

import com.craftmend.openaudiomc.OpenAudioMc;
import com.craftmend.openaudiomc.generic.platform.interfaces.TaskService;
import com.craftmend.openaudiomc.spigot.services.utils.interfaces.Feeder;
import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.function.Consumer;

public class DataWatcher<T> {

    private T value;
    private final int task;
    private Feeder<T> dataFeeder;
    @Getter private Consumer<T> callback;
    private boolean isRunning;
    private boolean forced = false;

    public DataWatcher(JavaPlugin plugin, boolean sync, int delayTicks) {
        Runnable executor = () -> {
            if (this.dataFeeder == null) return;
            T newValue = dataFeeder.feed();
            if (forced || (this.value != null && !newValue.equals(this.value))) this.callback.accept(newValue);
            this.value = newValue;
            forced = false;
        };

        TaskService taskService = OpenAudioMc.resolveDependency(TaskService.class);
        if (sync) {
            this.task = taskService.scheduleSyncRepeatingTask(executor, delayTicks, delayTicks);
        } else {
            this.task = taskService.scheduleAsyncRepeatingTask(executor, delayTicks, delayTicks);
        }

        isRunning = true;
    }

    public DataWatcher<T> setFeeder(Feeder<T> feeder) {
        this.dataFeeder = feeder;
        return this;
    }

    public DataWatcher<T> setTask(Consumer<T> task) {
        this.callback = task;
        return this;
    }

    public void forceTicK() {
        this.forced = true;
    }

    public boolean isRunning() {
        return this.isRunning;
    }

    public void stop() {
        TaskService taskService = OpenAudioMc.resolveDependency(TaskService.class);
        taskService.cancelRepeatingTask(this.task);
        this.isRunning = false;
    }

}
