package my.cute.discordbot.bot;

import net.dv8tion.jda.core.entities.Guild;
import my.cute.markov.InputHandler;
import my.cute.markov.MarkovDatabase;
import my.cute.markov.OutputHandler;

public class GuildDatabase {

	private final MarkovDatabase db;
	private final InputHandler input;
	private final OutputHandler output;
	
	public GuildDatabase(Guild g) {
		
		this.db = new MarkovDatabase(g.getId(), g.getName().replaceAll("[^A-Za-z0-9]", ""), 10, 8, true);
		this.input = new InputHandler(db);
		this.output = new OutputHandler(db);
		
	}
	
	public void processLine(String s) {
		this.input.processLine(s);
	}
	
	public String generateMessage(boolean b) {
		return this.output.createMessage(b);
	}
	
	public void save() {
		this.db.saveDatabase();
	}
	
	public void load() {
		this.db.loadDatabase();
	}
	
}
