package my.cute.discordbot.commands;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class CommandSet {
	
	Map<String, Command> commands;
	
	public CommandSet() {
		this.commands = new ConcurrentHashMap<String, Command>();
	}
	
	public CommandSet(Set<Command> source) {
		this.commands = new ConcurrentHashMap<String, Command>(source.size() + 2);
		source.forEach(command -> this.commands.putIfAbsent(command.getName(), command));
	}
	
	public boolean add(Command c) {
		if(this.commands.putIfAbsent(c.getName(), c) == null) {
			return true;
		} else {
			return false;
		}
	}
	
	public boolean contains(Command c) {
		return this.commands.containsKey(c.getName());
	}
	
	public Command getCommand(Command c) {
		return this.commands.get(c.getName());
	}
	
	public Command getCommand(String name) {
		return this.commands.get(name);
	}
	
}
