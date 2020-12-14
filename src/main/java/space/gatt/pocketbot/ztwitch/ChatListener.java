package space.gatt.pocketbot.ztwitch;

import com.github.philippheuer.events4j.simple.domain.EventSubscriber;
import com.github.twitch4j.chat.events.channel.*;
import com.github.twitch4j.events.ChannelGoLiveEvent;
import com.github.twitch4j.pubsub.events.ChatModerationEvent;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.MessageBuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.TextChannel;
import space.gatt.pocketbot.PocketBotMain;
import space.gatt.pocketbot.configs.GuildConfiguration;
import space.gatt.pocketbot.database.GameCache;
import space.gatt.pocketbot.utils.MessageUtil;
import space.gatt.pocketbot.utils.enums.ChannelOption;

import java.util.*;

public class ChatListener {


	private String parseMessage(String channel, String target, String triggerer, String context) {
		String message;
		if (target != null)
			message = "[ **#" + channel.toLowerCase() + "** ]  __" + target + "__ >> " + context + (triggerer != null ? " by " + triggerer : "");
		else
			message = "[ **#" + channel.toLowerCase() + "** ]  >> " + context + (triggerer != null ? " by " + triggerer : "");
		List<TwitchChannelWatcher> watchers = TwitchChannelWatcher.getWatchersForChannel(channel);
		if (watchers.size() > 0) {
			watchers.forEach(tcw -> {
				if (tcw.isConnectedToPubSub()) return;
				Guild guild = PocketBotMain.getInstance().getJDAInstance().getGuildById(tcw.getGuildID());
				if (guild == null) return;
				GuildConfiguration config = GuildConfiguration.getGuildConfiguration(guild);
				if (tcw.isWatchingLogs()) {
					TextChannel logChnl = config.getChannel(ChannelOption.TWITCH_LOG_CHANNEL);
					if (logChnl != null) logChnl.sendMessage(message).queue();
				}
			});
		}
		return message;
	}

	private String parseMessage(String channel, String target, String triggerer, String context, boolean ignorePubSubCheck) {
		if (! ignorePubSubCheck) return parseMessage(channel, target, triggerer, context);
		String message;
		if (target != null)
			message = "[ **#" + channel.toLowerCase() + "** ]  __" + target + "__ >> " + context + (triggerer != null ? " by " + triggerer : "");
		else
			message = "[ **#" + channel.toLowerCase() + "** ]  >> " + context + (triggerer != null ? " by " + triggerer : "");
		List<TwitchChannelWatcher> watchers = TwitchChannelWatcher.getWatchersForChannel(channel);
		if (watchers.size() > 0) {
			watchers.forEach(tcw -> {
				Guild guild = PocketBotMain.getInstance().getJDAInstance().getGuildById(tcw.getGuildID());
				if (guild == null) return;
				GuildConfiguration config = GuildConfiguration.getGuildConfiguration(guild);
				if (tcw.isWatchingLogs()) {
					TextChannel logChnl = config.getChannel(ChannelOption.TWITCH_LOG_CHANNEL);
					if (logChnl != null) logChnl.sendMessage(message).queue();
				}
			});
		}
		return message;
	}


	private String handlePubSubMessage(String channelID, String target, String triggerer, String context, boolean useChannelID) {
		if (! useChannelID) return parseMessage(channelID, target, triggerer, context);
		List<TwitchChannelWatcher> watchers = TwitchChannelWatcher.getWatchersForChannelFromId(channelID);
		if (watchers.size() == 0) return null;
		String channel = watchers.get(0).getChannelName();
		String message;
		message = (target != null) ?
				"[ **#" + channel.toLowerCase() + "** ]  __`" + target + "`__ » " + context + "" + (triggerer != null ? " by **`" + triggerer + "`**" : "") :
				"[ **#" + channel.toLowerCase() + "** ] » " + context + (triggerer != null ? " by **`" + triggerer + "`**" : "");

		for (TwitchChannelWatcher tcw : watchers){
			Guild guild = PocketBotMain.getInstance().getJDAInstance().getGuildById(tcw.getGuildID());
			if (guild != null) {
				GuildConfiguration config = GuildConfiguration.getGuildConfiguration(guild);
				if (tcw.isWatchingLogs()) {
					TextChannel logChnl = config.getChannel(ChannelOption.TWITCH_LOG_CHANNEL);
					if (logChnl != null)
						logChnl.sendMessage(new MessageBuilder().setContent(message).denyMentions(Message.MentionType.values()).build()).queue();
				}
			}else{
				System.out.println("Attempted to log to " + tcw.getGuildID() + " but returned value was Null");
			}
		}

		return message;
	}

	@EventSubscriber
	public void onModeration(ChatModerationEvent e) {
		//System.out.println(e.toString());
		String reason, action;
		if (e.getData().getModerationAction() == null) return;
		switch (e.getData().getModerationAction()) {

			case R9K_BETA:
				handlePubSubMessage(e.getChannelId(), e.getData().getModerationAction().name().toUpperCase(),
						! e.getData().getFromAutomod() ? e.getData().getCreatedBy() : "AUTOMOD", "enabled R9K Beta Mode (Unique Messages Mode)", true);
				return;
			case R9K_BETA_OFF:
				handlePubSubMessage(e.getChannelId(), e.getData().getModerationAction().name().toUpperCase(),
						! e.getData().getFromAutomod() ? e.getData().getCreatedBy() : "AUTOMOD", "disabled R9K Beta Mode (Unique Messages Mode)", true);
				return;
			case EMOTE_ONLY:
				handlePubSubMessage(e.getChannelId(), e.getData().getModerationAction().name().toUpperCase(),
						! e.getData().getFromAutomod() ? e.getData().getCreatedBy() : "AUTOMOD", "enabled Emotes Only Mode :smile:", true);
				return;
			case EMOTE_ONLY_OFF:
				handlePubSubMessage(e.getChannelId(), e.getData().getModerationAction().name().toUpperCase(),
						! e.getData().getFromAutomod() ? e.getData().getCreatedBy() : "AUTOMOD", "disabled Emotes Only Mode :frowning2:", true);
				return;

			case HOST:
				handlePubSubMessage(e.getChannelId(), e.getData().getModerationAction().name().toUpperCase(),
						! e.getData().getFromAutomod() ? e.getData().getCreatedBy() : "AUTOMOD", "started hosting " + e.getData().getArgs().get(0), true);
				return;
			case UNHOST:
				handlePubSubMessage(e.getChannelId(), e.getData().getModerationAction().name().toUpperCase(),
						! e.getData().getFromAutomod() ? e.getData().getCreatedBy() : "AUTOMOD", "stopped hosting", true);
				return;

			case SUBSCRIBERS:
				handlePubSubMessage(e.getChannelId(), e.getData().getModerationAction().name().toUpperCase(),
						! e.getData().getFromAutomod() ? e.getData().getCreatedBy() : "AUTOMOD",
						"enabled Subscribers Only Mode", true);
				return;
			case SUBSCRIBERS_OFF:
				handlePubSubMessage(e.getChannelId(), e.getData().getModerationAction().name().toUpperCase(),
						! e.getData().getFromAutomod() ? e.getData().getCreatedBy() : "AUTOMOD",
						"disabled Subscribers Only Mode", true);

			case SLOW:
				handlePubSubMessage(e.getChannelId(), e.getData().getModerationAction().name().toUpperCase(),
						! e.getData().getFromAutomod() ? e.getData().getCreatedBy() : "AUTOMOD",
						"enabled Slow Mode for " + e.getData().getArgs() + " seconds", true);
				return;
			case SLOW_OFF:
				handlePubSubMessage(e.getChannelId(), e.getData().getModerationAction().name().toUpperCase(),
						! e.getData().getFromAutomod() ? e.getData().getCreatedBy() : "AUTOMOD",
						"disabled Slow Mode", true);
				return;
			case FOLLOWERS:
				handlePubSubMessage(e.getChannelId(), e.getData().getModerationAction().name().toUpperCase(),
						! e.getData().getFromAutomod() ? e.getData().getCreatedBy() : "AUTOMOD",
						"enabled Followers Mode (" + e.getData().getArgs() + " Minutes)", true);
				return;
			case FOLLOWERS_OFF:
				handlePubSubMessage(e.getChannelId(), e.getData().getModerationAction().name().toUpperCase(),
						! e.getData().getFromAutomod() ? e.getData().getCreatedBy() : "AUTOMOD",
						"disabled Followers Only Mode", true);
				return;

			case DELETE:
				reason = e.getData().getArgs().size() > 1 ? e.getData().getArgs().get(1) : "";
				handlePubSubMessage(e.getChannelId(), e.getData().getArgs().get(0),
						! e.getData().getFromAutomod() ? e.getData().getCreatedBy() : "AUTOMOD",
						"||" + (! reason.isEmpty() ? reason : "Unknown") + "|| :scissors: **deleted message**", true);
				return;


			case BAN:
				reason = e.getData().getArgs().size() > 1 ? e.getData().getArgs().get(1) : "";
				handlePubSubMessage(e.getChannelId(), e.getData().getArgs().get(0), ! e.getData().getFromAutomod() ? e.getData().getCreatedBy() : "AUTOMOD", "**banned**" + (! reason.isEmpty() ? " for `" + reason + "`" : "") + " :hammer:", true);
				return;
			case UNBAN:
				handlePubSubMessage(e.getChannelId(), e.getData().getArgs().get(0), ! e.getData().getFromAutomod() ? e.getData().getCreatedBy() : "AUTOMOD", "**unbanned** :key: ", true);
				return;
			case TIMEOUT:
				reason = e.getData().getArgs().size() > 2 ? e.getData().getArgs().get(2) : "";
				handlePubSubMessage(e.getChannelId(), e.getData().getArgs().get(0), ! e.getData().getFromAutomod() ? e.getData().getCreatedBy() : "AUTOMOD", "**timed out**" + (! reason.isEmpty() ? " for `" + reason + "`" : "") + " for " + e.getData().getArgs().get(1) + " seconds :mute:", true);
				return;
			case UNTIMEOUT:
				handlePubSubMessage(e.getChannelId(), e.getData().getArgs().get(0), ! e.getData().getFromAutomod() ? e.getData().getCreatedBy() : "AUTOMOD", "**untimed out** :microphone2: ", true);
				return;

		}
	}

	private HashMap<String, Long> lastNotifyTime = new HashMap<>();

	@EventSubscriber
	public void onStreamStart(ChannelGoLiveEvent e) {
		List<TwitchChannelWatcher> watchers = TwitchChannelWatcher.getWatchersForChannel(e.getChannel().getName().toLowerCase());
		GameCache game = GameCache.getGame(e.getStream().getGameId());

		PocketBotMain.getInstance().getTwitchClient().getChat().sendMessage(e.getStream().getUserName(), e.getStream().getUserName() + " has gone live - " + e.getStream().getTitle());

		if (watchers.size() > 0) {
			watchers.forEach(tcw -> {
				Guild guild = PocketBotMain.getInstance().getJDAInstance().getGuildById(tcw.getGuildID());
				GuildConfiguration config = GuildConfiguration.getGuildConfiguration(guild);
				System.out.println(e.getStream().getUserName() + " has gone live");

				if (tcw.isWatchingLogs()) {
					try {
						tcw.attemptTokenRefresh("Stream Started Refresh");
						PocketBotMain.getInstance().getTwitchClient().getChat().joinChannel(e.getChannel().getName());
					}catch (Exception ex){
					}
				}
				if (config.getChannel(ChannelOption.TWITCH_ANNOUNCE_CHANNEL) != null && tcw.isWatchingStreamStart()) {
					Long lastNotifyTime = this.lastNotifyTime.getOrDefault(e.getChannel().getId(), 0L);
					Long timeDifference = System.currentTimeMillis() - lastNotifyTime;
					if (timeDifference > 180000 /* 3 minutes */){
						this.lastNotifyTime.put(e.getChannel().getId(), System.currentTimeMillis());
						EmbedBuilder announceBuilder = MessageUtil.getDefaultBuilder();
						announceBuilder.setTitle(e.getStream().getUserName() + " has gone live");

						String gameStr = game.getGameName();
						if (game.getGameId().equalsIgnoreCase("unknown"))
							gameStr = "Unknown ||[Game ID = " + e.getStream().getGameId() + "](https://twitchinsights.net/game/" + e.getStream().getGameId() + ")||";
						else
							gameStr = "[" + gameStr + "](https://twitchinsights.net/game/" + e.getStream().getGameId() + ")";
						announceBuilder.setDescription("[**" + e.getStream().getTitle() + "**](https://twitch.tv/" + e.getStream().getUserName() + ")");
						announceBuilder.addField("Category", "**" + gameStr + "**", true);
						announceBuilder.setImage("https://static-cdn.jtvnw.net/previews-ttv/live_user_" + e.getStream().getUserName().toLowerCase() + "-1280x720.jpg?r=" + UUID.randomUUID());
						announceBuilder.setThumbnail(game.getGameBoxArt());
						MessageBuilder finalBuilder = new MessageBuilder();
						finalBuilder.setEmbed(announceBuilder.build());
						if (tcw.getPingMessage() != null && !tcw.getPingMessage().isEmpty())
							finalBuilder.setContent(tcw.getPingMessage());

						config.getChannel(ChannelOption.TWITCH_ANNOUNCE_CHANNEL).sendMessage(finalBuilder.build()).queue();
					}
				}
			});
		}
	}

	@EventSubscriber // delete message
	public void onIRCMessage(IRCMessageEvent event) {

		if (event.getMessage().orElse("nomsglmaooo").equalsIgnoreCase("@pockety test") && event.getUserName().equalsIgnoreCase("gatt_au")){
			PocketBotMain.getInstance().getTwitchClient().getChat().sendMessage(event.getChannelName().get(), "I'm alive, and connected to chat.");
			return;
		}

		if (event.getCommandType().equalsIgnoreCase("join")) return;
		if (event.getCommandType().equalsIgnoreCase("part")) return;
		if (event.getCommandType().equalsIgnoreCase("hosttarget")) return;
		if (event.getCommandType().equalsIgnoreCase("userstate")) return;
		if (event.getCommandType().equalsIgnoreCase("roomstate")) return;
		if (event.getMessage().orElse("").contains(" hosting ")) return;
		if (event.getMessage().orElse("").toLowerCase().contains(" exiting host ")) return;
		//System.out.println(event.toString());

		switch (event.getCommandType().toLowerCase()) {
			case "clearmsg":
				if (event.getMessage().isPresent()) {
					System.out.println(parseMessage(event.getChannel().getName(), event.getUserName(), null, "`" + event.getMessage().orElse(null) + "` was deleted."));
				}
				break;
			case "notice":
				if (event.getMessage().isPresent()) {
					System.out.println(parseMessage(event.getChannel().getName(), "NOTICE", null, event.getMessage().orElse(null)));
				}
		}
	}

	@EventSubscriber
	public void onBan(UserBanEvent event) {
		System.out.println("CLASSIC : " + parseMessage(event.getChannel().getName(), event.getUser().getName(), null, "banned" + (! event.getReason().trim().isEmpty() ? " for " + event.getReason() : "") + " :hammer:"));
	}

	@EventSubscriber
	public void onTimeout(UserTimeoutEvent event) {
		System.out.println("CLASSIC : " + parseMessage(event.getChannel().getName(), event.getUser().getName(), null, "timed out for " + event.getDuration() + " seconds" + (! event.getReason().trim().isEmpty() ? " for " + event.getReason() : "") + " :mute: "));
	}

	@EventSubscriber
	public void onMod(ChannelModEvent event) {
		System.out.println("CLASSIC : " + parseMessage(event.getChannel().getName(), event.getUser().getName(), null, event.isMod() ? event.getUser().getName() + " has been added as a Moderator" : event.getUser().getName() + " has been removed as a Moderator"));
	}

	@EventSubscriber
	public void onClearChat(ClearChatEvent event) {
		System.out.println("CLASSIC : " + parseMessage(event.getChannel().getName(), null, null, "Chat has been cleared :broom:", true));
	}
}
