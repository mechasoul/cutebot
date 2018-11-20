package my.cute.discordbot.handlers;

import java.util.List;
import java.util.Random;

import my.cute.discordbot.IdUtils;
import my.cute.discordbot.bot.DatabaseManager;
import my.cute.discordbot.bot.GuildDatabase;
import my.cute.discordbot.preferences.GuildPreferences;
import my.cute.discordbot.preferences.PreferencesManager;
import my.cute.markov.InputHandler;
import my.cute.markov.MarkovDatabase;
import my.cute.markov.OutputHandler;
import net.dv8tion.jda.core.MessageBuilder;
import net.dv8tion.jda.core.entities.Emote;
import net.dv8tion.jda.core.entities.Guild;
import net.dv8tion.jda.core.entities.Message;
import net.dv8tion.jda.core.entities.TextChannel;
import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GuildMessageHandler {
	
	private static final Logger logger = LoggerFactory.getLogger(GuildMessageHandler.class);

	public final long id;
	private final Guild guild;
	private GuildPreferences preferences;
	//formatted identifier string
	private String guildString;
//	private InputHandler markovInput;
//	private OutputHandler markovOutput;
//	private MarkovDatabase markovDb;
	private GuildDatabase db;
	
	private WordFilterHandler wordFilterHandler;
	private AutonomyHandler autonomyHandler;
	
	public GuildMessageHandler(Guild g) {
		this.id = g.getIdLong();
		this.guild = g;
		this.guildString = IdUtils.getFormattedServer(this.id);
		
		this.preferences = PreferencesManager.getGuildPreferences(this.id);
		
//		markovDb = new MarkovDatabase(g.getId(), g.getName().replaceAll("[^A-Za-z0-9]", ""), 10, 6, true);
//		markovInput = new InputHandler(markovDb);
//		markovOutput = new OutputHandler(markovDb);
//		markovDb.loadDatabase();
		this.db = DatabaseManager.getDatabase(this.id);
		
		wordFilterHandler = new WordFilterHandler(this);
		autonomyHandler = new AutonomyHandler(this.guild);
	}
	
	//
	public boolean handle(GuildMessageReceivedEvent e) {
		
		TextChannel c = e.getChannel();
		String content = e.getMessage().getContentDisplay().toLowerCase();
		
		//wordfilterhandler determines whether or not we process the message
		//let it do its thing here and it returns whether or not to process
		boolean shouldProcess = this.wordFilterHandler.handle(e);
		boolean processed = false;
		boolean responseSent = false;
		
		if (isDiscussionChannel(e.getChannel())) {
			//check if we need to generate autonomous message
			if (this.autonomyHandler.handle()) {
				try {
					sendMessageToChannel(c);
					responseSent = true;
					System.out.println("autonomous message in server "
							+ this.guildString);
				} catch (Exception ex) {
					// TODO Auto-generated catch block
					System.out
							.println("exception thrown in autonomous message gen in server "
									+ this.guildString);
					ex.printStackTrace();
				}
			}
		}
		//thats all the wordfilter stuff
		//check if its in a valid channel. if so, process it
		if(shouldProcess && isDiscussionChannel(e.getChannel())) {
			this.db.processLine(e.getMessage().getContentRaw());
			processed = true;
		}
		
		if(!responseSent && content.contains("cutebot") && isQuestion(content)) {
			sendMessageToChannel(c);
			responseSent = true;
		}
		if(!responseSent && (content.contains("cutebot") || content.contains(":mothyes:"))) {
			generateReaction(e.getMessage(), c);
		}
		
		return processed;
		
	}
		
	//General message generation
	//Input: target channel c
	//Output: generates a message from the server's markovdb and sends it to the given channel
	//Checks for certain illegal message circumstances
	public void sendMessageToChannel(TextChannel c) {
		String msg;
		int attemptCounter = 0;
		MessageBuilder m = null;
		while(attemptCounter < 10) {
			//generate message string
			msg = this.db.generateMessage(true);
			attemptCounter++;

			//message validation
			//if the message is empty, something weird happened so skip the message regeneration and just send a garb msg
			if(msg.isEmpty()) {
				logger.info("WARNING generated empty message in server " + this.guildString);
				if(this.guild.getEmotesByName("mothahh", true).isEmpty()) {
					m = new MessageBuilder("aaa");
				} else {
					m = new MessageBuilder();
					m.append(this.guild.getEmotesByName("mothahh", true).get(0));
				}
				break;
			//message nonempty. check if it has a banned phrase - if so, regen
			} else if(this.wordFilterHandler.containsBannedPhrase(msg) != null){
				continue;
			//message content ok. final validation stuff
			} else {
				m = new MessageBuilder(msg);
				//if pings banned in server, strip them
				if(!preferences.allowPings)
					m.stripMentions(this.guild, Message.MentionType.USER, Message.MentionType.EVERYONE, Message.MentionType.HERE, Message.MentionType.ROLE);
				break;
			}
		}

		//if we never generated a message, generate garb msg
		if(m == null) {
			m = new MessageBuilder("aaa");
			logger.info("WARNING reached maximum message generation attempts in server " + this.guildString);
		}
		//otherwise, send our message
		c.sendMessage(m.build()).queue();


	} 
	
	private void generateReaction(Message message, TextChannel channel) {
		Random r = new Random();
		int randomNumber = r.nextInt(10);
		//low chance
		if(randomNumber==1) {
			//if emote doesnt exist, send url to image
			if(this.guild.getEmotesByName("mothuhh", true).isEmpty()) {
				List<Emote> emotes = this.guild.getEmotes();
				if(emotes.isEmpty()) {
					channel.sendMessage("http://i.imgur.com/zVnJhzy.png").queue();;
				} else {
					message.addReaction(emotes.get(r.nextInt(emotes.size()))).queue();
				}
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
	
	public boolean isQuestion(String s) {
		if(s.contains("what") || s.contains("who") || s.contains("why") || s.contains("wat") || s.contains("how") || s.contains("when") || s.contains("will") || s.contains("are you") || s.contains("are u") || s.contains("can you") || s.contains("do you") || s.contains("can u") || s.contains("do u") || s.contains("where") || s.contains("?")) {
			return true;
		} else {
			return false;
		}
	}
	
	private boolean isDiscussionChannel(TextChannel ch) {
		return this.preferences.discussionChannels.contains(ch.getIdLong());
	}
	
	
	
	public GuildPreferences getPreferences() {
		return this.preferences;
	}
	
	public String getGuildString() {
		return this.guildString;
	}
	
	public Guild getGuild() {
		return this.guild;
	}

	
}
