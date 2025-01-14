package com.loohp.interactionvisualizer.Managers;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.Plugin;

import com.loohp.interactionvisualizer.InteractionVisualizer;
import com.loohp.interactionvisualizer.EntityHolders.ArmorStand;
import com.loohp.interactionvisualizer.EntityHolders.Item;
import com.loohp.interactionvisualizer.EntityHolders.ItemFrame;
import com.loohp.interactionvisualizer.EntityHolders.VisualizerEntity;
import com.loohp.interactionvisualizer.Protocol.ServerPacketSender;

public class PacketManager implements Listener {
	
	private static Plugin plugin = InteractionVisualizer.plugin;
	
	public static ConcurrentHashMap<VisualizerEntity, List<Player>> active = new ConcurrentHashMap<VisualizerEntity, List<Player>>();
	public static ConcurrentHashMap<VisualizerEntity, Boolean> loaded = new ConcurrentHashMap<VisualizerEntity, Boolean>();
	private static ConcurrentHashMap<VisualizerEntity, Integer> cache = new ConcurrentHashMap<VisualizerEntity, Integer>();
	
	public static ConcurrentHashMap<Player, Set<VisualizerEntity>> playerStatus = new ConcurrentHashMap<Player, Set<VisualizerEntity>>();
	
	public static void run() {
		if (!plugin.isEnabled()) {
			return;
		}
		Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
			Iterator<Entry<VisualizerEntity, Boolean>> itr = loaded.entrySet().iterator();
			while (itr.hasNext()) {
				Entry<VisualizerEntity, Boolean> entry = itr.next();
				VisualizerEntity entity = entry.getKey();
				if (entity instanceof ArmorStand) {
					ArmorStand stand = (ArmorStand) entity;
					if (!PlayerLocationManager.hasPlayerNearby(stand.getLocation())) {
						continue;
					}
					if (entry.getValue()) {
						if (!plugin.isEnabled()) {
							return;
						}
						Bukkit.getScheduler().runTask(plugin, () -> {		
							List<Player> players = active.get(entity);
							if (players == null) {
								return;
							}
							if (isOccluding(stand.getLocation().getBlock().getType())) {
								removeArmorStand(InteractionVisualizer.getOnlinePlayers(), stand, false, false);
								loaded.put(entity, false);
							}
						});
					} else {
						Bukkit.getScheduler().runTask(plugin, () -> {
							if (!PlayerLocationManager.hasPlayerNearby(stand.getLocation())) {
								return;
							}
							List<Player> players = active.get(entity);
							if (players == null) {
								return;
							}
							if (!isOccluding(stand.getLocation().getBlock().getType())) {
								sendArmorStandSpawn(players, stand);
								updateArmorStand(stand);
								loaded.put(entity, true);
							}
						});
					}
				} else if (entity instanceof Item) {
					Item item = (Item) entity;
					if (!PlayerLocationManager.hasPlayerNearby(item.getLocation())) {
						continue;
					}
					if (entry.getValue()) {
						if (!plugin.isEnabled()) {
							return;
						}
						Bukkit.getScheduler().runTask(plugin, () -> {
							List<Player> players = active.get(entity);
							if (players == null) {
								return;
							}
							if (isOccluding(item.getLocation().getBlock().getType())) {
								removeItem(InteractionVisualizer.getOnlinePlayers(), item, false, false);
								loaded.put(entity, false);
							}
						});
					} else {
						if (!plugin.isEnabled()) {
							return;
						}
						Bukkit.getScheduler().runTask(plugin, () -> {
							List<Player> players = active.get(entity);
							if (players == null) {
								return;
							}
							if (!isOccluding(item.getLocation().getBlock().getType())) {
								sendItemSpawn(players, item);
								updateItem(item);
								loaded.put(entity, true);
							}
						});
					}
				} else if (entity instanceof ItemFrame) {
					ItemFrame frame = (ItemFrame) entity;
					if (!PlayerLocationManager.hasPlayerNearby(frame.getLocation())) {
						continue;
					}
					if (entry.getValue()) {
						if (!plugin.isEnabled()) {
							return;
						}
						Bukkit.getScheduler().runTask(plugin, () -> {
							List<Player> players = active.get(entity);
							if (players == null) {
								return;
							}
							if (isOccluding(frame.getLocation().getBlock().getType())) {
								removeItemFrame(InteractionVisualizer.getOnlinePlayers(), frame, false, false);
								loaded.put(entity, false);
							}
						});
					} else {
						if (!plugin.isEnabled()) {
							return;
						}
						Bukkit.getScheduler().runTask(plugin, () -> {
							List<Player> players = active.get(entity);
							if (players == null) {
								return;
							}
							if (!isOccluding(frame.getLocation().getBlock().getType())) {
								sendItemFrameSpawn(players, frame);
								updateItemFrame(frame);
								loaded.put(entity, true);
							}
						});
					}
				}
				try {TimeUnit.MILLISECONDS.sleep(5);} catch (InterruptedException e) {e.printStackTrace();}
			}
			if (plugin.isEnabled()) {
				Bukkit.getScheduler().runTaskLater(plugin, () -> run(), 1);
			}
		});
	}
	
	private static boolean isOccluding(Material material) {
		if (InteractionVisualizer.exemptBlocks.contains(material.toString().toUpperCase())) {
			return false;
		}
		return material.isOccluding();
	}
	
	public static int update() {
		return Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, () -> {
			for (Player player : Bukkit.getOnlinePlayers()) {
				Set<VisualizerEntity> activeList = playerStatus.get(player);
				if (activeList == null) {
					continue;
				}
				
				List<Player> playerList = new LinkedList<Player>();
				playerList.add(player);
				
				Location playerLocation = PlayerLocationManager.getPlayerLocation(player);
				for (VisualizerEntity entity : activeList) {
					int range = InteractionVisualizer.playerTrackingRange.getOrDefault(entity.getWorld(), 64);
					range *= range;
					if (!entity.getWorld().equals(playerLocation.getWorld()) || entity.getLocation().distanceSquared(playerLocation) > range) {
						if (entity instanceof ArmorStand) {
							ArmorStand stand = (ArmorStand) entity;
							removeArmorStand(playerList, stand, false, true);
						} else if (entity instanceof Item) {
							Item item = (Item) entity;
							removeItem(playerList, item, false, true);
						} else if (entity instanceof ItemFrame) {
							ItemFrame frame = (ItemFrame) entity;
							removeItemFrame(playerList, frame, false, true);
						}
					}
				}
				
				for (VisualizerEntity entity : active.keySet()) {
					int range = InteractionVisualizer.playerTrackingRange.getOrDefault(entity.getWorld(), 64);
					range *= range;
					if (entity.getWorld().equals(playerLocation.getWorld()) && entity.getLocation().distanceSquared(playerLocation) <= range) {
						if (activeList.contains(entity)) {
							continue;
						}
						if (!active.get(entity).contains(player)) {
							continue;
						}
						Boolean isLoaded = loaded.get(entity);
						if (isLoaded == null || !isLoaded) {
							continue;
						}
						
						if (entity instanceof ArmorStand) {
							ArmorStand stand = (ArmorStand) entity;
							sendArmorStandSpawn(playerList, stand);
							updateArmorStand(playerList, stand);
						} else if (entity instanceof Item) {
							Item item = (Item) entity;
							sendItemSpawn(playerList, item);
							updateItem(playerList, item);
						} else if (entity instanceof ItemFrame) {
							ItemFrame frame = (ItemFrame) entity;
							sendItemFrameSpawn(playerList, frame);
							updateItemFrame(playerList, frame);
						}
					}
				}
			}
		}, 0, 20).getTaskId();
	}
	/*
	public static void sendLightUpdate(List<Player> players, Location location, int skysubchunkbitmask, List<byte[]> skybytearray, int blocksubchunkbitmask, List<byte[]> blockbytearray) {
		PacketContainer packet = protocolManager.createPacket(PacketType.Play.Server.LIGHT_UPDATE);
		int chunkX = (int) Math.floor((double) location.getBlockX() / 16.0);
		int chunkZ = (int) Math.floor((double) location.getBlockZ() / 16.0);
		
		packet.getIntegers().write(0, chunkX);
		packet.getIntegers().write(1, chunkZ);
		packet.getIntegers().write(2, skysubchunkbitmask);
		packet.getIntegers().write(3, blocksubchunkbitmask);
		packet.getIntegers().write(4, ~skysubchunkbitmask);
		packet.getIntegers().write(5, ~blocksubchunkbitmask);
		packet.getModifier().write(6, skybytearray);
		packet.getModifier().write(7, blockbytearray);
		
		try {
        	for (Player player : players) {
				protocolManager.sendServerPacket(player, packet);
			}
		} catch (InvocationTargetException e) {
			e.printStackTrace();
		}
	}
	*/
	public static void sendHandMovement(List<Player> players, Player entity) {
		if (!plugin.isEnabled()) {
			return;
		}
		Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
			List<Player> playersInRange = filterOutOrRange(players, entity);
			ServerPacketSender.sendHandMovement(playersInRange, entity);
		});
	}
	
	public static void sendArmorStandSpawn(List<Player> players, ArmorStand entity) {
		if (!active.containsKey(entity)) {
			active.put(entity, players);
			loaded.put(entity, true);
		}
		
		if (!plugin.isEnabled()) {
			return;
		}
		Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
			List<Player> playersInRange = filterOutOffRange(players, entity);
			ServerPacketSender.spawnArmorStand(playersInRange, entity);
			playersInRange.forEach((each) -> {
				Set<VisualizerEntity> list = playerStatus.get(each);
				if (list != null) {
					list.add(entity);
				}
			});
		});
	}
	
	public static void updateArmorStand(ArmorStand entity) {
		List<Player> players = active.get(entity);
		if (players == null) {
			return;
		}
		updateArmorStand(players, entity);
	}
	
	public static void updateArmorStand(List<Player> players, ArmorStand entity) {
		updateArmorStand(players, entity, false);
	}
	
	public static void updateArmorStand(List<Player> players, ArmorStand entity, boolean bypasscache) {
		if (!bypasscache) {
			Integer lastCode = cache.get(entity);
			if (lastCode != null) {
				if (lastCode == entity.cacheCode()) {
					return;
				}
			}
		}
		
		if (!plugin.isEnabled()) {
			return;
		}
		Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
			List<Player> playersInRange = filterOutOffRange(players, entity);
			ServerPacketSender.updateArmorStand(playersInRange, entity);
		});
        
        cache.put(entity, entity.cacheCode());
	}
	
	public static void updateArmorStandOnlyMeta(ArmorStand entity) {
		List<Player> players = active.get(entity);
		if (players == null) {
			return;
		}
		updateArmorStandOnlyMeta(players, entity);
	}
	
	public static void updateArmorStandOnlyMeta(List<Player> players, ArmorStand entity) {
		updateArmorStandOnlyMeta(players, entity, false);
	}
	
	public static void updateArmorStandOnlyMeta(List<Player> players, ArmorStand entity, boolean bypasscache) {
		if (!bypasscache) {
			Integer lastCode = cache.get(entity);
			if (lastCode != null) {
				if (lastCode == entity.cacheCode()) {
					return;
				}
			}
		}
		
		if (!plugin.isEnabled()) {
			return;
		}
		Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
			List<Player> playersInRange = filterOutOffRange(players, entity);
			ServerPacketSender.updateArmorStandOnlyMeta(playersInRange, entity);
		});
        
        cache.put(entity, entity.cacheCode());
	}
	
	public static void removeArmorStand(List<Player> players, ArmorStand entity, boolean removeFromActive, boolean bypassFilter) {
		if (removeFromActive) {
			active.remove(entity);
			loaded.remove(entity);
			cache.remove(entity);
		}

		if (!plugin.isEnabled()) {
			return;
		}
		Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
			List<Player> playersInRange = bypassFilter ? players : filterOutOffRange(players, entity);
			ServerPacketSender.removeArmorStand(playersInRange, entity);
			playersInRange.forEach((each) -> {
				Set<VisualizerEntity> list = playerStatus.get(each);
				if (list != null) {
					list.remove(entity);
				}
			});
		});
	}
	
	public static void removeArmorStand(List<Player> players, ArmorStand entity) {
		removeArmorStand(players, entity, true, false);
	}
	
	public static void sendItemSpawn(List<Player> players, Item entity) {
		if (!active.containsKey(entity)) {
			active.put(entity, players);
			loaded.put(entity, true);
		}	
		if (entity.getItemStack().getType().equals(Material.AIR)) {
			return;
		}

		if (!plugin.isEnabled()) {
			return;
		}
		Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
			List<Player> playersInRange = filterOutOffRange(players, entity);
			ServerPacketSender.spawnItem(playersInRange, entity);
			playersInRange.forEach((each) -> {
				Set<VisualizerEntity> list = playerStatus.get(each);
				if (list != null) {
					list.add(entity);
				}
			});
		});
	}
	
	public static void updateItem(Item entity) {
		List<Player> players = active.get(entity);
		if (players == null) {
			return;
		}
		updateItem(players, entity);
	}
	
	public static void updateItem(List<Player> players, Item entity) {
		updateItem(players, entity, false);
	}
	
	public static void updateItem(List<Player> players, Item entity, boolean bypasscache) {		
		if (!bypasscache) {
			Integer lastCode = cache.get(entity);
			if (lastCode != null) {
				if (lastCode == entity.cacheCode()) {
					return;
				}
			}
		}
		
		if (!plugin.isEnabled()) {
			return;
		}
		Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
			List<Player> playersInRange = filterOutOffRange(players, entity);
			ServerPacketSender.updateItem(playersInRange, entity);
		});
		
		cache.put(entity, entity.cacheCode());
	}
	
	public static void removeItem(List<Player> players, Item entity, boolean removeFromActive, boolean bypassFilter) {
		if (entity.getItemStack().getType().equals(Material.AIR)) {
			return;
		}
		if (removeFromActive) {
			active.remove(entity);
			loaded.remove(entity);
			cache.remove(entity);
		}

		if (!plugin.isEnabled()) {
			return;
		}
		Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
			List<Player> playersInRange = bypassFilter ? players : filterOutOffRange(players, entity);
			ServerPacketSender.removeItem(playersInRange, entity);
			playersInRange.forEach((each) -> {
				Set<VisualizerEntity> list = playerStatus.get(each);
				if (list != null) {
					list.remove(entity);
				}
			});
		});
	}
	
	public static void removeItem(List<Player> players, Item entity) {
		removeItem(players, entity, true, false);
	}
	
	public static void sendItemFrameSpawn(List<Player> players, ItemFrame entity) {
		if (!active.containsKey(entity)) {
			active.put(entity, players);
			loaded.put(entity, true);
		}

		if (!plugin.isEnabled()) {
			return;
		}
		Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
			List<Player> playersInRange = filterOutOffRange(players, entity);
			ServerPacketSender.spawnItemFrame(playersInRange, entity);
			playersInRange.forEach((each) -> {
				Set<VisualizerEntity> list = playerStatus.get(each);
				if (list != null) {
					list.add(entity);
				}
			});
		});
	}
	
	public static void updateItemFrame(ItemFrame entity) {
		List<Player> players = active.get(entity);
		if (players == null) {
			return;
		}
		
		updateItemFrame(players, entity);
	}
	
	public static void updateItemFrame(List<Player> players , ItemFrame entity) {
		updateItemFrame(players, entity, false);
	}
	
	public static void updateItemFrame(List<Player> players , ItemFrame entity, boolean bypasscache) {
		if (!bypasscache) {
			Integer lastCode = cache.get(entity);
			if (lastCode != null) {
				if (lastCode == entity.cacheCode()) {
					return;
				}
			}
		}
		
		if (!plugin.isEnabled()) {
			return;
		}
		Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
			List<Player> playersInRange = filterOutOffRange(players, entity);
			ServerPacketSender.updateItemFrame(playersInRange, entity);
		});
        
        cache.put(entity, entity.cacheCode());
	}
	
	public static void removeItemFrame(List<Player> players, ItemFrame entity, boolean removeFromActive, boolean bypassFilter) {
		if (removeFromActive) {
			active.remove(entity);
			loaded.remove(entity);
			cache.remove(entity);
		}
		
		if (!plugin.isEnabled()) {
			return;
		}
		Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
			List<Player> playersInRange = bypassFilter ? players : filterOutOffRange(players, entity);
			ServerPacketSender.removeItemFrame(playersInRange, entity);
			playersInRange.forEach((each) -> {
				Set<VisualizerEntity> list = playerStatus.get(each);
				if (list != null) {
					list.remove(entity);
				}
			});
		});
	}
	
	public static void removeItemFrame(List<Player> players, ItemFrame entity) {
		removeItemFrame(players, entity, true, false);
	}
	
	private static List<Player> filterOutOffRange(List<Player> players, VisualizerEntity entity) {
		List<Player> playersInRange = new LinkedList<Player>();
		for (Player player : players) {
			Location playerLocation = PlayerLocationManager.getPlayerLocation(player);
			int range = InteractionVisualizer.playerTrackingRange.getOrDefault(entity.getWorld(), 64);
			range *= range;
			if (playerLocation.getWorld().equals(entity.getWorld()) && (playerLocation.distanceSquared(entity.getLocation()) <= range)) {
				playersInRange.add(player);
			}
		}
		return playersInRange;
	}
	
	private static List<Player> filterOutOrRange(List<Player> players, Entity entity) {
		List<Player> playersInRange = new LinkedList<Player>();
		for (Player player : players) {
			Location playerLocation = PlayerLocationManager.getPlayerLocation(player);
			int range = InteractionVisualizer.playerTrackingRange.getOrDefault(entity.getWorld(), 64);
			range *= range;
			if (playerLocation.getWorld().equals(entity.getWorld()) && (playerLocation.distanceSquared(entity.getLocation()) <= range)) {
				playersInRange.add(player);
			}
		}
		return playersInRange;
	}
	
	public static void reset(Player theplayer) {
		Bukkit.getScheduler().runTask(plugin, () -> removeAll(theplayer));
		int delay = 10 + (int) Math.ceil((double) active.size() / 5.0);
		Bukkit.getScheduler().runTaskLater(plugin, () -> sendPlayerPackets(theplayer), delay);
	}
	
	public static void removeAll(Player theplayer) {
		playerStatus.put(theplayer, Collections.newSetFromMap(new ConcurrentHashMap<VisualizerEntity, Boolean>()));
		if (!plugin.isEnabled()) {
			return;
		}
		Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
			List<Player> player = new ArrayList<Player>();
			player.add(theplayer);
			int count = 0;
			int delay = 1;
			for (Entry<VisualizerEntity, List<Player>> entry : active.entrySet()) {
				count++;
				if (count > 5) {
					delay++;
					count = 0;
				}
				VisualizerEntity entity = entry.getKey();
				if (entity instanceof ArmorStand) {
					Bukkit.getScheduler().runTaskLater(plugin, () -> removeArmorStand(player, (ArmorStand) entity, false, false), delay);
				}
				if (entity instanceof Item) {
					Bukkit.getScheduler().runTaskLater(plugin, () -> removeItem(player, (Item) entity, false, false), delay);
				}
				if (entity instanceof ItemFrame) {
					Bukkit.getScheduler().runTaskLater(plugin, () -> removeItemFrame(player, (ItemFrame) entity, false, false), delay);
				}
			}
		});
	}
	
	public static void sendPlayerPackets(Player theplayer) {
		playerStatus.put(theplayer, Collections.newSetFromMap(new ConcurrentHashMap<VisualizerEntity, Boolean>()));
		if (!plugin.isEnabled()) {
			return;
		}
		Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
			List<Player> player = new ArrayList<Player>();
			player.add(theplayer);
			int count = 0;
			int delay = 1;
			for (Entry<VisualizerEntity, List<Player>> entry : active.entrySet()) {
				VisualizerEntity entity = entry.getKey();
				if (entry.getValue().contains(theplayer)) {
					if (loaded.get(entity)) {
						count++;
						if (count > 5) {
							delay++;
							count = 0;
						}
						if (entity instanceof ArmorStand) {
							Bukkit.getScheduler().runTaskLater(plugin, () -> {
								sendArmorStandSpawn(player, (ArmorStand) entity);
								updateArmorStand(player, (ArmorStand) entity, true);
							}, delay);
						}
						if (entity instanceof Item) {
							Bukkit.getScheduler().runTaskLater(plugin, () -> {
								sendItemSpawn(player, (Item) entity);
								updateItem(player, (Item) entity, true);
							}, delay);	
						}
						if (entity instanceof ItemFrame) {
							Bukkit.getScheduler().runTaskLater(plugin, () -> {
								sendItemFrameSpawn(player, (ItemFrame) entity);
								updateItemFrame(player, (ItemFrame) entity, true);
							}, delay);
						}
					}
				}
			}
		});
	}
	
	@EventHandler
	public void onJoin(PlayerJoinEvent event) {
		playerStatus.put(event.getPlayer(), Collections.newSetFromMap(new ConcurrentHashMap<VisualizerEntity, Boolean>()));
	}
	
	@EventHandler
	public void onWorldChange(PlayerChangedWorldEvent event) {
		playerStatus.put(event.getPlayer(), Collections.newSetFromMap(new ConcurrentHashMap<VisualizerEntity, Boolean>()));
	}
	
	@EventHandler
	public void onLeave(PlayerQuitEvent event) {
		playerStatus.remove(event.getPlayer());
	}
}
