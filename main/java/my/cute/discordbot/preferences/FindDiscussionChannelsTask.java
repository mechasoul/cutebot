package my.cute.discordbot.preferences;

import java.time.OffsetDateTime;
import java.time.temporal.TemporalUnit;
import java.util.Set;

import my.cute.discordbot.bot.GuildScraper;
import my.cute.discordbot.bot.demo;
import net.dv8tion.jda.core.entities.Game;
import net.dv8tion.jda.core.entities.Guild;

public class FindDiscussionChannelsTask implements Runnable {

	private final Guild g;
	private final Long startTime;
	
	public FindDiscussionChannelsTask(Guild g) {
		this.g = g;
		this.startTime = null;
	}
	
	public FindDiscussionChannelsTask(Guild g, long a, TemporalUnit u) {
		this.g = g;
		this.startTime = OffsetDateTime.now().minus(a, u).toInstant().toEpochMilli();
	}
	
	@Override
	public void run() {
		PreferencesManager.addGuildModifyingDiscChan(this.g.getIdLong());
		Game currentGame = demo.getJda().getPresence().getGame();
		demo.getJda().getPresence().setGame(Game.playing("busy"));
		Set<Long> discChannels = PreferencesUtils
				.determineDiscussionChannels(GuildScraper.getGuildMessagesSince(g, startTime));
		PreferencesManager.getGuildPreferences(g.getIdLong()).setDiscussionChannels(discChannels);
		demo.getJda().getPresence().setGame(currentGame);
		PreferencesManager.removeGuildModifyingDiscChan(this.g.getIdLong());
	}

}
