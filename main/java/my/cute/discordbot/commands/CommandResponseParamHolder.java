package my.cute.discordbot.commands;

import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent;
import net.dv8tion.jda.core.events.message.priv.PrivateMessageReceivedEvent;

//helper class to hold relevant information needed for a command to execute
public class CommandResponseParamHolder {
	
	public PrivateMessageReceivedEvent privateMessageEvent;
	public GuildMessageReceivedEvent guildMessageEvent;
	public long targetGuild;
	public String message;
	
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
	
}