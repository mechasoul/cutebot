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
	
	@SuppressWarnings("unused")
	private static final Logger logger = LoggerFactory.getLogger(PrivateMessageHandler.class);
	private static final Logger pmLogger = LoggerFactory.getLogger("pmlog");

	public void handle(PrivateMessageReceivedEvent e) {
		pmLogger.info(IdUtils.getFormattedUser(e.getAuthor().getId()) + ": " + e.getMessage().getContentRaw());
		
		//first make sure whats given is a command
		//attempt to retrieve the command matching their msg
		//if it's not a cmd, we get null
		String sanitizedMessage = sanitize(e.getMessage().getContentDisplay());
		Command command = CommandManager.obtainPrivateCommand(sanitizedMessage);
		if(command == null) {
			e.getChannel().sendMessage("?? try !help").queue();
			return;
		} else {
			
			//at this point we've got a valid command and it's a private one
			//if it requires a target server, we need to obtain that and do permission check
			//otherwise, we just check permission
			CommandResponseParamHolder params = new CommandResponseParamHolder(e);
			params.message = sanitizedMessage;
			if (command.requiresTargetServer) {
				//attempt to get target server
				long serverId = CommandManager.obtainTargetGuildId(e);
				//error check it. invalid if they gave bad input
				if (serverId == -1L) {
					e.getChannel()
					.sendMessage("error: no valid server id specified")
					.queue();
					return;
				}
				//check if they have permission for that server
				if (!PermissionsManager.userHasPermission(e.getAuthor().getIdLong(), serverId,
						command.reqPermission)) {
					//error msg
					//don't show it was a command they dont have permission for, because idk
					e.getChannel().sendMessage("?? try !help").queue();
					return;
				}
				
				params.targetGuild = serverId;
				
			} else {
				
				if (!PermissionsManager.userHasPermission(e.getAuthor().getIdLong(), 
						command.reqPermission)) {
					//error msg
					//don't show it was a command they dont have permission for, because idk
					e.getChannel().sendMessage("?? try !help").queue();
					return;
				}
				
				params.targetGuild = -1L;
				
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
