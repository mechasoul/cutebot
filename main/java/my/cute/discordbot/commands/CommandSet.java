package my.cute.discordbot.commands;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class CommandSet {
	
	Map<String, Command> commands;
	
	public CommandSet() {
		this.commands = new ConcurrentHashMap<String, Command>();
	}
	
	public boolean add(Command c) {
		if(this.commands.containsKey(c.name)) {
			return false;
		} else {
			this.commands.put(c.name, c);
			return true;
		}
	}
	
	public boolean contains(Command c) {
		return this.commands.containsKey(c.name);
	}
	
	public Command getCommand(Command c) {
		return this.commands.get(c.name);
	}
	
	public Command getCommand(String name) {
		return this.commands.get(name);
	}
	
}
