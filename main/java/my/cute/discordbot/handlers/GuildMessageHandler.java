package my.cute.discordbot.handlers;

import java.util.List;
import java.util.Random;

import my.cute.discordbot.IdUtils;
import my.cute.discordbot.bot.DatabaseManager;
import my.cute.discordbot.preferences.PreferencesManager;
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
	private static final Logger messageLogger = LoggerFactory.getLogger("MessageLogger");

	private final long id;
	private final Guild guild;
	
	private WordFilterHandler wordFilterHandler;
	private AutonomyHandler autonomyHandler;
	
	public GuildMessageHandler(Guild g) {
		this.id = g.getIdLong();
		this.guild = g;
		
		wordFilterHandler = new WordFilterHandler(this.id);
		autonomyHandler = new AutonomyHandler(this.guild);
	}
	
	//TODO add guild command handling here
	public boolean handle(GuildMessageReceivedEvent e) {
		
		TextChannel c = e.getChannel();
		String content = e.getMessage().getContentDisplay().toLowerCase();
		
		//wordfilterhandler determines whether or not we process the message
		//let it do its thing here and it returns whether or not to process
		boolean shouldProcess = this.wordFilterHandler.handle(e);
		
		//does this check go somewhere else?
		if(e.getAuthor().isBot()) return false;
		
		boolean processed = false;
		boolean responseSent = false;
		
		//TODO permission check somewhere to see if we can send in channel?
		//or catch insufficientpermissionexception on the sendMessage call
		if (PreferencesManager.getGuildPreferences(this.id).isDiscussionChannel(c.getIdLong())) {
			//check if we need to generate autonomous message
			if (this.autonomyHandler.handle()) {
				Message m = generateMessage();
				c.sendMessage(m).queue();
				responseSent = true;
				messageLogger.info("autonomous message in server " 
						+ IdUtils.getFormattedServer(this.id) + ", message: " 
						+ m.getContentRaw());
			}
			if(shouldProcess) {
				DatabaseManager.getDatabase(this.id).processLine(e.getMessage().getContentRaw());
				processed = true;
			}
		}
		
		if(!responseSent && content.contains("cutebot") && isQuestion(content)) {
			Message m = generateMessage();
			c.sendMessage(m).queue();
			responseSent = true;
			messageLogger.info("prompted message in server " 
					+ IdUtils.getFormattedServer(this.id) + ", message: " 
					+ m.getContentRaw());
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
	private Message generateMessage() {
		String msg;
		int attemptCounter = 0;
		MessageBuilder mb = null;
		while(attemptCounter < 10) {
			//generate message string
			msg = DatabaseManager.getDatabase(this.id).generateMessage(true);
			attemptCounter++;

			//message validation
			//if the message is empty, something weird happened so skip the message regeneration and just send a garb msg
			if(msg.isEmpty()) {
				logger.warn("generated empty message in server " + IdUtils.getFormattedServer(this.id)
						+ " in GuildMessageHandler.generateMessage()");
				mb = new MessageBuilder();

				try {
					mb.append(this.guild.getEmotesByName("mothahh", true).get(0));
				} catch (IndexOutOfBoundsException e) {
					mb.append("aaa");
				}

				break;
			//message nonempty. check if it has a banned phrase - if so, regen
			} else if(PreferencesManager.getGuildPreferences(this.id)
					.checkStringAgainstFilter(msg) != null){
				continue;
			//message content ok. final validation stuff
			} else {
				mb = new MessageBuilder(msg);
				//if pings banned in server, strip them
				if(!PreferencesManager.getGuildPreferences(this.id).pingsEnabled())
					mb.stripMentions(this.guild, Message.MentionType.USER, Message.MentionType.EVERYONE, 
							Message.MentionType.HERE, Message.MentionType.ROLE);
				break;
			}
		}

		//if we never generated a message, generate garb msg
		if(mb == null) {
			mb = new MessageBuilder("aaa");
			logger.warn("reached maximum message generation attempts in server " 
					+ IdUtils.getFormattedServer(this.id) + " in GuildMessageHandler.sendMessageToChannel()"
					+ " (their filter: " + PreferencesManager.getGuildPreferences(this.id).getWordFilterToString() + ")");
		}
		
		return mb.build();


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
	
	public Guild getGuild() {
		return this.guild;
	}
	
	public long getId() {
		return this.id;
	}

	
}
