package my.cute.discordbot.bot;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Set;

import my.cute.discordbot.IdUtils;
import my.cute.discordbot.preferences.PreferencesManager;
import net.dv8tion.jda.core.entities.Guild;
import net.dv8tion.jda.core.entities.Message;

import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class RebuildGuildDatabaseTask implements Runnable {

	private static final Logger logger = LoggerFactory.getLogger(RebuildGuildDatabaseTask.class);
	private final long id;
	private final Long startTime;
	
	public RebuildGuildDatabaseTask(long i, Long s) {
		this.id = i;
		this.startTime = s;
	}
	
	/*
	 * input via constructor above: guild id, Long startTime representing the point in time 
	 * to start scraping from as an epoch millis time. null to scrape everything
	 * obtains relevant messages via GuildScraper then creates a new GuildDatabase
	 * and uses all obtained messages from the currently set discussionchannels to populate it
	 * finally replaces old db for that guild with the new one
	 * also saves all the scraped messages. i should do something with that i guess
	 */
	@Override
	public void run() {
		String guildString = IdUtils.getFormattedServer(this.id);
		logger.info("RebuildGuildDatabaseTask.run(): beginning guild "
				+ guildString + " db rebuild");
		Guild targetGuild = demo.getJda().getGuildById(id);
		Map<Long, List<Message>> allMessages = GuildScraper.getGuildMessagesSince(targetGuild, this.startTime);
		logger.info("RebuildGuildDatabaseTask.run(): scrape finished. beginning processing"
				+ " for guild " + guildString + " db rebuild");
		Set<Long> discChannels = PreferencesManager.getGuildPreferences(id).getDiscussionChannels();
		GuildDatabase db = new GuildDatabase(targetGuild);
		StringBuilder sb = new StringBuilder();
		try {
			//discussion channels being empty is permissible
			//in this case, all channels are treated as discussion channels
			//so we simply use the set of all channels obtained
			//i dont think this could break anything but maybe it does if we dont have some perms idk
			if(discChannels.isEmpty()) {
				logger.info("RebuildGuildDatabaseTask.run(): discussion channels empty for guild "
						+ guildString + ". defaulting to all channels");
				discChannels = allMessages.keySet();
			}
			discChannels.forEach(channel -> {
				sb.append("[!channel " + IdUtils.getFormattedChannel(channel, id) + "]\r\n");
				for(Message m : allMessages.get(channel)) {
					//only use non-bot messages
					if(!m.getAuthor().isBot()) {
						db.processLine(m.getContentRaw());
						//add it to our stringbuilder
						sb.append(m.getContentRaw());
						sb.append("\r\n");
					}
				}
			});
			logger.info("RebuildGuildDatabaseTask.run(): processing finished for guild "
					+ guildString + " db rebuild. beginning save");
			db.save();
			DatabaseManager.replaceDatabase(this.id, db);
			String data = sb.toString().trim();
			File outFile = new File("./" + id + ".txt");
			try {
				FileUtils.writeStringToFile(outFile, data, StandardCharsets.UTF_8);
			} catch (IOException e) {
				logger.warn("RebuildGuildDatabaseTask.run(): db scrape for " + IdUtils.getFormattedServer(id) 
						+ " encountered exception trying to save server log. db still saved and updated.");
				e.printStackTrace();
			}
		} catch (NullPointerException ex) {
			logger.error("RebuildGuildDatabaseTask.run(): nullpointer encountered in trying"
					+ " to process our new messages!\r\ndiscussion channels: " + discChannels
					+ "\r\ntextchannels scraped: " + allMessages.keySet()
					+ "db rebuild on server " + IdUtils.getFormattedServer(id) + " aborted");
			ex.printStackTrace();
			return;
		}
		logger.info("RebuildGuildDatabaseTask.run(): guild " + guildString + " db rebuild complete");
	}

}
