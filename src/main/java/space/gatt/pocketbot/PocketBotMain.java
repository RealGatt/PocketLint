package space.gatt.pocketbot;

import com.github.philippheuer.credentialmanager.CredentialManager;
import com.github.philippheuer.credentialmanager.CredentialManagerBuilder;
import com.github.philippheuer.credentialmanager.domain.OAuth2Credential;
import com.github.philippheuer.credentialmanager.identityprovider.TwitchIdentityProvider;
import com.github.philippheuer.events4j.simple.SimpleEventHandler;
import com.github.twitch4j.TwitchClient;
import com.github.twitch4j.TwitchClientBuilder;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.jagrosh.jdautilities.command.CommandClient;
import com.jagrosh.jdautilities.command.CommandClientBuilder;
import com.jagrosh.jdautilities.commons.waiter.EventWaiter;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.requests.GatewayIntent;
import net.dv8tion.jda.api.utils.Compression;
import net.dv8tion.jda.api.utils.cache.CacheFlag;
import space.gatt.pocketbot.commands.*;
import space.gatt.pocketbot.commands.memes.Excellent;
import space.gatt.pocketbot.commands.moderation.MuteCommand;
import space.gatt.pocketbot.commands.moderation.UnmuteCommand;
import space.gatt.pocketbot.configs.BotConfiguration;
import space.gatt.pocketbot.configs.Configuration;
import space.gatt.pocketbot.configs.MongoConfiguration;
import space.gatt.pocketbot.configs.TwitchConfiguration;
import space.gatt.pocketbot.database.MongoConnection;
import space.gatt.pocketbot.listeners.AuditLogWatcher;
import space.gatt.pocketbot.ztwitch.ChatListener;
import space.gatt.pocketbot.ztwitch.TwitchChannelWatcher;

import javax.security.auth.login.LoginException;
import java.io.File;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;

public class PocketBotMain {
	final static String dir = System.getProperty("user.dir") + "/data";
	private static PocketBotMain instance = null;
	private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
	private String[] startupArgs;
	private JDA jdaInstance;
	private CommandClient commandClient;
	private MongoConnection mongoConnection;
	private Gson gsonInstance = new GsonBuilder().disableHtmlEscaping().setPrettyPrinting().create();
	private MongoConfiguration databaseConfiguration;
	private BotConfiguration botConfiguration;
	private TwitchConfiguration twitchConfiguration;
	private TwitchClient twitchClient;
	private OAuth2Credential twitchCredentials;
	private CredentialManager credentialManager = CredentialManagerBuilder.builder().build();
	private EventWaiter waiter = new EventWaiter();
	private String twitchBotID = null;
	public PocketBotMain(String[] startupArgs) {
		this.startupArgs = startupArgs;
	}

	// init bot
	public static void main(String[] args) {
		instance = new PocketBotMain(args);
		instance.start();
	}

	public static PocketBotMain getInstance() {
		if (instance == null) {
			System.out.println("Instance is null???");
			System.exit(1);
		}
		return instance;
	}

	public EventWaiter getWaiter() {
		return waiter;
	}

	public OAuth2Credential getTwitchCredentials() {
		return twitchCredentials;
	}

	public CredentialManager getCredentialManager() {
		return credentialManager;
	}

	public String getTwitchBotID() {
		return twitchBotID;
	}

	public ScheduledExecutorService getScheduler() {
		return scheduler;
	}

	public BotConfiguration getBotConfiguration() {
		return botConfiguration;
	}

	public void start() {

		System.out.println("Loading Bot with the following Arguments: " + String.join(", ", startupArgs));
		botConfiguration = Configuration.load(BotConfiguration.class, new File(dir));
		databaseConfiguration = Configuration.load(MongoConfiguration.class, new File(dir));
		twitchConfiguration = Configuration.load(TwitchConfiguration.class, new File(dir));

		commandClient = new CommandClientBuilder()
				.setPrefix(botConfiguration.getBotPrefix())
				.setOwnerId("113462564217683968")
				.addCommand(new EmojiLockCommands())
				.addCommand(new ServerConfigCommands())
				.addCommand(new ViewActionLogCommands())
				.addCommand(new DebugCommand())
				.addCommand(new TwitchCommand())

				.addCommand(new MuteCommand())
				.addCommand(new UnmuteCommand())

				.addCommands(new Excellent())

				.setActivity(Activity.of(Activity.ActivityType.STREAMING, "BackPocket", "https://www.twitch.tv/back_pocket"))
				.useHelpBuilder(true).build();

		try {

			Set<GatewayIntent> intents = new HashSet<>();
			intents.addAll(GatewayIntent.getIntents(GatewayIntent.DEFAULT));
			intents.add(GatewayIntent.GUILD_MEMBERS);

			jdaInstance = JDABuilder.create(botConfiguration.getBotToken(), intents)
					.setAutoReconnect(true)
					.setCompression(Compression.ZLIB)
					.setActivity(Activity.of(Activity.ActivityType.WATCHING, "BackPocket", "https://www.twitch.tv/back_pocket"))
					.addEventListeners(commandClient, new AuditLogWatcher(), waiter)
					.disableCache(CacheFlag.ACTIVITY, CacheFlag.CLIENT_STATUS)
					.build();

			mongoConnection = new MongoConnection(databaseConfiguration.getMongoIP(), databaseConfiguration.getMongoPort(), databaseConfiguration.getMongoUsername(), databaseConfiguration.getMongoPassword());
			mongoConnection.registerMorphiaMaps(this, "space.gatt.pocketbot");

			twitchCredentials = new OAuth2Credential("twitch", twitchConfiguration.getTwitchOAuthToken());

			credentialManager.registerIdentityProvider(
					new TwitchIdentityProvider("fz5phr8pejusq0cte60lc2mepqpdlf", "c0o0p24xsbo2u1dp2jcqgm1ghrrm0z",
							"https://dev.gatt.space/pocketlint/"));
			credentialManager.save();

			twitchClient = TwitchClientBuilder.builder()
					.withCredentialManager(credentialManager)
					.withChatAccount(twitchCredentials)
					.withClientId("fz5phr8pejusq0cte60lc2mepqpdlf")
					.withClientSecret("c0o0p24xsbo2u1dp2jcqgm1ghrrm0z")
					.withRedirectUrl("https://dev.gatt.space/pocketlint/")
					.withScheduledThreadPoolExecutor(new ScheduledThreadPoolExecutor(8))
					.withEnableGraphQL(true)
					.withEnableChat(true)
					.withEnableHelix(true)
					.withEnableKraken(true)
					.withEnablePubSub(true)
					.withEnableTMI(true)
					.build();

			twitchBotID = PocketBotMain.getInstance().getTwitchClient().getKraken()
					.getUsersByLogin(Collections.singletonList("backpocketbot"))
					.execute().getUsers().get(0).getId();

			twitchClient.getEventManager().getEventHandler(SimpleEventHandler.class).registerListener(new ChatListener());

			getMongoConnection().getMultipleObjects(10000, TwitchChannelWatcher.class);
			TwitchChannelWatcher.loadAll();

		} catch (LoginException e) {
			e.printStackTrace();
			System.exit(9);
		}
	}

	public TwitchConfiguration getTwitchConfiguration() {
		return twitchConfiguration;
	}

	public TwitchClient getTwitchClient() {
		return twitchClient;
	}

	public MongoConnection getMongoConnection() {
		return mongoConnection;
	}

	public Gson getGsonInstance() {
		return gsonInstance;
	}

	public String getCommandPrefix() {
		return botConfiguration.getBotPrefix();
	}

	public CommandClient getCommandClient() {
		return commandClient;
	}

	public String[] getStartupArgs() {
		return startupArgs;
	}

	public JDA getJDAInstance() {
		return jdaInstance;
	}
}
