/*
 * CarbonChat
 *
 * Copyright (c) 2021 Josua Parks (Vicarious)
 *                    Contributors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package net.draycia.carbon.fabric.mixin;

import cloud.commandframework.types.tuples.Pair;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Predicate;
import net.draycia.carbon.api.CarbonChatProvider;
import net.draycia.carbon.api.users.CarbonPlayer;
import net.draycia.carbon.api.users.ComponentPlayerResult;
import net.draycia.carbon.fabric.callback.PlayerStatusMessageEvents;
import net.draycia.carbon.fabric.chat.MessageRecipientFilter;
import net.kyori.adventure.platform.fabric.FabricServerAudiences;
import net.minecraft.network.Connection;
import net.minecraft.network.chat.ChatType;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.PlayerChatMessage;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.PlayerList;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PlayerList.class)
abstract class PlayerListMixin {

    @Shadow @Final private MinecraftServer server;

    @Shadow
    public abstract void broadcastSystemMessage(Component component, boolean bool);

    @Shadow @Final private List<ServerPlayer> players;

    public Map<Thread, Pair<Component, Boolean>> carbon$joinMsg = new ConcurrentHashMap<>();

    @Redirect(
        method = "placeNewPlayer(Lnet/minecraft/network/Connection;Lnet/minecraft/server/level/ServerPlayer;)V",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/server/players/PlayerList;broadcastSystemMessage(Lnet/minecraft/network/chat/Component;Z)V"
        )
    )
    public void redirectJoinMessage(final PlayerList instance, final Component component, final boolean bool) {
        // move to after player is added to playerlist and world
        this.carbon$joinMsg.put(Thread.currentThread(), Pair.of(component, bool));
    }

    @Inject(
        method = "placeNewPlayer(Lnet/minecraft/network/Connection;Lnet/minecraft/server/level/ServerPlayer;)V",
        at = @At("RETURN")
    )
    public void injectJoin(final Connection connection, final ServerPlayer serverPlayer, final CallbackInfo ci) {
        final @Nullable Pair<Component, Boolean> remove = this.carbon$joinMsg.remove(Thread.currentThread());
        if (remove != null) {
            final PlayerStatusMessageEvents.MessageEvent event = PlayerStatusMessageEvents.MessageEvent.of(
                serverPlayer, remove.getFirst().asComponent()
            );
            PlayerStatusMessageEvents.JOIN_MESSAGE.invoker().onMessage(event);
            final net.kyori.adventure.text.@Nullable Component message = event.message();
            if (message != null) {
                this.broadcastSystemMessage(FabricServerAudiences.of(this.server).toNative(message), remove.getSecond());
            }
        }
    }

    @Redirect(
        method = "broadcastChatMessage(Lnet/minecraft/network/chat/PlayerChatMessage;Ljava/util/function/Predicate;Lnet/minecraft/server/level/ServerPlayer;Lnet/minecraft/network/chat/ChatType$Bound;)V",
        at = @At(
            value = "FIELD",
            target = "Lnet/minecraft/server/players/PlayerList;players:Ljava/util/List;"
        )
    )
    public List<ServerPlayer> redirectPlayers(
        final PlayerList instance,
        final PlayerChatMessage playerChatMessage,
        final Predicate<ServerPlayer> predicate,
        final @Nullable ServerPlayer serverPlayer,
        final ChatType.Bound bound
    ) {
        final ComponentPlayerResult<? extends CarbonPlayer> result = CarbonChatProvider.carbonChat().server().userManager().carbonPlayer(serverPlayer.getUUID()).join();
        final MessageRecipientFilter filter = new MessageRecipientFilter(serverPlayer, result.player().selectedChannel());
        return this.players.stream().filter(player -> !filter.shouldFilterMessageTo(player)).toList();
    }

}
