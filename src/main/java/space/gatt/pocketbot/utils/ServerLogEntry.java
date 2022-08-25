package space.gatt.pocketbot.utils;

import dev.morphia.annotations.Entity;
import dev.morphia.annotations.Id;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.*;
import space.gatt.pocketbot.PocketBotMain;
import space.gatt.pocketbot.configs.GuildConfiguration;
import space.gatt.pocketbot.database.interfaces.MorphiaHelper;
import space.gatt.pocketbot.utils.enums.AuditLogType;
import space.gatt.pocketbot.utils.enums.ChannelOption;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;


@MorphiaHelper(datastore = "pocketbot")
@Entity(value = "actions", useDiscriminator = false)
public class ServerLogEntry {

	@Id
	private String id;

	public String getId() {
		return id;
	}

	private boolean auditLogSuccess = false;

	public boolean isAuditLogSuccess() {
		return auditLogSuccess;
	}

	public void setAuditLogSuccess(boolean auditLogSuccess) {
		this.auditLogSuccess = auditLogSuccess;
	}

	private AuditLogType type;
	private String content, reason = null;
	private long relevantID, channelID, guildID, triggererID;
	private long logChannelMessageID = - 1, logChannelID = - 1;
	private long actionID;
	private String imageURL = null;

	private boolean botAction = false;

	private long time = System.currentTimeMillis();

	private transient Guild guild = null;
	private transient Message message = null;
	private transient User triggerUser = null;

	public ServerLogEntry() {
	}

	public ServerLogEntry(AuditLogType type, Guild guild) {
		this.type = type;
		this.guildID = guild.getIdLong();
		this.guild = guild;
		actionID = GuildConfiguration.getGuildConfiguration(guild).getNextLogID();
		this.id = guildID + "-" + actionID;
		save();
	}

	public ServerLogEntry(AuditLogType type, Guild guild, long backdatedTime) {
		this.type = type;
		this.guildID = guild.getIdLong();
		this.guild = guild;
		this.time = backdatedTime;
		actionID = GuildConfiguration.getGuildConfiguration(guild).getNextLogID();
		this.id = guildID + "-" + actionID;
		save();
	}

	public Guild getGuild() {
		if (guild == null) guild = PocketBotMain.getInstance().getJDAInstance().getGuildById(guildID);
		return guild;
	}

	public String getReason() {
		if (reason != null && reason.equalsIgnoreCase("xx no reason xx")) return "xx no reason xx";
		if (reason == null)
			if (getTriggererID() == -1) return "No reason given. Please use `_actionlog reason " + getActionID() + " The Reason` to update the reason.";
			else return "No reason given. " + (getTriggererAsUser() != null ? getTriggererAsUser().getAsMention() + ", p" : "P") + "lease use `_actionlog reason " + getActionID() + " The Reason` to update the reason.";
		if (reason.length() > 1024) return reason.substring(0, 1024);
		return reason;
	}

	public void setReason(String reason) {
		this.reason = reason;
		save();
	}

	public String getImageURL() {
		return imageURL;
	}

	public void setImageURL(String imageURL) {
		this.imageURL = imageURL;
	}

	public boolean isBotAction() {
		return botAction;
	}

	public void setBotAction(boolean botAction) {
		this.botAction = botAction;
	}

	public AuditLogType getType() {
		return type;
	}

	public long getChannelID() {
		return channelID;
	}

	public void setChannelID(long channelID) {
		this.channelID = channelID;
	}

	public String getContent() {
		if (content == null) return "";
		if (content.length() > 1024) return content.substring(0, 1024);
		return content;
	}

	public void setContent(String content) {
		this.content = content;
	}

	public long getRelevantID() {
		return relevantID;
	}

	public void setRelevantID(long relevantID) {
		this.relevantID = relevantID;
	}

	public User getTriggererAsUser() {
		if (triggerUser == null) triggerUser = PocketBotMain.getInstance().getJDAInstance().getUserById(getTriggererID());
		return triggerUser;
	}

	public long getTriggererID() {
		return triggererID;
	}

	public void setTriggererID(long triggererID) {
		this.triggererID = triggererID;
	}

	public long getGuildID() {
		return guildID;
	}

	public void setGuildID(long guildID) {
		this.guildID = guildID;
	}

	public long getActionID() {
		return actionID;
	}

	public long getLogChannelMessageID() {
		return logChannelMessageID;
	}

	public void setLogChannelMessageID(long logChannelMessageID) {
		this.logChannelMessageID = logChannelMessageID;
	}

	public Message getMessage() {
		try {
			GuildConfiguration config = GuildConfiguration.getGuildConfiguration(getGuild());
			if (logChannelID == - 1) logChannelID = config.getChannel(ChannelOption.AUDIT_LOG_CHANNEL).getIdLong();
			if (message == null)
				message = guild.getTextChannelById(logChannelID).retrieveMessageById(getLogChannelMessageID()).complete();
		} catch (Exception ignored) {
		}
		return message;
	}

	public void setMessage(Message message) {
		this.message = message;
		setLogChannelMessageID(message.getIdLong());
	}

	public String buildTime() {
		Date date = new Date(time);
		DateFormat formatter = new SimpleDateFormat("HH:mm:ss.SSS");
		formatter.setTimeZone(TimeZone.getTimeZone("Australia/Sydney"));
		return formatter.format(date);
	}

	public EmbedBuilder appendPreviousEntries(EmbedBuilder messageBuilder) {
		StringBuilder previousEntryBuilder = new StringBuilder();
		GuildConfiguration config = GuildConfiguration.getGuildConfiguration(getGuild());

		config.getAllEntriesForRelevantID(getRelevantID()).forEachOrdered(sle -> {
					if (sle.getLogChannelMessageID() >= 0)
						previousEntryBuilder.append("[").append(sle.getType().getName()).append("  #").append(sle.getActionID())
								.append("](https://discordapp.com/channels/")
								.append(getGuildID()).append("/").append(config.getChannel(ChannelOption.AUDIT_LOG_CHANNEL)
								.getId()).append("/").append(sle.getLogChannelMessageID()).append(") \n");
				}
		);

		if (previousEntryBuilder.length() > 0)
			messageBuilder.addField("Previous Entries", previousEntryBuilder.toString(), false);

		return messageBuilder;
	}

	public void setId(String id) {
		this.id = id;
	}

	private EmbedBuilder buildBasicData(EmbedBuilder messageBuilder) {
		GuildConfiguration config = GuildConfiguration.getGuildConfiguration(getGuild());
		switch (type) {
			case BAN_USER:
				messageBuilder.setTitle("User Banned");
				messageBuilder.addField("User Banned \uD83D\uDED1", "<@" + getRelevantID() + "> has been banned. (ID:" + getRelevantID() + ")", true);
				break;
			case UNBAN_USER:
				messageBuilder.setTitle("User Unbanned");
				messageBuilder.addField("User Unbanned ✅", "<@" + getRelevantID() + "> has been unbanned. (ID:" + getRelevantID() + ")", true);
				break;


			case USER_JOIN:
				messageBuilder.setTitle("User Joined");
				messageBuilder.addField("User Joined", "<@" + getRelevantID() + "> has joined. (ID:" + getRelevantID() + ")", false);
				break;
			case USER_LEAVE:
				messageBuilder.setTitle("User Left");
				messageBuilder.addField("User left", "<@" + getRelevantID() + "> has left. (ID:" + getRelevantID() + ")", false);
				break;

			case EMOTE_ADD:
				messageBuilder.setTitle("Emote Added");
				Emote emote = getGuild().getEmoteById(getRelevantID());
				messageBuilder.addField("Emote Added", emote.getAsMention() + "  (Name: " + emote.getName() + ")", true);
				break;
			case EMOTE_MODIFY:
				messageBuilder.setTitle("Emote Modified");
				emote = getGuild().getEmoteById(getRelevantID());
				messageBuilder.addField("Emote Modified", emote.getAsMention() + "  (Name: " + emote.getName() + ")", true);
				break;
			case EMOTE_DELETE:
				messageBuilder.setTitle("Emote Delete");
				emote = getGuild().getEmoteById(getRelevantID());
				messageBuilder.addField("Emote Deleted",
						emote != null
								? emote.getAsMention() + "  (Name: " + emote.getName() + ")"
								: getRelevantID() + "", true);
				break;

			case MESSAGE_SEND:
				messageBuilder.setTitle("Message Sent");
				messageBuilder.addField("Message Content", getContent(), true);
				messageBuilder.addField("Sent by", getTriggererAsUser().getAsMention() + "  (" + getTriggererAsUser().getName() + "#" + getTriggererAsUser().getDiscriminator() + ")", true);
				break;
			case MESSAGE_EDIT:
				ServerLogEntry originalMessageEntry = config.getFirstEntryForRelevantID(getRelevantID(), AuditLogType.MESSAGE_SEND).orElse(null);
				messageBuilder.setTitle("Message Edited");
				if (originalMessageEntry != null)
					messageBuilder.addField("Original Message", originalMessageEntry.getContent(), true);
				else messageBuilder.addField("Original Message", "Unable to find the original message.", true);
				messageBuilder.addField("New Message Content", getContent(), true);
				break;
			case MESSAGE_DELETE:
				originalMessageEntry = config.getFirstEntryForRelevantID(getRelevantID(), AuditLogType.MESSAGE_SEND).orElse(null);
				messageBuilder.setTitle("Message Deleted");

				if (originalMessageEntry != null)
					messageBuilder.addField("Message Content", originalMessageEntry.getContent(), true);
				else
					messageBuilder.addField("Message Content", "Unknown. I don't have any memory of that message.", true);

				messageBuilder.addField("Message ID", getRelevantID() + "", true);
				messageBuilder.addField("Channel", "<#" + getChannelID() + ">", true);
				break;

			case CHANNEL_CREATE:
				messageBuilder.setTitle("New Channel Created");
				messageBuilder.addField("Channel", "<#" + getRelevantID() + ">", true);
				messageBuilder.addField("Created by", getTriggererAsUser().getAsMention() + "  (" + getTriggererAsUser().getName() + "#" + getTriggererAsUser().getDiscriminator() + ")", true);
				break;
			case CHANNEL_MODIFY:
				messageBuilder.setTitle("Channel Modified");
				messageBuilder.addField("Channel", "<#" + getRelevantID() + ">", true);

				break;
			case CHANNEL_DELETE:
				messageBuilder.setTitle("Channel Deleted");
				messageBuilder.addField("Channel", "<#" + getRelevantID() + ">", true);
				break;

			case VOICE_CHANNEL_CREATE:
				messageBuilder.setTitle("New Voice Channel Created");
				messageBuilder.addField("Voice Channel", "<#" + getRelevantID() + ">", true);
				messageBuilder.addField("Voice Channel ID", getChannelID() + "", true);
				break;
			case VOICE_CHANNEL_MODIFY:
				messageBuilder.setTitle("Voice Channel Modified");
				messageBuilder.addField("Voice Channel", "<#" + getRelevantID() + ">", true);
				messageBuilder.addField("Voice Channel ID", getChannelID() + "", true);

				break;
			case VOICE_CHANNEL_DELETE:
				messageBuilder.setTitle("Voice Channel Deleted");
				messageBuilder.addField("Voice Channel", "<#" + getRelevantID() + ">", true);
				messageBuilder.addField("Voice Channel ID", getChannelID() + "", true);
				break;
		}
		return messageBuilder;
	}

	public EmbedBuilder build() {
		return build(false, false);
	}

	public EmbedBuilder build(boolean showExtraInformation, boolean editOriginal) {
		GuildConfiguration config = GuildConfiguration.getGuildConfiguration(getGuild());
		ServerLogEntry initialEntry = config.getFirstEntryForRelevantID(getRelevantID()).orElse(null);
		TextChannel logChannel = config.getChannel(ChannelOption.AUDIT_LOG_CHANNEL);
		EmbedBuilder messageBuilder = MessageUtil.getDefaultBuilder();
		if (isBotAction()) return null;
		Message originalMessage = getMessage();
		boolean appendHistory = false;
		switch (type) {
			case BAN_USER:
				messageBuilder.setTitle("User Banned");
				messageBuilder.addField("User Banned \uD83D\uDED1", "<@" + getRelevantID() + "> has been banned. (ID:" + getRelevantID() + ")", true);
				messageBuilder.addField("Username", getContent(), true);
				messageBuilder.addBlankField(false);
				messageBuilder.addField("Banned by", getTriggererAsUser().getAsMention() + "  (" + getTriggererAsUser().getName() + "#" + getTriggererAsUser().getDiscriminator() + ")", true);
				appendHistory = true;
				break;
			case UNBAN_USER:
				messageBuilder.setTitle("User Unbanned");
				messageBuilder.addField("User Unbanned ✅", "<@" + getRelevantID() + "> has been unbanned. (ID:" + getRelevantID() + ")", true);
				messageBuilder.addField("Username", getContent(), true);
				messageBuilder.addBlankField(false);
				messageBuilder.addField("Unbanned by", getTriggererAsUser().getAsMention() + "  (" + getTriggererAsUser().getName() + "#" + getTriggererAsUser().getDiscriminator() + ")", true);
				appendHistory = true;
				break;
			case USER_JOIN:
				messageBuilder.setTitle("User Joined");
				messageBuilder.addField("User Joined", "<@" + getRelevantID() + "> has joined. (ID:" + getRelevantID() + ")", false);
				messageBuilder.addField("Username", getContent(), true);
				appendHistory = true;
				break;
			case USER_LEAVE:
				messageBuilder.setTitle("User Left");
				messageBuilder.addField("User left", "<@" + getRelevantID() + "> has left. (ID:" + getRelevantID() + ")", false);
				messageBuilder.addField("Username", getContent(), true);
				appendHistory = true;
				break;

			case EMOTE_ADD:
				messageBuilder.setTitle("Emote Added");
				Emote emote = getGuild().getEmoteById(getRelevantID());
				messageBuilder.addField("Emote Added", emote.getAsMention() + "  (Name: " + emote.getName() + ")", true);
				messageBuilder.addBlankField(false);
				messageBuilder.addField("Added by", getTriggererAsUser().getAsMention() + "  (" + getTriggererAsUser().getName() + "#" + getTriggererAsUser().getDiscriminator() + ")", true);
				break;
			case EMOTE_MODIFY:
				messageBuilder.setTitle("Emote Modified");
				emote = getGuild().getEmoteById(getRelevantID());
				messageBuilder.addField("Emote Modified", emote.getAsMention() + "  (Name: " + emote.getName() + ")", true);
				messageBuilder.addBlankField(false);
				messageBuilder.addField("Added by", getTriggererAsUser().getAsMention() + "  (" + getTriggererAsUser().getName() + "#" + getTriggererAsUser().getDiscriminator() + ")", true);
				appendHistory = true;
				break;
			case EMOTE_DELETE:
				messageBuilder.setTitle("Emote Delete");
				emote = getGuild().getEmoteById(getRelevantID());
				messageBuilder.addField("Emote Deleted",
						emote != null
								? emote.getAsMention() + "  (Name: " + emote.getName() + ")"
								: getRelevantID() + "", true);
				messageBuilder.addBlankField(false);
				messageBuilder.addField("Deleted by", getTriggererAsUser().getAsMention() + "  (" + getTriggererAsUser().getName() + "#" + getTriggererAsUser().getDiscriminator() + ")", true);
				appendHistory = true;
				break;

			case MESSAGE_SEND:
				messageBuilder.setTitle("Message Sent");
				messageBuilder.addField("Message Content", getContent(), true);
				User usr = getTriggererAsUser();
				if (usr != null) {
					messageBuilder.addField("Sent by", getTriggererAsUser().getAsMention() + "  (" + getTriggererAsUser().getName() + "#" + getTriggererAsUser().getDiscriminator() + ")", true);
				}else{
					messageBuilder.addField("Sent by", "<@" + getTriggererID() + ">", true);
				}
				messageBuilder.addBlankField(false);
				messageBuilder.addField("Message ID", getRelevantID() + "", true);
				messageBuilder.addField("Channel", "<#" + getChannelID() + ">", true);
				messageBuilder.addField("Goto Message", "[Click here](https://discordapp.com/channels/" + guildID + "/" + getChannelID() + "/" + getRelevantID() + ")", false);
				break;
			case MESSAGE_EDIT:
				ServerLogEntry originalMessageEntry = config.getFirstEntryForRelevantID(getRelevantID(), AuditLogType.MESSAGE_SEND).orElse(null);
				User messageOwner = originalMessageEntry.getTriggererAsUser();
				messageBuilder.setTitle("Message Edited");
				if (originalMessageEntry != null)
					messageBuilder.addField("Original Message", originalMessageEntry.getContent(), true);
				else messageBuilder.addField("Original Message", "Unable to find the original message.", true);
				messageBuilder.addField("New Message Content", getContent(), true);
				messageBuilder.addBlankField(false);
				messageBuilder.addField("Sent by", messageOwner.getAsMention() + "  (" + messageOwner.getName() + "#" + messageOwner.getDiscriminator() + ")", true);
				messageBuilder.addField("Message ID", getRelevantID() + "", true);
				messageBuilder.addField("Channel", "<#" + getChannelID() + ">", true);
				messageBuilder.addField("Goto Message", "[Click here](https://discordapp.com/channels/" + guildID + "/" + getChannelID() + "/" + getRelevantID() + ")", false);
				appendHistory = true;
				break;
			case MESSAGE_DELETE:
				originalMessageEntry = config.getFirstEntryForRelevantID(getRelevantID(), AuditLogType.MESSAGE_SEND).orElse(null);
				messageOwner = originalMessageEntry.getTriggererAsUser();
				messageBuilder.setTitle("Message Deleted");

				if (originalMessageEntry != null) messageBuilder.addField("Message Content", originalMessageEntry.getContent(), true);
				else messageBuilder.addField("Message Content", "Unknown. I don't have any memory of that message.", true);
				messageBuilder.addField("Message Sender", messageOwner.getAsMention() + "  (" + messageOwner.getName() + "#" + messageOwner.getDiscriminator() + ")", true);
				messageBuilder.addField("Message ID", getRelevantID() + "", false);
				messageBuilder.addField("Channel", "<#" + getChannelID() + ">", true);
				messageBuilder.addBlankField(false);
				/*if (getTriggererID() == - 1)
					setTriggererID(originalMessageEntry != null ? originalMessageEntry.getTriggererID() : - 1);
				if (getTriggererID() > - 1) {
					System.out.println(getTriggererID());
					User triggerUser = PocketBotMain.getInstance()
							.getJDAInstance()
							.getUserById(getTriggererID());
					if (triggerUser == null || triggerUser.isBot()) {
						setBotAction(true);
						return null;
					}
					messageBuilder.addField("Deleted By", getTriggererAsUser().getAsMention() + "  (" + getTriggererAsUser().getName() + "#" + getTriggererAsUser().getDiscriminator() + ")", true);
				} else messageBuilder.addField("Deleted By", "Unsure. Most likely the Author of the post.", true);*/
				appendHistory = true;
				break;

			case CHANNEL_CREATE:
				messageBuilder.setTitle("New Channel Created");
				messageBuilder.addField("Channel", "<#" + getRelevantID() + ">", true);
				messageBuilder.addField("Created by", getTriggererAsUser().getAsMention() + "  (" + getTriggererAsUser().getName() + "#" + getTriggererAsUser().getDiscriminator() + ")", true);
				break;
			case CHANNEL_MODIFY:
				messageBuilder.setTitle("Channel Modified");
				messageBuilder.addField("Channel", "<#" + getRelevantID() + ">", true);
				messageBuilder.addBlankField(false);

				messageBuilder.addField("Modified by", getTriggererAsUser().getAsMention() + "  (" + getTriggererAsUser().getName() + "#" + getTriggererAsUser().getDiscriminator() + ")", true);
				messageBuilder.addField("Changes", getContent() == null ? "-" : getContent().isEmpty() ? "-" : getContent(), true);

				break;
			case CHANNEL_DELETE:
				messageBuilder.setTitle("Channel Deleted");
				messageBuilder.addField("Channel", "<#" + getRelevantID() + ">", true);
				messageBuilder.addBlankField(false);
				messageBuilder.addField("Deleted by", getTriggererAsUser().getAsMention() + "  (" + getTriggererAsUser().getName() + "#" + getTriggererAsUser().getDiscriminator() + ")", true);
				break;

			case VOICE_CHANNEL_CREATE:
				messageBuilder.setTitle("New Voice Channel Created");
				messageBuilder.addField("Voice Channel", "<#" + getRelevantID() + ">", true);
				messageBuilder.addField("Voice Channel ID", getChannelID() + "", true);
				messageBuilder.addBlankField(false);

				messageBuilder.addField("Created by", getTriggererAsUser().getAsMention() + "  (" + getTriggererAsUser().getName() + "#" + getTriggererAsUser().getDiscriminator() + ")", true);
				break;
			case VOICE_CHANNEL_MODIFY:
				messageBuilder.setTitle("Voice Channel Modified");
				messageBuilder.addField("Voice Channel", "<#" + getRelevantID() + ">", true);
				messageBuilder.addField("Voice Channel ID", getChannelID() + "", true);
				messageBuilder.addBlankField(false);

				messageBuilder.addField("Modified by", getTriggererAsUser().getAsMention() + "  (" + getTriggererAsUser().getName() + "#" + getTriggererAsUser().getDiscriminator() + ")", false);
				messageBuilder.addField("Changes", getContent() == null ? "-" : getContent().isEmpty() ? "-" : getContent(), true);

				break;
			case VOICE_CHANNEL_DELETE:
				messageBuilder.setTitle("Voice Channel Deleted");
				messageBuilder.addField("Voice Channel", "<#" + getRelevantID() + ">", true);
				messageBuilder.addField("Voice Channel ID", getChannelID() + "", true);
				messageBuilder.addBlankField(false);

				if (getTriggererAsUser() != null)
					messageBuilder.addField("Deleted by", getTriggererAsUser().getAsMention() + "  (" + getTriggererAsUser().getName() + "#" + getTriggererAsUser().getDiscriminator() + ")", false);
				else
					messageBuilder.addField("Deleted by", "Nobody - Most likely an internal Discord thing", true);

				break;
			case GIVE_ROLE:
				messageBuilder.setTitle("Roles Given");
				messageBuilder.addField("User", "<@" + getRelevantID() + ">", true);
				messageBuilder.addField("Roles taken", getContent(), true);
				messageBuilder.addBlankField(false);
				if (getTriggererAsUser() != null)
					messageBuilder.addField("Given by", getTriggererAsUser().getAsMention() + "  (" + getTriggererAsUser().getName() + "#" + getTriggererAsUser().getDiscriminator() + ")", true);
				else
					messageBuilder.addField("Given by", "Nobody - Most likely an internal Discord thing", true);
				break;
			case REMOVE_ROLE:
				messageBuilder.setTitle("Roles Removed");
				messageBuilder.addField("User", "<@" + getRelevantID() + ">", true);
				messageBuilder.addField("Roles taken", getContent(), true);
				messageBuilder.addBlankField(false);
				if (getTriggererAsUser() != null)
					messageBuilder.addField("Removed by", getTriggererAsUser().getAsMention() + "  (" + getTriggererAsUser().getName() + "#" + getTriggererAsUser().getDiscriminator() + ")", true);
				else
					messageBuilder.addField("Removed by", "No User", true);
				break;
			default:
				throw new IllegalStateException("Unexpected value: " + type);
		}

		messageBuilder.addBlankField(false);

		if (initialEntry != null && logChannel != null && appendHistory)
			messageBuilder = appendPreviousEntries(messageBuilder);

		String reason = getReason();
		if (!reason.equalsIgnoreCase("xx no reason xx")) messageBuilder.addField("Reason", reason, true);

		messageBuilder.setTimestamp(new Date(time).toInstant());
		messageBuilder.setFooter("Pockety | Log Action #" + getActionID(), PocketBotMain.getInstance().getJDAInstance().getSelfUser().getAvatarUrl());

		if (getImageURL() != null) messageBuilder.setImage(getImageURL());

		messageBuilder.setColor(getType() != null ? getType().getDisplayColor() : MessageUtil.getColor());

		save();
		return messageBuilder;
	}


	public void save() {
		PocketBotMain.getInstance().getMongoConnection().storeObject(this);
	}

}
