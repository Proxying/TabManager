package uk.co.proxying.tabmanager.tabObjects;

/**
 * Created by Kieran Quigley (Proxying) on 14-Jan-17.
 */
public class TabGroup implements BaseTab {

	public String groupName;
	public String prefix;
	public String suffix;

	public TabGroup(String groupName, String prefix, String suffix) {
		this.groupName = groupName;
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
