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

package me.lucko.luckperms.minestom;

import me.lucko.luckperms.common.config.generic.adapter.EnvironmentVariableConfigAdapter;
import me.lucko.luckperms.common.config.generic.adapter.MultiConfigurationAdapter;
import net.kyori.adventure.text.Component;
import net.luckperms.api.LuckPerms;
import net.luckperms.api.context.DefaultContextKeys;
import net.luckperms.api.context.ImmutableContextSet;
import net.luckperms.api.node.Node;
import net.minestom.server.MinecraftServer;
import net.minestom.server.command.CommandManager;
import net.minestom.server.command.builder.Command;
import net.minestom.server.command.builder.arguments.ArgumentString;
import net.minestom.server.command.builder.arguments.ArgumentType;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.event.Event;
import net.minestom.server.event.EventNode;
import net.minestom.server.event.player.AsyncPlayerConfigurationEvent;
import net.minestom.server.event.player.PlayerChatEvent;
import net.minestom.server.event.player.PlayerSpawnEvent;
import net.minestom.server.extras.MojangAuth;
import net.minestom.server.extras.lan.OpenToLAN;
import net.minestom.server.instance.InstanceContainer;
import net.minestom.server.instance.block.Block;
import net.minestom.server.network.ConnectionManager;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public final class MinestomServer {

    public static void main(String[] args) {
        MinecraftServer server = MinecraftServer.init();
        MinecraftServer.setBrandName("LuckPerms Demo");

        // initialize LuckPerms
        Path directory = Path.of("luckperms");
        LuckPerms luckPerms = LuckPermsMinestom.builder(directory)
                .commandRegistry(CommandRegistry.minestom())
                .contextProvider(new DummyContextProvider())
                .configurationAdapter(plugin -> new MultiConfigurationAdapter(plugin,
                        new EnvironmentVariableConfigAdapter(plugin),
                        new HoconConfigurationAdapter(plugin)
                )).permissionSuggestions("test.permission", "test.other")
                .dependencyManager(true)
                .enable();

        // set custom player provider (optional)
        ConnectionManager connectionManager = MinecraftServer.getConnectionManager();
        connectionManager.setPlayerProvider((connection, gameProfile) -> new ExamplePlayer(luckPerms, gameProfile.uuid(), gameProfile.name(), connection));

        // set up Minestom
        InstanceContainer instance = MinecraftServer.getInstanceManager().createInstanceContainer();
        instance.setGenerator(unit -> unit.modifier().fillHeight(0, 40, Block.GRASS_BLOCK));

        EventNode<Event> eventNode = MinecraftServer.getGlobalEventHandler();
        eventNode.addListener(AsyncPlayerConfigurationEvent.class, event -> {
            event.setSpawningInstance(instance);
            event.getPlayer().setRespawnPoint(new Pos(0, 41, 0));
        });

        // set custom chat handling (optional)
        eventNode.addListener(PlayerChatEvent.class, event -> {
            if (!(event.getPlayer() instanceof ExamplePlayer player)) return;
            event.setFormattedMessage(Component.text().append(
                    player.getPrefix(),
                    player.getName(),
                    player.getSuffix(),
                    Component.text(": "),
                    Component.text(event.getRawMessage())
            ).build());
        });

        // example of adding permissions to a player via the custom player class
        eventNode.addListener(PlayerSpawnEvent.class, event -> {
            if (!(event.getPlayer() instanceof ExamplePlayer player)) return;
            player.setPermission(
                    Node.builder("*")
                            //.expiry(10, TimeUnit.SECONDS)
                            .context(
                                    ImmutableContextSet.builder()
                                            .add(DefaultContextKeys.DIMENSION_TYPE_KEY, "minecraft:overworld")
                                            .add("dummy", "true")
                                            .build()
                            ).build(),
                    true
            ).thenAccept(result -> player.sendMessage("Attempted to add permission: " + result));
        });

        // command to check if a player has a permission
        CommandManager commandManager = MinecraftServer.getCommandManager();
        Command command = new Command("test");
        ArgumentString permissionArgument = ArgumentType.String("permission");
        command.addSyntax((sender, context) -> {
            String permission = context.get(permissionArgument);
            if (sender instanceof ExamplePlayer player) sender.sendMessage(player.getPermissionValue(permission).toString());
            else sender.sendMessage("Sender is not a player");
        }, permissionArgument);
        commandManager.register(command);

        // register shutdown hook to delete the temp directory
        MinecraftServer.getSchedulerManager().buildShutdownTask(() -> {
            try {
                LuckPermsMinestom.disable();
                Files.deleteIfExists(directory);
            } catch (IOException ignored) {
                // oh well...
            }
        });

        OpenToLAN.open();
        MojangAuth.init();

        server.start("0.0.0.0", 25565);
    }

}