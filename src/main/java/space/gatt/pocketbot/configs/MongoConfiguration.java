package space.gatt.pocketbot.configs;

public class MongoConfiguration extends Configuration {

	private String mongoIP = "Set Me", mongoUsername = "Set Me", mongoPassword = "Set Me";
	private int mongoPort = 27101;

	public String getMongoIP() {
		return mongoIP;
	}

	public int getMongoPort() {
		return mongoPort;
	}

	public String getMongoUsername() {
		return mongoUsername;
	}

	public String getMongoPassword() {
		return mongoPassword;
	}
}
