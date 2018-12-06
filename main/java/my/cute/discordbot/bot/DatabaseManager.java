package my.cute.discordbot.bot;

import java.time.OffsetDateTime;
import java.time.temporal.TemporalUnit;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import net.dv8tion.jda.core.entities.Guild;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DatabaseManager {

	private static final Map<Long, GuildDatabase> databases = new ConcurrentHashMap<Long, GuildDatabase>(10, 0.8f, 4);
	@SuppressWarnings("unused")
	private static final Logger logger = LoggerFactory.getLogger(DatabaseManager.class);
	
	
	public static void save() {
		databases.entrySet().forEach(entry -> entry.getValue().save());
	}
	
	public static void load() {
		databases.clear();
		for(Guild g : demo.getJda().getGuilds()) {
			//construct the db for guild g
			//it's automatically loaded from disk as part of the markovdatabase constructor
			GuildDatabase db = new GuildDatabase(g);
			databases.put(g.getIdLong(), db);
		}
	}
	
	public static void addServer(Guild g) {
		GuildDatabase db = new GuildDatabase(g);
		databases.put(g.getIdLong(), db);
	}
	
	public static boolean removeServer(Guild g) {
		return (databases.remove(g.getIdLong()) != null);
	}
	
	//method to scrape messages from server & rebuild database from scratch
	//scraping messages requires a lot of waiting so we dispatch new thread
	public static void rebuild(long id) {
		Thread t = new Thread(new RebuildGuildDatabaseTask(id, null));
		t.start();
	}
	
	public static void rebuild(long id, long amount, TemporalUnit unit) {
		Thread t = new Thread(new RebuildGuildDatabaseTask(id, 
				OffsetDateTime.now().minus(amount, unit).toInstant().toEpochMilli()));
		t.start();
	}
	
	public static GuildDatabase getDatabase(long id) {
		return databases.get(id);
	}
	
	static void replaceDatabase(long id, GuildDatabase db) {
		databases.put(id, db);
	}
}
