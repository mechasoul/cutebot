package my.cute.discordbot;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import my.cute.markov.InputHandler;
import my.cute.markov.MarkovDatabase;
import my.cute.markov.OutputHandler;
import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.MessageBuilder;
import net.dv8tion.jda.core.entities.Game;
import net.dv8tion.jda.core.entities.Guild;
import net.dv8tion.jda.core.entities.Message;
import net.dv8tion.jda.core.entities.MessageChannel;
import net.dv8tion.jda.core.entities.MessageEmbed;
import net.dv8tion.jda.core.entities.User;
import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent;
import net.dv8tion.jda.core.events.message.priv.PrivateMessageReceivedEvent;
import net.dv8tion.jda.core.hooks.ListenerAdapter;

import org.apache.commons.io.FileUtils;

public class MyListener extends ListenerAdapter {
	
	public Map<String, Integer> emoteCount;
	public Map<String, String> adminDatabase;
//	public Map<String, String> voiceChannels;
	List<Guild> connectedServers;
	public long curTime, prevTime;
	private long prevQuoteTime, curQuoteTime, quoteTimeThreshold;
	private ConcurrentLinkedQueue<Long> recentMessageTimes;
	private int recentMessageCount, recentMessageThreshold;
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
	
	Map<String, InputHandler> markovInput;
	Map<String, MarkovDatabase> markovDbs;
	Map<String, OutputHandler> markovOutput;
	
	
	
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
		prevQuoteTime = System.currentTimeMillis();
		curQuoteTime = System.currentTimeMillis();
		//1 hr in ms
		quoteTimeThreshold = 3600000;
		recentMessageTimes<Long> = new ConcurrentLinkedQueue<Long>();
		recentMessageCount = 0;
		//threshold holds the size of recentMessageTimes. recentMessageCount holds the number of
		//messages within a given timeframe
		recentMessageThreshold = 0;
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
		
		
		System.out.print("connected to: ");
		
		for(Guild g : connectedServers) {
			System.out.print(g.getName() + "-" + g.getId() + ", ");
		}
		System.out.println();
		
		for(Guild g : connectedServers) {
			String id = g.getId();
			markovDbs.put(id, new MarkovDatabase(g.getName() + "-" + id, 10, 6, true));
			markovInput.put(id, new InputHandler(markovDbs.get(id)));
			markovOutput.put(id,  new OutputHandler(markovDbs.get(id)));
			markovDbs.get(id).loadDatabase();
		}
		
		
	}
	

	
	//this is our response method most conditional stuff happens here
	@Override
	public void onGuildMessageReceived(GuildMessageReceivedEvent event) {
		curQuoteTime = System.currentTimeMillis();
		curTime = System.currentTimeMillis();
		cal = Calendar.getInstance();
		
		Message message = event.getMessage();
		String content = message.getContent();
		MessageChannel channel = event.getChannel();
		User author = event.getAuthor();
		Guild guild = event.getGuild();
		MessageBuilder m;
		
		messageSentFlag = false;
		
		//don't respond to bot messages, unless we want to trigger the apocalypse
		if(event.getAuthor().isBot()) {
			if(enableApocalypse) {
				String msg = markovOutput.get(guild.getId()).createMessage(true);
				if(msg.isEmpty()) {
					if(guild.getEmotesByName("mothahh", true).isEmpty()) {
						channel.sendMessage("aaa").queue();
					} else {
						m = new MessageBuilder();
						m.append(guild.getEmotesByName("mothahh", true).get(0));
						channel.sendMessage(m.build()).queue();
					}
				} else {
					channel.sendMessage(msg).queue();
				}
			} else {
				return;
			}
		}
		
		recentMessageCount++;
		recentMessageTimes.add(curQuoteTime);
		recentMessageThreshold++;
		//remove elements until the oldest message is within quoteTimeThreshold
		while(curQuoteTime - recentMessageTimes.peek() >= quoteTimeThreshold) {
			recentMessageTimes.poll();
			recentMessageThreshold--;
		}
		//check if we've exceeded our threshold for sending a message, and send if so
		if(recentMessageCount >= recentMessageThreshold) {
			recentMessageCount = 0;
			//consider putting this code into a function since im now reusing it in a couple places
			String msg = markovOutput.get(guild.getId()).createMessage(true);
			if(msg.isEmpty()) {
				if(guild.getEmotesByName("mothahh", true).isEmpty()) {
					channel.sendMessage("aaa").queue();
				} else {
					m = new MessageBuilder();
					m.append(guild.getEmotesByName("mothahh", true).get(0));
					channel.sendMessage(m.build()).queue();
				}
			} else {
				channel.sendMessage(msg).queue();
			}
			messageSentFlag = true;
		}
		
		//oznet id
		if(guild.getId().equals("101153748377686016")) {
			emoteCount = process(content, emoteCount);
		}
		
		//general sht
		
		//banned user ids go here
		if(!author.getId().equals("101030074504986624")) {
			markovInput.get(guild.getId()).processLine(message.getRawContent());
		}

		if(!messageSentFlag && content.toLowerCase().contains("cutebot") && isQuestion(content.toLowerCase())) {
			String msg = markovOutput.get(guild.getId()).createMessage(true);
			if(msg.isEmpty()) {
				if(guild.getEmotesByName("mothahh", true).isEmpty()) {
					channel.sendMessage("aaa").queue();
				} else {
					m = new MessageBuilder();
					m.append(guild.getEmotesByName("mothahh", true).get(0));
					channel.sendMessage(m.build()).queue();
				}
			} else {
				channel.sendMessage(msg).queue();
			}
			//stupid ass pingpong command
		} else if(content.toLowerCase().contains("cutebot") || content.toLowerCase().contains(":mothyes:")) {
			Random r = new Random();
			int randomNumber = r.nextInt(10);
			m = new MessageBuilder();
			//low chance
			if(randomNumber==1) {
				//if emote doesnt exist, send url to image
				if(guild.getEmotesByName("mothuhh", true).isEmpty()) {
					channel.sendMessage("http://i.imgur.com/zVnJhzy.png");
				} else {
					//else send image
					//this line sends as reaction
					message.addReaction(guild.getEmotesByName("mothuhh", true).get(0)).queue();
					//these lines send as its own message
					//m.append(guild.getEmotesByName("mothuhh", true).get(0));
					//channel.sendMessage(m.build()).queue();
				}
				//high chance
			} else {
				//make sure emote exists. if not, send url to image
				if(guild.getEmotesByName("mothyes", true).isEmpty()) {
					channel.sendMessage("http://i.imgur.com/20wmYfp.png").queue();
				} else {
					message.addReaction(guild.getEmotesByName("mothyes", true).get(0)).queue();
					//m.append(guild.getEmotesByName("mothyes", true).get(0));
					//channel.sendMessage(m.build()).queue();
				}
			}
		}

		//curTime - quoteTime >= 86400000 || 
		if(content.equalsIgnoreCase("!quote")) {
			sendQuote(channel);
		}
		//f id
		//🇦 🇧 🇨 🇩 🇪 🇫 🇬 🇭 🇮 🇯 🇰 🇱 🇲 🇳 🇴 🇵 🇶 🇷 🇸 🇹 🇺 🇻 🇼 🇽 🇾 🇿
		if(guild.getId().equals("115619022304706565")) {
			System.out.println(author.getName());
			/*
			* markovIn.processLine(message.getRawContent());
			* String msg = markovOut.createMessage(true);
			* if (msg.equals("")) {
			* 	System.out.println("message empty");
			* } else {
			* 	channel.sendMessage(msg).queue();
			* }
			*/
		}
		
		if(curTime - prevTime >= 1800000) {//half hour
			timeToSave = true;
		}
		if(content.equalsIgnoreCase("!exit") && isDeveloper(author)) {
			timeToSave = true;
			timeToExit = true;
			channel.sendMessage("ok").complete();
		}
		if(timeToSave) {
			save(emoteCount, guild);
			for(Guild g : connectedServers) {
				markovDbs.get(g.getId()).saveDatabase();
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
		String content = message.getContent();
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
			demo.getJda().getPresence().setGame(Game.of(content.substring(content.indexOf(' ') + 1)));
		}
		if(params[0].equalsIgnoreCase("!statusreset") && author.getName().equals("Mecha") && author.getDiscriminator().equals("5810")) {
			demo.getJda().getPresence().setGame(null);
		}
	}
	
//	@Override
//	public void onGuildJoin(GuildJoinEvent event) {
//		fillVoiceChannels();
//		
//		//check markov dbs
//		for(Guild g : demo.getJda().getGuilds()) {
//			String id = g.getId();
//			//if we dont have a db for this guild in our map, create one
//			if(!(markovDbs.containsKey(id))) {
//				//initialize our markov stuff and check our load
//				markovDbs.put(id, new MarkovDatabase(g.getName() + "-" + id, 10, 4, true));
//				markovInput.put(id, new InputHandler(markovDbs.get(id)));
//				markovOutput.put(id,  new OutputHandler(markovDbs.get(id)));
//				markovDbs.get(id).loadDatabase();
//			}
//		}
//	}
	
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
	
	public boolean isAdmin(User user) {
		if(adminDatabase.containsKey(user.getId())) {
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
	
	public String getFatLink() {
		Random r = new Random();
		switch(r.nextInt(10)) {
			case 0: return "https://www.nhlbi.nih.gov/health/health-topics/topics/obe";
			case 1: return "https://en.wikipedia.org/wiki/Obesity";
			case 2: return "http://www.heart.org/HEARTORG/HealthyLiving/WeightManagement/Obesity/Obesity-Information_UCM_307908_Article.jsp";
			case 3: return "http://www.who.int/mediacentre/factsheets/fs311/en/";
			case 4: return "https://medlineplus.gov/obesity.html";
			case 5: return "http://www.nhs.uk/conditions/Obesity/Pages/Introduction.aspx";
			case 6: return "https://www.cdc.gov/healthyweight/effects/";
			case 7: return "http://kidshealth.org/en/teens/obesity.html";
			case 8: return "http://i.imgur.com/n7CNsas.png";
			case 9: return "https://www.asu.edu/courses/css335/da_whybad.html";
		}
		return "you are fat";
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
	
	public void save(Map<String,Integer> m, Guild g) {
		StringBuilder builder = new StringBuilder();
		for (Map.Entry<String,Integer> kvp : m.entrySet()) {
		    builder.append(kvp.getKey());
		    builder.append(",");
		    builder.append(""+kvp.getValue());
		    builder.append("\r\n");
		}
		String timeStamp = new SimpleDateFormat("yyyyMMdd-HHmmss").format(Calendar.getInstance().getTime());
		String data = builder.toString().trim();
		try {
			FileUtils.writeStringToFile(new File("./emotecount/"+g.getName()+"/count_"+timeStamp+".csv"), data, utf8);
		} catch (IOException e) {
			e.printStackTrace();
		}
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
	
	//circular shift a 10-bit int by 7 bits for reasons
	public int shiftBits(int a) {
		int b = a & 0x7F;
		return (b << 4) | (a >> 7);
	}
	
	public int getDateInteger(Calendar c) {
		return (c.get(Calendar.MONTH) + 1) * 100 + c.get(Calendar.DAY_OF_MONTH);
	}
	
}

