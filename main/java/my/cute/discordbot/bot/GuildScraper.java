package my.cute.discordbot.bot;

import java.time.OffsetDateTime;
import java.time.temporal.TemporalUnit;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import my.cute.discordbot.IdUtils;
import net.dv8tion.jda.core.entities.Guild;
import net.dv8tion.jda.core.entities.Message;
import net.dv8tion.jda.core.entities.MessageHistory;
import net.dv8tion.jda.core.entities.TextChannel;
import net.dv8tion.jda.core.utils.MiscUtil;

public class GuildScraper {
	
	private static final Logger logger = LoggerFactory.getLogger(GuildScraper.class);
	
	//these methods defer to getGuildMessagesSince(Guild,Long)
	//and just make it easier to call without having to do the time conversion or whatever
	public static Map<Long, List<Message>> getGuildMessagesSince(Guild g) {
		return getGuildMessagesSince(g, null);
	}
	
	public static Map<Long, List<Message>> getGuildMessagesSince(Guild g, long amount, TemporalUnit unit) {
		return getGuildMessagesSince(g, OffsetDateTime.now().minus(amount, unit).toInstant().toEpochMilli());
	}
	
	

	/*
	 * input: guild g, startTime the time to start the scrape from, as an epoch millis long
	 * if startTime is null, scrapes everything
	 * output: map of textchannel ids -> list of messages from that channel since the start time,
	 * as returned by MessageHistory.getRetrievedHistory()
	 */
	public static Map<Long, List<Message>> getGuildMessagesSince(Guild g, Long startTime) {
		String guildString = IdUtils.getFormattedServer(g);
		logger.info("beginning message scrape on guild " + guildString);
		Map<Long, List<Message>> map = new ConcurrentHashMap<Long, List<Message>>();
		List<Message> messages=null;
		for(TextChannel c : g.getTextChannels()) {
			logger.info("guild " + guildString + " message scrape: looking at channel " + IdUtils.getFormattedChannel(c));
			String discordTimestamp = getHistoryStartTimestamp(c, startTime);
			MessageHistory mh = MessageHistory.getHistoryAfter(c, discordTimestamp).limit(50).complete();
			messages = mh.getRetrievedHistory();
			
			if(messages.isEmpty()) {
				map.put(c.getIdLong(), Collections.emptyList());
			} else {
				long recentId;
				try {
					do {
						recentId = messages.get(0).getIdLong();
						mh.retrieveFuture(100).complete();
						messages = mh.getRetrievedHistory();
					} while (recentId != messages.get(0).getIdLong());
					map.put(c.getIdLong(), messages);
				} catch(Exception e) {
					map.put(c.getIdLong(), Collections.emptyList());
				}
			}
			
			logger.info("guild " + guildString + " message scrape: counted " + map.get(c.getIdLong()).size() 
					+ " messages");
		}
		logger.info("guild " + guildString + " message scrape completed");
		return map;
	}
	
	private static String getHistoryStartTimestamp(TextChannel c, Long startTime) {
		long timestamp = (startTime != null ? startTime : c.getCreationTime().toInstant().toEpochMilli());
		return Long.toUnsignedString(MiscUtil.getDiscordTimestamp(timestamp));
	}
}
