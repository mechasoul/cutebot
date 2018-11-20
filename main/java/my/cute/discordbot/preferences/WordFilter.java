package my.cute.discordbot.preferences;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import java.util.stream.Collectors;

import my.cute.discordbot.IdUtils;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class WordFilter {
	private static final Logger logger = LoggerFactory.getLogger(WordFilter.class);
	private static final int MAX_BANNED_WORDS = 100;

	//set of banned words/phrases
	private HashSet<String> bannedWords = new HashSet<String>(8);
	//set of enum choices indicating different responses to banned phrases
	private EnumSet<FilterResponse> filterAction = EnumSet.of(FilterResponse.SKIP_PROCESS,
			FilterResponse.DELETE_MESSAGE, FilterResponse.SEND_RESPONSE);
	//the actual pattern object we use to match against messages
	//we just store it compiled here so we dont have to compile it every time
	private Pattern compiledFilter = null;
	private boolean isUsingExplicitRegex = false;
	private final long id;
	
	public WordFilter(long i) {
		this.id = i;
	}
	
	//takes a list of comma-separated words to add and adds them to filter
	//returns true if filter was changed as a result, false if not
	public boolean addFilteredWords(String s) {
		//arbitrary cap on banned word size
		//for practical purposes it shouldnt be very high and has a very negative effect
		//on our regex performance probably?
		if(this.bannedWords.size() >= MAX_BANNED_WORDS) return false;
		boolean filterChanged = false;
		for(String word : s.split(",")) {
			if(StringUtils.isBlank(word)) continue;
			if(this.bannedWords.add(word.trim())) filterChanged = true;
		}
		if(filterChanged) updateFilter();
		return filterChanged;
	}
	
	public boolean removeFilteredWords(String s) {
		if(this.bannedWords.size() == 0) return false;
		boolean filterChanged = false;
		for(String word : s.split(",")) {
			if(StringUtils.isBlank(word)) continue;
			if(this.bannedWords.remove(word)) filterChanged = true;
		}
		if(filterChanged) updateFilter();
		return filterChanged;
	}
	
	public void setFilteredWords(String s) {
		this.bannedWords = Arrays.stream(s.split(","))
			.filter(StringUtils::isNotBlank)
			.map(String::trim)
			.collect(Collectors.toCollection(HashSet::new));
		updateFilter();
	}
	
	public String getFilteredWords() {
		return this.bannedWords.isEmpty() ? "" : StringUtils.join(this.bannedWords, ",");
	}
	
	public String getExplicitRegex() {
		return this.compiledFilter.pattern();
	}
	
	public void stopUsingExplicitRegex() {
		this.isUsingExplicitRegex = false;
		updateFilter();
	}
	
	public boolean getExplicitRegexStatus() {
		return this.isUsingExplicitRegex;
	}
	
	public void clearFilter() {
		this.bannedWords.clear();
		this.isUsingExplicitRegex = false;
		this.compiledFilter = null;
	}
	
	
	//call this method on a guild when its filter has been changed
	//it breaks the filter into individual words, builds a regex, then compiles it and puts it
	//in that guild's pattern object
	//generated regex will match all words in the list, as whole words, optionally ending with an 's'
	//for example, filter list "apple,orange,banana" is built into string 
	//"\\b((?:apple)|(?:orange)|(?:banana))s?\\b"
	//which should match any whole word: apple, apples, orange, oranges, banana, bananas
	//the word "crabapple" would not trigger a match
	//TODO save to disk here
	private void updateFilter() {
		if(this.isUsingExplicitRegex) return;
		if(this.bannedWords.isEmpty()) {
			this.compiledFilter = null;
			return;
		}
		String newFilter = "\\b((?:" + StringUtils.join(this.bannedWords, ")|(?:") + "))s?\\b";
		this.compiledFilter = Pattern.compile(newFilter, Pattern.CASE_INSENSITIVE);
	}
	
	public void updateFilterExplicitly(String regex) throws PatternSyntaxException {
		if(StringUtils.isBlank(regex)) {
			this.compiledFilter = null;
			return;
		} else {
			this.compiledFilter = Pattern.compile(regex, Pattern.CASE_INSENSITIVE);
			this.isUsingExplicitRegex = true;
		}
	}
	
	void setCompiledFilter(String pattern) {
		this.compiledFilter = Pattern.compile(pattern, Pattern.CASE_INSENSITIVE);
	}
	
	void setExplicitRegexStatus(boolean status) {
		this.isUsingExplicitRegex = status;
	}
	
	public void setFilterResponse(String flags) {
		this.filterAction = convertStringToAction(flags);
	}
	
	public String getStringFilterResponse() {
		return convertActionToString(this.filterAction);
	}
	
	public static EnumSet<FilterResponse> convertStringToAction(String input) {
		EnumSet<FilterResponse> action = EnumSet.noneOf(FilterResponse.class);
		for(int i=0; i < input.length(); i++) {
			char c = input.charAt(i);
			switch (c) {
				case '1': 
					action.add(FilterResponse.SKIP_PROCESS);
					break;
				case '2':
					action.add(FilterResponse.SEND_RESPONSE);
					break;
				case '3':
					action.add(FilterResponse.DELETE_MESSAGE);
					break;
				case '4':
					action.add(FilterResponse.MUTE);
					break;
				case '5':
					action.add(FilterResponse.KICK);
					break;
				case '6':
					action.add(FilterResponse.BAN);
					break;
				default:
					break;
			}
		}
		return action;
	}
	
	public static String convertActionToString(EnumSet<FilterResponse> action) {
		StringBuilder flags = new StringBuilder();
		//not the most efficient way to do this but its small enough that its irrelevant
		//and i want the flags string to be 'sorted'
		if(action.contains(FilterResponse.SKIP_PROCESS)) flags.append("1");
		if(action.contains(FilterResponse.SEND_RESPONSE)) flags.append("2");
		if(action.contains(FilterResponse.DELETE_MESSAGE)) flags.append("3");
		if(action.contains(FilterResponse.MUTE)) flags.append("4");
		if(action.contains(FilterResponse.KICK)) flags.append("5");
		if(action.contains(FilterResponse.BAN)) flags.append("6");
		return flags.toString();
	}
	
	public void saveToDisk() {
		StringBuilder sb = new StringBuilder();
		sb.append("bannedwords=" 
				+ (this.bannedWords.isEmpty() ? "[empty]" : StringUtils.join(this.bannedWords, ",")) + "\r\n");
		sb.append("filteraction=" + convertActionToString(this.filterAction) + "\r\n");
		sb.append("compiledfilter=" 
				+ (this.compiledFilter == null ? "[null]" : this.compiledFilter.pattern()) + "\r\n");
		sb.append("isusingexplicitregex=" + this.isUsingExplicitRegex + "\r\n");
		try {
			FileUtils.writeStringToFile(new File("./markovdb/" + this.id + "/wordfilter.ini"), sb.toString().trim(), StandardCharsets.UTF_8);
		} catch (IOException e) {
			logger.error("unable to save wordfilter for guild " + IdUtils.getFormattedServer(this.id)
					+ " in WordFilter.saveToDisk()");
			e.printStackTrace();
		}
	}
	
	
	
	
}
