package my.cute.discordbot.bot;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PerformanceLogger {
	
	private static final Logger logger = LoggerFactory.getLogger("PerformanceLogger");
	
	public static final void log(String msg) {
		logger.info(msg);
	}
}
