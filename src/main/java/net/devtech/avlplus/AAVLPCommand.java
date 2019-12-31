package net.devtech.avlplus;

import net.devtech.avlplus.tasks.NormalActivityTask;
import org.bukkit.ChatColor;
import org.bukkit.Chunk;
import org.bukkit.NamespacedKey;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.craftbukkit.libs.it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import org.bukkit.craftbukkit.libs.it.unimi.dsi.fastutil.longs.LongSet;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Villager;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;
import java.awt.Point;

public class AAVLPCommand implements CommandExecutor {
	private final Plugin plugin;
	private final NamespacedKey key;

	public AAVLPCommand(Plugin plugin) {
		this.plugin = plugin;
		this.key = new NamespacedKey(plugin, "avlp.activatedChunks");
	}

	@Override
	public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
		if (sender instanceof Player) {
			Player player = (Player) sender;
			Chunk chunk = player.getLocation().getChunk();
			Point point = new Point(chunk.getX(), chunk.getZ());

			PersistentDataContainer container = player.getPersistentDataContainer();
			long[] chunks = container.get(this.key, PersistentDataType.LONG_ARRAY);
			LongSet set;
			if (chunks == null) set = new LongOpenHashSet();
			else set = new LongOpenHashSet(chunks);

			long key = AvlPlus.from(point);
			if (AvlPlus.VANILLA_CHUNKS.contains(point)) { // modified chunk
				if (set.remove(key) || player.hasPermission("avlp.override")) {
					AvlPlus.VANILLA_CHUNKS.remove(point);
					player.sendMessage(ChatColor.DARK_GREEN + "This chunk has been reverted to AVL mechanics!");
				} else {
					player.sendMessage(ChatColor.RED + "You do not have perms to this chunk to vanilla mechanics, only the original modifier may do this!");
				}
			} else { // unmodified
				if (set.size() < AvlPlus.maxChunks) {
					int count = 0;
					for (Entity entity : chunk.getEntities()) { // get entities in the chunk the player is in
						if (entity instanceof Villager) { // filter villagers only
							count++;
							NormalActivityTask.activateVillager((Villager) entity); // activate all the villagers and prevent them from being
						}
					}
					AvlPlus.VANILLA_CHUNKS.add(new Point(chunk.getX(), chunk.getZ()));
					set.add(key);
					player.sendMessage(ChatColor.GREEN + "Your " + count + " villagers have come back to life!");
				} else {
					player.sendMessage(ChatColor.RED + "You have exceeded your maximum vanilla chunk count!");
					player.sendMessage(ChatColor.RED + "vchunks:");
					for (Long l : set) {
						Point loc = AvlPlus.to(l);
						player.sendMessage(ChatColor.YELLOW + "\t[" + loc.x + "," + loc.y + "]");
					}
				}

			}
			container.set(this.key, PersistentDataType.LONG_ARRAY, set.toLongArray());
			return true;
		} else {
			sender.sendMessage(ChatColor.RED + "You must be a player to execute this command!");
			return false;
		}
	}
}
