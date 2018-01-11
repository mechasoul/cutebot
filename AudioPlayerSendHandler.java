package my.cute.discordbot;

import net.dv8tion.jda.core.audio.AudioSendHandler;

import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;
import com.sedmelluq.discord.lavaplayer.track.playback.AudioFrame;

public class AudioPlayerSendHandler implements AudioSendHandler {
	private final AudioPlayer audioPlayer;
	private AudioFrame lastFrame;

	public AudioPlayerSendHandler(AudioPlayer audioPlayer) {
		this.audioPlayer = audioPlayer;
	}

	@Override
	public boolean canProvide() {
		lastFrame = audioPlayer.provide();
		return lastFrame != null;
	}

	@Override
	public byte[] provide20MsAudio() {
		return lastFrame.data;
	}

	@Override
	public boolean isOpus() {
		return true;
	}
}
