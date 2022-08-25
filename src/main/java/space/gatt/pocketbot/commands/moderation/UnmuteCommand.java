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

public class UnmuteCommand extends Command {

	public UnmuteCommand() {
		this.name = "unmute";
		this.userPermissions = new Permission[]{Permission.MANAGE_ROLES, Permission.MANAGE_PERMISSIONS};
		this.help = "Unmute a user";
	}

	@Override
	protected void execute(CommandEvent commandEvent) {
		Member targetMember = MessageUtil.getMember(commandEvent.getGuild(), commandEvent.getArgs(), commandEvent.getMessage());
		if (targetMember == null)
			commandEvent.reply(MessageUtil.getErrorBuilder("You didn't give me a valid user. You gave me `" + commandEvent.getArgs() + "`").build());
		else {

			List<Role> mutedRoles = commandEvent.getGuild().getRolesByName("muted", true);
			Role mutedRole = mutedRoles.size() > 0 ? mutedRoles.get(0) : commandEvent.getGuild().createRole().setName("Muted").complete();

			commandEvent.getTextChannel().sendMessage("Unmuting... Please hold").queue(msg -> {

				EmbedBuilder msgBuilder = MessageUtil.getDefaultBuilder();
				try {
					commandEvent.getGuild().removeRoleFromMember(targetMember, mutedRole).queue();

					for (TextChannel chnl : commandEvent.getGuild().getTextChannels())
						try {
							if (chnl.getPermissionOverride(targetMember) != null)
								chnl.getPermissionOverride(targetMember).delete().queue();
						} catch (Exception e) {
						}
					for (VoiceChannel chnl : commandEvent.getGuild().getVoiceChannels())
						try {
							if (chnl.getPermissionOverride(targetMember) != null)
								chnl.getPermissionOverride(targetMember).delete().queue();
						} catch (Exception e) {
						}

					msgBuilder.setDescription("Successfully Unmuted " + targetMember.getAsMention() + " from all Text and Voice Channels.");

					msg.editMessage(new MessageBuilder().setContent("").setEmbed(msgBuilder.build()).build()).queue();

				} catch (Exception e) {
					msgBuilder = MessageUtil.getErrorBuilder(e.getMessage());
					msg.editMessage(new MessageBuilder().setContent("").setEmbed(msgBuilder.build()).build()).queue();
				}
			});
		}

	}
}
