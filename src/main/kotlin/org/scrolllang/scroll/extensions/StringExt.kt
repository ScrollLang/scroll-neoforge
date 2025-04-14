package org.scrolllang.scroll.extensions

import net.minecraft.resources.ResourceLocation

fun String.toResourceLocation(): ResourceLocation {
    val (namespace, path) = this.replace("\\s".toRegex(), "_").split(":").let {
        if (it.size == 2) it else listOf("minecraft", this)
    }
    return ResourceLocation.fromNamespaceAndPath(namespace, path)
}
