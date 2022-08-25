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
import java.util.regex.Pattern;
import java.util.stream.Stream;

@MorphiaHelper(datastore = "pocketbot")
@Entity(value = "guildconfiguration", useDiscriminator = false)
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
	private transient List<ServerLogEntry> logEntryList = null;

	public GuildConfiguration() {
		updateData();
	}
	// actionid/messageid

	public GuildConfiguration(long guildID) {
		this.guildID = guildID;
	}

	public static HashMap<Long, GuildConfiguration> getConfigurationHashMap() {
		return configurationHashMap;
	}

	public List<ServerLogEntry> getLogEntryList() {
		if (logEntryList == null){
			System.out.println("Pulling old Entries for Guild " + getGuildInstance().getName());
			logEntryList = PocketBotMain.getInstance().getMongoConnection().getMultipleObjects("_id", Pattern.compile(guildID + "-*", Pattern.CASE_INSENSITIVE), Integer.MAX_VALUE, ServerLogEntry.class);
			//logEntryList.forEach(ServerLogEntry::save);
			System.out.println("Pulled " + logEntryList.size() + " old Entries for Guild " + getGuildInstance().getName());
		}
		return logEntryList;
	}

	public static GuildConfiguration getGuildConfiguration(Guild guild) {
		return getGuildConfiguration(guild.getIdLong());
	}

	public static GuildConfiguration getGuildConfiguration(Long guildId) {
		if (configurationHashMap.containsKey(guildId)) return configurationHashMap.get(guildId).updateData();

		GuildConfiguration potentialConfig = PocketBotMain.getInstance().getMongoConnection().getSingleObject("_id", guildId, GuildConfiguration.class);
		if (potentialConfig == null) potentialConfig = new GuildConfiguration(guildId);
		configurationHashMap.put(guildId, potentialConfig);
		potentialConfig.save();
		return getGuildConfiguration(guildId).updateData();
	}

	public long getGuildID() {
		return guildID;
	}

	public Guild getGuildInstance() {
		return guildInstance;
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
		return getLogEntryList().stream().filter(let -> let.getActionID() == actionID).findFirst();
	}

	public Optional<ServerLogEntry> getFirstEntryForRelevantID(long relevant) {
		return getLogEntryList().stream().filter(let -> let.getRelevantID() == relevant).findFirst();
	}

	public Optional<ServerLogEntry> getLastEntryForRelevantID(long relevant) {
		return getLogEntryList().stream().filter(let -> let.getRelevantID() == relevant).reduce((first, second) -> second);
	}

	public Stream<ServerLogEntry> getAllEntriesForRelevantID(long relevant) {
		return getLogEntryList().stream().filter(let -> let.getRelevantID() == relevant);
	}

	public Optional<ServerLogEntry> getFirstEntryForRelevantID(long relevant, AuditLogType type) {
		return getLogEntryList().stream().filter(let -> let.getRelevantID() == relevant && let.getType() == type).findFirst();
	}

	public Optional<ServerLogEntry> getLastEntryForRelevantID(long relevant, AuditLogType type) {
		return getLogEntryList().stream().filter(let -> let.getRelevantID() == relevant && let.getType() == type).reduce((first, second) -> second);
	}

	public Stream<ServerLogEntry> getAllEntriesForRelevantID(long relevant, AuditLogType type) {
		return getLogEntryList().stream().filter(let -> let.getRelevantID() == relevant && let.getType() == type);
	}

	public void parseLogEntry(ServerLogEntry entry) {
		//System.out.println("Logging Entry " + entry.getActionID() + " for Guild " + guildID + " (" + entry.getType() + ")");
		if (auditLogOptions.getOrDefault(entry.getType(), entry.getType().getDefaultVal())) {
			TextChannel auditChannel = getChannel(ChannelOption.AUDIT_LOG_CHANNEL);
			if (auditChannel == null) return;
			//System.out.println("Audit Channel set.");

			EmbedBuilder msgB = entry.build();
			if (msgB != null)
				auditChannel.sendMessage(msgB.build()).queue(s -> entry.setLogChannelMessageID(s.getIdLong()));
		}
		getLogEntryList().add(entry);
		entry.save();
		save();
	}

	public void save() {
		PocketBotMain.getInstance().getMongoConnection().storeObject(this);
	}
}
