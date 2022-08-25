package space.gatt.pocketbot.configs;

import space.gatt.pocketbot.PocketBotMain;

import java.io.*;

public class Configuration {

	public static <T extends Configuration> T load(Class<T> configClass, File dataFolder) {
		if (! dataFolder.exists()) {
			dataFolder.mkdirs();
		}
		File configFile = new File(dataFolder + "/" + configClass.getSimpleName() + ".json");
		if (! configFile.exists()) {
			try {
				Configuration newConfig = configClass.newInstance();
				newConfig.saveToFile(dataFolder);
				return configClass.cast(newConfig);
			} catch (Exception e) {
				e.printStackTrace();
				return null;
			}
		} else {
			try (Reader reader = new FileReader(dataFolder + "/" + configClass.getSimpleName() + ".json")) {
				return PocketBotMain.getInstance().getGsonInstance().fromJson(reader, configClass);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		try {
			Configuration newConfig = configClass.newInstance();
			newConfig.saveToFile(dataFolder);
			return configClass.cast(newConfig);
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}

	public void saveToFile(File dataFolder) {
		if (! dataFolder.exists()) {
			dataFolder.mkdirs();
		}
		File configFile = new File(dataFolder + "/" + this.getClass().getSimpleName() + ".json");
		if (! configFile.exists()) {
			try {
				try (FileWriter writer = new FileWriter(dataFolder + "/" + this.getClass().getSimpleName() + ".json")) {
					PocketBotMain.getInstance().getGsonInstance().toJson(this, writer);
				} catch (IOException e) {
					e.printStackTrace();
				}
			} catch (Exception e) {
			}
		}
	}
}
