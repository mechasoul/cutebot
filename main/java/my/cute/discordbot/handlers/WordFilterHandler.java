package my.cute.discordbot.handlers;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.stream.Collectors;

import my.cute.discordbot.IdUtils;
import my.cute.discordbot.preferences.FilterResponse;
import my.cute.discordbot.preferences.GuildPreferences;
import my.cute.discordbot.preferences.PreferencesManager;
import net.dv8tion.jda.core.MessageBuilder;
import net.dv8tion.jda.core.Permission;
import net.dv8tion.jda.core.entities.Guild;
import net.dv8tion.jda.core.entities.Member;
import net.dv8tion.jda.core.entities.PermissionOverride;
import net.dv8tion.jda.core.entities.PrivateChannel;
import net.dv8tion.jda.core.entities.TextChannel;
import net.dv8tion.jda.core.entities.User;
import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent;
import net.dv8tion.jda.core.exceptions.HierarchyException;
import net.dv8tion.jda.core.exceptions.InsufficientPermissionException;
import net.dv8tion.jda.core.utils.PermissionUtil;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class WordFilterHandler {
	
	private final Logger logger = LoggerFactory.getLogger(WordFilterHandler.class);
	private final long id;
	
	public WordFilterHandler(long i) {
		this.id = i;
	}
	
	public boolean handle(GuildMessageReceivedEvent e) {
		Guild g = e.getGuild();
		User u = e.getAuthor();
		GuildPreferences preferences = PreferencesManager.getGuildPreferences(this.id);
		String guildString = IdUtils.getFormattedServer(this.id);
		boolean shouldProcess = true;
		//flags:
		//1: don't add filtered messages to database
		//2: send dm to user to warn them
		//3: delete msg
		//4: remove users message send permission
		//5: kick user
		//6: ban user
		String match = getBannedPhrase(e.getMessage().getContentDisplay());
		MessageBuilder response = new MessageBuilder();
		if(match != null) {
			response.append("dear valued user,\r\nyour recent message in server " + guildString
					+ " contained the following banned phrase:\r\n`" + match + "`\r\n");
			EnumSet<FilterResponse> responseAction = preferences.getWordFilterResponseAction();
			if(responseAction.contains(FilterResponse.SKIP_PROCESS)) {
				shouldProcess = false;
			}
			if(responseAction.contains(FilterResponse.DELETE_MESSAGE)) {
				try {
					e.getMessage().delete().queue(v -> response.append("your message has been deleted. "));
				} catch(InsufficientPermissionException ex) {
					logger.info("no permission to delete message w/ banned phrase in server " + guildString
							+ " in WordFilterHandler.handle()");
				} catch(Exception ex) {
					logger.warn("unknown exception: " + ex.getMessage() + " when trying to delete message"
							+ " w/ banned phrase in server " + guildString + " in WordFilterHandler.handle()");
					ex.printStackTrace();
				}
			}
			if(responseAction.contains(FilterResponse.MUTE)) {
				Member m = g.getMember(u);
				List<TextChannel> permittedChannels = getUserPermittedChannels(g, u);
				List<TextChannel> changedChannels = new ArrayList<TextChannel>();
				for(TextChannel c : permittedChannels) {
					try {
						PermissionOverride perm = c.getPermissionOverride(m);
						if(perm == null) {
							c.createPermissionOverride(m).setDeny(Permission.MESSAGE_WRITE).complete();
						} else {
							perm.getManager().deny(Permission.MESSAGE_WRITE);
						}
						changedChannels.add(c);
					} catch(InsufficientPermissionException ex) {
						logger.info("insufficient permission to remove user " 
								+ IdUtils.getFormattedUser(u.getIdLong())
								+ "'s message_write permission in channel "
								+ IdUtils.getFormattedChannel(c.getIdLong(), this.id)
								+ " in guild " + guildString);
					} catch(Exception ex) {
						logger.warn("unknown exception: " + ex.getMessage() + " when trying to remove "
								+ "message_write permission for user " + IdUtils.getFormattedUser(u.getIdLong())
								+ " in channel " + IdUtils.getFormattedChannel(e.getChannel().getIdLong(), this.id)
								+ " in server " + guildString + " in WordFilterHandler.handle()");
					}
				}
				if(!changedChannels.isEmpty()) {
					response.append("you can no longer send messages in the following channels: ");
					String channelList = StringUtils.join(
							changedChannels.stream().map(channel 
									-> IdUtils.getFormattedChannel(channel.getIdLong(), this.id))
									.collect(Collectors.toList()), ",");
					response.append(channelList);
					response.append(". ");
				}
			}
			if(responseAction.contains(FilterResponse.KICK)) {
				//TODO kick and ban after msg is sent? or cant message them probably
				try {
					g.getController().kick(u.getId()).complete();
					response.append("you have been kicked from the server. ");
				} catch (InsufficientPermissionException | HierarchyException ex) {
					logger.info("insufficient permission to kick user " + u.getId() + " (" 
							+ u.getName() + ") from guild " + guildString);
				}
			}
			if(responseAction.contains(FilterResponse.BAN)) {
				try {
					g.getController().ban(u, 0).complete();
					response.append("you have been banned from the server. ");
				} catch (InsufficientPermissionException | HierarchyException ex) {
					logger.info("insufficient permission to ban user " + u.getId() + " (" 
							+ u.getName() + ") from guild " + guildString);
				}
			}
			if(responseAction.contains(FilterResponse.SEND_RESPONSE)) {
				response.append("\r\n\nif you have any questions or concerns, please contact your resident administrator\n");
				response.append("yours truly,\r\n");
				response.append("http://i.imgur.com/20wmYfp.png\r\n");
				response.append("cutebot");
				PrivateChannel chan = u.openPrivateChannel().complete();
				chan.sendMessage(response.build()).queue();
			}
		}
		
		return shouldProcess;
	}
	
	//checks given message against server's bannedPhrase regex
	//returns the first matching string, or null if no match is found
	private String getBannedPhrase(String message) {
		return PreferencesManager.getGuildPreferences(this.id).checkStringAgainstFilter(message);
	}
	
	private List<TextChannel> getUserPermittedChannels(Guild guild, User user) {
		List<TextChannel> userChannels = new ArrayList<TextChannel>();
		List<TextChannel> allChannels = guild.getTextChannels();
		Member member = guild.getMember(user);
		for(TextChannel channel : allChannels) {
			if(PermissionUtil.checkPermission(channel, member, Permission.MESSAGE_WRITE)) {
				userChannels.add(channel);
			}
		}
		return userChannels;
	}

}
