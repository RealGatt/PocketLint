package space.gatt.pocketbot.commands;

import com.jagrosh.jdautilities.command.Command;
import com.jagrosh.jdautilities.command.CommandEvent;
import com.jagrosh.jdautilities.doc.standard.CommandInfo;
import com.jagrosh.jdautilities.doc.standard.Error;
import com.jagrosh.jdautilities.doc.standard.RequiredPermissions;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import space.gatt.pocketbot.configs.GuildConfiguration;
import space.gatt.pocketbot.utils.MessageUtil;
import space.gatt.pocketbot.utils.ServerLogEntry;

import java.util.Optional;

@CommandInfo(
		name = {"actionlog"},
		description = "View action logs.",
		requirements = {"Manage Server Emote"}
)
@Error("You don't have the required permissions for this command.")
@RequiredPermissions(Permission.MANAGE_SERVER)
public class ViewActionLogCommands extends Command {

	public ViewActionLogCommands() {
		this.name = "actionlog";
		this.guildOnly = true;
		this.help = "View action logs.";
		this.children = new Command[]{new ViewActionCommand()};
	}

	@Override
	protected void execute(CommandEvent commandEvent) {
		commandEvent.reply(MessageUtil.generateHelpForCommand(this).build());
	}

	public class ViewActionCommand extends Command {
		public ViewActionCommand() {
			this.name = "viewaction";
			this.guildOnly = true;
			this.help = "View a specific log.";
		}

		@Override
		protected void execute(CommandEvent commandEvent) {
			if (commandEvent.getArgs().isEmpty())
				commandEvent.reply(MessageUtil.getErrorBuilder("Give me a Action ID to check!").build());
			else {
				try {
					int actionId = Integer.parseInt(commandEvent.getArgs().split(" ")[0]);
					GuildConfiguration config = GuildConfiguration.getGuildConfiguration(commandEvent.getGuild());
					Optional<ServerLogEntry> log = config.getEntryForId(actionId);
					ServerLogEntry entry = log.orElse(null);
					if (entry == null)
						commandEvent.reply(MessageUtil.getErrorBuilder("Couldn't find an Action for the ID " + actionId).build());
					else {
						EmbedBuilder msgB = entry.build();
						if (msgB != null) commandEvent.reply(msgB.build());
					}
				} catch (Exception e) {
					commandEvent.reply(MessageUtil.getErrorBuilder("Give me a Action ID to check!\nYou gave me " + commandEvent.getArgs()).build());
				}
			}
		}
	}
}
