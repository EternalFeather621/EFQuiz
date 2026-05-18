package com.eternalfeather.efquiz;

import com.eternalfeather.efquiz.command.EFQuizCommand;
import com.eternalfeather.efquiz.listener.QuizChatListener;
import com.eternalfeather.efquiz.manager.ConfigManager;
import com.eternalfeather.efquiz.manager.PlayerDataManager;
import com.eternalfeather.efquiz.manager.QuizManager;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

public class Main extends JavaPlugin {

    private ConfigManager configManager;
    private PlayerDataManager playerDataManager;
    private QuizManager quizManager;

    @Override
    public void onEnable() {
        saveDefaultConfig();

        configManager = new ConfigManager(this);
        playerDataManager = new PlayerDataManager(this);
        quizManager = new QuizManager(this, configManager, playerDataManager);

        configManager.loadConfig();
        playerDataManager.loadPlayerData();

        Bukkit.getPluginManager().registerEvents(new QuizChatListener(quizManager), this);

        getCommand("efquiz").setExecutor(new EFQuizCommand(configManager, playerDataManager, quizManager));

        quizManager.startQuizTask();

        getLogger().info("EFQuiz enabled!");
    }

    @Override
    public void onDisable() {
        if (quizManager != null) {
            quizManager.stopQuizTask();
        }

        if (playerDataManager != null) {
            playerDataManager.savePlayerData();
        }

        getLogger().info("EFQuiz disabled!");
    }
}