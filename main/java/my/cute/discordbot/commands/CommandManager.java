package my.cute.discordbot.commands;

import java.util.List;

import my.cute.discordbot.IdUtils;
import my.cute.discordbot.preferences.PreferencesManager;
import net.dv8tion.jda.core.entities.Guild;
import net.dv8tion.jda.core.events.message.priv.PrivateMessageReceivedEvent;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CommandManager {

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
			String msg = sanitize(e.getMessage().getContentDisplay());
			String[] words = msg.split(" ");
			//all commands assume server id is the last parameter, so check it
			long serverId = Long.parseLong(words[words.length - 1]);
			if(PreferencesManager.isValidGuild(serverId)) return serverId;
			//no valid guild id specified
			//check if they're only in one server, and use it if so
			List<Guild> mutualGuilds = e.getAuthor().getMutualGuilds();
			if(mutualGuilds.size() == 1) return mutualGuilds.get(0).getIdLong();
			//they're in more than one guild
			//check if a default guild is specified
			Long defaultId = PermissionsManager.getDefaultGuild(e.getAuthor().getIdLong());
			if(defaultId != null) return defaultId.longValue();
			//no default guild specified
			//can't determine target server
			return -1L;
		} catch (NumberFormatException e1) {
			logger.info("WARNING numberformatexception in trying to parse id from user: " 
					+ IdUtils.getFormattedUser(e.getAuthor().getIdLong())
					+ "\r\nmsg: " + e.getMessage().getContentDisplay());
			return -1L;
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
