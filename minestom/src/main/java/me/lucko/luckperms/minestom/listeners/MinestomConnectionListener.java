package me.lucko.luckperms.minestom.listeners;

import me.lucko.luckperms.common.config.ConfigKeys;
import me.lucko.luckperms.common.locale.Message;
import me.lucko.luckperms.common.locale.TranslationManager;
import me.lucko.luckperms.common.model.User;
import me.lucko.luckperms.common.plugin.util.AbstractConnectionListener;
import me.lucko.luckperms.minestom.LPMinestomPlugin;
import net.kyori.adventure.text.Component;
import net.minestom.server.MinecraftServer;
import net.minestom.server.entity.Player;
import net.minestom.server.event.Event;
import net.minestom.server.event.EventNode;
import net.minestom.server.event.player.AsyncPlayerConfigurationEvent;
import net.minestom.server.event.player.AsyncPlayerPreLoginEvent;
import net.minestom.server.event.player.PlayerDisconnectEvent;

import java.util.concurrent.TimeUnit;

public final class MinestomConnectionListener extends AbstractConnectionListener {

    private final LPMinestomPlugin plugin;

    public MinestomConnectionListener(LPMinestomPlugin plugin, EventNode<Event> eventNode) {
        super(plugin);
        this.plugin = plugin;

        eventNode.addListener(AsyncPlayerPreLoginEvent.class, this::onPlayerPreLogin);
        eventNode.addListener(AsyncPlayerConfigurationEvent.class, this::onPlayerLogin);
        eventNode.addListener(PlayerDisconnectEvent.class, this::onPlayerDisconnect);
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    private void onPlayerPreLogin(AsyncPlayerPreLoginEvent event) {
        try {
            this.plugin.getBootstrap().getEnableLatch().await(60, TimeUnit.SECONDS);
        } catch (InterruptedException exception) {
            exception.printStackTrace(System.err);
        }

        Player player = event.getConnection().getPlayer();
        if (player == null) {
            this.plugin.getLogger().warn("Player is null for " + event.getGameProfile().uuid() + " - " + event.getGameProfile().name() + " - denying login.");
            return;
        }

        if (this.plugin.getConfiguration().get(ConfigKeys.DEBUG_LOGINS)) {
            this.plugin.getLogger().info("Processing pre-login for " + player.getUuid() + " - " + player.getUsername());
        }

        if (!player.isOnline()) {
            this.plugin.getLogger().info("Another plugin has cancelled the connection for " + player.getUuid() + " - " + player.getUsername() + ". No permissions data will be loaded.");
            return;
        }

        try {
            User user = loadUser(player.getUuid(), player.getUsername());
            recordConnection(player.getUuid());
            this.plugin.getEventDispatcher().dispatchPlayerLoginProcess(player.getUuid(), player.getUsername(), user);
        } catch (Exception ex) {
            this.plugin.getLogger().severe("Exception occurred whilst loading data for " + player.getUuid() + " - " + player.getUsername(), ex);

            Component reason = TranslationManager.render(Message.LOADING_DATABASE_ERROR.build());
            player.kick(reason);
            this.plugin.getEventDispatcher().dispatchPlayerLoginProcess(player.getUuid(), player.getUsername(), null);
        }
    }

    private void onPlayerLogin(AsyncPlayerConfigurationEvent event) {
        final Player player = event.getPlayer();

        if (this.plugin.getConfiguration().get(ConfigKeys.DEBUG_LOGINS)) {
            this.plugin.getLogger().info("Processing login for " + player.getUuid() + " - " + player.getName());
        }

        final User user = this.plugin.getUserManager().getIfLoaded(player.getUuid());

        if (user == null) {
            if (!getUniqueConnections().contains(player.getUuid())) {
                this.plugin.getLogger().warn("User " + player.getUuid() + " - " + player.getName() +
                        " doesn't have data pre-loaded, they have never been processed during pre-login in this session." +
                        " - denying login.");
            } else {
                this.plugin.getLogger().warn("User " + player.getUuid() + " - " + player.getName() +
                        " doesn't currently have data pre-loaded, but they have been processed before in this session." +
                        " - denying login.");
            }

            Component reason = TranslationManager.render(Message.LOADING_STATE_ERROR.build(), player.getLocale());
            player.kick(reason);
            return;
        }

        this.plugin.getContextManager().signalContextUpdate(player);
    }

    private void onPlayerDisconnect(PlayerDisconnectEvent event) {
        final Player player = event.getPlayer();
        handleDisconnect(player.getUuid());

        MinecraftServer.getSchedulerManager().scheduleNextTick(() -> this.plugin.getContextManager().onPlayerQuit(player));
    }

}
