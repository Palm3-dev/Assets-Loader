package com.palm3.packs_loader.common;

import com.google.common.collect.ImmutableList;
import com.mojang.logging.LogUtils;
import com.palm3.packs_loader.PacksLoaderMain;
import com.palm3.packs_loader.logging.PrettyLogging;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.ArrayList;
import java.util.List;

/**
 * This record is used to add a shutdown task to Minecraft.
 * @param task The task you want to execute.
 * @param taskName The name of the task, to identify it. Giving reasonable and understandable names is recommended.
 * @param callerModId The id of the mod that created the task and for which the task will be called. In short therms, your mod id.
 * @param executedForCrashes If the task should be also executed when the game crashes and the exception doesn't get caught.
 *                          <br><b>NOTE:</b> most of the time the game automatically catches the exception, it's not guaranteed
 *                          that the task will be executed on game crash 100% of the times.
 */
@ParametersAreNonnullByDefault
public record GameClosedTask(Runnable task, String taskName, String callerModId, boolean executedForCrashes) {
    private static final PrettyLogging PL = new PrettyLogging(LogUtils.getLogger(), PacksLoaderMain.DEF_PL_PARAMS);
    private static final List<GameClosedTask> REGISTERED_TASKS = new ArrayList<>();

    /**
     * Constructs an instance of this record.
     * @param task The task you want to execute.
     * @param taskName The name of the task, to identify it. Giving reasonable and understandable names is recommended.
     * @param callerModId The id of the mod that created the task and for which the task will be called. In short therms, your mod id.
     * @param executedForCrashes If the task should be also executed when the game crashes and the exception doesn't get caught.
     *                          <br><b>NOTE:</b> most of the time the game automatically catches the exception, it's not guaranteed
     *                          that the task will be executed on game crash 100% of the times.
     * @throws IllegalArgumentException If the name of the list registered for the given mod id already exists, in other words: a list with same name and mod id is already registered.
     */
    public GameClosedTask(Runnable task, String taskName, String callerModId, boolean executedForCrashes) {
        for (GameClosedTask gameClosedTask : REGISTERED_TASKS) {
            if (gameClosedTask.taskName().equals(taskName) && gameClosedTask.callerModId().equals(callerModId)) {
                throw new IllegalArgumentException("The given task with name '" + taskName + "' and mod id '" + callerModId + "' is already registered!");
            }
        }

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            PL.logI("Executing shutdown task '" + taskName + "' for mod with id '" + callerModId + "'");
            task.run();
        }));

        if (executedForCrashes) {
            Thread.setDefaultUncaughtExceptionHandler((thread, throwable) -> {
                PL.logW("Oops,something exploded! Executing shutdown-designated task '" + taskName + "' for mod with id '" + callerModId + "'");
                task.run();
            });
        }

        this.task = task;
        this.taskName = taskName;
        this.callerModId = callerModId;
        this.executedForCrashes = executedForCrashes;

        PL.logI("Created new shutdown/crash task: " + taskName);
        REGISTERED_TASKS.add(this);
    }

    /**
     * @return An {@link ImmutableList} of all the registered tasks.
     */
    public static List<GameClosedTask> getRegisteredTasks() {
        return ImmutableList.copyOf(REGISTERED_TASKS);
    }

    /**
     * Creates a new {@link GameClosedTask}.
     * @param task The task you want to execute.
     * @param taskName The name of the task, to identify it. Giving reasonable and understandable names is recommended.
     * @param callerModId The id of the mod that created the task and for which the task will be called. In short therms, your mod id.
     * @param executedForCrashes If the task should be also executed when the game crashes and the exception doesn't get caught.
     *                          <br><b>NOTE:</b> most of the time the game automatically catches the exception, it's not guaranteed
     *                          that the task will be executed on game crash 100% of the times.
     * @throws IllegalArgumentException If the name of the list registered for the given mod id already exists, in other words: a list with same name and mod id is already registered.
     */
    public static void create(Runnable task, String taskName, String callerModId, boolean executedForCrashes) {
        new GameClosedTask(task, taskName, callerModId, executedForCrashes);
    }
}