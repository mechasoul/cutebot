package my.cute.discordbot.music;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.LinkedList;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.logging.FileHandler;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

import my.cute.discordbot.bot.demo;
import net.dv8tion.jda.core.entities.User;
import net.dv8tion.jda.core.entities.VoiceChannel;

import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;
import com.sedmelluq.discord.lavaplayer.player.event.AudioEventAdapter;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import com.sedmelluq.discord.lavaplayer.track.AudioTrackEndReason;

public class TrackScheduler extends AudioEventAdapter {

	private final AudioPlayer player;
	private UserListManager manager;
	private final String guildId;
	private final LinkedBlockingQueue<AudioTrack> queue;
	private LinkedList<AudioTrack> radioList;
	public boolean playingDefaultTrack;
	public boolean playingRadio;
	private AudioTrack defaultTrack;
	private User self;
	private int radioIndex;
	private Random rand;
	
	private final Logger logger = Logger.getLogger(TrackScheduler.class.getName());
	private FileHandler fh = null;

	public TrackScheduler(AudioPlayer player, String s) {
		this.player = player;
		this.guildId = s;
		this.queue = new LinkedBlockingQueue<AudioTrack>();
		this.radioList = new LinkedList<AudioTrack>();
		this.manager = new UserListManager(this.guildId);
		this.playingDefaultTrack = false;
		this.playingRadio = true;
		this.defaultTrack = null;
		self = demo.getJda().getUserById("312807432839626753");
		this.radioIndex = -1;
		this.rand = new Random();
		
		SimpleDateFormat format = new SimpleDateFormat("M-d_HHmmss");
		try {
			fh = new FileHandler("./logs/logger/TrackScheduler"
					+ format.format(Calendar.getInstance().getTime()) + ".log");
		} catch (Exception e) {
			e.printStackTrace();
		}

		fh.setFormatter(new SimpleFormatter());
		logger.addHandler(fh);
	}
	
	public boolean queue(AudioTrack track) {
		// Calling startTrack with the noInterrupt set to true will start the track only if nothing is currently playing. If
		// something is playing, it returns false and does nothing. In that case the player was already playing so this
		// track goes to the queue instead.
		//returns true if track immediately starts, false otherwise
		//before we load a song in our outside class, we queue the song url and user. if our song starts immediately here, the
		//quees should have been empty, so we poll the info that was just added
			
		//play with nointerrupt. 
		//if it doesnt start playing, it returns false and we end up here (playing something that isnt default track)
		if (!player.startTrack(track, true)) {
			//track is queued
			System.out.println("track queued");
			queue.offer(track);
			return false;
		//playing nothing. our previous startTrack call returned true and the track started immediately
		} else {
			//track just started
			logger.info("track played immediately in server " + guildId);
			
			this.playingDefaultTrack = false;

			manager.emptySkippingUsers();
			manager.updatePermittedUsers();
			
			System.out.println("end of track playing immediately");
			
			return true;
		}

	}

	/**
	 * Start the next track, stopping the current one if it is playing.
	 */
	public void nextTrack() {
		// Start the next track, regardless of if something is already playing or not. In case queue was empty, we are
		// giving null to startTrack, which is a valid argument and will simply stop the player.
		//so this forces next track to play, so we can call it as part of skip or just on track end
		
		
		AudioTrack track = queue.poll();
		
		logger.info("in method nextTrack() in server " + guildId + ".\ntrack about to play is " + track + ".\n" 
				+ "current queue: " + queue);
		
		//play default track because our queue has emptied
		//note we have the option to null out defaulttrack whenever! if we do, we just stop the player
		//  -> in this case, our queue() logic above still works
		if(track == null) {
			this.playingDefaultTrack = true;
			AudioTrack clone = null;
			if(this.defaultTrack != null) {
				clone = this.defaultTrack.makeClone();
				clone.setUserData(this.self);
			} else {
				//no tracks in queue, no set default track. play radio if enabled
				if(this.playingRadio) {
					clone = this.radioList.get(radioIndex).makeClone();
					clone.setUserData(this.self);
				}
			}
			player.startTrack(clone, false);
		//next track is not null, so our queue is not empty yet! so we play the new track
		} else {
			this.playingDefaultTrack = false;
			User u = (User) track.getUserData();
			logger.info("userdata: " + u);	
			
			player.startTrack(track,  false);
		}
	}

	@Override
	public void onTrackEnd(AudioPlayer player, AudioTrack track, AudioTrackEndReason endReason) {
		
		//add the finished track to our list of radio tracks if it doesnt already exist
		if(!playingDefaultTrack && checkRadioForGivenTrack(track.getInfo().uri) == null) {
			radioList.addFirst(track);
			radioIndex++;
		}
		
		manager.emptySkippingUsers();
		manager.updatePermittedUsers();
		
		if(playingDefaultTrack && playingRadio) radioIndex++;
		
		//shuffling the radio list should go here
		if(radioIndex == radioList.size()) {
			radioIndex = 0;
			shuffle(radioList);
		}
		
		if(radioList.isEmpty()) this.playingRadio = false;
		
		// Only start the next track if the end reason is suitable for it (FINISHED or LOAD_FAILED)
		if (endReason.mayStartNext) {
			nextTrack();
		} 
		
	}
	
	@Override
	public void onTrackStuck(AudioPlayer player, AudioTrack track, long thresholdMs) {
	    System.out.println("stuck");
	  }
	
	public void clear() {
		queue.clear();
	}
	
	public AudioTrack checkForGivenTrack(String s) {
		for(AudioTrack t : this.queue) {
			if(t.getInfo().uri.equals(s)) {
				return t;
			}
		}
		return null;
	}
	
	public AudioTrack checkRadioForGivenTrack(String s) {
		for(AudioTrack t : this.radioList) {
			if(t.getInfo().uri.equals(s)) {
				return t;
			}
		}
		return null;
	}
	
	public void removeTrackFromQueue(AudioTrack track) {
		this.queue.remove(track);
	}
	
	//removes a given track from the radio list and adjusts radio index if necessary
	public void removeTrackFromRadio(AudioTrack track) {
		int index = this.radioList.indexOf(track);
		this.radioList.remove(index);
		if(index < this.radioIndex) radioIndex--;
	}
	
	public String getAssociatedGuildId() {
		return this.guildId;
	}
	
	//take in a string representation of a user id and check the queue for tracks submitted by same user
	//returns a count of the number of tracks submitted by user with same id
	public int getUserSongCount(String s) {
		int count = 0;
		for(AudioTrack t : queue) {
			User u = (User) t.getUserData();
			//check if the given user id matches this track's
			if (s.equals(u.getId())) count++;
		}
		
		return count;
	}
	
	//takes a string user id and returns list of all their currently queued tracks
	//NOTE this modifies the returned track's userinfo in a way unusual to eveyrthing else so b careful 
	public LinkedList<AudioTrack> getUserSongs(String s) {
		AudioTrack[] queueArray = this.queue.toArray(new AudioTrack[0]);
		LinkedList<AudioTrack> userSongList = new LinkedList<AudioTrack>();
		User u;
		AudioTrack t;
		for(int i=0; i < queueArray.length; i++) {
			u = (User) queueArray[i].getUserData();
			if(u.getId().equals(s)) {
				t = queueArray[i].makeClone();
				t.setUserData(i+1);
				userSongList.add(t);
			}
		}
		
		return userSongList;
	}
	
	//fisher-yates shuffle the list for fair but random playback
	public void shuffle(LinkedList<AudioTrack> list) {
		AudioTrack t;
		int j;
		for(int i=list.size()-1; i >= 0; i--) {
			t = list.get(i);
			j = rand.nextInt(i+1);
			list.set(i, list.get(j));
			list.set(j, t);
		}
		
		this.radioList = list;
	}
	
	//track skipping logic
	public void skipTrack() {
		AudioTrack curTrack = this.player.getPlayingTrack();
		//if it's a default track, stop playing it / remove from radio
		if(playingDefaultTrack) {
			if(playingRadio) {
				//find the actual source instance of the track and remove it
				for(AudioTrack t : radioList) {
					if(curTrack.getInfo().uri.equals(t.getInfo().uri)) {
						this.radioList.remove(t);
						break;
					}
				}
				
			} else {
				this.defaultTrack = null;
			}
		} 
		
		//track removed from radio if relevant, nulled out if default. 
		//now do the relevant maintenance (normally done on track end)
		
		manager.emptySkippingUsers();
		manager.updatePermittedUsers();
		
		if(playingDefaultTrack && playingRadio) radioIndex++;
		
		//shuffling the radio list if necessary
		if(radioIndex == radioList.size()) {
			radioIndex = 0;
			shuffle(radioList);
		}
		
		//finally, start next track
		nextTrack();
	}
	
	public int currentQueueSize() {
		return queue.size();
	}
	
	public int currentSkippingSize() {
		return manager.currentSkippingSize();
	}
	
	public Set<User> getPermittedUsers() {
		return manager.getPermittedUsers();
	}
	
	public void explicitlyUpdatePermittedUsers(VoiceChannel v) {
		manager.updatePermittedUsers(v);
	}
	
	public Set<User> getSkippingUsers() {
		return manager.getSkippingUsers();
	}
	
	public int requiredVotesToSkip() {
		return manager.requiredVotesToSkip();
	}
	
	public boolean addSkippingUser(User u) {
		return manager.addSkippingUser(u);
	}
	
	public LinkedBlockingQueue<AudioTrack> getQueue() {
		return this.queue;
	}
	
	public boolean queueContainsUrl(String s) {
		for(AudioTrack track : this.queue) {
			if(s.equals(track.getInfo().uri)) {
				return true;
			}
		}
		
		return false;
	}
	
	public void setDefaultTrack(AudioTrack t) {
		this.defaultTrack = t;
	}
	

}
