/*
 * This file is part of LuckPerms, licensed under the MIT License.
 *
 *  Copyright (c) lucko (Luck) <luck@lucko.me>
 *  Copyright (c) contributors
 *
 *  Permission is hereby granted, free of charge, to any person obtaining a copy
 *  of this software and associated documentation files (the "Software"), to deal
 *  in the Software without restriction, including without limitation the rights
 *  to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 *  copies of the Software, and to permit persons to whom the Software is
 *  furnished to do so, subject to the following conditions:
 *
 *  The above copyright notice and this permission notice shall be included in all
 *  copies or substantial portions of the Software.
 *
 *  THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 *  IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 *  FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 *  AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 *  LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 *  OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 *  SOFTWARE.
 */

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

        if (this.plugin.getConfiguration().get(ConfigKeys.DEBUG_LOGINS)) {
            this.plugin.getLogger().info("Processing pre-login for " + event.getGameProfile().uuid() + " - " + event.getGameProfile().name());
        }

        if (!event.getConnection().isOnline()) {
            this.plugin.getLogger().info("Another plugin has cancelled the connection for " + event.getGameProfile().name() + " - " + event.getGameProfile().uuid() + ". No permissions data will be loaded.");
            return;
        }

        try {
            User user = loadUser(event.getGameProfile().uuid(), event.getGameProfile().name());
            recordConnection(event.getGameProfile().uuid());
            this.plugin.getEventDispatcher().dispatchPlayerLoginProcess(event.getGameProfile().uuid(), event.getGameProfile().name(), user);
        } catch (Exception ex) {
            this.plugin.getLogger().severe("Exception occurred whilst loading data for " + event.getGameProfile().uuid() + " - " + event.getGameProfile().name(), ex);

            Component reason = TranslationManager.render(Message.LOADING_DATABASE_ERROR.build());
            event.getConnection().kick(reason);
            this.plugin.getEventDispatcher().dispatchPlayerLoginProcess(event.getGameProfile().uuid(), event.getGameProfile().name(), null);
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
