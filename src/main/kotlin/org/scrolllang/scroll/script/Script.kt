package org.scrolllang.scroll.script

import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import org.apache.commons.lang3.Validate

/**
 * Represents a parsed and loaded Script.
 */
class Script(val path: Path) {

	init {
		Validate.isTrue(!Files.isDirectory(path), "The path of the script was a directory. Must be a single file.")
	}

	val simpleName: String
		get() {
			val fileName = this.fileName
			return fileName.substring(0, fileName.lastIndexOf(".scroll"))
		}

	val fileName: String
		get() = path.fileName.toString()

	val file: File
		get() = path.toFile()

}
