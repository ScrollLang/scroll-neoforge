package org.scrolllang.scroll

import java.io.IOException
import java.net.URISyntaxException
import java.nio.file.Path
import org.scrolllang.scroll.exceptions.EmptyStacktraceException
import org.scrolllang.scroll.log.ExceptionPrinter
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import io.github.syst3ms.skriptparser.util.FileUtils

/**
 * Class used to register an addon for Scroll.
 */
abstract class ScrollAddon @JvmOverloads constructor(

	/**
	 * The instance of the Scroll addon.
	 * This is used to access the Scroll instance.
	 */
    val instance: Scroll,

	/**
	 * The name of the addon.
	 * This is used to identify the addon in Scroll.
	 */
    val name: String,
	priority: Int = 100
) {
	private val logger: Logger = LoggerFactory.getLogger(name)
	val priority: Int = if (priority < 0) 0 else priority

	/**
	 * Utility method to statically load syntaxes classes in packages.
	 * Setting mainPackage to me.example.addon and the subPackage to elements
	 * Will statically initialize all classes in me.example.addon.elements.
	 *
	 * @param mainPackage The main package to start from.
	 * @param subPackages The sub packages within the main package to initialize.
	 */
	protected fun loadClasses(mainPackage: String, vararg subPackages: String) {
		try {
			FileUtils.loadClasses(FileUtils.getJarFile(this::class.java), mainPackage, *subPackages)
		} catch (exception: IOException) {
			printException(exception, "Failed to initialize classes from addon $name")
		} catch (exception: URISyntaxException) {
			printException(exception, "Failed to initialize classes from addon $name")
		}
	}

	/**
	 * Called when the addon is loaded by Scroll.
	 */
	protected abstract fun initAddon()

	/**
	 * Called when you can safely start registering syntaxes.
	 * Should only be done on mod initialization. Not later.
	 *
	 * @param registration The registration to use to add syntaxes.
	 */
	internal abstract fun startRegistration(registration: ScrollRegistration)

	/**
	 * The data folder where your addon should store configurations.
	 * Directory may not exist. You need to make directories.
	 *
	 * @return Path to the data folder of this addon.
	 */
	open fun getDataFolder(): Path = instance.getAddonFolder().resolve(name)

	fun printException(exception: Exception, message: String): EmptyStacktraceException {
		return ExceptionPrinter(this, exception, message).print(logger)
	}

	fun info(message: String) {
		logger.info(message)
	}

	fun warn(message: String) {
		logger.warn(message)
	}

	fun error(message: String) {
		logger.error(message)
	}

	/**
	 * @return The URL to report errors for this addon at.
	 */
	open fun getReportURL(): String? = null
}
