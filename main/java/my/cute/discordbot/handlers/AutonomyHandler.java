package my.cute.discordbot.handlers;

import my.cute.discordbot.preferences.PreferencesManager;

//buddy class to handle the autonomous message output stuff
public class AutonomyHandler {
	private final long id;
	private long recentMessageTime;
	
	public AutonomyHandler(long i) {
		this.id = i;
		this.recentMessageTime = -1L;
	}
	
	/*
	 * autonomy works as follows
	 * returns true if it's time for an auto message, false if not
	 * if its disabled in server, return false
	 * starts counting from first message since our last auto message
	 * once it's been enough time, reset timer and return true
	 * next time a message is received, timer will be set to that message time
	 * and repeat
	 */
	public boolean handle() {
		
		if(!PreferencesManager.getGuildPreferences(this.id).autonomyEnabled()) {
			return false;
		}
		
		if(this.recentMessageTime == -1L) {
			this.recentMessageTime = System.currentTimeMillis();
		} else {
			if((System.currentTimeMillis() - recentMessageTime) >= PreferencesManager.getGuildPreferences(this.id).getAutonomyTimer()) {
				this.recentMessageTime = -1L;
				return true;
			}
		}
		
		return false;
	}
	
}
