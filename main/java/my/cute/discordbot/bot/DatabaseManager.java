package my.cute.discordbot.bot;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;

import my.cute.discordbot.IdUtils;
import net.dv8tion.jda.core.entities.Guild;
import net.dv8tion.jda.core.entities.Message;
import net.dv8tion.jda.core.entities.MessageHistory;
import net.dv8tion.jda.core.entities.TextChannel;
import net.dv8tion.jda.core.exceptions.InsufficientPermissionException;
import net.dv8tion.jda.core.utils.tuple.MutablePair;
import net.dv8tion.jda.core.utils.tuple.Pair;

import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DatabaseManager {

	private static final Map<Long, GuildDatabase> databases = new ConcurrentHashMap<Long, GuildDatabase>(10, 0.8f, 4);
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
	
	//method to scrape messages from server & rebuild database from scratch
	//TODO include some kind of time limit parameter so we can eg use messages from up to a year ago
	public static void rebuild(long id) {
		logger.info("starting scrape on server " + IdUtils.getFormattedServer(id));
		Guild targetGuild = demo.getJda().getGuildById(id);
		//convert all times to seconds
		double currentTime = ((double)System.currentTimeMillis()) / 1000;
		Map<String, Pair<Double, List<Message>>> allMessages = new ConcurrentHashMap<String, Pair<Double, List<Message>>>(10, 0.8f, 4);
		boolean success = true;
		double totalMessages=0;
		double totalTime=0;
		for(TextChannel c : targetGuild.getTextChannels()) {
			try {
				logger.info("beginning scrape on channel " + c + "...");
				List<Message> channelMessages = getMessagesInChannel(c);
				double channelTime;
				//avoid null exception
				if(channelMessages.isEmpty()) {
					channelTime = (currentTime - c.getCreationTime().toEpochSecond());
				} else {
					//maybe this should be (latest message time - oldest message time) rather than creation time
					//but im sure its close enough
					channelTime = (channelMessages.get(0).getCreationTime().toEpochSecond() - c.getCreationTime().toEpochSecond());
				}
				Pair<Double, List<Message>> pair = new MutablePair<Double, List<Message>>(channelTime, channelMessages);
				allMessages.put(c.getId(), pair);
				totalMessages+=channelMessages.size();
				totalTime+=channelTime;
			} catch (InsufficientPermissionException e) {
				e.printStackTrace();
				logger.info(e.getMessage() + " exception scraping " + c + ". proceeding");
			} catch (Exception e) {
				e.printStackTrace();
				logger.info(e.getMessage() + " exception scraping " + c + ". fatal error, exiting");
				success = false;
				return;
			}
			
		}
		
		Map<String, Boolean> usedChannels = new ConcurrentHashMap<String, Boolean>(10, 0.8f, 4);
		
		//TODO consider just skipping channels we encountered exception on
		//maybe we dont need to throw the whole thing out?
		if(success) {
			//messages per second
			double serverMessagesPerTime = totalMessages / totalTime;
			logger.info("server messages per time is " + serverMessagesPerTime);
			for(Entry<String, Pair<Double, List<Message>>> entry : allMessages.entrySet()) {
				//for each entry value, getLeft() gives the channel life in seconds
				//getRight() gives the channel message list
				//use these to obtain each channel's messages per second
				double channelMessagesPerTime = (((double)entry.getValue().getRight().size()) / entry.getValue().getLeft());
				//serverMessagesPerTime is the total messages in the server divided by the total sum lifetime
				//of all channels, as if all messages were sent in one single unified channel
				//thus each contributes its own weight of messages/time and so we divide by num
				//of channels to get the average - if a channel is at least this active, we use it
				//(allMessages contains 1 entry for each properly processed channel)
				boolean channelSufficientlyActive = channelMessagesPerTime >= (serverMessagesPerTime / (double)allMessages.size());
				logger.info("channel " + IdUtils.getFormattedChannel(entry.getKey(), id)
						+ " messages per time: " + channelMessagesPerTime + ", use: " + channelSufficientlyActive);
				usedChannels.put(entry.getKey(), channelSufficientlyActive);
			}
			
			GuildDatabase db = new GuildDatabase(demo.getJda().getGuildById(id));
			long curTime = System.currentTimeMillis();
			StringBuilder sb = new StringBuilder();
			//go over every channel we included
			for(Entry<String, Pair<Double, List<Message>>> entry : allMessages.entrySet()) {
				//only check channels we've designated to be used
				if(usedChannels.get(entry.getKey())) {
					//go over every message in that channel
					for(Message m : entry.getValue().getRight()) {
						//only use non-bot messages
						if(!m.getAuthor().isBot()) {
							db.processLine(m.getContentRaw());
							//add it to our stringbuilder
							sb.append(m.getContentRaw());
							sb.append("\r\n");
						}
					}
				}
			}
			
			logger.info("finished processing in " + (System.currentTimeMillis() - curTime) + "ms");
			logger.info("starting save");
			try {
				db.save();
				databases.put(id, db);
			} catch (Exception ex) {
				logger.error("EXCEPTION " + ex.getMessage() + " in saving newly built db. new db not saved");
			}
			
			String data = sb.toString().trim();
			File outFile = new File("./" + id + ".txt");
			try {
				FileUtils.writeStringToFile(outFile, data, StandardCharsets.UTF_8);
			} catch (IOException e) {
				logger.error("WARNING db scrape for " + IdUtils.getFormattedServer(id) 
						+ " encountered exception trying to save server log in DatabaseManager");
				e.printStackTrace();
			}
			
		}
	}
	
	//takes a textchannel c and returns a list of all messages in that channel
	//used for rebuilding database
	private static List<Message> getMessagesInChannel(TextChannel c) throws Exception {
		long currentTime = System.currentTimeMillis();
		long recentId;
		MessageHistory history = c.getHistory();
		List<Message> messages;
		history.retrievePast(100).queue();
		messages = history.getRetrievedHistory();
		try {
			if(!messages.isEmpty()) {
				do {
					if(messages.size() % 10000 == 0) System.out.println("counted " + messages.size() + " messages");
					recentId = messages.get(messages.size()-1).getIdLong();
					history.retrievePast(100).queue();
					messages = history.getRetrievedHistory();
				} while (recentId != messages.get(messages.size()-1).getIdLong());
			}
		} catch(Exception e) {
			e.printStackTrace();
			logger.info("encountered exception in channel " + c + " in guild " + c.getGuild());
			return new ArrayList<Message>();
		}	finally {
			logger.info("counted " + messages.size() + " messages total from " + c.getName() + " in " + (System.currentTimeMillis() - currentTime) + "ms");
		}
		
		return messages;
	}
	
	public static GuildDatabase getDatabase(long id) {
		return databases.get(id);
	}
}
