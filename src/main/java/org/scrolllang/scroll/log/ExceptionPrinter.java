package org.scrolllang.scroll.log;

import java.util.ArrayList;
import java.util.List;

import io.github.syst3ms.skriptparser.log.SkriptLogger;
import net.neoforged.fml.ModList;
import net.neoforged.fml.loading.FMLLoader;
import net.neoforged.neoforgespi.language.IModInfo;
import org.scrolllang.scroll.Scroll;
import org.scrolllang.scroll.ScrollAddon;
import org.scrolllang.scroll.ScrollLoader;
import org.scrolllang.scroll.exceptions.EmptyStacktraceException;
import org.slf4j.Logger;

/**
 * A printer that safely prints an exception to console without crashing.
 */
public class ExceptionPrinter extends CommonPrinter {

	private static final String EXCEPTION_PREFIX = "#!#! ";
	private static List<String> mods = new ArrayList<>();

	private final Throwable throwable;
	private final ScrollAddon addon;

	/**
	 * Constructs an exception printer.
	 * 
	 * @param throwable The cause of the exception.
	 * @param messages Optionally any messages needing to describe the error.
	 */
	public ExceptionPrinter(ScrollAddon addon, Throwable throwable, String... messages) {
		super(messages);
		this.throwable = throwable;
		this.addon = addon;
	}

	/**
	 * Prints a safe exception list with extra details to console.
	 * 
	 * @param logger The logger to print to.
	 * @return an EmptyStacktraceException to throw if code execution should terminate.
	 */
	public EmptyStacktraceException print(Logger logger) {
		// Don't send full exception message again, when caught exception (likely) comes from this method
		if (throwable instanceof EmptyStacktraceException)
			return new EmptyStacktraceException();

		logEx();
		logEx("[" + addon.getName() + "] Severe Error:");
		logEx(messages);
		logEx();
		if (mods.isEmpty()) {
			mods.add("Current Mods:");
			ModList.get().getMods().stream()
					.filter(info -> !info.getModId().equalsIgnoreCase("scroll"))
					.forEach(info -> mods.add("  " + info.getDisplayName() + " (" + info.getModId() + ") " + info.getVersion()));
			mods.add("");
		}
		mods.forEach(this::logEx);
		IModInfo scroll = Scroll.getInstance().getModContainer().getModInfo();

		if (addon.getName().equals("scroll")) {
			if (scroll.getVersion().toString().contains("nightly")) {
				logEx("You're running a (buggy) nightly version of Scroll.");
				logEx("If this is not a test server, switch to a more stable release NOW!");
				logEx("Your players are unlikely to appreciate crashes and/or data loss due to Scroll bugs.");
				logEx("");
				logEx("Just testing things? Good. Please report this bug, so that we can fix it before a stable release.");
				logEx("Issue tracker: " + addon.getReportURL() == null ? "https://github.com/ScrollLang/Scroll/issues" : addon.getReportURL());
	//		} else if (updater != null && updater.getReleaseStatus() == ReleaseStatus.OUTDATED) {
	//			logEx("You're running outdated version of Scroll! Please try updating it NOW; it might fix this issue.");
	//			logEx("Run /sc update check to get a download link to latest Scroll!");
	//			logEx("You will be given instructions how to report this error if it persists after update.");
			} else {
				logEx("Something went horribly wrong with Scroll.");
				logEx("This issue is NOT your fault! You probably can't fix it yourself, either.");
				logEx("You should report it at " + (addon.getReportURL() == null ? "https://github.com/ScrollLang/Scroll/issues" : addon.getReportURL()) + ". Please copy paste this report there (or use a paste service).");
				logEx("This ensures that your issue is noticed and will be fixed as soon as possible.");
			}
		}

		logEx();
		logEx("Stack trace:");
		Throwable cause = throwable;
		if (cause == null || cause.getStackTrace().length == 0) {
			logEx("  warning: no/empty exception given, dumping current stack trace instead");
			cause = new Exception(cause);
		}
		boolean first = true;
		while (cause != null) {
			logEx((first ? "" : "Caused by: ") + cause.toString());
			for (StackTraceElement element : cause.getStackTrace())
				logEx("    at " + element.toString());
			cause = cause.getCause();
			first = false;
		}

		logEx();
		var versions = FMLLoader.versionInfo();
		logEx("Version Information:");
		logEx("  Scroll: " + scroll.getVersion());
		logEx("  NeoForge: " + versions.neoForgeVersion());
		logEx("  Minecraft: " + versions.mcVersion());
		logEx("  Java: " + System.getProperty("java.version") + " " + System.getProperty("java.vendor") + " " + System.getProperty("java.vm.name"));
		logEx("  OS: " + System.getProperty("os.name") + " " + System.getProperty("os.arch") + " " + System.getProperty("os.version"));
		logEx();
		logEx("Environment: " + Scroll.getInstance().getDist());
		logEx();
		SkriptLogger scrollLogger = ScrollLoader.getCurrentLogger();
		logEx("Current script: " + (scrollLogger == null ? "null" : scrollLogger.getFileName()));
//		logEx("Current node: " + SkriptLogger.getNode());
//		logEx("Current item: " + (item == null ? "null" : item.toString(null, true)));
//		if (item != null && item.getTrigger() != null) {
//			Trigger trigger = item.getTrigger();
//			Script script = trigger.getScript();
//			logEx("Current trigger: " + trigger.toString(null, true) + " (" + (script == null ? "null" : script.getConfig().getFileName()) + ", line " + trigger.getLineNumber() + ")");
//		}
		logEx();
		logEx("End of Error.");
		logEx();
		return new EmptyStacktraceException();
	}

	private void logEx() {
		Logger logger = Scroll.getInstance().getLogger();
		logger.error(EXCEPTION_PREFIX);
	}

	private void logEx(String... lines) {
		Logger logger = Scroll.getInstance().getLogger();
		for (String line : lines)
            logger.error(EXCEPTION_PREFIX + "{}", line);
	}

}
