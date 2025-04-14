package org.scrolllang.scroll.elements

import io.github.syst3ms.skriptparser.registration.SkriptRegistration
import net.minecraft.core.registries.Registries
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.level.Level
import net.neoforged.neoforge.server.ServerLifecycleHooks
import org.scrolllang.scroll.extensions.toResourceLocation
import java.util.UUID

object ServerTypes {

	fun register(registration: SkriptRegistration) {
		registration.newType(ServerPlayer::class.java, "serverplayer", "player@s")
			.toStringFunction { it.name.string }
			.literalParser { input ->
				ServerLifecycleHooks.getCurrentServer()?.let { server ->
					runCatching { UUID.fromString(input) }
						.mapCatching { server.playerList.getPlayer(it) }
						.getOrNull() ?: server.playerList.getPlayerByName(input)
				}
			}
			.defaultChanger(DefaultChangers.PLAYER)
			.register()

		registration.newType(Level::class.java, "world", "(world|[server] level)@s")
			.literalParser { input ->
				ServerLifecycleHooks.getCurrentServer()?.registryAccess()
					?.lookupOrThrow(Registries.DIMENSION)
					?.firstOrNull { it.dimension().location() == input.toResourceLocation() }
			}
			.toStringFunction { it.dimension().location().toString() }
			.register()
	}

}
