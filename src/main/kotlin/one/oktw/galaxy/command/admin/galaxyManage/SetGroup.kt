package one.oktw.galaxy.command.admin.galaxyManage

import kotlinx.coroutines.experimental.launch
import one.oktw.galaxy.Main.Companion.galaxyManager
import one.oktw.galaxy.command.CommandBase
import one.oktw.galaxy.galaxy.data.extensions.getMember
import one.oktw.galaxy.galaxy.data.extensions.setGroup
import one.oktw.galaxy.galaxy.enums.Group
import one.oktw.galaxy.galaxy.enums.Group.ADMIN
import one.oktw.galaxy.galaxy.enums.Group.OWNER
import org.spongepowered.api.Sponge
import org.spongepowered.api.command.CommandResult
import org.spongepowered.api.command.CommandSource
import org.spongepowered.api.command.args.CommandContext
import org.spongepowered.api.command.args.GenericArguments
import org.spongepowered.api.command.spec.CommandSpec
import org.spongepowered.api.entity.living.player.Player
import org.spongepowered.api.entity.living.player.User
import org.spongepowered.api.service.user.UserStorageService
import org.spongepowered.api.text.Text
import org.spongepowered.api.text.format.TextColors.GREEN
import org.spongepowered.api.text.format.TextColors.RED
import java.util.*

class SetGroup : CommandBase {
    override val spec: CommandSpec
        get() = CommandSpec.builder()
            .executor(this)
            .permission("oktw.command.admin.galaxyManage.setGroup")
            .arguments(
                GenericArguments.optionalWeak(GenericArguments.uuid(Text.of("galaxy"))),
                GenericArguments.user(Text.of("member")),
                GenericArguments.enumValue(Text.of("Group"), Group::class.java)
            )
            .build()

    override fun execute(src: CommandSource, args: CommandContext): CommandResult {
        val userStorage = Sponge.getServiceManager().provide(UserStorageService::class.java).get()
        val member = args.getOne<User>("member").get()
        val group = args.getOne<Group>("Group").get()
        var galaxyUUID: UUID? = args.getOne<UUID>("galaxy").orElse(null)
        launch {
            val galaxy = galaxyUUID?.let { galaxyManager.get(it) } ?: (src as? Player)?.world?.let { galaxyManager.get(it) }

            if (galaxyUUID == null) {
                galaxyUUID = galaxy?.uuid
            }

            if (galaxyUUID != null) {
                val traveler = galaxy!!.getMember(member.uniqueId)
                when {
                    traveler == null -> {
                        src.sendMessage(Text.of(RED, "Error: Player is not a member in this galaxy."))
                    }
                    traveler.group == group -> {
                        src.sendMessage(Text.of(RED, "Error: Nothing changed."))
                    }
                    traveler.group == OWNER -> {
                        src.sendMessage(Text.of(RED, "Error: You are removing an owner"))
                    }
                    group == OWNER -> {
                        val oldOwner = userStorage.get(galaxy.members.first { it.group == OWNER }.uuid).get()
                        galaxy.setGroup(oldOwner.uniqueId, ADMIN)
                        galaxy.setGroup(member.uniqueId, OWNER)
                        src.sendMessage(Text.of(GREEN, "Galaxy owner transferred: ${oldOwner.name} -> ${member.name}"))
                    }
                    else -> {
                        galaxy.setGroup(member.uniqueId, group)
                        src.sendMessage(Text.of(GREEN, "Group of ${member.name} in ${galaxy.name} was set to ${group.name}!"))
                    }
                }
            } else {
                src.sendMessage(Text.of(RED, "Not enough argument: galaxy not found or missing."))
            }
        }
        return CommandResult.success()
    }
}
