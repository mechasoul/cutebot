package my.cute.discordbot.commands;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Random;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import my.cute.discordbot.IdUtils;
import my.cute.discordbot.bot.DatabaseManager;
import my.cute.discordbot.bot.demo;
import my.cute.discordbot.commands.Command.CommandType;
import my.cute.discordbot.preferences.GuildPreferences;
import my.cute.discordbot.preferences.PreferencesManager;
import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.MessageBuilder;
import net.dv8tion.jda.core.Permission;
import net.dv8tion.jda.core.entities.Game;
import net.dv8tion.jda.core.entities.Guild;
import net.dv8tion.jda.core.entities.Message;
import net.dv8tion.jda.core.entities.TextChannel;
import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent;
import net.dv8tion.jda.core.events.message.priv.PrivateMessageReceivedEvent;
import net.dv8tion.jda.core.exceptions.InsufficientPermissionException;
import net.dv8tion.jda.core.utils.PermissionUtil;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.LineIterator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CommandFactory {
	
	private static final Logger logger = LoggerFactory.getLogger(Command.class);
	private Set<Command> workingSet;
	
	public CommandFactory() {
		this.workingSet = new HashSet<Command>(20);
	}
	
	public CommandFactory addServerCommands() {
		//for adjusting the wordfilter
		//if user shares one server, they dont need to specify which server
		//use: !filter <mode> [<args>] [<server id>]
		//modes: add, remove, view, regex, action, clear
		//TODO add set mode
		String name = "!filter";
		Command c = new Command(name, RequiredPermissionLevel.ADMIN, true) {
			public void response(CommandResponseParamHolder parameterObject) {
				PrivateMessageReceivedEvent e = parameterObject.getPrivateMessageEvent();
				long targetGuild = parameterObject.getTargetGuild();
				String words[] = parameterObject.getMessage().split(" ");
				MessageBuilder response = new MessageBuilder();
				
				//quick error check
				if(words.length < 2) {
					e.getChannel().sendMessage("error: missing arguments").queue();
					return;
				}

				//get preferences
				GuildPreferences preferences = PreferencesManager.getGuildPreferences(targetGuild);

				//filter add case
				//use: !filter add <words> [<server id>]
				if(words[1].equalsIgnoreCase("add")) {
					
					if(words.length < 3) {
						e.getChannel().sendMessage("error: missing arguments").queue();
						return;
					}
					
					String splitMsg[] = parameterObject.getMessage().split("\"", 3);
					if(splitMsg.length < 3) {
						e.getChannel().sendMessage("error: word list not surrounded with \"\"").queue();
						return;
					} 

					if(preferences.addWordsToFilter(splitMsg[1])) {
						response.append("word filter for server " + IdUtils.getFormattedServer(targetGuild)
								+ " has been updated. current filtered words: `" 
								+ preferences.getFilteredWords() + "`\r\n");
					} else {
						response.append("no change has been made to the filter for server "
								+ IdUtils.getFormattedServer(targetGuild) + ". maybe you're already filtering those words, "
								+ "or your filter is too large?\r\n");
					}
					if(preferences.isUsingExplicitRegex()) {
						response.append("note: server " + IdUtils.getFormattedServer(targetGuild)
								+ " is currently using an admin-specified regex as its filter and no changes "
								+ "via `!filter add` will currently be used. if you did not set this, consult your "
								+ "fellow administrators\r\n");
					}
					
					
					
				//filter removal case
				//use: !filter remove "<words>" [<server id>]
				} else if(words[1].equalsIgnoreCase("remove")) {

					if(words.length < 3) {
						e.getChannel().sendMessage("error: missing arguments").queue();
						return;
					}
					
					String splitMsg[] = parameterObject.getMessage().split("\"", 3);
					if(splitMsg.length < 3) {
						e.getChannel().sendMessage("error: word list not surrounded with \"\"").queue();
						return;
					} 
					
					if(preferences.removeWordsFromFilter(splitMsg[1])) {
						response.append("word filter for server " + IdUtils.getFormattedServer(targetGuild)
								+ " has been updated. current filtered words: `" 
								+ preferences.getFilteredWords() + "`\r\n");
					} else {
						response.append("no change has been made to the filter for server "
								+ IdUtils.getFormattedServer(targetGuild) + ". maybe those words weren't being filtered?\r\n");
					}
					if(preferences.isUsingExplicitRegex()) {
						response.append("note: server " + IdUtils.getFormattedServer(targetGuild)
								+ " is currently using an admin-specified regex as its filter and no changes "
								+ "via `!filter remove` will currently be used. if you did not set this, consult your "
								+ "fellow administrators\r\n");
					}
					
					
					//filter view case
					//use: !filter view [<server id>]
				} else if(words[1].equalsIgnoreCase("view")) {
					//explicit regex needs to be handled separately
					if(preferences.isUsingExplicitRegex()) {
						response.append("current wordfilter for server " + IdUtils.getFormattedServer(targetGuild)
								+  " has been set to the following regex:\r\n`" 
								+ preferences.getExplicitRegex() + "`\r\n");
						response.append("the current (inactive) list of filtered words is: `"
								+ preferences.getFilteredWords() + "`\r\n");
					} else {
						response.append("current wordfilter for server " + IdUtils.getFormattedServer(targetGuild)
								+ ": `" + preferences.getFilteredWords() + "`\r\n");
					}
					
					response.append("the following actions will be taken when a user triggers the wordfilter:\r\n"
							+ formatFlagString(preferences.getWordFilterResponseFlags()));
					//filter clear case
					//use: !filter clear [<server id>]
				} else if(words[1].equalsIgnoreCase("clear")) {
					preferences.clearFilter();
					response.append("wordfilter for server " + IdUtils.getFormattedServer(targetGuild)
							+ " has been cleared\r\n");
					//set filter to regex case (for advanced use)
					//use: !filter regex <regex> [<server id>]
				} else if(words[1].equalsIgnoreCase("regex")) {

					if(words.length < 3) {
						e.getChannel().sendMessage("error: no regex supplied").queue();
						return;
					}
					
					String regex = parameterObject.getMessage().split(" ", 3)[2];

					//update pattern with the provided regex
					if(preferences.setExplicitRegex(regex)) {
						response.append("wordfilter for server " + IdUtils.getFormattedServer(targetGuild)
								+ " has been set to the following regex: \r\n`"
								+ preferences.getExplicitRegex() + "`\r\n");
					} else {
						e.getChannel().sendMessage("error: bad syntax on given regex").queue();
						return;
					}
					//specify action to take on someone using a banned phrase
					//use: !filter action <flags> [<server id>]
				} else if(words[1].equalsIgnoreCase("action")) {
					
					if(words.length < 3) {
						e.getChannel().sendMessage("error: missing arguments").queue();
						return;
					}
					//flags:
					//1: don't add filtered messages to database
					//2: send dm to user to warn them
					//3: delete msg
					//4: remove users message send permission
					//5: kick user
					//6: ban user
					if(words[2].length() > 6) {
						e.getChannel().sendMessage("error: too many flags supplied").queue();
						return;
					} else if(words[2].matches("[^123456]")) {
						e.getChannel().sendMessage("error: invalid flags").queue();
						return;
					}
					preferences.setWordFilterResponse(words[2]);
					response.append("the following actions will now be taken in server " 
							+ IdUtils.getFormattedServer(targetGuild) + " when a user triggers the wordfilter:\r\n"
							+ formatFlagString(words[2]));
					return;
				} else {
					e.getChannel().sendMessage("error: invalid mode of operation").queue();
					return;
				}
				e.getChannel().sendMessage(response.build()).queue();
			}
		};
		this.workingSet.add(c);

		//for adjusting permissions
		//can be used by admins but with restrictions (only on servers they have admin powers on)
		//TODO should probably pm people whose permissions get changed
		name = "!admin";
		c = new Command(name, RequiredPermissionLevel.ADMIN, true) {
			public void response(CommandResponseParamHolder parameterObject) {
				PrivateMessageReceivedEvent e = parameterObject.getPrivateMessageEvent();
				long targetGuild = parameterObject.getTargetGuild();
				String words[] = parameterObject.getMessage().split(" ");
				//words: 0 = command, 1 = mode of operation, 2 = user id, 3 = server id
				//modes: add, remove, view
				//syntax check
				if(words.length < 2) {
					e.getChannel().sendMessage("error: incorrect use of !admin command").queue();
					return;
				}
				
				//adding permission
				//use: !admin add <user id> [<server id>]
				//consider adding ability to add multiple servers at once
				if(words[1].equalsIgnoreCase("add")) {
					//syntax check
					if(words.length < 3 || words.length > 4) {
						e.getChannel().sendMessage("error: incorrect use of !admin command").queue();
						return;
					}
					//words[] has at least 3 elements by above check
					//check the user id in the server
					if(demo.getJda().getGuildById(targetGuild).getMemberById(words[2]) == null) {
						e.getChannel().sendMessage("error: no user with id " + words[2] + " exists in server " 
								+ IdUtils.getFormattedServer(targetGuild)).queue();
						return;
					}
					//should never throw numberformatexception by above checks
					if(PermissionsManager.addAdmin(Long.parseLong(words[2]), targetGuild)) {
						e.getChannel().sendMessage("user " + IdUtils.getFormattedUser(words[2], targetGuild)
								+ " has been given admin permissions for server " 
								+ IdUtils.getFormattedServer(targetGuild)).queue();
						return;
					} else {
						e.getChannel().sendMessage("error: user " + IdUtils.getFormattedUser(words[2], targetGuild) 
								+ " already has admin permissions for server "
								+ IdUtils.getFormattedServer(targetGuild)).queue();
						return;
					}
					//TODO should i be saving to disk somewhere in here?
					
					//admin removal case
					//use: !admin remove <user id> [<server id>]
				} else if(words[1].equalsIgnoreCase("remove")) {
					if(words.length < 3 || words.length > 4) {
						e.getChannel().sendMessage("error: incorrect use of !admin command").queue();
						return;
					}
					if(PermissionsManager.removeAdmin(Long.parseLong(words[2]), targetGuild)) {
						e.getChannel().sendMessage("user " + IdUtils.getFormattedUser(words[2], targetGuild)
								+ " has had their admin permissions for server "
								+ IdUtils.getFormattedServer(targetGuild) + " removed").queue();
						
						return;
					} else {
						e.getChannel().sendMessage("error: could not remove admin permissions for user " 
								+ IdUtils.getFormattedUser(words[2], targetGuild)
								+ " in server " + IdUtils.getFormattedServer(targetGuild)
								+ " (maybe they don't have admin permissions for that server? or they're server owner?)").queue();
						return;
					}
				} else if(words[1].equalsIgnoreCase("view")) {
					//admin view case
					//use: !admin view <server id>
					
					MessageBuilder mb = new MessageBuilder();
					mb.append("admins for server " + IdUtils.getFormattedServer(targetGuild) + ": \r\n");
					PermissionsManager.getServerAdmins(targetGuild)
						.forEach(userId -> mb.append(IdUtils.getFormattedUser(userId, targetGuild) + "\r\n"));
					e.getChannel().sendMessage(mb.build()).queue();
					return;
					
				} else {
					e.getChannel().sendMessage("error: invalid mode of operation").queue();
					return;
				}
			}
		};
		this.workingSet.add(c);

		//for allowing/disabling pings
		//use: !pings <mode> [<server>]
		//modes: off, on
		name = "!pings";
		c = new Command(name, RequiredPermissionLevel.ADMIN, true) {
			public void response(CommandResponseParamHolder parameterObject) {
				PrivateMessageReceivedEvent e = parameterObject.getPrivateMessageEvent();
				long targetGuild = parameterObject.getTargetGuild();
				String words[] = parameterObject.getMessage().split(" ");
				//words: 0 = command, 1 = mode of operation, 2 = server

				//error check
				if(words.length != 3 && words.length != 2) {
					e.getChannel().sendMessage("error: incorrect use of !pings command").queue();
					return;
				}


				//turn on pings
				if(words[1].equalsIgnoreCase("on")) {
					if(PreferencesManager.getGuildPreferences(targetGuild).pingsEnabled()) {
						e.getChannel().sendMessage("error: pings are already enabled for server "
								+ IdUtils.getFormattedServer(targetGuild)).queue();
						return;
					} else {
						PreferencesManager.getGuildPreferences(targetGuild).enablePings();
						e.getChannel().sendMessage("pings have been enabled for server " 
								+ IdUtils.getFormattedServer(targetGuild)).queue();
						return;
					}
				} else if(words[1].equalsIgnoreCase("off")) {
					//turn off pings
					if(!PreferencesManager.getGuildPreferences(targetGuild).pingsEnabled()) {
						e.getChannel().sendMessage("error: pings are already disabled for server "
								+ IdUtils.getFormattedServer(targetGuild)).queue();
						return;
					} else {
						PreferencesManager.getGuildPreferences(targetGuild).disablePings();;
						e.getChannel().sendMessage("pings have been disabled for server " 
								+ IdUtils.getFormattedServer(targetGuild)).queue();
						return;
					}
				} else {
					e.getChannel().sendMessage("error: invalid mode of operation").queue();
					return;
				}
			}
		};
		this.workingSet.add(c);
		
		//turns automatic messages for server on/off
		//modes: on, off, timer, view
		//!autoresponse <mode> [<parameter>] [<server>]
		//require time in minutes when using !autoresponse timer <time>
		name = "!autoresponse";
		c = new Command(name, RequiredPermissionLevel.ADMIN, true) {
			public void response(CommandResponseParamHolder parameterObject) {
				PrivateMessageReceivedEvent e = parameterObject.getPrivateMessageEvent();
				long targetGuild = parameterObject.getTargetGuild();
				String words[] = parameterObject.getMessage().split(" ");
				//words: 0 = command, 1 = mode of operation, 3+ = parameters
				//mode: on, off, timer, view

				//error check
				if(words.length != 3 && words.length != 2 && words.length != 4) {
					e.getChannel().sendMessage("error: incorrect use of !autoresponse command").queue();
					return;
				}
				
				if(words[1].equalsIgnoreCase("on")) {
					if(PreferencesManager.getGuildPreferences(targetGuild).autonomyEnabled()) {
						e.getChannel().sendMessage("error: automatic messages are already enabled"
								+ " for server " + IdUtils.getFormattedServer(targetGuild)).queue();
						return;
					} else {
						PreferencesManager.getGuildPreferences(targetGuild).enableAutonomy();
						e.getChannel().sendMessage("automatic messages have been enabled for server "
								+ IdUtils.getFormattedServer(targetGuild)).queue();
						return;
					}
				} else if(words[1].equalsIgnoreCase("off")) {
					if(!PreferencesManager.getGuildPreferences(targetGuild).autonomyEnabled()) {
						e.getChannel().sendMessage("error: automatic messages are already disabled"
								+ " for server " + IdUtils.getFormattedServer(targetGuild)).queue();
						return;
					} else {
						PreferencesManager.getGuildPreferences(targetGuild).disableAutonomy();
						e.getChannel().sendMessage("automatic messages have been disabled for server "
								+ IdUtils.getFormattedServer(targetGuild)).queue();
						return;
					}
				} else if(words[1].equalsIgnoreCase("timer")) {
					
					try {
						//error check
						if(words.length < 3) {
							e.getChannel().sendMessage("error: incorrect use of !autoresponse command").queue();
							return;
						}
						long newTimer = Long.parseLong(words[2]);
						if(newTimer <= 0 || newTimer > 525600) {
							e.getChannel().sendMessage("error: invalid duration").queue();
							return;
						}
						PreferencesManager.getGuildPreferences(targetGuild).setAutonomyTimer(newTimer * 60L * 1000L);
						e.getChannel().sendMessage("automatic message timer has been updated for server " 
								+ IdUtils.getFormattedServer(targetGuild)).queue();
						return;
					} catch (NumberFormatException ex) {
						logger.warn("invalid autonomy timer provided from user "
								+ IdUtils.getFormattedUser(e.getAuthor().getIdLong()) + ": " + words[2]);
						e.getChannel().sendMessage("error: invalid duration").queue();
						return;
					}
					
				} else if(words[1].equalsIgnoreCase("view")) {
					
					long timer = (PreferencesManager.getGuildPreferences(targetGuild).getAutonomyTimer() / 60000L);
					String status = PreferencesManager.getGuildPreferences(targetGuild).autonomyEnabled() ? "enabled" : "disabled";
					e.getChannel().sendMessage("automatic messages for server "
							+ IdUtils.getFormattedServer(targetGuild)
							+ " are currently " + status + ". time in between messages is set to " + timer + " minutes").queue();
					return;
					
				} else {
					
					e.getChannel().sendMessage("error: invalid mode of operation").queue();
					return;
					
				}
			}
		};
		
		this.workingSet.add(c);
		
		//for admin ability to manually change channels to be used for database
		//use: !channels <mode> [<parameter>] [<server>]
		//modes should be add, remove, set to a specified list, view
		name = "!channels";
		c = new Command(name, RequiredPermissionLevel.ADMIN, true) {
			
			@Override
			public void response(CommandResponseParamHolder parameterObject) {
				
				PrivateMessageReceivedEvent e = parameterObject.getPrivateMessageEvent();
				long targetGuild = parameterObject.getTargetGuild();
				String words[] = parameterObject.getMessage().split(" ");
				//words: 0 = command, 1 = mode of operation, 2 = server
				//modes: add, remove, set, view

				//error check
				if(words.length != 3 && words.length != 2 && words.length != 4) {
					e.getChannel().sendMessage("error: incorrect use of !channels command").queue();
					return;
				}
				
				if(words[1].equalsIgnoreCase("add")) {
					if(words.length < 3) {
						e.getChannel().sendMessage("error: incorrect use of !channels command").queue();
						return;
					}
					
					if(IdUtils.isValidChannel(words[2], targetGuild)) {
						if(PreferencesManager.getGuildPreferences(targetGuild).addDiscussionChannel(words[2])) {
							e.getChannel().sendMessage("will now process messages from channel "
									+ IdUtils.getFormattedChannel(words[2], targetGuild) + " in server "
									+ IdUtils.getFormattedServer(targetGuild)).queue();
							return;
						} else {
							e.getChannel().sendMessage("error: already processing messages from channel "
									+ IdUtils.getFormattedChannel(words[2], targetGuild) + " in server "
									+ IdUtils.getFormattedServer(targetGuild)).queue();
							return;
						}
					} else {
						e.getChannel().sendMessage("error: `" + words[2] + "` is not a valid channel id in server "
								+ IdUtils.getFormattedServer(targetGuild)).queue();
						return;
					}
					
				} else if(words[1].equalsIgnoreCase("remove")) {
					if(words.length < 3) {
						e.getChannel().sendMessage("error: incorrect use of !channels command").queue();
						return;
					}
					
					if(IdUtils.isValidChannel(words[2], targetGuild)) {
						if(PreferencesManager.getGuildPreferences(targetGuild).removeDiscussionChannel(words[2])) {
							e.getChannel().sendMessage("no longer processing messages from channel "
									+ IdUtils.getFormattedChannel(words[2], targetGuild) + " in server "
									+ IdUtils.getFormattedServer(targetGuild)).queue();
							return;
						} else {
							e.getChannel().sendMessage("error: already not processing messages from channel "
									+ IdUtils.getFormattedChannel(words[2], targetGuild) + " in server "
									+ IdUtils.getFormattedServer(targetGuild)).queue();
							return;
						}
						
					} else {
						e.getChannel().sendMessage("error: `" + words[2] + "` is not a valid channel id in server"
								+ IdUtils.getFormattedServer(targetGuild)).queue();
						return;
					}
					
				} else if(words[1].equalsIgnoreCase("set")) {
					if(words.length < 3) {
						e.getChannel().sendMessage("error: incorrect use of !channels command").queue();
						return;
					}
					
					String channels[] = words[2].trim().split(",");
					HashSet<Long> usedChannels = Arrays.asList(channels).stream()
							.filter(channelId -> IdUtils.isValidChannel(channelId, targetGuild))
							.map(Long::parseLong)
							.collect(Collectors.toCollection(HashSet::new));
					
					if(usedChannels.isEmpty()) {
						e.getChannel().sendMessage("error: no valid channel ids supplied").queue();
						return;
					}
					
					PreferencesManager.getGuildPreferences(targetGuild).setDiscussionChannels(usedChannels);
					StringBuilder sb = new StringBuilder();
					usedChannels.forEach(channelId -> sb.append(IdUtils.getFormattedChannel(channelId, targetGuild) + "\r\n"));
					e.getChannel().sendMessage("now processing messages from the following channels in server "
							+ IdUtils.getFormattedServer(targetGuild) + ": \r\n"
							+ sb.toString()).queue();
					return;
				} else if(words[1].equalsIgnoreCase("view")) {
					//TODO
				} else {
					e.getChannel().sendMessage("error: invalid mode of operation").queue();
					return;
				}
				
			}
		};
		
		this.workingSet.add(c);

		return this;
	}
	
	public CommandFactory addDevCommands() {
		//initiate shutdown
		String name = "!exit";
		Command c = new Command(name, RequiredPermissionLevel.DEV, false) {
			public void response(CommandResponseParamHolder parameterObject) {
				
				PrivateMessageReceivedEvent e = parameterObject.getPrivateMessageEvent();
				
				e.getChannel().sendMessage("ok").queue();
				
				/*
				 * following things need to be saved:
				 * database (1 per server)
				 * guildpreferences (1 per server)
				 * admin list (1 per server)
				 * wordfilter (1 per server)
				 * default guilds
				 * TODO daily quote index
				 * TODO music shit (playlists)
				 */
				
				demo.stopListening();
				
				DatabaseManager.save();
				PreferencesManager.save();
				PermissionsManager.save();
				
				demo.startShutdown();
				
			}
		};
		this.workingSet.add(c);
		
		name = "!rebuild";
		c = new Command(name, RequiredPermissionLevel.DEV, true) {
			public void response(CommandResponseParamHolder parameterObject) {
				
				//TODO add a save to file in here that future scrapes can use
				long targetGuild = parameterObject.getTargetGuild();
				parameterObject.getPrivateMessageEvent().getChannel().sendMessage("beginning rebuild on server "
						+ IdUtils.getFormattedServer(targetGuild)).queue();
				DatabaseManager.rebuild(targetGuild);
				
			}
		};
		
		this.workingSet.add(c);
		
		name = "!status";
		c = new Command(name, RequiredPermissionLevel.DEV, false) {
			public void response(CommandResponseParamHolder parameterObject) {
				
				String status = parameterObject.getMessage().substring(8);
				demo.getJda().getPresence().setGame(Game.playing(status));
				parameterObject.getPrivateMessageEvent().getChannel().sendMessage("status set").queue();
				
			}
		};
		
		this.workingSet.add(c);
		
		name = "!statusreset";
		c = new Command(name, RequiredPermissionLevel.DEV, false) {
			public void response(CommandResponseParamHolder parameterObject) {
				demo.getJda().getPresence().setGame(null);
				parameterObject.getPrivateMessageEvent().getChannel().sendMessage("status reset").queue();
			}
		};
		
		this.workingSet.add(c);
		
		name = "!users";
		c = new Command(name, RequiredPermissionLevel.DEV, true) {
			public void response(CommandResponseParamHolder parameterObject) {
				StringBuilder sb = new StringBuilder();
				Guild targetGuild = demo.getJda().getGuildById(parameterObject.getTargetGuild());
				sb.append("owner: " + IdUtils.getFormattedUser(targetGuild.getOwner().getUser().getIdLong()) + "\r\n");
				sb.append("members: \r\n");
				targetGuild.getMembers().forEach(
						member -> sb.append(IdUtils.getFormattedUser(member.getUser().getIdLong()) + "\r\n")
						);
				try {
					FileUtils.writeStringToFile(new File("./query/userlist-" + parameterObject.getTargetGuild()+".txt"), 
							sb.toString().trim(), "UTF-8");
					parameterObject.getPrivateMessageEvent().getChannel().sendMessage("userlist.txt successfully written "
							+ " for guild " + IdUtils.getFormattedServer(parameterObject.getTargetGuild())).queue();
				} catch (IOException e) {
					logger.error("couldn't write userlist.txt for guild " + IdUtils.getFormattedServer(parameterObject.getTargetGuild()));
					e.printStackTrace();
					parameterObject.getPrivateMessageEvent().getChannel().sendMessage("error writing userlist.txt").queue();
				}
				
			}
		};
		
		this.workingSet.add(c);
		
		//returns a list of all currently connected servers
		//syntax: !servers
		name = "!servers";
		c = new Command(name, RequiredPermissionLevel.DEV, false) {
			public void response(CommandResponseParamHolder parameterObject) {
				
				StringBuilder sb = new StringBuilder();
				demo.getJda().getGuilds().forEach(
						guild -> sb.append(IdUtils.getFormattedServer(guild.getIdLong()) + "\r\n")
				);
				parameterObject.getPrivateMessageEvent().getChannel().sendMessage("currently connected servers:\r\n"
						+ sb.toString().trim()).queue();
				
			}
		};
		
		this.workingSet.add(c);
		
		//sends a message to all servers
		//channel selection is: first any channels with cutebot in the name, oldest first
		//then any channels we have message_write permission in, oldest first
		//syntax is !megaphone [message]
		//message is selected exactly via substring()
		name = "!megaphone";
		c = new Command(name, RequiredPermissionLevel.DEV, false) {
			public void response(CommandResponseParamHolder parameterObject) {
				String message;
				PrivateMessageReceivedEvent e = parameterObject.getPrivateMessageEvent();
				
				try {
					message = parameterObject.getMessage().substring(11);
				} catch (IndexOutOfBoundsException ex) {
					e.getChannel().sendMessage("you did something wrong").queue();
					ex.printStackTrace();
					return;
				}
				
				//for tracking guilds where message wasn't sent, for debug
				List<Guild> failedGuilds = new ArrayList<Guild>();
				
				for(Guild g : demo.getJda().getGuilds()) {
					List<TextChannel> outputChannels = getMegaphoneChannels(g);
					boolean messageSuccessfullySent = false;
					int count=0;
					while(count < outputChannels.size() && !messageSuccessfullySent) {
						//on each iteration of loop, either we successfully send message,
						//or sendMessage() throws an exception and we increment count to go to the next channel
						try {
							outputChannels.get(count).sendMessage(message).complete();
							messageSuccessfullySent = true;
						} catch (InsufficientPermissionException ex) {
							logger.info("megaphone lacked message_write permissions on channel " 
									+ IdUtils.getFormattedChannel(outputChannels.get(count).getIdLong(), g.getIdLong())
									+ " in server " + IdUtils.getFormattedServer(g.getIdLong()));
							count++;
						} catch (Exception ex) {
							logger.warn("unknown exception: " + ex.getMessage() + " in megaphone output on channel "
									+ IdUtils.getFormattedChannel(outputChannels.get(count).getIdLong(), g.getIdLong())
									+ " in server " + IdUtils.getFormattedServer(g.getIdLong()));
							count++;
						}
					}
					
					if(!messageSuccessfullySent) {
						logger.warn("failed megaphone output on server " + IdUtils.getFormattedServer(g.getIdLong())
								+ " (no viable channels?)");
						failedGuilds.add(g);
					}
				}
				
				if(failedGuilds.isEmpty()) {
					e.getChannel().sendMessage("message sent to all servers").queue();
				} else {
					StringBuilder sb = new StringBuilder();
					failedGuilds.forEach(guild -> sb.append(IdUtils.getFormattedServer(guild.getIdLong()) + "\r\n"));
					e.getChannel()
						.sendMessage("failed to send message to the following guilds:\r\n" + sb.toString().trim()).queue();
				}
			}
		};
		
		this.workingSet.add(c);
		
		//force remove bot from a server
		//do i need this idk
		name = "!leaveserver";
		c = new Command(name, RequiredPermissionLevel.DEV, true) {
			public void response(CommandResponseParamHolder parameterObject) {
				
			}
		};
		
		return this;
	}
	
	public CommandFactory addGuildCommands() {
	
		String name = "!quote";
		Command c = new Command(name, RequiredPermissionLevel.USER, CommandType.GUILD, false) {
			public void response(CommandResponseParamHolder parameterObject) {
				String quote;
				//quote of the day info is stored in qotd.txt
				//first line is the date as written by LocalDateTime.toString()
				//second line is the quote used for the day
				if(Files.isRegularFile(Paths.get("./qotd.txt"))) {
					try (LineIterator it = FileUtils.lineIterator(new File("./qotd.txt"), "UTF-8")) {
						LocalDateTime storedDate = LocalDateTime.parse(it.nextLine());
						LocalDateTime currentDate = LocalDateTime.now();
						if(isSameDate(storedDate, currentDate)) {
							//use stored quote
							quote = it.nextLine();
						} else {
							//new day, generate new quote
							quote = generateNewQotd(currentDate);
						}
						
					} catch (NoSuchElementException ex) {
						//file missing required lines for some reason
						quote = "help me";
						ex.printStackTrace();
						logger.error("qotd.txt missing required lines in Command.response() for !quote guild command in CommandFactory:"
								+ ex.getMessage());
					} catch (IOException ex) {
						logger.error("i/o error in trying to read qotd.txt in Command.response() for !quote guild command "
								+ "in CommandFactory: " + ex.getMessage());
						quote = "i cant find the quotes so im taking a day off";
						ex.printStackTrace();
					}
				} else {
					//generate new qotd.txt on first run
					quote = generateNewQotd(LocalDateTime.now());
				}
				
				MessageBuilder message = new MessageBuilder();
				message.append("today's Twitch Chat:tm: Quote Of The Day (!quote)");
				EmbedBuilder embed = new EmbedBuilder();
				embed.setDescription(quote);
				message.setEmbed(embed.build());
				parameterObject.getGuildMessageEvent().getChannel().sendMessage(message.build()).queue();
				
			}
		};
		this.workingSet.add(c);
		
		name = "!exit";
		c = new Command(name, RequiredPermissionLevel.USER, CommandType.GUILD, false) {
			public void response(CommandResponseParamHolder parameterObject) {
				
				GuildMessageReceivedEvent e = parameterObject.getGuildMessageEvent();
				
				//send a mothwat
				
				try {
					MessageBuilder mb = new MessageBuilder();
					mb.append(e.getGuild().getEmotesByName("mothwat", true).get(0));
					e.getChannel().sendMessage(mb.build()).queue();
				} catch (IndexOutOfBoundsException ex) {
					//no such emote
					//attempt to send image file
					//ok so i think this tries to send file mothwat.png
					//parameters provided to queue() are consumers for success and failure
					//success callback is null so default action is taken on success
					//failure callback has us send message to channel w/ imgur link
					//so we should send that if trying to send img file fails
					e.getChannel().sendFile(new File("./mothwat.png"), (Message) null)
						.queue(null,
								(message -> e.getChannel().sendMessage("https://i.imgur.com/0sgFqGD.png").queue())
								);
				}
				
			}
		};
		this.workingSet.add(c);
		
		return this;
	}
	
	public CommandFactory addUserCommands() {
		
		//for changing user's default server
		//use: !default <arg>
		//valid arguments are: <server id>, view
		String name = "!default";
		Command c = new Command(name, RequiredPermissionLevel.USER, CommandType.PRIVATE, false) {
			public void response(CommandResponseParamHolder parameterObject) {
				
				PrivateMessageReceivedEvent e = parameterObject.getPrivateMessageEvent();
				String words[] = parameterObject.getMessage().split(" ");
				//words: 0 = command, 1 = argument
				
				if(words.length != 2) {
					e.getChannel().sendMessage("error: incorrect use of !default command").queue();
					return;
				}
				
				//to see currently set default guild
				if(words[1].equalsIgnoreCase("view")) {
					Long defaultValue = PermissionsManager.getDefaultGuild(e.getAuthor().getIdLong());
					if(defaultValue == null) {
						e.getChannel().sendMessage("you haven't set a default server").queue();
						return;
					} else {
						e.getChannel().sendMessage("your current default server is " 
								+ IdUtils.getFormattedServer(defaultValue)).queue();
						return;
					}
				} else {
					//to set default guild
					try {
						long defaultServer = Long.parseLong(words[1]);
						if(demo.getJda().getGuildById(defaultServer) == null) {
							e.getChannel().sendMessage("error: not in a server with id '" + words[1] + "'").queue();
							return;
						} else {
							PermissionsManager.setDefaultGuild(e.getAuthor().getIdLong(), defaultServer);
							e.getChannel().sendMessage("your default server for commands has been set to "
									+ IdUtils.getFormattedServer(defaultServer)).queue();
							return;
						}
					} catch (NumberFormatException ex) {
						e.getChannel().sendMessage("error: invalid server id '" + words[1] + "'").queue();
						return;
					}
				}
				
			}
		};
		this.workingSet.add(c);
		
		return this;
	}
	
	public CommandSet build() {
		return new CommandSet(this.workingSet);
	}
	
	public CommandFactory clean() {
		this.workingSet.clear();
		return this;
	}
	
	private static String formatFlagString(String s) {
		String str="";
		if(s.contains("1")) {
			str += "1: the message will not be processed into the database\r\n";
		}
		if(s.contains("2")) {
			str += "2: the user will receive a dm informing them that they used a banned phrase, as well as any action that was taken towards them\r\n";
		}
		if(s.contains("3")) {
			str += "3: the user's message will be deleted\r\n";
		}
		if(s.contains("4")) {
			str += "4: the user's permission to send messages will be revoked across all channels in the server\r\n";
		}
		if(s.contains("5")) {
			str += "5: the user will be kicked from the server\r\n";
		}
		if(s.contains("6")) {
			str += "6: the user will be banned from the server\r\n";
		}
		return str;
	}
	
	//given a guild g, obtains possibly relevant channels for megaphone output
	//priority is given to channels with name containing "cutebot"
	//and then any channels we have message_write permissions in, oldest channel first
	private static List<TextChannel> getMegaphoneChannels(Guild g) {
		//this does some inefficient looping and redundant channel adding
		//but any given guild will probably have few enough channels that it's irrelevant
		List<TextChannel> channels = new ArrayList<TextChannel>();
		List<TextChannel> guildChannels = g.getTextChannels();
		Pattern cutebotPattern = Pattern.compile("cutebot", Pattern.CASE_INSENSITIVE);
		guildChannels.stream()
			.filter(channel -> cutebotPattern.matcher(channel.getName()).find())
			.forEach(channel -> channels.add(channel));
		guildChannels.forEach(
				channel -> {
					if(PermissionUtil.checkPermission(channel, g.getSelfMember(), Permission.MESSAGE_WRITE))
						channels.add(channel);
				} );
		return channels;
	}
	
	private static boolean isSameDate(LocalDateTime first, LocalDateTime second) {
		return (first.getYear() == second.getYear()) && (first.getDayOfYear() == second.getDayOfYear());
	}
	
	//takes current datetime as parameter
	//prints its toString() representation to new qotd.txt
	//along with a randomly chosen line from twitchchat.txt, where each line is 1 quote
	//creates that new qotd.txt and returns the new quote for use
	//this should be multiple methods but idk its just for use here
	private static String generateNewQotd(LocalDateTime current) {
		StringBuilder sb = new StringBuilder();
		String quote;
		sb.append(current.toString() + "\r\n");
		List<String> quotes = new ArrayList<String>(200);
		try (LineIterator it = FileUtils.lineIterator(new File("./twitchchat.txt"), "UTF-8")) {
			while(it.hasNext()) {
				String line = it.nextLine();
				quotes.add(line);
			}
			Random r = new Random();
			quote = quotes.get(r.nextInt(quotes.size()));
		} catch (IOException | IllegalArgumentException e) {
			//exceptions shouldn't really be encountered as long as the file isn't modified outside of program
			logger.error("error trying to read twitchchat.txt for new quote of the day in CommandFactory.generateNewQotd(LocalDateTime):"
					+ e.getMessage());
			e.printStackTrace();
			quote = "i cant find the quotes so im taking a day off";
		} catch (Exception e) {
			logger.error("unknown error trying to read twitchchat.txt for new quote of the day in CommandFactory.generateNewQotd(LocalDateTime):"
					+ e.getMessage());
			e.printStackTrace();
			quote = "i cant find the quotes so im taking a day off";
		}
		
		sb.append(quote);
		try {
			FileUtils.writeStringToFile(new File("./qotd.txt"), sb.toString().trim(), "UTF-8");
		} catch (IOException e) {
			logger.error("i/o error trying to write new qotd.txt in CommandFactory.generateNewQotd(LocalDateTime):"
					+ e.getMessage());
			e.printStackTrace();
		}
		return quote;
	}
		
	
}
