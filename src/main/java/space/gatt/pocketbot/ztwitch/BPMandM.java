package space.gatt.pocketbot.ztwitch;

import com.github.philippheuer.events4j.simple.domain.EventSubscriber;
import com.github.twitch4j.chat.events.channel.IRCMessageEvent;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.TextChannel;
import space.gatt.pocketbot.PocketBotMain;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

public class BPMandM {

	public static int CORRECT_NUMBER = 10000;

	private HashMap<String, Integer> guesses = new HashMap<>();
	private HashMap<Integer, List<String>> invertedGuesses = new HashMap<>();
	private HashMap<String, Message> guessMessages = new HashMap<>();

	private HashMap<String, Long> cooldown = new HashMap<>();

	private String logChannelID = "786362381638828042";

	private TextChannel logChannel = PocketBotMain.getInstance().getJDAInstance().getTextChannelById(logChannelID);

	private boolean acceptGuesses = false;

	private boolean addGuess(String username, Integer guessNumber, boolean announce) throws Exception{
		if (guessNumber < 1)
			throw new Exception("lmao no");


		boolean denyGuess = guesses.containsValue(guessNumber);
		if (denyGuess){
			if (announce)
				PocketBotMain.getInstance().getTwitchClient().getChat().sendMessage("back_pocket", "/me " + "@" + username + " - Whoops! Someone has already guessed " + guessNumber + "! Try another one!");

			return false;
		}

		boolean updatedGuess = guesses.containsKey(username.toLowerCase());
		int originalGuess = guesses.getOrDefault(username, -1);


		guesses.put(username.toLowerCase(), guessNumber);

		List<String> guessesForNumber = invertedGuesses.getOrDefault(guessNumber, new ArrayList<>());
		if (updatedGuess){
			List<String> guessesForOriginalNumber = invertedGuesses.getOrDefault(originalGuess, new ArrayList<>());
			guessesForOriginalNumber.remove(username.toLowerCase());
			invertedGuesses.put(originalGuess, guessesForOriginalNumber);
		}
		guessesForNumber.add(username.toLowerCase());
		invertedGuesses.put(guessNumber, guessesForNumber);

		if (announce)
			if (updatedGuess)
				PocketBotMain.getInstance().getTwitchClient().getChat().sendMessage("back_pocket", "/me " + "@" + username + " - Updated your guess to " + guessNumber);
			else
				PocketBotMain.getInstance().getTwitchClient().getChat().sendMessage("back_pocket", "/me " + "@" + username + " - You have guessed " + guessNumber + "! Use the !guess command again to change your guess. The !guess command can only be used once every minute.");

		if (announce) {

			if (guessMessages.containsKey(username))
				guessMessages.get(username).delete().queue();
			logChannel.sendMessage("`" + username + "` guessed `" + guessNumber + "`").queue(msg -> {
				guessMessages.put(username, msg);
			});
		}

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
			int guessNumber = -1;
			try{
				guessNumber = Integer.parseInt(guess);
				addGuess(event.getUserName(), guessNumber, true);
			}catch (Exception e){
				if (!e.getMessage().equalsIgnoreCase("lmao no")) e.printStackTrace();
				PocketBotMain.getInstance().getTwitchClient().getChat().sendMessage(event.getChannelName().get(),
						"/me " + "@" + event.getUserName() + " - Your !guess was invalid. Try again! (Use !guess NUMBER)");
			}
			return;
		}

		if (event.getMessage().orElse("nomsglmaooo").startsWith("!resetguesses") &&
				event.getChannelId().equalsIgnoreCase("485404757") && event.getUserName().equalsIgnoreCase("gatt_au")) {
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
				event.getChannelId().equalsIgnoreCase("485404757") && event.getUserName().equalsIgnoreCase("gatt_au")) {
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

		if (event.getMessage().orElse("nomsglmaooo").startsWith("!randomvotes") &&
				event.getChannelId().equalsIgnoreCase("485404757") && event.getUserName().equalsIgnoreCase("gatt_au")) {
			for (int i = 0; i<100; i++) {
				Integer guess = ThreadLocalRandom.current().nextInt(CORRECT_NUMBER / 2, CORRECT_NUMBER * 2);
				for (int z = 0; z < 10; z++) {
					String randomName = UUID.randomUUID().toString().split("-")[0];
					try {
						addGuess(randomName, guess, false);
					} catch (Exception e) {
						e.printStackTrace();
					}
					System.out.println(randomName + " guessed " + guess);
				}
			}
			try {
				addGuess("gatt_au", CORRECT_NUMBER, true);
				addGuess("gatt", CORRECT_NUMBER, false);
				addGuess("greg", CORRECT_NUMBER, false);
				addGuess("harry", CORRECT_NUMBER, false);
				addGuess("peanut_m&m", CORRECT_NUMBER, false);
			}catch (Exception e){

			}
			return;
		}

		if (event.getMessage().orElse("nomsglmaooo").startsWith("!announcewinner") && event.getChannelId().equalsIgnoreCase("485404757") && event.getUserName().equalsIgnoreCase("gatt_au")) {
			int c = guesses.values().stream()
					.min(Comparator.comparingInt(i -> Math.abs(i - CORRECT_NUMBER)))
					.orElseThrow(() -> new NoSuchElementException("No value present"));

			List<String> closestGuessers = invertedGuesses.getOrDefault(c, new ArrayList<>());
			if (closestGuessers.size() > 0){
				String congrats = "@" + String.join(", @", closestGuessers);
				PocketBotMain.getInstance().getTwitchClient().getChat().sendMessage(event.getChannelName().get(), "/me The correct amount of M&Ms is... " + CORRECT_NUMBER);
				PocketBotMain.getInstance().getTwitchClient().getChat().sendMessage(event.getChannelName().get(), "/me The following person had the closest guess of " + c + "! " + congrats);
			}else
				PocketBotMain.getInstance().getTwitchClient().getChat().sendMessage(event.getChannelName().get(), "/me !!! Somehow, nobody got close?");

		}
	}
}
