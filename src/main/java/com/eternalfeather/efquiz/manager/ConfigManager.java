package com.eternalfeather.efquiz.manager;

import com.eternalfeather.efquiz.model.QuizQuestion;
import com.eternalfeather.efquiz.util.ColorUtil;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class ConfigManager {

    private final JavaPlugin plugin;

    private String prefix;
    private int intervalSeconds;
    private int rewardMoney;
    private String rewardCommand;
    private boolean caseSensitive;

    private final List<QuizQuestion> questions = new ArrayList<QuizQuestion>();

    public ConfigManager(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public void loadConfig() {
        plugin.reloadConfig();

        FileConfiguration config = plugin.getConfig();

        prefix = ColorUtil.color(config.getString("settings.prefix", "&6[EF答题]&r "));
        intervalSeconds = config.getInt("settings.interval-seconds", 300);
        rewardMoney = config.getInt("settings.reward-money", 100);
        rewardCommand = config.getString("settings.reward-command", "eco give %player% %money%");
        caseSensitive = config.getBoolean("settings.case-sensitive", false);

        if (intervalSeconds < 10) {
            intervalSeconds = 10;
        }

        questions.clear();

        List<Map<?, ?>> questionMapList = config.getMapList("questions");

        for (Map<?, ?> map : questionMapList) {
            Object questionObject = map.get("question");
            Object answersObject = map.get("answers");

            if (questionObject == null || answersObject == null) {
                continue;
            }

            String questionText = String.valueOf(questionObject);
            List<String> answers = new ArrayList<String>();

            if (answersObject instanceof List) {
                List<?> answerList = (List<?>) answersObject;

                for (Object answer : answerList) {
                    if (answer != null) {
                        answers.add(String.valueOf(answer));
                    }
                }
            }

            if (!questionText.trim().isEmpty() && !answers.isEmpty()) {
                questions.add(new QuizQuestion(questionText, answers));
            }
        }

        plugin.getLogger().info("Loaded " + questions.size() + " quiz questions.");
    }

    public String getPrefix() {
        return prefix;
    }

    public int getIntervalSeconds() {
        return intervalSeconds;
    }

    public int getRewardMoney() {
        return rewardMoney;
    }

    public String getRewardCommand() {
        return rewardCommand;
    }

    public boolean isCaseSensitive() {
        return caseSensitive;
    }

    public List<QuizQuestion> getQuestions() {
        return questions;
    }
}