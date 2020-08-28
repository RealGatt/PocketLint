package space.gatt.pocketbot.commands.memes;

import com.jagrosh.jdautilities.command.Command;
import com.jagrosh.jdautilities.command.CommandEvent;

public class Excellent extends Command {

	public Excellent() {
		this.name = "excellent";
	}

	@Override
	protected void execute(CommandEvent commandEvent) {
		commandEvent.reply("https://i.imgur.com/HhXPkJJ.gif");
	}
}
