package my.cute.discordbot.commands;

import my.cute.discordbot.IdUtils;
import net.dv8tion.jda.core.events.message.priv.PrivateMessageReceivedEvent;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CommandManager {

	@SuppressWarnings("unused")
	private static final Logger logger = LoggerFactory.getLogger(CommandManager.class);
	private static CommandSet privateCommands;
	private static CommandSet guildCommands;
	
	public static void build() {
		//call commandfactory.<methods> and assign to this.commands
	}
	
	//given a message event, attempts to obtain and return the server id of the target server
	//returns the server id, or -1 if it cant be determined
	public static long obtainTargetGuildId(PrivateMessageReceivedEvent e) {
		
		try {
			String msg = sanitize(e.getMessage().getContentRaw());
			String[] words = msg.split(" ");
			//all commands assume server id is the last parameter, so check it
			long serverId = Long.parseLong(words[words.length - 1]);
			if(IdUtils.isValidServer(serverId)) return serverId;
			//non-guild id long provided by user
			//use default guild
			//every admin should have a default guild, added when they become an admin of a guild
			//non-admin users won't have an entry in defaultGuilds, so check for that
			Long defaultGuildId = PermissionsManager.getDefaultGuild(e.getAuthor().getIdLong());
			return (defaultGuildId == null ? -1L : defaultGuildId.longValue());
		} catch (NumberFormatException e1) {
			//last argument wasn't a long
			//likely they're using default guild
			//in any case, get default guild value
			Long defaultGuildId = PermissionsManager.getDefaultGuild(e.getAuthor().getIdLong());
			return (defaultGuildId == null ? -1L : defaultGuildId.longValue());
		}
	}
	
	public static Command obtainPrivateCommand(String message) {
		if(StringUtils.isBlank(message)) return null;
		String givenCommand = message.split(" ")[0];
		return privateCommands.getCommand(givenCommand);
	}
	
	public static Command obtainGuildCommand(String message) {
		if(StringUtils.isBlank(message)) return null;
		String givenCommand = message.split(" ")[0];
		return guildCommands.getCommand(givenCommand);
	}
	
	
	
	private static String sanitize(String s) {
		return s.replaceAll("\\s+", " ").trim();
	}
	
}
