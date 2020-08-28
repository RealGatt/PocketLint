package space.gatt.pocketbot.commands;

import com.jagrosh.jdautilities.command.Command;
import com.jagrosh.jdautilities.command.CommandEvent;
import net.dv8tion.jda.api.entities.GuildChannel;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.entities.VoiceChannel;
import space.gatt.pocketbot.utils.MessageUtil;

import java.util.concurrent.TimeUnit;

public class DebugCommand extends Command {

	public DebugCommand() {
		this.name = "debug";
		this.ownerCommand = true;
		this.children = new Command[]{new AllChannelsDebug()};
	}

	@Override
	protected void execute(CommandEvent commandEvent) {
		commandEvent.reply(MessageUtil.generateHelpForCommand(this).build());
	}

	public class AllChannelsDebug extends Command {
		public AllChannelsDebug() {
			this.name = "allchannels";
			this.ownerCommand = true;
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
