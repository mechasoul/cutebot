package my.cute.discordbot.commands;

import my.cute.discordbot.IdUtils;
import my.cute.discordbot.bot.demo;
import net.dv8tion.jda.core.events.message.priv.PrivateMessageReceivedEvent;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CommandManager {

	@SuppressWarnings("unused")
	private static final Logger logger = LoggerFactory.getLogger(CommandManager.class);
	private static CommandSet privateCommands;
	private static CommandSet guildCommands;
	
	public static void load() {
		//call commandfactory.<methods> and assign to this.commands
		CommandFactory factory = new CommandFactory();
		privateCommands = factory.addPrivateCommands()
			.addDevCommands()
			.addUserCommands()
			.build();
		factory.clean();
		guildCommands = factory.addGuildCommands()
				.build();
	}
	
	//given a message event, attempts to obtain and return the server id of the target server
	//returns the server id, or -1 if it cant be determined
	public static long obtainTargetGuildId(PrivateMessageReceivedEvent e) throws IllegalArgumentException {
		
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
			return obtainDefaultGuildId(e.getAuthor().getIdLong());
		} catch (NumberFormatException e1) {
			//last argument wasn't a long
			//likely they're using default guild
			//in any case, get default guild value
			return obtainDefaultGuildId(e.getAuthor().getIdLong());
		}
	}
	
	//get default guild for a given user
	//check their entry in default guilds map. if it's nonnull, one has been set
	//check to make sure it's still a valid guild (bot is in it). if so, return it
	//if not, throw exception
	//if default guild was null, no default is set. returns -1 in that case
	private static long obtainDefaultGuildId(long userId) throws IllegalArgumentException {
		Long defaultGuildId = PermissionsManager.getDefaultGuild(userId);
		if(defaultGuildId != null) {
			if(demo.getJda().getGuildById(defaultGuildId) != null) {
				return defaultGuildId.longValue();
			} else {
				throw new IllegalArgumentException("guild id '" + defaultGuildId 
						+ "' is not a valid id for any cutebot server");
			}
		} else {
			return -1L;
		}
	}
	
	public static Command obtainPrivateCommand(String message) {
		if(StringUtils.isBlank(message)) return null;
		String givenCommand = message.split(" ")[0];
		return privateCommands.getCommand(givenCommand);
	}
	
	public static Command obtainGuildCommand(String message) {
		String sanitizedMessage = sanitize(message);
		if(StringUtils.isBlank(sanitizedMessage)) return null;
		String givenCommand = sanitizedMessage.split(" ")[0];
		return guildCommands.getCommand(givenCommand);
	}
	
	
	
	private static String sanitize(String s) {
		return s.replaceAll("\\s+", " ").trim();
	}
	
}
