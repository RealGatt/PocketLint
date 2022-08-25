package space.gatt.pocketbot.database;

import com.github.twitch4j.helix.domain.Game;
import dev.morphia.InsertManyOptions;
import dev.morphia.annotations.Entity;
import dev.morphia.annotations.Id;
import org.apache.commons.lang.StringEscapeUtils;
import space.gatt.pocketbot.PocketBotMain;
import space.gatt.pocketbot.database.interfaces.MorphiaHelper;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;


@MorphiaHelper(datastore = "pocketbot")
@Entity(value = "gamecache")
public class GameCache {
	private static HashMap<String, GameCache> cache = new HashMap<>();
	static {
		cache.put("UNKNOWN", new GameCache("UNKNOWN", "Unknown", "https://static-cdn.jtvnw.net/ttv-static/404_boxart.jpg"));
	}

	public static void cacheGame(GameCache cached){
		cache.put(cached.getGameId(), cached);
	}

	public static int saveCache(){
		List<GameCache> list = new ArrayList();
		Objects.requireNonNull(list);
		cache.values().forEach(list::add);
		PocketBotMain.getInstance().getMongoConnection().getDatastore("pocketbot").save(list);
		return cache.values().size();
	}

	public static GameCache getGame(String id){
		System.out.println("Requesting Game " + id);
		if (cache.containsKey(id)) return cache.get(id);
		try {
			GameCache potential = PocketBotMain.getInstance().getMongoConnection().getSingleObject("_id", id, GameCache.class);
			if (potential != null){
				cache.put(potential.getGameId(), potential);
				return potential;
			}

			Game gme = PocketBotMain.getInstance().getTwitchClient().getHelix()
					.getGames(PocketBotMain.getInstance().getTwitchCredentials().getAccessToken(),
					Arrays.asList(id),
					Collections.emptyList())
					.execute().getGames().get(0);
			String game = gme.getName();
			String boxart = gme.getBoxArtUrl(600, 800);
			GameCache newCache = new GameCache(id, game, boxart);
			cache.put(id, newCache);
			PocketBotMain.getInstance().getMongoConnection().storeObject(newCache);
			return newCache;
		}catch (Exception e){
			e.printStackTrace();
			return cache.get("UNKNOWN");
		}
	}

	@Id
	private String gameId;
	private String gameName;
	private String gameBoxArt;

	public GameCache() {
	}

	public GameCache(String gameId, String gameName, String gameBoxArt) {
		this.gameId = gameId;
		this.gameName = gameName;
		this.gameBoxArt = gameBoxArt;
	}

	public String getGameId() {
		return gameId;
	}

	public String getGameName() {
		return StringEscapeUtils.unescapeJava(gameName);
	}

	public String getGameBoxArt() {
		return gameBoxArt;
	}
}
