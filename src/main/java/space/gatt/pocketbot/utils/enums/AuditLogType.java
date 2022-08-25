package space.gatt.pocketbot.utils.enums;

import org.apache.commons.lang3.StringUtils;
import space.gatt.pocketbot.utils.MessageUtil;

import java.awt.*;

public enum AuditLogType {
	USER_JOIN(true), USER_LEAVE(true), // join/leave

	BAN_USER(true, Color.RED), UNBAN_USER(true, Color.GREEN), // ban/unban

	MESSAGE_EDIT(true, Color.YELLOW), MESSAGE_DELETE(true, Color.YELLOW), MESSAGE_SEND(false), // messages

	ROLE_CREATE(true, Color.CYAN), ROLE_DELETE(true, Color.RED), ROLE_MODIFY(true, Color.YELLOW), // roles
	GIVE_ROLE(true, Color.YELLOW), REMOVE_ROLE(true, Color.YELLOW),

	CHANNEL_CREATE(true, Color.CYAN), CHANNEL_DELETE(true, Color.RED), CHANNEL_MODIFY(true, Color.YELLOW), // channels
	VOICE_CHANNEL_CREATE(true, Color.CYAN), VOICE_CHANNEL_DELETE(true, Color.RED), VOICE_CHANNEL_MODIFY(true, Color.YELLOW), // channels

	CHANNEL_PERMISSION_OVERRIDE(true), ROLE_PERMISSION_CHANGE(true),

	EMOTE_ADD(false, Color.GREEN), EMOTE_DELETE(false, Color.RED), EMOTE_MODIFY(false, Color.YELLOW); // emotes

	private final boolean defaultVal;
	private final Color displayColor;

	AuditLogType(boolean defaultVal) {
		this.defaultVal = defaultVal;
		this.displayColor = MessageUtil.getColor();
	}

	AuditLogType(boolean defaultVal, Color displayColor) {
		this.defaultVal = defaultVal;
		this.displayColor = displayColor;
	}

	public boolean getDefaultVal() {
		return defaultVal;
	}

	public Color getDisplayColor() {
		return displayColor;
	}

	public String getName() {
		return StringUtils.capitalize(super.name().replaceAll("_", " ").toLowerCase());
	}
}
