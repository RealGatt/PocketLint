package space.gatt.pocketbot.ztwitch;


import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.philippheuer.credentialmanager.domain.OAuth2Credential;
import com.github.twitch4j.helix.domain.UserList;
import com.github.twitch4j.pubsub.PubSubSubscription;
import com.netflix.hystrix.HystrixCommand;
import dev.morphia.annotations.Entity;
import dev.morphia.annotations.Id;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.User;
import okhttp3.*;
import space.gatt.pocketbot.PocketBotMain;
import space.gatt.pocketbot.configs.GuildConfiguration;
import space.gatt.pocketbot.database.interfaces.MorphiaHelper;
import space.gatt.pocketbot.utils.MessageUtil;
import space.gatt.pocketbot.utils.enums.ChannelOption;
import space.gatt.pocketbot.utils.enums.ReactionEvent;

import java.util.*;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@MorphiaHelper(datastore = "pocketbot")
@Entity(value = "twitchchannels", useDiscriminator = false)
public class TwitchChannelWatcher {

	private static final HashMap<String, TwitchChannelWatcher> watchers = new HashMap<>();
	@Id
	private String _id;

	private String channelName;
	private Long rolePingID, guildID;
	private String pingMessage = null;
	private boolean watchingLogs = false, watchingStreamStart = false;
	private String twitchChannelID = null;
	private String oauthToken = null, refreshToken = null, authorizationToken = null;

	private String linkingUserID = null;
	private HashMap<ReactionEvent, String> eventMessages = new HashMap<>();

	private transient List<PubSubSubscription> subscriptions = new ArrayList<>();
	private transient OAuth2Credential tokenCredentials = null;
	private transient boolean tokenSetup = false;

	public OAuth2Credential getTokenCredentials() {
		return tokenCredentials;
	}

	public TwitchChannelWatcher() {
		PocketBotMain.getInstance().getScheduler().schedule(this::load, 5, TimeUnit.MILLISECONDS);
	}

	public TwitchChannelWatcher(String channelName, Guild guild) {
		this.channelName = channelName;
		this.guildID = guild.getIdLong();
		this._id = channelName + "-" + guildID;
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

	public static void reloadAll(){
		System.out.println("RELOADING ALL WATCHERS");
		System.out.println("RELOADING ALL WATCHERS");
		System.out.println("RELOADING ALL WATCHERS");
		watchers.forEach((str, watcher)->{
			if (watcher.scheduled != null) watcher.scheduled.cancel(true);
			watcher.scheduled = null;
		});
		watchers.clear();
		PocketBotMain.getInstance().getMongoConnection().getMultipleObjects(10000, TwitchChannelWatcher.class);
		loadAll();
	}
	public static void loadAll() {
		watchers.values().forEach(TwitchChannelWatcher::load);
	}

	public String getTwitchChannelID() {
		return twitchChannelID != null ? twitchChannelID : "";
	}


	public HashMap<ReactionEvent, String> getEventMessages() {
		if (eventMessages == null) eventMessages = new HashMap<>();
		return eventMessages;
	}

	public boolean isCorrectLinkingUser(User usr){
		return usr.getId().equalsIgnoreCase("113462564217683968") || usr.getId().equalsIgnoreCase(getLinkingUserID());
	}

	public String getLinkingUserID() {
		return linkingUserID != null ? linkingUserID : "";
	}

	public void setLinkingUserID(String linkingUserID) {
		this.linkingUserID = linkingUserID;
	}

	private transient boolean loaded = false;

	public void load() {
		if (loaded) return;
		if (channelName == null) {
			System.out.println("channelname is null");
			System.out.println(PocketBotMain.getInstance().getGsonInstance().toJson(this));
			return;
		}

		if (PocketBotMain.getInstance().getJDAInstance().getGuildById(getGuildID()) == null) {
			System.out.println("Guild for " + _id + " is unavailable. (" + getGuildID() + ")");
		}

		watchers.put(channelName.toLowerCase() + "-" + guildID, this);
		System.out.println("Storing " + channelName.toLowerCase() + "-" + guildID + " to HashMap");

		System.out.println("Loaded Watcher for Channel " + channelName.toLowerCase());
		PocketBotMain.getInstance().getTwitchClient().getChat().joinChannel(channelName);

		loaded = true;

		System.out.println(PocketBotMain.getInstance().getGsonInstance().toJson(this));
		setupTwitch();
	}

	private Optional<OAuth2Credential> getRefreshToken(String code){
		OkHttpClient client = new OkHttpClient();
		try {
			// api call
			RequestBody formBody = new FormBody.Builder()
					.add("client_id", PocketBotMain.getInstance().getTwitchConfiguration().getTwitchClientID())
					.add("client_secret", PocketBotMain.getInstance().getTwitchConfiguration().getTwitchClientSecret())
					.add("code", code)
					.add("grant_type", "authorization_code")
					.add("redirect_uri", PocketBotMain.getInstance().getTwitchConfiguration().getTwitchClientRedirect())
					.build();

			Request request = new Request.Builder()
					.url("https://id.twitch.tv/oauth2/token")
					.post(formBody)
					.build();

			Response response = client.newCall(request).execute();
			String responseBody = response.body().string();

			// parse response
			if (response.isSuccessful()) {
				ObjectMapper objectMapper = new ObjectMapper();
				HashMap<String, Object> tokenInfo = objectMapper.readValue(responseBody, new TypeReference<HashMap<String, Object>>() {});
				String refreshToken = (String) tokenInfo.get("refresh_token");
				String accessToken = (String) tokenInfo.get("access_token");
				List<String> scopes = (List<String>) tokenInfo.get("scopes");
				int expiresIn = (int) tokenInfo.get("expires_in");

				// create credential instance
				OAuth2Credential newCredential = new OAuth2Credential("twitch",
						accessToken,
						refreshToken,
						twitchChannelID, channelName, expiresIn, scopes);

				// inject credential context
				newCredential.getContext().put("client_id", (String) tokenInfo.get("client_id"));
				authorizationToken = code;

				return Optional.of(newCredential);
			} else {
				throw new RuntimeException("Request Failed! Code: " + response.code() + " - " + responseBody);
			}

		} catch (Exception ex) {
			ex.printStackTrace();
			// ignore, invalid token
		} finally {
			client.dispatcher().executorService().shutdown();
			client.connectionPool().evictAll();

			if (client.cache() != null && !client.cache().isClosed()) {
				try {
					client.cache().close();
				} catch (Exception ex) {
				}
			}
		}
		return Optional.empty();
	}

	private Optional<OAuth2Credential> refreshToken(){
		OkHttpClient client = new OkHttpClient();
		try {
			// api call
			RequestBody formBody = new FormBody.Builder()
					.add("client_id", PocketBotMain.getInstance().getTwitchConfiguration().getTwitchClientID())
					.add("client_secret", PocketBotMain.getInstance().getTwitchConfiguration().getTwitchClientSecret())
					.add("refresh_token", refreshToken)
					.add("grant_type", "refresh_token")
					.add("redirect_uri", PocketBotMain.getInstance().getTwitchConfiguration().getTwitchClientRedirect())
					.build();

			Request request = new Request.Builder()
					.url("https://id.twitch.tv/oauth2/token")
					.post(formBody)
					.build();

			Response response = client.newCall(request).execute();
			String responseBody = response.body().string();

			// parse response
			if (response.isSuccessful()) {
				ObjectMapper objectMapper = new ObjectMapper();
				HashMap<String, Object> tokenInfo = objectMapper.readValue(responseBody, new TypeReference<HashMap<String, Object>>() {});
				String refreshToken = (String) tokenInfo.get("refresh_token");
				String accessToken = (String) tokenInfo.get("access_token");
				List<String> scopes = (List<String>) tokenInfo.get("scopes");
				int expiresIn = (int) tokenInfo.get("expires_in");

				// create credential instance
				OAuth2Credential newCredential = new OAuth2Credential("twitch",
						accessToken,
						refreshToken,
						twitchChannelID, channelName, expiresIn, scopes);

				// inject credential context
				newCredential.getContext().put("client_id", (String) tokenInfo.get("client_id"));

				return Optional.of(newCredential);
			} else {
				throw new RuntimeException("Request Failed! Code: " + response.code() + " - " + responseBody);
			}

		} catch (Exception ex) {
			ex.printStackTrace();
			// ignore, invalid token
		} finally {
			client.dispatcher().executorService().shutdown();
			client.connectionPool().evictAll();

			if (client.cache() != null && !client.cache().isClosed()) {
				try {
					client.cache().close();
				} catch (Exception ex) {
				}
			}
		}
		return Optional.empty();
	}

	public boolean acceptCode(String twitchCode){
		try {
			OAuth2Credential refreshToken = getRefreshToken(twitchCode).orElse(null);
			if (refreshToken == null) {
				System.out.println("Refresh Token was null");
				sendInvalidTokenMessage();
			} else {
				this.refreshToken = refreshToken.getRefreshToken();
				this.oauthToken = refreshToken.getAccessToken();
				tokenCredentials = refreshToken;
				tokenSetup = true;
				save();
				setupTwitch();
				return true;
			}
		}catch (Exception e){
			e.printStackTrace();
			return false;
		}

		return false;
	}

	public void sendInvalidTokenMessage(){
		if (!isWatchingLogs()) return;
		GuildConfiguration config = GuildConfiguration.getGuildConfiguration(guildID);
		config.getChannel(ChannelOption.TWITCH_LOG_CHANNEL).sendMessage(
				MessageUtil.getErrorBuilder("**Invalid oauth token stored for channel " + channelName + "**" +
						"\nRun `_twitch auth setup " + channelName + "` to resolve the issue.")
						.addField("Logger", "Moderation Logging will revert back to `basic` mode", true)
						.addField("Watcher", "The Bot can no longer see when the given channel goes online until the issue is resolved", true).build()).queue();

		tokenSetup = false;

		/*moderationSubscription = null;
		this.refreshToken = null;
		this.oauthToken = null;
		this.authorizationToken = null;*/
		save();
	}
	public boolean attemptTokenRefresh(){
		return attemptTokenRefresh("Unknown Reason");
	}

	private transient long lastRefreshTime = System.currentTimeMillis() - (1000*61);

	public boolean attemptTokenRefresh(String reason){

		if (authorizationToken == null) {
			sendInvalidTokenMessage();
			return false;
		}

		if (System.currentTimeMillis() - lastRefreshTime < 1000*60) {
			System.out.println("Skipping Token Refresh for " + channelName + ", it has been less than a minute since the last one.    (REASON: " + reason + ")");
			return true;
		}


		OAuth2Credential refreshedCredentials = refreshToken().orElse(null);
		if (refreshedCredentials == null){
			sendInvalidTokenMessage();
			return false;
		}
		this.refreshToken = refreshedCredentials.getRefreshToken();
		this.oauthToken = refreshedCredentials.getAccessToken();
		tokenCredentials = refreshedCredentials;
		tokenSetup = true;
		lastRefreshTime = System.currentTimeMillis();

		save();

		return true;
	}

	public boolean isTokenSetup() {
		return tokenSetup;
	}

	private transient ScheduledFuture<?> scheduled = null;

	public void setupTwitch() {

		if (twitchChannelID == null) {
			UserList uList = PocketBotMain.getInstance().getTwitchClient().getHelix().getUsers(PocketBotMain.getInstance().getTwitchCredentials().getAccessToken(),
					null, Collections.singletonList(channelName)).execute();
			if (uList.getUsers().size() > 0)
				twitchChannelID = uList.getUsers().get(0).getId();
			System.out.println("Set Channel ID for " + channelName + " to " + twitchChannelID);
			save();
		}

		if (isWatchingStreamStart())
			PocketBotMain.getInstance().getTwitchClient().getClientHelper().enableStreamEventListener(twitchChannelID, channelName.toLowerCase());

		boolean tokenRefreshed = attemptTokenRefresh("Bot Booted");
		if (!tokenRefreshed) System.out.println("TOKEN FOR " + channelName + " WAS NOT REFRESHED. (guild: " + getGuildID() + ")");
		if (scheduled == null)
			scheduled = PocketBotMain.getInstance().getScheduler().scheduleAtFixedRate(()->attemptTokenRefresh("Three Hourly Reset"), 1, 3*60, TimeUnit.MINUTES);


		if (oauthToken != null) {

			System.out.println("Setting up PubSub for (" + twitchChannelID + ") " + channelName + "-" + guildID);// + " using token " + oauthToken);

			try {

				HystrixCommand flw = PocketBotMain.getInstance().getTwitchClient().getHelix()
						.getFollowers(tokenCredentials.getAccessToken(), twitchChannelID, null, null, 2); //getFollowedStreams(tokenCredentials.getAccessToken(),
								//"116516928", "chat", "other");
				flw.execute();

				if (flw.isFailedExecution()) throw new Throwable("Invalid oauth token");


//				PocketBotMain.getInstance().getScheduler().schedule(()->{
//
//
//					HystrixCommand flw2 = PocketBotMain.getInstance().getTwitchClient().getHelix()
//							.unblockUser(tokenCredentials.getAccessToken(), "116516928");
//					flw2.execute();
//
//				}, 1, TimeUnit.SECONDS);

				PocketBotMain.getInstance().getScheduler().scheduleAtFixedRate(()->{

					PocketBotMain.getInstance().getScheduler().schedule(()->{
						subscriptions.add(PocketBotMain.getInstance().getTwitchClient().getPubSub()
								.listenForModerationEvents(getTokenCredentials(),
										twitchChannelID,
										twitchChannelID));
						subscriptions.add(PocketBotMain.getInstance().getTwitchClient().getPubSub()
								.listenForSubscriptionEvents(getTokenCredentials(),
										twitchChannelID));
						subscriptions.add(PocketBotMain.getInstance().getTwitchClient().getPubSub()
								.listenForRaidEvents(getTokenCredentials(),
										twitchChannelID));

						if (!PocketBotMain.getInstance().getTwitchClient().getChat().isChannelJoined(channelName))
							PocketBotMain.getInstance().getTwitchClient().getChat().joinChannel(channelName);

						System.out.println("Refreshed moderation subscription for " + channelName);
					}, 1, TimeUnit.SECONDS);

					System.out.println("Refreshing moderation subscription for " + channelName);
					subscriptions.forEach(sub->{PocketBotMain.getInstance().getTwitchClient().getPubSub().unsubscribeFromTopic(sub);});
					subscriptions.clear();

				}, 0, 60, TimeUnit.MINUTES);

			} catch (Throwable e) {
				if (e.getMessage().contains("Invalid oauth token"))
					sendInvalidTokenMessage();
				e.printStackTrace();
			}
		} else {
			System.out.println("No oAuth token set for " + channelName + "-" + guildID);
		}
	}

	public boolean isConnectedToPubSub() {
		return subscriptions.size() > 0;
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
