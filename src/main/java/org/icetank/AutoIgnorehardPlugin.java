package org.icetank;

import org.rusherhack.client.api.RusherHackAPI;
import org.rusherhack.client.api.plugin.Plugin;

/**
 * Example rusherhack plugin
 *
 * @author John200410
 */
public class AutoIgnorehardPlugin extends Plugin {
	
	@Override
	public void onLoad() {
		
		//logger
		this.getLogger().info("AutoIgnorehard plugin loaded!");
		
		//creating and registering a new module
		final AutoIgnorehard autoIgnorehard = new AutoIgnorehard();
		RusherHackAPI.getModuleManager().registerFeature(autoIgnorehard);
	}
	
	@Override
	public void onUnload() {
		this.getLogger().info("AutoIgnorehard plugin unloaded!");
	}
	
}