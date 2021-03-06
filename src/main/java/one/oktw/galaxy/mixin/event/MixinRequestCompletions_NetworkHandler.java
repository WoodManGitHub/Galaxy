/*
 * OKTW Galaxy Project
 * Copyright (C) 2018-2019
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published
 * by the Free Software Foundation, either version 3 of the License, or
 * any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package one.oktw.galaxy.mixin.event;

import net.minecraft.server.network.ServerPlayNetworkHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.network.packet.RequestCommandCompletionsC2SPacket;
import one.oktw.galaxy.Main;
import one.oktw.galaxy.event.type.RequestCommandCompletionsEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerPlayNetworkHandler.class)
public class MixinRequestCompletions_NetworkHandler {
    @Shadow
    public ServerPlayerEntity player;

    @Inject(method = "onRequestCommandCompletions", at = @At("HEAD"), cancellable = true)
    private void onRequestCommandCompletions(RequestCommandCompletionsC2SPacket packet, CallbackInfo info) {
        Main main = Main.Companion.getMain();
        if (main == null) return;
        if (main.getEventManager().emit(new RequestCommandCompletionsEvent(packet, player)).getCancel()) {
            info.cancel();
        }
    }
}
