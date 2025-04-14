package org.scrolllang.scroll.elements

import org.jetbrains.annotations.Nullable
import org.scrolllang.scroll.utils.collections.CollectionUtils
import io.github.syst3ms.skriptparser.types.changers.ChangeMode
import io.github.syst3ms.skriptparser.types.changers.Changer
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.Entity.RemovalReason
import net.minecraft.world.entity.player.Player

object DefaultChangers {

	val ENTITY = object : Changer<Entity> {
		override fun acceptsChange(mode: ChangeMode): Array<Class<*>>? {
			return when (mode) {
				// TODO
				// ChangeMode.ADD -> CollectionUtils.array(ItemType::class.java, Inventory::class.java, Experience::class.java)
				// ChangeMode.DELETE -> CollectionUtils.array()
				// ChangeMode.REMOVE -> CollectionUtils.array(PotionEffectType::class.java, ItemType::class.java, Inventory::class.java)
				// ChangeMode.REMOVE_ALL -> CollectionUtils.array(PotionEffectType::class.java, ItemType::class.java)
				// ChangeMode.SET, ChangeMode.RESET -> null // REMIND reset entity? (unshear, remove held item, reset weapon/armour, ...)
				else -> null
			}
		}

		override fun change(entities: Array<Entity>, delta: Array<Any?>, mode: ChangeMode) {
			// TODO
		}
	}

	val PLAYER = object : Changer<Player> {
		@Nullable
		override fun acceptsChange(mode: ChangeMode): Array<Class<*>>? {
			return if (mode == ChangeMode.DELETE) null else ENTITY.acceptsChange(mode)
		}

		override fun change(players: Array<Player>, delta: Array<Any?>, mode: ChangeMode) {
			ENTITY.change(players, delta, mode)
		}
	}

	val NON_LIVING_ENTITY = object : Changer<Entity> {
		@Nullable
		override fun acceptsChange(mode: ChangeMode): Array<Class<*>>? {
			return if (mode == ChangeMode.DELETE) CollectionUtils.array() else null
		}

		override fun change(entities: Array<Entity>, delta: Array<Any?>, mode: ChangeMode) {
			require(mode == ChangeMode.DELETE)
			for (entity in entities) {
				if (entity is Player) continue
				entity.remove(RemovalReason.DISCARDED)
			}
		}
	}

}
