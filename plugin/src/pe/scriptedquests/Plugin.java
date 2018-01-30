package pe.scriptedquests;

import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

import pe.scriptedquests.commands.QuestTrigger;
import pe.scriptedquests.commands.ReloadQuests;
import pe.scriptedquests.listeners.EntityListener;
import pe.scriptedquests.listeners.PlayerListener;
import pe.scriptedquests.managers.QuestNpcManager;
import pe.scriptedquests.managers.QuestCompassManager;

public class Plugin extends JavaPlugin {
	public QuestCompassManager mQuestCompassManager;
	public QuestNpcManager mNpcManager;

	public World mWorld;

	//	Logic that is performed upon enabling the plugin.
	@Override
	public void onEnable() {
		PluginManager manager = getServer().getPluginManager();

		mWorld = Bukkit.getWorlds().get(0);

		manager.registerEvents(new EntityListener(this), this);
		manager.registerEvents(new PlayerListener(this), this);

		getCommand("reloadQuests").setExecutor(new ReloadQuests(this));
		getCommand("questTrigger").setExecutor(new QuestTrigger(this));

		mQuestCompassManager = new QuestCompassManager(this);
		mNpcManager = new QuestNpcManager(this);
	}

	//	Logic that is performed upon disabling the plugin.
	@Override
	public void onDisable() {
		getServer().getScheduler().cancelTasks(this);
	}
}
