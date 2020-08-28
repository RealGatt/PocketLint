package space.gatt.pocketbot.configs;

public class TwitchConfiguration extends Configuration {

	private String TwitchOAuthToken = "SetMe", TwitchClientID = "SetMe", TwitchClientSecret = "SetMe", TwitchClientRedirect = "SetMe";

	public String getTwitchOAuthToken() {
		return TwitchOAuthToken;
	}

	public String getTwitchClientID() {
		return TwitchClientID;
	}

	public String getTwitchClientSecret() {
		return TwitchClientSecret;
	}

	public String getTwitchClientRedirect() {
		return TwitchClientRedirect;
	}
}
