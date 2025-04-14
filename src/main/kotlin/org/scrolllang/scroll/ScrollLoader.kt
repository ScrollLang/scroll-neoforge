package org.scrolllang.scroll

import io.github.syst3ms.skriptparser.Parser
import io.github.syst3ms.skriptparser.log.LogEntry
import io.github.syst3ms.skriptparser.log.SkriptLogger
import io.github.syst3ms.skriptparser.parsing.ScriptLoader
import org.jetbrains.annotations.ApiStatus.Internal
import org.jetbrains.annotations.NotNull
import org.scrolllang.scroll.configuration.ScrollConfiguration
import org.scrolllang.scroll.language.ScrollEvent
import org.scrolllang.scroll.script.Script
import org.scrolllang.scroll.translations.Translation.filesKey
import org.scrolllang.scroll.translations.Translation.scriptKey
import org.scrolllang.scroll.translations.Translation.scrollKey
import org.scrolllang.scroll.utils.FileUtils
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Path
import java.util.*
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ExecutionException
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException
import java.util.function.Predicate
import java.util.stream.Collectors
import java.util.stream.Stream

object ScrollLoader {

	private var scriptsFolder: Path = FileUtils.getOrCreateDir(Scroll.instance.getScriptsFolder())

	private val reservedNames = setOf(
		"configuration.scroll",
		"languages.scroll",
		"language.scroll",
		"settings.scroll",
		"config.scroll",
		"lang.scroll"
	)

	private val loadedScripts = mutableListOf<Script>()

	const val DISABLED_PREFIX = "-"
	const val EXTENSION = ".scroll"

	@JvmStatic
	var currentLogger: SkriptLogger? = null
		private set

	private var currentScript: Script? = null

	fun getCurrentlyLoadingScript(): Script? = currentScript

	fun validateScriptAt(script: Path): Boolean {
		if (Files.isDirectory(script)) {
			Scroll.error(scriptKey("load.error.directory", script.toString()))
			return false
		}
		val fileName = script.fileName.toString()
		if (!fileName.endsWith(EXTENSION)) return false
		if (fileName.startsWith(DISABLED_PREFIX)) return false
		if (reservedNames.any { it.equals(fileName, ignoreCase = true) }) {
			Scroll.error(scriptKey("name.reserved", fileName))
			return false
		}
		return true
	}

	fun collectScriptsAt(directory: Path): Stream<Path> = collectScriptsAt(directory, ::validateScriptAt)

	fun collectScriptsAt(directory: Path, filter: Predicate<Path>): Stream<Path> {
		if (!Files.isDirectory(directory)) {
			Scroll.error(scriptKey("load.error.not.directory", directory.toString()))
			return Stream.empty()
		}
		return try {
			Files.list(directory)
				.filter(Objects::nonNull)
				.flatMap { path ->
					if (Files.isDirectory(path)) {
						try {
							Files.list(path)
						} catch (exception: IOException) {
							Scroll.printException(exception, filesKey("read.directory", directory))
							Stream.empty()
						}
					} else {
						Stream.of(path)
					}
				}
				.filter(filter)
		} catch (exception: IOException) {
			Scroll.printException(exception, filesKey("read.directory", directory))
			Stream.empty()
		}
	}

	@Internal
	fun loadScriptsAtDirectory(scriptsPath: Path): List<Script> {
		if (!Files.isDirectory(scriptsPath)) {
			Scroll.error(scriptKey("load.internal.scripts"))
			return emptyList()
		}
		val start = System.nanoTime()
		loadedScripts.clear() // TODO proper unloading.
		scriptsFolder = scriptsPath
		val scripts = collectScriptsAt(scriptsPath)
			.parallel()
			.map(::loadScriptAt)
			.filter(Optional<Script>::isPresent)
			.map(Optional<Script>::get)
			.collect(Collectors.toList())
		if (scripts.isEmpty()) {
			Scroll.warning(scrollKey("no.scripts"))
			return scripts
		}
		loadedScripts.addAll(scripts)
		Scroll.info(
			scrollKey(
				"scripts.loaded",
				scripts.size,
				TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - start)
			)
		)
		return scripts
	}

	fun loadScriptAt(path: Path): Optional<Script> {
		if (Files.isDirectory(path)) {
			Scroll.error(scriptKey("load.error.directory", path.toString()))
			return Optional.empty()
		}
		if (!validateScriptAt(path)) return Optional.empty()
		val script = loadedScripts.stream()
			.filter { it.path == path }
			.map {
				// TODO re-add
				//CommandManager.unregisterAll(it)
				it
			}
			.findFirst()
			.orElse(Script(path))
		loadedScripts.remove(script)
		currentScript = script
		ScriptLoader.getTriggerMap().entries.stream()
			.filter { entry ->
				val fileName = path.fileName.toString().replace("(.+)\\..+".toRegex(), "$1")
				entry.key.equals(fileName, ignoreCase = true)
			}
			.map { it.value }
			.flatMap { it.stream() }
			.forEach { trigger ->
				val event = trigger.event
				if (event !is ScrollEvent) return@forEach
				event.triggers.clear()
			}
		val entries: List<LogEntry>
		try {
			currentLogger = SkriptLogger(ScrollConfiguration.debug)
			entries = CompletableFuture.supplyAsync { ScriptLoader.loadScript(path, currentLogger!!, ScrollConfiguration.debug) }.get(10, TimeUnit.MINUTES)
			currentLogger!!.finalizeLogs()
			entries.addAll(currentLogger!!.close())
			Parser.printLogs(entries, Calendar.getInstance(), true)
			currentLogger = null
		} catch (exception: InterruptedException) {
			Scroll.error(scriptKey("loading.timeout", path.fileName))
			return Optional.empty()
		} catch (exception: TimeoutException) {
			Scroll.error(scriptKey("loading.timeout", path.fileName))
			return Optional.empty()
		} catch (exception: ExecutionException) {
			Scroll.printException(exception, scriptKey("parse.exception", path.fileName))
			return Optional.empty()
		}
		currentScript = null
		loadedScripts.add(script)
		return Optional.of(script)
	}

	fun loadScriptsAt(directory: Path): List<Script> {
		if (!Files.isDirectory(directory)) {
			Scroll.error(scriptKey("load.error.not.directory", directory.toString()))
			return emptyList()
		}
		return collectScriptsAt(directory)
			.map(::loadScriptAt)
			.filter(Optional<Script>::isPresent)
			.map(Optional<Script>::get)
			.collect(Collectors.toList())
	}

	fun reloadScript(script: Script): Optional<Script> = loadScriptAt(script.path)

	fun reloadScripts(scripts: Collection<Script>): List<Script> = scripts.stream()
		.map(::reloadScript)
		.filter(Optional<Script>::isPresent)
		.map(Optional<Script>::get)
		.collect(Collectors.toList())

	fun disableScript(script: Script) {
		val path = script.path
		val fileName = script.fileName
		loadedScripts.remove(script)
		if (!Files.exists(path)) return // Was renamed
		try {
			Files.move(path, scriptsFolder.resolve(DISABLED_PREFIX + path.fileName))
		} catch (exception: IOException) {
			Scroll.error(scriptKey("disable.failed", fileName))
		}
	}

	fun disableScripts(scripts: Collection<Script>) {
		scripts.forEach(::disableScript)
	}

	fun enableScriptAt(path: Path): Optional<Script> {
		if (!Files.exists(path)) return Optional.empty()
		val fileName = path.fileName.toString()
		if (!fileName.startsWith(DISABLED_PREFIX)) return Optional.empty()
		val futurePath = scriptsFolder.resolve(fileName.substring(DISABLED_PREFIX.length))
		if (loadedScripts.any { it.path == futurePath }) {
			Scroll.error(scriptKey("enable.failed", fileName))
			return Optional.empty()
		}
		return try {
			loadScriptAt(Files.move(path, futurePath))
		} catch (exception: IOException) {
			Scroll.error(scriptKey("enable.failed", fileName))
			Optional.empty()
		}
	}

	fun enableScriptsAt(paths: Collection<Path>) {
		paths.forEach(::enableScriptAt)
	}

	fun getScriptByName(name: String): Optional<Script> = loadedScripts.stream().filter { script ->
		val fileName = script.fileName
		fileName.equals(name, ignoreCase = true) || fileName.substring(0, fileName.lastIndexOf(EXTENSION)).equals(name, ignoreCase = true)
	}.findFirst()

	@NotNull
	fun getLoadedScripts(): Collection<Script> = Collections.unmodifiableCollection(loadedScripts)
}