package my.cute.discordbot.commands;

import net.dv8tion.jda.core.events.message.priv.PrivateMessageReceivedEvent;

public abstract class Command {
	
	public enum CommandType {
		PRIVATE, GUILD
	}
	
	private final CommandType type;
	private final String name;
	private final RequiredPermissionLevel reqPermission;
	private final boolean requiresTargetServer;
	private String description;
	private String help;
	
	public Command(String n) {
		this.name = n;
		this.reqPermission = RequiredPermissionLevel.USER;
		this.type = CommandType.PRIVATE;
		this.requiresTargetServer = false;
	}
	
	public Command(String n, RequiredPermissionLevel p, boolean server) {
		name = n;
		reqPermission = p;
		this.type = CommandType.PRIVATE;
		this.requiresTargetServer = server;
	}
	
	public Command(String n, RequiredPermissionLevel p, CommandType t, boolean server) {
		name = n;
		reqPermission = p;
		this.type = t;
		this.requiresTargetServer = server;
	}
	
	public Command(String n, String d, RequiredPermissionLevel p, boolean server) {
		name = n;
		description = d;
		reqPermission = p;
		this.type = CommandType.PRIVATE;
		this.requiresTargetServer = server;
	}
	
	public Command(String n, String d, String h, RequiredPermissionLevel p, CommandType type, boolean server) {
		name = n;
		help = h;
		description = d;
		reqPermission = p;
		this.type = type;
		this.requiresTargetServer = server;
	}
	
	//each command object overrides this with its own code to execute when someone uses the command
	public abstract void response(CommandResponseParamHolder p);
	
	
	public String getTargetGuild(PrivateMessageReceivedEvent e) {
		return null;
	}
	
	public CommandType getType() {
		return type;
	}

	public String getName() {
		return name;
	}

	public RequiredPermissionLevel getReqPermission() {
		return reqPermission;
	}

	public boolean requiresTargetServer() {
		return requiresTargetServer;
	}

	public String getDescription() {
		return description;
	}

	public String getHelp() {
		return help;
	}

	public void setDescription(String d) {
		description = d;
	}
	
	public void setHelp(String h) {
		help = h;
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((name == null) ? 0 : name.hashCode());
		result = prime * result + ((type == null) ? 0 : type.hashCode());
		return result;
	}

	/* 
	 * two commands are equal if they have the same name and type
	 */
	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (getClass() != obj.getClass()) {
			return false;
		}
		Command other = (Command) obj;
		if (name == null) {
			if (other.name != null) {
				return false;
			}
		} else if (!name.equals(other.name)) {
			return false;
		}
		if (type != other.type) {
			return false;
		}
		return true;
	}
	
}
