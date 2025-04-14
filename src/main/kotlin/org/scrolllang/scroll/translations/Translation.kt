package org.scrolllang.scroll.translations

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.minimessage.MiniMessage
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer
import org.scrolllang.scroll.Scroll

object Translation {

	private val plainTextSerializer: PlainTextComponentSerializer = PlainTextComponentSerializer.plainText()
	private val miniMessage: MiniMessage = MiniMessage.miniMessage()

	enum class KeyCategory { CONFIGURATION, LANGUAGE, SCROLL, SCRIPT, FILES, USER }

	fun configurationKey(key: String, vararg args: Any): String = plainText(KeyCategory.CONFIGURATION, key, args)
	fun languageKey(key: String, vararg args: Any): String = plainText(KeyCategory.LANGUAGE, key, args)
	fun userMessageKey(key: String, vararg args: Any): String = plainText(KeyCategory.USER, key, args)
	fun scrollKey(key: String, vararg args: Any): String = plainText(KeyCategory.SCROLL, key, args)
	fun scriptKey(key: String, vararg args: Any): String = plainText(KeyCategory.SCRIPT, key, args)
	fun filesKey(key: String, vararg args: Any): String = plainText(KeyCategory.FILES, key, args)

	private fun translationKey(category: KeyCategory?, key: String): String = "${category?.name?.plus(".") ?: ""}$key".lowercase()

	fun component(category: KeyCategory?, key: String, vararg replacements: Any?): Component {
	    val translationKey = translationKey(category, key)
	    val tags = replacements.mapIndexed { i, value ->
	        when (value) {
	            is Component -> Placeholder.component("$i", value)
	            is TagResolver -> value
	            else -> Placeholder.unparsed("$i", value.toString())
	        }
	    }
		val message = Scroll.language.get(translationKey, translationKey)
	    return if (tags.isEmpty()) miniMessage.deserialize(message)
	    else miniMessage.deserialize(message, *tags.toTypedArray())
	}

	fun plainText(key: String): String = plainText(null, key, emptyArray<Any>())
	fun plainText(component: Component): String =  plainTextSerializer.serialize(component)
	fun plainText(type: KeyCategory? = null, key: String, vararg replacements: Any): String =
		plainText(component(type, key, *replacements))

}
