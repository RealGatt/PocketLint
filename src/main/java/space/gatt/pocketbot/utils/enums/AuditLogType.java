package space.gatt.pocketbot.utils.enums;

import org.apache.commons.lang3.StringUtils;

public enum AuditLogType {
	USER_JOIN(true), USER_LEAVE(true), // join/leave

	BAN_USER(true), UNBAN_USER(true), // ban/unban

	MESSAGE_EDIT(true), MESSAGE_DELETE(true), MESSAGE_SEND(false), // messages

	ROLE_CREATE(true), ROLE_DELETE(true), ROLE_MODIFY(true), // roles
	GIVE_ROLE(true), REMOVE_ROLE(true),

	CHANNEL_CREATE(true), CHANNEL_DELETE(true), CHANNEL_MODIFY(true), // channels
	VOICE_CHANNEL_CREATE(true), VOICE_CHANNEL_DELETE(true), VOICE_CHANNEL_MODIFY(true), // channels

	CHANNEL_PERMISSION_OVERRIDE(true), ROLE_PERMISSION_CHANGE(true),

	EMOTE_ADD(false), EMOTE_DELETE(false), EMOTE_MODIFY(false); // emotes

	private boolean defaultVal;

	AuditLogType(boolean defaultVal) {
		this.defaultVal = defaultVal;
	}

	public boolean getDefaultVal() {
		return defaultVal;
	}

	public String getName() {
		return StringUtils.capitalize(super.name().replaceAll("_", " ").toLowerCase());
	}
}
