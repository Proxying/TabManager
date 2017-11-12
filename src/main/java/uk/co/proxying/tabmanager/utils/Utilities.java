package uk.co.proxying.tabmanager.utils;

import org.spongepowered.api.Sponge;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.serializer.TextSerializers;
import uk.co.proxying.tabmanager.TabManager;
import uk.co.proxying.tabmanager.tabObjects.BaseTab;
import uk.co.proxying.tabmanager.tabObjects.TabGroup;
import uk.co.proxying.tabmanager.tabObjects.TabPlayer;

import me.rojo8399.placeholderapi.PlaceholderService;

/**
 * Created by Kieran Quigley (Proxying) on 14-Jan-17.
 */
public class Utilities {

	public static void scheduleAsyncTask(Runnable runnable, int delayTicks) {
		Sponge.getScheduler().createTaskBuilder().execute(runnable).delayTicks(delayTicks).async().submit(TabManager.getInstance());
	}

	public static void scheduleSyncTask(Runnable runnable, int delayTicks) {
		Sponge.getScheduler().createTaskBuilder().execute(runnable).delayTicks(delayTicks).submit(TabManager.getInstance());
	}

	public static void scheduleAsyncRepeatingTask(Runnable runnable, int intervalTicks) {
		Sponge.getScheduler().createTaskBuilder().execute(runnable).intervalTicks(intervalTicks).async().submit(TabManager.getInstance());
	}

	public static void scheduleSyncRepeatingTask(Runnable runnable, int intervalTicks) {
		Sponge.getScheduler().createTaskBuilder().execute(runnable).intervalTicks(intervalTicks).submit(TabManager.getInstance());
	}

	public static void updateForcedPlayerName(Player player) {
		TabPlayer tabPlayer = TabManager.getInstance().getTabPlayers().get(player.getUniqueId());
		if (tabPlayer != null) {
			if (TabManager.getInstance().isAddToTeam()) {
				ScoreHandler.getInstance().addPlayerSoloData(player, tabPlayer);
			}
			TabManager.getInstance().getPlayerGroups().put(player.getUniqueId(), tabPlayer);
			if (!TabManager.getInstance().isChangeVanilla()) {
				return;
			}
			Text prefixText = tryFillPlaceholders(player, tabPlayer.prefix);
			Text suffixText = tryFillPlaceholders(player, tabPlayer.suffix);
			// don't deserialize the player's name to allow formats by the prefix
			Text toDisplay = Text.of(prefixText, player.getName(), suffixText);
			for (Player player1 : Sponge.getServer().getOnlinePlayers()) {
				if (player1.getTabList().getEntry(player.getUniqueId()).isPresent()) {
					player1.getTabList().getEntry(player.getUniqueId()).get().setDisplayName(toDisplay);
				}
			}
		}
	}

	public static void updateGroupPlayerName(Player player, TabGroup playerGroup) {
		if (playerGroup != null) {
			if (TabManager.getInstance().isAddToTeam()) {
				ScoreHandler.getInstance().addPlayerGroupData(player, playerGroup);
			}
			TabManager.getInstance().getPlayerGroups().put(player.getUniqueId(), playerGroup);
			if (!TabManager.getInstance().isChangeVanilla()) {
				return;
			}
			Text prefixText = tryFillPlaceholders(player, playerGroup.prefix);
			Text suffixText = tryFillPlaceholders(player, playerGroup.suffix);
			// don't deserialize the player's name to allow formats by the prefix
			Text toDisplay = Text.of(prefixText, player.getName(), suffixText);
			for (Player player1 : Sponge.getServer().getOnlinePlayers()) {
				if (player1.getTabList().getEntry(player.getUniqueId()).isPresent()) {
					player1.getTabList().getEntry(player.getUniqueId()).get().setDisplayName(toDisplay);
				}
			}
		}
	}

	public static void checkAndUpdateName(Player player) {
		updateOtherUsersForPlayer(player);
		if (!TabManager.getInstance().getTabHeader().equalsIgnoreCase("")) {
			player.getTabList().setHeader(tryFillPlaceholders(player, TabManager.getInstance().getTabHeader()));
		}
		if (!TabManager.getInstance().getTabFooter().equalsIgnoreCase("")) {
			player.getTabList().setFooter(tryFillPlaceholders(player, TabManager.getInstance().getTabFooter()));
		}
		if (TabManager.getInstance().getTabPlayers().containsKey(player.getUniqueId())) {
			updateForcedPlayerName(player);
			return;
		}

		TabGroup foundGroup = PermsHelper.findCorrectGroup(player);
		if (foundGroup != null) {
			updateGroupPlayerName(player, foundGroup);
		}
	}

	public static void updateOtherUsersForPlayer(Player player) {
		for (Player player1 : Sponge.getServer().getOnlinePlayers()) {
			if (player.getTabList().getEntry(player1.getUniqueId()).isPresent()) {
				BaseTab tab = TabManager.getInstance().getTabPlayers().get(player1.getUniqueId());
				if (tab == null) {
					tab = TabManager.getInstance().getPlayerGroups().get(player1.getUniqueId());
					if (tab == null) {
						continue;
					}
				}
				Text prefixText = tryFillPlaceholders(player, tab.getPrefix());
				Text suffixText = tryFillPlaceholders(player, tab.getSuffix());
				// don't deserialize the player's name to allow formats by the prefix
				Text update = Text.of(prefixText, player1.getName(), suffixText);
				player.getTabList().getEntry(player1.getUniqueId()).get().setDisplayName(update);
			}
		}
	}

	private static Text tryFillPlaceholders(Player targetPlayer, String string) {
		PlaceholderService placeholderService = TabManager.getInstance().getPlaceholderService();
		if (placeholderService != null) {
			return placeholderService.replacePlaceholders(string, targetPlayer, null); // observer = null
		} else {
			return deserializeText(string);
		}
	}

	private static Text deserializeText(String string) {
		return TextSerializers.FORMATTING_CODE.deserialize(string);
	}
}
