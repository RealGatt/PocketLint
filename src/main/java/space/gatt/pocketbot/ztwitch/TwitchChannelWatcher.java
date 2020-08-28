package space.gatt.pocketbot.ztwitch;


import com.github.philippheuer.credentialmanager.domain.OAuth2Credential;
import com.github.twitch4j.pubsub.PubSubSubscription;
import com.netflix.hystrix.HystrixCommand;
import dev.morphia.annotations.Entity;
import dev.morphia.annotations.Id;
import dev.morphia.annotations.Transient;
import net.dv8tion.jda.api.entities.Guild;
import space.gatt.pocketbot.PocketBotMain;
import space.gatt.pocketbot.configs.GuildConfiguration;
import space.gatt.pocketbot.database.interfaces.MorphiaHelper;
import space.gatt.pocketbot.utils.MessageUtil;
import space.gatt.pocketbot.utils.enums.ChannelOption;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@MorphiaHelper(datastore = "pocketbot")
@Entity(value = "twitchchannels", noClassnameStored = true)
public class TwitchChannelWatcher {

	private static final HashMap<String, TwitchChannelWatcher> watchers = new HashMap<>();
	@Id
	private String channelName;
	private Long rolePingID, guildID;
	private String pingMessage;
	private boolean watchingLogs = false, watchingStreamStart = false;
	private String twitchChannelID = null;
	private String oauthToken = null;
	private transient PubSubSubscription moderationSubscription = null;
	@Transient
	private UUID instanceUUID = UUID.randomUUID();
	public TwitchChannelWatcher() {
		System.out.println(PocketBotMain.getInstance().getGsonInstance().toJson(this));
		PocketBotMain.getInstance().getScheduler().schedule(this::load, 5, TimeUnit.MILLISECONDS);
	}

	public TwitchChannelWatcher(String channelName, Guild guild) {
		this.channelName = channelName;
		this.guildID = guild.getIdLong();
		PocketBotMain.getInstance().getTwitchClient().getChat().joinChannel(channelName);
		//PocketBotMain.getInstance().getTwitchClient().getClientHelper().enableStreamEventListener(channelName.toLowerCase());
	}

	public static List<TwitchChannelWatcher> getWatchersForChannelFromId(String twitchChannelID) {
		return watchers.values().stream().filter(t -> t.getTwitchChannelID() != null && t.getTwitchChannelID().equalsIgnoreCase(twitchChannelID)).collect(Collectors.toList());
	}

	public static List<TwitchChannelWatcher> getWatchersForChannel(String channelName) {
		return watchers.values().stream().filter(t -> t.getChannelName().equalsIgnoreCase(channelName)).collect(Collectors.toList());
	}

	public static List<TwitchChannelWatcher> getWatchersForGuild(Guild guild) {
		return watchers.values().stream().filter(t -> t.getGuildID() == guild.getIdLong()).collect(Collectors.toList());
	}

	public static TwitchChannelWatcher getWatcher(String channelName, Guild guild) {
		UUID instance = UUID.randomUUID();
		//System.out.println(guild.getId() + " is requesting " + channelName + " [" + instance + "]");
		if (watchers.containsKey(channelName.toLowerCase() + "-" + guild.getId())) {
			//System.out.println(channelName.toLowerCase() + "-" + guild.getId() + " is in the hashmap [" + instance + "]");
			return watchers.get(channelName.toLowerCase() + "-" + guild.getId());
		}
		//System.out.println(channelName.toLowerCase() + "-" + guild.getId() + " is not in the hashmap [" + instance + "]");
		watchers.put(channelName.toLowerCase() + "-" + guild.getId(), new TwitchChannelWatcher(channelName.toLowerCase(), guild));
		return getWatcher(channelName.toLowerCase(), guild);
	}

	public static void loadAll() {
		watchers.values().forEach(TwitchChannelWatcher::load);
	}

	public String getTwitchChannelID() {
		return twitchChannelID != null ? twitchChannelID : "";
	}

	public void load() {
		if (channelName == null) {
			System.out.println("channelname is null");
			System.out.println(PocketBotMain.getInstance().getGsonInstance().toJson(this));
			return;
		}

		watchers.put(channelName.toLowerCase() + "-" + guildID, this);
		System.out.println("Storing " + channelName.toLowerCase() + "-" + guildID + " to HashMap");

		System.out.println("Loaded Watcher for Channel " + channelName.toLowerCase());
		PocketBotMain.getInstance().getTwitchClient().getChat().joinChannel(channelName);
		System.out.println("LOADED: \n" + PocketBotMain.getInstance().getGsonInstance().toJson(this));
		setupTwitch();
	}

	public void setupTwitch() {
		if (oauthToken != null) {
			if (twitchChannelID == null) {
				twitchChannelID = PocketBotMain.getInstance().getTwitchClient().getKraken()
						.getUsersByLogin(Collections.singletonList(channelName))
						.execute().getUsers().get(0).getId();
				System.out.println("Set Channel ID for " + channelName + " to " + twitchChannelID);
				save();
			}

			System.out.println("Setting up PubSub for (" + twitchChannelID + ") " + channelName + "-" + guildID + " using token " + oauthToken);
			try {
				OAuth2Credential oAuth2Credential = new OAuth2Credential("twitch", oauthToken);

				HystrixCommand flw = PocketBotMain.getInstance().getTwitchClient().getHelix().createFollow(oAuth2Credential.getAccessToken(),
						twitchChannelID, PocketBotMain.getInstance().getTwitchBotID(), false);
				flw.execute();

				if (flw.isFailedExecution()) throw new Throwable("Invalid oauth token");
				flw = PocketBotMain.getInstance().getTwitchClient().getHelix().deleteFollow(oAuth2Credential.getAccessToken(), twitchChannelID
						, PocketBotMain.getInstance().getTwitchBotID());
				flw.execute();


				PocketBotMain.getInstance().getTwitchClient().getClientHelper().enableStreamEventListener(channelName.toLowerCase());
				moderationSubscription = PocketBotMain.getInstance().getTwitchClient().getPubSub()
						.listenForModerationEvents(oAuth2Credential, twitchChannelID);
			} catch (Throwable e) {
				GuildConfiguration config = GuildConfiguration.getGuildConfiguration(guildID);
				config.getChannel(ChannelOption.TWITCH_LOG_CHANNEL).sendMessage(
						MessageUtil.getErrorBuilder("**Invalid oauth token stored for channel " + channelName + "**" +
								"\nRun `_twitch auth setup " + channelName + "` to resolve the issue.")
								.addField("Logger", "Moderation Logging will revert back to `basic` mode", true)
								.addField("Watcher", "The Bot can no longer see when the given channel goes online until the issue is resolved", true).build()).queue();
				moderationSubscription = null;
			}
		} else {
			System.out.println("No oAuth token set for " + channelName + "-" + guildID);
		}
	}

	public boolean isConnectedToPubSub() {
		return moderationSubscription != null;
	}

	public String getOAuthToken() {
		return oauthToken;
	}

	public void setOAuthToken(String oauthToken) {
		this.oauthToken = oauthToken;
	}

	public Long getGuildID() {
		return guildID;
	}

	public void setGuildID(Long guildID) {
		this.guildID = guildID;
	}

	public boolean isWatchingLogs() {
		return watchingLogs;
	}

	public void setWatchingLogs(boolean watchingLogs) {
		this.watchingLogs = watchingLogs;
	}

	public boolean isWatchingStreamStart() {
		return watchingStreamStart;
	}

	public void setWatchingStreamStart(boolean watchingStreamStart) {
		this.watchingStreamStart = watchingStreamStart;
	}

	public boolean isActive() {
		return isWatchingLogs() || isWatchingStreamStart();
	}

	public String getChannelName() {
		return channelName;
	}

	public void setChannelName(String channelName) {
		this.channelName = channelName;
	}

	public Long getRolePingID() {
		return rolePingID;
	}

	public void setRolePingID(Long rolePingID) {
		this.rolePingID = rolePingID;
	}

	public String getPingMessage() {
		return pingMessage;
	}

	public void setPingMessage(String pingMessage) {
		this.pingMessage = pingMessage;
	}

	public void save() {
		PocketBotMain.getInstance().getMongoConnection().storeObject(this);
	}
}
