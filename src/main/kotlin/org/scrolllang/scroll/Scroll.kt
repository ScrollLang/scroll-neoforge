package org.scrolllang.scroll

import com.mojang.logging.LogUtils
import io.github.syst3ms.skriptparser.Parser
import io.github.syst3ms.skriptparser.lang.Trigger
import io.github.syst3ms.skriptparser.lang.TriggerContext
import io.github.syst3ms.skriptparser.log.ErrorType
import io.github.syst3ms.skriptparser.log.SkriptLogger
import io.github.syst3ms.skriptparser.registration.SkriptAddon
import io.github.syst3ms.skriptparser.registration.SkriptRegistration
import io.github.syst3ms.skriptparser.registration.SkriptRegistration.EventRegistrar
import net.kyori.adventure.platform.modcommon.MinecraftAudiences
import net.kyori.adventure.platform.modcommon.MinecraftClientAudiences
import net.kyori.adventure.platform.modcommon.MinecraftServerAudiences
import net.minecraft.resources.ResourceLocation
import net.neoforged.api.distmarker.Dist
import net.neoforged.bus.api.IEventBus
import net.neoforged.bus.api.SubscribeEvent
import net.neoforged.fml.ModContainer
import net.neoforged.fml.common.EventBusSubscriber
import net.neoforged.fml.common.Mod
import net.neoforged.fml.config.ModConfig
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent
import net.neoforged.fml.loading.FMLPaths
import net.neoforged.neoforge.client.gui.ConfigurationScreen
import net.neoforged.neoforge.client.gui.IConfigScreenFactory
import net.neoforged.neoforge.event.server.ServerStartingEvent
import net.neoforged.neoforge.event.server.ServerStoppingEvent
import org.reflections.Reflections
import org.reflections.scanners.Scanners
import org.reflections.util.FilterBuilder
import org.reflections.vfs.Vfs
import org.scrolllang.scroll.configuration.ScrollConfiguration
import org.scrolllang.scroll.elements.ServerTypes
import org.scrolllang.scroll.elements.Types
import org.scrolllang.scroll.exceptions.EmptyStacktraceException
import org.scrolllang.scroll.language.ScrollEvent
import org.scrolllang.scroll.log.ExceptionPrinter
import org.scrolllang.scroll.translations.Language
import org.scrolllang.scroll.utils.UnionUrlType
import org.scrolllang.scroll.utils.Version
import org.slf4j.Logger
import java.net.URLClassLoader
import java.nio.file.Files
import java.nio.file.Path
import java.util.*
import java.util.stream.Stream

@Mod(Scroll.MOD_ID)
class Scroll(modEventBus: IEventBus, private val dist: Dist, private val container: ModContainer) : SkriptAddon() {

    companion object {
        const val MOD_ID = "scroll"

        @JvmStatic
        lateinit var instance: Scroll
            private set

        @JvmStatic
        lateinit var language: Language
            private set

        @JvmStatic
        var audiences: MinecraftAudiences? = null
            private set

        fun resource(path: String) = ResourceLocation.fromNamespaceAndPath(MOD_ID, path)

        private val GAME_DIRECTORY = FMLPaths.GAMEDIR.get().normalize().toAbsolutePath()
        private val SCROLL_FOLDER = GAME_DIRECTORY.resolve("scroll")

        private val LANGUAGES_FOLDER = SCROLL_FOLDER.resolve("languages")
        private val SCRIPTS_FOLDER = SCROLL_FOLDER.resolve("scripts")
        private val ADDONS_FOLDER = SCROLL_FOLDER.resolve("addons")
        private val LOGGER: Logger = LogUtils.getLogger()
        private val ADDONS = mutableListOf<ScrollAddon>()

        private lateinit var REGISTRATION: SkriptRegistration
        private lateinit var SELF_ADDON: ScrollAddon

        /**
         * Logs an error during the parse time of Scroll scripts.
         * Defaults to `ErrorType.SEMANTIC_ERROR` if not specified.
         *
         * @param message The message to log.
         * @param type The error type (optional, defaults to `ErrorType.SEMANTIC_ERROR`).
         * @param tip A hint for the user (optional).
         */
        fun error(message: String, type: ErrorType = ErrorType.SEMANTIC_ERROR, tip: String? = null) {
            ScrollLoader.currentLogger?.error(message, type, tip) ?: LOGGER.error(message)
        }

        /**
         * Logs informational messages during the parse time of Scroll scripts.
         *
         * @param message The message to log.
         */
        fun info(message: String) {
            ScrollLoader.currentLogger?.info(message) ?: LOGGER.info(message)
        }

        /**
         * Logs warnings during the parse time of Scroll scripts.
         *
         * @param message The message to log.
         * @param tip A hint for the user (optional).
         */
        fun warning(message: String, tip: String? = null) {
            ScrollLoader.currentLogger?.warn(message, tip) ?: LOGGER.warn(message)
        }

        /**
         * Safely prints an exception and extra information to the console.
         *
         * @param throwable The Throwable that caused an error.
         * @param messages Optional messages to print along with the exception.
         * @return An EmptyStacktraceException to throw if code execution should terminate.
         */
        fun printException(throwable: Throwable, vararg messages: String): EmptyStacktraceException {
            return printException(SELF_ADDON, throwable, *messages)
        }

        /**
         * Safely prints an exception and extra information to the console.
         *
         * @param addon The ScrollAddon associated with the error.
         * @param throwable The Throwable that caused an error.
         * @param messages Optional messages to print along with the exception.
         * @return An EmptyStacktraceException to throw if code execution should terminate.
         */
        fun printException(addon: ScrollAddon, throwable: Throwable, vararg messages: String): EmptyStacktraceException {
            return ExceptionPrinter(addon, throwable, *messages).print(LOGGER)
        }
    }

    private val version = Version(container.modInfo.version.toString())

    init {
        instance = this
        listOf(SCROLL_FOLDER, ADDONS_FOLDER, SCRIPTS_FOLDER, LANGUAGES_FOLDER).forEach { Files.createDirectories(it) }
        SELF_ADDON = object : ScrollAddon(this, MOD_ID) {
            override fun startRegistration(registration: ScrollRegistration) {}
            override fun getDataFolder() = SCROLL_FOLDER
            override fun initAddon() {}
        }

        container.registerConfig(ModConfig.Type.STARTUP, ScrollConfiguration.SPEC, "scroll-configuration.toml")
        language = Language(this)

        val registrationLogger = SkriptLogger(ScrollConfiguration.debug)
        REGISTRATION = SkriptRegistration(this, registrationLogger)
        ADDONS.add(SELF_ADDON)
    }

    fun register() {
        // Add NeoForge union URL type to Reflections for skript-parser to use.
        Vfs.addDefaultURLTypes(UnionUrlType())

        // Initialize skript-parser
        Parser.init(emptyArray(), emptyArray(), emptyArray(), true, SCROLL_FOLDER, Scroll::class.java.classLoader)

        //TypeManager.register(REGISTRATION)
        val filter = FilterBuilder()
        Stream.of(
            "org.scrolllang.scroll.elements",
            //"org.scrolllang.scroll.commands",
        ).forEach { filter.includePackage(it) }

        container.modInfo.owningFile.file.scanResult.classes.forEach {
            val className = it.clazz.className
            if (!filter.test(className)) return@forEach
            Class.forName(className, true, Scroll::class.java.classLoader)
        }

        val calendar = Calendar.getInstance()
        Parser.printLogs(REGISTRATION.register(), calendar, true)
        ADDONS.sortedByDescending { it.priority }.forEach { addon ->
            ScrollRegistration(addon).apply {
                addon.startRegistration(this)
                Parser.printLogs(register(), calendar, true)
            }
        }

        ScrollLoader.loadScriptsAtDirectory(SCRIPTS_FOLDER)
        // TODO Handle triggers not being cleared after a reload.
    }

    @SubscribeEvent
    fun onServerStarting(event: ServerStartingEvent) {
        ServerTypes.register(REGISTRATION)
        audiences = MinecraftServerAudiences.of(event.getServer())
        register()
    }

    @SubscribeEvent
    fun onServerStopping(event: ServerStoppingEvent) {
        audiences = null
    }

    @EventBusSubscriber(modid = MOD_ID, bus = EventBusSubscriber.Bus.MOD, value = [Dist.CLIENT])
    object ClientModEvents {
        @SubscribeEvent
        fun onClientSetup(event: FMLClientSetupEvent) {
            instance.register()
            audiences = MinecraftClientAudiences.of()
            instance.container.registerExtensionPoint(
                IConfigScreenFactory::class.java,
                IConfigScreenFactory { modContainer, screen ->
                    ConfigurationScreen(
                        modContainer,
                        screen
                    )
                }
            )
        }
    }

    /**
     * Returns the path of the specified resource in the mod files.
     *
     * @param resolve The paths to resolve in the mod files.
     * @return The path of the resource, or null if it does not exist.
     */
    fun resolveJarContents(vararg resolve: String): Path? {
        return container.modInfo.owningFile.file.findResource(*resolve).takeIf { Files.exists(it) }
    }

    fun getScrollAddons(): List<ScrollAddon> = ADDONS.toList()
    fun getRegistration(): SkriptRegistration = REGISTRATION
    fun getLanguageFolder(): Path = LANGUAGES_FOLDER
    fun getModContainer(): ModContainer = container
    fun getScriptsFolder(): Path = SCRIPTS_FOLDER
    fun getSelfAddon(): ScrollAddon = SELF_ADDON
    fun getScrollFolder(): Path = SCROLL_FOLDER
    fun getAddonFolder(): Path = ADDONS_FOLDER
    fun getVersion(): Version = version
    fun getLogger(): Logger = LOGGER
    fun getDist(): Dist = dist

    /**
     * Returns an EventRegistrar for a ScrollEvent.
     *
     * @param name The name of the event.
     * @param event The ScrollEvent class.
     * @param context The TriggerContext this ScrollEvent will handle.
     * @param patterns The ScrollEvent patterns.
     * @return EventRegistrar
     */
    fun <T : ScrollEvent> newEvent(
        name: String,
        event: Class<T>,
        context: Class<out TriggerContext>,
        vararg patterns: String
    ): EventRegistrar<T> {
        return REGISTRATION.newEvent(event, *patterns)
            .setHandledContexts(context)
            .addData("scroll-information", ScrollEvent.Information(name)) as EventRegistrar<T>
    }

    /**
     * Registers a ScrollEvent.
     *
     * @param name The name of the event.
     * @param event The ScrollEvent class.
     * @param context The TriggerContext this ScrollEvent will handle.
     * @param patterns The ScrollEvent patterns.
     */
    fun <T : ScrollEvent> addEvent(
        name: String,
        event: Class<T>,
        context: Class<out TriggerContext>,
        vararg patterns: String
    ) {
        newEvent(name, event, context, *patterns).register()
    }

    override fun handleTrigger(trigger: Trigger) {
        val event = trigger.event
        if (!canHandleEvent(event) || event !is ScrollEvent) return
        event.triggers.addTriggers(trigger)
    }

}
