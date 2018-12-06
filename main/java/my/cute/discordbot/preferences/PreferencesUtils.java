package my.cute.discordbot.preferences;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import net.dv8tion.jda.core.entities.Message;

public class PreferencesUtils {
	
	/*
	 * determines which channels from a guild are "discussion channels"
	 * input: a map of textchannel ids -> list of messages from that textchannel
	 * (as obtained by GuildScraper, probably)
	 * output: a set of the textchannel ids that qualify as discussion channels
	 * "discussion channels" are going to be active chatting channels and are not gimmick channels,
	 * bot command channels, highly specific topic channels, etc
	 * 
	 * we do this by taking an overall "messages per time" of all provided channels
	 * any channel at least that active is a discussion channel
	 */
	public static Set<Long> determineDiscussionChannels(Map<Long, List<Message>> messages) {
		Set<Long> discChannels = new HashSet<Long>(messages.size() + 2);
		//this will hold the time contribution for each channel, so we don't 
		//have to recalculate it every time
		Map<Long, Long> durations = new ConcurrentHashMap<Long, Long>(messages.size() + 2);
		double totalMessages=0;
		double totalTime=0;
		
		//as far as i can tell, we need 2 passes
		//first to accumulate the total messages and time, then to check each entry
		//against the total messages per time
		for(Map.Entry<Long, List<Message>> entry : messages.entrySet()) {
			List<Message> channelMessages = entry.getValue();
			//0 elem lists will definitely throw an exception when we get(0)
			//1 elem lists will provide a duration of 0 due to how we calculate it
			//i think we can just safely skip any such candidates to avoid headaches
			//shouldn't affect any of our calculations? its as if they just dont exist
			if(channelMessages.size() > 1) {
				//we assume the list is ordered from newest to oldest
				//this is true for jda's messagehistory class
				long duration = (channelMessages.get(0).getCreationTime().toEpochSecond() -
						channelMessages.get(channelMessages.size() - 1).getCreationTime().toEpochSecond());
				durations.put(entry.getKey(), duration);
				totalTime += duration;
				totalMessages += channelMessages.size();
			}
		}
		
		double totalMessagesPerSecond = totalMessages / totalTime;
		
		//second pass
		//i guess i could iterate over durations instead here and itd be technically faster
		//whatever
		for(Map.Entry<Long, List<Message>> entry : messages.entrySet()) {
			List<Message> channelMessages = entry.getValue();
			if(channelMessages.size() > 1) {
				//duration exists for any channel with size > 1
				//shouldnt have any exceptions anywhere here as long as maps arent somehow modified during this
				//(shouldnt be)
				double channelMessagesPerSecond = ((double)channelMessages.size()) / ((double)durations.get(entry.getKey()));
				if(channelMessagesPerSecond >= totalMessagesPerSecond)
					discChannels.add(entry.getKey());
			} 
		}
		
		return discChannels;
	}
	
}
