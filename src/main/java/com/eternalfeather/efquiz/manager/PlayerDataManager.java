package com.eternalfeather.efquiz.manager;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

public class PlayerDataManager {

    private final JavaPlugin plugin;

    private File playerDataFile;
    private FileConfiguration playerDataConfig;

    private final Map<String, Integer> correctCounts = new LinkedHashMap<String, Integer>();

    public PlayerDataManager(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public void loadPlayerData() {
        playerDataFile = new File(plugin.getDataFolder(), "playerdata.yml");

        if (!playerDataFile.exists()) {
            try {
                plugin.getDataFolder().mkdirs();
                playerDataFile.createNewFile();
            } catch (IOException e) {
                plugin.getLogger().warning("Could not create playerdata.yml!");
                e.printStackTrace();
                return;
            }
        }

        playerDataConfig = YamlConfiguration.loadConfiguration(playerDataFile);

        correctCounts.clear();

        if (playerDataConfig.contains("players")) {
            for (String playerName : playerDataConfig.getConfigurationSection("players").getKeys(false)) {
                int count = playerDataConfig.getInt("players." + playerName, 0);
                correctCounts.put(playerName, count);
            }
        }

        plugin.getLogger().info("Loaded " + correctCounts.size() + " player quiz records.");
    }

    public void savePlayerData() {
        if (playerDataConfig == null || playerDataFile == null) {
            return;
        }

        playerDataConfig.set("players", null);

        for (Map.Entry<String, Integer> entry : correctCounts.entrySet()) {
            playerDataConfig.set("players." + entry.getKey(), entry.getValue());
        }

        try {
            playerDataConfig.save(playerDataFile);
        } catch (IOException e) {
            plugin.getLogger().warning("Could not save playerdata.yml!");
            e.printStackTrace();
        }
    }

    public void addCorrectCount(Player player) {
        String playerName = player.getName();

        int oldCount = 0;

        if (correctCounts.containsKey(playerName)) {
            oldCount = correctCounts.get(playerName);
        }

        correctCounts.put(playerName, oldCount + 1);

        savePlayerData();
    }

    public int getCorrectCount(String playerName) {
        if (!correctCounts.containsKey(playerName)) {
            return 0;
        }

        return correctCounts.get(playerName);
    }

    public Map<String, Integer> getCorrectCounts() {
        return correctCounts;
    }
}