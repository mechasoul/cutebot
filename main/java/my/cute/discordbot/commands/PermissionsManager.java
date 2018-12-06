package my.cute.discordbot.commands;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.HashSet;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import my.cute.discordbot.IdUtils;
import my.cute.discordbot.bot.demo;
import net.dv8tion.jda.core.entities.Guild;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.LineIterator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PermissionsManager {

	private static final Logger logger = LoggerFactory.getLogger(PermissionsManager.class);
	//user -> set of servers
	private static final Map<Long, HashSet<Long>> userToServerPerms = new ConcurrentHashMap<Long, HashSet<Long>>();
	//server -> set of users
	private static final Map<Long, HashSet<Long>> serverToUserPerms = new ConcurrentHashMap<Long, HashSet<Long>>();
	//user -> server
	private static final Map<Long, Long> defaultGuilds = new ConcurrentHashMap<Long, Long>();
	
	public static void save() {
		for(Map.Entry<Long, HashSet<Long>> entry : serverToUserPerms.entrySet()) {
			StringBuilder sb = new StringBuilder();
			entry.getValue().forEach(userId -> sb.append(userId + "\r\n"));
			try {
				FileUtils.writeStringToFile(new File("./markovdb/" + entry.getKey() + "/admins.ini"), 
						sb.toString().trim(), "UTF-8");
			} catch (IOException e) {
				logger.error("couldn't save admins.ini for server " 
						+ IdUtils.getFormattedServer(entry.getKey()) + " in PermissionsManager.save()");
				e.printStackTrace();
			}
		}
		
		StringBuilder sb = new StringBuilder();
		defaultGuilds.entrySet().forEach(entry -> sb.append(entry.getKey()+","+entry.getValue()+"\r\n"));
		try {
			FileUtils.writeStringToFile(new File("./markovdb/defaultguilds.ini"),
					sb.toString().trim(), "UTF-8");
		} catch (IOException e) {
			logger.error("couldn't save defaultguilds.ini in PermissionsManager.save()");
			e.printStackTrace();
		}
	}
	
	public static void load() {
		//construct permissions map from file
		//honestly i'm unsure about always keeping this file in memory since really it shouldnt be used THAT often
		//but it should be relatively small and its kind of a pain to load/save everything from/to file every time
		userToServerPerms.clear();
		serverToUserPerms.clear();
		defaultGuilds.clear();
		
		//construct default server object
		//holds the default server for each user so they can choose not to specify server if there's only 1 they use
		//format for each line is <user id>,<server id>
		try (LineIterator defaultIterator = FileUtils.lineIterator(new File("./markovdb/defaultguilds.ini"), "UTF-8")) {
			while(defaultIterator.hasNext()) {
				String words[] = defaultIterator.nextLine().split(",");
				defaultGuilds.put(Long.parseLong(words[0]), Long.parseLong(words[1]));
			}
		} catch (FileNotFoundException ex) {
			//first run initialisation
			//nothing really has to happen here
			//default guilds for owners will be initialised as admins.ini check below happens
			//defaultguilds.ini will be created on save() call
			logger.warn("PermissionsManager.load(): defaultguilds.ini not found! will be created on next save, "
					+ "proceeding...");
		} catch (IOException | NumberFormatException e) {
			logger.error("couldn't build default server object in PermissionsManager.load(). fatal error");
			e.printStackTrace();
			System.exit(1);
		}
		
		demo.getJda().getGuilds().forEach(PermissionsManager::loadServer);
		
	}
	
	private static void loadServer(Guild g) {
		Long serverId = g.getIdLong();
		serverToUserPerms.put(serverId, new HashSet<Long>(4));
		try (LineIterator permIterator = FileUtils.lineIterator(new File("./markovdb/" + serverId + "/admins.ini"), "UTF-8")) {
			//each line has id of an admin in that server
			while(permIterator.hasNext()) {
				Long userId = Long.parseLong(permIterator.nextLine());
				addAdmin(userId, serverId);		
			}
			//empty admins.ini check
			//other problems will throw an exception but this would fail quietly
			//shouldnt ever happen, but maybe in exceptional circumstances
			if(serverToUserPerms.get(serverId).isEmpty()) {
				addAdmin(g.getOwner().getUser().getIdLong(), serverId);
				logger.warn("admins.ini was empty for server " 
						+ IdUtils.getFormattedServer(serverId) + "! added owner as admin, "
						+ "proceeding...");
			}	
		} catch (FileNotFoundException ex) {
			//first run initialisation/load after new server join here
			//similar to above, add owner as admin. admins.ini will be created at next save
			addAdmin(g.getOwner().getUser().getIdLong(), serverId);
			logger.info("admins.ini not found for (new?) server " 
					+ IdUtils.getFormattedServer(serverId) + "! added owner as admin, "
					+ "proceeding...");
		} catch (IOException | NumberFormatException e) {
			logger.error("PermissionsManager.loadServer(long): couldn't build permissions object for guild "
					+ IdUtils.getFormattedServer(serverId) + ". fatal error: "
					+ e.getMessage());
			e.printStackTrace();
			demo.exit(1);
		} 
	}
	
	//check if user has admin permission over the specified server
	//if user id isn't present in permissions map, returns false
	//else, if server id isn't present in user id's server hashset, returns false
	//else, return true
	public static boolean userHasPermission(long userId, long serverId) {
		if(!userToServerPerms.containsKey(userId)) return false;
		if(!userToServerPerms.get(userId).contains(serverId)) {
			return false;
		} else {
			return true;
		}
	}
	
	//checks if a user has the given permission level for the given server
	public static boolean userHasPermission(long userId, long serverId, RequiredPermissionLevel p) {
		//maybe this won't always be correct but for now if it's just user permissions, everyone can use it
		if(p == RequiredPermissionLevel.USER) {
			return true;
		} else if(p == RequiredPermissionLevel.DEV) {
			return isDeveloper(userId);
		} else {
			//admin perm check
			
			//devs have admin permissions everywhere
			if(isDeveloper(userId)) return true;
			//if the user isn't an admin anywhere then return false
			if(!userToServerPerms.containsKey(userId)) return false;
			//otherwise, check if the server is one they're an admin for
			if(!userToServerPerms.get(userId).contains(serverId)) {
				return false;
			} else {
				return true;
			}
		}
	}
	
	public static boolean userHasPermission(long userId, RequiredPermissionLevel p) {
		if(p == RequiredPermissionLevel.USER) {
			return true;
		} else if(p == RequiredPermissionLevel.DEV) {
			return isDeveloper(userId);
		} else {
			//admin permission
			//as of right now every admin permission has a server associated
			//so a command having no targeted server but requiring admin permission
			//doesnt make sense
			//but i dont trust myself to remember that
			logger.warn("admin command with no required server looking for permission");
			return isDeveloper(userId);
		}
	}
	
	//gives specified user admin powers over specified server
	//returns true if permission was new, or false if they already were admin
	public static boolean addAdmin(Long userId, Long serverId) {
		boolean added = false;
		//devs don't get added as admin as they have permissions everywhere
		if(isDeveloper(userId)) return false;
		
		//check perms map for user. if they're absent, create new hashset for their servers
		if(!userToServerPerms.containsKey(userId)) {
			HashSet<Long> servers = new HashSet<Long>(4);
			servers.add(serverId);
			userToServerPerms.put(userId, servers);
			defaultGuilds.putIfAbsent(userId,  serverId);
			added = true;
		} else {
			if(userToServerPerms.get(userId).add(serverId)) {
				added = true;
			}
		}
		
		serverToUserPerms.get(serverId).add(userId);
		
		return added;
	}
	
	//for general admin removal, use this
	//don't typically want to remove server owner
	public static boolean removeAdmin(Long userId, Long serverId) {
		
		//server owner can't be removed
		if(demo.getJda().getGuildById(serverId).getOwner().getUser().getIdLong() == userId) {
			return false;
		}
		
		return forceRemoveAdmin(userId, serverId);
	}
	
	//removes specified user's admin powers over specified server
	//returns true if privileges were successfully removed, or false if not (eg doesnt exist)
	public static boolean forceRemoveAdmin(Long userId, Long serverId) {
		boolean removed = false;
		
		if(!userToServerPerms.containsKey(userId)) {
			return false;
		} else {
			HashSet<Long> servers = userToServerPerms.get(userId);
			//check their servers for the given id. remove it if it's there
			if(servers.remove(serverId)) {
				//if it was there, check if it was their last server
				if(servers.isEmpty()) {
					//remove that user's entry from the map if it's now empty
					userToServerPerms.remove(userId);
				} 
				removed = true;
			} else {
				//set unchanged
				removed = false;
			}
		}
		
		if(removed) {
			serverToUserPerms.get(serverId).remove(userId);
		}
		
		return removed;
	}
	
	public static void addServer(Guild g) {
		loadServer(g);
	}
	
	//note this cleans entries from maps, but not off disk
	//also note this doesn't change defaultguilds set to the current server
	//i don't want to iterate every defaultguild on server leave so i'd have to change
	//defaultguilds to a per-server basis to remove them here
	//so for now check if we can just account for it in the actual defaultguild check in commandmanager
	public static void removeServer(Guild g) {
		Long serverId = g.getIdLong();
		HashSet<Long> admins = serverToUserPerms.get(serverId);
		//remove server from all its admins' lists
		admins.forEach(userId -> removeAdmin(userId, serverId));
		forceRemoveAdmin(g.getOwner().getUser().getIdLong(), serverId);
		if(!admins.isEmpty()) {
			logger.warn("PermissionsManager.removeServer(Guild): removed admins and owner "
					+ "from admin set for server " + IdUtils.getFormattedServer(serverId)
					+ ", but it isn't empty? will clear it now. admins: " + admins.toString());
		}
		admins.clear();
		//and remove server from map altogether
		serverToUserPerms.remove(serverId);
	}
	
	//should never be in a server without a corresponding entry in serverToUserPerms
	//so this should never return null as long as we're managing the map correctly on server join/etc
	public static HashSet<Long> getServerAdmins(long serverId) {
		return serverToUserPerms.get(serverId);
	}
	
	//could return null but shouldn't ever be called in a circumstance where it will
	public static HashSet<Long> getServersWithAdminPermissions(long userId) {
		return userToServerPerms.get(userId);
	}
	
	public static boolean isDeveloper(long id) {
		return (id == 115618938510901249L);
	}
	
	public static Long getDefaultGuild(long userId) {
		return defaultGuilds.get(userId);
	}
	
	
	public static void setDefaultGuild(long userId, long serverId) {
		defaultGuilds.put(userId, serverId);
	}
}
