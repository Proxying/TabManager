package uk.co.proxying.tabmanager.utils;

import io.github.nucleuspowered.nucleus.api.NucleusAPI;
import me.rojo8399.placeholderapi.PlaceholderService;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.serializer.TextSerializers;
import uk.co.proxying.tabmanager.TabManager;
import uk.co.proxying.tabmanager.tabObjects.BaseTab;
import uk.co.proxying.tabmanager.tabObjects.TabGroup;
import uk.co.proxying.tabmanager.tabObjects.TabPlayer;

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
            Text prefixText;
            Text suffixText;
            if (tabPlayer.getPrefix().contains("%") && tabPlayer.getPrefix().indexOf("%") != tabPlayer.getPrefix().lastIndexOf("%")) {
                prefixText = tryFillPlaceholders(player, tabPlayer.getPrefix());
            } else {
                prefixText = deserializeText(tabPlayer.getPrefix());
            }
            if (tabPlayer.getSuffix().contains("%") && tabPlayer.getSuffix().indexOf("%") != tabPlayer.getSuffix().lastIndexOf("%")) {
                suffixText = tryFillPlaceholders(player, tabPlayer.getSuffix());
            } else {
                suffixText = deserializeText(tabPlayer.getSuffix());
            }
            // don't deserialize the player's name to allow formats by the prefix
            Text toDisplay = Text.of(prefixText, TabManager.getInstance().isUseNicknames(player) ? checkPlayerNickname(player) : player.getName(), suffixText);
            if (player.getTabList().getEntry(player.getUniqueId()).isPresent()) {
                player.getTabList().getEntry(player.getUniqueId()).get().setDisplayName(toDisplay);
            }
            //Redundant I think since the player will update this themselves sometime earlier/later in the loop.
            /*for (Player player1 : Sponge.getServer().getOnlinePlayers()) {
                if (player1.getTabList().getEntry(player.getUniqueId()).isPresent()) {
                    player1.getTabList().getEntry(player.getUniqueId()).get().setDisplayName(toDisplay);
                }
            }*/
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
            Text prefixText;
            Text suffixText;
            if (playerGroup.getPrefix().contains("%") && playerGroup.getPrefix().indexOf("%") != playerGroup.getPrefix().lastIndexOf("%")) {
                prefixText = tryFillPlaceholders(player, playerGroup.getPrefix());
            } else {
                prefixText = deserializeText(playerGroup.getPrefix());
            }
            if (playerGroup.getSuffix().contains("%") && playerGroup.getSuffix().indexOf("%") != playerGroup.getSuffix().lastIndexOf("%")) {
                suffixText = tryFillPlaceholders(player, playerGroup.getSuffix());
            } else {
                suffixText = deserializeText(playerGroup.getSuffix());
            }
            // don't deserialize the player's name to allow formats by the prefix
            Text toDisplay = Text.of(prefixText, TabManager.getInstance().isUseNicknames(player) ? checkPlayerNickname(player) : player.getName(), suffixText);
            if (player.getTabList().getEntry(player.getUniqueId()).isPresent()) {
                player.getTabList().getEntry(player.getUniqueId()).get().setDisplayName(toDisplay);
            }
            //Redundant I think since the player will update this themselves sometime earlier/later in the loop.
            /*for (Player player1 : Sponge.getServer().getOnlinePlayers()) {
                if (player1.getTabList().getEntry(player.getUniqueId()).isPresent()) {
                    player1.getTabList().getEntry(player.getUniqueId()).get().setDisplayName(toDisplay);
                }
            }*/
        }
    }

    public static void checkAndUpdateName(Player player) {
        updateOtherUsersForPlayer(player);
        if (!TabManager.getInstance().getTabHeader().equalsIgnoreCase("")) {
            player.getTabList().setHeader(TabManager.getInstance().isAttemptPlaceholders() ? tryFillPlaceholders(player, TabManager.getInstance().getTabHeader()) : deserializeText(TabManager.getInstance().getTabHeader()));
        }
        if (!TabManager.getInstance().getTabFooter().equalsIgnoreCase("")) {
            player.getTabList().setFooter(TabManager.getInstance().isAttemptPlaceholders() ? tryFillPlaceholders(player, TabManager.getInstance().getTabFooter()) : deserializeText(TabManager.getInstance().getTabFooter()));
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
                Text prefixText;
                Text suffixText;
                if (tab.getPrefix().contains("%") && tab.getPrefix().indexOf("%") != tab.getPrefix().lastIndexOf("%")) {
                    prefixText = tryFillPlaceholders(player, tab.getPrefix());
                } else {
                    prefixText = deserializeText(tab.getPrefix());
                }
                if (tab.getSuffix().contains("%") && tab.getSuffix().indexOf("%") != tab.getSuffix().lastIndexOf("%")) {
                    suffixText = tryFillPlaceholders(player, tab.getSuffix());
                } else {
                    suffixText = deserializeText(tab.getSuffix());
                }
                // don't deserialize the player's name to allow formats by the prefix
                Text update = Text.of(prefixText, TabManager.getInstance().isUseNicknames(player1) ? checkPlayerNickname(player1) : player1.getName(), suffixText);
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

    private static Text checkPlayerNickname(Player player) {
        return NucleusAPI.getNicknameService().get().getNickname(player).get();
    }
}
