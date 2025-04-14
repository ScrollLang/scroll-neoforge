package org.scrolllang.scroll

import io.github.syst3ms.skriptparser.lang.SkriptEvent
import io.github.syst3ms.skriptparser.lang.TriggerContext
import io.github.syst3ms.skriptparser.registration.SkriptAddon
import io.github.syst3ms.skriptparser.registration.SkriptRegistration

class ScrollRegistration(
	private val registerer: ScrollAddon
) : SkriptRegistration(Scroll.instance) {

	override fun getRegisterer(): SkriptAddon {
		throw UnsupportedOperationException()
	}

	fun getAddonRegisterer(): ScrollAddon {
		return registerer
	}

	/**
	 * Must use [Scroll.newEvent]
	 */
	override fun <E : SkriptEvent> newEvent(c: Class<E>, vararg patterns: String): EventRegistrar<E> {
		throw UnsupportedOperationException()
	}

	/**
	 * Must use [Scroll.newEvent]
	 */
	override fun addEvent(
		c: Class<out SkriptEvent>,
		handledContexts: Array<Class<out TriggerContext>>,
		vararg patterns: String
	) {
		throw UnsupportedOperationException()
	}

	/**
	 * Must use [Scroll.newEvent]
	 */
	override fun addEvent(
		c: Class<out SkriptEvent>,
		handledContexts: Array<Class<out TriggerContext>>,
		priority: Int,
		vararg patterns: String
	) {
		throw UnsupportedOperationException()
	}
}