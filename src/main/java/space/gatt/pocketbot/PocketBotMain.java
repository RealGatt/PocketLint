package space.gatt.pocketbot;

import com.github.philippheuer.credentialmanager.CredentialManager;
import com.github.philippheuer.credentialmanager.CredentialManagerBuilder;
import com.github.philippheuer.credentialmanager.domain.OAuth2Credential;
import com.github.philippheuer.events4j.simple.SimpleEventHandler;
import com.github.twitch4j.TwitchClient;
import com.github.twitch4j.TwitchClientBuilder;
import com.github.twitch4j.auth.providers.TwitchIdentityProvider;
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
import space.gatt.pocketbot.commands.*;
import space.gatt.pocketbot.commands.memes.Excellent;
import space.gatt.pocketbot.commands.moderation.MuteCommand;
import space.gatt.pocketbot.commands.moderation.UnmuteCommand;
import space.gatt.pocketbot.commands.owner.DebugCommand;
import space.gatt.pocketbot.commands.owner.EvalCommand;
import space.gatt.pocketbot.configs.BotConfiguration;
import space.gatt.pocketbot.configs.Configuration;
import space.gatt.pocketbot.configs.MongoConfiguration;
import space.gatt.pocketbot.configs.TwitchConfiguration;
import space.gatt.pocketbot.database.MongoConnection;
import space.gatt.pocketbot.listeners.AuditLogWatcher;
import space.gatt.pocketbot.ztwitch.ChatListener;
import space.gatt.pocketbot.ztwitch.Guesser;
import space.gatt.pocketbot.ztwitch.TwitchChannelWatcher;

import javax.security.auth.login.LoginException;
import java.io.File;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class PocketBotMain {
	final static String dir = System.getProperty("user.dir") + "/data";
	private static PocketBotMain instance = null;
	private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(10);
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
	private TwitchIdentityProvider twitchIdentityProvider;
	private ChatListener chatListener;

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

	public ScheduledExecutorService getScheduler() {
		return scheduler;
	}

	public BotConfiguration getBotConfiguration() {
		return botConfiguration;
	}

	public TwitchIdentityProvider getTwitchIdentityProvider() {
		return twitchIdentityProvider;
	}


	public void start() {

		System.out.println("Loading Bot with the following Arguments: " + String.join(", ", startupArgs));
		File dataFolder = new File(dir);
		botConfiguration = Configuration.load(BotConfiguration.class, dataFolder);
		databaseConfiguration = Configuration.load(MongoConfiguration.class, dataFolder);
		twitchConfiguration = Configuration.load(TwitchConfiguration.class, dataFolder);

		commandClient = new CommandClientBuilder()
				.setPrefix(botConfiguration.getBotPrefix())
				.setAlternativePrefix("pockety ")
				.setOwnerId("113462564217683968")
				.addCommand(new EmojiLockCommands())
				.addCommand(new ServerConfigCommands())
				.addCommand(new ViewActionLogCommands())
				.addCommand(new TwitchCommand())
				.addCommand(new DirectoryCommand())

				.addCommand(new MuteCommand())
				.addCommand(new UnmuteCommand())

				.addCommands(new Excellent())


				.addCommand(new DebugCommand())
				.addCommand(new EvalCommand())

				.setActivity(Activity.of(Activity.ActivityType.STREAMING, "Back_Pocket", "https://www.twitch.tv/back_pocket"))
				.useHelpBuilder(true).build();

		try {

			Set<GatewayIntent> intents = new HashSet<>();
			intents.addAll(GatewayIntent.getIntents(GatewayIntent.DEFAULT));
			intents.addAll(Arrays.asList(GatewayIntent.DIRECT_MESSAGES, GatewayIntent.GUILD_BANS, GatewayIntent.GUILD_EMOJIS,
					GatewayIntent.GUILD_INVITES, GatewayIntent.GUILD_MESSAGE_REACTIONS,
					GatewayIntent.GUILD_MESSAGES, GatewayIntent.GUILD_MEMBERS));

			jdaInstance = JDABuilder.create(botConfiguration.getBotToken(), intents)
					.setAutoReconnect(true)
					.setCompression(Compression.ZLIB)
					.setActivity(Activity.of(Activity.ActivityType.WATCHING, "Back_Pocket", "https://www.twitch.tv/back_pocket"))
					.addEventListeners(commandClient, new AuditLogWatcher(), waiter)
					.build();


			mongoConnection = new MongoConnection(databaseConfiguration.getURI());
			mongoConnection.registerMorphiaMaps(this, "space.gatt.pocketbot");

			twitchCredentials = new OAuth2Credential("twitch", twitchConfiguration.getTwitchOAuthToken());

			twitchIdentityProvider = new TwitchIdentityProvider(twitchConfiguration.getTwitchClientID(), twitchConfiguration.getTwitchClientSecret(), twitchConfiguration.getTwitchClientRedirect());

			credentialManager.registerIdentityProvider(twitchIdentityProvider);
			credentialManager.addCredential("twitch", twitchCredentials);

			credentialManager.save();

			twitchClient = TwitchClientBuilder.builder()
					.withCredentialManager(credentialManager)
					.withChatAccount(twitchCredentials)
					.withClientId(twitchConfiguration.getTwitchClientID())
					.withClientSecret(twitchConfiguration.getTwitchClientSecret())
					.withRedirectUrl(twitchConfiguration.getTwitchClientRedirect())
					.withScheduledThreadPoolExecutor(new ScheduledThreadPoolExecutor(8))
					.withEnableGraphQL(true)
					.withEnableChat(true)
					.withEnableHelix(true)
					.withEnablePubSub(true)
					.withEnableTMI(true)
					.build();

			chatListener = new ChatListener();
			twitchClient.getEventManager().getEventHandler(SimpleEventHandler.class).registerListener(chatListener);
			//twitchClient.getEventManager().getEventHandler(SimpleEventHandler.class).registerListener(new Guesser());

			getMongoConnection().getMultipleObjects(10000, TwitchChannelWatcher.class);
			getScheduler().schedule(TwitchChannelWatcher::loadAll, 1, TimeUnit.SECONDS);

		} catch (LoginException e) {
			e.printStackTrace();
			System.exit(9);
		}
	}

	public ChatListener getChatListener() {
		return chatListener;
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
