package uk.co.proxying.tabmanager.listeners;

import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.filter.cause.Root;
import org.spongepowered.api.event.network.ClientConnectionEvent;
import uk.co.proxying.tabmanager.TabManager;
import uk.co.proxying.tabmanager.utils.ScoreHandler;
import uk.co.proxying.tabmanager.utils.Utilities;

/**
 * Created by Kieran Quigley (Proxying) on 14-Jan-17.
 */
public class PlayerListener {

	@Listener
	public void onPlayerJoin(ClientConnectionEvent.Join event, @Root Player player) {
		Utilities.scheduleSyncTask(() -> Utilities.checkAndUpdateName(player), 50);
	}

	@Listener
	public void onPlayerLeave(ClientConnectionEvent.Disconnect event, @Root Player player) {
		TabManager.getInstance().getPlayerGroups().remove(player.getUniqueId());
		ScoreHandler.getInstance().removeFromTeam(player);
	}
}
