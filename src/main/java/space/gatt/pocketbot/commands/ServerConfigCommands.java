package space.gatt.pocketbot.commands;

import com.jagrosh.jdautilities.command.Command;
import com.jagrosh.jdautilities.command.CommandEvent;
import com.jagrosh.jdautilities.doc.standard.CommandInfo;
import com.jagrosh.jdautilities.doc.standard.Error;
import com.jagrosh.jdautilities.doc.standard.RequiredPermissions;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.TextChannel;
import space.gatt.pocketbot.configs.GuildConfiguration;
import space.gatt.pocketbot.utils.MessageUtil;
import space.gatt.pocketbot.utils.enums.AuditLogType;
import space.gatt.pocketbot.utils.enums.ChannelOption;

import java.util.ArrayList;
import java.util.List;

@CommandInfo(
		name = {"sconfig"},
		description = "Set Configuration Options.",
		requirements = {"Manage Emotes Permission"}
)
@Error("You don't have the required permissions for this command.")
@RequiredPermissions(Permission.MANAGE_SERVER)
public class ServerConfigCommands extends Command {
	public ServerConfigCommands() {
		this.name = "sconfig";
		this.guildOnly = true;
		this.help = "Set configuration options";
		this.children = new Command[]{new ListOptions(), new SetOption()};
	}

	@Override
	protected void execute(CommandEvent commandEvent) {
		commandEvent.reply(MessageUtil.generateHelpForCommand(this).build());
	}

	public class SetOption extends Command {
		public SetOption() {
			this.name = "set";
			this.guildOnly = true;
			this.help = "Set configuration options";
			this.requiredRole = "Pockety Admin";
			List<Command> children = new ArrayList<>();
			for (ChannelOption channelOption : ChannelOption.values())
				children.add(new SetChannelOptions(channelOption));
			for (AuditLogType auditLogType : AuditLogType.values())
				children.add(new SetAuditLogOptions(auditLogType));
			this.children = children.toArray(new Command[0]);
		}

		@Override
		protected void execute(CommandEvent commandEvent) {
			commandEvent.reply(MessageUtil.generateHelpForCommand(this, "sconfig ").build());
		}

		public class SetChannelOptions extends Command {
			private ChannelOption option;

			public SetChannelOptions(ChannelOption option) {
				this.option = option;
				this.name = option.name().toLowerCase();
				this.guildOnly = true;
				this.help = "Set the " + option.getName() + " option";
				this.requiredRole = "Pockety Admin";
			}

			@Override
			protected void execute(CommandEvent commandEvent) {
				EmbedBuilder messageBuilder = MessageUtil.getDefaultBuilder();
				GuildConfiguration config = GuildConfiguration.getGuildConfiguration(commandEvent.getGuild());
				messageBuilder.setTitle("Set Channel Options - " + option);
				config.setChannelOption(option, commandEvent.getTextChannel());
				messageBuilder.addField("Success", "All " + option.name() + " related messages will be posted to " + commandEvent.getTextChannel().getAsMention(), true);
				commandEvent.reply(messageBuilder.build());
			}
		}

		public class SetAuditLogOptions extends Command {
			private AuditLogType auditLogType;

			public SetAuditLogOptions(AuditLogType type) {
				this.auditLogType = type;
				this.name = auditLogType.name().toLowerCase();
				this.guildOnly = true;
				this.help = "Set the " + auditLogType.getName() + " option";
				this.requiredRole = "Pockety Admin";
			}

			@Override
			protected void execute(CommandEvent commandEvent) {
				EmbedBuilder messageBuilder = MessageUtil.getDefaultBuilder();
				GuildConfiguration config = GuildConfiguration.getGuildConfiguration(commandEvent.getGuild());
				messageBuilder.setTitle("Set Audit Log Options - " + auditLogType);
				if (commandEvent.getArgs().equalsIgnoreCase("yes") || commandEvent.getArgs().equalsIgnoreCase("true") || commandEvent.getArgs().equalsIgnoreCase("on")) {
					config.getAuditLogOptions().put(auditLogType, true);
					messageBuilder.setDescription("Changed " + auditLogType.getName() + " **On**");
				} else if (commandEvent.getArgs().equalsIgnoreCase("no") || commandEvent.getArgs().equalsIgnoreCase("false") || commandEvent.getArgs().equalsIgnoreCase("off")) {
					config.getAuditLogOptions().put(auditLogType, false);
					messageBuilder.setDescription("Changed " + auditLogType.getName() + " **Off**");
				}
				commandEvent.reply(messageBuilder.build());
			}
		}
	}


	public class ListOptions extends Command {
		public ListOptions() {
			this.name = "list";
			this.guildOnly = true;
			this.help = "List configuration options";
			this.requiredRole = "Pockety Admin";
			this.children = new Command[]{new ChannelOptions(), new AuditLogOptions()};
		}

		@Override
		protected void execute(CommandEvent commandEvent) {
			commandEvent.reply(MessageUtil.generateHelpForCommand(this, "sconfig ").build());
		}

		public class ChannelOptions extends Command {
			public ChannelOptions() {
				this.name = "channel";
				this.guildOnly = true;
				this.help = "List channel related options";
				this.requiredRole = "Pockety Admin";
			}

			@Override
			protected void execute(CommandEvent commandEvent) {
				EmbedBuilder messageBuilder = MessageUtil.getDefaultBuilder();
				GuildConfiguration config = GuildConfiguration.getGuildConfiguration(commandEvent.getGuild());
				messageBuilder.setTitle("Channel Options");
				for (ChannelOption type : ChannelOption.values()) {
					TextChannel value = config.getChannel(type);
					messageBuilder.addField(type.name(), value != null ? value.getAsMention() : "Not Set", true);
				}
				commandEvent.reply(messageBuilder.build());
			}
		}

		public class AuditLogOptions extends Command {
			public AuditLogOptions() {
				this.name = "auditlog";
				this.guildOnly = true;
				this.help = "List audit log related options";
				this.requiredRole = "Pockety Admin";
			}

			@Override
			protected void execute(CommandEvent commandEvent) {
				EmbedBuilder messageBuilder = MessageUtil.getDefaultBuilder();
				GuildConfiguration config = GuildConfiguration.getGuildConfiguration(commandEvent.getGuild());
				messageBuilder.setTitle("Audit Log Options");
				for (AuditLogType type : AuditLogType.values()) {
					boolean enabled = config.getAuditLogOptions().getOrDefault(type, type.getDefaultVal());
					messageBuilder.addField(type.name() + " enabled?", enabled ? "Yes" : "No", true);
				}
				commandEvent.reply(messageBuilder.build());
			}
		}
	}
}
