package my.cute.discordbot.preferences;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashSet;
import java.util.regex.PatternSyntaxException;
import java.util.stream.Collectors;

import my.cute.discordbot.IdUtils;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GuildPreferences {
	private static final Logger logger = LoggerFactory.getLogger(GuildPreferences.class);
	//whether to allow cutebot to send messages containing pings
	//if false, any pings will be stripped from messages
	private boolean allowPings;
	private final WordFilter wordFilter;
	//set of the channels to take messages from for our database
	//each entry is a string representation of the channel id
	private HashSet<Long> discussionChannels;
	//whether autonomy is on/off
	private boolean autonomyEnabled;
	//time between autonomous messages, in ms
	private long autonomyTimer;
	//guild id
	private final long id;
	
	public GuildPreferences(long i) {
		this.allowPings = true;
		this.discussionChannels = null;
		this.autonomyEnabled = false;
		this.id = i;
		this.autonomyTimer = 86400000L;
		this.wordFilter = new WordFilter(this.id);
		
	}
	
	public GuildPreferences(long i, WordFilter w) {
		this.allowPings = true;
		this.discussionChannels = null;
		this.autonomyEnabled = false;
		this.id = i;
		this.autonomyTimer = 86400000L;
		this.wordFilter = w;
		
	}
	
	public GuildPreferences(boolean p, long i) {
		this.allowPings = p;
		this.id = i;
		this.wordFilter = new WordFilter(this.id);
	}
	
	//note admins are omitted from save here because they're actually maintained 
	public void saveToDisk() {
		this.wordFilter.saveToDisk();
		StringBuilder sb = new StringBuilder();
		sb.append("pings=" + this.allowPings + "\r\n");
		sb.append("discussionchannels=" 
				+ (this.discussionChannels.isEmpty() ? "[empty]" : StringUtils.join(discussionChannels, ",")) + "\r\n");
		sb.append("autonomy=" + this.autonomyEnabled + "\r\n");
		sb.append("autonomytimer=" + this.autonomyTimer + "\r\n");
		try {
			FileUtils.writeStringToFile(new File("./markovdb/" + this.id + "/preferences.ini"), sb.toString().trim(), StandardCharsets.UTF_8);
		} catch (IOException e) {
			logger.error("unable to save preferences for guild " + IdUtils.getFormattedServer(this.id)
					+ " in GuildPreferences.saveToDisk()");
			e.printStackTrace();
		}
	}
	
	public void setPingStatus(boolean status) {
		this.allowPings = status;
	}
	
	public boolean pingsEnabled() {
		return this.allowPings;
	}
	
	public void setAutonomyStatus(boolean status) {
		this.autonomyEnabled = status;
	}
	
	public boolean autonomyEnabled() {
		return this.autonomyEnabled;
	}
	
	public void setAutonomyTimer(long t) {
		this.autonomyTimer = t;
	}
	
	public long getAutonomyTimer() {
		return this.autonomyTimer;
	}
	
	public boolean addDiscussionChannel(String channelId) {
		try {
			return this.discussionChannels.add(Long.parseLong(channelId));
		} catch (NumberFormatException e) {
			return false;
		}
	}
	
	public boolean removeDiscussionChannel(String channelId) {
		try {
			return this.discussionChannels.remove(Long.parseLong(channelId));
		} catch (NumberFormatException e) {
			return false;
		}
	}
	
	public void setDiscussionChannels(HashSet<Long> channels) {
		this.discussionChannels.clear();
		this.discussionChannels = new HashSet<Long>(channels);
	}
	
	//takes comma-separated list of ids for discussion channels
	//used in loading from disk
	void setDiscussionChannels(String list) {
		this.discussionChannels = Arrays.stream(list.split(","))
				.map(Long::parseLong)
				.collect(Collectors.toCollection(HashSet::new));
	}
	
	//String s is comma-separated list of words to add to wordfilter
	public boolean addWordsToFilter(String s) {
		return this.wordFilter.addFilteredWords(s);
	}
	
	public boolean removeWordsFromFilter(String s) {
		return this.wordFilter.removeFilteredWords(s);
	}
	
	public String getFilteredWords() {
		return this.wordFilter.getFilteredWords();
	}
	
	public String getExplicitRegex() {
		return this.wordFilter.getExplicitRegex();
	}
	
	public boolean setExplicitRegex(String s) {
		try {
			this.wordFilter.updateFilterExplicitly(s);
			return true;
		} catch (PatternSyntaxException ex) {
			return false;
		}
	}
	
	public boolean isUsingExplicitRegex() {
		return this.wordFilter.getExplicitRegexStatus();
	}
	
	public void clearFilter() {
		this.wordFilter.clearFilter();
	}
	
	public void setWordFilterResponse(String flags) {
		this.wordFilter.setFilterResponse(flags);
	}
	
	public String getWordFilterResponse() {
		return this.wordFilter.getStringFilterResponse();
	}
}
