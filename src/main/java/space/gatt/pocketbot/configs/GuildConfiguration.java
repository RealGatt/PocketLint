package space.gatt.pocketbot.configs;

import dev.morphia.annotations.Entity;
import dev.morphia.annotations.Id;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.TextChannel;
import space.gatt.pocketbot.PocketBotMain;
import space.gatt.pocketbot.database.interfaces.MorphiaHelper;
import space.gatt.pocketbot.utils.ServerLogEntry;
import space.gatt.pocketbot.utils.enums.AuditLogType;
import space.gatt.pocketbot.utils.enums.ChannelOption;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

@MorphiaHelper(datastore = "pocketbot")
@Entity(value = "guildconfiguration", noClassnameStored = true)
public class GuildConfiguration {

	private static HashMap<Long, GuildConfiguration> configurationHashMap = new HashMap<>();
	@Id
	private long guildID;
	private transient Guild guildInstance;
	private HashMap<AuditLogType, Boolean> auditLogOptions = new HashMap<>();
	private HashMap<ChannelOption, Long> channelOptions = new HashMap<>();
	private List<String> twitchAuditLogs = new ArrayList<>();
	private HashMap<String, Long> twitchToRolePing = new HashMap<>();
	private long nextActionLogID = 0L;
	private List<ServerLogEntry> logEntryList = new ArrayList<>();

	public GuildConfiguration() {
	}
	// actionid/messageid

	public GuildConfiguration(long guildID) {
		this.guildID = guildID;
	}

	public static GuildConfiguration getGuildConfiguration(Long guildId) {
		GuildConfiguration potentialConfig = PocketBotMain.getInstance().getMongoConnection().getSingleObject("_id", guildId, GuildConfiguration.class);
		if (configurationHashMap.containsKey(guildId)) return configurationHashMap.get(guildId).updateData();
		if (potentialConfig == null) potentialConfig = new GuildConfiguration(guildId);
		configurationHashMap.put(guildId, potentialConfig);
		potentialConfig.save();
		return getGuildConfiguration(guildId).updateData();
	}

	public static GuildConfiguration getGuildConfiguration(Guild guild) {
		return getGuildConfiguration(guild.getIdLong());
	}

	public GuildConfiguration updateData() {
		guildInstance = PocketBotMain.getInstance().getJDAInstance().getGuildById(guildID);
		return this;
	}

	public Long getNextLogID() {
		nextActionLogID++;
		return nextActionLogID;
	}

	public List<String> getTwitchAuditLogs() {
		return twitchAuditLogs;
	}

	public HashMap<String, Long> getTwitchToRolePing() {
		return twitchToRolePing;
	}

	public HashMap<AuditLogType, Boolean> getAuditLogOptions() {
		return auditLogOptions;
	}

	public void setChannelOption(ChannelOption option, TextChannel channel) {
		channelOptions.put(option, channel.getIdLong());
		save();
	}

	public TextChannel getChannel(ChannelOption option) {
		if (! channelOptions.containsKey(option)) return null;
		return guildInstance.getTextChannelById(channelOptions.get(option));
	}

	public Optional<ServerLogEntry> getEntryForId(long actionID) {
		return logEntryList.stream().filter(let -> let.getActionID() == actionID).findFirst();
	}

	public Optional<ServerLogEntry> getFirstEntryForRelevantID(long relevant) {
		return logEntryList.stream().filter(let -> let.getRelevantID() == relevant).findFirst();
	}

	public Optional<ServerLogEntry> getLastEntryForRelevantID(long relevant) {
		return logEntryList.stream().filter(let -> let.getRelevantID() == relevant).reduce((first, second) -> second);
	}

	public Stream<ServerLogEntry> getAllEntriesForRelevantID(long relevant) {
		return logEntryList.stream().filter(let -> let.getRelevantID() == relevant);
	}

	public Optional<ServerLogEntry> getFirstEntryForRelevantID(long relevant, AuditLogType type) {
		return logEntryList.stream().filter(let -> let.getRelevantID() == relevant && let.getType() == type).findFirst();
	}

	public Optional<ServerLogEntry> getLastEntryForRelevantID(long relevant, AuditLogType type) {
		return logEntryList.stream().filter(let -> let.getRelevantID() == relevant && let.getType() == type).reduce((first, second) -> second);
	}

	public Stream<ServerLogEntry> getAllEntriesForRelevantID(long relevant, AuditLogType type) {
		return logEntryList.stream().filter(let -> let.getRelevantID() == relevant && let.getType() == type);
	}

	public void parseLogEntry(ServerLogEntry entry) {
		System.out.println("Logging Entry " + entry.getActionID() + " for Guild " + guildID + " (" + entry.getType() + ")");
		if (auditLogOptions.getOrDefault(entry.getType(), entry.getType().getDefaultVal())) {
			TextChannel auditChannel = getChannel(ChannelOption.AUDIT_LOG_CHANNEL);
			System.out.println(auditChannel);
			if (auditChannel == null) return;
			System.out.println("Audit Channel set.");

			EmbedBuilder msgB = entry.build();
			if (msgB != null)
				auditChannel.sendMessage(msgB.build()).queue(s -> entry.setLogChannelMessageID(s.getIdLong()));
		} else {
			System.out.println("Logging this task is disabled");
		}
		logEntryList.add(entry);
		save();
	}

	public void save() {
		PocketBotMain.getInstance().getMongoConnection().storeObject(this);
	}
}
