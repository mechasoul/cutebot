package my.cute.discordbot.preferences;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;

import my.cute.discordbot.IdUtils;
import my.cute.discordbot.bot.demo;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.LineIterator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PreferencesLoader {
	private static final Logger logger = LoggerFactory.getLogger(PreferencesLoader.class);

	public static GuildPreferences load(long id) {
		WordFilter filter = WordFilterLoader.load(id);
		GuildPreferences prefs = new GuildPreferences(id, filter);
		try (LineIterator it = FileUtils.lineIterator(new File("./markovdb/" + id + "/preferences.ini"), "UTF-8")) {
			while(it.hasNext()) {
				String line = it.nextLine();
				String tokens[] = line.split("=",2);
				String option = tokens[0];
				String value = tokens[1];

				switch(option) {
				//pings option
				//format: pings=true|false
				//controls whether bot will output messages containing pings
				case "pings":
					prefs.setPingStatus(Boolean.parseBoolean(value));
					break;
				case "discussionchannels":
					if(!value.equals("[empty]")) prefs.setDiscussionChannels(value);
					break;
				case "autonomy":
					prefs.setAutonomyStatus(Boolean.parseBoolean(value));
					break;
				case "autonomytimer":
					prefs.setAutonomyTimer(Long.parseLong(value));
					break;
				default:
					logger.warn("unknown option encountered in PreferencesLoader.load() for guild "
							+ IdUtils.getFormattedServer(id) + ". line: " + line);
					break;

				}
			}
		} catch (FileNotFoundException ex) {
			//first-run case for this guild
			//default values loaded in GuildPreferences constructor
			//no more action needed
			logger.info("created new GuildPreferences object with default values for new guild "
					+ IdUtils.getFormattedServer(id) + ". proceeding...");
		} catch (IOException e) {
			logger.error("couldn't build preferences for guild " + IdUtils.getFormattedServer(id)
					+ " in PreferencesLoader.load(). fatal error: " + e.getMessage());
			e.printStackTrace();
			demo.exit(1);
		}
		return prefs;
	}
	
}
