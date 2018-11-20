package my.cute.discordbot;

import my.cute.discordbot.bot.demo;
import net.dv8tion.jda.core.entities.Member;
import net.dv8tion.jda.core.entities.TextChannel;
import net.dv8tion.jda.core.entities.User;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class IdUtils {
	
	private static final Logger logger = LoggerFactory.getLogger(IdUtils.class);

	public static String getFormattedServer(long serverId) {
		try {
			String out = demo.getJda().getGuildById(serverId).getName() + " (" + serverId + ")";
			return out;
		} catch (NullPointerException e) {
			// TODO Auto-generated catch block
			logger.error("nullpointer in trying to retrieve formatted server string for server "
					+ serverId);
			e.printStackTrace();
			return "null error";
		}
	}
	
	public static String getFormattedUser(long userId, long serverId) {
		try {
			Member m = demo.getJda().getGuildById(serverId).getMemberById(userId);
			String out = m.getEffectiveName() + " (" + m.getUser().getName() + "#"
					+ m.getUser().getDiscriminator() + " id " + userId + ")";
			return out;
		} catch (NullPointerException e) {
			logger.error("nullpointer in trying to retrieve formatted user string for user " + userId
					+ ", server " + serverId);
			e.printStackTrace();
			return "null error";
		}
	}
	
	public static String getFormattedUser(String userId, long serverId) {
		try {
			String out = getFormattedUser(Long.parseLong(userId), serverId);
			return out;
		} catch (NumberFormatException e) {
			logger.error("numberformatexception in trying to parse given user id: " + userId);
			e.printStackTrace();
			return "numberformatexception";
		}
	}
	
	public static String getFormattedUser(long userId) {
		try {
			User u = demo.getJda().getUserById(userId);
			String out = u.getName() + "#" + u.getDiscriminator() + " id " + userId;
			return out;
		} catch (NullPointerException e) {
			logger.error("nullpointer in trying to retrieve formatted user string for user " + userId);
			e.printStackTrace();
			return "null error";
		}
	}
	
	public static String getFormattedUser(String userId) {
		try {
			String out = getFormattedUser(Long.parseLong(userId));
			return out;
		} catch (NumberFormatException e) {
			logger.error("numberformatexception in trying to parse given user id: " + userId);
			e.printStackTrace();
			return "numberformatexception";
		}
	}
	
	public static String getFormattedChannel(String channelId, long serverId) {
		try {
			TextChannel t = demo.getJda().getGuildById(serverId).getTextChannelById(channelId);
			String out = t.getName() + " (" + t.getId() + ")";
			return out;
		} catch (NumberFormatException e) {
			return "numberformatexception";
		} catch (NullPointerException e) {
			return "nullpointerexception";
		}
	}
	
	public static String getFormattedChannel(long channelId, long serverId) {
		return getFormattedChannel(""+channelId, serverId);
	}
	
	public static boolean isValidChannel(String channelId, long serverId) {
		
		//we should only call this in a command response so we know server id is valid
		//so this should never throw a nullpointerexception
		//but im gonna catch it for debug in case
		try {
			boolean exists = (demo.getJda().getGuildById(serverId).getTextChannelById(channelId) != null);
			return exists;
		} catch (NullPointerException e) {
			logger.error("ERROR null pointer that shouldn't exist in isValidChannel call");
			e.printStackTrace();
			return false;
		} catch (NumberFormatException e) {
			return false;
		}
		
	}
	
	public static boolean isValidChannel(long channelId, long serverId) {
		return isValidChannel(""+channelId, serverId);
	}
	
	public static boolean isValidServer(long id) {
		return demo.getJda().getGuildById(id) != null;
	}
	
}
