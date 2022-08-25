package space.gatt.pocketbot.listeners;

import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.audit.*;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.events.channel.text.TextChannelCreateEvent;
import net.dv8tion.jda.api.events.channel.text.TextChannelDeleteEvent;
import net.dv8tion.jda.api.events.channel.text.update.TextChannelUpdateNameEvent;
import net.dv8tion.jda.api.events.channel.text.update.TextChannelUpdatePositionEvent;
import net.dv8tion.jda.api.events.channel.text.update.TextChannelUpdateTopicEvent;
import net.dv8tion.jda.api.events.channel.voice.VoiceChannelCreateEvent;
import net.dv8tion.jda.api.events.channel.voice.VoiceChannelDeleteEvent;
import net.dv8tion.jda.api.events.channel.voice.update.VoiceChannelUpdateNameEvent;
import net.dv8tion.jda.api.events.channel.voice.update.VoiceChannelUpdatePositionEvent;
import net.dv8tion.jda.api.events.guild.GuildBanEvent;
import net.dv8tion.jda.api.events.guild.GuildUnbanEvent;
import net.dv8tion.jda.api.events.guild.member.GuildMemberJoinEvent;
import net.dv8tion.jda.api.events.guild.member.GuildMemberRemoveEvent;
import net.dv8tion.jda.api.events.guild.member.GuildMemberRoleAddEvent;
import net.dv8tion.jda.api.events.guild.member.GuildMemberRoleRemoveEvent;
import net.dv8tion.jda.api.events.guild.member.update.GuildMemberUpdateNicknameEvent;
import net.dv8tion.jda.api.events.message.guild.GuildMessageDeleteEvent;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;
import net.dv8tion.jda.api.events.message.guild.GuildMessageUpdateEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import space.gatt.pocketbot.configs.GuildConfiguration;
import space.gatt.pocketbot.utils.ServerLogEntry;
import space.gatt.pocketbot.utils.enums.AuditLogType;

import javax.annotation.Nonnull;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

public class AuditLogWatcher extends ListenerAdapter {

	private static Set<Long> ignoredIDs = new HashSet<>();

	public static Set<Long> getIgnoredIDs() {
		return ignoredIDs;
	}

	@Override
	public void onGuildMessageReceived(@Nonnull GuildMessageReceivedEvent event) {
		if (getIgnoredIDs().contains(event.getMessageIdLong())) return;
		GuildConfiguration configuration = GuildConfiguration.getGuildConfiguration(event.getGuild());
		ServerLogEntry logEntry = new ServerLogEntry(AuditLogType.MESSAGE_SEND, event.getGuild());
		logEntry.setContent(event.getMessage().getContentRaw());
		logEntry.setRelevantID(event.getMessageIdLong());
		logEntry.setTriggererID(event.getAuthor().getIdLong());
		logEntry.setChannelID(event.getMessage().getTextChannel().getIdLong());
		logEntry.setBotAction(event.getAuthor().isBot());
		logEntry.setReason("xx no reason xx");
		logEntry.setAuditLogSuccess(true);
		configuration.parseLogEntry(logEntry);
	}

	@Override
	public void onGuildMessageUpdate(@Nonnull GuildMessageUpdateEvent event) {
		if (getIgnoredIDs().contains(event.getMessageIdLong())) return;
		GuildConfiguration configuration = GuildConfiguration.getGuildConfiguration(event.getGuild());
		ServerLogEntry logEntry = new ServerLogEntry(AuditLogType.MESSAGE_EDIT, event.getGuild());
		logEntry.setContent(event.getMessage().getContentRaw());
		logEntry.setRelevantID(event.getMessageIdLong());
		logEntry.setTriggererID(event.getMember().getUser().getIdLong());
		logEntry.setChannelID(event.getMessage().getTextChannel().getIdLong());
		logEntry.setBotAction(event.getAuthor().isBot());
		logEntry.setReason("xx no reason xx");
		logEntry.setAuditLogSuccess(true);
		configuration.parseLogEntry(logEntry);
	}

	@Override
	public void onGuildMessageDelete(@Nonnull GuildMessageDeleteEvent event) {
		if (getIgnoredIDs().contains(event.getMessageIdLong())) return;
		OffsetDateTime now = OffsetDateTime.now();
		GuildConfiguration configuration = GuildConfiguration.getGuildConfiguration(event.getGuild());
		ServerLogEntry logEntry = new ServerLogEntry(AuditLogType.MESSAGE_DELETE, event.getGuild());
		logEntry.setRelevantID(event.getMessageIdLong());
		logEntry.setChannelID(event.getChannel().getIdLong());
		ServerLogEntry originalMessageEntry = configuration.getLastEntryForRelevantID(event.getMessageIdLong(), AuditLogType.MESSAGE_SEND).orElse(null);
		if (!event.getGuild().getSelfMember().hasPermission(Permission.VIEW_AUDIT_LOGS)) {
			logEntry.setAuditLogSuccess(false);
			configuration.parseLogEntry(logEntry);
			return;
		}
		event.getGuild().retrieveAuditLogs().type(ActionType.MESSAGE_DELETE).queue((s) -> {
			logEntry.setBotAction(originalMessageEntry != null && originalMessageEntry.isBotAction());
			/*if (originalMessageEntry != null) {
				if (s.size() > 0) {
					for (AuditLogEntry entry : s) {
						System.out.println(entry.getIdLong() + " @ " +  entry.getTimeCreated().toEpochSecond() + " - by " + entry.getTargetIdLong() +" vs " + entry.getUser().getId());
						if (ignoredIDs.contains(entry.getIdLong())) continue;

						if (entry.getType() == ActionType.MESSAGE_DELETE
								&& (now.toEpochSecond() - entry.getTimeCreated().toEpochSecond()) <= 1
								&& originalMessageEntry.getTriggererID() == entry.getTargetIdLong()) {
							ignoredIDs.add(entry.getIdLong());
							logEntry.setTriggererID(entry.getUser().getIdLong());
							logEntry.setReason(entry.getReason() != null ? entry.getReason() : null);
							//logEntry.setContent(entry.getReason() != null ? entry.getReason() : "No reason given. " + logEntry.getTriggererAsUser().getAsMention() + ", please use `_actionlog reason " + logEntry.getActionID() + " The Reason` to update the reason.");
							logEntry.setBotAction(entry.getUser().isBot());
							configuration.parseLogEntry(logEntry);
							return;
						}
					}
				}
			}
			logEntry.setContent("User deleted their own message");*/
			logEntry.setTriggererID(-1);
			logEntry.setAuditLogSuccess(true);
			configuration.parseLogEntry(logEntry);
		});
	}

	// Text Channels
	@Override
	public void onTextChannelDelete(@Nonnull TextChannelDeleteEvent event) {
		if (getIgnoredIDs().contains(event.getChannel().getIdLong())) return;
		GuildConfiguration configuration = GuildConfiguration.getGuildConfiguration(event.getGuild());
		ServerLogEntry logEntry = new ServerLogEntry(AuditLogType.CHANNEL_DELETE, event.getGuild());
		logEntry.setRelevantID(event.getChannel().getIdLong());
		logEntry.setContent(event.getChannel().getName());
		logEntry.setChannelID(event.getChannel().getIdLong());
		if (!event.getGuild().getSelfMember().hasPermission(Permission.VIEW_AUDIT_LOGS)) {
			logEntry.setAuditLogSuccess(false);
			configuration.parseLogEntry(logEntry);
			return;
		}
		event.getGuild().retrieveAuditLogs().type(ActionType.CHANNEL_DELETE).queue((s) -> {
			if (s.size() > 0) {
				for (AuditLogEntry entry : s) {
					if (entry.getTargetIdLong() == event.getChannel().getIdLong()) {
						logEntry.setTriggererID(entry.getUser().getIdLong());
						logEntry.setReason(entry.getReason() != null ? entry.getReason() : null);
						//logEntry.setContent(logEntry.getContent() + "\n" + (entry.getReason() != null ? entry.getReason() : "No reason given. " + logEntry.getTriggererAsUser().getAsMention() + ", please use `_actionlog reason " + logEntry.getActionID() + " The Reason` to update the reason."));
						logEntry.setBotAction(entry.getUser().isBot());
						logEntry.setAuditLogSuccess(true);
						configuration.parseLogEntry(logEntry);
						return;
					}
				}
			}
			logEntry.setAuditLogSuccess(false);
			configuration.parseLogEntry(logEntry);
		});
	}

	@Override
	public void onTextChannelUpdateName(@Nonnull TextChannelUpdateNameEvent event) {
		if (getIgnoredIDs().contains(event.getChannel().getIdLong())) return;
		GuildConfiguration configuration = GuildConfiguration.getGuildConfiguration(event.getGuild());
		ServerLogEntry logEntry = new ServerLogEntry(AuditLogType.CHANNEL_MODIFY, event.getGuild());
		logEntry.setRelevantID(event.getChannel().getIdLong());
		logEntry.setContent("**__New Name:__** " + event.getNewName() + "\n**__Old Name:__** " + event.getOldName());
		logEntry.setChannelID(event.getChannel().getIdLong());

		if (!event.getGuild().getSelfMember().hasPermission(Permission.VIEW_AUDIT_LOGS)) {
			logEntry.setAuditLogSuccess(false);
			configuration.parseLogEntry(logEntry);
			return;
		}
		event.getGuild().retrieveAuditLogs().type(ActionType.CHANNEL_UPDATE).queue((s) -> {
			if (s.size() > 0) {
				for (AuditLogEntry entry : s) {
					if (entry.getTargetIdLong() == event.getChannel().getIdLong()) {
						logEntry.setTriggererID(entry.getUser().getIdLong());
						logEntry.setReason(entry.getReason() != null ? entry.getReason() : null);
						//logEntry.setContent(logEntry.getContent() + "\n" + (entry.getReason() != null ? entry.getReason() : "No reason given. " + logEntry.getTriggererAsUser().getAsMention() + ", please use `_actionlog reason " + logEntry.getActionID() + " The Reason` to update the reason."));
						logEntry.setBotAction(entry.getUser().isBot());
						logEntry.setAuditLogSuccess(true);
						configuration.parseLogEntry(logEntry);
						return;
					}
				}
			}
			logEntry.setAuditLogSuccess(false);
			configuration.parseLogEntry(logEntry);
		});
	}

	@Override
	public void onTextChannelUpdateTopic(@Nonnull TextChannelUpdateTopicEvent event) {
		if (getIgnoredIDs().contains(event.getChannel().getIdLong())) return;
		GuildConfiguration configuration = GuildConfiguration.getGuildConfiguration(event.getGuild());
		ServerLogEntry logEntry = new ServerLogEntry(AuditLogType.CHANNEL_MODIFY, event.getGuild());
		logEntry.setRelevantID(event.getChannel().getIdLong());
		logEntry.setContent("**__New Topic:__** " + event.getNewTopic() + "\n**__Old Topic:__** " + event.getOldTopic());
		logEntry.setChannelID(event.getChannel().getIdLong());

		if (!event.getGuild().getSelfMember().hasPermission(Permission.VIEW_AUDIT_LOGS)) {
			logEntry.setAuditLogSuccess(false);
			configuration.parseLogEntry(logEntry);
			return;
		}
		event.getGuild().retrieveAuditLogs().type(ActionType.CHANNEL_UPDATE).queue((s) -> {
			if (s.size() > 0) {
				for (AuditLogEntry entry : s) {
					if (entry.getTargetIdLong() == event.getChannel().getIdLong()) {
						logEntry.setTriggererID(entry.getUser().getIdLong());
						logEntry.setReason(entry.getReason() != null ? entry.getReason() : null);
						//logEntry.setContent(logEntry.getContent() + "\n" + (entry.getReason() != null ? entry.getReason() : "No reason given. " + logEntry.getTriggererAsUser().getAsMention() + ", please use `_actionlog reason " + logEntry.getActionID() + " The Reason` to update the reason."));
						logEntry.setBotAction(entry.getUser().isBot());
						logEntry.setAuditLogSuccess(true);
						configuration.parseLogEntry(logEntry);
						return;
					}
				}
			}
			logEntry.setAuditLogSuccess(false);
			configuration.parseLogEntry(logEntry);
		});
	}

	@Override
	public void onTextChannelUpdatePosition(@Nonnull TextChannelUpdatePositionEvent event) {
//		if (getIgnoredIDs().contains(event.getChannel().getIdLong())) return;
//		GuildConfiguration configuration = GuildConfiguration.getGuildConfiguration(event.getGuild());
//		ServerLogEntry logEntry = new ServerLogEntry(AuditLogType.CHANNEL_MODIFY, event.getGuild());
//		logEntry.setRelevantID(event.getChannel().getIdLong());
//		logEntry.setContent("**__New Position:__** " + event.getNewPosition() + "\n**__Old Position:__** " + event.getOldPosition());
//		logEntry.setChannelID(event.getChannel().getIdLong());
//
//		if (!event.getGuild().getSelfMember().hasPermission(Permission.VIEW_AUDIT_LOGS)) {
//			logEntry.setAuditLogSuccess(false);
//			configuration.parseLogEntry(logEntry);
//			return;
//		}
//		event.getGuild().retrieveAuditLogs().type(ActionType.CHANNEL_UPDATE).queue((s) -> {
//			if (s.size() > 0) {
//				for (AuditLogEntry entry : s) {
//					if (entry.getTargetIdLong() == event.getChannel().getIdLong()) {
//						logEntry.setTriggererID(entry.getUser().getIdLong());
//						logEntry.setReason(entry.getReason() != null ? entry.getReason() : null);
//						//logEntry.setContent(logEntry.getContent() + "\n" + (entry.getReason() != null ? entry.getReason() : "No reason given. " + logEntry.getTriggererAsUser().getAsMention() + ", please use `_actionlog reason " + logEntry.getActionID() + " The Reason` to update the reason."));
//						logEntry.setBotAction(entry.getUser().isBot());
//						logEntry.setAuditLogSuccess(true);
//						configuration.parseLogEntry(logEntry);
//						return;
//					}
//				}
//			}
//			logEntry.setAuditLogSuccess(false);
//			configuration.parseLogEntry(logEntry);
//		});
	}

	@Override
	public void onTextChannelCreate(@Nonnull TextChannelCreateEvent event) {
		if (getIgnoredIDs().contains(event.getChannel().getIdLong())) return;
		GuildConfiguration configuration = GuildConfiguration.getGuildConfiguration(event.getGuild());
		ServerLogEntry logEntry = new ServerLogEntry(AuditLogType.CHANNEL_CREATE, event.getGuild());
		logEntry.setRelevantID(event.getChannel().getIdLong());
		logEntry.setChannelID(event.getChannel().getIdLong());
		logEntry.setContent("**Channel Name:** " + event.getChannel().getName());

		if (!event.getGuild().getSelfMember().hasPermission(Permission.VIEW_AUDIT_LOGS)) {
			logEntry.setAuditLogSuccess(false);
			configuration.parseLogEntry(logEntry);
			return;
		}
		event.getGuild().retrieveAuditLogs().type(ActionType.CHANNEL_CREATE).queue((s) -> {
			if (s.size() > 0) {
				for (AuditLogEntry entry : s) {
					if (entry.getTargetIdLong() == event.getChannel().getIdLong()) {
						logEntry.setTriggererID(entry.getUser().getIdLong());
						logEntry.setReason(entry.getReason() != null ? entry.getReason() : null);
						//logEntry.setContent(logEntry.getContent() + "\n" + (entry.getReason() != null ? entry.getReason() : "No reason given. " + logEntry.getTriggererAsUser().getAsMention() + ", please use `_actionlog reason " + logEntry.getActionID() + " The Reason` to update the reason."));
						logEntry.setBotAction(entry.getUser().isBot());
						logEntry.setAuditLogSuccess(true);
						configuration.parseLogEntry(logEntry);
						return;
					}
				}
			}
			logEntry.setAuditLogSuccess(false);
			configuration.parseLogEntry(logEntry);
		});
	}

	@Override
	public void onVoiceChannelDelete(@Nonnull VoiceChannelDeleteEvent event) {
		if (getIgnoredIDs().contains(event.getChannel().getIdLong())) return;
		GuildConfiguration configuration = GuildConfiguration.getGuildConfiguration(event.getGuild());
		ServerLogEntry logEntry = new ServerLogEntry(AuditLogType.VOICE_CHANNEL_DELETE, event.getGuild());
		logEntry.setRelevantID(event.getChannel().getIdLong());
		logEntry.setContent("**Channel Name:** " + event.getChannel().getName());
		logEntry.setChannelID(event.getChannel().getIdLong());
		if (!event.getGuild().getSelfMember().hasPermission(Permission.VIEW_AUDIT_LOGS)) {
			logEntry.setAuditLogSuccess(false);
			configuration.parseLogEntry(logEntry);
			return;
		}
		event.getGuild().retrieveAuditLogs().type(ActionType.CHANNEL_DELETE).queue((s) -> {
			if (s.size() > 0) {
				for (AuditLogEntry entry : s) {
					if (entry.getTargetIdLong() == event.getChannel().getIdLong()) {
						logEntry.setTriggererID(entry.getUser().getIdLong());
						logEntry.setReason(entry.getReason() != null ? entry.getReason() : null);
						//logEntry.setContent(logEntry.getContent() + "\n" + (entry.getReason() != null ? entry.getReason() : "No reason given. " + logEntry.getTriggererAsUser().getAsMention() + ", please use `_actionlog reason " + logEntry.getActionID() + " The Reason` to update the reason."));
						logEntry.setBotAction(entry.getUser().isBot());
						logEntry.setAuditLogSuccess(true);
						configuration.parseLogEntry(logEntry);
						return;
					}
				}
			}
			logEntry.setAuditLogSuccess(false);
			configuration.parseLogEntry(logEntry);
		});
	}

	@Override
	public void onVoiceChannelUpdateName(@Nonnull VoiceChannelUpdateNameEvent event) {
		if (getIgnoredIDs().contains(event.getChannel().getIdLong())) return;
		GuildConfiguration configuration = GuildConfiguration.getGuildConfiguration(event.getGuild());
		ServerLogEntry logEntry = new ServerLogEntry(AuditLogType.VOICE_CHANNEL_MODIFY, event.getGuild());
		logEntry.setRelevantID(event.getChannel().getIdLong());
		logEntry.setContent("**__New Name:__** " + event.getNewName() + "\n**__Old Name:__** " + event.getOldName());
		logEntry.setChannelID(event.getChannel().getIdLong());

		if (!event.getGuild().getSelfMember().hasPermission(Permission.VIEW_AUDIT_LOGS)) {
			logEntry.setAuditLogSuccess(false);
			configuration.parseLogEntry(logEntry);
			return;
		}
		event.getGuild().retrieveAuditLogs().type(ActionType.CHANNEL_UPDATE).queue((s) -> {
			if (s.size() > 0) {
				for (AuditLogEntry entry : s) {
					if (entry.getTargetIdLong() == event.getChannel().getIdLong()) {
						logEntry.setTriggererID(entry.getUser().getIdLong());
						logEntry.setReason(entry.getReason() != null ? entry.getReason() : null);
						//logEntry.setContent(logEntry.getContent() + "\n" + (entry.getReason() != null ? entry.getReason() : "No reason given. " + logEntry.getTriggererAsUser().getAsMention() + ", please use `_actionlog reason " + logEntry.getActionID() + " The Reason` to update the reason."));
						logEntry.setBotAction(entry.getUser().isBot());
						logEntry.setAuditLogSuccess(true);
						configuration.parseLogEntry(logEntry);
						return;
					}
				}
			}
			logEntry.setAuditLogSuccess(false);
			configuration.parseLogEntry(logEntry);
		});
	}
//
//	@Override
//	public void onVoiceChannelUpdatePosition(@Nonnull VoiceChannelUpdatePositionEvent event) {
//		if (getIgnoredIDs().contains(event.getChannel().getIdLong())) return;
//		GuildConfiguration configuration = GuildConfiguration.getGuildConfiguration(event.getGuild());
//		ServerLogEntry logEntry = new ServerLogEntry(AuditLogType.VOICE_CHANNEL_MODIFY, event.getGuild());
//		logEntry.setRelevantID(event.getChannel().getIdLong());
//		logEntry.setContent("**__New Position:__** " + event.getNewPosition() + "\n**__Old Position:__** " + event.getOldPosition());
//		logEntry.setChannelID(event.getChannel().getIdLong());
//
//		if (!event.getGuild().getSelfMember().hasPermission(Permission.VIEW_AUDIT_LOGS)) {
//			logEntry.setAuditLogSuccess(false);
//			configuration.parseLogEntry(logEntry);
//			return;
//		}
//		event.getGuild().retrieveAuditLogs().type(ActionType.CHANNEL_UPDATE).queue((s) -> {
//			if (s.size() > 0) {
//				for (AuditLogEntry entry : s) {
//					if (entry.getTargetIdLong() == event.getChannel().getIdLong()) {
//						logEntry.setTriggererID(entry.getUser().getIdLong());
//						logEntry.setReason(entry.getReason() != null ? entry.getReason() : null);
//						//logEntry.setContent(logEntry.getContent() + "\n" + (entry.getReason() != null ? entry.getReason() : "No reason given. " + logEntry.getTriggererAsUser().getAsMention() + ", please use `_actionlog reason " + logEntry.getActionID() + " The Reason` to update the reason."));
//						logEntry.setBotAction(entry.getUser().isBot());
//						logEntry.setAuditLogSuccess(true);
//						configuration.parseLogEntry(logEntry);
//						return;
//					}
//				}
//			}
//			logEntry.setAuditLogSuccess(false);
//			configuration.parseLogEntry(logEntry);
//		});
//	}

	@Override
	public void onVoiceChannelCreate(@Nonnull VoiceChannelCreateEvent event) {
		if (getIgnoredIDs().contains(event.getChannel().getIdLong())) return;
		GuildConfiguration configuration = GuildConfiguration.getGuildConfiguration(event.getGuild());
		ServerLogEntry logEntry = new ServerLogEntry(AuditLogType.CHANNEL_CREATE, event.getGuild());
		logEntry.setRelevantID(event.getChannel().getIdLong());
		logEntry.setChannelID(event.getChannel().getIdLong());
		logEntry.setContent("**Channel Name:** " + event.getChannel().getName());

		if (!event.getGuild().getSelfMember().hasPermission(Permission.VIEW_AUDIT_LOGS)) {
			logEntry.setAuditLogSuccess(false);
			configuration.parseLogEntry(logEntry);
			return;
		}
		event.getGuild().retrieveAuditLogs().type(ActionType.CHANNEL_CREATE).queue((s) -> {
			if (s.size() > 0) {
				for (AuditLogEntry entry : s) {
					if (entry.getTargetIdLong() == event.getChannel().getIdLong()) {
						logEntry.setTriggererID(entry.getUser().getIdLong());
						logEntry.setReason(entry.getReason() != null ? entry.getReason() : null);
						//logEntry.setContent(logEntry.getContent() + "\n" + (entry.getReason() != null ? entry.getReason() : "No reason given. " + logEntry.getTriggererAsUser().getAsMention() + ", please use `_actionlog reason " + logEntry.getActionID() + " The Reason` to update the reason."));
						logEntry.setBotAction(entry.getUser().isBot());
						logEntry.setAuditLogSuccess(true);
						configuration.parseLogEntry(logEntry);
						return;
					}
				}
			}
			logEntry.setAuditLogSuccess(false);
			configuration.parseLogEntry(logEntry);
		});
	}


	// Bans, Kicks and Unbans

	@Override
	public void onGuildBan(@Nonnull GuildBanEvent event) {
		GuildConfiguration configuration = GuildConfiguration.getGuildConfiguration(event.getGuild());
		ServerLogEntry logEntry = new ServerLogEntry(AuditLogType.BAN_USER, event.getGuild());
		logEntry.setRelevantID(event.getUser().getIdLong());
		logEntry.setContent(event.getUser().getName() + "#" + event.getUser().getDiscriminator());

		if (!event.getGuild().getSelfMember().hasPermission(Permission.VIEW_AUDIT_LOGS)) {
			logEntry.setAuditLogSuccess(false);
			configuration.parseLogEntry(logEntry);
			return;
		}
		event.getGuild().retrieveAuditLogs().type(ActionType.BAN).queue((s) -> {
			if (s.size() > 0) {
				for (AuditLogEntry entry : s) {
					if (entry.getTargetIdLong() == event.getUser().getIdLong()) {
						logEntry.setTriggererID(entry.getUser().getIdLong());
						logEntry.setReason(entry.getReason() != null ? entry.getReason() : null);
						//logEntry.setContent(entry.getReason() != null ? entry.getReason() : "No reason given. " + logEntry.getTriggererAsUser().getAsMention() + ", please use `_actionlog reason " + logEntry.getActionID() + " The Reason` to update the reason.");
						logEntry.setBotAction(entry.getUser().isBot());
						logEntry.setAuditLogSuccess(true);
						configuration.parseLogEntry(logEntry);
						return;
					}
				}
			}
			logEntry.setAuditLogSuccess(false);
			configuration.parseLogEntry(logEntry);
		});
	}

	@Override
	public void onGuildUnban(@Nonnull GuildUnbanEvent event) {
		GuildConfiguration configuration = GuildConfiguration.getGuildConfiguration(event.getGuild());
		ServerLogEntry logEntry = new ServerLogEntry(AuditLogType.UNBAN_USER, event.getGuild());
		logEntry.setRelevantID(event.getUser().getIdLong());
		logEntry.setContent(event.getUser().getName() + "#" + event.getUser().getDiscriminator());

		if (!event.getGuild().getSelfMember().hasPermission(Permission.VIEW_AUDIT_LOGS)) {
			logEntry.setAuditLogSuccess(false);
			configuration.parseLogEntry(logEntry);
			return;
		}
		event.getGuild().retrieveAuditLogs().type(ActionType.UNBAN).queue((s) -> {
			if (s.size() > 0) {
				for (AuditLogEntry entry : s) {
					if (entry.getTargetIdLong() == event.getUser().getIdLong()) {
						logEntry.setTriggererID(entry.getUser().getIdLong());
						logEntry.setReason(entry.getReason() != null ? entry.getReason() : null);
						//logEntry.setContent(entry.getReason() != null ? entry.getReason() : "No reason given. " + logEntry.getTriggererAsUser().getAsMention() + ", please use `_actionlog reason " + logEntry.getActionID() + " The Reason` to update the reason.");
						logEntry.setBotAction(entry.getUser().isBot());
						logEntry.setAuditLogSuccess(true);
						configuration.parseLogEntry(logEntry);
						return;
					}
				}
			}
			logEntry.setAuditLogSuccess(false);
			configuration.parseLogEntry(logEntry);
		});
	}

	@Override
	public void onGuildMemberJoin(@Nonnull GuildMemberJoinEvent event) {
		GuildConfiguration configuration = GuildConfiguration.getGuildConfiguration(event.getGuild());
		ServerLogEntry logEntry = new ServerLogEntry(AuditLogType.USER_JOIN, event.getGuild());
		logEntry.setRelevantID(event.getUser().getIdLong());
		logEntry.setContent(event.getUser().getName() + "#" + event.getUser().getDiscriminator());
		logEntry.setBotAction(event.getUser().isBot());
		logEntry.setImageURL(event.getUser().getAvatarUrl());
		logEntry.setReason("xx no reason xx");
		configuration.parseLogEntry(logEntry);
	}

	@Override
	public void onGuildMemberRemove(@Nonnull GuildMemberRemoveEvent event) {
		GuildConfiguration configuration = GuildConfiguration.getGuildConfiguration(event.getGuild());
		ServerLogEntry logEntry = new ServerLogEntry(AuditLogType.USER_LEAVE, event.getGuild());
		logEntry.setRelevantID(event.getUser().getIdLong());
		logEntry.setBotAction(event.getUser().isBot());
		logEntry.setContent(event.getUser().getName() + "#" + event.getUser().getDiscriminator());
		logEntry.setImageURL(event.getUser().getAvatarUrl());
		logEntry.setReason("xx no reason xx");
		logEntry.setAuditLogSuccess(true);
		configuration.parseLogEntry(logEntry);
	}

	@Override
	public void onGuildMemberRoleAdd(@Nonnull GuildMemberRoleAddEvent event) {
		GuildConfiguration configuration = GuildConfiguration.getGuildConfiguration(event.getGuild());
		ServerLogEntry logEntry = new ServerLogEntry(AuditLogType.GIVE_ROLE, event.getGuild());
		logEntry.setRelevantID(event.getUser().getIdLong());
		List<String> roleNames = new ArrayList<>();
		for (Role r : event.getRoles()) roleNames.add(r.getAsMention());
		logEntry.setContent("**Given Roles** " + String.join(", ", roleNames));
		if (!event.getGuild().getSelfMember().hasPermission(Permission.VIEW_AUDIT_LOGS)) {
			logEntry.setAuditLogSuccess(false);
			configuration.parseLogEntry(logEntry);
			return;
		}
		event.getGuild().retrieveAuditLogs().type(ActionType.MEMBER_ROLE_UPDATE).queue((s) -> {
			if (s.size() > 0) {
				for (AuditLogEntry entry : s) {
					if (entry.getTargetIdLong() == event.getUser().getIdLong() && entry.getChangeByKey(AuditLogKey.MEMBER_ROLES_ADD) != null) {
						logEntry.setTriggererID(entry.getUser().getIdLong());
						logEntry.setReason(entry.getReason() != null ? entry.getReason() : null);
						//logEntry.setContent(entry.getReason() != null ? entry.getReason() : "No reason given. " + logEntry.getTriggererAsUser().getAsMention() + ", please use `_actionlog reason " + logEntry.getActionID() + " The Reason` to update the reason.");
						logEntry.setBotAction(entry.getUser().isBot());
						logEntry.setAuditLogSuccess(true);
						configuration.parseLogEntry(logEntry);
						return;
					}
				}
			}
			logEntry.setAuditLogSuccess(false);
			configuration.parseLogEntry(logEntry);
		});
	}

	@Override
	public void onGuildMemberRoleRemove(@Nonnull GuildMemberRoleRemoveEvent event) {
		GuildConfiguration configuration = GuildConfiguration.getGuildConfiguration(event.getGuild());
		ServerLogEntry logEntry = new ServerLogEntry(AuditLogType.REMOVE_ROLE, event.getGuild());
		logEntry.setRelevantID(event.getUser().getIdLong());
		List<String> roleNames = new ArrayList<>();
		for (Role r : event.getRoles())
			roleNames.add(r.getAsMention());
		logEntry.setContent("**Taken Roles** " + String.join(", ", roleNames));
		if (!event.getGuild().getSelfMember().hasPermission(Permission.VIEW_AUDIT_LOGS)) {
			logEntry.setAuditLogSuccess(false);
			configuration.parseLogEntry(logEntry);
			return;
		}
		event.getGuild().retrieveAuditLogs().type(ActionType.MEMBER_ROLE_UPDATE).queue((s) -> {
			if (s.size() > 0) {
				for (AuditLogEntry entry : s) {
					if (entry.getTargetIdLong() == event.getUser().getIdLong() && entry.getChangeByKey(AuditLogKey.MEMBER_ROLES_REMOVE) != null) {
						logEntry.setTriggererID(entry.getUser().getIdLong());
						logEntry.setReason(entry.getReason() != null ? entry.getReason() : null);
						//logEntry.setContent(entry.getReason() != null ? entry.getReason() : "No reason given. " + logEntry.getTriggererAsUser().getAsMention() + ", please use `_actionlog reason " + logEntry.getActionID() + " The Reason` to update the reason.");
						logEntry.setBotAction(entry.getUser().isBot());
						logEntry.setAuditLogSuccess(true);
						configuration.parseLogEntry(logEntry);
						return;
					}
				}
			}
			logEntry.setAuditLogSuccess(false);
			configuration.parseLogEntry(logEntry);
		});
	}

	@Override
	public void onGuildMemberUpdateNickname(@Nonnull GuildMemberUpdateNicknameEvent event) {
		GuildConfiguration configuration = GuildConfiguration.getGuildConfiguration(event.getGuild());
	}
}
