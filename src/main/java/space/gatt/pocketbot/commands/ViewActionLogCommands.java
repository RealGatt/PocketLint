package space.gatt.pocketbot.commands;

import com.jagrosh.jdautilities.command.Command;
import com.jagrosh.jdautilities.command.CommandEvent;
import com.jagrosh.jdautilities.doc.standard.CommandInfo;
import com.jagrosh.jdautilities.doc.standard.Error;
import com.jagrosh.jdautilities.doc.standard.RequiredPermissions;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Message;
import space.gatt.pocketbot.configs.GuildConfiguration;
import space.gatt.pocketbot.listeners.AuditLogWatcher;
import space.gatt.pocketbot.utils.MessageUtil;
import space.gatt.pocketbot.utils.ServerLogEntry;

import java.util.Optional;
import java.util.concurrent.TimeUnit;

@CommandInfo(
		name = {"actionlog", "auditlog"},
		description = "View action logs.",
		requirements = {"Manage Server Permission"}
)
@Error("You don't have the required permissions for this command.")
@RequiredPermissions(Permission.MANAGE_SERVER)
public class ViewActionLogCommands extends Command {

	public ViewActionLogCommands() {
		this.name = "actionlog";
		this.guildOnly = true;
		this.help = "View action logs.";
		this.userPermissions = new Permission[]{Permission.MANAGE_SERVER};
		this.children = new Command[]{new ViewActionCommand(), new ActionLogReason()};
	}

	@Override
	protected void execute(CommandEvent commandEvent) {
		commandEvent.reply(MessageUtil.generateHelpForCommand(this).build());
	}

	public class ActionLogReason extends Command {
		public ActionLogReason() {
			this.name = "reason";
			this.guildOnly = true;
			this.userPermissions = new Permission[]{Permission.MANAGE_SERVER};
			this.help = "Set the reason for a given Action";
		}

		@Override
		protected void execute(CommandEvent commandEvent) {
			String[] args = commandEvent.getArgs().split("\\s+");
			if (args.length < 2)
				commandEvent.reply(MessageUtil.getErrorBuilder("**Usage:** `_actionlog reason ActionID The Reason`").build());
			else {
				try {
					int actionId = Integer.parseInt(args[0]);
					args[0] = "";
					String newReason = String.join(" ", args).trim();
					GuildConfiguration config = GuildConfiguration.getGuildConfiguration(commandEvent.getGuild());
					Optional<ServerLogEntry> log = config.getEntryForId(actionId);
					ServerLogEntry entry = log.orElse(null);
					AuditLogWatcher.getIgnoredIDs().add(commandEvent.getMessage().getIdLong());
					if (entry == null)
						commandEvent.reply(MessageUtil.getErrorBuilder("Couldn't find an Action for the ID " + actionId).build());
					else {
						if (newReason.length() == 0)
							commandEvent.reply(MessageUtil.getErrorBuilder("No reason was given.").build());
						else {
							entry.setReason(newReason + "\n - " +
									commandEvent.getAuthor().getAsMention() + "  (" + commandEvent.getAuthor().getName() + "#" + commandEvent.getAuthor().getDiscriminator() + ")");
							GuildConfiguration configuration = GuildConfiguration.getGuildConfiguration(entry.getGuildID());
							configuration.save();
							EmbedBuilder msgB = entry.build();
							if (msgB != null) {
								Message msg = entry.getMessage();
								if (msg != null) {
									msg.editMessage(msgB.build()).queue();
									commandEvent.getTextChannel().sendMessage(
											MessageUtil.getDefaultBuilder().setDescription("Success. Updated The reason for Action ID " + actionId + " to `" + newReason
													+ "`\nClick [here](" + msg.getJumpUrl() + ") to go to the message").build())
											.queue(newMsg -> {
												AuditLogWatcher.getIgnoredIDs().add(newMsg.getIdLong());
												newMsg.delete().reason("Command Cleanup").queueAfter(5, TimeUnit.SECONDS);
												commandEvent.getMessage().delete().reason("Command Cleanup").queueAfter(5, TimeUnit.SECONDS);
											});
								} else {
									commandEvent.getTextChannel().sendMessage(
											MessageUtil.getDefaultBuilder().setDescription("Success. Updated The reason for Action ID " + actionId + " to `" + newReason + "`").build())
											.queue(newMsg -> {
												AuditLogWatcher.getIgnoredIDs().add(newMsg.getIdLong());
												newMsg.delete().reason("Command Cleanup").queueAfter(5, TimeUnit.SECONDS);
												commandEvent.getMessage().delete().reason("Command Cleanup").queueAfter(5, TimeUnit.SECONDS);
											});
									;
								}
							} else {
								commandEvent.reply(MessageUtil.getErrorBuilder("Something went wrong while building the new message. The reason has been updated still.").build());
							}
						}
					}
				} catch (Exception e) {
					e.printStackTrace();
					commandEvent.reply(MessageUtil.getErrorBuilder("Give me a Action ID to check!\nYou gave me " + commandEvent.getArgs()).build());
				}
			}

		}
	}

	public class ViewActionCommand extends Command {
		public ViewActionCommand() {
			this.name = "viewaction";
			this.aliases = new String[]{"view", "action"};
			this.guildOnly = true;
			this.help = "View a specific log.";
			this.userPermissions = new Permission[]{Permission.MANAGE_SERVER};
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
					e.printStackTrace();
				commandEvent.reply(MessageUtil.getErrorBuilder("Give me a Action ID to check!\nYou gave me `" + commandEvent.getArgs() + "`").build());
				}
			}
		}
	}
}
