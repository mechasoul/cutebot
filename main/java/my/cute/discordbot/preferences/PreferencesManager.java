package my.cute.discordbot.preferences;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import my.cute.discordbot.bot.demo;
import net.dv8tion.jda.core.entities.Guild;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PreferencesManager {

	private static final Map<Long, GuildPreferences> guildPreferences = new ConcurrentHashMap<Long, GuildPreferences>(10, 0.8f, 4);
	@SuppressWarnings("unused")
	private static final Logger logger = LoggerFactory.getLogger(PreferencesManager.class);
	private static final Set<Long> guildsModifyingDiscChannels = ConcurrentHashMap.newKeySet();
	
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
	
	public static void addServer(Guild g) {
		GuildPreferences prefs = PreferencesLoader.load(g.getIdLong());
		guildPreferences.putIfAbsent(g.getIdLong(), prefs);
	}
	
	public static boolean removeServer(Guild g) {
		return (guildPreferences.remove(g.getIdLong()) != null);
	}
	
	public static GuildPreferences getGuildPreferences(long id) {
		return guildPreferences.get(id);
	}
	
	public static boolean addGuildModifyingDiscChan(long id) {
		return guildsModifyingDiscChannels.add(id);
	}
	
	public static boolean isGuildModifyingDiscChan(long id) {
		return guildsModifyingDiscChannels.contains(id);
	}
	
	public static boolean removeGuildModifyingDiscChan(long id) {
		return guildsModifyingDiscChannels.remove(id);
	}
	
}
