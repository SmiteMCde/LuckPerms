package me.lucko.luckperms.minestom;

import me.lucko.luckperms.common.plugin.bootstrap.LuckPermsBootstrap;
import me.lucko.luckperms.common.plugin.scheduler.AbstractJavaScheduler;
import me.lucko.luckperms.common.plugin.scheduler.SchedulerAdapter;
import net.minestom.server.MinecraftServer;

import java.util.concurrent.Executor;

public final class MinestomSchedulerAdapter extends AbstractJavaScheduler implements SchedulerAdapter {

    private final Executor sync;

    public MinestomSchedulerAdapter(LuckPermsBootstrap bootstrap) {
        super(bootstrap);
        this.sync = r -> MinecraftServer.getSchedulerManager().scheduleNextProcess(r);
    }

    @Override
    public Executor sync() {
        return this.sync;
    }

}
