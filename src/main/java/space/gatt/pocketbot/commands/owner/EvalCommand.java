package space.gatt.pocketbot.commands.owner;

import com.jagrosh.jdautilities.command.Command;
import com.jagrosh.jdautilities.command.CommandEvent;
import space.gatt.pocketbot.PocketBotMain;
import space.gatt.pocketbot.configs.GuildConfiguration;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;

public class EvalCommand extends Command {

    public EvalCommand() {
        this.name = "linteval";
        this.help = "Evaluates a string, and does it.";
        this.guildOnly = false;
        this.ownerCommand = true;
    }

    @Override
    protected void execute(CommandEvent event) {
        ScriptEngine se = new ScriptEngineManager().getEngineByName("Nashorn");
        se.put("bot", PocketBotMain.getInstance().getJDAInstance().getSelfUser());
        se.put("botmember", event.getSelfMember());
        se.put("event", event);
        se.put("jda", event.getJDA());
        se.put("guild", event.getGuild());
        se.put("guildconfig", GuildConfiguration.getGuildConfiguration(event.getGuild()));
        se.put("shardmanager", PocketBotMain.getInstance().getJDAInstance().getShardManager());
        String args = event.getArgs().replaceAll("([^(]+?)\\s*->", "function($1)");

        try {
            event.replySuccess("Evaluated Successfully:\n```\n" + se.eval(args) + " ```");
        } catch (Exception e) {
            event.replyError("An exception was thrown:\n```\n" + e + " ```");
        }
    }
}