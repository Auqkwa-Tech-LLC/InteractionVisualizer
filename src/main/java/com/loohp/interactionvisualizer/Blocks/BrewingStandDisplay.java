package com.loohp.interactionvisualizer.Blocks;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;

import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.EulerAngle;
import org.bukkit.util.Vector;

import com.loohp.interactionvisualizer.InteractionVisualizer;
import com.loohp.interactionvisualizer.API.VisualizerRunnableDisplay;
import com.loohp.interactionvisualizer.API.Events.InteractionVisualizerReloadEvent;
import com.loohp.interactionvisualizer.EntityHolders.ArmorStand;
import com.loohp.interactionvisualizer.EntityHolders.Item;
import com.loohp.interactionvisualizer.Managers.PacketManager;
import com.loohp.interactionvisualizer.Managers.PlayerLocationManager;
import com.loohp.interactionvisualizer.Managers.TileEntityManager;
import com.loohp.interactionvisualizer.Managers.TileEntityManager.TileEntityType;
import com.loohp.interactionvisualizer.Utils.ChatColorUtils;

public class BrewingStandDisplay extends VisualizerRunnableDisplay implements Listener {
	
	public ConcurrentHashMap<Block, HashMap<String, Object>> brewstand = new ConcurrentHashMap<Block, HashMap<String, Object>>();
	public final int max = 20 * 20;
	private int checkingPeriod = 20;
	private int gcPeriod = 600;
	private String progressBarCharacter = "";
	private String emptyColor = "&7";
	private String filledColor = "&e";
	private String noFuelColor = "&c";
	private int progressBarLength = 10;
	
	public BrewingStandDisplay() {
		onReload(new InteractionVisualizerReloadEvent());
	}
	
	@EventHandler
	public void onReload(InteractionVisualizerReloadEvent event) {
		checkingPeriod = InteractionVisualizer.plugin.getConfig().getInt("Blocks.BrewingStand.CheckingPeriod");
		gcPeriod = InteractionVisualizer.plugin.getConfig().getInt("GarbageCollector.Period");
		progressBarCharacter = ChatColorUtils.translateAlternateColorCodes('&', InteractionVisualizer.plugin.getConfig().getString("Blocks.BrewingStand.Options.ProgressBarCharacter"));
		emptyColor = ChatColorUtils.translateAlternateColorCodes('&', InteractionVisualizer.plugin.getConfig().getString("Blocks.BrewingStand.Options.EmptyColor"));
		filledColor = ChatColorUtils.translateAlternateColorCodes('&', InteractionVisualizer.plugin.getConfig().getString("Blocks.BrewingStand.Options.FilledColor"));
		noFuelColor = ChatColorUtils.translateAlternateColorCodes('&', InteractionVisualizer.plugin.getConfig().getString("Blocks.BrewingStand.Options.NoFuelColor"));
		progressBarLength = InteractionVisualizer.plugin.getConfig().getInt("Blocks.BrewingStand.Options.ProgressBarLength");
	}
	
	@Override
	public int gc() {
		return Bukkit.getScheduler().runTaskTimerAsynchronously(InteractionVisualizer.plugin, () -> {
			Iterator<Entry<Block, HashMap<String, Object>>> itr = brewstand.entrySet().iterator();
			int count = 0;
			int maxper = (int) Math.ceil((double) brewstand.size() / (double) gcPeriod);
			int delay = 1;
			while (itr.hasNext()) {
				count++;
				if (count > maxper) {
					count = 0;
					delay++;
				}
				Entry<Block, HashMap<String, Object>> entry = itr.next();
				Bukkit.getScheduler().runTaskLater(InteractionVisualizer.plugin, () -> {
					Block block = entry.getKey();
					boolean active = false;
					if (isActive(block.getLocation())) {
						active = true;
					}
					if (!active) {
						HashMap<String, Object> map = entry.getValue();
						if (map.get("Item") instanceof Item) {
							Item item = (Item) map.get("Item");
							PacketManager.removeItem(InteractionVisualizer.getOnlinePlayers(), item);
						}
						if (map.get("Stand") instanceof ArmorStand) {
							ArmorStand stand = (ArmorStand) map.get("Stand");
							PacketManager.removeArmorStand(InteractionVisualizer.getOnlinePlayers(), stand);
						}
						brewstand.remove(block);
						return;
					}
					if (!block.getType().equals(Material.BREWING_STAND)) {
						HashMap<String, Object> map = entry.getValue();
						if (map.get("Item") instanceof Item) {
							Item item = (Item) map.get("Item");
							PacketManager.removeItem(InteractionVisualizer.getOnlinePlayers(), item);
						}
						if (map.get("Stand") instanceof ArmorStand) {
							ArmorStand stand = (ArmorStand) map.get("Stand");
							PacketManager.removeArmorStand(InteractionVisualizer.getOnlinePlayers(), stand);
						}
						brewstand.remove(block);
						return;
					}
				}, delay);
			}
		}, 0, gcPeriod).getTaskId();
	}
	
	@Override
	public int run() {		
		return Bukkit.getScheduler().runTaskTimerAsynchronously(InteractionVisualizer.plugin, () -> {
			Bukkit.getScheduler().runTask(InteractionVisualizer.plugin, () -> {
				List<Block> list = nearbyBrewingStand();
				for (Block block : list) {
					if (brewstand.get(block) == null && isActive(block.getLocation())) {
						if (block.getType().equals(Material.BREWING_STAND)) {
							HashMap<String, Object> map = new HashMap<String, Object>();
							map.put("Item", "N/A");
							map.putAll(spawnArmorStands(block));
							brewstand.put(block, map);
						}
					}
				}
			});
			
			Iterator<Entry<Block, HashMap<String, Object>>> itr = brewstand.entrySet().iterator();
			int count = 0;
			int maxper = (int) Math.ceil((double) brewstand.size() / (double) checkingPeriod);
			int delay = 1;
			while (itr.hasNext()) {
				Entry<Block, HashMap<String, Object>> entry = itr.next();
				
				count++;
				if (count > maxper) {
					count = 0;
					delay++;
				}
				Bukkit.getScheduler().runTaskLater(InteractionVisualizer.plugin, () -> {
					Block block = entry.getKey();
					if (!isActive(block.getLocation())) {
						return;
					}
					if (!block.getType().equals(Material.BREWING_STAND)) {
						return;
					}
					org.bukkit.block.BrewingStand brewingstand = (org.bukkit.block.BrewingStand) block.getState();

					Bukkit.getScheduler().runTaskAsynchronously(InteractionVisualizer.plugin, () -> {
						Inventory inv = brewingstand.getInventory();
						ItemStack itemstack = inv.getItem(3);
						if (itemstack != null) {
							if (inv.getItem(3).getType().equals(Material.AIR)) {
								itemstack = null;
							}
						}
						
						Item item = null;
						if (entry.getValue().get("Item") instanceof String) {
							if (itemstack != null) {
								item = new Item(brewingstand.getLocation().clone().add(0.5, 1.0, 0.5));
								item.setItemStack(itemstack);
								item.setVelocity(new Vector(0, 0, 0));
								item.setPickupDelay(32767);
								item.setGravity(false);
								entry.getValue().put("Item", item);
								PacketManager.sendItemSpawn(InteractionVisualizer.itemDrop, item);
								PacketManager.updateItem(item);
							} else {
								entry.getValue().put("Item", "N/A");
							}
						} else {
							item = (Item) entry.getValue().get("Item");
							if (itemstack != null) {
								if (!item.getItemStack().equals(itemstack)) {
									item.setItemStack(itemstack);
									PacketManager.updateItem(item);
								}
								item.setPickupDelay(32767);
								item.setGravity(false);
							} else {
								entry.getValue().put("Item", "N/A");
								PacketManager.removeItem(InteractionVisualizer.getOnlinePlayers(), item);
							}
						}
	
						if (brewingstand.getFuelLevel() == 0) {
							ArmorStand stand = (ArmorStand) entry.getValue().get("Stand");
							if (hasPotion(brewingstand)) {
								stand.setCustomNameVisible(true);
								String name = noFuelColor;
								for (int i = 0; i < progressBarLength; i++) {
									name += progressBarCharacter;
								}
								stand.setCustomName(name);
								PacketManager.updateArmorStand(stand);
							} else {
								stand.setCustomNameVisible(false);
								stand.setCustomName("");
								PacketManager.updateArmorStand(stand);
							}
						} else {					
							ArmorStand stand = (ArmorStand) entry.getValue().get("Stand");
							if (hasPotion(brewingstand)) {
								int time = brewingstand.getBrewingTime();					
								String symbol = "";
								double percentagescaled = (double) (max - time) / (double) max * (double) progressBarLength;
								double i = 1;
								for (i = 1; i < percentagescaled; i++) {
									symbol += filledColor + progressBarCharacter;
								}
								i = i - 1;
								if ((percentagescaled - i) > 0 && (percentagescaled - i) < 0.33) {
									symbol += emptyColor + progressBarCharacter;
								} else if ((percentagescaled - i) > 0 && (percentagescaled - i) < 0.67) {
									symbol += emptyColor + progressBarCharacter;
								} else if ((percentagescaled - i) > 0) {
									symbol += filledColor + progressBarCharacter;
								}
								for (i = progressBarLength - 1; i >= percentagescaled; i--) {
									symbol += emptyColor + progressBarCharacter;
								}
								if (!stand.getCustomName().equals(symbol) || !stand.isCustomNameVisible()) {
									stand.setCustomNameVisible(true);
									stand.setCustomName(symbol);
									PacketManager.updateArmorStandOnlyMeta(stand);
								}
							} else {
								if (!stand.getCustomName().equals("") || stand.isCustomNameVisible()) {
									stand.setCustomNameVisible(false);
									stand.setCustomName("");
									PacketManager.updateArmorStandOnlyMeta(stand);
								}
							}
						}
					});
				}, delay);
			}
		}, 0, checkingPeriod).getTaskId();		
	}
	
	@EventHandler(priority=EventPriority.MONITOR)
	public void onUseBrewingStand(InventoryClickEvent event) {
		if (event.isCancelled()) {
			return;
		}
		if (event.getWhoClicked().getGameMode().equals(GameMode.SPECTATOR)) {
			return;
		}
		if (event.getView().getTopInventory() == null) {
			return;
		}
		try {
			if (event.getView().getTopInventory().getLocation() == null) {
				return;
			}
		} catch (Exception | AbstractMethodError e) {
			return;
		}
		if (event.getView().getTopInventory().getLocation().getBlock() == null) {
			return;
		}
		if (!event.getView().getTopInventory().getLocation().getBlock().getType().equals(Material.BREWING_STAND)) {
			return;
		}
		
		if (event.getRawSlot() >= 0 && event.getRawSlot() <= 4) {
			PacketManager.sendHandMovement(InteractionVisualizer.getOnlinePlayers(), (Player) event.getWhoClicked());
		}
	}
	
	@EventHandler(priority=EventPriority.MONITOR)
	public void onDragBrewingStand(InventoryDragEvent event) {
		if (event.isCancelled()) {
			return;
		}
		if (event.getWhoClicked().getGameMode().equals(GameMode.SPECTATOR)) {
			return;
		}
		if (event.getView().getTopInventory() == null) {
			return;
		}
		try {
			if (event.getView().getTopInventory().getLocation() == null) {
				return;
			}
		} catch (Exception | AbstractMethodError e) {
			return;
		}
		if (event.getView().getTopInventory().getLocation().getBlock() == null) {
			return;
		}
		if (!event.getView().getTopInventory().getLocation().getBlock().getType().equals(Material.BREWING_STAND)) {
			return;
		}
		
		for (int slot : event.getRawSlots()) {
			if (slot >= 0 && slot <= 4) {
				PacketManager.sendHandMovement(InteractionVisualizer.getOnlinePlayers(), (Player) event.getWhoClicked());
				break;
			}
		}
	}
	
	@EventHandler(priority=EventPriority.MONITOR)
	public void onBreakBrewingStand(BlockBreakEvent event) {
		if (event.isCancelled()) {
			return;
		}
		Block block = event.getBlock();
		if (!brewstand.containsKey(block)) {
			return;
		}

		HashMap<String, Object> map = brewstand.get(block);
		if (map.get("Item") instanceof Item) {
			Item item = (Item) map.get("Item");
			PacketManager.removeItem(InteractionVisualizer.getOnlinePlayers(), item);
		}
		if (map.get("Stand") instanceof ArmorStand) {
			ArmorStand stand = (ArmorStand) map.get("Stand");
			PacketManager.removeArmorStand(InteractionVisualizer.getOnlinePlayers(), stand);
		}
		brewstand.remove(block);
	}
	
	public boolean hasPotion(org.bukkit.block.BrewingStand brewingstand) {
		Inventory inv = brewingstand.getInventory();
		if (inv.getItem(0) != null) {
			if (!inv.getItem(0).getType().equals(Material.AIR)) {
				return true;
			}
		}
		if (inv.getItem(1) != null) {
			if (!inv.getItem(1).getType().equals(Material.AIR)) {
				return true;
			}
		}
		if (inv.getItem(2) != null) {
			if (!inv.getItem(2).getType().equals(Material.AIR)) {
				return true;
			}
		}
		return false;
	}
	
	public List<Block> nearbyBrewingStand() {
		return TileEntityManager.getTileEntites(TileEntityType.BREWING_STAND);
	}
	
	public boolean isActive(Location loc) {
		return PlayerLocationManager.hasPlayerNearby(loc);
	}
	
	public HashMap<String, ArmorStand> spawnArmorStands(Block block) { //.add(0.68, 0.700781, 0.35)
		HashMap<String, ArmorStand> map = new HashMap<String, ArmorStand>();
		Location loc = block.getLocation().clone().add(0.5, 0.700781, 0.5);
		ArmorStand slot1 = new ArmorStand(loc.clone());
		setStand(slot1);
		
		map.put("Stand", slot1);
		
		PacketManager.sendArmorStandSpawn(InteractionVisualizer.holograms, slot1);
		
		return map;
	}
	
	public void setStand(ArmorStand stand) {
		stand.setBasePlate(false);
		stand.setMarker(true);
		stand.setGravity(false);
		stand.setSmall(true);
		stand.setInvulnerable(true);
		stand.setSilent(true);
		stand.setVisible(false);
		stand.setCustomName("");
		stand.setRightArmPose(new EulerAngle(0.0, 0.0, 0.0));
	}

}
