package org.scrolllang.scroll.utils

import org.scrolllang.scroll.Scroll
import org.scrolllang.scroll.translations.Translation.filesKey
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Path

object FileUtils {

	/**
	 * Returns or creates the directories if they don't exist.
	 * This has proper exception catching and language reference.
	 *
	 * @param path The path to find and create.
	 * @return The resolved path.
	 */
	fun getOrCreateDir(path: Path): Path {
		return if (Files.exists(path)) {
			path
		} else {
			try {
				Files.createDirectories(path)
			} catch (exception: IOException) {
				throw Scroll.printException(exception, filesKey("create.directory", path.fileName))
			}
		}
	}
}
