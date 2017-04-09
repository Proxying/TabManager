package uk.co.proxying.tabmanager.tabObjects;

import java.util.UUID;

/**
 * Created by Kieran (Proxying) on 19-Jan-17.
 */
public class TabPlayer implements BaseTab {

	public UUID playerUUID;
	public String prefix;
	public String suffix;

	public TabPlayer(UUID playerUUID, String prefix, String suffix) {
		this.playerUUID = playerUUID;
		this.prefix = prefix;
		this.suffix = suffix;
	}

	@Override public String getPrefix() {
		return prefix;
	}

	@Override public String getSuffix() {
		return suffix;
	}
}
