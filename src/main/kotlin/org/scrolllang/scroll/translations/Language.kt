package org.scrolllang.scroll.translations

import org.scrolllang.scroll.Scroll
import org.scrolllang.scroll.configuration.ScrollConfiguration
import org.scrolllang.scroll.language.Reloadable
import org.scrolllang.scroll.translations.Translation.languageKey
import java.io.FileInputStream
import java.io.IOException
import java.nio.file.Files
import java.nio.file.InvalidPathException
import java.util.*
import java.util.regex.Pattern

/**
 * Main class for loading languages from the properties files.
 */
class Language(private val instance: Scroll) : Reloadable {

    private val otherNodePattern: Pattern = Pattern.compile("\\{(.*?)\\}")
	private val languagesFolder = instance.getLanguageFolder()
	private val languages = mutableSetOf<String>()
    private val defaultLanguage = "en-US"

    private var properties: Properties? = loadLanguage()
    private var english: Properties = loadEnglish()

    /**
     * Method that loads the language' properties.
     *
     * @return The parsed Properties file.
     * @throws IOException
     */
    private fun loadLanguage(): Properties? {
		languages.clear()
        instance.resolveJarContents("languages")?.let { path ->
            Files.list(path).forEach { filePath ->
				val fileName = filePath.fileName.toString()
				languages.add(fileName.substringBeforeLast('.'))
                val languagePath = languagesFolder.resolve(fileName)
                if (!Files.exists(languagePath)) {
                    try {
                        Files.copy(filePath, languagePath)
                    } catch (exception: IOException) {
                        Scroll.printException(exception, languageKey("failed.copy", fileName))
                    }
                }
            }
        }

        val language = ScrollConfiguration.language
        val path = try {
            languagesFolder.resolve("$language.properties")
        } catch (exception: InvalidPathException) {
            languagesFolder.resolve("$defaultLanguage.properties").also {
                Scroll.error(languageKey("not.found", "$language.properties"))
            }
        }

        return try {
            Properties().apply {
                FileInputStream(path.toFile()).use { load(it) }
            }
        } catch (exception: Exception) {
            Scroll.printException(exception, languageKey("failed.load", path.toString()))
            null
        }
    }

    /**
     * Loads the defaultLanguage properties language file for usage in the [getOrDefaultEnglish] method.
     *
     * @return The parsed Properties file.
     * @throws IOException
     */
    private fun loadEnglish(): Properties {
        val path = languagesFolder.resolve("$defaultLanguage.properties")
        return Properties().apply {
            FileInputStream(path.toFile()).use { load(it) }
        }
    }

    /**
     * Reloads the currently selected language file.
     * Will read the main configuration node before to see if the language has changed.
     */
    override fun reload(): Boolean {
        return try {
            properties = loadLanguage()
            english = loadEnglish()
            true
        } catch (exception: Exception) {
            Scroll.printException(exception, languageKey("reload.fail", "scroll/configuration.toml"))
            false
        }
    }

	/**
	 * @return Get all the languages available in the languages folder.
	 */
	fun getAvailableLanguages(): Set<String> = languages.toSet()

    /**
     * The English properties file is always to be updated.
     * So if a language file does not contain the correct key, this method
     * will default the value to the English value. Typically because outdated file.
     *
     * @param key The key to search the properties file for.
     * @return The String value from the defined language file otherwise from English language file.
     */
    fun getOrDefaultEnglish(key: String): String {
        var value = properties?.getProperty(key) ?: english.getProperty(key) ?: return key
        val matcher = otherNodePattern.matcher(value)
        value = matcher.replaceAll { matchResult ->
            val group = matchResult.group(1)
            if (group.equals(key, ignoreCase = true)) group else getOrDefaultEnglish(group)
        }
        return value
    }

    /**
     * Returns a key from the language file, null if not found.
     *
     * @param key The key to lookup in the language file.
     * @return The value of the key if found, otherwise will return null.
     */
    fun get(key: String): String? = properties?.getProperty(key)

    /**
     * Returns a key from the language file, otherwise uses the parameter provided for default value.
     *
     * @param key The key to lookup in the language file.
     * @param defaultValue The default value that will be returned if no key was present in the language file.
     * @return The value of the key if found, otherwise the default value parameter.
     */
    fun get(key: String, defaultValue: String): String = get(key) ?: defaultValue

}
