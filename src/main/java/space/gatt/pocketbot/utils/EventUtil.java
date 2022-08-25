package space.gatt.pocketbot.utils;

import com.github.twitch4j.helix.domain.StreamSchedule;
import space.gatt.pocketbot.PocketBotMain;
import space.gatt.pocketbot.ztwitch.TwitchChannelWatcher;

import java.util.ArrayList;
import java.util.List;

public class EventUtil {
	public static List<String> getEventsForChannel(String channel) {
		List<TwitchChannelWatcher> watchers = TwitchChannelWatcher.getWatchersForChannel(channel.toLowerCase());
		if (watchers.size() == 0) {
			System.out.println("Watchers for " + channel.toLowerCase() + " is 0");
			TwitchChannelWatcher.reloadAll();
			watchers = TwitchChannelWatcher.getWatchersForChannel(channel.toLowerCase());
		}

		TwitchChannelWatcher tsw = watchers.get(0);
		String twitchChannelId = tsw.getTwitchChannelID();
		StreamSchedule schedule = PocketBotMain.getInstance().getTwitchClient().getHelix().getChannelStreamSchedule(PocketBotMain.getInstance().getTwitchCredentials().getAccessToken(),
				twitchChannelId, null, null, null, null, null).execute().getSchedule();

		List<String> eventData = new ArrayList<>();
		schedule.getSegments().forEach((ss) -> {
			if (ss.getCanceledUntil() == null)
				eventData.add(twitchChannelId + ":::" + (ss.isRecurring() ? ss.getId() + "-recurring-" + ss.getStartTime().toEpochMilli() : ss.getId()) + ":::" + (!ss.getTitle().isEmpty() ? ss.getTitle() : "No Set Title") + ":::" + ss.getStartTime().toEpochMilli() + ":::" + ss.getEndTime().toEpochMilli() + ":::" + (ss.getCategory() != null ? ss.getCategory().getName() : "No Category") + ":::" + tsw.getChannelName());
		});
		return eventData;
	}
}
