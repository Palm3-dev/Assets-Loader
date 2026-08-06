package com.palm3.packs_loader.common;

import com.mojang.logging.LogUtils;
import com.palm3.packs_loader.PacksLoaderMain;
import com.palm3.packs_loader.logging.PrettyLogging;

import javax.annotation.ParametersAreNonnullByDefault;

/**
 * This record is used to add a shutdown task to Minecraft.
 * @param task The task you want to execute.
 * @param executeForCrashes If the task should be also executed when the game crashes.
 * @param taskName The name of the task, to identify it. Giving reasonable and understandable names is recommended.
 */
@ParametersAreNonnullByDefault
public record GameClosedTask(Runnable task, boolean executeForCrashes, String taskName) {

    private static final PrettyLogging PL = new PrettyLogging(LogUtils.getLogger(), PacksLoaderMain.DEF_PL_PARAMS);

    /**
     * This record is used to add a shutdown task to Minecraft.
     * @param task The task you want to execute.
     * @param executeForCrashes If the task should be also executed when the game crashes.
     * @param taskName The name of the task, to identify it. Giving reasonable and understandable names is recommended.
     */
    public GameClosedTask(Runnable task, boolean executeForCrashes, String taskName) {
        PL.logI("Created new shutdown/crash task: " + taskName);
        this.task = task;
        this.executeForCrashes = executeForCrashes;
        this.taskName = taskName;

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            PL.logI("Executing shutdown task: " + taskName);
            task.run();
        }));

        if (executeForCrashes) {
            Thread.setDefaultUncaughtExceptionHandler((thread, throwable) -> {
                PL.logW("Oops,something exploded! Executing shutdown-designated task: " + taskName);
                task.run();
            });
        }
    }

    /**
     * This record is used to add a shutdown task to Minecraft.
     * @param task The task you want to execute.
     * @param executeForCrashes If the task should be also executed when the game crashes.
     * @param taskName The name of the task, to identify it. Giving reasonable and understandable names is recommended.
     */
    public static GameClosedTask create(Runnable task, boolean executeForCrashes, String taskName) {
        return new GameClosedTask(task, executeForCrashes, taskName);
    }
}
