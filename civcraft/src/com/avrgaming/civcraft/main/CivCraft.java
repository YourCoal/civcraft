/*************************************************************************
 * 
 * AVRGAMING LLC
 * __________________
 * 
 *  [2013] AVRGAMING LLC
 *  All Rights Reserved.
 * 
 * NOTICE:  All information contained herein is, and remains
 * the property of AVRGAMING LLC and its suppliers,
 * if any.  The intellectual and technical concepts contained
 * herein are proprietary to AVRGAMING LLC
 * and its suppliers and may be covered by U.S. and Foreign Patents,
 * patents in process, and are protected by trade secret or copyright law.
 * Dissemination of this information or reproduction of this material
 * is strictly forbidden unless prior written permission is obtained
 * from AVRGAMING LLC.
 */
package com.avrgaming.civcraft.main;

import java.io.IOException;
import java.sql.SQLException;

import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

import com.avrgaming.anticheat.ACManager;
import com.avrgaming.civcraft.arena.ArenaListener;
import com.avrgaming.civcraft.command.AcceptCommand;
import com.avrgaming.civcraft.command.BuildCommand;
import com.avrgaming.civcraft.command.DenyCommand;
import com.avrgaming.civcraft.command.EconCommand;
import com.avrgaming.civcraft.command.HereCommand;
import com.avrgaming.civcraft.command.PayCommand;
import com.avrgaming.civcraft.command.ReportCommand;
import com.avrgaming.civcraft.command.SelectCommand;
import com.avrgaming.civcraft.command.TradeCommand;
import com.avrgaming.civcraft.command.VoteCommand;
import com.avrgaming.civcraft.command.admin.AdminCommand;
import com.avrgaming.civcraft.command.camp.CampCommand;
import com.avrgaming.civcraft.command.civ.CivChatCommand;
import com.avrgaming.civcraft.command.civ.CivCommand;
import com.avrgaming.civcraft.command.debug.DebugCommand;
import com.avrgaming.civcraft.command.market.MarketCommand;
import com.avrgaming.civcraft.command.mod.ModeratorCommand;
import com.avrgaming.civcraft.command.plot.PlotCommand;
import com.avrgaming.civcraft.command.resident.ResidentCommand;
import com.avrgaming.civcraft.command.town.TownChatCommand;
import com.avrgaming.civcraft.command.town.TownCommand;
import com.avrgaming.civcraft.config.CivSettings;
import com.avrgaming.civcraft.database.SQL;
import com.avrgaming.civcraft.database.SQLUpdate;
import com.avrgaming.civcraft.endgame.EndConditionNotificationTask;
import com.avrgaming.civcraft.event.EventTimerTask;
import com.avrgaming.civcraft.exception.CivException;
import com.avrgaming.civcraft.exception.InvalidConfiguration;
import com.avrgaming.civcraft.listener.BlockListener;
import com.avrgaming.civcraft.listener.BonusGoodieManager;
import com.avrgaming.civcraft.listener.ChatListener;
import com.avrgaming.civcraft.listener.CustomItemManager;
import com.avrgaming.civcraft.listener.DebugListener;
import com.avrgaming.civcraft.listener.DisableXPListener;
import com.avrgaming.civcraft.listener.MarkerPlacementManager;
import com.avrgaming.civcraft.listener.PlayerListener;
import com.avrgaming.civcraft.listener.TagAPIListener;
import com.avrgaming.civcraft.lorestorage.LoreCraftableMaterialListener;
import com.avrgaming.civcraft.lorestorage.LoreGuiItemListener;
import com.avrgaming.civcraft.mobs.MobSpawner;
import com.avrgaming.civcraft.mobs.listeners.MobListener;
import com.avrgaming.civcraft.mobs.timers.MobSpawnerTimer;
import com.avrgaming.civcraft.nocheat.NoCheatPlusSurvialFlyHandler;
import com.avrgaming.civcraft.populators.TradeGoodPopulator;
import com.avrgaming.civcraft.pvplogger.PvPLogger;
import com.avrgaming.civcraft.randomevents.RandomEventSweeper;
import com.avrgaming.civcraft.sessiondb.SessionDBAsyncTimer;
import com.avrgaming.civcraft.structure.Farm;
import com.avrgaming.civcraft.structure.farm.FarmGrowthSyncTask;
import com.avrgaming.civcraft.structure.farm.FarmPreCachePopulateTimer;
import com.avrgaming.civcraft.structurevalidation.StructureValidationChecker;
import com.avrgaming.civcraft.structurevalidation.StructureValidationPunisher;
import com.avrgaming.civcraft.threading.TaskMaster;
import com.avrgaming.civcraft.threading.sync.SyncBuildUpdateTask;
import com.avrgaming.civcraft.threading.sync.SyncGetChestInventory;
import com.avrgaming.civcraft.threading.sync.SyncGrowTask;
import com.avrgaming.civcraft.threading.sync.SyncLoadChunk;
import com.avrgaming.civcraft.threading.sync.SyncUpdateChunks;
import com.avrgaming.civcraft.threading.sync.SyncUpdateInventory;
import com.avrgaming.civcraft.threading.tasks.ArrowProjectileTask;
import com.avrgaming.civcraft.threading.tasks.ProjectileComponentTimer;
import com.avrgaming.civcraft.threading.tasks.ScoutTowerTask;
import com.avrgaming.civcraft.threading.timers.AnnouncementTimer;
import com.avrgaming.civcraft.threading.timers.BeakerTimer;
import com.avrgaming.civcraft.threading.timers.ChangeGovernmentTimer;
import com.avrgaming.civcraft.threading.timers.PlayerLocationCacheUpdate;
import com.avrgaming.civcraft.threading.timers.PlayerProximityComponentTimer;
import com.avrgaming.civcraft.threading.timers.ReduceExposureTimer;
import com.avrgaming.civcraft.threading.timers.RegenTimer;
import com.avrgaming.civcraft.threading.timers.UnitTrainTimer;
import com.avrgaming.civcraft.threading.timers.UpdateEventTimer;
import com.avrgaming.civcraft.threading.timers.WindmillTimer;
import com.avrgaming.civcraft.trade.TradeInventoryListener;
import com.avrgaming.civcraft.util.BukkitObjects;
import com.avrgaming.civcraft.util.ChunkCoord;
import com.avrgaming.civcraft.util.TimeTools;
import com.avrgaming.global.perks.PlatinumManager;
import com.avrgaming.global.scores.CalculateScoreTimer;
import com.avrgaming.global.serverstatus.ServerStatusUpdateTimer;
import com.avrgaming.moblib.MobLib;

import fr.neatmonster.nocheatplus.checks.CheckType;
import fr.neatmonster.nocheatplus.hooks.NCPHookManager;

public final class CivCraft extends JavaPlugin {

	private boolean isError = false;
	private final PlayerListener civPlayerListener = new PlayerListener();
	private final BlockListener civBlockListener = new BlockListener();
	private final ChatListener civChatListener = new ChatListener();
	private final BonusGoodieManager civBonusGoodieManager = new BonusGoodieManager();
	private final MarkerPlacementManager civMarkerPlacementManager = new MarkerPlacementManager();
	private final CustomItemManager customManager = new CustomItemManager();
	private final DebugListener civDebugListener = new DebugListener();
	private final LoreCraftableMaterialListener civCraftableListener = new LoreCraftableMaterialListener();
	private final LoreGuiItemListener civGuiItemListener = new LoreGuiItemListener();
	private final TradeInventoryListener civTradeInventoryListener = new TradeInventoryListener();
	private final TagAPIListener civTagAPIListener = new TagAPIListener();
//	private CreativeInventoryPacketManager creativeInvPacketManager = new CreativeInventoryPacketManager();
	
	private static JavaPlugin plugin;	
	
	private void startTimers() {
		
		TaskMaster.asyncTimer("SQLUpdate", new SQLUpdate(), 5);
		
		// Sync Timers
		TaskMaster.syncTimer(SyncBuildUpdateTask.class.getName(), 
				new SyncBuildUpdateTask(), 0, 1);
		
		TaskMaster.syncTimer(SyncUpdateChunks.class.getName(), 
				new SyncUpdateChunks(), 0, TimeTools.toTicks(1));
		
		TaskMaster.syncTimer(SyncLoadChunk.class.getName(), 
				new SyncLoadChunk(), 0, 1);
		
		TaskMaster.syncTimer(SyncGetChestInventory.class.getName(),
				new SyncGetChestInventory(), 0, 1);
		
		TaskMaster.syncTimer(SyncUpdateInventory.class.getName(),
				new SyncUpdateInventory(), 0, 1);
		
		TaskMaster.syncTimer(SyncGrowTask.class.getName(),
				new SyncGrowTask(), 0, 1);
		
		TaskMaster.syncTimer(PlayerLocationCacheUpdate.class.getName(), 
				new PlayerLocationCacheUpdate(), 0, 10);
		
		TaskMaster.asyncTimer("RandomEventSweeper", new RandomEventSweeper(), 0, TimeTools.toTicks(10));
		
		// Structure event timers
		TaskMaster.asyncTimer("UpdateEventTimer", new UpdateEventTimer(), TimeTools.toTicks(1));
		TaskMaster.asyncTimer("RegenTimer", new RegenTimer(), TimeTools.toTicks(5));

		TaskMaster.asyncTimer("BeakerTimer", new BeakerTimer(60), TimeTools.toTicks(60));
		TaskMaster.syncTimer("UnitTrainTimer", new UnitTrainTimer(), TimeTools.toTicks(1));
		TaskMaster.asyncTimer("ReduceExposureTimer", new ReduceExposureTimer(), 0, TimeTools.toTicks(5));

		try {
			double arrow_firerate = CivSettings.getDouble(CivSettings.warConfig, "arrow_tower.fire_rate");
			TaskMaster.syncTimer("arrowTower", new ProjectileComponentTimer(), (int)(arrow_firerate*20));
			
//			double cannon_firerate = CivSettings.getDouble(CivSettings.warConfig, "cannon_tower.fire_rate");
//			TaskMaster.syncTimer("cannonTower", new CannonTowerTask(), (int)(cannon_firerate*20));
			
			TaskMaster.asyncTimer("ScoutTowerTask", new ScoutTowerTask(), TimeTools.toTicks(1));
			
		} catch (InvalidConfiguration e) {
			e.printStackTrace();
			return;
		}
		TaskMaster.syncTimer("arrowhomingtask", new ArrowProjectileTask(), 5);
			
		// Global Event timers		
		TaskMaster.syncTimer("FarmCropCache", new FarmPreCachePopulateTimer(), TimeTools.toTicks(30));
	
		TaskMaster.asyncTimer("FarmGrowthTimer",
				new FarmGrowthSyncTask(), TimeTools.toTicks(Farm.GROW_RATE));

		TaskMaster.asyncTimer("announcer", new AnnouncementTimer("tips.txt"), 0, TimeTools.toTicks(60*60));
		
		TaskMaster.asyncTimer("ChangeGovernmentTimer", new ChangeGovernmentTimer(), TimeTools.toTicks(60));
		TaskMaster.asyncTimer("CalculateScoreTimer", new CalculateScoreTimer(), 0, TimeTools.toTicks(60));
		
		TaskMaster.asyncTimer(PlayerProximityComponentTimer.class.getName(), 
				new PlayerProximityComponentTimer(), TimeTools.toTicks(1));
		
		TaskMaster.asyncTimer(ServerStatusUpdateTimer.class.getName(), new ServerStatusUpdateTimer(), TimeTools.toTicks(5));
		TaskMaster.asyncTimer(EventTimerTask.class.getName(), new EventTimerTask(), TimeTools.toTicks(5));

		TaskMaster.asyncTimer(PlatinumManager.class.getName(), new PlatinumManager(), TimeTools.toTicks(5));
		
		TaskMaster.syncTimer("PvPLogger", new PvPLogger(), TimeTools.toTicks(5));
		TaskMaster.syncTimer("WindmillTimer", new WindmillTimer(), TimeTools.toTicks(60));
		TaskMaster.asyncTimer("EndGameNotification", new EndConditionNotificationTask(), TimeTools.toTicks(3600));
		
		// Debug version
	//	TaskMaster.asyncTask(new StructureValidationChecker(), TimeTools.toTicks(30));
	//	TaskMaster.asyncTimer("StructureValidationPunisher", new StructureValidationPunisher(), TimeTools.toTicks(30));
		
		// Production version
		TaskMaster.asyncTask(new StructureValidationChecker(), TimeTools.toTicks(120));
		TaskMaster.asyncTimer("StructureValidationPunisher", new StructureValidationPunisher(), TimeTools.toTicks(3600));
		TaskMaster.asyncTimer("SessionDBAsyncTimer", new SessionDBAsyncTimer(), 10);
		
		//TaskMaster.syncTimer("Apoc", new Apocalypse(), TimeTools.toTicks(1200));
		TaskMaster.syncTimer("MobSpawner", new MobSpawnerTimer(), TimeTools.toTicks(2));
	}
	
	private void registerEvents() {
		final PluginManager pluginManager = getServer().getPluginManager();
		pluginManager.registerEvents(civBlockListener, this);
		pluginManager.registerEvents(civChatListener, this);
		pluginManager.registerEvents(civBonusGoodieManager, this);
		pluginManager.registerEvents(civMarkerPlacementManager, this);
		pluginManager.registerEvents(customManager, this);
		pluginManager.registerEvents(civPlayerListener, this);		
		pluginManager.registerEvents(civDebugListener, this);
		pluginManager.registerEvents(civCraftableListener, this);
		pluginManager.registerEvents(civGuiItemListener, this);
		pluginManager.registerEvents(new DisableXPListener(), this);
		pluginManager.registerEvents(new PvPLogger(), this);
		pluginManager.registerEvents(civTradeInventoryListener, this);
		pluginManager.registerEvents(civTagAPIListener, this);
		pluginManager.registerEvents(new MobListener(), this);
		pluginManager.registerEvents(new ArenaListener(), this);
	}
	
	private void registerNPCHooks() {
		NCPHookManager.addHook(CheckType.MOVING_SURVIVALFLY, new NoCheatPlusSurvialFlyHandler());
	}
	
	@Override
	public void onEnable() {
		setPlugin(this);
		this.saveDefaultConfig();
		
		CivLog.init(this);
		BukkitObjects.initialize(this);
		
		//Load World Populators
		BukkitObjects.getWorlds().get(0).getPopulators().add(new TradeGoodPopulator());
		
		try {
			CivSettings.init(this);
			SQL.initialize();
			SQL.initCivObjectTables();
			ChunkCoord.buildWorldList();
			CivGlobal.loadGlobals();
			ACManager.init();

		} catch (InvalidConfiguration | SQLException | IOException | InvalidConfigurationException | CivException | ClassNotFoundException e) {
			e.printStackTrace();
			setError(true);
			return;
			//TODO disable plugin?
		}
		
		// Init commands
		getCommand("town").setExecutor(new TownCommand());
		getCommand("resident").setExecutor(new ResidentCommand());
		getCommand("dbg").setExecutor(new DebugCommand());
		getCommand("plot").setExecutor(new PlotCommand());
		getCommand("accept").setExecutor(new AcceptCommand());
		getCommand("deny").setExecutor(new DenyCommand());
		getCommand("civ").setExecutor(new CivCommand());
		getCommand("tc").setExecutor(new TownChatCommand());
		getCommand("cc").setExecutor(new CivChatCommand());
		//getCommand("gc").setExecutor(new GlobalChatCommand());
		getCommand("ad").setExecutor(new AdminCommand());
		getCommand("econ").setExecutor(new EconCommand());
		getCommand("pay").setExecutor(new PayCommand());
		getCommand("build").setExecutor(new BuildCommand());
		getCommand("market").setExecutor(new MarketCommand());
		getCommand("select").setExecutor(new SelectCommand());
		getCommand("mod").setExecutor(new ModeratorCommand());
		getCommand("here").setExecutor(new HereCommand());
		getCommand("camp").setExecutor(new CampCommand());
		getCommand("report").setExecutor(new ReportCommand());
		getCommand("vote").setExecutor(new VoteCommand());
		getCommand("trade").setExecutor(new TradeCommand());

		registerEvents();
		registerNPCHooks();
		MobLib.registerAllEntities();
		startTimers();
		MobSpawner.register();
				
		//creativeInvPacketManager.init(this);		
	}
	
	@Override
	public void onDisable() {
		MobSpawner.despawnAll();
	}

	public boolean isError() {
		return isError;
	}

	public void setError(boolean isError) {
		this.isError = isError;
	}


	public static JavaPlugin getPlugin() {
		return plugin;
	}


	public static void setPlugin(JavaPlugin plugin) {
		CivCraft.plugin = plugin;
	}


	
	
}