package my.cute.discordbot.bot;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import my.cute.discordbot.commands.Command;
import my.cute.discordbot.commands.CommandFactory;
import my.cute.discordbot.commands.RequiredPermissionLevel;
import my.cute.discordbot.handlers.AutonomyHandler;
import my.cute.discordbot.handlers.GuildMessageHandler;
import my.cute.discordbot.preferences.GuildPreferences;
import my.cute.markov.InputHandler;
import my.cute.markov.MarkovDatabase;
import my.cute.markov.OutputHandler;
import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.MessageBuilder;
import net.dv8tion.jda.core.entities.Emote;
import net.dv8tion.jda.core.entities.Game;
import net.dv8tion.jda.core.entities.Guild;
import net.dv8tion.jda.core.entities.Member;
import net.dv8tion.jda.core.entities.Message;
import net.dv8tion.jda.core.entities.MessageChannel;
import net.dv8tion.jda.core.entities.MessageEmbed;
import net.dv8tion.jda.core.entities.MessageHistory;
import net.dv8tion.jda.core.entities.PermissionOverride;
import net.dv8tion.jda.core.entities.PrivateChannel;
import net.dv8tion.jda.core.entities.TextChannel;
import net.dv8tion.jda.core.entities.User;
import net.dv8tion.jda.core.events.guild.GuildJoinEvent;
import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent;
import net.dv8tion.jda.core.events.message.priv.PrivateMessageReceivedEvent;
import net.dv8tion.jda.core.exceptions.HierarchyException;
import net.dv8tion.jda.core.exceptions.InsufficientPermissionException;
import net.dv8tion.jda.core.hooks.ListenerAdapter;
import net.dv8tion.jda.core.utils.PermissionUtil;
import net.dv8tion.jda.core.utils.tuple.MutablePair;
import net.dv8tion.jda.core.utils.tuple.Pair;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.LineIterator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MyListener extends ListenerAdapter {
	
	public static class PermissionSet {
		
	}
	
	private final Logger logger = LoggerFactory.getLogger(MyListener.class);
	
	public Map<String, Integer> emoteCount;
	public Map<String, String> adminDatabase;
//	public Map<String, String> voiceChannels;
	List<Guild> connectedServers;
	public long curTime, prevTime;
	private Map<Long, GuildMessageHandler> guildMessageHandlers;
	private boolean messageSentFlag;
	public boolean timeToSave, timeToExit;
	Charset utf8;
	Calendar cal;
	
	boolean enableApocalypse;
	
	int p;
	Random generalRandom;
	
	ArrayList<String> quotes;
	String[] quoteArray;
	
//	AudioPlayerManager playerManager;
//	AudioPlayer player;
//	TrackScheduler trackScheduler;
//	Guild voiceGuild;
//	String voiceChannel;
//	MyAudioLoadHandler defaultHandler;
//	
//	String[] playlistTracks;
//	String currentTrackInfo;
//	String loopTrack;
	
	Map<String, GuildPreferences> preferences;
	Map<String, HashSet<String>> permissions;
	
	Map<String, InputHandler> markovInput;
	Map<String, MarkovDatabase> markovDbs;
	Map<String, OutputHandler> markovOutput;
	
	//TODO fix this generic thrown exception
	//TODO maybe preferences and permissions and shit should be in their own class?
	public MyListener() throws Exception {
		
		emoteCount = new ConcurrentHashMap<String, Integer>(50,0.8f,4);
		adminDatabase = new ConcurrentHashMap<String, String>(10,0.8f,4);
		
		//me
		adminDatabase.put("115618938510901249", demo.ACTIVE_SERVER);
		//lalo
		adminDatabase.put("131225193237577729", "124360183223681025");
		
//		voiceChannels = new ConcurrentHashMap<String, String>(10,0.8f,4);
//		//explicitly add some voice channels for some guilds
//		voiceChannels.put("101153748377686016", "318143459561177090");
//		voiceChannels.put("115619022304706565", "115619022304706566");
//		fillVoiceChannels();

		enableApocalypse = false;
		
		curTime = System.currentTimeMillis();
		prevTime = System.currentTimeMillis();
//		autonomyHandlers = new ConcurrentHashMap<String, AutonomyHandler>(4, 0.8f, 4);
//		//oznet
//		autonomyHandlers.put("101153748377686016", new AutonomyHandler(3600000, "101153748377686016"));
//		//ll
//		autonomyHandlers.put("133082652004712449", new AutonomyHandler(21600000, "133082652004712449"));
//		//some lalo server
//		autonomyHandlers.put("401439342536556544", new AutonomyHandler(7200000, "401439342536556544"));
//		//other lalo server
//		autonomyHandlers.put("260178869158543360", new AutonomyHandler(3600000, "260178869158543360"));
//		//saxxy server
//		autonomyHandlers.put("162556290592145408", new AutonomyHandler(7200000, "162556290592145408"));
//		//lalo friend server
//		autonomyHandlers.put("309816855219142666", new AutonomyHandler(7200000, "309816855219142666"));
		
		messageSentFlag = false;
		timeToSave = false;
		timeToExit = false;
		utf8 = StandardCharsets.UTF_8;
		p = 104711;
		generalRandom = new Random();
		
		quotes = (ArrayList<String>)FileUtils.readLines(new File("twitchchat.txt"),utf8);
		quoteArray = quotes.toArray(new String[0]);
		
//		playerManager = new DefaultAudioPlayerManager();
//		AudioSourceManagers.registerRemoteSources(playerManager);
//		AudioSourceManagers.registerLocalSource(playerManager);
//		playerManager.getConfiguration().setResamplingQuality(ResamplingQuality.HIGH);
//		player = playerManager.createPlayer();
//		trackScheduler = new TrackScheduler(player, this);
//		defaultHandler = new MyAudioLoadHandler(trackScheduler);
//		player.addListener(trackScheduler);
//		voiceGuild = demo.getJda().getGuildById(demo.ACTIVE_SERVER);
//		voiceChannel = "245917989256298496";
//		defaultHandler = new MyAudioLoadHandler(trackScheduler);
//		currentTrackInfo = "not playing";
//		loopTrack = "";
		
		connectedServers = demo.getJda().getGuilds();
		
		markovDbs = new ConcurrentHashMap<String, MarkovDatabase>(10, 0.8f, 4);
		markovInput = new ConcurrentHashMap<String, InputHandler>(10, 0.8f, 4);
		markovOutput = new ConcurrentHashMap<String, OutputHandler>(10, 0.8f, 4);
		
		preferences = new ConcurrentHashMap<String, GuildPreferences>(10, 0.8f, 4);
		permissions = new ConcurrentHashMap<String, HashSet<String>>(10,0.8f,4);
		//construct permissions map from file
		//honestly i'm unsure about always keeping this file in memory since really it shouldnt be used THAT often
		//but it should be relatively small and its kind of a pain to load/save everything from/to file every time
		try (LineIterator permIterator = FileUtils.lineIterator(new File("./markovdb/permissions.csv"), "UTF-8")) {
			//each line has format: <user>,<server>,<server>,...,<server>
			//where <user> is the user id of that admin and each <server> is a different server they have permissions for
			//each line at minimum is one user and one server
			//TODO reverse this and put it in guildmessagehandler
			while(permIterator.hasNext()) {
				String words[] = permIterator.nextLine().split(",");
				HashSet<String> servers = new HashSet<String>(words.length);
				//indices 1 to n-1 are server ids, so add them to the hashset
				for(int i=1; i < words.length; i++) {
					servers.add(words[i]);
				}
				//index 0 is the user id, so use that as the key for our map
				permissions.put(words[0], servers);
			}
		} 
		
		
		
		System.out.print("connected to: ");
		
		for(Guild g : connectedServers) {
			System.out.print(g.getName() + "-" + g.getId() + ", ");
		}
		System.out.println();
		
		for(Guild g : connectedServers) {
			long id = g.getIdLong();
			//this.guildMessageHandlers.put(id, new GuildMessageHandler(g));
			
//			preferences.put(id, new Preferences(id));
//			File prefFile = new File("./markovdb/" + id + "/preferences.ini");
//			if(prefFile.isFile()) {
//				LineIterator it = FileUtils.lineIterator(prefFile, "UTF-8");
//				try {
//					while(it.hasNext()) {
//						String line = it.nextLine();
//						String tokens[] = line.split("=",2);
//						String option = tokens[0];
//						String value = tokens[1];
//						//pings option
//						//format: pings=true|false
//						//controls whether bot will output messages containing pings
//						if(option.equals("pings")) {
//							preferences.get(id).allowPings=Boolean.getBoolean(value);
//						//wordfilter option
//						//format is one of the following two:
//						//bannedphrase=<wordlist> where <wordlist> is a comma-separated list of banned words
//						//bannedphrase=<[_REGEX]> which indicates that the Pattern has been explicitly set
//						} else if(option.equals("bannedphrase")) {
//							preferences.get(id).bannedPhrase = value;
//						//extension of the wordfilter option
//						//retrieve the regex used for the pattern and compile it
//						//format:
//						//compiledfilter=<regex> where regex is the string used to compile the pattern for the server
//						} else if(option.equals("compiledfilter")) {
//							preferences.get(id).compiledFilter = Pattern.compile(value);
//						}
//					}
//				} finally {
//					it.close();
//				}
//			}
		}
		
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
	 */
	
	/*
	 * use following logic for command permissions
	 * if reqPermission is user: allow command
	 * if reqPermission is admin: determine target guild, check if they have perms for that guild
	 * if reqPermission is dev: check dev database
	 * 
	 * -> admin commands need a way to retrieve target guild from given message event
	 */

	
	//this is our response method most conditional stuff happens here
	//on a message, we do the following:
	//make sure its not from a bot
	//create autonomous msg if relevant
	//moderate msg if uses banned phrase
	//process into db if not banned phrase & in relevant channel & not from banned user
	//message response
	//quote & other specific server command checks
	//check for exit, save
	@Override
	public void onGuildMessageReceived(GuildMessageReceivedEvent event) {
		curTime = System.currentTimeMillis();
		cal = Calendar.getInstance();
		
		Message message = event.getMessage();
		String content = message.getContentDisplay().toLowerCase();
		TextChannel channel = event.getChannel();
		User author = event.getAuthor();
		Guild guild = event.getGuild();
		MessageBuilder m;
		
		//don't respond to bot messages, unless we want to trigger the apocalypse
		if(event.getAuthor().isBot()) {
			return;
		}
		
//		//if we're in a server w/ autonomous message enabled
//		if(autonomyHandlers.containsKey(guild.getId())) {
//			//process the message, and if it returns true it's time to send a message
//			if(autonomyHandlers.get(guild.getId()).handleMessage()) {
//				//consider putting this code into a function since im now reusing it in a couple places
//				try {
//					sendMessageToChannel(guild, channel);
//					messageSentFlag = true;
//					System.out.println("autonomous message in server " + formatGuildId(guild.getId()));
//				} catch (Exception e) {
//					// TODO Auto-generated catch block
//					System.out.println("exception thrown in autonomous message gen in server " + formatGuildId(guild.getId()));
//					e.printStackTrace();
//				}
//			}
//		}
//		
//		//general sht
//		
//		//message processing
//		markovInput.get(guild.getId()).processLine(message.getContentRaw());
//		
//		if(!messageSentFlag && content.toLowerCase().contains("cutebot") && isQuestion(content.toLowerCase())) {
//			sendMessageToChannel(guild, channel);
//			//stupid ass pingpong command
//		} else if(content.toLowerCase().contains("cutebot") || content.toLowerCase().contains(":mothyes:")) {
//			Random r = new Random();
//			int randomNumber = r.nextInt(10);
//			m = new MessageBuilder();
//			//low chance
//			if(randomNumber==1) {
//				//if emote doesnt exist, send url to image
//				if(guild.getEmotesByName("mothuhh", true).isEmpty()) {
//					List<Emote> emotes = guild.getEmotes();
//					if(emotes.isEmpty()) {
//						channel.sendMessage("http://i.imgur.com/zVnJhzy.png").queue();;
//					} else {
//						message.addReaction(emotes.get(r.nextInt(emotes.size()))).queue();
//					}
//				} else {
//					//else send image
//					//this line sends as reaction
//					message.addReaction(guild.getEmotesByName("mothuhh", true).get(0)).queue();
//					//these lines send as its own message
//					//m.append(guild.getEmotesByName("mothuhh", true).get(0));
//					//channel.sendMessage(m.build()).queue();
//				}
//				//high chance
//			} else {
//				//make sure emote exists. if not, send url to image
//				if(guild.getEmotesByName("mothyes", true).isEmpty()) {
//					channel.sendMessage("http://i.imgur.com/20wmYfp.png").queue();
//				} else {
//					message.addReaction(guild.getEmotesByName("mothyes", true).get(0)).queue();
//					//m.append(guild.getEmotesByName("mothyes", true).get(0));
//					//channel.sendMessage(m.build()).queue();
//				}
//			}
//		}
		
		this.guildMessageHandlers.get(event.getGuild().getIdLong()).handle(event);
		
		//TODO do we put all these commands into an actual Command object shell?

		if(content.equals("!quote")) {
			sendQuote(channel);
		} 
//		//f id
//		//🇦 🇧 🇨 🇩 🇪 🇫 🇬 🇭 🇮 🇯 🇰 🇱 🇲 🇳 🇴 🇵 🇶 🇷 🇸 🇹 🇺 🇻 🇼 🇽 🇾 🇿
//		if(guild.getId().equals("115619022304706565")) {
//			System.out.println(author.getName());
//		}
		
		if(curTime - prevTime >= 43200000) {//12 hours
			timeToSave = true;
		}
		if(content.equalsIgnoreCase("!exit") && isDeveloper(author)) {
			timeToSave = true;
			timeToExit = true;
			channel.sendMessage("ok").complete();
		}
		if(timeToSave) {
			//TODO save stuff goes here
			//this shit is old
			for(Guild g : demo.getJda().getGuilds()) {
				try {
					this.guildMessageHandlers.get(g.getIdLong()).getDatabase().saveDatabase();
				} catch (Exception e) {
					// TODO Auto-generated catch block
					logger.info("exception: " + e.getMessage() + "\r\nencountered in database save for server " + g.getId() + "-" + g.getName());
					e.printStackTrace();
				}
			}
			prevTime = curTime;
			timeToSave = false;
		}
		if(timeToExit) {
			demo.startShutdown();
		}
	}
	
	
	@Override
	public void onPrivateMessageReceived(PrivateMessageReceivedEvent event) {
		//Guild oznet = demo.getJda().getGuildById("101153748377686016");
		//Guild f = demo.getJda().getGuildById("115619022304706565");
		
		Message message = event.getMessage();
		String content = message.getContentDisplay();
		User author = event.getAuthor();
		String[] params = content.split(" ");//ex !command param1 param2
		
		
		//do nothing if user doesn't have admin rights
		//if(adminDatabase.get(humanName) == null) return;
		//otherwise user has admin rights, so set the server for their command to the one they have admin powers over
		//todo account for a single user to send commands to multiple servers - probably overload the commands
		//String relevantServer = adminDatabase.get(humanName);
		
		
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
		if(params[0].equalsIgnoreCase("!status") && author.getName().equals("Mecha") && author.getDiscriminator().equals("5810")) {
			demo.getJda().getPresence().setGame(Game.of(Game.GameType.DEFAULT, content.substring(content.indexOf(' ') + 1)));
		}
		if(params[0].equalsIgnoreCase("!statusreset") && author.getName().equals("Mecha") && author.getDiscriminator().equals("5810")) {
			demo.getJda().getPresence().setGame(null);
		}
		
		if(params[0].equalsIgnoreCase("!users") && author.getName().equals("Mecha") && author.getDiscriminator().equals("5810")) {
			try {
				List<Member> members = demo.getJda().getGuildById(params[1]).getMembers();
				User u;
				String s="";
				int count=0;
				for(Member m : members) {
					u = m.getUser();
					s+=u.getName() + "#" + u.getDiscriminator() + "\n";
					count++;
					if(count % 50 == 0) {
						event.getChannel().sendMessage(s).queue();
						s = "";
					}
				}
				
				if(count % 50 != 0) event.getChannel().sendMessage(s).queue();
			} catch (Exception e) {
				// TODO Auto-generated catch block
				event.getChannel().sendMessage("something went wrong in users poll").queue();
				e.printStackTrace();
			}
			
		}
		
		if(params[0].equalsIgnoreCase("!exit") && isDeveloper(author)) {
			event.getChannel().sendMessage("ok").complete();
			for(Guild g : connectedServers) {
				markovDbs.get(g.getId()).saveDatabase();
			}
			demo.startShutdown();
		}
		
		if(params[0].equalsIgnoreCase("!scrape") && isDeveloper(author)) {
			String targetId = "391533825642987520";
			event.getChannel().sendMessage("starting " + targetId + " scrape").complete();
			Guild targetGuild = demo.getJda().getGuildById(targetId);
			//convert all times to minutes
			double currentTime = ((double)System.currentTimeMillis()) / 1000 / 60;
			double targetGuildCreationTime = (double)targetGuild.getCreationTime().toEpochSecond() / 60;
			Map<String, Pair<Double, List<Message>>> allMessages = new ConcurrentHashMap<String, Pair<Double, List<Message>>>(10, 0.8f, 4);
			boolean success = true;
			double totalMessages=0;
			for(TextChannel c : targetGuild.getTextChannels()) {
				try {
					List<Message> channelMessages = getMessagesInChannel(c);
					double channelCreationTime = (double)c.getCreationTime().toEpochSecond() / 60;
					Pair<Double, List<Message>> pair = new MutablePair<Double, List<Message>>(channelCreationTime, channelMessages);
					allMessages.put(c.getId(), pair);
					totalMessages+=channelMessages.size();
				} catch (InsufficientPermissionException e) {
					e.printStackTrace();
					event.getChannel().sendMessage(e.getMessage() + " exception scraping " + c + ". proceeding").queue();
				} catch (Exception e) {
					e.printStackTrace();
					event.getChannel().sendMessage(e.getMessage() + " exception scraping " + c).queue();
					success = false;
				}
				
			}
			
			Map<String, Boolean> usedChannels = new ConcurrentHashMap<String, Boolean>(10, 0.8f, 4);
			
			if(success) {
				//messages per minute
				double serverMessagesPerTime = totalMessages / (currentTime - targetGuildCreationTime);
				event.getChannel().sendMessage("server messages per time is " + serverMessagesPerTime).queue();
				for(Entry<String, Pair<Double, List<Message>>> entry : allMessages.entrySet()) {
					TextChannel c = targetGuild.getTextChannelById(entry.getKey());
					List<Message> channelMessages = entry.getValue().getRight();
					double channelMessagesPerTime = ((double) channelMessages.size() / ((channelMessages.get(0).getCreationTime().toEpochSecond() - c.getCreationTime().toEpochSecond()) / 60));
					boolean channelSufficientlyActive = channelMessagesPerTime >= (serverMessagesPerTime / (double)allMessages.size());
					event.getChannel().sendMessage("channel " + entry.getKey() + "-" + c.getName() + " messages per time: " + channelMessagesPerTime + ", use: " + channelSufficientlyActive).queue();
					usedChannels.put(entry.getKey(), channelSufficientlyActive);
				}
				
				MarkovDatabase db = new MarkovDatabase(targetId, demo.getJda().getGuildById(targetId).getName(), 10, 6, true);
				InputHandler input = new InputHandler(db);
				curTime = System.currentTimeMillis();
				StringBuilder sb = new StringBuilder();
				//go over every channel we included
				for(Entry<String, Pair<Double, List<Message>>> entry : allMessages.entrySet()) {
					//only check channels we've designated to be used
					TextChannel c = targetGuild.getTextChannelById(entry.getKey());
					if(usedChannels.get(entry.getKey())) {
						//go over every message in that channel
						for(Message m : entry.getValue().getRight()) {
							//only use non-bot messages
							if(!m.getAuthor().isBot()) {
								input.processLine(m.getContentRaw());
								//add it to our stringbuilder
								sb.append(m.getContentRaw());
								sb.append("\r\n");
							}
						}
					}
				}
				
				System.out.println("finished processing in " + (System.currentTimeMillis() - curTime) + "ms");
				System.out.println("starting save");
				db.saveDatabase();
				
				String data = sb.toString().trim();
				File outFile = new File("./potatoes.txt");
				try {
					FileUtils.writeStringToFile(outFile, data, utf8);
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				
			}
		}
	}
	
	@Override
	public void onGuildJoin(GuildJoinEvent event) {
		
		
		
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
	
	//General message generation
	//Input: target server g, target channel c
	//Output: generates a message from the server's markovdb and sends it to the given channel
	//Checks for certain illegal message circumstances
	public void sendMessageToChannel(Guild g, TextChannel c) {
		String msg;
		int attemptCounter = 0;
		MessageBuilder m = null;
		while(attemptCounter < 10) {
			//generate message string
			msg = markovOutput.get(g.getId()).createMessage(true);
			attemptCounter++;
			
			//message validation
			//if the message is empty, something weird happened so skip the message regeneration and just send a garb msg
			if(msg.isEmpty()) {
				if(g.getEmotesByName("mothahh", true).isEmpty()) {
					m = new MessageBuilder("aaa");
				} else {
					m = new MessageBuilder();
					m.append(g.getEmotesByName("mothahh", true).get(0));
				}
				break;
			//message nonempty. check if it has a banned phrase - if so, regen
			} else if(containsBannedPhrase(msg, g.getId()) != null){
				continue;
			//message content ok. final validation stuff
			} else {
				m = new MessageBuilder(msg);
				//if pings banned in server, strip them
				if(!preferences.get(g.getId()).allowPings)
					m.stripMentions(g, Message.MentionType.USER, Message.MentionType.EVERYONE, Message.MentionType.HERE, Message.MentionType.ROLE);
				break;
			}
		}
		
		//if we never generated a message, generate garb msg
		if(m == null) m = new MessageBuilder("aaa");
		//otherwise, send our message
		c.sendMessage(m.build()).queue();
		
		
	} 
	
	//call this on a given message to process it into database
	//makes all the necessary checks to determine whether or not to actually process it
	/*
	 * things to check include:
	 * does it contain a banned phrase? (if so, do something based on our moderation setting)
	 * is it in a valid channel?
	 * is it from a banned user? (TODO)
	 */
	public boolean processMessage(GuildMessageReceivedEvent e) {
		Guild g = e.getGuild();
		String targetId = g.getId();
		User u = e.getAuthor();
		boolean shouldProcess = true;
		boolean processed = false;
		//flags:
		//1: don't add filtered messages to database
		//2: send dm to user to warn them
		//3: delete msg
		//4: remove users message send permission
		//5: kick user
		//6: ban user
		String match = containsBannedPhrase(e.getMessage().getContentDisplay(), targetId);
		MessageBuilder response = new MessageBuilder();
		if(match != null) {
			response.append("dear valued user,\r\nyour recent message in server " + formatGuildId(targetId)
					+ " contained the following banned phrase:\r\n`" + match + "`\r\n");
			String targetFlags = preferences.get(targetId).filterAction;
			if(targetFlags.contains("1")) {
				shouldProcess = false;
			}
			if(targetFlags.contains("3")) {
				try {
					e.getMessage().delete().complete();
					response.append("your message has been deleted. ");
				} catch(InsufficientPermissionException ex) {
					logger.info("attempted & failed to delete message w/ banned phrase in server " + formatGuildId(targetId));
				}
			}
			if(targetFlags.contains("4")) {
				Member m = g.getMember(u);
				List<TextChannel> permittedChannels = getUserPermittedChannels(g, u);
				List<TextChannel> changedChannels = new ArrayList<TextChannel>();
				for(TextChannel c : permittedChannels) {
					try {
						PermissionOverride perm = c.getPermissionOverride(m);
						if(perm == null) {
							c.createPermissionOverride(m).setDeny(net.dv8tion.jda.core.Permission.MESSAGE_WRITE).complete();
						} else {
							perm.getManager().deny(net.dv8tion.jda.core.Permission.MESSAGE_WRITE);
						}
						changedChannels.add(c);
					} catch(InsufficientPermissionException ex) {
						logger.info("insufficient permission to remove user " + u.getId()
								+ " (" + u.getName() + ")'s message_write permission in channel "
								+ c.getId() + " (" + c.getName() + ") in guild " + formatGuildId(targetId));
					}
				}
				if(!changedChannels.isEmpty()) {
					response.append("you can no longer send messages in the following channels: ");
					response.append(changedChannels.get(0).getId() + " (" + changedChannels.get(0).getName() + ")");
					for(int i=1; i < changedChannels.size(); i++) {
						TextChannel c = changedChannels.get(i);
						response.append(", ");
						response.append(c.getId() + " (" + c.getName() + ")");
					}
					response.append(". ");
				}
			}
			if(targetFlags.contains("5")) {
				try {
					g.getController().kick(u.getId()).complete();
					response.append("you have been kicked from the server. ");
				} catch (InsufficientPermissionException | HierarchyException ex) {
					logger.info("insufficient permission to kick user " + u.getId() + " (" 
							+ u.getName() + ") from guild " + formatGuildId(targetId));
				}
			}
			if(targetFlags.contains("6")) {
				try {
					g.getController().ban(u, 0).complete();
					response.append("you have been banned from the server. ");
				} catch (InsufficientPermissionException | HierarchyException ex) {
					logger.info("insufficient permission to ban user " + u.getId() + " (" 
							+ u.getName() + ") from guild " + formatGuildId(targetId));
				}
			}
			if(targetFlags.contains("2")) {
				response.append("\r\n\nif you have any questions or concerns, please contact your resident administrator\n");
				response.append("yours truly,\r\n");
				response.append("http://i.imgur.com/20wmYfp.png\r\n");
				response.append("cutebot");
				PrivateChannel chan = u.openPrivateChannel().complete();
				chan.sendMessage(response.build()).queue();
			}
		}
		
		//thats all the wordfilter stuff
		//check if its in a valid channel. if so, process it
		if(shouldProcess && preferences.get(targetId).discussionChannels.contains(e.getChannel())) {
			markovInput.get(targetId).processLine(e.getMessage().getContentRaw());
			processed = true;
		}
		return processed;
		
	}
	
	public void processHistory(List<List<Message>> messageSet, String id) {
		//nuke our old db
		markovDbs.put(id, new MarkovDatabase(id, demo.getJda().getGuildById(id).getName(), 10, 6, true));
		markovInput.put(id, new InputHandler(markovDbs.get(id)));
		markovOutput.put(id,  new OutputHandler(markovDbs.get(id)));
		for(List<Message> messages : messageSet) {
			for(Message m : messages) {
				if(!m.getAuthor().isBot() && (containsBannedPhrase(m.getContentRaw(), id) == null)) {
					markovInput.get(id).processLine(m.getContentRaw());
				}
			}
		}
	}
	
	//checks given message against server's bannedPhrase regex
	//returns the first matching string, or null if no match is found
	//TODO return all matching strings?
	public String containsBannedPhrase(String message, String guildId) {
		//Pattern is null if no banned phrase so return false by default
		if(preferences.get(guildId).bannedPhrase == null) return null;
		Matcher m = preferences.get(guildId).compiledFilter.matcher(message);
		if(m.find()) {
			return m.group();
		}
		return null;
	}
	
	private void bannedMessageResponse(GuildMessageReceivedEvent e) {
		
	}
	
	public List<Message> getMessagesInChannel(TextChannel c) throws Exception {
		long currentTime = System.currentTimeMillis();
		long recentId;
		MessageHistory history = c.getHistory();
		List<Message> messages;
		history.retrievePast(100).complete();
		messages = history.getRetrievedHistory();
		try {
			if(!messages.isEmpty()) {
				do {
					if(messages.size() % 10000 == 0) System.out.println("counted " + messages.size() + " messages");
					recentId = messages.get(messages.size()-1).getIdLong();
					history.retrievePast(100).complete();
					messages = history.getRetrievedHistory();
				} while (recentId != messages.get(messages.size()-1).getIdLong());
			}
		} catch(Exception e) {
			e.printStackTrace();
			logger.info("encountered exception in channel " + c + " in guild " + c.getGuild());
			return null;
		}	finally {
			logger.info("counted " + messages.size() + " messages total from " + c.getName() + " in " + (System.currentTimeMillis() - currentTime) + "ms");
		
//			curTime = System.currentTimeMillis();
//			System.out.println("starting processing");
//			if(success) {
//				for(Message msg : messages) {
//					if(!msg.getAuthor().isBot()) {
//						markovIn.processLine(msg.getRawContent());
//					}
//				}
//				
//				System.out.println("finished processing in " + (System.currentTimeMillis() - curTime) + "ms");
//				System.out.println("starting save");
//				curTime = System.currentTimeMillis();
//				markovDb.saveDatabase();
//				System.out.println("finished saving in " + (System.currentTimeMillis() - curTime) + "ms");
//			}
		}
		
		return messages;
	}
	
	//TODO remove admin permission checks from each individual command
	//centralize it in the check for command validity in onPrivateMessageReceived
	//as well as guild id validation/retrieval
//	public void buildCommands() {
//		//for adjusting the wordfilter
//		//if user shares one server, they dont need to specify which server
//		//use: !filter <mode> [<args>] [<server id>]
//		//modes: add, remove, show/view, regex, clear/delete
//		String name = "!filter";
//		Command c = new Command(name, Permission.ADMIN) {
//			public void response(PrivateMessageReceivedEvent e) {
//				List<Guild> mutualGuilds = e.getAuthor().getMutualGuilds();
//				String targetGuild=null;
//				
//				//remove excess whitespace to make things easier to process
//				String msg = CommandFactory.sanitize(e.getMessage().getContentDisplay());
//				String[] words = msg.split(" ");
//				
//				//quick error check
//				if(words.length < 2) {
//					e.getChannel().sendMessage("error: missing arguments").queue();
//					return;
//				}
//				
//				//determine target server. if server id is supplied, it should be the
//				//last word, so check it.
//				//note args need to be surrounded with "", so if no server id is supplied,
//				//the last word will end in a " and thus cannot match a server id
//				
//				//server id was given by user
//				if(preferences.containsKey(words[words.length - 1])) {
//					//check if they have authority for that server
//					if(adminDatabase.get(e.getAuthor().getId()).contains(words[words.length - 1])) {
//						//user has authority over specified server
//						targetGuild = words[words.length - 1];
//					} else {
//						//user does not have authority over specified server
//						e.getChannel().sendMessage("error: you don't have permission to change the filter for that server (id " + words[words.length - 1] + ")").queue();
//						return;
//					}
//				//server id not given by user
//				} else {
//					//check if they're only in one mutual server, and use it if so
//					if(mutualGuilds.size() == 1) {
//						targetGuild = mutualGuilds.get(0).getId();
//					} else {
//						//more than one mutual guild. ambiguous
//						e.getChannel().sendMessage("error: no server specified").queue();
//						return;
//					}
//				}
//				
//				//targetGuild now contains the server id to use
//				//obtain target filter. this is either a comma-separated list of banned words
//				//or the special token "<[_REGEX]>"
//				String targetFilter = preferences.get(targetGuild).bannedPhrase;
//				
//				//filter add case
//				//use: !filter add "<words>" [<server id>]
//				if(words[1].equals("add")) {
//					
//					//assuming it's a normal filter (not regex),
//					//add new words if they don't exist already
//
//					//filter already exists case
//					//we have to test null here to avoid some exceptions
//					if(targetFilter != null) {
//						//error condition checks
//						//if wordfilter has been set to an explicit regex, don't allow
//						//add/removal
//						if(targetFilter.equals("<[_REGEX]>")) {
//							e.getChannel().sendMessage("error: filter has been set to explicit regex; add/remove have been disabled. if you didn't set this, consult your fellow administrators").queue();
//							return;
//						}
//						//require word list to be enclosed in ""
//						//two " means the message is split into 3 parts, so less is an error
//						if(msg.split("\"").length < 3) {
//							e.getChannel().sendMessage("error: new words to filter not surrounded with \"\"").queue();
//							return;
//						}
//						//require word list to be nonempty
//						if(msg.split("\"")[1].isEmpty()) {
//							e.getChannel().sendMessage("error: list of new words to filter is empty").queue();
//							return;
//						}
//						
//						//split on ", retrieve index 1: this should be comma-separated list
//						//of words to add to filter. split it on ,
//						String newFilteredWords[] = msg.split("\"")[1].split(",");
//						List<String> currentFilteredWords = Arrays.asList( targetFilter.split(",") );
//						//for tracking which words have actually been added, for feedback after
//						Set<String> addedWords = new HashSet<String>(10);
//						//for each new word to add, check if it exists in our filter already
//						//add it if not
//						for(String newWord : newFilteredWords) {
//							
//							//skip over reserved tokens
//							if(newWord.equals("<[_REGEX]>")) continue;
//							
//							if(!currentFilteredWords.contains(newWord)) {
//								currentFilteredWords.add(newWord);
//								addedWords.add(newWord);
//							}
//						}
//						
//						//addedWords now contains all unique new words to add
//						for(String word : addedWords) {
//							targetFilter = targetFilter + "," + word;
//						}
//						
//						preferences.get(targetGuild).bannedPhrase = targetFilter;
//						//TODO add a method to save filter to disk and update pattern
//						//call it here
//						//actually filter saving will happen during db save i think
//						updateFilter(targetGuild);
//						
//						
//						e.getChannel().sendMessage("added the following to the word filter for server " 
//								+ demo.getJda().getGuildById(targetGuild).getName() + "(" + targetGuild 
//								+ "): " + addedWords).queue();
//					//no filter exists case
//					} else {
//						
//						//require word list to be enclosed in ""
//						if(msg.indexOf('\"') == -1) {
//							e.getChannel().sendMessage("error: new words to filter not surrounded with \"\"").queue();
//							return;
//						}
//						//require word list to be nonempty
//						if(msg.split("\"")[1].isEmpty()) {
//							e.getChannel().sendMessage("error: list of new words to filter is empty").queue();
//							return;
//						}
//						
//						//split on ", retrieve index 1: this should be comma-separated list
//						//of words to add to filter. split it on ,
//						String newFilteredWords[] = msg.split("\"")[1].split(",");
//						
//						
//						List<String> addedWords = new ArrayList<String>(10);
//						
//						for(String newWord : newFilteredWords) {
//							if(!addedWords.contains(newWord)) {
//								addedWords.add(newWord);
//							}
//						}
//						
//						//addedWords contains at least one element by above error
//						//condition checks, so this never throws exception
//						targetFilter = "" + addedWords.get(0);
//						//add remaining words
//						for(int i=1; i < addedWords.size(); i++) {
//							targetFilter = targetFilter + "," + addedWords.get(i);
//						}
//						
//						//update settings
//						preferences.get(targetGuild).bannedPhrase = targetFilter;
//						//TODO call filter update method here
//						updateFilter(targetGuild);
//						
//						e.getChannel().sendMessage("added the following to the word filter for server " 
//								+ demo.getJda().getGuildById(targetGuild).getName() + "(" + targetGuild 
//								+ "): " + addedWords).queue();
//					}
//				//filter removal case
//				//use: !filter remove "<words>" [<server id>]
//				} else if(words[1].equals("remove")) {
//					//load the target server's filter. assuming it's a normal filter (not regex),
//					//remove words if they exist
//
//					//filter already exists case
//					//we have to test null here to avoid some exceptions
//					if(targetFilter != null) {
//						//error condition checks
//						//if wordfilter has been set to an explicit regex, don't allow
//						//add/removal
//						if(targetFilter.equals("<[_REGEX]>")) {
//							e.getChannel().sendMessage("error: filter has been set to explicit regex; add/remove have been disabled. if you didn't set this, consult your fellow administrators").queue();
//							return;
//						}
//						//require word list to be enclosed in ""
//						if(msg.split("\"").length < 3) {
//							e.getChannel().sendMessage("error: words to remove not surrounded with \"\"").queue();
//							return;
//						}
//						//require word list to be nonempty
//						if(msg.split("\"")[1].isEmpty()) {
//							e.getChannel().sendMessage("error: list of words to remove is empty").queue();
//							return;
//						}
//						
//						//split on ", retrieve index 1: this should be comma-separated list
//						//of words to add to filter. split it on ,
//						List<String> removeFilteredWords = Arrays.asList(msg.split("\"")[1].split(","));
//						List<String> currentFilteredWords = Arrays.asList(targetFilter.split(","));
//						List<String> removedWords = new ArrayList<String>(10);
//						//for each word in filter, check if it's slated for removal
//						//if so, remove it. track which words have been removed for feedback later
//						for(String currentWord : currentFilteredWords) {
//							if(removeFilteredWords.contains(currentWord)) {
//								currentFilteredWords.remove(currentWord);
//								removedWords.add(currentWord);
//							}
//						}
//						
//						//rebuild our filter with pruned list of filtered words
//						//if filter has been totally deleted, set to null.
//						if(currentFilteredWords.size() == 0) {
//							targetFilter = null;
//						} else {
//							//at least one element exists so this never throws exception
//							targetFilter = currentFilteredWords.get(0);
//							//append remaining words
//							for(int i=1; i < currentFilteredWords.size(); i++) {
//								targetFilter += "," + currentFilteredWords.get(i);
//							}
//						}
//
//						
//						preferences.get(targetGuild).bannedPhrase = targetFilter;
//						//TODO add a method to save filter to disk and update pattern
//						//call it here
//						updateFilter(targetGuild);
//						
//						e.getChannel().sendMessage("removed the following from the word filter for server " 
//								+ demo.getJda().getGuildById(targetGuild).getName() + "(" + targetGuild 
//								+ "): " + removedWords).queue();
//					//no filter exists case
//					} else {
//						//nothing to remove, so simply report error
//						e.getChannel().sendMessage("error: server " + demo.getJda().getGuildById(targetGuild).getName() + "("
//								+ targetGuild + ") currently has no existing wordfilter").queue();
//					}
//				//filter show case
//				//use: !filter show [<server id>]
//				} else if(words[1].equals("show")) {
//					//explicit regex needs to be handled separately
//					if(targetFilter.equals("<[_REGEX]>")) {
//						e.getChannel().sendMessage("current wordfilter for server " + demo.getJda().getGuildById(targetGuild).getName()
//								+ "(" + targetGuild + ") has been set to the following regex:\n" 
//								+ preferences.get(targetGuild).compiledFilter.pattern() + "\n").queue();
//					} else {
//						e.getChannel().sendMessage("current wordfilter for server " + demo.getJda().getGuildById(targetGuild).getName()
//								+ "(" + targetGuild + "): " + targetFilter).queue();
//					}
//				//filter clear case
//				//use: !filter clear [<server id>]
//				} else if(words[1].equals("clear")) {
//					targetFilter = null;
//					//TODO do we call filter update here? or do we handle null in message processing?
//					e.getChannel().sendMessage("wordfilter for server " + demo.getJda().getGuildById(targetGuild).getName()
//							+ "(" + targetGuild + ") has been cleared").queue();
//				//set filter to regex case (for advanced use)
//				//use: !filter regex <regex> [<server id>]
//				} else if(words[1].equals("regex")) {
//					//in this case we require a 3rd argument
//					//this check was covered above by requiring "", but regex can be arbitrary 
//					//so we just need some 3rd word
//					if(words.length < 3) {
//						e.getChannel().sendMessage("error: no regex supplied").queue();
//						return;
//					}
//					
//					//update pattern with the provided regex
//					try {
//						updateFilterExplicitly(targetGuild, words[2]);
//						e.getChannel().sendMessage("wordfilter for server " + demo.getJda().getGuildById(targetGuild).getName()
//								+ "(" + targetGuild + ") has been set").queue();
//					} catch(PatternSyntaxException ex) {
//						e.getChannel().sendMessage("error: bad syntax on given regex").queue();
//					}
//				//specify action to take on someone using a banned phrase
//				//use: !filter action <flags> [<server id>]
//				} else if(words[1].equals("action")) {
//					//flags:
//					//1: don't add filtered messages to database
//					//2: send dm to user to warn them
//					//3: delete msg
//					//4: remove users message send permission
//					//5: kick user
//					//6: ban user
//					//7: send censored version of msg? maybe thats dumb yea actually dont do this
//					if(words[2].length() > 6) {
//						e.getChannel().sendMessage("error: too many flags supplied").queue();
//						return;
//					} else if(words[2].matches("[^123456]")) {
//						e.getChannel().sendMessage("error: invalid flags").queue();
//						return;
//					}
//					preferences.get(targetGuild).filterFlags = words[2];
//					e.getChannel().sendMessage("the following actions will now be taken when a user sends a message with a filtered phrase:\r\n"
//							+ formatFlagString(words[2])).queue();
//					return;
//				} else {
//					e.getChannel().sendMessage("error: invalid mode of operation").queue();
//					return;
//				}
//			}
//		};
//		commands.add(name, c);
//		
//		//for adjusting permissions
//		//maintains a file on disk of user ids followed by the servers they have permission over
//		//can be used by admins but with restrictions (only on servers they have admin powers on)
//		//part of the "let them do whatever they want and they can sort it out" philosophy
//		//TODO make specifying server optional if only in 1
//		name = "!admin";
//		c = new Command(name, Permission.ADMIN) {
//			public void response(PrivateMessageReceivedEvent e) {
//				String[] words = CommandFactory.sanitize(e.getMessage().getContentDisplay()).split(" ");
//				//words: 0 = command, 1 = mode of operation, 2 = user id, 3 = server id
//				
//				//preliminary error checks
//				//TODO should these be in a single method? im repeating a lot
//				//syntax check
//				if(words.length != 4) {
//					e.getChannel().sendMessage("error: incorrect use of !admin command").queue();
//					return;
//				}
//				//check the server id
//				if(!demo.getGuildIds().contains(words[3])) {
//					e.getChannel().sendMessage("error: invalid server id").queue();
//					return;
//				}
//				//check the user id in the server
//				if(demo.getJda().getGuildById(words[3]).getMemberById(words[2]) == null) {
//					e.getChannel().sendMessage("error: no user with id " + words[2] + " exists in server " + formatGuildId(words[3])).queue();
//					return;
//				}
//				//now we know they've given 3 arguments, with the 3rd being a valid server and 2nd a valid member in that server
//				//next, ensure they actually are allowed to modify that server's admin list
//				
//				//i believe this condition shouldnt ever happen, since if they're not admin they get a visibility error
//				//on trying to use !admin - so if they get here they must be an admin of something
//				//but just to be safe we'll check to ensure we avoid null pointer exception
//				if(!permissions.containsKey(e.getAuthor().getId())) {
//					e.getChannel().sendMessage("error: you don't have permission to use that command").queue();
//					return;
//				}
//				//check if they have authority over that server
//				if(!permissions.get(e.getAuthor().getId()).contains(words[3])) {
//					e.getChannel().sendMessage("error: you don't have authority to change permissions for that server").queue();
//					return;
//				}
//				
//				
//				//adding permission
//				//use: !admin add <user id> <server id>
//				//consider adding ability to add multiple servers at once
//				if(words[1].equals("add")) {
//					//if user exists in permissions map already, add the server to their set of servers
//					//otherwise, add them as an entry, with a new set of just that server
//					if(permissions.containsKey(words[2])) {
//						HashSet<String> servers = permissions.get(words[2]);
//						if(servers.add(words[3])) {
//							permissions.put(words[2], servers);
//							e.getChannel().sendMessage("user " + words[2] + " has been given admin permissions for server " 
//									+ formatGuildId(words[3])).queue();
//							return;
//						} else {
//							e.getChannel().sendMessage("error: user " + words[2] + " already has admin permissions for server "
//									+ formatGuildId(words[3])).queue();
//							return;
//						}
//						//TODO should i be saving to disk somewhere in here?
//					} else {
//						HashSet<String> server = new HashSet<String>(1);
//						server.add(words[3]);
//						permissions.put(words[2], server);
//						e.getChannel().sendMessage("user " + words[2] + " has been given admin permissions for server " 
//								+ formatGuildId(words[3])).queue();
//						return;
//					}
//					
//				//admin removal case
//				//use: !admin remove <user id> <server id>
//				} else if(words[1].equals("remove")) {
//					//removal requires some extra error checks
//					if(!permissions.containsKey(words[2])) {
//						e.getChannel().sendMessage("error: user " + words[2] + " does not have admin permissions for server "
//								+ formatGuildId(words[3])).queue();
//						return;
//					}
//					if(!permissions.get(words[2]).contains(words[3])) {
//						e.getChannel().sendMessage("error: user " + words[2] + " does not have admin permissions for server "
//								+ formatGuildId(words[3])).queue();
//						return;
//					}
//					//if only 1 server exists, remove that user from permissions map
//					//otherwise, just remove that server
//					if(permissions.get(words[2]).size() == 1) {
//						permissions.remove(words[2]);
//						e.getChannel().sendMessage("user " + words[2] + "'s admin permissions for server "
//								+ formatGuildId(words[3]) + " have been removed").queue();
//						return;
//					} else {
//						HashSet<String> servers = permissions.get(words[2]);
//						//set contains server id by above error condition check
//						//race condition these nuts
//						servers.remove(words[3]);
//						permissions.put(words[2], servers);
//						e.getChannel().sendMessage("user " + words[2] + "'s admin permissions for server "
//								+ formatGuildId(words[3]) + " have been removed").queue();
//						return;
//					}
//				} else {
//					e.getChannel().sendMessage("error: invalid mode of operation provided").queue();
//					return;
//				}
//			}
//		};
//		commands.add(name, c);
//		
//		//for allowing/disabling pings
//		//use: !pings <mode> <server>
//		name = "!pings";
//		c = new Command(name, Permission.ADMIN) {
//			public void response(PrivateMessageReceivedEvent e) {
//				String[] words = CommandFactory.sanitize(e.getMessage().getContentDisplay()).split(" ");
//				//words: 0 = command, 1 = mode of operation, 2 = server
//				
//				//error check
//				if(words.length != 3) {
//					e.getChannel().sendMessage("error: incorrect use of !pings command").queue();
//					return;
//				}
//				
//				
//				//turn on pings
//				if(words[1].equals("on")) {
//					if(preferences.get(words[2]).allowPings) {
//						e.getChannel().sendMessage("error: pings are already enabled for server "
//								+ formatGuildId(words[2])).queue();
//						return;
//					} else {
//						preferences.get(words[2]).allowPings = true;
//						e.getChannel().sendMessage("pings have been enabled for server " + formatGuildId(words[2])).queue();
//						return;
//					}
//				} else if(words[1].equals("off")) {
//					if(!preferences.get(words[2]).allowPings) {
//						e.getChannel().sendMessage("error: pings are already disabled for server "
//								+ formatGuildId(words[2])).queue();
//						return;
//					} else {
//						preferences.get(words[2]).allowPings = false;
//						e.getChannel().sendMessage("pings have been disabled for server " + formatGuildId(words[2])).queue();
//						return;
//					}
//				} else {
//					e.getChannel().sendMessage("error: invalid mode of operation").queue();
//					return;
//				}
//			}
//		};
//		commands.add(name, c);
//		
//		name = "!scrape";
//	}
	
	//call this method on a guild when its filter has been changed
	//it breaks the filter into individual words, builds a regex, then compiles it and puts it
	//in that guild's pattern object
	//generated regex will match all words in the list, as whole words, optionally ending with an 's'
	//for example, filter list "apple,orange,banana" is built into string 
	//"\\b((?:apple)|(?:orange)|(?:banana))s?\\b"
	//which should match any whole word: apple, apples, orange, oranges, banana, bananas
	//the word "crabapple" would not trigger a match
	private void updateFilter(String targetGuild) throws PatternSyntaxException {
		String filteredWords[] = preferences.get(targetGuild).bannedPhrase.split(",");
		String newFilter = "\\b(";
		for(String word : filteredWords) {
			newFilter += "(?:" + word + ")|";
		}
		//the final added pipe needs to be removed
		newFilter = newFilter.substring(0, newFilter.length() - 1);
		newFilter += ")s?\\b";
		preferences.get(targetGuild).compiledFilter = Pattern.compile(newFilter, Pattern.CASE_INSENSITIVE);
	}
	
	//creates a pattern object with the given regex, for the designated server
	private void updateFilterExplicitly(String targetGuild, String regex) throws PatternSyntaxException {
		preferences.get(targetGuild).compiledFilter = Pattern.compile(regex, Pattern.CASE_INSENSITIVE);
		preferences.get(targetGuild).bannedPhrase = "<[_REGEX]>";
	}
	
	public boolean isAdmin(User user) {
		if(permissions.containsKey(user.getId())) {
			return true;
		} else {
			return false;
		}
	}
	
	public boolean isDeveloper(User user) {
		if(user.getId().equals("115618938510901249")) {
			return true;
		} else {
			return false;
		}
	}
	
	public boolean isQuestion(String s) {
		if(s.contains("what") || s.contains("who") || s.contains("why") || s.contains("wat") || s.contains("how") || s.contains("when") || s.contains("will") || s.contains("are you") || s.contains("are u") || s.contains("can you") || s.contains("do you") || s.contains("can u") || s.contains("do u") || s.contains("where") || s.contains("?")) {
			return true;
		} else {
			return false;
		}
	}
	
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
		}
		//should be unreachable
		return "";
	}
	
	public Map<String,Integer> process(String str, Map<String,Integer> master) {
		Set<String> s = new HashSet<String>();
		String curString = "";
		int curCount = 0;
		Pattern myPattern = Pattern.compile(":[^\\s:]+:");
		Matcher myMatcher = myPattern.matcher(str);
		while(myMatcher.find()) {
			curString = str.substring(myMatcher.start(), myMatcher.end());
			s.add(curString);
		}
		for(String i : s) {
			curString = i;
			if(master.get(curString)!=null) {
				curCount = master.get(curString);
				master.put(curString, curCount+1);
			} else {
				master.put(curString,  1);
			}
		}
		
		return master;
	}
	
	public MessageEmbed quoteOfTheDay() {
		Calendar c = Calendar.getInstance();
		int s = getDateInteger(c);
		EmbedBuilder e = new EmbedBuilder();
		
		int end = Math.floorMod(shiftBits(s) * p, quoteArray.length);
		
		e.setDescription(quoteArray[end]);
		return e.build();
	}
	
	public void sendQuote(MessageChannel c) {
		MessageBuilder newMsg = new MessageBuilder();
		newMsg.append("today's Twitch Chat:tm: Quote Of The Day (!quote)");
		newMsg.setEmbed(quoteOfTheDay());
		c.sendMessage(newMsg.build()).complete();
	}
	
	public String formatGuildId(String id) {
		return (id + " (" + demo.getJda().getGuildById(id).getName() + ")");
	}
	
	//circular shift a 10-bit int by 7 bits for reasons
	public int shiftBits(int a) {
		int b = a & 0x7F;
		return (b << 4) | (a >> 7);
	}
	
	public int getDateInteger(Calendar c) {
		return (c.get(Calendar.MONTH) + 1) * 100 + c.get(Calendar.DAY_OF_MONTH);
	}
	
	private String formatFlagString(String s) {
		String str="";
		if(s.contains("1")) {
			str += "1: the message will not be processed into my database\r\n";
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
	
	private List<TextChannel> getUserPermittedChannels(Guild g, User u) {
		List<TextChannel> userChannels = new ArrayList<TextChannel>();
		List<TextChannel> allChannels = g.getTextChannels();
		Member m = g.getMember(u);
		for(TextChannel c : allChannels) {
			if(PermissionUtil.checkPermission(c, m, net.dv8tion.jda.core.Permission.MESSAGE_WRITE)) {
				userChannels.add(c);
			}
		}
		return userChannels;
	}
	
}

