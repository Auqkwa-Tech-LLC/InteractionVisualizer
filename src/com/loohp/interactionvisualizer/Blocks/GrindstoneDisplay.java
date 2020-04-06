package com.loohp.interactionvisualizer.Blocks;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.InventoryView;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.EulerAngle;
import org.bukkit.util.Vector;

import com.loohp.interactionvisualizer.InteractionVisualizer;
import com.loohp.interactionvisualizer.Utils.EntityCreator;
import com.loohp.interactionvisualizer.Utils.PacketSending;

public class GrindstoneDisplay implements Listener {
	
	public static HashMap<Block, HashMap<String, Object>> openedGrindstone = new HashMap<Block, HashMap<String, Object>>();
	@SuppressWarnings("serial")
	public static List<Material> tools = new ArrayList<Material>(){
		{
			add(Material.WOODEN_AXE);
			add(Material.WOODEN_HOE);
			add(Material.WOODEN_PICKAXE);
			add(Material.WOODEN_SHOVEL);
			add(Material.WOODEN_SWORD);
			add(Material.STONE_AXE);
			add(Material.STONE_HOE);
			add(Material.STONE_PICKAXE);
			add(Material.STONE_SHOVEL);
			add(Material.STONE_SWORD);
			add(Material.IRON_AXE);
			add(Material.IRON_HOE);
			add(Material.IRON_PICKAXE);
			add(Material.IRON_SHOVEL);
			add(Material.IRON_SWORD);
			add(Material.GOLDEN_AXE);
			add(Material.GOLDEN_HOE);
			add(Material.GOLDEN_PICKAXE);
			add(Material.GOLDEN_SHOVEL);
			add(Material.GOLDEN_SWORD);
			add(Material.DIAMOND_AXE);
			add(Material.DIAMOND_HOE);
			add(Material.DIAMOND_PICKAXE);
			add(Material.DIAMOND_SHOVEL);
			add(Material.DIAMOND_SWORD);
			add(Material.BOW);
			add(Material.FISHING_ROD);
		}
	};
	

	@EventHandler
	public void onUseGrindstone(InventoryClickEvent event) {
		if (event.getWhoClicked().getGameMode().equals(GameMode.SPECTATOR)) {
			return;
		}
		if (event.getView().getTopInventory() == null) {
			return;
		}
		if (event.getView().getTopInventory().getLocation() == null) {
			return;
		}
		if (event.getView().getTopInventory().getLocation().getBlock() == null) {
			return;
		}
		if (!event.getView().getTopInventory().getLocation().getBlock().getType().equals(Material.GRINDSTONE)) {
			return;
		}
		
		if (event.getRawSlot() >= 0 && event.getRawSlot() <= 2) {
			PacketSending.sendHandMovement(InteractionVisualizer.getOnlinePlayers(), (Player) event.getWhoClicked());
		}
	}
	
	@EventHandler
	public void onDragGrindstone(InventoryDragEvent event) {
		if (event.getWhoClicked().getGameMode().equals(GameMode.SPECTATOR)) {
			return;
		}
		if (event.getView().getTopInventory() == null) {
			return;
		}
		if (event.getView().getTopInventory().getLocation() == null) {
			return;
		}
		if (event.getView().getTopInventory().getLocation().getBlock() == null) {
			return;
		}
		if (!event.getView().getTopInventory().getLocation().getBlock().getType().equals(Material.GRINDSTONE)) {
			return;
		}
		
		for (int slot : event.getRawSlots()) {
			if (slot >= 0 && slot <= 2) {
				PacketSending.sendHandMovement(InteractionVisualizer.getOnlinePlayers(), (Player) event.getWhoClicked());
				break;
			}
		}
	}
	
	@EventHandler
	public void onCloseGrindstone(InventoryCloseEvent event) {
		if (event.getView().getTopInventory() == null) {
			return;
		}
		if (event.getView().getTopInventory().getLocation() == null) {
			return;
		}
		if (event.getView().getTopInventory().getLocation().getBlock() == null) {
			return;
		}
		if (!event.getView().getTopInventory().getLocation().getBlock().getType().equals(Material.GRINDSTONE)) {
			return;
		}
		
		Block block = event.getView().getTopInventory().getLocation().getBlock();
		
		if (!openedGrindstone.containsKey(block)) {
			return;
		}
		
		HashMap<String, Object> map = openedGrindstone.get(block);
		if (!map.get("Player").equals((Player) event.getPlayer())) {
			return;
		}
		
		for (int i = 0; i <= 2; i++) {
			if (map.get(String.valueOf(i)) instanceof Entity) {
				Entity entity = (Entity) map.get(String.valueOf(i));
				if (entity instanceof Item) {
					PacketSending.removeItem(InteractionVisualizer.getOnlinePlayers(), (Item) entity);
				} else if (entity instanceof ArmorStand) {
					PacketSending.removeArmorStand(InteractionVisualizer.getOnlinePlayers(), (ArmorStand) entity);
				}
				entity.remove();
			}
		}
		openedGrindstone.remove(block);
	}
	
	public static int run() {		
		return new BukkitRunnable() {
			public void run() {
				
				for (Player player : InteractionVisualizer.getOnlinePlayers()) {
					if (player.getGameMode().equals(GameMode.SPECTATOR)) {
						continue;
					}
					if (player.getOpenInventory() == null) {
						continue;
					}
					if (player.getOpenInventory().getTopInventory() == null) {
						continue;
					}
					if (player.getOpenInventory().getTopInventory().getLocation() == null) {
						continue;
					}
					if (player.getOpenInventory().getTopInventory().getLocation().getBlock() == null) {
						continue;
					}
					if (!player.getOpenInventory().getTopInventory().getLocation().getBlock().getType().equals(Material.GRINDSTONE)) {
						continue;
					}
					
					InventoryView view = player.getOpenInventory();
					Block block = view.getTopInventory().getLocation().getBlock();
					Location loc = block.getLocation();
					
					if (!openedGrindstone.containsKey(block)) {
						HashMap<String, Object> map = new HashMap<String, Object>();
						map.put("Player", player);
						map.put("2", "N/A");
						map.putAll(spawnArmorStands(player, block));
						openedGrindstone.put(block, map);
					}
					
					HashMap<String, Object> map = openedGrindstone.get(block);
					
					if (!map.get("Player").equals(player)) {
						continue;
					}
					ItemStack[] items = new ItemStack[]{view.getItem(0),view.getItem(1)};

					if (view.getItem(2) != null) {
						ItemStack itemstack = view.getItem(2);
						if (itemstack.getType().equals(Material.AIR)) {
							itemstack = null;
						}
						Item item = null;
						if (map.get("2") instanceof String) {
							if (itemstack != null) {
								item = (Item) EntityCreator.create(loc.clone().add(0.5, 1.2, 0.5), EntityType.DROPPED_ITEM);
								item.setItemStack(itemstack);
								item.setVelocity(new Vector(0, 0, 0));
								item.setPickupDelay(32767);
								item.setGravity(false);
								map.put("2", item);
								PacketSending.sendItemSpawn(InteractionVisualizer.itemDrop, item);
								PacketSending.updateItem(InteractionVisualizer.getOnlinePlayers(), item);
							} else {
								map.put("2", "N/A");
							}
						} else {
							item = (Item) map.get("2");
							if (itemstack != null) {
								if (!item.getItemStack().equals(itemstack)) {
									item.setItemStack(itemstack);
									PacketSending.updateItem(InteractionVisualizer.getOnlinePlayers(), item);
								}
								item.setPickupDelay(32767);
								item.setGravity(false);
							} else {
								map.put("2", "N/A");
								PacketSending.removeItem(InteractionVisualizer.getOnlinePlayers(), item);
								item.remove();
							}
						}
					}
					for (int i = 0; i < 2; i++) {
						ArmorStand stand = (ArmorStand) map.get(String.valueOf(i));
						ItemStack item = items[i];
						if (item.getType().equals(Material.AIR)) {
							item = null;
						}
						if (item != null) {
							if (item.getType().isBlock() && !standMode(stand).equals("Block")) {
								toggleStandMode(stand, "Block");
							} else if (tools.contains(item.getType()) && !standMode(stand).equals("Tool")) {
								toggleStandMode(stand, "Tool");
							} else if (!item.getType().isBlock() && !tools.contains(item.getType()) && !standMode(stand).equals("Item")) {
								toggleStandMode(stand, "Item");
							}
							stand.getEquipment().setItemInMainHand(item);
							PacketSending.updateArmorStand(InteractionVisualizer.getOnlinePlayers(), stand);
						} else {
							stand.getEquipment().setItemInMainHand(new ItemStack(Material.AIR));
							PacketSending.updateArmorStand(InteractionVisualizer.getOnlinePlayers(), stand);
						}
					}
				}
				
			}
		}.runTaskTimer(InteractionVisualizer.plugin, 0, 5).getTaskId();
	}
	
	public static String standMode(ArmorStand stand) {
		if (stand.getCustomName().startsWith("IV.Grindstone.")) {
			return stand.getCustomName().substring(stand.getCustomName().lastIndexOf("."));
		}
		return null;
	}
	
	public static void toggleStandMode(ArmorStand stand, String mode) {
		if (!stand.getCustomName().equals("IV.Grindstone.Item")) {
			if (stand.getCustomName().equals("IV.Grindstone.Block")) {
				stand.setCustomName("IV.Grindstone.Item");
				stand.setRotation(stand.getLocation().getYaw() - 45, stand.getLocation().getPitch());
				stand.setRightArmPose(new EulerAngle(0.0, 0.0, 0.0));
				stand.teleport(stand.getLocation().add(rotateVectorAroundY(stand.getLocation().clone().getDirection().multiply(-0.09), -90)));
				stand.teleport(stand.getLocation().add(stand.getLocation().clone().getDirection().multiply(-0.12)));
			}
			if (stand.getCustomName().equals("IV.Grindstone.Tool")) {
				stand.setCustomName("IV.Grindstone.Item");
				stand.teleport(stand.getLocation().add(rotateVectorAroundY(stand.getLocation().clone().getDirection().multiply(0.3), -90)));
				stand.teleport(stand.getLocation().add(stand.getLocation().clone().getDirection().multiply(0.1)));
				stand.teleport(stand.getLocation().add(0, 0.26, 0));
				stand.setRightArmPose(new EulerAngle(0.0, 0.0, 0.0));
			}
		}
		if (mode.equals("Block")) {
			stand.setCustomName("IV.Grindstone.Block");
			stand.teleport(stand.getLocation().add(stand.getLocation().clone().getDirection().multiply(0.12)));
			stand.teleport(stand.getLocation().add(rotateVectorAroundY(stand.getLocation().clone().getDirection().multiply(0.09), -90)));
			stand.setRightArmPose(new EulerAngle(357.9, 0.0, 0.0));
			stand.setRotation(stand.getLocation().getYaw() + 45, stand.getLocation().getPitch());		
		}
		if (mode.equals("Tool")) {
			stand.setCustomName("IV.Grindstone.Tool");
			stand.setRightArmPose(new EulerAngle(357.99, 0.0, 300.0));
			stand.teleport(stand.getLocation().add(0, -0.26, 0));
			stand.teleport(stand.getLocation().add(stand.getLocation().clone().getDirection().multiply(-0.1)));
			stand.teleport(stand.getLocation().add(rotateVectorAroundY(stand.getLocation().clone().getDirection().multiply(-0.3), -90)));
		}
	}
	
	public static HashMap<String, ArmorStand> spawnArmorStands(Player player, Block block) { //.add(0.68, 0.600781, 0.35)
		HashMap<String, ArmorStand> map = new HashMap<String, ArmorStand>();
		Location loc = block.getLocation().clone().add(0.5, 0.600781, 0.5);
		ArmorStand center = (ArmorStand) EntityCreator.create(loc, EntityType.ARMOR_STAND);
		float yaw = getCardinalDirection(player);
		center.setRotation(yaw, center.getLocation().getPitch());
		setStand(center);
		center.setCustomName("IV.Grindstone.Center");
		Vector vector = rotateVectorAroundY(center.getLocation().clone().getDirection().normalize().multiply(0.19), -100).add(center.getLocation().clone().getDirection().normalize().multiply(-0.11));
		ArmorStand middle = (ArmorStand) EntityCreator.create(loc.clone().add(vector), EntityType.ARMOR_STAND);
		setStand(middle, yaw);
		ArmorStand slot0 = (ArmorStand) EntityCreator.create(middle.getLocation().clone().add(rotateVectorAroundY(center.getLocation().clone().getDirection().normalize().multiply(0.2), -90)), EntityType.ARMOR_STAND);
		setStand(slot0, yaw + 20);
		ArmorStand slot1 = (ArmorStand) EntityCreator.create(middle.getLocation().clone().add(rotateVectorAroundY(center.getLocation().clone().getDirection().normalize().multiply(0.2), 90)), EntityType.ARMOR_STAND);
		setStand(slot1, yaw - 20);
		
		map.put("0", slot0);
		map.put("1", slot1);
		center.remove();
		middle.remove();
		
		PacketSending.sendArmorStandSpawn(InteractionVisualizer.itemStand, slot0);
		PacketSending.sendArmorStandSpawn(InteractionVisualizer.itemStand, slot1);
		
		return map;
	}
	
	public static void setStand(ArmorStand stand, float yaw) {
		stand.setArms(true);
		stand.setBasePlate(false);
		stand.setMarker(true);
		stand.setGravity(false);
		stand.setInvulnerable(true);
		stand.setSilent(true);
		stand.setVisible(false);
		stand.setSmall(true);
		stand.setRightArmPose(new EulerAngle(0.0, 0.0, 0.0));
		stand.setCustomName("IV.Grindstone.Item");
		stand.setRotation(yaw, stand.getLocation().getPitch());
	}
	
	public static void setStand(ArmorStand stand) {
		stand.setArms(true);
		stand.setBasePlate(false);
		stand.setMarker(true);
		stand.setSmall(true);
		stand.setGravity(false);
		stand.setSilent(true);
		stand.setInvulnerable(true);
		stand.setVisible(false);
	}
	
	public static Vector rotateVectorAroundY(Vector vector, double degrees) {
	    double rad = Math.toRadians(degrees);
	   
	    double currentX = vector.getX();
	    double currentZ = vector.getZ();
	   
	    double cosine = Math.cos(rad);
	    double sine = Math.sin(rad);
	   
	    return new Vector((cosine * currentX - sine * currentZ), vector.getY(), (sine * currentX + cosine * currentZ));
	}
	
	public static float getCardinalDirection(Entity e) {

		double rotation = (e.getLocation().getYaw() - 90.0F) % 360.0F;

		if (rotation < 0.0D) {
			rotation += 360.0D;
		}
		if ((0.0D <= rotation) && (rotation < 45.0D))
			return 90.0F;
		if ((45.0D <= rotation) && (rotation < 135.0D))
			return 180.0F;
		if ((135.0D <= rotation) && (rotation < 225.0D))
			return -90.0F;
		if ((225.0D <= rotation) && (rotation < 315.0D))
			return 0.0F;
		if ((315.0D <= rotation) && (rotation < 360.0D)) {
			return 90.0F;
		}
		return 0.0F;
	}

}
