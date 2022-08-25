package space.gatt.pocketbot.utils.enums;

import org.apache.commons.lang3.StringUtils;

public enum ChannelOption {
	AUDIT_LOG_CHANNEL, WELCOME_CHANNEL, TWITCH_LOG_CHANNEL, TWITCH_ANNOUNCE_CHANNEL;

	public String getName() {
		return StringUtils.capitalize(super.name().replaceAll("_", " ").toLowerCase());
	}

}
