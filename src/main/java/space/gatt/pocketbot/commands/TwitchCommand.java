package space.gatt.pocketbot.commands;

import com.github.twitch4j.chat.enums.TMIConnectionState;
import com.jagrosh.jdautilities.command.Command;
import com.jagrosh.jdautilities.command.CommandEvent;
import com.jagrosh.jdautilities.doc.standard.CommandInfo;
import com.jagrosh.jdautilities.doc.standard.Error;
import com.jagrosh.jdautilities.doc.standard.RequiredPermissions;
import com.jagrosh.jdautilities.menu.Paginator;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.MessageBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;
import space.gatt.pocketbot.PocketBotMain;
import space.gatt.pocketbot.configs.GuildConfiguration;
import space.gatt.pocketbot.listeners.AuditLogWatcher;
import space.gatt.pocketbot.utils.MessageUtil;
import space.gatt.pocketbot.ztwitch.TwitchChannelWatcher;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

@CommandInfo(
		name = {"twitch"},
		description = "Manage Twitch related configurations.",
		requirements = {"Manage Server Emote"}
)
@Error("You don't have the required permissions for this command.")
@RequiredPermissions(Permission.MANAGE_SERVER)

public class TwitchCommand extends Command {

	public TwitchCommand() {
		this.name = "twitch";
		this.help = "Manage Twitch related configurations.";
		this.userPermissions = new Permission[]{Permission.MANAGE_SERVER};
		this.children = new Command[]{new TwitchAuth(), new ManageTwitch()};
	}

	@Override
	protected void execute(CommandEvent commandEvent) {
		commandEvent.reply(MessageUtil.generateHelpForCommand(this).build());
	}

	public class TwitchAuth extends Command {
		public TwitchAuth() {
			this.name = "auth";
			this.help = "Manage oauth keys for Twitch Channels. Setting an oauth token for a channel lets me access more moderation and audit logs.";
			this.userPermissions = new Permission[]{Permission.MANAGE_SERVER};
			this.children = new Command[]{new SetupChannel(), new ListChannel()};
		}

		@Override
		protected void execute(CommandEvent commandEvent) {
			commandEvent.reply(MessageUtil.generateHelpForCommand(this, "twitch ").build());
		}

		public class SetupChannel extends Command {
			public SetupChannel() {
				this.name = "setup";
				this.help = "Setup an oAuth token for a channel";
				this.userPermissions = new Permission[]{Permission.MANAGE_SERVER};
			}

			@Override
			protected void execute(CommandEvent commandEvent) {
				if (commandEvent.getArgs().isEmpty())
					commandEvent.reply(MessageUtil.getErrorBuilder("No Channel Given! Use `_twitch auth setup CHANNELNAME`").build());
				else {
					String channel = commandEvent.getArgs();
					if (channel.length() < 4 || channel.length() > 25 || channel.contains(" ")) {
						commandEvent.reply(MessageUtil.getErrorBuilder("The given username `" + commandEvent.getArgs() + "` isn't a valid username.").build());
						return;
					}
					TextChannel chnl = commandEvent.getTextChannel();
					EmbedBuilder messageBuilder = MessageUtil.getDefaultBuilder();
					GuildConfiguration config = GuildConfiguration.getGuildConfiguration(commandEvent.getGuild());
					if (! config.getTwitchAuditLogs().contains(commandEvent.getArgs().toLowerCase())) {
						commandEvent.reply(MessageUtil.getErrorBuilder("`" + commandEvent.getArgs().toLowerCase() + "` hasn't been added as a Channel for me to watch/log.\nUse `_twitch manage add " + commandEvent.getArgs().toLowerCase() + "`").build());
						return;
					}
					messageBuilder.setDescription("[**Click here**](https://id.twitch.tv/oauth2/authorize?response_type=token&client_id=fz5phr8pejusq0cte60lc2mepqpdlf&redirect_uri=https://dev.gatt.space/pocketlint/&scope=channel_check_subscription+channel_subscriptions+channel:moderate+openid+user_read+chat:read+bits:read+analytics:read:extensions+analytics:read:games+channel:read:hype_train+channel:read:subscriptions+user:edit:follows+user_follows_edit) " +
						"to generate an oAuth Token." +
						"\nReply in this channel with your token." +
						"\nThe token must be generated with the **" + commandEvent.getArgs() + "** Twitch Account." +
						"\nThe bot will attempt to make **" + commandEvent.getArgs() + "** follow `backpocketbot` on Twitch to validate the token, then if successful, will immediately unfollow.");
					chnl.sendMessage(new MessageBuilder()
						.setEmbed(messageBuilder.build())
						.setContent(commandEvent.getAuthor().getAsMention())
						.build())
						.queue(msg ->
							PocketBotMain.getInstance().getWaiter().waitForEvent(GuildMessageReceivedEvent.class,
								e -> (e.getMessage().getAuthor() == commandEvent.getAuthor()),
								e -> {
									String token = e.getMessage().getContentRaw();
									AuditLogWatcher.getIgnoredIDs().add(e.getMessageIdLong());
									e.getMessage().delete().queue();
									TwitchChannelWatcher watcher = TwitchChannelWatcher.getWatcher(channel, commandEvent.getGuild());
									try {
										watcher.setOAuthToken(token);
										watcher.setupTwitch();
										watcher.save();
										messageBuilder.setDescription("Updated the Token for `" + channel.toLowerCase() + "`");
									} catch (Exception exp) {
										messageBuilder.setDescription("Something went wrong! That might have been an invalid token.");
									}
									msg.editMessage(messageBuilder.build()).queue();
								}, 2, TimeUnit.MINUTES, () -> {
									msg.editMessage(":octagonal_sign: **Timed Out!** :octagonal_sign:").queue();
								})
						);
				}
			}
		}

		public class ListChannel extends Command {
			public ListChannel() {
				this.name = "list";
				this.help = "Lists status for channels you've added.";
				this.userPermissions = new Permission[]{Permission.MANAGE_SERVER};
			}

			@Override
			protected void execute(CommandEvent commandEvent) {
				Paginator.Builder paginatorBuilder = new Paginator.Builder().showPageNumbers(true).setUsers().setUsers(commandEvent.getAuthor()).allowTextInput(false)
						.setText("**Current Twitch Connection Status:** " + PocketBotMain.getInstance().getTwitchClient().getChat().getConnectionState().name())
						.waitOnSinglePage(true).setItemsPerPage(3).setColor(MessageUtil.getColor())

						.setTimeout(3, TimeUnit.MINUTES).useNumberedItems(true).setColumns(3).setFinalAction(msg -> {
							msg.clearReactions().queue();
							msg.editMessage(msg.getContentRaw() + "\n**Timed Out**").queue();
						});

				if (PocketBotMain.getInstance().getTwitchClient().getChat().getConnectionState() != TMIConnectionState.CONNECTED)
					PocketBotMain.getInstance().getTwitchClient().getChat().reconnect();

				List<TwitchChannelWatcher> watching = TwitchChannelWatcher.getWatchersForGuild(commandEvent.getGuild());

				if (watching.size() > 0) {
					for (TwitchChannelWatcher watcher : watching) {

						boolean joined = PocketBotMain.getInstance().getTwitchClient().getChat().isChannelJoined(watcher.getChannelName());
						boolean tokenSet = watcher.getOAuthToken() != null;
						String twitchChannel = watcher.getChannelName();

						paginatorBuilder.addItems("**__[" + twitchChannel.toLowerCase() + "](https://twitch.tv/" + twitchChannel.toLowerCase() + ")__**\n" +
								(joined ? ":white_check_mark: Currently in Chat" : ":negative_squared_cross_mark: Currently not in chat. Attempting to rejoin.") + "\n" +
								(tokenSet ? ":white_check_mark: OAuth Token has been set" : ":negative_squared_cross_mark: **No oAuth token found. Logs will be limited. Use `_twitch auth setup " + twitchChannel + "`**")
						);
						if (! joined)
							PocketBotMain.getInstance().getTwitchClient().getChat().joinChannel(twitchChannel.toLowerCase());
					}

					paginatorBuilder.setEventWaiter(PocketBotMain.getInstance().getWaiter()).build()
							.display(commandEvent.getChannel());

				} else {
					EmbedBuilder embedBuilder = MessageUtil.getDefaultBuilder();
					embedBuilder.setTitle("Channels");
					embedBuilder.setDescription("**Current Twitch Connection Status:** " + PocketBotMain.getInstance().getTwitchClient().getChat().getConnectionState().name());
					embedBuilder.setDescription("You're not watching any channels.");
					commandEvent.reply(embedBuilder.build());
				}
			}
		}
	}

	public class GoLiveNotifications extends Command{
		public GoLiveNotifications() {
			this.name = "notifications";
			this.help = "Setup Go Live Notifications for channels";
			this.userPermissions = new Permission[]{Permission.MANAGE_SERVER};
		}

		@Override
		protected void execute(CommandEvent commandEvent) {
			commandEvent.reply(MessageUtil.generateHelpForCommand(this, "twitch").build());
		}
	}

	public class ManageTwitch extends Command {
		public ManageTwitch() {
			this.name = "manage";
			this.help = "Add, remove and list Channels I watch.";
			this.userPermissions = new Permission[]{Permission.MANAGE_SERVER};
			this.children = new Command[]{new AddChannel(), new RemoveChannel(), new ListChannel()};
		}

		@Override
		protected void execute(CommandEvent commandEvent) {
			commandEvent.reply(MessageUtil.generateHelpForCommand(this, "twitch").build());
		}

		public class ListChannel extends Command {
			public ListChannel() {
				this.name = "list";
				this.help = "Lists channels currently watching, and connection to Twitch";
				this.userPermissions = new Permission[]{Permission.MANAGE_SERVER};
			}

			@Override
			protected void execute(CommandEvent commandEvent) {
				GuildConfiguration config = GuildConfiguration.getGuildConfiguration(commandEvent.getGuild());

				Paginator.Builder paginatorBuilder = new Paginator.Builder().showPageNumbers(true).setUsers().setUsers(commandEvent.getAuthor()).allowTextInput(false)
						.setText("**Current Twitch Connection Status:** " + PocketBotMain.getInstance().getTwitchClient().getChat().getConnectionState().name())
						.waitOnSinglePage(true).setItemsPerPage(3).setColor(MessageUtil.getColor())

						.setTimeout(3, TimeUnit.MINUTES).useNumberedItems(true).setColumns(3).setFinalAction(msg -> {
							msg.clearReactions().queue();
							msg.editMessage(msg.getContentRaw() + "\n**Timed Out**").queue();
						});

				if (PocketBotMain.getInstance().getTwitchClient().getChat().getConnectionState() != TMIConnectionState.CONNECTED)
					PocketBotMain.getInstance().getTwitchClient().getChat().reconnect();

				if (config.getTwitchAuditLogs().size() > 0) {
					for (String twitchChannel : config.getTwitchAuditLogs()) {

						boolean joined = PocketBotMain.getInstance().getTwitchClient().getChat().isChannelJoined(twitchChannel);
						TwitchChannelWatcher watcher = TwitchChannelWatcher.getWatcher(twitchChannel, commandEvent.getGuild());

						paginatorBuilder.addItems("**__[" + twitchChannel.toLowerCase() + "](https://twitch.tv/" + twitchChannel.toLowerCase() + ")__**\n" +
								(joined ? ":white_check_mark: Currently in Chat" : ":negative_squared_cross_mark: Currently not in chat. Attempting to rejoin.") +
								"\n**Watching Moderation Events:** " + watcher.isWatchingLogs() +
								"\n**Watching Stream Start:** " + watcher.isWatchingStreamStart() +
								(commandEvent.getArgs().equalsIgnoreCase("debug") ? "\n**JSON:** \n```json\n" + PocketBotMain.getInstance().getGsonInstance().toJson(watcher) + "\n```" : "")
						);
						if (! joined)
							PocketBotMain.getInstance().getTwitchClient().getChat().joinChannel(twitchChannel.toLowerCase());
					}

					paginatorBuilder.setEventWaiter(PocketBotMain.getInstance().getWaiter()).build()
							.display(commandEvent.getChannel());

				} else {
					EmbedBuilder embedBuilder = MessageUtil.getDefaultBuilder();
					embedBuilder.setTitle("Watching Channels");
					embedBuilder.setDescription("**Current Twitch Connection Status:** " + PocketBotMain.getInstance().getTwitchClient().getChat().getConnectionState().name());
					embedBuilder.setDescription("You're not watching any channels.");
					commandEvent.reply(embedBuilder.build());
				}
			}
		}

		public class RemoveChannel extends Command {
			public RemoveChannel() {
				this.name = "remove";
				this.help = "Removes the given channels from the watchers list. Separate channels with a Space";
				this.userPermissions = new Permission[]{Permission.MANAGE_SERVER};
			}

			@Override
			protected void execute(CommandEvent commandEvent) {
				GuildConfiguration config = GuildConfiguration.getGuildConfiguration(commandEvent.getGuild());
				List<String> channelsAdded = new ArrayList<>();

				for (String chnl : commandEvent.getArgs().split(" ")) {
					if (! channelsAdded.contains(chnl)) {
						channelsAdded.add(chnl);
						TwitchChannelWatcher.getWatcher(chnl, commandEvent.getGuild()).setWatchingLogs(false);
						TwitchChannelWatcher.getWatcher(chnl, commandEvent.getGuild()).save();
					}
				}

				for (String channel : channelsAdded)
					config.getTwitchAuditLogs().remove(channel);


				EmbedBuilder embedBuilder = MessageUtil.getDefaultBuilder();
				embedBuilder.setTitle("Watching Channels");
				embedBuilder.addField("Stopped Watching", String.join(", ", channelsAdded), true);
				embedBuilder.addField("Channels Watching", String.join(", ", config.getTwitchAuditLogs()), true);

				commandEvent.reply(embedBuilder.build());
			}
		}

		public class AddChannel extends Command {
			public AddChannel() {
				this.name = "add";
				this.help = "Add the given channels to the watchers list. Separate channels with a Space.";
				this.userPermissions = new Permission[]{Permission.MANAGE_SERVER};
			}

			@Override
			protected void execute(CommandEvent commandEvent) {
				GuildConfiguration config = GuildConfiguration.getGuildConfiguration(commandEvent.getGuild());
				List<String> channelsAdded = new ArrayList<>();

				for (String chnl : commandEvent.getArgs().split(" ")) {
					if (! channelsAdded.contains(chnl)) {
						if (chnl.length() >= 4 && chnl.length() <= 25) {
							channelsAdded.add(chnl);
							TwitchChannelWatcher.getWatcher(chnl, commandEvent.getGuild()).setWatchingLogs(true);
							TwitchChannelWatcher.getWatcher(chnl, commandEvent.getGuild()).save();
						}
					}
				}

				for (String channel : channelsAdded) {
					if (! config.getTwitchAuditLogs().contains(channel))
						config.getTwitchAuditLogs().add(channel);
				}

				EmbedBuilder embedBuilder = MessageUtil.getDefaultBuilder();
				embedBuilder.setTitle("Watching Channels");
				embedBuilder.addField("Now Watching", String.join(", ", channelsAdded), true);
				embedBuilder.addField("Channels Watching", String.join(", ", config.getTwitchAuditLogs()), true);

				commandEvent.reply(embedBuilder.build());
			}
		}
	}
}
