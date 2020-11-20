package space.gatt.pocketbot.commands.moderation;

import com.jagrosh.jdautilities.command.Command;
import com.jagrosh.jdautilities.command.CommandEvent;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.MessageBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.entities.VoiceChannel;
import space.gatt.pocketbot.utils.MessageUtil;

import java.util.List;

public class MuteCommand extends Command {

	public MuteCommand() {
		this.name = "mute";
		this.userPermissions = new Permission[]{Permission.MANAGE_ROLES, Permission.MANAGE_PERMISSIONS};
		this.help = "Mute a user";
	}

	@Override
	protected void execute(CommandEvent commandEvent) {
		Member targetMember = MessageUtil.getMember(commandEvent.getGuild(), commandEvent.getArgs(), commandEvent.getMessage());
		if (targetMember == null)
			commandEvent.reply(MessageUtil.getErrorBuilder("You didn't give me a valid user. You gave me `" + commandEvent.getArgs() + "`").build());
		else {

			int userHighest = MessageUtil.getHighestRoleId(targetMember);
			int botHighest = MessageUtil.getHighestRoleId(commandEvent.getSelfMember());
			if (userHighest >= botHighest) {
				commandEvent.reply(MessageUtil.getErrorBuilder(targetMember.getAsMention() + " has a role higher than me.").build());
				return;
			}

			List<Role> mutedRoles = commandEvent.getGuild().getRolesByName("muted", true);
			Role mutedRole = mutedRoles.size() > 0 ? mutedRoles.get(0) : commandEvent.getGuild().createRole().setName("Muted").complete();

			commandEvent.getTextChannel().sendMessage("Muting " + targetMember.getEffectiveName() + "... Please hold").queue(msg -> {

				EmbedBuilder msgBuilder = MessageUtil.getDefaultBuilder();
				try {
					commandEvent.getGuild().addRoleToMember(targetMember, mutedRole).queue();

					for (TextChannel chnl : commandEvent.getGuild().getTextChannels()) {
						try {
							if (chnl.getPermissionOverride(targetMember) == null)
								chnl.createPermissionOverride(targetMember).queue(po ->
										chnl.putPermissionOverride(targetMember).deny(Permission.MESSAGE_ADD_REACTION, Permission.MESSAGE_WRITE).queue()
								);
							else
								chnl.putPermissionOverride(targetMember).deny(Permission.MESSAGE_ADD_REACTION, Permission.MESSAGE_WRITE).queue();
						} catch (Exception e) {
						}
					}
					for (VoiceChannel chnl : commandEvent.getGuild().getVoiceChannels())
						try {
							if (chnl.getPermissionOverride(targetMember) == null)
								chnl.createPermissionOverride(targetMember).queue(po ->
										chnl.putPermissionOverride(targetMember).deny(Permission.VOICE_CONNECT).queue()
								);
							else
								chnl.putPermissionOverride(targetMember).deny(Permission.VOICE_CONNECT).queue();
						} catch (Exception e) {
						}

					msgBuilder.setDescription("Successfully Muted " + targetMember.getAsMention() + " from all Text and Voice Channels.");

					msg.editMessage(new MessageBuilder().setContent("").setEmbed(msgBuilder.build()).build()).queue();

				} catch (Exception e) {
					e.printStackTrace();
					msgBuilder = MessageUtil.getErrorBuilder(e.getMessage());
					msg.editMessage(new MessageBuilder().setEmbed(msgBuilder.build()).build()).queue();
				}
			});
		}
	}
}
