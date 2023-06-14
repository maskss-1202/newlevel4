package mask.lab.newlevel4;


import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class Newlevel4 extends JavaPlugin implements Listener {

    private Map<UUID, Integer> playerKills;
    private Map<UUID, Integer> playerLevels;
    private Map<UUID, BossBar> playerBossBars;
    private Map<UUID, Boolean> level50Rewarded;
    private Map<UUID, Boolean> level100Rewarded;

    @Override
    public void onEnable() {
        playerKills = new HashMap<>();
        playerLevels = new HashMap<>();
        playerBossBars = new HashMap<>();
        level50Rewarded = new HashMap<>();
        level100Rewarded = new HashMap<>();

        // プレイヤーデータフォルダを作成
        File dataFolder = new File(getDataFolder(), "playerdata");
        if (!dataFolder.exists()) {
            dataFolder.mkdirs();
        }

        // イベントリスナーの登録
        Bukkit.getPluginManager().registerEvents(this, this);

        // オンラインプレイヤーのデータをロード
        for (Player player : Bukkit.getOnlinePlayers()) {
            loadPlayerData(player.getUniqueId());
        }

        getLogger().info("YourPlugin has been enabled.");
    }

    @Override
    public void onDisable() {
        // オンラインプレイヤーのデータを保存
        for (Player player : Bukkit.getOnlinePlayers()) {
            savePlayerData(player.getUniqueId());
        }

        playerKills.clear();
        playerLevels.clear();
        playerBossBars.values().forEach(BossBar::removeAll);
        playerBossBars.clear();
        level50Rewarded.clear();
        level100Rewarded.clear();

        getLogger().info("YourPlugin has been disabled.");
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        UUID playerId = player.getUniqueId();

        // プレイヤーデータをロード
        loadPlayerData(playerId);
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        UUID playerId = player.getUniqueId();

        // プレイヤーデータを保存
        savePlayerData(playerId);

        // データをクリア
        playerKills.remove(playerId);
        playerLevels.remove(playerId);
        playerBossBars.remove(playerId);
        level50Rewarded.remove(playerId);
        level100Rewarded.remove(playerId);
    }

    @EventHandler
    public void onEntityDeath(EntityDeathEvent event) {
        if (!(event.getEntity().getKiller() instanceof Player)) {
            return;
        }

        Player player = (Player) event.getEntity().getKiller();
        UUID playerId = player.getUniqueId();

        if (!isSupportedEntityType(event.getEntityType())) {
            return;
        }

        int kills = playerKills.getOrDefault(playerId, 0);
        kills++;

        int level = playerLevels.getOrDefault(playerId, 0);
        if (kills >= 10) {
            level++;
            kills = 0;
        }
        playerKills.put(playerId, kills);
        playerLevels.put(playerId, level);

        updatePlayerLevel(player, level);
        updateBossBar(player, kills);

        if (level == 50 && !level50Rewarded.getOrDefault(playerId, false)) {
            giveDiamonds(player, 32);
            level50Rewarded.put(playerId, true);
            makeInvincible(player, 5 * 60); // 5 minutes
        } else if (level == 100 && !level100Rewarded.getOrDefault(playerId, false)) {
            giveDiamonds(player, 128);
            level100Rewarded.put(playerId, true);
            makeInvincible(player, 5 * 60); // 5 minutes
        }
    }

    private boolean isSupportedEntityType(EntityType entityType) {
        return entityType == EntityType.ZOMBIE || entityType == EntityType.SKELETON ||
                entityType == EntityType.CREEPER || entityType == EntityType.SPIDER ||
                entityType == EntityType.HUSK;
    }

    private void updatePlayerLevel(Player player, int level) {
        ChatColor nameColor;

        if (level >= 100) {
            nameColor = ChatColor.RED;
        } else if (level >= 50) {
            nameColor = ChatColor.GREEN;
        } else if (level >= 20) {
            nameColor = ChatColor.AQUA;
        } else {
            nameColor = ChatColor.YELLOW;
        }

        String prefix = ChatColor.WHITE + "[" + nameColor + "Lv." + level + ChatColor.WHITE + "] ";
        player.setPlayerListName(prefix + player.getName());
        player.setDisplayName(prefix + player.getName());
    }

    private void updateBossBar(Player player, int remainingKills) {
        BossBar bossBar = playerBossBars.get(player.getUniqueId());

        if (bossBar == null) {
            bossBar = Bukkit.createBossBar("残りキル数: " + (10 - remainingKills), BarColor.BLUE, BarStyle.SOLID);
            playerBossBars.put(player.getUniqueId(), bossBar);
        } else {
            bossBar.setTitle("残りキル数: " + (10 - remainingKills));
            bossBar.setProgress(1 - ((double) remainingKills / 10));
        }

        bossBar.addPlayer(player);

        new BukkitRunnable() {
            @Override
            public void run() {
                BossBar currentBossBar = playerBossBars.get(player.getUniqueId());
                if (currentBossBar != null) {
                    currentBossBar.removePlayer(player);
                }
            }
        }.runTaskLater(this, 20 * 20); // 20 seconds delay
    }

    private void giveDiamonds(Player player, int amount) {
        player.getInventory().addItem(new ItemStack(Material.DIAMOND, amount));
        player.sendMessage(ChatColor.GOLD + "おめでとう！レベル" + amount + "に到達したのでダイヤモンド" + amount + "個を獲得しました！");
    }

    private void makeInvincible(Player player, int seconds) {
        player.addPotionEffect(new PotionEffect(PotionEffectType.DAMAGE_RESISTANCE, seconds * 20, 255));
    }

    private void savePlayerData(UUID playerId) {
        File dataFile = new File(getDataFolder(), "playerdata.yml");
        YamlConfiguration config = new YamlConfiguration();

        config.set("players." + playerId + ".kills", playerKills.getOrDefault(playerId, 0));
        config.set("players." + playerId + ".level", playerLevels.getOrDefault(playerId, 0));

        try {
            config.save(dataFile);
        } catch (IOException e) {
            getLogger().severe("Failed to save player data for player: " + playerId);
        }
    }

    private void loadPlayerData(UUID playerId) {
        File dataFile = new File(getDataFolder(), "playerdata.yml");
        YamlConfiguration config = YamlConfiguration.loadConfiguration(dataFile);

        if (dataFile.exists()) {
            int kills = config.getInt("players." + playerId + ".kills", 0);
            int level = config.getInt("players." + playerId + ".level", 0);
            playerKills.put(playerId, kills);
            playerLevels.put(playerId, level);
        }
    }
}