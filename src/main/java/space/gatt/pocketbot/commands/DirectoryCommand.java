package space.gatt.pocketbot.commands;

import com.jagrosh.jdautilities.command.Command;
import com.jagrosh.jdautilities.command.CommandEvent;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.*;
import space.gatt.pocketbot.utils.MessageUtil;

import java.util.ArrayList;
import java.util.List;

public class DirectoryCommand extends Command {

	public DirectoryCommand() {
		this.name = "directory";
		this.guildOnly = true;
		this.help = "Post a Server Directory. **Usage:** `_directory A Role`";
		this.botPermissions = new Permission[]{Permission.MANAGE_SERVER};
		this.userPermissions = new Permission[]{Permission.MANAGE_SERVER};
	}

	@Override
	protected void execute(CommandEvent commandEvent) {

		Role roleToCheck = commandEvent.getGuild().getPublicRole();
		if (!commandEvent.getArgs().isEmpty()){
			List<Role> potentialRole = commandEvent.getGuild().getRolesByName(commandEvent.getArgs(), true);
			if (potentialRole.size() > 0) roleToCheck = potentialRole.get(0);
		}

		Message confirmationMessage = commandEvent.getTextChannel()
				.sendMessage(MessageUtil.getDefaultBuilder().setDescription("Using " + roleToCheck.getAsMention() + " to build Directory. Please wait").build())
				.complete();


		List<MessageEmbed.Field> fieldList = new ArrayList<>();

		for (net.dv8tion.jda.api.entities.Category category : commandEvent.getGuild().getCategories()){
			if (roleToCheck.hasAccess(category)){
				List<String> directoryList = new ArrayList<>();
				for (GuildChannel channel : category.getChannels()){
					if (roleToCheck.hasAccess(channel)) {
						if (channel instanceof TextChannel) {
							TextChannel txtInstance = (TextChannel) channel;
							String message = ">  :notepad_spiral:    " + txtInstance.getAsMention() + "";
							if (txtInstance.getTopic() != null){
								String[] topics = txtInstance.getTopic().split("\n");
								String topic = topics[0];
								message += "\n      `" + (topic.length() > 100 ?
										topic.replaceAll("`", "'") : topic) + "`";
							}
							message += "\n";
							directoryList.add(message);
						} else if (channel instanceof VoiceChannel) {
							VoiceChannel vcInstance = (VoiceChannel) channel;
							directoryList.add(">  :microphone2:   <#" + vcInstance.getId() + "> \n");
						}
					}
				}
				if (directoryList.size() > 0) {
					StringBuilder directoryBuilder = new StringBuilder();
					boolean firstField = true;
					for (String line : directoryList){
						if (directoryBuilder.length() + line.length() >= 1024){
							if (firstField){
								MessageEmbed.Field categoryField = new MessageEmbed.Field("<#" + category.getId() + ">", directoryBuilder.toString(), true);
								fieldList.add(categoryField);
								firstField = false;
								directoryBuilder = new StringBuilder();
							}else{
								MessageEmbed.Field categoryField = new MessageEmbed.Field(" ", directoryBuilder.toString(), true);
								fieldList.add(categoryField);
								firstField = false;
								directoryBuilder = new StringBuilder();
							}
						}
						directoryBuilder.append(line);
					}

					if (firstField){
						MessageEmbed.Field categoryField = new MessageEmbed.Field("<#" + category.getId() + ">", directoryBuilder.toString(), true);
						fieldList.add(categoryField);
					}else{
						MessageEmbed.Field categoryField = new MessageEmbed.Field(" ", directoryBuilder.toString(), true);
						fieldList.add(categoryField);
					}
				}
			}
		}
		for (MessageEmbed.Field field : fieldList){
			commandEvent.getTextChannel().sendMessage("**" + field.getName() +"**" + "\n\n" + field.getValue()).queue();
		}

//		List<EmbedBuilder> builders = new ArrayList<>();
//		EmbedBuilder currentBuilder = MessageUtil.getDefaultBuilder();
//		for (MessageEmbed.Field field : fieldList){
//			if (currentBuilder.getFields().size() < 25 && (currentBuilder.length() + field.getName().length() + field.getValue().length()) < 6000){
//				currentBuilder.addField(field);
//			}else{
//				builders.add(currentBuilder);
//				currentBuilder = MessageUtil.getDefaultBuilder();
//				currentBuilder.addField(field);
//			}
//		}
//		builders.add(currentBuilder);
//		for (EmbedBuilder builder : builders){
//			commandEvent.getTextChannel()
//					.sendMessage(builder.build())
//					.queue();
//		}
		confirmationMessage.delete().queue();
	}
}
