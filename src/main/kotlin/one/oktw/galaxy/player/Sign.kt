/*
 * OKTW Galaxy Project
 * Copyright (C) 2018-2020
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

package one.oktw.galaxy.player

import net.minecraft.block.entity.SignBlockEntity
import net.minecraft.text.LiteralText
import one.oktw.galaxy.event.annotation.EventListener
import one.oktw.galaxy.event.type.PlayerInteractBlockEvent
import one.oktw.galaxy.event.type.PlayerUpdateSignEvent
import kotlin.math.sign

class Sign {
    @EventListener(sync = true)
    fun onPlayerInteractBlock(event: PlayerInteractBlockEvent) {
        if (event.player.isSneaking) {
            val world = event.player.serverWorld
            val blockHitResult = event.packet.hitY
            val entity = world.getBlockEntity(blockHitResult.blockPos)
            if (entity is SignBlockEntity) {
                val signBlockEntity = entity as? SignBlockEntity ?: return
                event.player.openEditSignScreen(signBlockEntity)
            }
        }
    }

    @EventListener(sync = true)
    fun onPlayerUpdateSign(event: PlayerUpdateSignEvent) {
        val world = event.player.serverWorld
        val entity = world.getBlockEntity(event.packet.pos)
        if (entity != null) {
            val signBlockEntity = entity as? SignBlockEntity ?: return
            for (i in 0..3) {
                val line = event.packet.text[i].replace("&", "§")
                signBlockEntity.setTextOnRow(i, LiteralText(line))
            }
            event.cancel = true
            event.player.networkHandler.sendPacket(signBlockEntity.toUpdatePacket())
        }
    }
}