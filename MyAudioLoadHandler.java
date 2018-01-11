package my.cute.discordbot;

import com.sedmelluq.discord.lavaplayer.player.AudioLoadResultHandler;
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException;
import com.sedmelluq.discord.lavaplayer.track.AudioPlaylist;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;

public class MyAudioLoadHandler implements AudioLoadResultHandler {
	
	TrackScheduler trackScheduler;
	
	public MyAudioLoadHandler(TrackScheduler t) {
		trackScheduler = t;
	}

	@Override
	public void loadFailed(FriendlyException throwable) {
		System.out.println("error: " + throwable.getMessage());
	}

	@Override
	public void noMatches() {
		System.out.println("nothing found");
	}

	@Override
	public void playlistLoaded(AudioPlaylist playlist) {
		for (AudioTrack track : playlist.getTracks()) {
			trackScheduler.queue(track);
		}
	}

	@Override
	public void trackLoaded(AudioTrack track) {
		System.out.println("queueing track: " + track.getInfo().title + ", " + track.getInfo().uri);
		//if url contained a specified start time
		trackScheduler.queue(track);
	}

}
