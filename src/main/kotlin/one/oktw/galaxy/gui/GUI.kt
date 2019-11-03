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

package one.oktw.galaxy.gui

import net.minecraft.container.Container
import net.minecraft.container.ContainerType
import net.minecraft.container.ContainerType.*
import net.minecraft.container.NameableContainerProvider
import net.minecraft.container.SlotActionType
import net.minecraft.container.SlotActionType.*
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.entity.player.PlayerInventory
import net.minecraft.inventory.BasicInventory
import net.minecraft.inventory.Inventory
import net.minecraft.item.ItemStack
import net.minecraft.text.Text
import java.util.concurrent.ConcurrentHashMap

class GUI(private val type: ContainerType<out Container>, private val title: Text) : NameableContainerProvider {
    companion object {
        private val genericContainerType = listOf(GENERIC_9X1, GENERIC_9X2, GENERIC_9X3, GENERIC_9X4, GENERIC_9X5, GENERIC_9X6)
    }

    private val inventory: Inventory = when (type) {
        GENERIC_9X1 -> BasicInventory(9)
        GENERIC_9X2 -> BasicInventory(9 * 2)
        GENERIC_9X3 -> BasicInventory(9 * 3)
        GENERIC_9X4 -> BasicInventory(9 * 4)
        GENERIC_9X5 -> BasicInventory(9 * 5)
        GENERIC_9X6 -> BasicInventory(9 * 6)
        GENERIC_3X3 -> BasicInventory(9)
        HOPPER -> BasicInventory(5)
        else -> throw IllegalArgumentException("Unsupported container type: $type")
    }
    private val playerInventoryRange = inventory.invSize until inventory.invSize + 3 * 9
    private val playerHotBarRange = playerInventoryRange.last + 1..playerInventoryRange.last + 1 + 9
    private val bindings = ConcurrentHashMap<Int, (GUI, ItemStack) -> Any>()
    private val rangeBindings = ConcurrentHashMap<Pair<IntRange, IntRange>, (GUI, ItemStack) -> Any>()
    private var allowUseSlot = HashSet<Int>()

    override fun getDisplayName() = title

    override fun createMenu(syncId: Int, playerInventory: PlayerInventory, player: PlayerEntity): Container? {
        return when (type) {
            in genericContainerType -> GenericContainer(syncId, playerInventory)
            GENERIC_3X3 -> Generic3x3Container(syncId, playerInventory)
            HOPPER -> HopperContainer(syncId, playerInventory)
            else -> null
        }
    }

    fun setAllowUse(index: Int, canUse: Boolean) {
        if (index !in 0 until inventory.invSize) throw IndexOutOfBoundsException("Allow use index out of inventory range")

        if (canUse) allowUseSlot.add(index) else allowUseSlot.remove(index)
    }

    fun setAllowUse(x: Int, y: Int, canUse: Boolean) {
        if (!checkRange(x, y)) throw IndexOutOfBoundsException("Allow use index out of inventory range")

        if (canUse) allowUseSlot.add(xyToIndex(x, y)) else allowUseSlot.remove(xyToIndex(x, y))
    }

    fun addBinding(index: Int, function: (GUI, ItemStack) -> Any) {
        if (index !in 0 until inventory.invSize) throw IndexOutOfBoundsException("Binding index out of inventory range")

        bindings[index] = function
    }

    fun addBinding(x: Int, y: Int, function: (GUI, ItemStack) -> Any) {
        if (!checkRange(x, y)) throw IndexOutOfBoundsException("Binding index out of inventory range")

        bindings[x * y] = function
    }

    fun addBinding(xRange: IntRange, yRange: IntRange, function: (GUI, ItemStack) -> Any) {
        if (!checkRange(xRange.first, yRange.first) || !checkRange(xRange.last, yRange.last)) {
            throw IndexOutOfBoundsException("Binding index out of inventory range")
        }

        rangeBindings[Pair(xRange, yRange)] = function
    }

    fun editInventory(block: InventoryEditor.() -> Unit) {
        block.invoke(InventoryEditor(inventory))
        inventory.markDirty()
    }

    private fun xyToIndex(x: Int, y: Int): Int {
        return when (type) {
            in genericContainerType -> y * 9 + x
            GENERIC_3X3 -> y * 3 + x
            HOPPER -> x
            else -> throw IllegalArgumentException("Unsupported container type: $type")
        }
    }

    private fun indexToXY(index: Int): Pair<Int, Int> {
        if (index !in 0 until inventory.invSize) throw IndexOutOfBoundsException(index)

        return when (type) {
            in genericContainerType -> Pair(index % 9, index / 9)
            GENERIC_3X3 -> Pair(index % 3, index / 3)
            HOPPER -> Pair(index, 0)
            else -> throw IllegalArgumentException("Unsupported container type: $type")
        }
    }

    class InventoryEditor(private val inventory: Inventory) {
        fun set(index: Int, item: ItemStack) {
            inventory.setInvStack(index, item)
        }

        fun fill(xRange: IntRange, yRange: IntRange, item: ItemStack) {
            // TODO
            inventory.markDirty()
        }
    }

    private fun checkRange(x: Int, y: Int) = xyToIndex(x, y) in 0 until inventory.invSize

    private inner class GenericContainer(syncId: Int, playerInventory: PlayerInventory) :
        net.minecraft.container.GenericContainer(type, syncId, playerInventory, inventory, inventory.invSize / 9) {

        override fun onSlotClick(slot: Int, button: Int, action: SlotActionType, player: PlayerEntity): ItemStack? {
            // Trigger binding TODO allow binding cancel player change
            if (slot in 0 until inventory.invSize) {
                bindings[slot]?.invoke(this@GUI, inventory.getInvStack(slot).copy())

                indexToXY(slot).let { (x, y) ->
                    rangeBindings.filterKeys { (xRange, yRange) -> x in xRange && y in yRange }.values.forEach {
                        it.invoke(this@GUI, inventory.getInvStack(slot).copy())
                    }
                }
            }

            // Cancel player change inventory
            if (slot < inventory.invSize && slot != -999 && slot !in allowUseSlot) {
                if (action == QUICK_CRAFT) endQuickCraft()
                return null
            }

            return when (action) {
                PICKUP, SWAP, CLONE, THROW, QUICK_CRAFT -> super.onSlotClick(slot, button, action, player)
                QUICK_MOVE -> {
                    if (slot in 0 until inventory.invSize && slot !in allowUseSlot) return null

                    var itemStack = ItemStack.EMPTY
                    val inventorySlot = slotList[slot]

                    if (inventorySlot != null && inventorySlot.hasStack()) {
                        val slotItemStack = inventorySlot.stack
                        itemStack = slotItemStack.copy()

                        // TODO move item to canUse slot
                        if (slot in playerInventoryRange) {
                            if (!insertItem(slotItemStack, playerHotBarRange.first, playerHotBarRange.last, false)) return ItemStack.EMPTY
                        } else if (slot in playerHotBarRange) {
                            if (!insertItem(slotItemStack, playerInventoryRange.first, playerInventoryRange.last, false)) return ItemStack.EMPTY
                        }

                        // clean up empty slot
                        if (slotItemStack.isEmpty) {
                            inventorySlot.stack = ItemStack.EMPTY
                        } else {
                            inventorySlot.markDirty()
                        }
                    }

                    return itemStack
                }
                PICKUP_ALL -> { // Rewrite PICKUP_ALL only take from player inventory. TODO move item from canUse slot
                    if (slot < 0) return null

                    val cursorItemStack = player.inventory.cursorStack
                    val clickSlot = slotList[slot]
                    if (!cursorItemStack.isEmpty && (!clickSlot.hasStack() || !clickSlot.canTakeItems(player))) {
                        val step = if (button == 0) 1 else -1

                        for (tryTime in 0..1) {
                            var index = inventory.invSize
                            while (index >= inventory.invSize && index < slotList.size && cursorItemStack.count < cursorItemStack.maxCount) {
                                val scanSlot = slotList[index]
                                if (scanSlot.hasStack()
                                    && canInsertItemIntoSlot(scanSlot, cursorItemStack, true)
                                    && scanSlot.canTakeItems(player)
                                    && canInsertIntoSlot(cursorItemStack, scanSlot)
                                ) {
                                    val selectItemStack = scanSlot.stack
                                    if (tryTime != 0 || selectItemStack.count != selectItemStack.maxCount) {
                                        val takeCount = (cursorItemStack.maxCount - cursorItemStack.count).coerceAtMost(selectItemStack.count)
                                        val selectItemStack2 = scanSlot.takeStack(takeCount)
                                        cursorItemStack.increment(takeCount)
                                        if (selectItemStack2.isEmpty) {
                                            scanSlot.stack = ItemStack.EMPTY
                                        }

                                        scanSlot.onTakeItem(player, selectItemStack2)
                                    }
                                }
                                index += step
                            }
                        }
                    }

                    this.sendContentUpdates()

                    cursorItemStack
                }
            }
        }
    }

    private inner class Generic3x3Container(syncId: Int, playerInventory: PlayerInventory) :
        net.minecraft.container.Generic3x3Container(syncId, playerInventory, inventory) {
        override fun onSlotClick(slot: Int, button: Int, action: SlotActionType, player: PlayerEntity): ItemStack? {
            if (slot < inventory.invSize && slot != -999 && slot !in allowUseSlot) {
                if (action == QUICK_CRAFT) endQuickCraft()
                return null
            }

            return when (action) {
                PICKUP, SWAP, CLONE, THROW, QUICK_CRAFT -> super.onSlotClick(slot, button, action, player)
                QUICK_MOVE -> {
                    if (slot in 0 until inventory.invSize && slot !in allowUseSlot) return null

                    var itemStack = ItemStack.EMPTY
                    val inventorySlot = slotList[slot]

                    if (inventorySlot != null && inventorySlot.hasStack()) {
                        val slotItemStack = inventorySlot.stack
                        itemStack = slotItemStack.copy()

                        if (slot in playerInventoryRange) {
                            if (!insertItem(slotItemStack, playerHotBarRange.first, playerHotBarRange.last, false)) return ItemStack.EMPTY
                        } else if (slot in playerHotBarRange) {
                            if (!insertItem(slotItemStack, playerInventoryRange.first, playerInventoryRange.last, false)) return ItemStack.EMPTY
                        }

                        // clean up empty slot
                        if (slotItemStack.isEmpty) {
                            inventorySlot.stack = ItemStack.EMPTY
                        } else {
                            inventorySlot.markDirty()
                        }
                    }

                    return itemStack
                }
                PICKUP_ALL -> { // Rewrite PICKUP_ALL only take from player inventory
                    if (slot < 0) return null

                    val cursorItemStack = player.inventory.cursorStack
                    val clickSlot = slotList[slot]
                    if (!cursorItemStack.isEmpty && (!clickSlot.hasStack() || !clickSlot.canTakeItems(player))) {
                        val step = if (button == 0) 1 else -1

                        for (tryTime in 0..1) {
                            var index = inventory.invSize
                            while (index >= inventory.invSize && index < slotList.size && cursorItemStack.count < cursorItemStack.maxCount) {
                                val scanSlot = slotList[index]
                                if (scanSlot.hasStack()
                                    && canInsertItemIntoSlot(scanSlot, cursorItemStack, true)
                                    && scanSlot.canTakeItems(player)
                                    && canInsertIntoSlot(cursorItemStack, scanSlot)
                                ) {
                                    val selectItemStack = scanSlot.stack
                                    if (tryTime != 0 || selectItemStack.count != selectItemStack.maxCount) {
                                        val takeCount = (cursorItemStack.maxCount - cursorItemStack.count).coerceAtMost(selectItemStack.count)
                                        val selectItemStack2 = scanSlot.takeStack(takeCount)
                                        cursorItemStack.increment(takeCount)
                                        if (selectItemStack2.isEmpty) {
                                            scanSlot.stack = ItemStack.EMPTY
                                        }

                                        scanSlot.onTakeItem(player, selectItemStack2)
                                    }
                                }
                                index += step
                            }
                        }
                    }

                    this.sendContentUpdates()

                    cursorItemStack
                }
            }
        }
    }

    private inner class HopperContainer(syncId: Int, playerInventory: PlayerInventory) :
        net.minecraft.container.HopperContainer(syncId, playerInventory, inventory) {
        override fun onSlotClick(slot: Int, button: Int, action: SlotActionType, player: PlayerEntity): ItemStack? {
            if (slot < inventory.invSize && slot != -999 && slot !in allowUseSlot) {
                if (action == QUICK_CRAFT) endQuickCraft()
                return null
            }

            return when (action) {
                PICKUP, SWAP, CLONE, THROW, QUICK_CRAFT -> super.onSlotClick(slot, button, action, player)
                QUICK_MOVE -> {
                    if (slot in 0 until inventory.invSize && slot !in allowUseSlot) return null

                    var itemStack = ItemStack.EMPTY
                    val inventorySlot = slotList[slot]

                    if (inventorySlot != null && inventorySlot.hasStack()) {
                        val slotItemStack = inventorySlot.stack
                        itemStack = slotItemStack.copy()

                        if (slot in playerInventoryRange) {
                            if (!insertItem(slotItemStack, playerHotBarRange.first, playerHotBarRange.last, false)) return ItemStack.EMPTY
                        } else if (slot in playerHotBarRange) {
                            if (!insertItem(slotItemStack, playerInventoryRange.first, playerInventoryRange.last, false)) return ItemStack.EMPTY
                        }

                        // clean up empty slot
                        if (slotItemStack.isEmpty) {
                            inventorySlot.stack = ItemStack.EMPTY
                        } else {
                            inventorySlot.markDirty()
                        }
                    }

                    return itemStack
                }
                PICKUP_ALL -> { // Rewrite PICKUP_ALL only take from player inventory
                    if (slot < 0) return null

                    val cursorItemStack = player.inventory.cursorStack
                    val clickSlot = slotList[slot]
                    if (!cursorItemStack.isEmpty && (!clickSlot.hasStack() || !clickSlot.canTakeItems(player))) {
                        val step = if (button == 0) 1 else -1

                        for (tryTime in 0..1) {
                            var index = inventory.invSize
                            while (index >= inventory.invSize && index < slotList.size && cursorItemStack.count < cursorItemStack.maxCount) {
                                val scanSlot = slotList[index]
                                if (scanSlot.hasStack()
                                    && canInsertItemIntoSlot(scanSlot, cursorItemStack, true)
                                    && scanSlot.canTakeItems(player)
                                    && canInsertIntoSlot(cursorItemStack, scanSlot)
                                ) {
                                    val selectItemStack = scanSlot.stack
                                    if (tryTime != 0 || selectItemStack.count != selectItemStack.maxCount) {
                                        val takeCount = (cursorItemStack.maxCount - cursorItemStack.count).coerceAtMost(selectItemStack.count)
                                        val selectItemStack2 = scanSlot.takeStack(takeCount)
                                        cursorItemStack.increment(takeCount)
                                        if (selectItemStack2.isEmpty) {
                                            scanSlot.stack = ItemStack.EMPTY
                                        }

                                        scanSlot.onTakeItem(player, selectItemStack2)
                                    }
                                }
                                index += step
                            }
                        }
                    }

                    this.sendContentUpdates()

                    cursorItemStack
                }
            }
        }
    }
}
