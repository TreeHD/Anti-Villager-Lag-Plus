package net.devtech.avlplus;

import net.devtech.avlplus.metrics.Metrics;
import net.devtech.avlplus.tasks.CompatibilityCheckTask;
import net.devtech.avlplus.tasks.MainTask;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.craftbukkit.libs.it.unimi.dsi.fastutil.HashCommon;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;
import javax.naming.Name;
import java.awt.Point;
import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public class AvlPlus extends JavaPlugin {
	private BukkitTask task;
	private AvlConfiguration config;

	// I could replace this with a LongSet but for some reason craftbukkit wont import
	// it's micro optimizations anyways :P
	public static final Set<Point> VANILLA_CHUNKS = new HashSet<>();
	public static long maxChunks;
	@Override
	public void onEnable() {
		try {
			if (new CompatibilityCheckTask(this).passedCheck()) {
				this.config = new AvlConfiguration(this, AvlConfiguration.CONFIG_CURRENT_VERSION, "config.yml");
				this.config.loadFromFile();
				maxChunks = this.config.getLong("vanilla-chunks-per-player", 1);

				this.startTasks();
				new Metrics(this);

				Objects.requireNonNull(this.getCommand("aavlp")).setExecutor(new AAVLPCommand(this));
				Objects.requireNonNull(this.getCommand("vlp")).setExecutor(AvlPlus::vlp);
				logger().info("Successfully enabled.");

				this.loadAAVLP();
			}
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public void onDisable() {
		try {
			this.saveAAVLP();
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	public static boolean isInVanilla(Entity villager) {
		Location loc = villager.getLocation();
		return VANILLA_CHUNKS.contains(new Point(loc.getBlockX() >> 4, loc.getBlockZ() >> 4));
	}

	public void loadAAVLP() throws IOException {
		File file = new File(this.getDataFolder(), "vanilla_chunks.yml");
		if (!file.exists()) this.saveAAVLP();
		YamlConfiguration configuration = YamlConfiguration.loadConfiguration(file);
		configuration.getLongList("chunks").stream().mapToLong(Long::longValue).mapToObj(AvlPlus::to).forEach(VANILLA_CHUNKS::add);
	}

	public void saveAAVLP() throws IOException {
		File file = new File(this.getDataFolder(),"vanilla_chunks.yml");
		YamlConfiguration configuration = new YamlConfiguration();
		configuration.set("chunks", VANILLA_CHUNKS.stream().mapToLong(AvlPlus::from).boxed().collect(Collectors.toList()));
		configuration.save(file);
	}

	private void startTasks() {
		if (this.task != null) {
			this.task.cancel();
		}

		long ticksPerAllowSearch = this.config.getLong("ticks-per-allow-search", 600L /* Default value, if config does not contain the entry */);

		this.task = Bukkit.getScheduler().runTaskTimer(this, new MainTask(this), 0L, ticksPerAllowSearch <= 0 ? 600 : ticksPerAllowSearch);
	}

	public static Logger logger() {
		return JavaPlugin.getPlugin(AvlPlus.class).getLogger();
	}

	public AvlConfiguration getAvlConfig() {
		return this.config;
	}

	private static boolean vlp(CommandSender sender, Command command, String label, String[] args) {
		if (sender instanceof Player) {
			if (isInVanilla((Entity) sender))
				sender.sendMessage(ChatColor.GREEN + "This chunk is using vanilla mechanics!");
			else sender.sendMessage(ChatColor.DARK_GREEN + "This chunk is using Avl mechanics!");
			return true;
		}
		sender.sendMessage("must be a player to execute this command!");
		return false;
	}

	public static long from(Point p) {
		return (long) p.x << 32 | p.y & 0xFFFFFFFFL;
	}

	public static Point to(long l) {
		return new Point((int) (l >> 32), (int) l);
	}
}
