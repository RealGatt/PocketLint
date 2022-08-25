package space.gatt.pocketbot.commands;

import com.jagrosh.jdautilities.command.Command;
import com.jagrosh.jdautilities.command.CommandEvent;
import com.jagrosh.jdautilities.doc.standard.CommandInfo;
import com.jagrosh.jdautilities.doc.standard.Error;
import com.jagrosh.jdautilities.doc.standard.RequiredPermissions;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Emote;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Role;
import space.gatt.pocketbot.PocketBotMain;
import space.gatt.pocketbot.utils.MessageUtil;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@CommandInfo(
		name = {"elock", "emotelock", "emojilock"},
		description = "Lock emotes behind certain roles.",
		requirements = {"Manage Emotes Permission"}
)
@Error("You don't have the required permissions for this command.")
@RequiredPermissions(Permission.MANAGE_EMOTES)
public class EmojiLockCommands extends Command {
	public EmojiLockCommands() {
		this.name = "elock";
		this.aliases = new String[]{"emotelock", "emojilock"};
		this.guildOnly = true;
		this.help = "Lock emotes behind certain roles";
		this.children = new Command[]{new ListEmojiLock(), new LockEmojiCommand(), new LockAddEmojiCommand(), new UnlockEmote()};
	}

	@Override
	protected void execute(CommandEvent commandEvent) {
		commandEvent.reply(MessageUtil.generateHelpForCommand(this).build());
	}

	public class ListEmojiLock extends Command {

		public ListEmojiLock() {
			this.name = "list";
			this.guildOnly = true;
			this.help = "List roles that have access to a specific emote.\n**Usage:** `" +
					PocketBotMain.getInstance()
							.getCommandPrefix() + "elock list :MyEmote:`";
		}

		@Override
		protected void execute(CommandEvent commandEvent) {
			if (commandEvent.getMessage().getEmotes().size() > 0) {

				EmbedBuilder messageBuilder = MessageUtil.getDefaultBuilder();
				for (Emote checkEmote : commandEvent.getMessage().getEmotes()) {
					if (checkEmote.getManager().getGuild().getId().equalsIgnoreCase(commandEvent.getGuild().getId())) {
						if (checkEmote.getRoles().size() > 0) {
							List<String> roleNames = new ArrayList<>();
							for (Role r : checkEmote.getRoles()) roleNames.add(r.getAsMention());
							messageBuilder.addField("Permitted Roles for " + checkEmote.getAsMention(), String.join(", ", roleNames), true);
						} else {
							messageBuilder.setDescription("All roles are allowed to use " + checkEmote.getAsMention() + " :)");
						}
					} else
						messageBuilder.addField(checkEmote.getName(), checkEmote.getName() + " isn't an emote on this server", true);
				}
				commandEvent.reply(messageBuilder.build());
			} else {
				commandEvent.reply(MessageUtil.getErrorBuilder("I need a Server Emote to check against!").build());
			}
		}
	}

	public class UnlockEmote extends Command {

		public UnlockEmote() {
			this.name = "unlock";
			this.guildOnly = true;
			this.botPermissions = new Permission[]{Permission.MANAGE_EMOTES};
			this.userPermissions = new Permission[]{Permission.MANAGE_EMOTES};
			this.help = "Unlocks an emote, allowing all groups to use it.\n**Usage:** `" +
					PocketBotMain.getInstance()
							.getCommandPrefix() + "elock unlock :MyEmote:`";
		}

		@Override
		protected void execute(CommandEvent commandEvent) {
			if (commandEvent.getMessage().getEmotes().size() > 0) {
				StringBuilder emoteBuilder = new StringBuilder();
				EmbedBuilder messageBuilder = MessageUtil.getDefaultBuilder();
				for (Emote checkEmote : commandEvent.getMessage().getEmotes()) {
					if (checkEmote.getManager().getGuild().getId().equalsIgnoreCase(commandEvent.getGuild().getId())) {
						if (checkEmote.getRoles().size() > 0) {
							List<String> roleNames = new ArrayList<>();
							for (Role r : checkEmote.getRoles()) roleNames.add(r.getAsMention());
							messageBuilder.addField("Removed Roles from " + checkEmote.getAsMention(), String.join(", ", roleNames), true);
						}
						emoteBuilder.append(checkEmote.getAsMention());
						checkEmote.getManager().setRoles(null).queue();
					} else {
						messageBuilder.addField(checkEmote.getName(), checkEmote.getName() + " isn't an emote on this server", true);
					}
				}
				messageBuilder.setDescription("All roles are allowed to use " + emoteBuilder.toString() + " :)");

				commandEvent.getTextChannel().sendMessage(messageBuilder.build()).queue();
			} else {
				commandEvent.reply(MessageUtil.getErrorBuilder("I need a Server Emote to check against!").build());
			}
		}
	}


	public class LockEmojiCommand extends Command {

		public LockEmojiCommand() {
			this.name = "lock";
			this.botPermissions = new Permission[]{Permission.MANAGE_EMOTES};
			this.userPermissions = new Permission[]{Permission.MANAGE_EMOTES};
			this.guildOnly = true;
			this.help = "Lock specified emotes to only a list of given roles\n**Usage:** `" +
					PocketBotMain.getInstance()
							.getCommandPrefix() + "elock lock :MyEmote: :AnotherEmote: Pocket Tier, Pocketeers`";
		}

		@Override
		protected void execute(CommandEvent commandEvent) {
			if (commandEvent.getMessage().getEmotes().size() > 0) {
				EmbedBuilder messageBuilder = MessageUtil.getDefaultBuilder();

				String argsGiven = commandEvent.getArgs().replaceAll(", ", ",");
				Set<Role> rolesToLockTo = new HashSet<>();

				List<Emote> validEmotes = new ArrayList<>();

				for (Emote checkEmote : commandEvent.getMessage().getEmotes()) {
					argsGiven = argsGiven.replaceAll(checkEmote.getAsMention(), "").trim();
					try {
						if (checkEmote.getManager().getGuild().getId().equals(commandEvent.getGuild().getId()))
							validEmotes.add(checkEmote);
					} catch (Exception e) {
					}
				}

				StringBuilder stringBuilder = new StringBuilder();
				boolean multiple = commandEvent.getArgs().contains(",");
				if (multiple) {
					String[] roleInputs = argsGiven.split(",");
					for (String roleInput : roleInputs) {
						List<Role> potentialRoles = commandEvent.getGuild().getRolesByName(roleInput, true);
						if (potentialRoles.size() > 0) {
							if (potentialRoles.size() == 1) {
								rolesToLockTo.add(potentialRoles.get(0));
								stringBuilder.append("Added ").append(potentialRoles.get(0).getAsMention()).append("\n");
							} else {
								rolesToLockTo.add(potentialRoles.get(0));
								stringBuilder.append("Added ").append(potentialRoles.get(0).getAsMention()).append(", but there were " + potentialRoles.size() + " potential roles.\n");
							}
						} else {
							stringBuilder.append("Couldn't find a role that goes by `").append(roleInput).append("`\n");
						}
					}
				} else {
					List<Role> potentialRoles = commandEvent.getGuild().getRolesByName(argsGiven, true);
					if (potentialRoles.size() > 0) {
						if (potentialRoles.size() == 1) {
							rolesToLockTo.add(potentialRoles.get(0));
							stringBuilder.append("Added ").append(potentialRoles.get(0).getAsMention()).append("\n");
						} else {
							rolesToLockTo.add(potentialRoles.get(0));
							stringBuilder.append("Added ").append(potentialRoles.get(0).getAsMention()).append("\n");
						}
					} else {
						stringBuilder.append("Couldn't find a role that goes by `").append(argsGiven).append("`\n");
					}
				}
				if (rolesToLockTo.size() == 0) {
					commandEvent.reply(MessageUtil.getErrorBuilder("I couldn't locate any roles for you. You gave me the following: " + argsGiven).build());
					return;
				}

				for (Role role : commandEvent.getGuild().getRoles()) {
					List<Member> peopleInRole = commandEvent.getGuild().getMembersWithRoles(role);
					if (role.isManaged() && peopleInRole.contains(commandEvent.getSelfMember()) && peopleInRole.size() == 1) { // add the bots own role to it
						rolesToLockTo.add(role);
						break;
					}
				}

				messageBuilder.setDescription("Processing...");

				commandEvent.getTextChannel().sendMessage(messageBuilder.build()).queue(s -> {
					messageBuilder.setDescription(null);

					int validEmoteCount = validEmotes.size();
					int[] success = {0};

					for (Emote checkEmote : validEmotes) {
						try {
							if (! checkEmote.getManager().getGuild().getId().equalsIgnoreCase(commandEvent.getGuild().getId())) {
								messageBuilder.addField(checkEmote.getAsMention(), checkEmote.getAsMention() + " isn't an Emote that's on this server - unable to update.", false);
							} else {
								messageBuilder.addField(checkEmote.getAsMention(), stringBuilder.toString(), false);
								checkEmote.getManager().setRoles(rolesToLockTo).queue(yes -> {
									success[0]++;
									if (success[0] == validEmoteCount) s.editMessage(messageBuilder.build()).queue();
								});
							}
						} catch (Exception e) {
							messageBuilder.addField(checkEmote.getName(), checkEmote.getName() + " isn't an Emote that's on this server - unable to update.", false);
						}
					}

				});
			} else {
				commandEvent.reply(MessageUtil.getErrorBuilder("I need a Server Emote to check against!").build());
			}
		}
	}


	public class LockAddEmojiCommand extends Command {

		public LockAddEmojiCommand() {
			this.name = "lockadd";
			this.botPermissions = new Permission[]{Permission.MANAGE_EMOTES};
			this.userPermissions = new Permission[]{Permission.MANAGE_EMOTES};
			this.guildOnly = true;
			this.help = "Add a role to the list of roles that can use an Emote\n**Usage:** `" +
					PocketBotMain.getInstance()
							.getCommandPrefix() + "elock lockadd :MyEmote: :AnotherEmote: Pocket Tier, Pocketeers`";
		}

		@Override
		protected void execute(CommandEvent commandEvent) {
			if (commandEvent.getMessage().getEmotes().size() > 0) {
				EmbedBuilder messageBuilder = MessageUtil.getDefaultBuilder();

				String argsGiven = commandEvent.getArgs().replaceAll(", ", ",");
				Set<Role> rolesToLockTo = new HashSet<>();

				List<Emote> validEmotes = new ArrayList<>();

				for (Emote checkEmote : commandEvent.getMessage().getEmotes()) {
					argsGiven = argsGiven.replaceAll(checkEmote.getAsMention(), "").trim();
					try {
						if (checkEmote.getManager().getGuild().getId().equals(commandEvent.getGuild().getId()))
							validEmotes.add(checkEmote);
					} catch (Exception e) {
					}
				}

				StringBuilder stringBuilder = new StringBuilder();
				boolean multiple = commandEvent.getArgs().contains(",");
				if (multiple) {
					String[] roleInputs = argsGiven.split(",");
					for (String roleInput : roleInputs) {
						List<Role> potentialRoles = commandEvent.getGuild().getRolesByName(roleInput, true);
						if (potentialRoles.size() > 0) {
							if (potentialRoles.size() == 1) {
								rolesToLockTo.add(potentialRoles.get(0));
								stringBuilder.append("Added ").append(potentialRoles.get(0).getAsMention()).append("\n");
							} else {
								rolesToLockTo.add(potentialRoles.get(0));
								stringBuilder.append("Added ").append(potentialRoles.get(0).getAsMention()).append(", but there were " + potentialRoles.size() + " potential roles.\n");
							}
						} else {
							stringBuilder.append("Couldn't find a role that goes by `").append(roleInput).append("`\n");
						}
					}
				} else {
					List<Role> potentialRoles = commandEvent.getGuild().getRolesByName(argsGiven, true);
					if (potentialRoles.size() > 0) {
						if (potentialRoles.size() == 1) {
							rolesToLockTo.add(potentialRoles.get(0));
							stringBuilder.append("Added ").append(potentialRoles.get(0).getAsMention()).append("\n");
						} else {
							rolesToLockTo.add(potentialRoles.get(0));
							stringBuilder.append("Added ").append(potentialRoles.get(0).getAsMention()).append("\n");
						}
					} else {
						stringBuilder.append("Couldn't find a role that goes by `").append(argsGiven).append("`\n");
					}
				}
				if (rolesToLockTo.size() == 0) {
					commandEvent.reply(MessageUtil.getErrorBuilder("I couldn't locate any roles for you. You gave me the following: " + argsGiven).build());
					return;
				}

				for (Role role : commandEvent.getGuild().getRoles()) {
					List<Member> peopleInRole = commandEvent.getGuild().getMembersWithRoles(role);
					if (role.isManaged() && peopleInRole.contains(commandEvent.getSelfMember()) && peopleInRole.size() == 1) { // add the bots own role to it
						rolesToLockTo.add(role);
						break;
					}
				}

				messageBuilder.setDescription("Processing...");

				commandEvent.getTextChannel().sendMessage(messageBuilder.build()).queue(s -> {
					messageBuilder.setDescription(null);

					int validEmoteCount = validEmotes.size();
					int[] success = {0};

					for (Emote checkEmote : validEmotes) {
						try {
							if (! checkEmote.getManager().getGuild().getId().equalsIgnoreCase(commandEvent.getGuild().getId())) {
								messageBuilder.addField(checkEmote.getAsMention(), checkEmote.getAsMention() + " isn't an Emote that's on this server - unable to update.", false);
							} else {
								messageBuilder.addField(checkEmote.getAsMention(), stringBuilder.toString(), false);
								Set<Role> rolesForThisEmote = new HashSet<>(checkEmote.getRoles());
								rolesForThisEmote.addAll(rolesToLockTo);
								checkEmote.getManager().setRoles(rolesForThisEmote).queue(yes -> {
									success[0]++;
									if (success[0] == validEmoteCount) s.editMessage(messageBuilder.build()).queue();
								});
							}
						} catch (Exception e) {
							messageBuilder.addField(checkEmote.getName(), checkEmote.getName() + " isn't an Emote that's on this server - unable to update.", false);
						}
					}

				});
			} else {
				commandEvent.reply(MessageUtil.getErrorBuilder("I need a Server Emote to check against!").build());
			}
		}
	}

}
