package my.cute.discordbot.preferences;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import my.cute.discordbot.bot.demo;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PreferencesManager {

	private static final Map<Long, GuildPreferences> guildPreferences = new ConcurrentHashMap<Long, GuildPreferences>(10, 0.8f, 4);
	@SuppressWarnings("unused")
	private static final Logger logger = LoggerFactory.getLogger(PreferencesManager.class);
	
	public static void load() {
		for(Long id : demo.getGuildIdLongs()) {
			GuildPreferences prefs = PreferencesLoader.load(id);
			guildPreferences.put(id, prefs);
		}
	}
	
	public static void save() {
		guildPreferences.entrySet()
			.forEach(entry -> entry.getValue().saveToDisk());
	}
	
	public static GuildPreferences getGuildPreferences(long id) {
		return guildPreferences.get(id);
	}
	
}
