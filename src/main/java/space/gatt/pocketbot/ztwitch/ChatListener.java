package space.gatt.pocketbot.ztwitch;

import com.github.philippheuer.events4j.simple.domain.EventSubscriber;
import com.github.twitch4j.chat.events.channel.*;
import com.github.twitch4j.chat.flag.FlagType;
import com.github.twitch4j.common.enums.CommandPermission;
import com.github.twitch4j.common.enums.SubscriptionPlan;
import com.github.twitch4j.common.enums.SubscriptionType;
import com.github.twitch4j.events.ChannelGoLiveEvent;
import com.github.twitch4j.events.ChannelGoOfflineEvent;
import com.github.twitch4j.eventsub.events.ChannelRaidEvent;
import com.github.twitch4j.pubsub.domain.ChatModerationAction;
import com.github.twitch4j.pubsub.domain.SubGiftData;
import com.github.twitch4j.pubsub.domain.SubscriptionData;
import com.github.twitch4j.pubsub.events.ChannelSubGiftEvent;
import com.github.twitch4j.pubsub.events.ChannelSubscribeEvent;
import com.github.twitch4j.pubsub.events.ChatModerationEvent;
import com.github.twitch4j.pubsub.events.HypeTrainApproachingEvent;
import com.github.twitch4j.pubsub.events.HypeTrainEndEvent;
import com.github.twitch4j.pubsub.events.HypeTrainStartEvent;
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
import space.gatt.pocketbot.utils.enums.ReactionEvent;

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
						e.getData().getFromAutomod() == null ? e.getData().getCreatedBy() : "AUTOMOD", "enabled R9K Beta Mode (Unique Messages Mode)", true);
				return;
			case R9K_BETA_OFF:
				handlePubSubMessage(e.getChannelId(), e.getData().getModerationAction().name().toUpperCase(),
						e.getData().getFromAutomod() == null ? e.getData().getCreatedBy() : "AUTOMOD", "disabled R9K Beta Mode (Unique Messages Mode)", true);
				return;
			case EMOTE_ONLY:
				handlePubSubMessage(e.getChannelId(), e.getData().getModerationAction().name().toUpperCase(),
						e.getData().getFromAutomod() == null ? e.getData().getCreatedBy() : "AUTOMOD", "enabled Emotes Only Mode :smile:", true);
				return;
			case EMOTE_ONLY_OFF:
				handlePubSubMessage(e.getChannelId(), e.getData().getModerationAction().name().toUpperCase(),
						e.getData().getFromAutomod() == null ? e.getData().getCreatedBy() : "AUTOMOD", "disabled Emotes Only Mode :frowning2:", true);
				return;

			case HOST:
				handlePubSubMessage(e.getChannelId(), e.getData().getModerationAction().name().toUpperCase(),
						e.getData().getFromAutomod() == null ? e.getData().getCreatedBy() : "AUTOMOD", "started hosting " + e.getData().getArgs().get(0), true);
				return;
			case UNHOST:
				handlePubSubMessage(e.getChannelId(), e.getData().getModerationAction().name().toUpperCase(),
						e.getData().getFromAutomod() == null ? e.getData().getCreatedBy() : "AUTOMOD", "stopped hosting", true);
				return;
			case SUBSCRIBERS:
				handlePubSubMessage(e.getChannelId(), e.getData().getModerationAction().name().toUpperCase(),
						e.getData().getFromAutomod() == null ? e.getData().getCreatedBy() : "AUTOMOD",
						"enabled Subscribers Only Mode", true);
				return;
			case SUBSCRIBERS_OFF:
				handlePubSubMessage(e.getChannelId(), e.getData().getModerationAction().name().toUpperCase(),
						e.getData().getFromAutomod() == null ? e.getData().getCreatedBy() : "AUTOMOD",
						"disabled Subscribers Only Mode", true);

			case SLOW:
				handlePubSubMessage(e.getChannelId(), e.getData().getModerationAction().name().toUpperCase(),
						e.getData().getFromAutomod() == null ? e.getData().getCreatedBy() : "AUTOMOD",
						"enabled Slow Mode for " + e.getData().getArgs() + " seconds", true);
				return;
			case SLOW_OFF:
				handlePubSubMessage(e.getChannelId(), e.getData().getModerationAction().name().toUpperCase(),
						e.getData().getFromAutomod() == null ? e.getData().getCreatedBy() : "AUTOMOD",
						"disabled Slow Mode", true);
				return;
			case FOLLOWERS:
				handlePubSubMessage(e.getChannelId(), e.getData().getModerationAction().name().toUpperCase(),
						e.getData().getFromAutomod() == null ? e.getData().getCreatedBy() : "AUTOMOD",
						"enabled Followers Mode (" + e.getData().getArgs() + " Minutes)", true);
				return;
			case FOLLOWERS_OFF:
				handlePubSubMessage(e.getChannelId(), e.getData().getModerationAction().name().toUpperCase(),
						e.getData().getFromAutomod() == null ? e.getData().getCreatedBy() : "AUTOMOD",
						"disabled Followers Only Mode", true);
				return;

			case DELETE:
				reason = e.getData().getArgs().size() > 1 ? e.getData().getArgs().get(1) : "";
				handlePubSubMessage(e.getChannelId(), e.getData().getArgs().get(0),
						e.getData().getFromAutomod() == null ? e.getData().getCreatedBy() : "AUTOMOD",
						"||" + (! reason.isEmpty() ? reason : "Unknown") + "|| :scissors: **deleted message**", true);
				return;
			case BAN:
				reason = e.getData().getArgs().size() > 1 ? e.getData().getArgs().get(1) : "";
				handlePubSubMessage(
					e.getChannelId(),
					e.getData().getArgs().get(0),
					e.getData().getFromAutomod() == null
							? e.getData().getCreatedBy() :
							"AUTOMOD", "**banned**" +
							(! reason.isEmpty() ? " for `" + reason + "`" : "") + " :hammer:",
					true);
				return;
			case UNBAN:
				handlePubSubMessage(e.getChannelId(), e.getData().getArgs().get(0), e.getData().getFromAutomod() == null ? e.getData().getCreatedBy() : "AUTOMOD", "**unbanned** :key: ", true);
				return;
			case TIMEOUT:
				reason = e.getData().getArgs().size() > 2 ? e.getData().getArgs().get(2) : "";
				handlePubSubMessage(e.getChannelId(), e.getData().getArgs().get(0), e.getData().getFromAutomod() == null ? e.getData().getCreatedBy() : "AUTOMOD", "**timed out**" + (! reason.isEmpty() ? " for `" + reason + "`" : "") + " for " + e.getData().getArgs().get(1) + " seconds :mute:", true);
				return;
			case UNTIMEOUT:
				handlePubSubMessage(e.getChannelId(), e.getData().getArgs().get(0), e.getData().getFromAutomod() == null ? e.getData().getCreatedBy() : "AUTOMOD", "**untimed out** :microphone2: ", true);
				return;

		}
	}

	private HashMap<String, Long> lastNotifyTime = new HashMap<>();
	private List<String> ignoreNext = new ArrayList<>();
	private List<String> ignoreStreamIDs = new ArrayList<>();
	private List<String> send = Arrays.asList("back_pocket", "jenz");

	private boolean doThank = true;
	private List<String> thankAfter = new ArrayList<>();
	@EventSubscriber
	public void onHypeTrainStart(HypeTrainApproachingEvent e){
		if (!e.getData().getChannelId().equalsIgnoreCase("485404757")) return;
		thankAfter = new ArrayList<>();
		doThank = false;
	}
	@EventSubscriber
	public void onHypeTrainEnd(HypeTrainEndEvent e){
		if (!e.getChannelId().equalsIgnoreCase("485404757")) return;
		if (thankAfter.size() > 0){
			String toThank = String.join(", ", thankAfter);
			PocketBotMain.getInstance().getTwitchClient().getChat().sendMessage("back_pocket", "/announce The Hype Train has ended! Thankyou to " + toThank + " for subscribing during the Hype Train! Sincerely, your doms. pocketyHEART");
		}
		thankAfter = new ArrayList<>();
		doThank = true;
	}

	@EventSubscriber
	public void onStreamEnd(ChannelGoOfflineEvent e){
		System.out.println(e.getChannel().getName() + " went offline");
		if (send.contains(e.getChannel().getName().toLowerCase())) PocketBotMain.getInstance().getTwitchClient().getChat().sendMessage(e.getChannel().getName(), "/emoteonly");
	}

	@EventSubscriber
	public void onStreamStart(ChannelGoLiveEvent e) {
		if (ignoreNext.contains(e.getChannel().getName().toLowerCase())){
			System.out.println("Ignoring Stream " + e.getChannel().getName() + "- been added to ignore next list.");
			ignoreNext.remove(e.getChannel().getName().toLowerCase());
			return;
		}
		if (send.contains(e.getStream().getUserName().toLowerCase())) PocketBotMain.getInstance().getTwitchClient().getChat().sendMessage(e.getStream().getUserName(), "/emoteonlyoff");
		String streamID = e.getStream().getId();
		if (ignoreStreamIDs.contains(streamID)) {
			System.out.println("Ignoring Stream with ID " + streamID + "- already cached.");
			return;
		}
		ignoreStreamIDs.add(streamID);
		System.out.println(e.getStream().getUserName() + " has gone live");
		System.out.println("Stream with ID " + streamID + "- has started. Adding to list to ignore");
		List<TwitchChannelWatcher> watchers = TwitchChannelWatcher.getWatchersForChannel(e.getChannel().getName().toLowerCase());
		GameCache game = GameCache.getGame(e.getStream().getGameId());


		if (send.contains(e.getStream().getUserName().toLowerCase())) PocketBotMain.getInstance().getTwitchClient().getChat().sendMessage(e.getStream().getUserName(), e.getStream().getUserName() + " has gone live - " + e.getStream().getTitle());
		if (watchers.size() == 0){
			System.out.println("Watchers for " + e.getStream().getUserName()  +" is 0. Reloading from db");
			TwitchChannelWatcher.reloadAll();
			watchers = TwitchChannelWatcher.getWatchersForChannel(e.getChannel().getName().toLowerCase());

		}
		if (watchers.size() > 0) {
			System.out.println("Watchers for " + e.getStream().getUserName()  +" is " + watchers.size());
			watchers.forEach(tcw -> {
				GuildConfiguration config = GuildConfiguration.getGuildConfiguration(tcw.getGuildID());

				System.out.println(tcw.getGuildID() + " is a valid guild for " + e.getStream().getUserName());
				if (tcw.isWatchingLogs()) {
					try {
						tcw.attemptTokenRefresh("Stream Started Refresh");
						PocketBotMain.getInstance().getTwitchClient().getChat().joinChannel(e.getChannel().getName());
					}catch (Exception ex){
					}
				}
				if (config.getChannel(ChannelOption.TWITCH_ANNOUNCE_CHANNEL) != null && tcw.isWatchingStreamStart()) {
					System.out.println("Attempting to notify " + tcw.getGuildID());
					Long lastNotifyTime = this.lastNotifyTime.getOrDefault(e.getChannel().getId(), 0L);
					Long timeDifference = System.currentTimeMillis() - lastNotifyTime;
					if (timeDifference > 1.8e+6 /* 30 minutes */){
						this.lastNotifyTime.put(e.getChannel().getId(), System.currentTimeMillis());
						EmbedBuilder announceBuilder = MessageUtil.getDefaultBuilder();
						announceBuilder.setTitle(e.getStream().getUserName() + " has gone live");
						String gameStr = game.getGameName();
						if (game.getGameId().equalsIgnoreCase("unknown"))
							gameStr = "Unknown ||[Game ID = " + e.getStream().getGameId() + "](https://twitchinsights.net/game/" + e.getStream().getGameId() + ")||";
						else
							gameStr = "[" + gameStr + "](https://twitchinsights.net/game/" + e.getStream().getGameId() + ")";
						announceBuilder.setDescription("**[" + e.getStream().getTitle() + "](https://twitch.tv/" + e.getStream().getUserName() + ")**");
						announceBuilder.addField("Category", "**" + gameStr + "**", true);
						announceBuilder.setImage("https://static-cdn.jtvnw.net/previews-ttv/live_user_" + e.getStream().getUserName().toLowerCase() + "-1280x720.jpg?r=" + UUID.randomUUID());
						announceBuilder.setThumbnail(game.getGameBoxArt());
						announceBuilder.setTimestamp(e.getStream().getStartedAtInstant());
						MessageBuilder finalBuilder = new MessageBuilder();
						finalBuilder.setEmbed(announceBuilder.build());
						if (tcw.getPingMessage() != null && !tcw.getPingMessage().isEmpty())
							finalBuilder.setContent(tcw.getPingMessage());

						config.getChannel(ChannelOption.TWITCH_ANNOUNCE_CHANNEL).sendMessage(finalBuilder.build()).queue();
					}else{
						System.out.println("Not notifying " + tcw.getGuildID() + "- hasn't been 30 minutes since last stream");
					}
				}
			});
		}
	}

	@EventSubscriber // delete message
	public void onIRCMessage(IRCMessageEvent event) {
		String msg = event.getMessage().orElse("nomsglmaooo");

		if (msg.equalsIgnoreCase("@pockety test") && event.getUserName().equalsIgnoreCase("gatt_au")){
			PocketBotMain.getInstance().getTwitchClient().getChat().sendMessage(event.getChannelName().get(), "I'm alive, and connected to chat.");
			return;
		}

		if (msg.toLowerCase().startsWith("@pockety ignorenext ") && event.getUserName().equalsIgnoreCase("gatt_au")){
			msg = msg.toLowerCase().replaceFirst("@pockety ignorenext ", "").toLowerCase();
			if (!ignoreNext.contains(msg)){
				ignoreNext.add(msg);
				PocketBotMain.getInstance().getTwitchClient().getChat().sendMessage(event.getChannelName().get(), "@" + event.getUserName() + "  Ok, I'll ignore @" + msg + "'s next Stream.");
			}else{
				ignoreNext.remove(msg);
				PocketBotMain.getInstance().getTwitchClient().getChat().sendMessage(event.getChannelName().get(), "@" + event.getUserName() + "  Ok, I won't ignore @" + msg + "'s next Stream.");
			}
			return;
		}

		if (msg.toLowerCase().startsWith("@pockety testsub ") && event.getUserName().equalsIgnoreCase("gatt_au")){
			msg = msg.toLowerCase().replaceFirst("@pockety testsub ", "").toLowerCase();
			String[] args = msg.split(" ");
			SubscriptionData fakeData = new SubscriptionData();
			fakeData.setChannelId(event.getChannelId());
			fakeData.setChannelName(event.getChannelName().orElse("back_pocket"));
			fakeData.setContext(SubscriptionType.fromString(args[0].toUpperCase()));
			fakeData.setSubPlan(SubscriptionPlan.valueOf(args[1].toUpperCase()));
			fakeData.setCumulativeMonths(Integer.parseInt(args[2]));
			fakeData.setStreakMonths(Integer.parseInt(args[3]));

			fakeData.setRecipientDisplayName(event.getUserName());
			fakeData.setRecipientUserName(event.getUserName());
			fakeData.setRecipientId(event.getUserId());

			fakeData.setUserId(event.getUserId());
			fakeData.setUserName(event.getUserName());
			fakeData.setIsGift(false);

			ChannelSubscribeEvent fakeEvent = new ChannelSubscribeEvent(fakeData);
			onSubscribe(fakeEvent);
			return;
		}
		if (msg.toLowerCase().startsWith("@pockety testgiftsub ") && event.getUserName().equalsIgnoreCase("gatt_au")){
			msg = msg.toLowerCase().replaceFirst("@pockety testgiftsub ", "").toLowerCase();
			String[] args = msg.split(" ");

			GiftSubscriptionsEvent fakeEvent = new GiftSubscriptionsEvent(event.getChannel(), event.getUser(), args[0].toUpperCase(), 1, 199);
			onGift(fakeEvent);
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

	private static HashMap<ReactionEvent, List<String>> responses = new HashMap<>();
	static {
		responses.put(ReactionEvent.SUBSCRIBE, Arrays.asList(
				"/me Thanks for subscribing for the first time @%user%! pocketyHEART pocketyGodlike",
				"/me Thanks for subscribing for the first time @%user%! You're stuck here forever! pocketyHEART ",
				"/me Thanks for subbing for the first time @%user%! Sincerely, your doms. pocketyHEART",
				"/me Thanks for subscribing for the first time @%user%! pocketyHEART pocketyGold",
				"/me Thanks for subscribing for the first time @%user%! You're stuck here forever! pocketyHEART pocketyGold",
				"/me Thanks for subbing for the first time @%user%! Sincerely, your doms. pocketyHEART pocketyGold"));
		responses.put(ReactionEvent.SUBSCRIBE_RECURRING, Arrays.asList(
				"/me Thanks for subscribing for %months% months, @%user%! pocketyHEART pocketyGodlike",
				"/me Thanks for subscribing for %months% months, @%user%! You're stuck here forever! pocketyHEART pocketyGodlike",
				"/me Thanks for subbing for %months% months, @%user%! Sincerely, your doms. pocketyHEART pocketyGodlike",
				"/me Thanks for subscribing for %months% months, @%user%! pocketyHEART pocketyGold",
				"/me Thanks for subscribing for %months% months, @%user%! You're stuck here forever! pocketyHEART pocketyGold",
				"/me Thanks for subbing for %months% months, @%user%! Sincerely, your doms. pocketyHEART pocketyGold"));

		responses.put(ReactionEvent.PRIME_SUBSCRIBE, Arrays.asList(
				"/me Thanks for subscribing with Twitch Prime, @%user%! pocketyHEART pocketyGold",
				"/me Thanks for subscribing with Twitch Prime, @%user%! You're stuck here forever! pocketyHEART pocketyGold",
				"/me Thanks for subbing with Twitch Prime, @%user%! Sincerely, your doms. pocketyHEART pocketyGold",
				"/me Thanks for subscribing with Twitch Prime, @%user%! pocketyHEART pocketyGodlike_HF",
				"/me Thanks for subscribing with Twitch Prime, @%user%! You're stuck here forever! pocketyHEART pocketyGodlike_HF",
				"/me Thanks for subbing with Twitch Prime, @%user%! Sincerely, your doms. pocketyHEART pocketyGodlike_HF"));
		responses.put(ReactionEvent.PRIME_SUBSCRIBE_RECURRING, Arrays.asList(
				"/me Thanks for subscribing with Twitch Prime for %months% months, @%user%! pocketyHEART pocketyGold",
				"/me Thanks for subscribing with Twitch Prime for %months% months, @%user%! You're stuck here forever! pocketyHEART pocketyGold",
				"/me Thanks for subbing with Twitch Prime for %months% months, @%user%! Sincerely, your doms. pocketyHEART pocketyGold",
				"/me Thanks for subscribing with Twitch Prime for %months% months, @%user%! pocketyHEART pocketyGodlike",
				"/me Thanks for subscribing with Twitch Prime for %months% months, @%user%! You're stuck here forever! pocketyHEART pocketyGodlike",
				"/me Thanks for subbing with Twitch Prime for %months% months, @%user%! Sincerely, your doms. pocketyHEART pocketyGodlike"));

		responses.put(ReactionEvent.GIFT_SUBS, Arrays.asList(
				"/me !!! Thanks for the %amount%x Tier %tier% Gift Subs @%user%! pocketyHEART pocketyGold",
				"/me !!! Thanks for the %amount%x Tier %tier% Gift Subs @%user%! Do you want a kiss? pocketyHEART pocketyGodlike"));
	}

	private String parseSubscribeEventMessage(SubscriptionData data, String message){
		message = message.replaceAll("%user%",
				data.getIsGift() ? data.getRecipientDisplayName() : data.getUserName());
		message = message.replaceAll("%giftuser%",
				data.getContext() == SubscriptionType.ANON_SUB_GIFT || data.getContext() == SubscriptionType.ANON_RESUB_GIFT
				? "Anonymous" :
				data.getUserName());
		message = message.replaceAll("%tier%",
				(data.getSubPlan().ordinal() - 1) + "");
		message = message.replaceAll("%months%",
				(data.getCumulativeMonths() + ""));
		message = message.replaceAll("%planname%",
				data.getSubPlanName());
		message = message.replaceAll("%streakmonths%",
				data.getStreakMonths()+"");
		return message;
	}

	private String parseGiftSubMessage(GiftSubscriptionsEvent data, String message){
		message = message.replaceAll("%user%", data.getUser().getName());
		message = message.replaceAll("%tier%", (data.getSubscriptionPlan().charAt(0)+""));
		message = message.replaceAll("%amount%", (data.getCount()) + "");
		message = message.replaceAll("%totalamount%", (data.getTotalCount()) + "");
		return message;
	}

	@EventSubscriber
	public void onRaid(RaidEvent event) {
		System.out.println("RAID : " + parseMessage(event.getChannel().getName(), event.getRaider().getName(), null, "raided with " + event.getViewers() + " viewers!", true));

		if (send.contains(event.getChannel().getName().toLowerCase()))
		PocketBotMain.getInstance().getTwitchClient().getChat().sendMessage(event.getChannel().getName(), "/me " + event.getRaider().getName() + " raided with " + event.getViewers() + " viewers!");
	}

	@EventSubscriber
	public void onSubscribe(ChannelSubscribeEvent event){
		System.out.println("New Subscription for " + event.getData().getChannelName());
		if (event.getData().getChannelId().equalsIgnoreCase("485404757")){
			if (!event.getData().getIsGift()){
				if (event.getData().getUserName().equalsIgnoreCase("gatt_au")){
					String message = parseSubscribeEventMessage(event.getData(), "/announce pocketyGodlike pocketyGodlike pocketyGodlike My creator %user% resubscribed for %months% months! pocketyGodlike pocketyGodlike pocketyGodlike");
					PocketBotMain.getInstance().getTwitchClient().getChat().sendMessage(event.getData().getChannelName(), message);
					return;
				}
				if (!doThank) {
					thankAfter.add(event.getData().getUserName());
					return;
				};
				String message;
				List<String> potentialMessages;
				if (event.getData().getSubPlan() == SubscriptionPlan.TWITCH_PRIME)
					potentialMessages = responses.get(event.getData().getContext() == SubscriptionType.RESUB ? ReactionEvent.PRIME_SUBSCRIBE_RECURRING : ReactionEvent.PRIME_SUBSCRIBE);
				else
					potentialMessages = responses.get(event.getData().getContext() == SubscriptionType.RESUB ? ReactionEvent.SUBSCRIBE_RECURRING : ReactionEvent.SUBSCRIBE);
				message = potentialMessages.get(new Random().nextInt(potentialMessages.size()));
				message = parseSubscribeEventMessage(event.getData(), message);
				PocketBotMain.getInstance().getTwitchClient().getChat().sendMessage(event.getData().getChannelName(), message);
			}
		}
	}
	@EventSubscriber
	public void onGift(GiftSubscriptionsEvent event){
		System.out.println("New Gift Sub to " + event.getChannel().getName() + " (" + event.getChannel().getId() + ")");
		if (event.getChannel().getName().equalsIgnoreCase("back_pocket")){
			String message = responses.get(ReactionEvent.GIFT_SUBS).get(new Random().nextInt(responses.get(ReactionEvent.GIFT_SUBS).size()));
			message = parseGiftSubMessage(event, message);
			PocketBotMain.getInstance().getTwitchClient().getChat().sendMessage("back_pocket", message);
		}
	}
}
