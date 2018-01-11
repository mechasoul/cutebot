package my.cute.discordbot;

import java.io.PrintStream;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import net.dv8tion.jda.core.entities.Emote;
import net.dv8tion.jda.core.entities.Member;
import net.dv8tion.jda.core.entities.Message;
import net.dv8tion.jda.core.entities.MessageChannel;
import net.dv8tion.jda.core.entities.MessageHistory;
import net.dv8tion.jda.core.entities.TextChannel;
import net.dv8tion.jda.core.entities.User;
import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent;
import net.dv8tion.jda.core.events.message.priv.PrivateMessageReceivedEvent;
import net.dv8tion.jda.core.hooks.ListenerAdapter;

public class MyScraper extends ListenerAdapter {
	
	long curTime;
	boolean startedScraping;
	
	public MyScraper() {
		curTime = System.currentTimeMillis();
		startedScraping = false;
	}

	@Override
	public void onGuildMessageReceived(GuildMessageReceivedEvent event) {
//		curTime = System.currentTimeMillis();
//		Calendar cal = Calendar.getInstance();
//		
//		Message message = event.getMessage();
//		String content = message.getContent();
//		MessageChannel channel = event.getChannel();
//		User author = event.getAuthor();
//		Guild guild = event.getGuild();
//		MessageBuilder m;
//		
//		if(content.equalsIgnoreCase("!exit") && author.getId().equals("115618938510901249")) {
//			demo.startShutdown();
//		}
//		
//		if(guild.getId().equals("101153748377686016")) {
//			if(message.getContent().contains("exhibit A") && message.getContent().contains("cutebot")) {
//				File emoteFile = new File("./emotecount/Oznet/oznet.csv");
//				try {
//					LineIterator it = FileUtils.lineIterator(emoteFile, "UTF-8");
//					
//					m = new MessageBuilder();
//					m.append("emote use in oznet (#brbnoose)\r\n");
//					m.appendCodeBlock(padRight("emote name", (40 - "emote name".length())) + padRight("count", 5) + padRight("exists?", 3), "");
//					
//					int count = 0;
//					
//					while(it.hasNext()) {
//						if(count >= 20) {
//							count = 0;
//							System.out.println("resetting count");
//							channel.sendMessage(m.build()).queue();
//							m = new MessageBuilder();
//						}
//							String[] s = it.nextLine().split(",\\s*");
//							String str = (demo.getJda().getGuildById("101153748377686016").getEmotesByName(s[0], true).size() != 0) + "";
//							m.appendCodeBlock(padRight(s[0], (40 - s[0].length())) + padRight(s[1], (10 - s[1].length())) + padRight(str, (10 - str.length())), "");
//							count++;
//					}
//					
//					m.append("\r\nhttp://i.imgur.com/XJ3qBib.png");
//					
//					channel.sendMessage(m.build()).queue();
//							
//					it.close();
//				} catch (IOException e1) {
//					// TODO Auto-generated catch block
//					e1.printStackTrace();
//				}
//			}
//		}
//		
	}
	
	public List<Message> getMessagesInChannel(TextChannel c, PrintStream ps) throws Exception {
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
		} finally {
			output("counted " + messages.size() + " messages total from " + c.getName() + " in " + (System.currentTimeMillis() - currentTime) + "ms", System.out, ps);
		
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
	
	public void countEmotes(List<Message> list, PrintStream ps) {
		Map<String, Integer> emoteMap = new ConcurrentHashMap<String, Integer>(50, 0.8f, 8);
		for(Message m : list) {
			//set of all emotes used in the message
			Set<Emote> set = new HashSet<Emote>(m.getEmotes());
			//proceed if the message contained some emotes
			if(!(set.isEmpty())) {
				for(Emote e : set) {
					//if emote is not in map yet, create entry for it and set count to 1
					if(!(emoteMap.containsKey(e.getName()))) {
						emoteMap.put(e.getName(), 1);
					}
					//otherwise, get its count and increment it
					else {
						int curVal = emoteMap.get(e.getName());
						emoteMap.put(e.getName(), curVal+1);
					}
				}
			}
		}
		
		output("", System.out, ps);
		for(Map.Entry<String, Integer> e : emoteMap.entrySet()) {
			output(e.getKey() + ", " + e.getValue(), System.out, ps);
		}
		output("", System.out, ps);
	}
	
	@Override
	public void onPrivateMessageReceived(PrivateMessageReceivedEvent event) {
		Message message = event.getMessage();
		String content = message.getContent();
		MessageChannel channel = event.getChannel();
		User author = event.getAuthor();
		String[] words = content.split(" ");
		
		try {
			if(words[0].equalsIgnoreCase("!users") && author.getId().equals("115618938510901249")) {
				List<Member> members = demo.getJda().getGuildById(words[1]).getMembers();
				int count=0;
				String msg="";
				while(count < members.size()) {
					User user = members.get(count).getUser();
					msg += user.getName() + "#" + user.getDiscriminator() + "\n";
					count++;
					if(count % 50 == 0) {
						channel.sendMessage(msg).complete();
						msg = "";
					}
				}
				channel.sendMessage(msg).queue();
			}
		} catch (Exception e) {
			// TODO Auto-generated catch block
			channel.sendMessage("something went wrong").queue();
			e.printStackTrace();
		}
	}
	
	private void output(String text, PrintStream ps1, PrintStream ps2) {        
	    ps1.println(text);
	    ps2.println(text);
	}
	
	public static String padRight(String s, int n) {
	     for(int i=0; i < n; i++) {
	    	 s+=" ";
	     }
	     return s;
	}

	public static String padLeft(String s, int n) {
	    return String.format("%1$" + n + "s", s);  
	}
}
