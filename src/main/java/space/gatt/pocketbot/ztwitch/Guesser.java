package space.gatt.pocketbot.ztwitch;

import com.github.philippheuer.events4j.simple.domain.EventSubscriber;
import com.github.twitch4j.chat.events.channel.IRCMessageEvent;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.TextChannel;
import space.gatt.pocketbot.PocketBotMain;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

public class Guesser {

	public static String CORRECT_CODE = "setme";

	private HashMap<String, String> guesses = new HashMap<>();
	private HashMap<String, List<String>> invertedGuesses = new HashMap<>();
	private HashMap<String, Message> guessMessages = new HashMap<>();

	private HashMap<String, Long> cooldown = new HashMap<>();

	private String logChannelID = "849896507578515477";

	private TextChannel logChannel = PocketBotMain.getInstance().getJDAInstance().getTextChannelById(logChannelID);

	private boolean acceptGuesses = false;

//	private boolean addGuess(String username, Integer guessNumber, boolean announce) throws Exception{
//		if (guessNumber < 1)
//			throw new Exception("lmao no");
//
//
//		boolean denyGuess = guesses.containsValue(guessNumber);
//		if (denyGuess){
//			if (announce)
//				PocketBotMain.getInstance().getTwitchClient().getChat().sendMessage("back_pocket", "/me " + "@" + username + " - Whoops! Someone has already guessed " + guessNumber + "! Try another one!");
//
//			return false;
//		}
//
//		boolean updatedGuess = guesses.containsKey(username.toLowerCase());
//		int originalGuess = guesses.getOrDefault(username, -1);
//
//
//		guesses.put(username.toLowerCase(), guessNumber);
//
//		List<String> guessesForNumber = invertedGuesses.getOrDefault(guessNumber, new ArrayList<>());
//		if (updatedGuess){
//			List<String> guessesForOriginalNumber = invertedGuesses.getOrDefault(originalGuess, new ArrayList<>());
//			guessesForOriginalNumber.remove(username.toLowerCase());
//			invertedGuesses.put(originalGuess, guessesForOriginalNumber);
//		}
//		guessesForNumber.add(username.toLowerCase());
//		invertedGuesses.put(guessNumber, guessesForNumber);
//
//		if (announce)
//			if (updatedGuess)
//				PocketBotMain.getInstance().getTwitchClient().getChat().sendMessage("back_pocket", "/me " + "@" + username + " - Updated your guess to " + guessNumber);
//			else
//				PocketBotMain.getInstance().getTwitchClient().getChat().sendMessage("back_pocket", "/me " + "@" + username + " - You have guessed " + guessNumber + "! Use the !guess command again to change your guess. The !guess command can only be used once every minute.");
//
//		if (announce) {
//
//			if (guessMessages.containsKey(username))
//				guessMessages.get(username).delete().queue();
//			logChannel.sendMessage("`" + username + "` guessed `" + guessNumber + "`").queue(msg -> {
//				guessMessages.put(username, msg);
//			});
//		}
//
//		return true;
//	}

	private List<String> moderators = Arrays.asList("jexx___",
			"kitcarnage",
			"whattheshark",
			"phoenixolion",
			"nightbot",
			"caboose221",
			"lisdaliasalvatore",
			"n0xbishop",
			"disturbed778",
			"gatt_au",
			"pockety",
			"argentv",
			"allsyntaxerrors",
			"sithkat",
			"goosegus");


	private boolean addGuess(String username, String code){

		code = code.toLowerCase();

		boolean updatedGuess = guesses.containsKey(username.toLowerCase());
		String originalGuess = guesses.getOrDefault(username, "setme");
		guesses.put(username.toLowerCase(), code);
		List<String> guessesForCode = invertedGuesses.getOrDefault(code, new ArrayList<>());
		if (updatedGuess){
			List<String> guessesForOriginalNumber = invertedGuesses.getOrDefault(originalGuess, new ArrayList<>());
			guessesForOriginalNumber.remove(username.toLowerCase());
			invertedGuesses.put(originalGuess, guessesForOriginalNumber);
		}

		guessesForCode.add(username.toLowerCase());
		invertedGuesses.put(code, guessesForCode);
		if (guessMessages.containsKey(username))
			guessMessages.get(username).delete().queue();

		logChannel.sendMessage("`" + username + "` guessed `" + code + "`").queue(msg -> {
			guessMessages.put(username, msg);
		});

		return true;

	}

	@EventSubscriber // delete message
	public void onIRCMessage(IRCMessageEvent event) {

		if (event.getMessage().orElse("nomsglmaooo").startsWith("!guess ") && event.getChannelId().equalsIgnoreCase("485404757")) {

			if (! acceptGuesses) return;

			Long lastNotifyTime = cooldown.getOrDefault(event.getUserId(), 0L);
			Long timeDifference = System.currentTimeMillis() - lastNotifyTime;
			if (timeDifference < 60000 /* 1 minutes */) return;


			cooldown.put(event.getUserId(), System.currentTimeMillis());

			String message = event.getMessage().get().replaceAll("!guess ", "").trim();
			String[] splitMessage = message.split(" ");

			String guess = splitMessage[0];
			addGuess(event.getUserName(), guess);
			return;
		}

		if (event.getMessage().orElse("nomsglmaooo").startsWith("!resetguesses") &&
				event.getChannelId().equalsIgnoreCase("485404757") && moderators.contains(event.getUserName().toLowerCase())) {
			guesses.clear();
			invertedGuesses.clear();
			cooldown.clear();
			guessMessages.forEach((s, msg)->{
				msg.delete().queue();
			});
			guessMessages.clear();
			PocketBotMain.getInstance().getTwitchClient().getChat().sendMessage(event.getChannelName().get(),
					"/me " + "@" + event.getUserName() + " - Everything has been reset");
			return;
		}

		if (event.getMessage().orElse("nomsglmaooo").startsWith("!toggleguesses") &&
				event.getChannelId().equalsIgnoreCase("485404757") && moderators.contains(event.getUserName().toLowerCase())) {
			acceptGuesses = ! acceptGuesses;
			if (acceptGuesses){
				PocketBotMain.getInstance().getTwitchClient().getChat().sendMessage(event.getChannelName().get(),
						"/me " + "@" + event.getUserName() + " - Guessing has been enabled");
			}else{
				PocketBotMain.getInstance().getTwitchClient().getChat().sendMessage(event.getChannelName().get(),
						"/me " + "@" + event.getUserName() + " - Guessing has been disabled");
			}
			return;
		}

		if (event.getMessage().orElse("nomsglmaooo").startsWith("!announcewinner") && event.getChannelId().equalsIgnoreCase("485404757") && moderators.contains(event.getUserName().toLowerCase())) {

			List<String> correctGuessers = invertedGuesses.getOrDefault(CORRECT_CODE.toLowerCase(), new ArrayList<>());
			if (correctGuessers.size() > 0){
				Collections.shuffle(correctGuessers);
				String winner = correctGuessers.get(0);
				PocketBotMain.getInstance().getTwitchClient().getChat().sendMessage(event.getChannelName().get(), "/me The correct codeword was " + CORRECT_CODE + "!");
				PocketBotMain.getInstance().getTwitchClient().getChat().sendMessage(event.getChannelName().get(), "/me " + winner + " has won the giveaway!");
			}else
				PocketBotMain.getInstance().getTwitchClient().getChat().sendMessage(event.getChannelName().get(), "/me !!! Somehow, nobody guessed the correct answer");

		}
	}
}
