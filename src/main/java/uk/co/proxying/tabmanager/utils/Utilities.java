package uk.co.proxying.tabmanager.utils;

import io.github.nucleuspowered.nucleus.api.NucleusAPI;
import me.rojo8399.placeholderapi.PlaceholderService;
import me.rojo8399.placeholderapi.impl.utils.TextUtils;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.data.key.Keys;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.entity.living.player.tab.TabListEntry;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.serializer.TextSerializers;
import uk.co.proxying.tabmanager.TabManager;
import uk.co.proxying.tabmanager.tabObjects.BaseTab;
import uk.co.proxying.tabmanager.tabObjects.TabGroup;
import uk.co.proxying.tabmanager.tabObjects.TabPlayer;

import java.util.Map;

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

    private static void updateForcedPlayerName(Player player) {
        TabPlayer tabPlayer = TabManager.getInstance().getTabPlayers().get(player.getUniqueId());
        if (tabPlayer != null) {
            if (TabManager.getInstance().isAddToTeam()) {
                ScoreHandler.getInstance().addPlayerSoloData(player, tabPlayer);
            }
            TabManager.getInstance().getPlayerGroups().put(player.getUniqueId(), tabPlayer);
            if (!TabManager.getInstance().isChangeVanilla()) {
                return;
            }
            StringBuilder toUpdate = new StringBuilder();
            toUpdate.append(tabPlayer.getPrefix()).append(TabManager.getInstance().isUseNicknames(player) ? checkPlayerNickname(player) : player.getName()).append(tabPlayer.getSuffix());

            Text displayThis = tryFillPlaceholders(player, toUpdate.toString());
            if (player.getTabList().getEntry(player.getUniqueId()).isPresent()) {
                player.getTabList().getEntry(player.getUniqueId()).get().setDisplayName(displayThis);
            }
        }
    }

    public static void updateGroupPlayerName(Player player, TabGroup playerGroup) {
        if (playerGroup != null) {
            if (TabManager.getInstance().isAddToTeam()) {
                ScoreHandler.getInstance().addPlayerGroupData(player, playerGroup);
            }
            if (!TabManager.getInstance().isChangeVanilla()) {
                return;
            }
            StringBuilder toUpdate = new StringBuilder();
            toUpdate.append(playerGroup.getPrefix()).append(TabManager.getInstance().isUseNicknames(player) ? checkPlayerNickname(player) : player.getName()).append(playerGroup.getSuffix());

            Text displayThis = tryFillPlaceholders(player, toUpdate.toString());
            // don't deserialize the player's name to allow formats by the prefix
            if (player.getTabList().getEntry(player.getUniqueId()).isPresent()) {
                player.getTabList().getEntry(player.getUniqueId()).get().setDisplayName(displayThis);
            }
        }
    }

    public static void checkAndUpdateName(Player player, boolean forceGroupRecheck) {
        updateOtherUsersForPlayer(player);
        if (!TabManager.getInstance().getTabHeader().equalsIgnoreCase("")) {
            player.getTabList().setHeader(TabManager.getInstance().isAttemptPlaceholders() ? attemptFillPlaceHolders(player, TabManager.getInstance().getTabHeader()) : deserializeText(TabManager.getInstance().getTabHeader()));
        }
        if (!TabManager.getInstance().getTabFooter().equalsIgnoreCase("")) {
            player.getTabList().setFooter(TabManager.getInstance().isAttemptPlaceholders() ? attemptFillPlaceHolders(player, TabManager.getInstance().getTabFooter()) : deserializeText(TabManager.getInstance().getTabFooter()));
        }
        if (TabManager.getInstance().getTabPlayers().containsKey(player.getUniqueId())) {
            updateForcedPlayerName(player);
            return;
        }

        if (forceGroupRecheck) {
            checkAndUpdateGroup(player);
        } else {
            if (TabManager.getInstance().getPlayerGroups().get(player.getUniqueId()) != null) {
                BaseTab baseTab = TabManager.getInstance().getPlayerGroups().get(player.getUniqueId());
                if (baseTab instanceof TabGroup) {
                    updateGroupPlayerName(player, (TabGroup) baseTab);
                }
            }
        }
    }

    public static void checkAndUpdateGroup(Player player) {
        BaseTab oldTab = TabManager.getInstance().getPlayerGroups().get(player.getUniqueId());
        TabGroup oldGroup = null;
        if (oldTab != null && oldTab instanceof TabGroup) {
            oldGroup = (TabGroup) oldTab;
        }
        TabGroup tabGroup = PermsHelper.findCorrectGroup(player, oldGroup);
        if (tabGroup != null) {
            TabManager.getInstance().getPlayerGroups().put(player.getUniqueId(), tabGroup);
        }
    }

    private static void updateOtherUsersForPlayer(Player player) {
        for (Player player1 : Sponge.getServer().getOnlinePlayers()) {
            if (player1.get(Keys.VANISH).orElse(false)) {
                if (!player.equals(player1) && !player.hasPermission("nucleus.vanish.see")) {
                    if (player.getTabList().getEntry(player1.getUniqueId()).isPresent()) {
                        player.getTabList().removeEntry(player1.getUniqueId());
                    }
                }
                //Secondary Player vanished, don't update their tab list entry for the current player and remove them if they are in players tablist.
            } else {
                if (player.getTabList().getEntry(player1.getUniqueId()).isPresent()) {
                    BaseTab tab = TabManager.getInstance().getTabPlayers().get(player1.getUniqueId());
                    if (tab == null) {
                        tab = TabManager.getInstance().getPlayerGroups().get(player1.getUniqueId());
                        if (tab == null) {
                            continue;
                        }
                    }
                    StringBuilder toUpdate = new StringBuilder();
                    toUpdate.append(tab.getPrefix()).append(TabManager.getInstance().isUseNicknames(player1) ? checkPlayerNickname(player1) : player1.getName()).append(tab.getSuffix());

                    Text displayThis = tryFillPlaceholders(player, toUpdate.toString());
                    player.getTabList().getEntry(player1.getUniqueId()).get().setDisplayName(displayThis);
                } else {
                    //Player was vanished and removed from tab, now should be re-added.
                    BaseTab tab = TabManager.getInstance().getTabPlayers().get(player1.getUniqueId());
                    if (tab == null) {
                        tab = TabManager.getInstance().getPlayerGroups().get(player1.getUniqueId());
                        if (tab == null) {
                            continue;
                        }
                    }
                    StringBuilder toUpdate = new StringBuilder();
                    toUpdate.append(tab.getPrefix()).append(TabManager.getInstance().isUseNicknames(player1) ? checkPlayerNickname(player1) : player1.getName()).append(tab.getSuffix());

                    Text displayThis = tryFillPlaceholders(player, toUpdate.toString());
                    player.getTabList().addEntry(TabListEntry.builder()
                            .displayName(displayThis)
                            .profile(player1.getProfile())
                            .gameMode(player1.gameMode().get())
                            .latency(player1.getConnection().getLatency())
                            .list(player.getTabList()).build());
                }
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

    private static Text attemptFillPlaceHolders(Player target, String string) {
        PlaceholderService placeholderService = TabManager.getInstance().getPlaceholderService();
        if (placeholderService != null) {
            Map<String, Object> retreivedPlaceholders = placeholderService.fillPlaceholders(TextUtils.parse(string, placeholderService.getDefaultPattern()), target, null);
            for (Map.Entry<String, Object> entry : retreivedPlaceholders.entrySet()) {
                if (entry.getValue() instanceof Text) {
                    if (string.contains(entry.getKey())) {
                        string = string.replaceAll("%" + entry.getKey() + "%", ((Text) entry.getValue()).toPlain());
                    } else {
                        TabManager.getInstance().getLogger().info("Cannot find key " + entry.getKey() + " in string.");
                    }
                } else {
                    //New Papi 4.5.1 doesn't pass them through as text objects anymore yikes. Add this to support new Papi and keep the old one for old Papi.
                    if (string.contains(entry.getKey())) {
                        string = string.replaceAll("%" + entry.getKey() + "%", String.valueOf(entry.getValue()));
                    } else {
                        TabManager.getInstance().getLogger().info("Cannot find key " + entry.getKey() + " in string.");
                    }
                }
            }
            return deserializeText(string);
        } else {
            TabManager.getInstance().getLogger().info("placeholder service null?");
            return deserializeText(string);
        }
    }

    private static Text deserializeText(String string) {
        return TextSerializers.FORMATTING_CODE.deserialize(string);
    }



    private static String checkPlayerNickname(Player player) {
        if (NucleusAPI.getNicknameService().isPresent()) {
            if(TabManager.getInstance().usePlainNickname()){
                return NucleusAPI.getNicknameService().get().getNickname(player).orElse(Text.of(player.getName() + "Name")).toPlain();
            }else {
                return TextSerializers.FORMATTING_CODE.serialize(NucleusAPI.getNicknameService().get().getNickname(player).orElse(Text.of(player.getName() + "Name")));
            }
        } else {
            if(TabManager.getInstance().usePlainNickname()){
                return Text.of(player.getName()).toPlain();
            }else {
                return TextSerializers.FORMATTING_CODE.serialize(Text.of(player.getName()));
            }

        }
    }

}
