package org.scrolllang.scroll.objects

import net.minecraft.world.level.Level
import net.minecraft.world.phys.Vec3

data class Location(val vector: Vec3, val world: Level) {
	val x: Double get() = vector.x
	val y: Double get() = vector.y
	val z: Double get() = vector.z
}