package uk.co.proxying.tabmanager.utils;

import org.spongepowered.api.entity.living.player.User;
import uk.co.proxying.tabmanager.TabManager;
import uk.co.proxying.tabmanager.tabObjects.TabGroup;

import java.util.Map;

/**
 * Created by Kieran Quigley (Proxying) on 14-Jan-17.
 */
public class PermsHelper {

	static TabGroup findCorrectGroup(User user) {
		for (Map.Entry<String, TabGroup> entry : TabManager.getInstance().getTabGroups().entrySet()) {
			if (user.hasPermission(("tabmanager.group.") + entry.getKey().toLowerCase())) {
				return entry.getValue();
			}
		}
		return null;
	}
}
