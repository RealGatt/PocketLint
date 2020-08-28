package space.gatt.pocketbot.configs;

public class BotConfiguration extends Configuration {

	private String botToken = "Set Me", botPrefix = "_";
	private boolean devMode = false;

	public boolean isDevMode() {
		return devMode;
	}

	public String getBotToken() {
		return botToken;
	}

	public String getBotPrefix() {
		return botPrefix;
	}
}
