package uk.co.proxying.tabmanager.commands;

import org.spongepowered.api.command.CommandException;
import org.spongepowered.api.command.CommandResult;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.command.args.CommandContext;
import org.spongepowered.api.command.spec.CommandExecutor;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.format.TextColors;
import uk.co.proxying.tabmanager.PluginInfo;

/**
 * Created by Kieran Quigley (Proxying) on 15-Jan-17.
 */
public class MainCommand implements CommandExecutor {
	@Override public CommandResult execute(CommandSource src, CommandContext args) throws CommandException {
		src.sendMessage(Text.of(TextColors.GREEN, "Uhm, this currently doesn't do anything. But for something to read..."));
		src.sendMessage(Text.of(TextColors.GREEN, "Current plugin version: " + PluginInfo.VERSION));
		return CommandResult.success();
	}
}
