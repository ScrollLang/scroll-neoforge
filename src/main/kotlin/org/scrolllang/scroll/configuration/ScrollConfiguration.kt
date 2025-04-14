package org.scrolllang.scroll.configuration

import net.neoforged.bus.api.SubscribeEvent
import net.neoforged.fml.common.EventBusSubscriber
import net.neoforged.fml.event.config.ModConfigEvent
import net.neoforged.neoforge.common.ModConfigSpec
import org.scrolllang.scroll.Scroll.Companion.MOD_ID
import org.scrolllang.scroll.translations.Translation.configurationKey

/**
 * NOTE: This configuration is loaded on STARTUP, follow this guideline from NeoForge documentation:
 *
 * Configurations registered under the STARTUP type can cause desyncs between the client and server,
 * such as if the configuration is used to disable the registration of content. Therefore,
 * it is highly recommended that any configurations within STARTUP are not used to
 * enable or disable features that may change the content of the mod.
 */
@EventBusSubscriber(modid = MOD_ID, bus = EventBusSubscriber.Bus.MOD)
object ScrollConfiguration {

    private val BUILDER = ModConfigSpec.Builder()

    // Configuration options

    private val LANGUAGE = BUILDER
        .translation("scroll.configuration.language")
        .gameRestart()
        .define("language", "en-US")

    private val DEBUG = BUILDER
        .translation("scroll.configuration.debug")
        .gameRestart()
        .define("debug", false)

    val SPEC: ModConfigSpec = BUILDER.build()

    // Values

    var language: String = "en-US"
        private set

    var debug: Boolean = false
        private set

    @SubscribeEvent
    fun onLoad(event: ModConfigEvent) {
        language = LANGUAGE.get()
        debug = DEBUG.get()
    }

}
