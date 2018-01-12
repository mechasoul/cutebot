package my.cute.discordbot;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.FileHandler;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.player.DefaultAudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.player.AudioConfiguration.ResamplingQuality;
import com.sedmelluq.discord.lavaplayer.source.AudioSourceManagers;
import com.sedmelluq.discord.lavaplayer.track.AudioPlaylist;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;

import net.dv8tion.jda.core.entities.Guild;
import net.dv8tion.jda.core.entities.Member;
import net.dv8tion.jda.core.entities.User;
import net.dv8tion.jda.core.entities.VoiceChannel;
import net.dv8tion.jda.core.events.ReconnectedEvent;
import net.dv8tion.jda.core.events.guild.voice.GuildVoiceLeaveEvent;
import net.dv8tion.jda.core.events.message.priv.PrivateMessageReceivedEvent;
import net.dv8tion.jda.core.hooks.ListenerAdapter;
import net.dv8tion.jda.core.managers.AudioManager;

public class MyMusicPlayer extends ListenerAdapter {

	ConcurrentHashMap<String, Command> commands;
	HashSet<String> commandNames;
	Permission user = Permission.USER;
	Permission admin = Permission.ADMIN;
	Permission dev = Permission.DEV;
	
	HashSet<String> devDatabase;
	HashSet<String> adminDatabase;
	List<String> bannedSongs;
	
	AudioPlayerManager playerManager;
	ConcurrentHashMap<String, AudioPlayer> players;
	ConcurrentHashMap<String, TrackScheduler> trackSchedulers;
	ConcurrentHashMap<String, MyAudioLoadHandler> defaultHandlers;
	ConcurrentHashMap<String, Boolean> connectedToVoice;
	ConcurrentHashMap<String, Set<String>> permittedUsers;
	ConcurrentHashMap<String, String> connectedChannels;
	
	List<Guild> connectedServers;
	
	ConcurrentHashMap<String, Integer> songCount;
	int currentQueueSize;
	final int maxQueueSize = 20;
	final int maxSongsPerUser = 4;
	
	private final Logger logger = Logger.getLogger(MyMusicPlayer.class.getName());
    private FileHandler fh = null;
	
	public MyMusicPlayer() {
		commands = new ConcurrentHashMap<String, Command>(20);
		commandNames = new HashSet<String>(20);
		buildCommands();
		
		connectedServers = demo.getJda().getGuilds();
		
		adminDatabase = new HashSet<String>(2);
		adminDatabase.add("131225193237577729");//one of these is lalo
		adminDatabase.add("112373188746321920");//one of them is saxxy
		adminDatabase.add("101153800856825856");//shffl
		devDatabase = new HashSet<String>(2);
		devDatabase.add("115618938510901249");//me
		
		bannedSongs = new ArrayList<String>(1);
		bannedSongs.add("https://www.youtube.com/watch?v=Ew1AM8ZYDNU");
		
		playerManager = new DefaultAudioPlayerManager();
		AudioSourceManagers.registerRemoteSources(playerManager);
		AudioSourceManagers.registerLocalSource(playerManager);
		playerManager.getConfiguration().setResamplingQuality(ResamplingQuality.MEDIUM);
		playerManager.setFrameBufferDuration(20);
		players = new ConcurrentHashMap<String, AudioPlayer>(10);
		trackSchedulers = new ConcurrentHashMap<String, TrackScheduler>(10);
		defaultHandlers = new ConcurrentHashMap<String, MyAudioLoadHandler>(10);
		connectedToVoice = new ConcurrentHashMap<String, Boolean>(10);
		permittedUsers = new ConcurrentHashMap<String, Set<String>>(10);
		connectedChannels = new ConcurrentHashMap<String, String>(10);
		
		
		for(Guild g : connectedServers) {
			connectedToVoice.put(g.getId(), false);
			
			AudioPlayer p = playerManager.createPlayer();
			p.setVolume(30);
			TrackScheduler t = new TrackScheduler(p, g.getId());
			MyAudioLoadHandler d = new MyAudioLoadHandler(t);
			p.addListener(t);
			
			players.put(g.getId(), p);
			trackSchedulers.put(g.getId(), t);
			defaultHandlers.put(g.getId(), d);
		}
		
		songCount = new ConcurrentHashMap<String, Integer>(10);
		currentQueueSize = 0;
		
		SimpleDateFormat format = new SimpleDateFormat("M-d_HHmmss");
        try {
            fh = new FileHandler("./logs/logger/MyMusicPlayer"
                + format.format(Calendar.getInstance().getTime()) + ".log");
        } catch (Exception e) {
            e.printStackTrace();
        }

        fh.setFormatter(new SimpleFormatter());
        logger.addHandler(fh);
	}
	
	public void onPrivateMessageReceived(PrivateMessageReceivedEvent e) {
		if(e.getAuthor().isBot()) return;
		String message = e.getMessage().getContent();
		try {
			//first check that the message is a command
			if(isCommand(message.split(" ")[0])) {
				//then check that they have permission to use that command
				if(hasPermission(e.getAuthor().getId(), commands.get(message.split(" ")[0]).reqPermission)) {
					commands.get(message.split(" ")[0]).response(e);
				} else {
					e.getChannel().sendMessage("you don't have permission to use that command").queue();
				}
			} else {
				e.getChannel().sendMessage("??").queue();
			}
		} catch (Exception e1) {
			// TODO Auto-generated catch block
			e.getChannel().sendMessage("something went horribly wrong please call an adult").queue();
			e1.printStackTrace();
		}
	}
	
	/*
	 * TODO
	 * queue command: should show ur songs and where they are in queue
	 * 
	 */
	
	private void buildCommands() {
		String name = "!song";
		Command c = new Command(name, user) {
			public void response(PrivateMessageReceivedEvent e) {
				InfoHolder relevantInfo;
				String serverId="";
				relevantInfo = getTargetInfo(e);
				
				//uhh this should really be like a try catch with getTargetInfo throwing exceptions 
				//but i wont tell anyone if you dont
				if(relevantInfo == null) return;
				
				serverId = relevantInfo.guildId;
				
				relevantInfo = null;
				
				
				//simple stuff we really only care that theyre in a server where we're playing something
				//if we're not connected to voice or we're not playing then we got nothin
				AudioTrack currentTrack = players.get(serverId).getPlayingTrack();
				
				if(!demo.getJda().getGuildById(serverId).getAudioManager().isConnected() || currentTrack == null) {
					e.getChannel().sendMessage("not playing").queue();
					return;
				} else {
					User currentUser = (User) currentTrack.getUserData();
					e.getChannel().sendMessage("current song is " + currentTrack.getInfo().uri + " (requested by " 
							+ currentUser.getName() + "#" + currentUser.getDiscriminator() + ")").queue();
					return;
				}
			}
		};
		commands.put(name, c);
		commandNames.add(name);
		
		//command for actually adding a song to the queue
		//looks for the user in a voice channel, and if theyre allowed, queues their song
		name = "!play";
		c = new Command(name, user) {
			public void response(PrivateMessageReceivedEvent e) {
				User u = e.getAuthor();
				String message = e.getMessage().getContent();
				String[] words = message.split(" ");
				InfoHolder relevantInfo;
				String serverId="";
				VoiceChannel targetVoiceChannel=null;
				Guild targetGuild=null;
				
				relevantInfo = getTargetInfo(e);
				
				//uhh this should really be like a try catch with getTargetInfo throwing exceptions 
				//but i wont tell anyone if you dont
				if(relevantInfo == null) return;
				
				targetGuild = relevantInfo.guild;
				targetVoiceChannel = relevantInfo.voiceChannel;
				serverId = relevantInfo.guildId;
				
				relevantInfo = null;
				
				TrackScheduler trackScheduler = trackSchedulers.get(serverId);
				
				Member m = demo.getJda().getGuildById(serverId).getMember(u);
				//message should be exactly !play <link>, so 2 words, and 2nd youtube. if not, error
				if(words.length != 2) {
					e.getChannel().sendMessage("wrong use of !play command").queue();
					return;
				}
				
				if(isBannedSong(words[1])) {
					e.getChannel().sendMessage("stop queueing this song").queue();
					return;
				}
				
				if(!(isYoutubeLink(words[1]))) {
					e.getChannel().sendMessage("youtube links only (dont use shortened link either ok)").queue();
					return;
				}
				
				//ok now we have the member instance of the user in their currently connected voice guild and the youtube link they sent to play
				//our final check requires them to have been in voice when current song started playing, assuming we are currently playing
				//if this is true or if we arent currently playing we queue the song
				if(demo.getJda().getGuildById(serverId).getAudioManager().isConnected()) {
					//enter if user is in a separate voice channel from (currently active) bot
					if(!(m.getVoiceState().getChannel().getId().equals(connectedChannels.get(serverId)))) {
						e.getChannel().sendMessage("must be in the same voice channel to queue a song").queue();
						return;
					//in the same channel
					} else {
						//not playing anything so we accept whatever
						if(players.get(serverId).getPlayingTrack() == null) {
							queueSong(e, serverId);
						//playing something, so conditions apply
						} else {
							//if they're on permitted users list, accept their song
							if(trackScheduler.playingDefaultTrack || trackScheduler.getPermittedUsers().contains(u)) {
								queueSong(e, serverId);
							//otherwise dont
							} else {
								e.getChannel().sendMessage("wait until end of current song to queue (you joined recently)").queue();
								return;
							}
						}
					}
				} else if(demo.getJda().getGuildById(serverId).getAudioManager().isAttemptingToConnect()) {
					//connecting
					e.getChannel().sendMessage("im trying to connect hold ON").queue();
					return;
				} else {
					//bot not connected to voice. connect to target channel and queue the song
					connectToVoice(targetGuild, targetVoiceChannel);
					//force-update permitted users to prevent concurrency issues where we arent connected to voicechannel
					//when we make the check for the active channel in UserListManager
					trackScheduler.explicitlyUpdatePermittedUsers(targetVoiceChannel);
					queueSong(e, serverId);
				}
			}
		};
		commands.put(name, c);
		commandNames.add(name);
		
		
		name = "!skip";
		c = new Command(name, user) {
			public void response(PrivateMessageReceivedEvent e) {
				User u = e.getAuthor();
				InfoHolder relevantInfo;
				String serverId="";
				relevantInfo = getTargetInfo(e);
				
				//uhh this should really be like a try catch with getTargetInfo throwing exceptions 
				//but i wont tell anyone if you dont
				if(relevantInfo == null) return;
				
				serverId = relevantInfo.guildId;
				
				relevantInfo = null;
				
				TrackScheduler trackScheduler = trackSchedulers.get(serverId);
				
				Member m = demo.getJda().getGuildById(serverId).getMember(u);
				AudioManager manager = demo.getJda().getGuildById(serverId).getAudioManager();
				
				if(manager.isConnected()) {
					//enter if user is in a separate voice channel from (currently active) bot
					if(!(m.getVoiceState().getChannel().getId().equals(manager.getConnectedChannel().getId()))) {
						e.getChannel().sendMessage("must be in the same voice channel to skip").queue();
						return;
					//in the same channel
					} else {
						//if they're on permitted users list, accept their vote skip
						if(trackScheduler.getPermittedUsers().contains(u)) {
							//adds user to the set of users who voted to skip
							//returns true if the user was not previously in the set. false if they were
							if(trackScheduler.addSkippingUser(u)) {
								e.getChannel().sendMessage("vote to skip the current song accepted (" 
										+ (trackScheduler.requiredVotesToSkip() - trackScheduler.currentSkippingSize()) + " more votes required to skip)").queue();
							} else {
								e.getChannel().sendMessage("you already voted to skip (" 
										+ (trackScheduler.requiredVotesToSkip() - trackScheduler.currentSkippingSize()) + " more votes required to skip)").queue();
								return;
							}
							
							//skip
							if((trackScheduler.requiredVotesToSkip() - trackScheduler.currentSkippingSize()) <= 0) {
								trackScheduler.skipTrack();
							}
							
						//otherwise dont
						} else {
							e.getChannel().sendMessage("you arent allowed to skip this song (you joined recently)").queue();
							return;
						}
					}
				} else if(manager.isAttemptingToConnect()) {
					e.getChannel().sendMessage("idk wahts going on lol").queue();
					return;
				} else {
					e.getChannel().sendMessage("im not even playing anything ??").queue();
					return;
				}
			}
		};
		commands.put(name, c);
		commandNames.add(name);
		
		
		name = "!remove";
		c = new Command(name, user) {
			public void response(PrivateMessageReceivedEvent e) {
				User u = e.getAuthor();
				String message = e.getMessage().getContent();
				String[] words = message.split(" ");
				InfoHolder relevantInfo;
				String serverId="";
				relevantInfo = getTargetInfo(e);
				
				//uhh this should really be like a try catch with getTargetInfo throwing exceptions 
				//but i wont tell anyone if you dont
				if(relevantInfo == null) return;
				
				serverId = relevantInfo.guildId;
				
				relevantInfo = null;
				
				TrackScheduler trackScheduler = trackSchedulers.get(serverId);
				
				//message should be exactly !play <link>, so 2 words, and 2nd youtube. if not, error
				if(words.length != 2) {
					e.getChannel().sendMessage("wrong use of !remove command").queue();
					return;
				}
				
				if(!(isYoutubeLink(words[1]))) {
					e.getChannel().sendMessage("youtube links only (dont use shortened link either ok)").queue();
					return;
				}
				
				//no special restrictions on removing songs, users can do it whenever
				if(demo.getJda().getGuildById(serverId).getAudioManager().isConnected()) {
					//check for the url in queue first, then radio
					AudioTrack givenTrack = trackScheduler.checkForGivenTrack(words[1]);
					if(givenTrack != null) {
						//check it was their song
						if(u.equals( (User) givenTrack.getUserData())) {
							//get it out
							trackScheduler.removeTrackFromQueue(givenTrack);
							e.getChannel().sendMessage("song removed from queue").queue();
							return;
						} else {
							e.getChannel().sendMessage("song submitted by someone other than you (not allowed)").queue();
							return;
						}
					//check for url in radio
					} else {
						givenTrack = trackScheduler.checkRadioForGivenTrack(words[1]);
						if(givenTrack != null) {
							//check it was their song
							if(u.equals( (User) givenTrack.getUserData())) {
								//get it out
								trackScheduler.removeTrackFromRadio(givenTrack);
								e.getChannel().sendMessage("song removed from radio").queue();
								return;
							} else {
								e.getChannel().sendMessage("song submitted by someone other than you (not allowed)").queue();
								return;
							}
						}
						
						e.getChannel().sendMessage("song not found in queue (make sure the url is the same)").queue();
						return;
					}
				} else {
					//bot not connected to voice
					e.getChannel().sendMessage("im not even playing anything ??").queue();
					return;
				}
			}
		};
		commands.put(name, c);
		commandNames.add(name);
		
		
		name = "!queue";
		c = new Command(name, user) {
			public void response(PrivateMessageReceivedEvent e) {
				User u = e.getAuthor();
				InfoHolder relevantInfo;
				String serverId="";
				relevantInfo = getTargetInfo(e);
				
				//uhh this should really be like a try catch with getTargetInfo throwing exceptions 
				//but i wont tell anyone if you dont
				if(relevantInfo == null) return;
				
				serverId = relevantInfo.guildId;
				
				relevantInfo = null;
				
				TrackScheduler trackScheduler = trackSchedulers.get(serverId);
				
				logger.info("queue check: \n" + trackScheduler.getQueue());
				
				ListIterator<AudioTrack> userSongs = trackScheduler.getUserSongs(u.getId()).listIterator(0);
				
				if(demo.getJda().getGuildById(serverId).getAudioManager().isConnected()) {
					e.getChannel().sendMessage("queue currently contains " + trackScheduler.currentQueueSize() + " songs. you have " 
							+ userSongs.size() + " song(s) in queue").queue();
					if(userSongs.size() > 0) {
						e.getChannel().sendMessage("your queued song(s):").queue();
						while(userSongs.hasNext()) {
							AudioTrack t = userSongs.next();
							e.getChannel().sendMessage("position " + (Integer) t.getUserData() + ": " 
									+ t.getInfo().title + " (" + t.getInfo().uri + ")").queue();
						}
					}
					return;
				} else {
					e.getChannel().sendMessage("no").queue();
					return;
				}
			}
		};
		commands.put(name, c);
		commandNames.add(name);
		
		//TODO
//		name = "!forceplay";
//		c = new Command(name, admin) {
//			public void response(PrivateMessageReceivedEvent e) {
//				User u = e.getAuthor();
//				String message = e.getMessage().getContent();
//				String[] words = message.split(" ");
//				InfoHolder relevantInfo;
//				String serverId="";
//				VoiceChannel targetVoiceChannel=null;
//				Guild targetGuild=null;
//				
//				relevantInfo = getTargetInfo(e);
//				
//				//uhh this should really be like a try catch with getTargetInfo throwing exceptions 
//				//but i wont tell anyone if you dont
//				if(relevantInfo == null) return;
//				
//				targetGuild = relevantInfo.guild;
//				targetVoiceChannel = relevantInfo.voiceChannel;
//				serverId = relevantInfo.guildId;
//				
//				relevantInfo = null;
//				
//				TrackScheduler trackScheduler = trackSchedulers.get(serverId);
//				
//				Member m = demo.getJda().getGuildById(serverId).getMember(u);
//				//message should be exactly !play <link>, so 2 words, and 2nd youtube. if not, error
//				if(words.length != 2) {
//					e.getChannel().sendMessage("wrong use of !play command").queue();
//					return;
//				}
//				
//				if(!(isYoutubeLink(words[1]))) {
//					e.getChannel().sendMessage("youtube links only (dont use shortened link either ok)").queue();
//					return;
//				}
//				
//				//ok now we have the member instance of the user in their currently connected voice guild and the youtube link they sent to play
//				//our final check requires them to have been in voice when current song started playing, assuming we are currently playing
//				//if this is true or if we arent currently playing we queue the song
//				if(demo.getJda().getGuildById(serverId).getAudioManager().isConnected()) {
//					
//					//force play
//					
//				} else if(demo.getJda().getGuildById(serverId).getAudioManager().isAttemptingToConnect()) {
//					//connecting
//					e.getChannel().sendMessage("im trying to connect hold ON").queue();
//					return;
//				} else {
//					//bot not connected to voice. connect to target channel and queue the song
//					connectToVoice(targetGuild, targetVoiceChannel);
//					//force-update permitted users to prevent concurrency issues where we arent connected to voicechannel
//					//when we make the check for the active channel in UserListManager
//					trackScheduler.explicitlyUpdatePermittedUsers(targetVoiceChannel);
//					queueSong(e, serverId);
//				}
//			}
//		};
//		commands.put(name, c);
//		commandNames.add(name);
		
		name = "!default";
		c = new Command(name, admin) {
			public void response(PrivateMessageReceivedEvent e) {
				InfoHolder relevantInfo;
				String serverId="";
				relevantInfo = getTargetInfo(e);
				String[] words = e.getMessage().getContent().split(" ");
				
				//uhh this should really be like a try catch with getTargetInfo throwing exceptions 
				//but i wont tell anyone if you dont
				if(relevantInfo == null) return;
				
				serverId = relevantInfo.guildId;
				
				relevantInfo = null;
				
				TrackScheduler trackScheduler = trackSchedulers.get(serverId);
				
				if(words.length != 2) {
					e.getChannel().sendMessage("wrong use of !default command").queue();
					return;
				}
				
				//special case if we !default null - disable default play
				if(words[1].equals("off")) {
					trackScheduler.setDefaultTrack(null);
					trackScheduler.playingRadio = false;
					e.getChannel().sendMessage("default track disabled").queue();
					return;
				//2nd special case, turn on radio play
				} else if(words[1].equals("radio")) {
					trackScheduler.setDefaultTrack(null);
					trackScheduler.playingRadio = true;
					e.getChannel().sendMessage("radio play enabled").queue();
					return;
				//assume it's a link otherwise
				} else {
					loadDefaultTrack(serverId, words[1], e);
				}
			}
		};
		commands.put(name, c);
		commandNames.add(name);
		
		name = "!exit";
		c = new Command(name, dev) {
			public void response(PrivateMessageReceivedEvent e) {
				e.getChannel().sendMessage("ok").complete();
				demo.startShutdown();
			}
		};
		commands.put(name, c);
		commandNames.add(name);
	}
	
	private void queueSong(PrivateMessageReceivedEvent e, String serverId) {
		TrackScheduler trackScheduler = trackSchedulers.get(serverId);
		String userId = e.getAuthor().getId();
		String songUrl = e.getMessage().getContent().split(" ")[1];
		//first check for full queue
		if(trackScheduler.currentQueueSize() == this.maxQueueSize) {
			e.getChannel().sendMessage("queue is currently full try again later").queue();
			return;
		//check if song is in queue already
		} else if(trackScheduler.queueContainsUrl(songUrl)) {
			e.getChannel().sendMessage("song already in queue").queue();
			return;
		} else {
			//queue not full so we add our song
			//first check if theyre at max songs already
			int userSongCount = trackScheduler.getUserSongCount(userId);
			if(userSongCount >= this.maxSongsPerUser) {
				e.getChannel().sendMessage("you're already at the max number of songs (" + this.maxSongsPerUser + " per user), try again later").queue();
				return;
			} else {
				//add song
				
				loadItemsIntoQueue(songUrl, e, serverId);
					
			}
		}	
	}
	
	private void loadItemsIntoQueue(String songUrl, PrivateMessageReceivedEvent e, String serverId) {
		//track queueing logic
		//add user and url to trackscheduler's queue of respective data for retrieval later
		//call playermanager's loaditem to convert the url into AudioTrack
		//audio load handler's trackLoaded(AudioTrack) will fire after and that calls
		//TrackScheduler.queue(AudioTrack)
		//this.trackSchedulers.get(serverId).addToUrlQueue(songUrl);
		//this.trackSchedulers.get(serverId).addToUserQueue(u);
		
		//this.playerManager.loadItem(songUrl, this.defaultHandlers.get(serverId));
		this.playerManager.loadItemOrdered(serverId, songUrl, new MyAudioLoadHandler(trackSchedulers.get(serverId)) {
			@Override
			public void trackLoaded(AudioTrack track) {
				track.setUserData(e.getAuthor());
				logger.info("loaded track " + track.getInfo().title + " (" + track.getInfo().uri + ") from server " + serverId 
						+ "\n in channel " + connectedChannels.get(serverId) + ", from user " + e.getAuthor().getName() + "#" + e.getAuthor().getDiscriminator()
						+ " in channel " + demo.getJda().getGuildById(serverId).getMember(e.getAuthor()).getVoiceState().getChannel());
				
				//10mins
				if(track.getDuration() >= 600000 && !isDev(e.getAuthor())) {
					e.getChannel().sendMessage("https://www.youtube.com/watch?v=Z6_ZNW1DACE").queue();
					logger.info("track was too long!");
				} else {
					e.getChannel().sendMessage("song added to queue").queue();
					trackScheduler.queue(track);
				}
				
			}
			
			@Override
			public void playlistLoaded(AudioPlaylist playlist) {
				AudioTrack selectedTrack = playlist.getSelectedTrack();
				trackLoaded(selectedTrack);
			}
		});
	}
	
	private void loadDefaultTrack(String serverId, String songUrl, PrivateMessageReceivedEvent e) {
		this.playerManager.loadItemOrdered(serverId, songUrl, new MyAudioLoadHandler(trackSchedulers.get(serverId)) {
			@Override
			public void trackLoaded(AudioTrack track) {
				//set userdata to cutebot
				track.setUserData(demo.getJda().getUserById("312807432839626753"));
				logger.info("loaded DEFAULT track " + track.getInfo().title + " (" + track.getInfo().uri + ")");
				trackScheduler.setDefaultTrack(track);
				e.getChannel().sendMessage("default track set").queue();
			}
		});
	}
	
	private void connectToVoice(Guild g, VoiceChannel v) {
		AudioManager a = g.getAudioManager();
		if (!a.isConnected() && !a.isAttemptingToConnect()) {
			if(a.getSendingHandler()==null) a.setSendingHandler(new AudioPlayerSendHandler(this.players.get(g.getId())));
			a.openAudioConnection(v);
		}
		this.connectedToVoice.put(g.getId(), true);
		this.connectedChannels.put(g.getId(),  v.getId());
	}
	
	private boolean isBannedSong(String s) {
		return bannedSongs.contains(s);
	}
	
	public void onGuildVoiceLeave(GuildVoiceLeaveEvent event) {
		this.connectedToVoice.put(event.getGuild().getId(), false);
	}
	
	//reconnecting to voice channel wasnt working properly maybe this helps?
	public void onReconnect(ReconnectedEvent e) {
		for(Guild g : demo.getJda().getGuilds()) {
			if(g.getAudioManager().isConnected()) connectToVoice(g, g.getAudioManager().getConnectedChannel());
		}
	}
	
	//cooked up method to perform stuff that we have to do at the start of basically every command
	private InfoHolder getTargetInfo(PrivateMessageReceivedEvent e) {
		User u = e.getAuthor();
		String serverId="";
		List<Guild> sharedGuilds = e.getAuthor().getMutualGuilds();
		//need user to be in a shared server
		if(sharedGuilds.isEmpty()) {
			e.getChannel().sendMessage("you arent in a server w/ me ??").queue();
			return null;
		}
		//check to see if user is in voice 
		for(Guild g : sharedGuilds) {
			if(g.getMember(u).getVoiceState().inVoiceChannel()) {
				return new InfoHolder(g, g.getMember(u).getVoiceState().getChannel(), g.getId());
			}
		}
		
		//if user wasnt in voice channel in any of the shared guilds we end up in this
		if(serverId.isEmpty()) {
			e.getChannel().sendMessage("you need to be in a voice channel to use that command").queue();
			return null;
		}
		
		return null;
	}
	
	private boolean isCommand(String s) {
		return commandNames.contains(s);
	}
	
	private boolean isYoutubeLink(String s) {
		return s.contains(".youtube.com/watch?v=");
	}
	
	private boolean isDev(User u) {
		return this.devDatabase.contains(u.getId());
	}
	
	private boolean hasPermission(String userId, Permission permission) {
		if(permission == Permission.ADMIN) {
			return (this.adminDatabase.contains(userId) || this.devDatabase.contains(userId));
		} else if(permission == Permission.DEV) {
			return this.devDatabase.contains(userId);
		} else {
			return true;
		}
	}
	
	private class InfoHolder {
		public final Guild guild;
		public final VoiceChannel voiceChannel;
		public final String guildId;
		
		public InfoHolder(Guild g, VoiceChannel v, String s) {
			this.guild = g;
			this.voiceChannel = v;
			this.guildId = s;
		}
	}
	
}
