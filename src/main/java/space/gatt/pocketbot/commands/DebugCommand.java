package space.gatt.pocketbot.commands;

import com.github.twitch4j.helix.domain.Game;
import com.github.twitch4j.helix.domain.GameTopList;
import com.jagrosh.jdautilities.command.Command;
import com.jagrosh.jdautilities.command.CommandEvent;
import net.dv8tion.jda.api.entities.*;
import space.gatt.pocketbot.PocketBotMain;
import space.gatt.pocketbot.configs.GuildConfiguration;
import space.gatt.pocketbot.database.GameCache;
import space.gatt.pocketbot.utils.MessageUtil;
import space.gatt.pocketbot.utils.ServerLogEntry;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class DebugCommand extends Command {

	public DebugCommand() {
		this.name = "debug";
		this.ownerCommand = true;
		this.children = new Command[]{new AllChannelsDebug(), new FixStorage(), new CacheGames()};
	}

	@Override
	protected void execute(CommandEvent commandEvent) {
		commandEvent.reply(MessageUtil.generateHelpForCommand(this).build());
	}

	public class CacheGames extends Command{
		public CacheGames() {
			this.name = "cachegames";
			this.ownerCommand = true;
			this.help = "Caches the top 1000 games";
		}

		@Override
		protected void execute(CommandEvent commandEvent) {
			String lastCursor = null;
			for (int next = 0; next < 10; next ++){
				GameTopList request = PocketBotMain.getInstance().getTwitchClient().getHelix().getTopGames(PocketBotMain.getInstance().getTwitchCredentials().getAccessToken(), lastCursor, null, "100").execute();
				lastCursor = request.getPagination().getCursor();
				List<Game> topGames = request.getGames();
				List<String> games = new ArrayList<>();
				for (Game g : topGames){
					GameCache cached = new GameCache(g.getId(), g.getName(), g.getBoxArtUrl(600, 800));
					GameCache.cacheGame(cached);
					games.add(cached.getGameName());
				}
				commandEvent.reply("Cached the following games to memory: " + String.join(", ", games));
			}
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
				for (ServerLogEntry sle : config.getLogEntryList()){
					sle.setId(sle.getGuildID() + "-" + sle.getActionID());
					sle.save();
				}
				config.getLogEntryList().clear();
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
