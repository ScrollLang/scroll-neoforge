package org.scrolllang.scroll.utils

import org.reflections.vfs.Vfs
import org.reflections.vfs.SystemDir
import org.reflections.vfs.JarInputDir
import java.io.File
import java.net.URL
import java.net.URLDecoder
import java.nio.charset.StandardCharsets
import java.util.regex.Pattern

/**
 * Custom URL type for handling "union" URLs in the format:
 * union:/C:/path%20example/to/mod.jar%23220!/
 *
 * A union URL can reference multiple elements.
 * NeoForge uses this URL format to reference either;
 * - The jar files of mod libraries.
 * - The build folder of mod classes.
 */
class UnionUrlType : Vfs.UrlType {

    override fun matches(url: URL): Boolean {
        return url.protocol == "union"
    }

    override fun createDir(url: URL): Vfs.Dir {
        return try {
            // 1. Decode URL-encoded characters (e.g., %20 -> space)
            val decodedPath = URLDecoder.decode(url.path, StandardCharsets.UTF_8.name())
            val jarPath = decodedPath
                .substringAfter("union:") // Remove protocol
                .replace(Pattern.quote("%23"), "#")      // Restore "#" in filenames (e.g., Gradle cache)
                .substringBeforeLast("#") // Remove fragment (e.g., #220)
                .substringBefore("!/")    // Trim inner JAR paths
                .removePrefix("/")        // Fix Windows paths like "/C:/..."

            val file = File(jarPath)
            println("File path: ${file.toPath()}")
            if (file.isDirectory) {
                SystemDir(file)
            } else {
                JarInputDir(file.toURI().toURL())
            }
        } catch (e: Exception) {
            throw RuntimeException("Failed to create Vfs.Dir for $url", e)
        }
    }
}
