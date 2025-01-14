package com.loohp.interactionvisualizer.API;

import java.util.HashSet;
import java.util.Set;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryType;

import com.loohp.interactionvisualizer.Managers.TaskManager;

/**
This class is used for Displays which are shown when a player interact with a certain interface
*/
public abstract class VisualizerInteractDisplay {
	
	/**
	DO NOT CHANGE THESE FIELD
	*/
	private InventoryType type;
	private Set<Integer> tasks;
	
	/**
	This method will be called whenever a player opens the InventoryType registered.
	*/
	public abstract void process(Player player);
	
	/**
	This method is used if you need a runnable, return the task id, return -1 to disable
	*/
	public int run() {
		return -1;
	}
	
	/**
	Register this custom display to InteractionVisualizer.
	*/
	public final void register(InventoryType type) {
		this.type = type;
		TaskManager.processes.get(type).add(this);
		this.tasks = new HashSet<Integer>();
		int run = run();
		if (run >= 0) {
			this.tasks.add(run);
		}
	}
	
	/**
	Unregister this custom display to InteractionVisualizer.
	You don't have to use this normally.
	*/
	public final void unregister() {
		TaskManager.processes.get(type).remove(this);
		this.tasks.forEach(each -> Bukkit.getScheduler().cancelTask(each));
	}

}
