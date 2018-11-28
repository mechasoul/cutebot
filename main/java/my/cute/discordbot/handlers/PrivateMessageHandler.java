package my.cute.discordbot.handlers;

import my.cute.discordbot.IdUtils;
import my.cute.discordbot.commands.Command;
import my.cute.discordbot.commands.CommandManager;
import my.cute.discordbot.commands.CommandResponseParamHolder;
import my.cute.discordbot.commands.PermissionsManager;
import net.dv8tion.jda.core.events.message.priv.PrivateMessageReceivedEvent;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class PrivateMessageHandler {
	
	private static final Logger logger = LoggerFactory.getLogger(PrivateMessageHandler.class);
	private static final Logger pmLogger = LoggerFactory.getLogger("PMLogger");

	public void handle(PrivateMessageReceivedEvent e) {
		pmLogger.info(IdUtils.getFormattedUser(e.getAuthor().getId()) + ": " + e.getMessage().getContentRaw());
		
		//sanitize messages from non-devs
		//limit to 200 chars (prob enough for any reasonable regex...)
		//and remove excess whitespace
		String workingMessage;
		if(PermissionsManager.isDeveloper(e.getAuthor().getIdLong())) {
			workingMessage = e.getMessage().getContentRaw();
		} else {
			workingMessage = sanitize(e.getMessage().getContentRaw());
		}
		
		//first make sure whats given is a command
		//attempt to retrieve the command matching their msg
		//if it's not a cmd, we get null
		Command command = CommandManager.obtainPrivateCommand(workingMessage);
		if(command == null) {
			e.getChannel().sendMessage("?? try !help").queue();
			return;
		} else {
			
			//at this point we've got a valid command and it's a private one
			//if it requires a target server, we need to obtain that and do permission check
			//otherwise, we just check permission
			CommandResponseParamHolder params = new CommandResponseParamHolder(e);
			params.setMessage(workingMessage);
			if (command.requiresTargetServer()) {
				//attempt to get target server
				//if no server provided, uses default server
				long serverId = CommandManager.obtainTargetGuildId(e);
				//admins should never return -1L here, because admins should
				//always have an entry in PermissionsManager.defaultGuilds
				//regular users however won't necessarily have default guild set
				//(unless they do it manually)
				if (serverId == -1L) {
					logger.info("unable to obtain default guild for user " 
							+ IdUtils.getFormattedUser(e.getAuthor().getIdLong())
							+ "'s command, message: " + workingMessage);
					e.getChannel()
					.sendMessage("error: no valid server id specified "
							+ "(provide a server id or set your default server with !default)")
					.queue();
					return;
				}
				//check if they have permission for that server
				if (!PermissionsManager.userHasPermission(e.getAuthor().getIdLong(), serverId,
						command.getReqPermission())) {
					//error msg
					//don't show it was a command they dont have permission for, because idk
					e.getChannel().sendMessage("?? try !help").queue();
					return;
				}
				
				params.setTargetGuild(serverId);
				
			} else {
				
				if (!PermissionsManager.userHasPermission(e.getAuthor().getIdLong(), 
						command.getReqPermission())) {
					//error msg
					//don't show it was a command they dont have permission for, because idk
					e.getChannel().sendMessage("?? try !help").queue();
					return;
				}
				
				params.setTargetGuild(-1L);
				
			}
			
			//they provided a valid command, server, and they have permission to use the command
			//execute it
			command.response(params);
			return;
		}
	}
	
	private static String sanitize(String s) {
		String out = s.replaceAll("\\s+", " ");
		return out.substring(0, (out.length() < 200 ? out.length() : 200)).trim();
	}
	
}
