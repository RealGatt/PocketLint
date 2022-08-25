package space.gatt.pocketbot.utils;

import com.jagrosh.jdautilities.command.Command;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.*;
import org.apache.commons.lang3.StringUtils;
import space.gatt.pocketbot.PocketBotMain;

import java.awt.*;
import java.time.OffsetDateTime;
import java.util.List;

public class MessageUtil {

	private static Color clr = Color.decode("#4C7BBD"), errorClr = Color.decode("#fc2403");

	public static Color getColor() {
		return clr;
	}

	public static EmbedBuilder getDefaultBuilder() {
		EmbedBuilder builder = new EmbedBuilder();
		builder.setFooter(PocketBotMain.getInstance().getJDAInstance().getSelfUser().getName(), PocketBotMain.getInstance().getJDAInstance().getSelfUser().getAvatarUrl());
		builder.setTimestamp(OffsetDateTime.now());
		builder.setColor(getColor());
		return builder;
	}

	public static EmbedBuilder getErrorBuilder(String reason) {
		EmbedBuilder builder = getDefaultBuilder();
		builder.setDescription(reason);
		builder.setColor(errorClr);
		return builder;
	}

	public static EmbedBuilder generateHelpForCommand(Command cmd) {
		return generateHelpForCommand(cmd, "");
	}

	public static EmbedBuilder generateHelpForCommand(Command cmd, String prefix) {
		EmbedBuilder helpBuilder = getDefaultBuilder();
		helpBuilder.setTitle(StringUtils.capitalize(cmd.getName()));
		helpBuilder.addField(PocketBotMain.getInstance().getCommandPrefix() + prefix.trim() + " " + cmd.getName(), cmd.getHelp(), false);
		if (cmd.getChildren().length > 0) {
			for (Command childCommand : cmd.getChildren()) {
				helpBuilder.addField(PocketBotMain.getInstance().getCommandPrefix() + prefix.trim() + " " + cmd.getName() + " " + childCommand.getName(), childCommand.getHelp(), true);
			}
		}
		return helpBuilder;
	}

	public static Member getMember(Guild g, String input, Message messageInput) {
		if (messageInput.getMentionedMembers().size() > 0) return messageInput.getMentionedMembers().get(0);

		User potentialRegularUser = null;
		List<User> potentialUsers;

		try {
			potentialUsers = PocketBotMain.getInstance().getJDAInstance().getUsersByName(input, true);
			if (potentialUsers.size() > 0) potentialRegularUser = potentialUsers.get(0);
			if (potentialRegularUser != null && g.getMember(potentialRegularUser) != null)
				return g.getMember(potentialRegularUser);
		} catch (Exception e) {
		}

		try {
			potentialRegularUser = PocketBotMain.getInstance().getJDAInstance().getUserById(input);
			if (potentialRegularUser != null && g.getMember(potentialRegularUser) != null)
				return g.getMember(potentialRegularUser);
		} catch (Exception e) {
		}

		try {
			potentialRegularUser = PocketBotMain.getInstance().getJDAInstance().getUserByTag(input);
			if (potentialRegularUser != null && g.getMember(potentialRegularUser) != null)
				return g.getMember(potentialRegularUser);
		} catch (Exception e) {
		}

		try {
			if (g.getMemberByTag(input) != null) return g.getMemberByTag(input);
		} catch (Exception e) {
		}

		try {
			if (g.getMemberById(input) != null) return g.getMemberById(input);
		} catch (Exception e) {
		}

		if (g.getMembersByName(input, true).size() > 0) return g.getMembersByName(input, true).get(0);

		if (g.getMembersByNickname(input, true).size() > 0) return g.getMembersByNickname(input, true).get(0);

		if (g.getMembersByEffectiveName(input, true).size() > 0) return g.getMembersByEffectiveName(input, true).get(0);

		return null;
	}


	public static int getHighestRoleId(Member m) {
		int highestRole = 0;
		for (Role r : m.getRoles()) {
			if (r.getPosition() > highestRole) {
				highestRole = r.getPosition();
			}
		}
		return highestRole;
	}

}
