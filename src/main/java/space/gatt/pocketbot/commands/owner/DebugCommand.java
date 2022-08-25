package space.gatt.pocketbot.commands.owner;

import com.github.twitch4j.common.events.domain.EventChannel;
import com.github.twitch4j.events.ChannelGoLiveEvent;
import com.github.twitch4j.helix.domain.Game;
import com.github.twitch4j.helix.domain.GameTopList;
import com.github.twitch4j.helix.domain.Stream;
import com.github.twitch4j.helix.domain.StreamSchedule;
import com.google.gson.Gson;
import com.jagrosh.jdautilities.command.Command;
import com.jagrosh.jdautilities.command.CommandEvent;
import net.dv8tion.jda.api.MessageBuilder;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.interactions.components.Button;
import net.dv8tion.jda.api.interactions.components.ButtonStyle;
import net.dv8tion.jda.api.interactions.components.Component;
import org.checkerframework.checker.units.qual.Mass;
import space.gatt.pocketbot.PocketBotMain;
import space.gatt.pocketbot.configs.GuildConfiguration;
import space.gatt.pocketbot.database.GameCache;
import space.gatt.pocketbot.utils.MessageUtil;
import space.gatt.pocketbot.ztwitch.Guesser;
import space.gatt.pocketbot.ztwitch.TwitchChannelWatcher;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class DebugCommand extends Command {

	public DebugCommand() {
		this.name = "debug";
		this.ownerCommand = true;
		this.children = new Command[]{new AllChannelsDebug(), new FixStorage(), new CacheGames(), new GetGame(), new EmulateStream(), new SetMandM(), new MassBan(), new SyncSchedule()};
	}

	@Override
	protected void execute(CommandEvent commandEvent) {
		commandEvent.reply(MessageUtil.generateHelpForCommand(this).build());
	}

	public class SyncSchedule extends Command {
		public SyncSchedule() {
			this.name = "syncschedule";
			this.ownerCommand = true;
			this.help = "Sync a Twitch Schedule to Discord Events";
		}

		@Override
		protected void execute(CommandEvent commandEvent) {
			if (commandEvent.getArgs().isEmpty()){
				commandEvent.reply("Give me a Channel Name");
				return;
			}
			String channel = commandEvent.getArgs();

			List<TwitchChannelWatcher> watchers = TwitchChannelWatcher.getWatchersForChannel(channel.toLowerCase());
			if (watchers.size() == 0){
				System.out.println("Watchers for " + channel.toLowerCase() + " is 0");
				TwitchChannelWatcher.reloadAll();
				watchers = TwitchChannelWatcher.getWatchersForChannel(channel.toLowerCase());
			}

			TwitchChannelWatcher tsw = watchers.get(0);
			String twitchChannelId = tsw.getTwitchChannelID();
			StreamSchedule schedule = PocketBotMain.getInstance().getTwitchClient().getHelix().getChannelStreamSchedule(PocketBotMain.getInstance().getTwitchCredentials().getAccessToken(),
					twitchChannelId, null, null,null, null, null).execute().getSchedule();

			List<String> eventData = new ArrayList<>();
			schedule.getSegments().forEach((ss)->{
				if (ss.getCanceledUntil() == null)  eventData.add(twitchChannelId + ":::" + (ss.isRecurring() ? ss.getId() + "-recurring-" + ss.getStartTime().toEpochMilli() : ss.getId()) + ":::" + (!ss.getTitle().isEmpty() ? ss.getTitle() : "No Set Title") + ":::" + ss.getStartTime().toEpochMilli() + ":::" + ss.getEndTime().toEpochMilli() +  ":::" + (ss.getCategory() != null ? ss.getCategory().getName() : "No Category") + ":::" + tsw.getChannelName());
			});
			MessageBuilder msgB = new MessageBuilder();
			msgB.setContent("Does this data look correct? (This is 100% not me handing off the data to the NJS bot :) )");
			msgB.setActionRows(ActionRow.of(Button.of(ButtonStyle.PRIMARY, "create-events", "Create Events")));
			InputStream inputStream = new ByteArrayInputStream(String.join(System.lineSeparator(), eventData).getBytes(StandardCharsets.UTF_8));
			commandEvent.getTextChannel().sendMessage(msgB.build()).addFile(inputStream, "allevents.txt").queue();
		}
	}

	public class SetMandM extends Command{
		public SetMandM() {
			this.name = "code";
			this.ownerCommand = true;
			this.help = "Set Code";
		}

		@Override
		protected void execute(CommandEvent commandEvent) {
			Guesser.CORRECT_CODE = commandEvent.getArgs().toLowerCase();
			commandEvent.reply("Set CORRECT_CODE to " + commandEvent.getArgs().toLowerCase());
			commandEvent.getMessage().delete().complete();
		}
	}

	public class MassBan extends Command{
		public MassBan() {
			this.name = "banbots";
			this.ownerCommand = true;
			this.help = "Set Code";
		}

		@Override
		protected void execute(CommandEvent commandEvent) {
			String[] args = commandEvent.getArgs().split(" ");
			List<String> bans = new ArrayList<>();
			try {
				URL url = new URL(args[1]);
				// read text returned by server
				BufferedReader in = new BufferedReader(new InputStreamReader(url.openStream()));

				String line;
				while ((line = in.readLine()) != null) {
					if (!bans.contains(line))
					bans.add(line);
				}
				in.close();
			}
			catch (MalformedURLException e) {
				commandEvent.reply("Malformed URL: " + e.getMessage());
				return;
			}
			catch (IOException e) {
				commandEvent.reply("I/O Error: " + e.getMessage());
			}

			if (!PocketBotMain.getInstance().getTwitchClient().getChat().isChannelJoined(args[0]))
				PocketBotMain.getInstance().getTwitchClient().getChat().joinChannel(args[0]);


			for (String b : bans){
				PocketBotMain.getInstance().getTwitchClient().getChat().sendMessage(args[0], "/ban " + b + " SPAMBOT");
			}

			commandEvent.reply("Ok.");
		}
	}

	public class GetGame extends Command{
		public GetGame() {
			this.name = "getgame";
			this.ownerCommand = true;
			this.help = "Get game from memory by ID";
		}

		@Override
		protected void execute(CommandEvent commandEvent) {
			String id = commandEvent.getArgs();
			GameCache game = GameCache.getGame(id);
			if (game == null){
				commandEvent.reply("ID `" + id + "` returned null");
				return;
			}
			String jsonData = new Gson().toJson(game);
			commandEvent.reply(jsonData);
		}
	}

	public class EmulateStream extends Command{
		public EmulateStream() {
			this.name = "emustream";
			this.ownerCommand = true;
			this.help = "Start a fake stream";
		}

		@Override
		protected void execute(CommandEvent commandEvent) {

			if (commandEvent.getArgs().isEmpty()){
				commandEvent.reply("Give me a Channel Name");
				return;
			}

			String channel = commandEvent.getArgs();

			List<TwitchChannelWatcher> watchers = TwitchChannelWatcher.getWatchersForChannel(channel.toLowerCase());
			if (watchers.size() == 0){
				System.out.println("Watchers for " + channel.toLowerCase() + " is 0");
				TwitchChannelWatcher.reloadAll();
				watchers = TwitchChannelWatcher.getWatchersForChannel(channel.toLowerCase());
			}

			TwitchChannelWatcher tsw = watchers.get(0);
			EventChannel eCh = new EventChannel(tsw.getTwitchChannelID(), tsw.getChannelName());
			Stream userStream = PocketBotMain.getInstance().getTwitchClient().getHelix().getStreams(PocketBotMain.getInstance().getTwitchCredentials().getAccessToken(),
					null, null, null, null, null, null,
					Arrays.asList(tsw.getTwitchChannelID()), null)
					.execute()
					.getStreams()
					.get(0);

			ChannelGoLiveEvent fakeEvent = new ChannelGoLiveEvent(eCh, userStream);
			PocketBotMain.getInstance().getChatListener().onStreamStart(fakeEvent);
		}
	}

	public class CacheGames extends Command{
		public CacheGames() {
			this.name = "cachegames";
			this.ownerCommand = true;
			this.help = "Caches the top 1000 games";
		}

		@Override
		protected synchronized void execute(CommandEvent commandEvent) {
			String lastCursor = null;
			int gamecount = 0;
			for (int next = 0; next < 100; next ++){
				try {
					GameTopList request = PocketBotMain.getInstance().getTwitchClient().getHelix().getTopGames(PocketBotMain.getInstance().getTwitchCredentials().getAccessToken(), lastCursor, null, "100").queue().get(next+1, TimeUnit.SECONDS);
					lastCursor = request.getPagination().getCursor();
					List<Game> topGames = request.getGames();
					List<String> games = new ArrayList<>();
					for (Game g : topGames) {
						GameCache cached = new GameCache(g.getId(), g.getName(), g.getBoxArtUrl(600, 800));
						GameCache.cacheGame(cached);
						games.add(cached.getGameName());
					}
					gamecount = GameCache.saveCache();
					System.out.println("Cached the following games to memory: " + String.join(", ", games) + "    There are now " + gamecount + " games in memory");
				}catch (Exception e){
					e.printStackTrace();
				}
			}
			commandEvent.reply("Done. Stored " + gamecount + " games to memory");
		}
	}

	public class FixStorage extends Command {
		public FixStorage(){
			this.name = "fixstorage";
			this.ownerCommand = true;
			this.help = "Not needed anymore";
		}

		@Override
		protected void execute(CommandEvent commandEvent) {
			for (Guild guilds : PocketBotMain.getInstance().getJDAInstance().getGuilds()){
				GuildConfiguration config = GuildConfiguration.getGuildConfiguration(guilds);
				config.getLogEntryList();
				TwitchChannelWatcher.getWatchersForGuild(guilds).forEach(TwitchChannelWatcher::save);
				config.save();
				commandEvent.reply("Fixed " + config.getGuildInstance().getName());
			}
		}
	}

	public class AllChannelsDebug extends Command {
		public AllChannelsDebug() {
			this.name = "allchannels";
			this.ownerCommand = true;
			this.help = "debugs channels and stuff";
		}


		@Override
		protected void execute(CommandEvent commandEvent) {
			TextChannel putthemhere = commandEvent.getTextChannel();
			try {
				Message pleaseHold = putthemhere.sendMessage("Please hold...").complete(false);

				for (net.dv8tion.jda.api.entities.Category category : commandEvent.getGuild().getCategories()) {
					putthemhere.sendMessage("```diff\n- " + category.getName() + "\n```").queue();
					for (GuildChannel channel : category.getChannels()) {
						if (channel instanceof TextChannel) {
							putthemhere.sendMessage(((TextChannel) channel).getAsMention() + "  [Text Channel] :notepad_spiral: ").queue(s -> {
								s.addReaction("❤").queueAfter(1, TimeUnit.SECONDS);
								s.addReaction("\uD83D\uDC99").queueAfter(2, TimeUnit.SECONDS);
								s.addReaction("\uD83D\uDC9A").queueAfter(3, TimeUnit.SECONDS);
								s.addReaction("\uD83C\uDDFD").queueAfter(4, TimeUnit.SECONDS);
							});
						} else if (channel instanceof VoiceChannel) {
							putthemhere.sendMessage(((VoiceChannel) channel).getName() + "  [Voice Channel] :speaker:").queue(s -> {
								s.addReaction("❤").queueAfter(1, TimeUnit.SECONDS);
								s.addReaction("\uD83D\uDC99").queueAfter(2, TimeUnit.SECONDS);
								s.addReaction("\uD83D\uDC9A").queueAfter(3, TimeUnit.SECONDS);
								s.addReaction("\uD83C\uDDFD").queueAfter(4, TimeUnit.SECONDS);
							});
						}
					}
				}

				pleaseHold.delete().queue();

			} catch (Exception e) {
				commandEvent.replyError(e.getMessage());
			}
		}
	}

}
