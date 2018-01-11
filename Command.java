package my.cute.discordbot;

import net.dv8tion.jda.core.events.message.priv.PrivateMessageReceivedEvent;

public class Command {

	final String name;
	public final Permission reqPermission;
	String description;
	String help;
	
	public Command(String n, Permission p) {
		name = n;
		reqPermission = p;
	}
	
	public Command(String n, String d, Permission p) {
		name = n;
		description = d;
		reqPermission = p;
	}
	
	public Command(String n, String d, String h, Permission p) {
		name = n;
		help = h;
		description = d;
		reqPermission = p;
	}
	
	public void response(PrivateMessageReceivedEvent e) {
		
	}
	
	public void setDescription(String d) {
		description = d;
	}
	
	public void setHelp(String h) {
		help = h;
	}
	
}
