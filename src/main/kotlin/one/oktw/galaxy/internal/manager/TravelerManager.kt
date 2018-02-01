package one.oktw.galaxy.internal.manager

import com.mongodb.client.model.Filters.eq
import com.mongodb.client.model.UpdateOptions
import kotlinx.coroutines.experimental.launch
import one.oktw.galaxy.Main.Companion.databaseManager
import one.oktw.galaxy.Main.Companion.galaxyManager
import one.oktw.galaxy.internal.types.Position
import one.oktw.galaxy.internal.types.Traveler
import org.spongepowered.api.entity.living.player.Player

class TravelerManager {
    private val travelerCollation = databaseManager.database.getCollection("Traveler", Traveler::class.java)

    private fun createTraveler(player: Player): Traveler {
        val traveler = Traveler(player.uniqueId, position = Position().fromPosition(player.location.position))
        traveler.save()
        return traveler
    }

    fun getTraveler(player: Player): Traveler {
        return travelerCollation.find(eq("uuid", player.uniqueId)).first() ?: createTraveler(player)
    }

    fun saveTraveler(traveler: Traveler) {
        launch { travelerCollation.replaceOne(eq("uuid", traveler.uuid), traveler, UpdateOptions().upsert(true)) }
    }

    fun updateTraveler(player: Player) {
        val traveler = getTraveler(player)

        traveler.position.fromPosition(player.location.position)
        traveler.position.planet = galaxyManager.getPlanet(player.world.uniqueId)?.uuid

        traveler.save()
    }
}
