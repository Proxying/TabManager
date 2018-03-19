package uk.co.proxying.tabmanager.utils;

import org.spongepowered.api.Sponge;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.scoreboard.CollisionRules;
import org.spongepowered.api.scoreboard.Scoreboard;
import org.spongepowered.api.scoreboard.Team;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.serializer.TextSerializers;
import uk.co.proxying.tabmanager.tabObjects.TabGroup;
import uk.co.proxying.tabmanager.tabObjects.TabPlayer;

/**
 * Created by Kieran Quigley (Proxying) on 15-Jan-17.
 */
public class ScoreHandler {

	private static ScoreHandler instance = null;

	public static ScoreHandler getInstance() {
		if (instance == null) {
			instance = new ScoreHandler();
		}
		return instance;
	}

	private static Scoreboard scoreboard;

	public void setup() {
		scoreboard = Sponge.getServer().getServerScoreboard().orElse(Scoreboard.builder().build());
		clearTeams();
	}

	public void clearTeams() {
		for (Team team : scoreboard.getTeams()) {
			if (team.getName().contains("TabM_")) {
				for (Text member : team.getMembers()) {
					team.removeMember(member);
				}
				team.unregister();
			}
		}
	}

	public boolean addPlayerGroupData(Player player, TabGroup tabGroup) {
		Team team;
		if (scoreboard.getTeam("TabM_" + tabGroup.groupName.toLowerCase()).isPresent()) {
			team = scoreboard.getTeam("TabM_" + tabGroup.groupName.toLowerCase()).get();
		} else {
			team = Team.builder().allowFriendlyFire(true).canSeeFriendlyInvisibles(false).collisionRule(CollisionRules.NEVER)
					.name("TabM_" + tabGroup.groupName.toLowerCase()).prefix(Text.of(TextSerializers.formattingCode('&').deserialize(tabGroup.prefix)))
					.suffix(Text.of(TextSerializers.formattingCode('&').deserialize(tabGroup.suffix))).build();
			scoreboard.registerTeam(team);
		}
		team.addMember(player.getTeamRepresentation());
		return true;
	}

	public boolean addPlayerSoloData(Player player, TabPlayer tabPlayer) {
		Team team;
		if (scoreboard.getTeam("TabM_" + player.getName().toLowerCase()).isPresent()) {
			team = scoreboard.getTeam("TabM_" + player.getName().toLowerCase()).get();
		} else {
			team = Team.builder().allowFriendlyFire(true).canSeeFriendlyInvisibles(false).collisionRule(CollisionRules.NEVER)
					.name("TabM_" + player.getName().toLowerCase()).prefix(Text.of(TextSerializers.formattingCode('&').deserialize(tabPlayer.prefix)))
					.suffix(Text.of(TextSerializers.formattingCode('&').deserialize(tabPlayer.suffix))).build();
			scoreboard.registerTeam(team);
		}
		team.addMember(player.getTeamRepresentation());
		return true;
	}

	public boolean removeFromTeam(Player player) {
		Team team;
		if (scoreboard.getMemberTeam(player.getTeamRepresentation()).isPresent()) {
			team = scoreboard.getMemberTeam(player.getTeamRepresentation()).get();
			if (team.getName().contains("TabM_")) {
				team.removeMember(player.getTeamRepresentation());
				return true;
			}
		} else {
			return false;
		}
		return false;
	}
}
