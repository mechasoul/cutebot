package my.cute.discordbot.commands;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.PatternSyntaxException;
import java.util.stream.Collectors;

import my.cute.discordbot.IdUtils;
import my.cute.discordbot.bot.DatabaseManager;
import my.cute.discordbot.bot.demo;
import my.cute.discordbot.commands.Command.CommandType;
import my.cute.discordbot.preferences.FilterResponse;
import my.cute.discordbot.preferences.GuildPreferences;
import my.cute.discordbot.preferences.PreferencesManager;
import net.dv8tion.jda.core.MessageBuilder;
import net.dv8tion.jda.core.entities.Game;
import net.dv8tion.jda.core.entities.Guild;
import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent;
import net.dv8tion.jda.core.events.message.priv.PrivateMessageReceivedEvent;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CommandFactory {
	
	private static final Logger logger = LoggerFactory.getLogger(CommandFactory.class);
	private CommandSet workingSet;
	
	public CommandFactory() {
		this.workingSet = new CommandSet();
	}
	
	public CommandFactory addServerCommands() {
		//for adjusting the wordfilter
		//if user shares one server, they dont need to specify which server
		//use: !filter <mode> [<args>] [<server id>]
		//modes: add, remove, show/view, regex, clear/delete
		String name = "!filter";
		Command c = new Command(name, RequiredPermissionLevel.ADMIN, true) {
			public void response(CommandResponseParamHolder parameterObject) {
				PrivateMessageReceivedEvent e = parameterObject.privateMessageEvent;
				long targetGuild = parameterObject.targetGuild;
				String words[] = parameterObject.message.split(" ");
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
					
					String splitMsg[] = parameterObject.message.split("\"", 3);
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
					
					String splitMsg[] = parameterObject.message.split("\"", 3);
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
							+ formatFlagString(preferences.getWordFilterResponse()));
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
					
					String regex = parameterObject.message.split(" ", 3)[2];

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
		//maintains a file on disk of user ids followed by the servers they have permission over
		//can be used by admins but with restrictions (only on servers they have admin powers on)
		//part of the "let them do whatever they want and they can sort it out" philosophy
		//TODO make specifying server optional if only in 1
		//TODO should probably pm people whose permissions get changed
		name = "!admin";
		c = new Command(name, RequiredPermissionLevel.ADMIN, true) {
			public void response(CommandResponseParamHolder parameterObject) {
				PrivateMessageReceivedEvent e = parameterObject.privateMessageEvent;
				long targetGuild = parameterObject.targetGuild;
				String words[] = parameterObject.message.split(" ");
				//words: 0 = command, 1 = mode of operation, 2 = user id, 3 = server id
				
				//preliminary error checks
				//syntax check
				if(words.length != 4 && words.length != 3) {
					e.getChannel().sendMessage("error: incorrect use of !admin command").queue();
					return;
				}
				//check the user id in the server
				if(demo.getJda().getGuildById(targetGuild).getMemberById(words[2]) == null) {
					e.getChannel().sendMessage("error: no user with id " + words[2] + " exists in server " 
							+ IdUtils.getFormattedServer(targetGuild)).queue();
					return;
				}
				//now we know they've given valid args - 2 a valid member in that server


				//adding permission
				//use: !admin add <user id> <server id>
				//consider adding ability to add multiple servers at once
				if(words[1].equalsIgnoreCase("add")) {
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
					//use: !admin remove <user id> <server id>
				} else if(words[1].equalsIgnoreCase("remove")) {
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
				} else if(words[1].equalsIgnoreCase("list")) {
					//admin view case
					//use: !admin list <server id>
					
					MessageBuilder mb = new MessageBuilder();
					mb.append("admins for server " + IdUtils.getFormattedServer(targetGuild) + ": \r\n");
					for(Long userId : PreferencesManager.getGuildPreferences(targetGuild).getAdmins()) {
						mb.append(IdUtils.getFormattedUser(userId) + "\r\n");
					}
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
		//use: !pings <mode> <server>
		name = "!pings";
		c = new Command(name, RequiredPermissionLevel.ADMIN, true) {
			public void response(CommandResponseParamHolder parameterObject) {
				PrivateMessageReceivedEvent e = parameterObject.privateMessageEvent;
				long targetGuild = parameterObject.targetGuild;
				String words[] = parameterObject.message.split(" ");
				//words: 0 = command, 1 = mode of operation, 2 = server

				//error check
				if(words.length != 3 && words.length != 2) {
					e.getChannel().sendMessage("error: incorrect use of !pings command").queue();
					return;
				}


				//turn on pings
				if(words[1].equalsIgnoreCase("on")) {
					if(PreferencesManager.getGuildPreferences(targetGuild).allowPings) {
						e.getChannel().sendMessage("error: pings are already enabled for server "
								+ IdUtils.getFormattedServer(targetGuild)).queue();
						return;
					} else {
						PreferencesManager.getGuildPreferences(targetGuild).allowPings = true;
						e.getChannel().sendMessage("pings have been enabled for server " 
								+ IdUtils.getFormattedServer(targetGuild)).queue();
						return;
					}
				} else if(words[1].equalsIgnoreCase("off")) {
					//turn off pings
					if(!PreferencesManager.getGuildPreferences(targetGuild).allowPings) {
						e.getChannel().sendMessage("error: pings are already disabled for server "
								+ IdUtils.getFormattedServer(targetGuild)).queue();
						return;
					} else {
						PreferencesManager.getGuildPreferences(targetGuild).allowPings = false;
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
		name = "!autonomy";
		c = new Command(name, RequiredPermissionLevel.ADMIN, true) {
			public void response(CommandResponseParamHolder parameterObject) {
				PrivateMessageReceivedEvent e = parameterObject.privateMessageEvent;
				long targetGuild = parameterObject.targetGuild;
				String words[] = parameterObject.message.split(" ");
				//words: 0 = command, 1 = mode of operation, 3+ = parameters

				//error check
				if(words.length != 3 && words.length != 2 && words.length != 4) {
					e.getChannel().sendMessage("error: incorrect use of !autonomy command").queue();
					return;
				}
				
				if(words[1].equalsIgnoreCase("on")) {
					if(PreferencesManager.getGuildPreferences(targetGuild).autonomyEnabled) {
						e.getChannel().sendMessage("error: automatic messages are already enabled"
								+ " for server " + IdUtils.getFormattedServer(targetGuild)).queue();
						return;
					} else {
						PreferencesManager.getGuildPreferences(targetGuild).autonomyEnabled = true;
						e.getChannel().sendMessage("automatic messages have been enabled for server "
								+ IdUtils.getFormattedServer(targetGuild)).queue();
						return;
					}
				} else if(words[1].equalsIgnoreCase("off")) {
					if(!PreferencesManager.getGuildPreferences(targetGuild).autonomyEnabled) {
						e.getChannel().sendMessage("error: automatic messages are already disabled"
								+ " for server " + IdUtils.getFormattedServer(targetGuild)).queue();
						return;
					} else {
						PreferencesManager.getGuildPreferences(targetGuild).autonomyEnabled = false;
						e.getChannel().sendMessage("automatic messages have been disabled for server "
								+ IdUtils.getFormattedServer(targetGuild)).queue();
						return;
					}
				} else if(words[1].equalsIgnoreCase("set")) {
					
					try {
						long newTimer = Long.parseLong(words[2]);
						if(newTimer <= 0 || newTimer > 525600) {
							e.getChannel().sendMessage("error: invalid duration").queue();
							return;
						}
						PreferencesManager.getGuildPreferences(targetGuild).autonomyThreshold = (newTimer * 60L * 1000L);
						e.getChannel().sendMessage("automatic message timer has been updated for server " 
								+ IdUtils.getFormattedServer(targetGuild)).queue();
						return;
					} catch (NumberFormatException ex) {
						logger.info("WARNING invalid autonomy timer provided from user "
								+ IdUtils.getFormattedUser(e.getAuthor().getIdLong()) + ": " + words[2]);
						e.getChannel().sendMessage("error: invalid duration").queue();
						return;
					}
					
				} else if(words[1].equalsIgnoreCase("view")) {
					
					long timer = PreferencesManager.getGuildPreferences(targetGuild).autonomyThreshold / 60000L;
					String status = PreferencesManager.getGuildPreferences(targetGuild).autonomyEnabled ? "enabled" : "disabled";
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
		//modes should be add, remove, set to a specified list, view
		name = "!channels";
		c = new Command(name, RequiredPermissionLevel.ADMIN, true) {
			
			@Override
			public void response(CommandResponseParamHolder parameterObject) {
				
				PrivateMessageReceivedEvent e = parameterObject.privateMessageEvent;
				long targetGuild = parameterObject.targetGuild;
				String words[] = parameterObject.message.split(" ");
				//words: 0 = command, 1 = mode of operation, 2 = server

				//error check
				if(words.length != 3 && words.length != 2 && words.length != 4) {
					e.getChannel().sendMessage("error: incorrect use of !channels command").queue();
					return;
				}
				
				if(words[1].equalsIgnoreCase("add")) {
					
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
						e.getChannel().sendMessage("error: `" + words[2] + "` is not a valid channel id in server"
								+ IdUtils.getFormattedServer(targetGuild)).queue();
						return;
					}
					
				} else if(words[1].equalsIgnoreCase("remove")) {
					
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
					
					String channels[] = words[2].split(",");
					HashSet<Long> usedChannels = Arrays.asList(channels).stream()
							.filter(channelId -> IdUtils.isValidChannel(channelId, targetGuild))
							.map(Long::parseLong)
							.collect(Collectors.toCollection(HashSet::new));
					
					if(usedChannels.isEmpty()) {
						e.getChannel().sendMessage("error: no valid channel ids supplied").queue();
						return;
					}
					
					PreferencesManager.getGuildPreferences(targetGuild).setDiscussionChannels(usedChannels);
					e.getChannel().sendMessage("now processing messages from the following channels in server "
							+ IdUtils.getFormattedServer(targetGuild) + ": \r\n"
							+ usedChannels.stream()
								.map(channelId -> IdUtils.getFormattedChannel(channelId, targetGuild))
								.collect(Collectors.toList())
							).queue();
					return;
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
				
				PrivateMessageReceivedEvent e = parameterObject.privateMessageEvent;
				long targetGuild = parameterObject.targetGuild;
				String words[] = parameterObject.message.split(" ");
				
				e.getChannel().sendMessage("ok").complete();
				for(Guild g : demo.getJda().getGuilds()) {
					//TODO saving stuff goes here
				}
				//other saving stuff goes here
				demo.startShutdown();
				
			}
		};
		this.workingSet.add(c);
		
		name = "!rebuild";
		c = new Command(name, RequiredPermissionLevel.DEV, true) {
			public void response(CommandResponseParamHolder parameterObject) {
				
				//TODO add a save to file in here that future scrapes can use
				long targetGuild = parameterObject.targetGuild;
				parameterObject.privateMessageEvent.getChannel().sendMessage("beginning rebuild on server "
						+ IdUtils.getFormattedServer(targetGuild)).queue();
				DatabaseManager.rebuild(targetGuild);
				
			}
		};
		
		this.workingSet.add(c);
		
		name = "!status";
		c = new Command(name, RequiredPermissionLevel.DEV, false) {
			public void response(CommandResponseParamHolder parameterObject) {
				
				String status = parameterObject.message.substring(8);
				demo.getJda().getPresence().setGame(Game.playing(status));
				parameterObject.privateMessageEvent.getChannel().sendMessage("status set").queue();
				
			}
		};
		
		this.workingSet.add(c);
		
		name = "!statusreset";
		c = new Command(name, RequiredPermissionLevel.DEV, false) {
			public void response(CommandResponseParamHolder parameterObject) {
				demo.getJda().getPresence().setGame(null);
				parameterObject.privateMessageEvent.getChannel().sendMessage("status reset").queue();
			}
		};
		
		this.workingSet.add(c);
		
		name = "!users";
		c = new Command(name, RequiredPermissionLevel.DEV, true) {
			public void response(CommandResponseParamHolder parameterObject) {
				
			}
		};
		
		name = "!megaphone";
		c = new Command(name, RequiredPermissionLevel.DEV, true) {
			public void response(CommandResponseParamHolder parameterObject) {
				
			}
		};
		
		this.workingSet.add(c);
		
		return this;
	}
	
	public CommandFactory addGuildCommands() {
	
		String name = "!quote";
		Command c = new Command(name, RequiredPermissionLevel.USER, CommandType.GUILD, false) {
			public void response(CommandResponseParamHolder parameterObject) {
				
				GuildMessageReceivedEvent e = parameterObject.guildMessageEvent;
				
			}
		};
		this.workingSet.add(c);
		
		name = "!exit";
		c = new Command(name, RequiredPermissionLevel.USER, CommandType.GUILD, false) {
			public void response(CommandResponseParamHolder parameterObject) {
				
				//send a mothwat
				
			}
		};
		this.workingSet.add(c);
		
		return this;
	}
	
	public CommandSet build() {
		return this.workingSet;
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
	
}
