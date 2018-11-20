package my.cute.discordbot.preferences;

import java.util.EnumSet;

public enum FilterResponse {
	SKIP_PROCESS, SEND_RESPONSE, DELETE_MESSAGE, 
	MUTE, KICK, BAN;
	
	public static final EnumSet<FilterResponse> ALL_OPTIONS = EnumSet.allOf(FilterResponse.class);
}
