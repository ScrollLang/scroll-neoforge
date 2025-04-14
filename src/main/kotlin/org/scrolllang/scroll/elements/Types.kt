package org.scrolllang.scroll.elements

import io.github.syst3ms.skriptparser.registration.SkriptRegistration
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.minimessage.MiniMessage
import net.minecraft.commands.CommandSource
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.Item
import net.minecraft.world.item.ItemStack
import net.minecraft.world.phys.Vec3
import org.scrolllang.scroll.Scroll
import org.scrolllang.scroll.extensions.toResourceLocation
import org.scrolllang.scroll.objects.Location
import org.scrolllang.scroll.translations.Translation.plainText
import kotlin.jvm.optionals.getOrNull

object Types {

	init {
		val registration = Scroll.instance.getRegistration()
		registration.apply {
			newType(Entity::class.java, "entity", "entit@y@ies")
				.toStringFunction { it.name.toString() }
				.defaultChanger(DefaultChangers.ENTITY)
				.register()

			newType(LivingEntity::class.java, "livingentity", "livingEntit@y@ies")
				.toStringFunction { it.name.string }
				.defaultChanger(DefaultChangers.ENTITY)
				.register()

			newType(Component::class.java, "text", "(text|component)@s")
				.literalParser { MiniMessage.miniMessage().deserialize(it) }
				.toStringFunction(::plainText)
				.register()

			newType(Player::class.java, "player entity", "player entit@y@ies")
				.toStringFunction { it.displayName.toString() }
				.register()

			newType(ItemStack::class.java, "itemstack", "itemStack@s")
				.toStringFunction { "${it.count} ${plainText("language.of")} ${BuiltInRegistries.ITEM.getKey(it.item).namespace}" }
				.register()

			newType(Vec3::class.java, "vector", "vec(tor|3)@s")
				.toStringFunction { "${it.x},${it.y},${it.z}" }
				.register()

			newType(Location::class.java, "location", "location@s")
				.toStringFunction { "${it.x},${it.y},${it.z},${it.world.dimension().location()}" }
				.register()

			newType(CommandSource::class.java, "commandsource", "commandSource@s").register()

			newType(Item::class.java, "item", "item@s")
				.literalParser { input -> input.toResourceLocation().let(BuiltInRegistries.ITEM::getOptional).getOrNull() }
				.toStringFunction { BuiltInRegistries.ITEM.getKey(it).toString() }
				.register()
		}
	}

}
