package my.cute.discordbot.commands;

import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent;
import net.dv8tion.jda.core.events.message.priv.PrivateMessageReceivedEvent;

//helper class to hold relevant information needed for a command to execute
public class CommandResponseParamHolder {
	
	private PrivateMessageReceivedEvent privateMessageEvent;
	private GuildMessageReceivedEvent guildMessageEvent;
	private long targetGuild;
	private String message;
	
	public CommandResponseParamHolder() {
		this.privateMessageEvent = null;
		this.targetGuild = -1L;
		this.message = null;
	}
	
	public CommandResponseParamHolder(PrivateMessageReceivedEvent e) {
		this.privateMessageEvent = e;
		this.guildMessageEvent = null;
		this.targetGuild = -1L;
		this.message = null;
	}
	
	public CommandResponseParamHolder(GuildMessageReceivedEvent e) {
		this.privateMessageEvent = null;
		this.guildMessageEvent = e;
		this.targetGuild = -1L;
		this.message = null;
	}
	
	public CommandResponseParamHolder(PrivateMessageReceivedEvent e, long t, String m) {
		this.privateMessageEvent = e;
		this.targetGuild = t;
		this.message = m;
	}

	public PrivateMessageReceivedEvent getPrivateMessageEvent() {
		return privateMessageEvent;
	}

	public void setPrivateMessageEvent(PrivateMessageReceivedEvent privateMessageEvent) {
		this.privateMessageEvent = privateMessageEvent;
	}

	public GuildMessageReceivedEvent getGuildMessageEvent() {
		return guildMessageEvent;
	}

	public void setGuildMessageEvent(GuildMessageReceivedEvent guildMessageEvent) {
		this.guildMessageEvent = guildMessageEvent;
	}

	public long getTargetGuild() {
		return targetGuild;
	}

	public void setTargetGuild(long targetGuild) {
		this.targetGuild = targetGuild;
	}

	public String getMessage() {
		return message;
	}

	public void setMessage(String message) {
		this.message = message;
	}
	
}