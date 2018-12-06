package my.cute.discordbot.bot;

import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import my.cute.discordbot.commands.PermissionsManager;
import my.cute.discordbot.handlers.GuildMessageHandler;
import my.cute.discordbot.handlers.PrivateMessageHandler;
import my.cute.discordbot.preferences.FindDiscussionChannelsTask;
import my.cute.discordbot.preferences.PreferencesManager;
import net.dv8tion.jda.core.entities.Guild;
import net.dv8tion.jda.core.entities.Message;
import net.dv8tion.jda.core.events.guild.GuildJoinEvent;
import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent;
import net.dv8tion.jda.core.events.message.priv.PrivateMessageReceivedEvent;
import net.dv8tion.jda.core.hooks.ListenerAdapter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MyListener extends ListenerAdapter {
	
	@SuppressWarnings("unused")
	private static final Logger logger = LoggerFactory.getLogger(MyListener.class);
	
	private long curTime, prevTime;
	private Map<Long, GuildMessageHandler> guildMessageHandlers;
	
	public MyListener() {
		
		this.guildMessageHandlers = new ConcurrentHashMap<Long, GuildMessageHandler>(demo.getGuildIds().size() + 3);
		
		curTime = System.currentTimeMillis();
		prevTime = System.currentTimeMillis();
		
		demo.getJda().getGuilds().forEach(
			guild -> guildMessageHandlers.put(guild.getIdLong(), new GuildMessageHandler(guild))
		);
	}
	
	/*
	 * TODO
	 * implement functionality to ignore pings and detect banned phrases & deal with them
	 * (see sendMessage function) //sort of done? need to make sure banned phrases excluded from db + optionally punish it
	 * 								//ok done i think?? need test like everything else
	 * add command to add/remove/set banned phrase ///done????
	 * add a strictness setting to manage how cutebot deals with banned phrases
	 * 		0 do nothing but exclude from dict, 1 delete msg, 2 delete + dm them, etc
	 * 		actually associate each number w/ an action and let user specify which actions/flags to use like 125 or 1356 or w/e
	 * 		(whats a better way to do this? something w/ long and enum)
	 * 		///kinda done? it would be practical if dif words could have dif settings, but meh
	 * add saving to filter update methods
	 * 		consider like saving prefs after they havent been adjusted for x time, but for now just do on each transaction
	 * add command to turn pings on/off //done??
	 * finish function to process entire message list at once and replace databse with it
	 * ^related, add (preferences?) used channels functionality to whether or not process msg//done
	 * add preferences to loading/saving
	 * 		actually as above save prefs on each transaction for now but yea add them to loading
	 * add admin list to load/save
	 * megaphone command
	 * help command & docs
	 * lots of musicbot shit
	 * server playlists
	 * make it work properly on recently joined servers w/o requiring restart
	 * consider changing preference saving to save a special token or something if prefs are null
	 * add automated cutebot response to prefs
	 * add channel selection to prefs - automate on first join/run, possibly allow admins to change?
	 * check for channel choice change periodically? should keep track of how many msgs we've tracked during each check if so
	 * server leave command
	 * gotta redo the whole music framework
	 * fuck
	 */
	

	
	//this is our response method most conditional stuff happens here
	//on a message, we do the following:
	//make sure its not from a bot
	//delegate to message handler for corresponding guild
	//check for save
	@Override
	public void onGuildMessageReceived(GuildMessageReceivedEvent event) {
		curTime = System.currentTimeMillis();
		
		//don't respond to bot messages
		if(event.getAuthor().isBot()) {
			return;
		}
		
		this.guildMessageHandlers.get(event.getGuild().getIdLong()).handle(event);
		
		//🇦 🇧 🇨 🇩 🇪 🇫 🇬 🇭 🇮 🇯 🇰 🇱 🇲 🇳 🇴 🇵 🇶 🇷 🇸 🇹 🇺 🇻 🇼 🇽 🇾 🇿
		
		if(curTime - prevTime >= 43200000) {//12 hours
			prevTime = curTime;
			demo.save();
		}
	}
	
	//possibly include some stuff for blocked users here
	@Override
	public void onPrivateMessageReceived(PrivateMessageReceivedEvent event) {
//		if(params[0].equalsIgnoreCase("!play") && author.getName().equals("Mecha") && author.getDiscriminator().equals("5810")) {
//			myChannel = voiceGuild.getVoiceChannelById(voiceChannel);
//			loadAndPlay(myChannel, params[1]);
//		}
//		if(params[0].equalsIgnoreCase("!loop") && author.getName().equals("Mecha") && author.getDiscriminator().equals("5810")) {
//			myChannel = voiceGuild.getVoiceChannelById(voiceChannel);
//			demo.loopOn = true;
//			loopTrack = params[1];
//			loadAndPlay(myChannel, loopTrack);
//		}
//		if(params[0].equalsIgnoreCase("!join") && author.getName().equals("Mecha") && author.getDiscriminator().equals("5810")) {
//			myChannel = voiceGuild.getVoiceChannelById(voiceChannel);
//			
//			AudioManager manager = voiceGuild.getAudioManager();
//			manager.setSendingHandler(new AudioPlayerSendHandler(player));
//			manager.openAudioConnection(myChannel);
//			demo.inVoice = true;
//		}
//		if(params[0].equalsIgnoreCase("!skip") && author.getName().equals("Mecha") && author.getDiscriminator().equals("5810")) {
//			skipTrack();
//			demo.loopOn = false;
//		}
//		
//		if(params[0].equalsIgnoreCase("!leave") && author.getName().equals("Mecha") && author.getDiscriminator().equals("5810")) {
//			emptyQueue();
//			voiceGuild.getAudioManager().closeAudioConnection();
//			demo.inVoice = false;
//			demo.playlistOn = false;
//			demo.loopOn = false;
//		}
//		if(params[0].equalsIgnoreCase("!vol") && author.getName().equals("Mecha") && author.getDiscriminator().equals("5810")) {
//			setVolume(Integer.parseInt(params[1]));
//		}
//		if(params[0].equalsIgnoreCase("!chan") && author.getName().equals("Mecha") && author.getDiscriminator().equals("5810")) {
//			voiceChannel = params[1];
//		}
//		if(params[0].equalsIgnoreCase("!local") && author.getName().equals("Mecha") && author.getDiscriminator().equals("5810")) {
//			myChannel = voiceGuild.getVoiceChannelById(voiceChannel);
//			loadAndPlay(myChannel, "./audio/" + params[1]+ ".mp3");
//		}
//		if(params[0].equalsIgnoreCase("!playlist") && author.getName().equals("Mecha") && author.getDiscriminator().equals("5810")) {
//			try {
//				
//				myChannel = voiceGuild.getVoiceChannelById(voiceChannel);
//				playlistTracks = readPlaylist("./playlists/"+params[1]+".txt");
//				nextPlaylistSong(trackScheduler);
//				demo.playlistOn = true;
//			} catch (Exception e) {
//				// TODO Auto-generated catch block
//				e.printStackTrace();
//			}
//		}
//		if(params[0].equalsIgnoreCase("!song")) {
//			if(demo.playlistOn) {
//				event.getChannel().sendMessage("currently playing: " + getFormattedTrackInfo()).queue();
//			} else {
//				event.getChannel().sendMessage("not playing").queue();
//			}
//		}
		PrivateMessageHandler.handle(event);
	}
	
	//a lot needs to happen here i think
	//add guildmessagehandler
	//add database
	//add preferences
	//add admins.ini and add server owner to it
	//possibly scrape channels to determine discussion channels and build db
	@Override
	public void onGuildJoin(GuildJoinEvent event) {
		Guild guild = event.getGuild();
		guildMessageHandlers.put(guild.getIdLong(), new GuildMessageHandler(guild));
		PreferencesManager.addServer(guild);
		PermissionsManager.addServer(guild);
		DatabaseManager.addServer(guild);
		Thread t = new Thread(new FindDiscussionChannelsTask(guild, 3, ChronoUnit.DAYS));
		t.start();
	}
	
//	public void loadAndPlay(VoiceChannel channel, String trackUrl) {
//		//assuming youtube link. if it seeks to a time, then this is length 2 and index 1 contains seconds to seek to
//		String[] timeCheck = trackUrl.split("t=");
//		AudioManager manager = voiceGuild.getAudioManager();
//		manager.setSendingHandler(new AudioPlayerSendHandler(player));
//		manager.openAudioConnection(channel);
//		
//		playerManager.loadItem(trackUrl, new MyAudioLoadHandler(trackScheduler) {
//			@Override
//			public void trackLoaded(AudioTrack track) {
//				System.out.println("queueing track");
//				//if url contained a specified start time
//				if(timeCheck.length==2) {
//					track.setPosition(Long.parseLong(timeCheck[1]) * 1000);
//				}
//				trackScheduler.queue(track);
//			}
//		});
//	}
//	
//	public void skipTrack() {
//		trackScheduler.nextTrack();
//	}
//	
//	public void emptyQueue() {
//		trackScheduler.clear();
//	}
//	
//	public void setVolume(int vol) {
//		player.setVolume(vol);
//	}
//	
//	//pass in a txt file w/ song filepatch on each line
//	//returns string array w/ all those filepaths
//	public String[] readPlaylist(String fileLocation) throws Exception {
//		ArrayList<String> lines = (ArrayList<String>)FileUtils.readLines(new File(fileLocation),utf8);
//		return lines.toArray(new String[0]);
//	}
//	
//	public void nextPlaylistSong(TrackScheduler t) {
//		String trackFilepath = playlistTracks[generalRandom.nextInt(playlistTracks.length)];
//		playerManager.loadItem(trackFilepath, defaultHandler);
//	}
//	
//	public String getFormattedTrackInfo() {
//		if(player.getPlayingTrack() == null) return "not playing";
//		return ""+player.getPlayingTrack().getInfo().author + " - " + player.getPlayingTrack().getInfo().title;
//	}
//	
//	//add voice channels for all guilds where we didnt explicitly define them
//	public void fillVoiceChannels() {
//		List<VoiceChannel> channels;
//		//iterate over all guilds
//		for(Guild g : demo.getJda().getGuilds()) {
//			//if we havent already defined a voice channel for them
//			if(!(voiceChannels.containsKey(g.getId()))) {
//				channels = g.getVoiceChannels();
//				//and voice channels exist...
//				if(!(channels.isEmpty())) {
//					//put the first channel in as default. list is nonempty by above
//					voiceChannels.put(g.getId(), channels.get(0).getId());
//					//iterate over all its voice channels
//					for(VoiceChannel v : channels) {
//						//if a voice channel contains the word cute, we set that as the default voice channel
//						if(v.getName().contains("cute")) {
//							voiceChannels.put(g.getId(), v.getId());
//							break;
//						}
//					}
//				}
//			}
//		}
//	}
	
	public void addReactionText(Message m, String s) {
		for(char c : s.toCharArray()) {
			m.addReaction(characterToUnicode(c)).queue();
		}
	}
	
	//🇦 🇧 🇨 🇩 🇪 🇫 🇬 🇭 🇮 🇯 🇰 🇱 🇲 🇳 🇴 🇵 🇶 🇷 🇸 🇹 🇺 🇻 🇼 🇽 🇾 🇿
	public String characterToUnicode(char c) {
		switch(c) {
			case 'a': return "🇦";
			case 'b': return "🇧";
			case 'c': return "🇨";
			case 'd': return "🇩"; 
			case 'e': return "🇪"; 
			case 'f': return "🇫";
			case 'g': return "🇬";
			case 'h': return "🇭";
			case 'i': return "🇮";
			case 'j': return "🇯";
			case 'k': return "🇰";
			case 'l': return "🇱";
			case 'm': return "🇲";
			case 'n': return "🇳";
			case 'o': return "🇴";
			case 'p': return "🇵";
			case 'q': return "🇶";
			case 'r': return "🇷";
			case 's': return "🇸";
			case 't': return "🇹";
			case 'u': return "🇺";
			case 'v': return "🇻";
			case 'w': return "🇼";
			case 'x': return "🇽";
			case 'y': return "🇾";
			case 'z': return "🇿";
			default: return "";
		}
	}
}

