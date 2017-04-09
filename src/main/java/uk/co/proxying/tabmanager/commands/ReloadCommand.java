package uk.co.proxying.tabmanager.commands;

import org.spongepowered.api.command.CommandException;
import org.spongepowered.api.command.CommandResult;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.command.args.CommandContext;
import org.spongepowered.api.command.spec.CommandExecutor;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.format.TextColors;
import uk.co.proxying.tabmanager.PluginInfo;
import uk.co.proxying.tabmanager.TabManager;

/**
 * Created by Kieran Quigley (Proxying) on 15-Jan-17.
 */
public class ReloadCommand implements CommandExecutor {

	@Override
	public CommandResult execute(CommandSource src, CommandContext args) throws CommandException {
		TabManager.getInstance().refreshCache();
		TabManager.getInstance().refreshCurrentPlayers();
		src.sendMessage(Text.of(TextColors.GREEN, PluginInfo.NAME + " has been refreshed."));
		return CommandResult.success();
	}
}
