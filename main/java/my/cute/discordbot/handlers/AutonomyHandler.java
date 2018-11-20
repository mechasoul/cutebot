package my.cute.discordbot.handlers;

import java.util.concurrent.ConcurrentLinkedQueue;

import my.cute.discordbot.preferences.PreferencesManager;
import net.dv8tion.jda.core.entities.Guild;

//buddy class to handle the autonomous message output stuff
public class AutonomyHandler {
	private boolean enabled;
	private long curMessageTime;
	private ConcurrentLinkedQueue<Long> recentMessageTimes;
	private int recentMessageCount, recentMessageThreshold;
	private final Guild guild;
	
	public AutonomyHandler(Guild g) {
		this.enabled = false;
		this.curMessageTime = 0;
		this.recentMessageTimes = new ConcurrentLinkedQueue<Long>();
		this.recentMessageCount = 0;
		this.recentMessageThreshold = 0;
		this.guild = g;
	}
	
	/*
	 * autonomy works as follows
	 * we track the number of messages sent recently
	 * (where recently is defined by messageTimeThreshold)
	 * we track them by maintaining a queue of all the times at which a msg was received
	 * and maintaining a count of how many messages have been received in total
	 * our queue holds an entry for each message received within the threshold
	 * so at any time its size represents the number of msgs received in the recent threshold
	 * once our total message count exceeds the queue size, we send a message
	 * and reset our total message received count to 0
	 * i dont remember why i decided to do it this way
	 * TODO is there any reason to not just scrap all of this,
	 * hold the time of the last autonomous msg, and check if current time - that time exceeds threshold?
	 * i think the this.recentMessageCount > this.recentMessageThreshold check not being >= 
	 * maybe changes things slightly but uhh this seems like a lot of junk for no reason
	 */
	public boolean handle() {
		if(!this.enabled) {
			return false;
		}
		this.curMessageTime = System.currentTimeMillis();
		
		this.recentMessageCount++;
		this.recentMessageTimes.add(this.curMessageTime);
		this.recentMessageThreshold++;
		//remove elements until the oldest message is within messageTimeThreshold
		while(this.curMessageTime - this.recentMessageTimes.peek() >= PreferencesManager.getGuildPreferences(this.guild.getIdLong()).autonomyThreshold) {
			this.recentMessageTimes.poll();
			this.recentMessageThreshold--;
		}
		
		if(this.recentMessageCount > this.recentMessageThreshold) {
			this.recentMessageCount = 0;
			return true;
		}
		
		return false;
	}
	
	public void enable() {
		this.enabled = true;
	}
	
	public void disable() {
		this.enabled = false;
	}
	
	public Guild getGuild() {
		return this.guild;
	}
}
