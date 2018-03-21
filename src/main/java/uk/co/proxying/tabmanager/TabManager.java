package uk.co.proxying.tabmanager;

import com.google.gson.*;
import com.google.inject.Inject;
import io.github.nucleuspowered.nucleus.api.NucleusAPI;
import lombok.Getter;
import me.rojo8399.placeholderapi.PlaceholderService;
import ninja.leaping.configurate.ConfigurationNode;
import ninja.leaping.configurate.commented.CommentedConfigurationNode;
import ninja.leaping.configurate.loader.ConfigurationLoader;
import org.slf4j.Logger;
import org.spongepowered.api.Game;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.command.spec.CommandSpec;
import org.spongepowered.api.config.ConfigDir;
import org.spongepowered.api.config.DefaultConfig;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.event.EventManager;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.game.GameReloadEvent;
import org.spongepowered.api.event.game.state.GameAboutToStartServerEvent;
import org.spongepowered.api.event.game.state.GamePreInitializationEvent;
import org.spongepowered.api.event.game.state.GameStartedServerEvent;
import org.spongepowered.api.event.service.ChangeServiceProviderEvent;
import org.spongepowered.api.plugin.Dependency;
import org.spongepowered.api.plugin.Plugin;
import org.spongepowered.api.scheduler.Task;
import org.spongepowered.api.service.permission.PermissionService;
import org.spongepowered.api.text.Text;
import uk.co.proxying.tabmanager.commands.MainCommand;
import uk.co.proxying.tabmanager.commands.ReloadCommand;
import uk.co.proxying.tabmanager.listeners.PlayerListener;
import uk.co.proxying.tabmanager.stats.Metrics;
import uk.co.proxying.tabmanager.tabObjects.BaseTab;
import uk.co.proxying.tabmanager.tabObjects.TabGroup;
import uk.co.proxying.tabmanager.tabObjects.TabPlayer;
import uk.co.proxying.tabmanager.utils.ScoreHandler;
import uk.co.proxying.tabmanager.utils.Utilities;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Plugin(
		id = PluginInfo.ID,
		name = PluginInfo.NAME,
		version = PluginInfo.VERSION,
		description = PluginInfo.DESCRIPTION,
		authors = {
				"Proxying",
				"RandomByte"
		},
		dependencies = {
				@Dependency(id = "placeholderapi"),
				@Dependency(id = "nucleus")
		}
) public class TabManager {

	@Inject
	private Logger logger;

	@Inject @ConfigDir(sharedRoot = false)
	private File configDir;

	@Inject @DefaultConfig(sharedRoot = false)
	private File defaultConfig;

	@Inject @DefaultConfig(sharedRoot = false)
	private ConfigurationLoader<CommentedConfigurationNode> configManager;

	private CommentedConfigurationNode configurationNode;

	@Inject
	private Game game;
	@Inject
	private Metrics metrics;

	@Getter
	private static TabManager instance;

	@Getter
	private PermissionService permissionService;

	@Getter
	private PlaceholderService placeholderService;

	@Getter
	private LinkedHashMap<String, TabGroup> tabGroups = new LinkedHashMap<>();

	@Getter
	private LinkedHashMap<UUID, TabPlayer> tabPlayers = new LinkedHashMap<>();

	@Getter
	private Map<UUID, BaseTab> playerGroups = new HashMap<>();

	@Getter
	private boolean changeVanilla = true;

	@Getter
	private boolean addToTeam = false;

	@Getter
	private int updateIntervalSeconds = -1;

	@Getter
	private String tabHeader = "";

	@Getter
	private String tabFooter = "";

	private boolean useNicknames = false;

	@Getter
	private boolean attemptPlaceholders = false;

	private static final String UPDATE_TASK_NAME = "tabmanager-S-update-task";

	@Listener
	public void preInit(GamePreInitializationEvent event) {
		this.logger.info("[TabManager " + PluginInfo.VERSION + "] is beginning setup.");
		this.logger.info("[TabManager " + PluginInfo.VERSION + "] uses bStats, not for any nefarious reasons, just because the author (Proxying) is nosey.");
		this.logger.info("[TabManager " + PluginInfo.VERSION + "] Feel free to disable this in the bStats config, but I'd appreciate it if you didn't.");
		instance = this;
		try {
			if (!defaultConfig.exists()) {
				defaultConfig.createNewFile();
				configurationNode = configManager.load();
				configurationNode.getNode(PluginInfo.NAME, "Edit Vanilla Tab List").setComment("This edits the Vanilla tab list (Will not effect the Pixelmon custom tab list, best to disable this when you have Pixelmon's enabled within the pixelmon.hocon.").setValue(true);
				configurationNode.getNode(PluginInfo.NAME, "Add Players to Teams").setComment("This will add players on the server to Teams within the scoreboard, "
						+ "this will override custom tab menus such as the Pixelmon GUI. Allowing for custom prefix/suffix within that. "
						+ "HOWEVER, this will also give the player a prefix and/or suffix while chatting in-game, "
						+ "so a plugin like Nucleus will have to also be used to handle chat if you want it to look different from your options here.").setValue(false);
				configManager.save(configurationNode);
			}
			configurationNode = configManager.load();
			// update config from older versions
			if (configurationNode.getNode(PluginInfo.NAME, "update-interval").isVirtual()) {
				configurationNode.getNode(PluginInfo.NAME, "update-interval").setComment("The interval in seconds in which the tab texts get updated. "
						+ "This is only needed when you are using PlaceholderAPI. Set to -1 to disable the updating.").setValue(5);
				configManager.save(configurationNode);
			}
			if (configurationNode.getNode(PluginInfo.NAME, "Use Player Nicknames").isVirtual()) {
				configurationNode.getNode(PluginInfo.NAME, "Use Player Nicknames").setComment("This will attempt to use a players display name/nickname rather than their actual Minecraft name.").setValue(false);
				configManager.save(configurationNode);
			}
		}
		catch (IOException e) {
			e.printStackTrace();
		}
		this.logger.info("[TabManager " + PluginInfo.VERSION + "] has finished setup.");
	}

	@Listener
	public void onChangeServiceProvider(ChangeServiceProviderEvent event) {
		if (event.getService().equals(PermissionService.class)) {
			permissionService = (PermissionService) event.getNewProviderRegistration().getProvider();
		}
		if (Sponge.getPluginManager().getPlugin("placeholderapi").isPresent()) {
			if (event.getService().equals(PlaceholderService.class)) {
				placeholderService = (PlaceholderService) event.getNewProviderRegistration().getProvider();
			}
		}
	}

	@Listener
	public void aboutToStart(GameAboutToStartServerEvent event) {
		EventManager em = game.getEventManager();
		Sponge.getCommandManager().register(this, CommandSpec.builder()
				.permission("tabmanager.use")
				.executor(new MainCommand())
				.description(Text.of("The main Tab Manager Command"))
				.child(CommandSpec.builder()
						.executor(new ReloadCommand())
						.description(Text.of("Used to refresh the plugin after edits have been made to the config(s)."))
						.build(), "reload")
				.build(), "tab", "tm", "tabs");
		em.registerListeners(this, new PlayerListener());
	}

	@Listener
	public void onServerStart(GameStartedServerEvent event) {
		refreshCache();
		Utilities.scheduleSyncTask(() -> ScoreHandler.getInstance().setup(), 20);
		startUpdateTask();
	}

	@Listener
	public void onServerReload(GameReloadEvent event) {
		refreshCache();
		refreshCurrentPlayers();
		startUpdateTask();
	}

	public void refreshCurrentPlayers() {
		playerGroups.clear();
		ScoreHandler.getInstance().clearTeams();
		for (Player player : Sponge.getServer().getOnlinePlayers()) {
			Utilities.checkAndUpdateName(player);
		}
	}

	public void refreshCache() {
		if (permissionService == null) {
			this.logger.warn("Permission service not present, things will not work...");
		}
		this.changeVanilla = getRootNode().getNode(PluginInfo.NAME, "Edit Vanilla Tab List").getBoolean();
		this.addToTeam = getRootNode().getNode(PluginInfo.NAME, "Add Players to Teams").getBoolean();
		this.updateIntervalSeconds = getRootNode().getNode(PluginInfo.NAME, "update-interval").getInt();
		this.useNicknames = getRootNode().getNode(PluginInfo.NAME, "Use Player Nicknames").getBoolean();
		tabGroups.clear();
		tabPlayers.clear();
		tabHeader = "";
		tabFooter = "";
		if (!Files.exists(Paths.get(this.configDir + File.separator + "vanilla.json"))) {
			try {
				Files.createFile(Paths.get(this.configDir + File.separator + "vanilla.json"));

				Gson gson = new GsonBuilder().setPrettyPrinting().create();
				JsonObject base = new JsonObject();
				JsonObject examples = new JsonObject();

				JsonObject header = new JsonObject();
				examples.add("header", header);
				header.addProperty("type", "Header");
				header.addProperty("display", "This is a header");

				JsonObject footer = new JsonObject();
				examples.add("footer", footer);
				footer.addProperty("type", "Footer");
				footer.addProperty("display", "This is a footer");

				base.add("options", examples);

				String prettyOutput = gson.toJson(base);

				try (FileWriter file = new FileWriter(new File(this.configDir, "vanilla.json"))) {
					file.write(prettyOutput);
					file.flush();
					file.close();
				}
				catch (IOException e) {
					e.printStackTrace();
				}
			}
			catch (IOException e) {
				e.printStackTrace();
			}
		} else {
			try {
				JsonParser parser = new JsonParser();
				JsonObject jsonObject = (JsonObject) parser.parse(new FileReader(new File(this.configDir, "vanilla.json")));
				JsonObject groupsObject = jsonObject.getAsJsonObject("options");
				for (Map.Entry<String, JsonElement> entry : groupsObject.entrySet()) {
					JsonObject singularGroup = (JsonObject) entry.getValue();
					String type = singularGroup.get("type").getAsString();
					if (type == null || type.isEmpty()) {
						continue;
					}
					String display = singularGroup.get("display").getAsString();
					if (type.equalsIgnoreCase("header")) {
						this.tabHeader = display;
					} else if (type.equalsIgnoreCase("footer")) {
						this.tabFooter = display;
					}
					this.logger.info(type.toLowerCase() + " found. Inserting into cache. " + display);
				}
			}
			catch (FileNotFoundException e) {
				e.printStackTrace();
			}
		}
		this.logger.info("Checking for groups file and populating groups. Best of luck kids.");
		if (!Files.exists(Paths.get(this.configDir + File.separator + "groups.json"))) {
			try {
				Files.createFile(Paths.get(this.configDir + File.separator + "groups.json"));
				Gson gson = new GsonBuilder().setPrettyPrinting().create();
				JsonObject base = new JsonObject();
				JsonObject groups = new JsonObject();

				JsonObject defaultGroup = new JsonObject();
				groups.add("HIGHEST_GROUP_(OWNER)", defaultGroup);
				defaultGroup.addProperty("name", "owner");
				defaultGroup.addProperty("prefix", "[OWNER]");
				defaultGroup.addProperty("suffix", "[OWNER]");
				for (int i = 0; i < 3; i++) {
					JsonObject singularGroup = new JsonObject();
					groups.add("LOWER_GROUP_(" + i + ")", singularGroup);
					singularGroup.addProperty("name", "group" + i);
					singularGroup.addProperty("prefix", "[PREFIX]");
					singularGroup.addProperty("suffix", "[SUFFIX]");
				}
				base.add("groupList", groups);

				JsonObject lowestGroup = new JsonObject();
				groups.add("LOWEST_GROUP(DEFAULT)", lowestGroup);
				lowestGroup.addProperty("name", "default");
				lowestGroup.addProperty("prefix", "[DEFAULT]");
				lowestGroup.addProperty("suffix", "[DEFAULT]");

				String prettyOutput = gson.toJson(base);

				try (FileWriter file = new FileWriter(new File(this.configDir, "groups.json"))) {
					file.write(prettyOutput);
					file.flush();
					file.close();
				}
				catch (IOException e) {
					e.printStackTrace();
				}
			}
			catch (IOException e) {
				e.printStackTrace();
			}
		} else {
			try {
				JsonParser parser = new JsonParser();
				JsonObject jsonObject = (JsonObject) parser.parse(new FileReader(new File(this.configDir, "groups.json")));
				JsonObject groupsObject = jsonObject.getAsJsonObject("groupList");
				for (Map.Entry<String, JsonElement> entry : groupsObject.entrySet()) {
					JsonObject singularGroup = (JsonObject) entry.getValue();
					String groupName = singularGroup.get("name").getAsString();
					if (groupName == null || groupName.isEmpty()) {
						continue;
					}
					String groupPrefix = singularGroup.get("prefix").getAsString();
					String groupSuffix = singularGroup.get("suffix").getAsString();
					TabGroup tabGroup = new TabGroup(groupName, groupPrefix, groupSuffix);
					tabGroups.put(groupName.toLowerCase(), tabGroup);
					this.logger.info("Group " + groupName.toLowerCase() + " found. Inserting into cache.");
				}
				if (tabGroups.isEmpty()) {
					this.logger.warn("No groups found within groups.json!");
				}
			}
			catch (FileNotFoundException e) {
				e.printStackTrace();
			}
		}
		this.logger.info("Checking for players file and populating players. Best of luck kids.");
		if (!Files.exists(Paths.get(this.configDir + File.separator + "players.json"))) {
			try {
				Files.createFile(Paths.get(this.configDir + File.separator + "players.json"));
				Gson gson = new GsonBuilder().setPrettyPrinting().create();
				JsonObject base = new JsonObject();
				JsonObject players = new JsonObject();

				JsonObject defaultGroup = new JsonObject();
				players.add("Notch", defaultGroup);
				defaultGroup.addProperty("uuid", "069a79f4-44e9-4726-a5be-fca90e38aaf5");
				defaultGroup.addProperty("prefix", "[NOTCH]");
				defaultGroup.addProperty("suffix", "[NOTCH]");
				base.add("playerList", players);

				String prettyOutput = gson.toJson(base);

				try (FileWriter file = new FileWriter(new File(this.configDir, "players.json"))) {
					file.write(prettyOutput);
					file.flush();
					file.close();
				}
				catch (IOException e) {
					e.printStackTrace();
				}
			}
			catch (IOException e) {
				e.printStackTrace();
			}
		}
		else {
			try {
				JsonParser parser = new JsonParser();
				JsonObject jsonObject = (JsonObject) parser.parse(new FileReader(new File(this.configDir, "players.json")));
				JsonObject playersObject = jsonObject.getAsJsonObject("playerList");
				Map<UUID, TabPlayer> playerMap = new HashMap<>();
				for (Map.Entry<String, JsonElement> entry : playersObject.entrySet()) {
					JsonObject singularGroup = (JsonObject) entry.getValue();
					String playerUUID = singularGroup.get("uuid").getAsString();
					if (playerUUID == null || playerUUID.isEmpty()) {
						continue;
					}
					UUID uuid;
					try {
						uuid = UUID.fromString(playerUUID);
					}
					catch (IllegalArgumentException e) {
						this.logger.warn("Invalid UUID of " + playerUUID + " found.");
						continue;
					}
					String playerPrefix = singularGroup.get("prefix").getAsString();
					String playerSuffix = singularGroup.get("suffix").getAsString();
					TabPlayer tabPlayer = new TabPlayer(uuid, playerPrefix, playerSuffix);
					playerMap.put(uuid, tabPlayer);
					this.logger.info("Player " + playerUUID + " found. Inserting into cache.");
				}
				if (!playerMap.isEmpty()) {
					tabPlayers.putAll(playerMap);
				}
				else {
					this.logger.warn("No groups found within groups.json!");
				}
			}
			catch (FileNotFoundException e) {
				e.printStackTrace();
			}
		}

		if ((tabFooter.contains("%") && tabFooter.indexOf("%") != tabFooter.lastIndexOf("%")) || tabHeader.contains("%") && tabHeader.indexOf("%") != tabHeader.lastIndexOf("%")) {
			attemptPlaceholders = true;
		}
	}

	public ConfigurationNode getRootNode() {
		try {
			return configManager.load();
		}
		catch (IOException e) {
			e.printStackTrace();
		}
		return null;
	}

	private void startUpdateTask() {
		// cancel old tasks
		for (Task task : Sponge.getScheduler().getTasksByName(UPDATE_TASK_NAME)) {
			task.cancel();
		}

		if (updateIntervalSeconds < 1) return;

		Task.builder()
				.name(UPDATE_TASK_NAME)
				.interval(updateIntervalSeconds, TimeUnit.SECONDS)
				.execute(() -> {
					for (Player player : Sponge.getServer().getOnlinePlayers()) {
						Utilities.checkAndUpdateName(player);
					}
				}).submit(this);
	}

	public Logger getLogger() {
		return this.logger;
	}

	public boolean isUseNicknames(Player player) {
		return useNicknames && NucleusAPI.getNicknameService().isPresent() && NucleusAPI.getNicknameService().get().getNickname(player).isPresent();
	}
}
