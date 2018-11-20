package my.cute.discordbot.music;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import my.cute.discordbot.bot.demo;
import net.dv8tion.jda.core.entities.GuildVoiceState;
import net.dv8tion.jda.core.entities.Member;
import net.dv8tion.jda.core.entities.User;
import net.dv8tion.jda.core.entities.VoiceChannel;

public class UserListManager {

	private Set<User> permittedUsers;
	private Set<User> skippingUsers;
	private final String guildId;
	private int votesToSkip;
	
	public UserListManager(String id) {
		this.permittedUsers = new HashSet<User>();
		this.skippingUsers = new HashSet<User>();
		this.guildId = id;
		this.votesToSkip=0;
	}
	
	//after this is called, permittedUsers will contain all non-bot users in the voice channel with us
	public void updatePermittedUsers() {
		//grab the relevant channel for the given guild
		GuildVoiceState state = demo.getJda().getGuildById(this.guildId).getMemberById("312807432839626753").getVoiceState();
		if(state.inVoiceChannel()) {
			this.permittedUsers = getUsersWithoutBots(state.getChannel().getMembers());
			this.votesToSkip = (int) Math.ceil((double) permittedUsers.size() / 2);
		} else {
			//error! shouldnt happen but maybe can because of concurrency stuff
			//permit everyone. this is lazy but idk
			this.permittedUsers = getUsersWithoutBots(demo.getJda().getGuildById(this.guildId).getMembers());
			this.votesToSkip = 1;
		}
		
	}
	
	//use this if theres a voicechannel to provide
	public void updatePermittedUsers(VoiceChannel v) {
		this.permittedUsers = getUsersWithoutBots(v.getMembers());
		this.votesToSkip = (int) Math.ceil((double) permittedUsers.size() / 2);
	}
	
	//clear the set of users who have voted to skip
	public void emptySkippingUsers() {
		skippingUsers.clear();
	}
	
	//turn a list of members into a set of users, ignoring bots
	public Set<User> getUsersWithoutBots(List<Member> members) {
		Set<User> s = new HashSet<User>();
		for(Member m : members) {
			//ignore bot users
			if(!m.getUser().isBot()) {
				s.add(m.getUser());
			}
		}
		
		return s;
	}
	
	public Set<User> getPermittedUsers() {
		return this.permittedUsers;
	}
	
	public Set<User> getSkippingUsers() {
		return this.skippingUsers;
	}
	
	public int currentSkippingSize() {
		return this.skippingUsers.size();
	}
	
	public int requiredVotesToSkip() {
		return this.votesToSkip;
	}
	
	public boolean addSkippingUser(User u) {
		return this.skippingUsers.add(u);
	}
}
