package com.craftmend.openaudiomc.spigot.modules.show.runnables;

import com.craftmend.openaudiomc.OpenAudioMc;
import com.craftmend.openaudiomc.generic.platform.interfaces.TaskService;
import com.craftmend.openaudiomc.generic.redis.packets.ExecuteCommandPacket;
import com.craftmend.openaudiomc.spigot.modules.show.interfaces.ShowRunnable;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import org.bukkit.Bukkit;
import org.bukkit.World;

@AllArgsConstructor
@NoArgsConstructor
    public class CommandRunnable extends ShowRunnable {

    private String command;
    private String worldName;

    @Override
    public void prepare(String serialized, World world) {
        this.command = serialized;
        this.worldName = world.getName();
        if (this.command.startsWith("/")) this.command = this.command.replace("/" , "");
        // trim command
        this.command = this.command.trim();
    }

    @Override
    public String serialize() {
        return command;
    }

    @Override
    public void run() {
        if (!isExecutedFromRedis() && !command.toLowerCase().startsWith("oa show")) new ExecuteCommandPacket(command).send();

        OpenAudioMc.resolveDependency(TaskService.class).runSync(() -> Bukkit.getServer().dispatchCommand(Bukkit.getConsoleSender(), command.trim()));

        /**
        if (worldName == null) {
            OpenAudioMc.resolveDependency(TaskService.class).runSync(() -> Bukkit.getServer().dispatchCommand(Bukkit.getConsoleSender(), command));
        } else {
            Entity executor = getExecutorEntity(worldName);

            if (executor == null) {
                throw new IllegalStateException("There is no entity loaded to execute the show trigger");
            }

            OpenAudioMc.resolveDependency(TaskService.class).runSync(() -> Bukkit.getServer().dispatchCommand(executor, command));
        }
         **/
    }
}
