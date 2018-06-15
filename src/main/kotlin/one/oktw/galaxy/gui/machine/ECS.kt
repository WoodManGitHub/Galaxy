package one.oktw.galaxy.gui.machine

import one.oktw.galaxy.Main.Companion.languageService
import one.oktw.galaxy.Main.Companion.main
import one.oktw.galaxy.data.DataUUID
import one.oktw.galaxy.galaxy.planet.data.Planet
import one.oktw.galaxy.gui.Confirm
import one.oktw.galaxy.gui.GUI
import one.oktw.galaxy.gui.GUIHelper
import one.oktw.galaxy.item.enums.ButtonType.*
import one.oktw.galaxy.item.type.Button
import org.spongepowered.api.data.key.Keys
import org.spongepowered.api.entity.living.player.Player
import org.spongepowered.api.event.item.inventory.ClickInventoryEvent
import org.spongepowered.api.event.item.inventory.InteractInventoryEvent
import org.spongepowered.api.item.inventory.Inventory
import org.spongepowered.api.item.inventory.InventoryArchetypes
import org.spongepowered.api.item.inventory.ItemStackSnapshot
import org.spongepowered.api.item.inventory.property.InventoryTitle
import org.spongepowered.api.item.inventory.query.QueryOperationTypes
import org.spongepowered.api.item.inventory.type.GridInventory
import org.spongepowered.api.text.Text
import org.spongepowered.api.text.format.TextColors.*
import org.spongepowered.api.text.format.TextStyles.BOLD
import java.util.*
import java.util.Arrays.asList

class ECS(private val planet: Planet, manage: Boolean = false) : GUI() {
    private val lang = languageService.getDefaultLanguage() // TODO set language
    private val buttonID = Array(2) { UUID.randomUUID() }
    override val token = "ECS-${planet.uuid}${if (manage) "-manage" else ""}"
    override val inventory: Inventory = Inventory.builder()
        .of(InventoryArchetypes.CHEST)
        .property(InventoryTitle.of(Text.of(lang["UI.ECS.Title"])))
        .listener(InteractInventoryEvent::class.java, this::eventProcess)
        .build(main)

    init {
        val inventory = inventory.query<GridInventory>(QueryOperationTypes.INVENTORY_TYPE.of(GridInventory::class.java))

        // Info
        Button(GUI_INFO).createItemStack()
            .apply {
                offer(Keys.DISPLAY_NAME, Text.of(GREEN, BOLD, lang["UI.ECS.Button.info"]))
                offer(
                    Keys.ITEM_LORE, asList(
                        Text.of(BOLD, YELLOW, lang["UI.ECS.Info.Level"], RESET, ":", planet.level),
                        Text.of(BOLD, YELLOW, lang["UI.ECS.Info.Range"], RESET, ":", planet.size),
                        Text.of(BOLD, YELLOW, lang["UI.ECS.Info.Effect"], RESET, ":")
                    ).apply { addAll(planet.effect.map { Text.of(BOLD, it.type.potionTranslation) }) }
                )
            }
            .let { inventory.set(4, 0, it) }

        if (manage) {
            // Upgrade level
            Button(UPGRADE).createItemStack()
                .apply {
                    offer(DataUUID(buttonID[0]))
                    offer(Keys.DISPLAY_NAME, Text.of(GREEN, BOLD, lang["UI.ECS.Button.upgrade"]))
                }
                .let { inventory.set(3, 2, it) }

            // Effect
            Button(PLUS).createItemStack()
                .apply {
                    offer(DataUUID(buttonID[1]))
                    offer(Keys.DISPLAY_NAME, Text.of(GREEN, BOLD, lang["UI.ECS.button.effect"]))
                }
                .let { inventory.set(6, 2, it) }
        }

        GUIHelper.fillEmptySlot(inventory)

        // register event
        registerEvent(InteractInventoryEvent.Close::class.java, this::closeEventListener)
        registerEvent(ClickInventoryEvent::class.java, this::clickEventListener)
    }

    private fun closeEventListener(event: InteractInventoryEvent.Close) {
        event.cursorTransaction.setCustom(ItemStackSnapshot.NONE)
        event.cursorTransaction.isValid = true
    }

    private fun clickEventListener(event: ClickInventoryEvent) {
        event.isCancelled = true

        val itemUUID = event.cursorTransaction.default[DataUUID.key].orElse(null) ?: return

        when (itemUUID) {
            buttonID[0] -> clickUpgrade(event.source as Player)
            buttonID[1] -> clickEffect()
        }
    }

    private fun clickUpgrade(player: Player) {
        GUIHelper.open(player) {
            Confirm(Text.of(lang["UI.ECS.UpgradeConfirm"])) {
                // TODO
            }
        }
    }

    private fun clickEffect() {
        // TODO
    }
}
