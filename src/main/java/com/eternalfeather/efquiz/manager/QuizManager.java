package com.eternalfeather.efquiz.manager;

import com.eternalfeather.efquiz.model.QuizQuestion;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

import java.util.List;
import java.util.Random;

public class QuizManager {

    private final JavaPlugin plugin;
    private final ConfigManager configManager;
    private final PlayerDataManager playerDataManager;

    private final Random random = new Random();

    private BukkitTask quizTask;

    private int lastQuestionIndex = -1;

    private volatile QuizQuestion currentQuestion;
    private volatile boolean questionAnswered = false;

    public QuizManager(JavaPlugin plugin, ConfigManager configManager, PlayerDataManager playerDataManager) {
        this.plugin = plugin;
        this.configManager = configManager;
        this.playerDataManager = playerDataManager;
    }

    public void startQuizTask() {
        if (quizTask != null) {
            quizTask.cancel();
        }

        long delayTicks = 20L * 10L;
        long intervalTicks = 20L * configManager.getIntervalSeconds();

        quizTask = Bukkit.getScheduler().runTaskTimer(plugin, new Runnable() {
            @Override
            public void run() {
                broadcastRandomQuestion();
            }
        }, delayTicks, intervalTicks);
    }

    public void stopQuizTask() {
        if (quizTask != null) {
            quizTask.cancel();
            quizTask = null;
        }
    }

    public void resetLastQuestionIndex() {
        lastQuestionIndex = -1;
    }

    public void broadcastRandomQuestion() {
        List<QuizQuestion> questions = configManager.getQuestions();

        if (questions.isEmpty()) {
            Bukkit.broadcastMessage(configManager.getPrefix() + ChatColor.RED + "题库为空，请在 config.yml 中添加题目。");
            return;
        }

        int questionIndex;

        if (questions.size() == 1) {
            questionIndex = 0;
        } else {
            do {
                questionIndex = random.nextInt(questions.size());
            } while (questionIndex == lastQuestionIndex);
        }

        lastQuestionIndex = questionIndex;

        QuizQuestion question = questions.get(questionIndex);

        currentQuestion = question;
        questionAnswered = false;

        Bukkit.broadcastMessage(configManager.getPrefix() + ChatColor.YELLOW + "题目：" + ChatColor.WHITE + question.getQuestion());
        Bukkit.broadcastMessage(configManager.getPrefix() + ChatColor.GRAY + "请直接在聊天框输入答案。");
    }

    public boolean submitAnswer(final Player player, String message) {
        final QuizQuestion question = currentQuestion;

        if (question == null) {
            return false;
        }

        if (questionAnswered) {
            return false;
        }

        String playerAnswer = message.trim();

        if (!isCorrectAnswer(playerAnswer, question)) {
            return false;
        }

        questionAnswered = true;

        Bukkit.getScheduler().runTask(plugin, new Runnable() {
            @Override
            public void run() {
                rewardPlayer(player, question);
            }
        });

        return true;
    }

    private boolean isCorrectAnswer(String playerAnswer, QuizQuestion question) {
        for (String answer : question.getAnswers()) {
            String correctAnswer = answer.trim();

            if (configManager.isCaseSensitive()) {
                if (playerAnswer.equals(correctAnswer)) {
                    return true;
                }
            } else {
                if (playerAnswer.equalsIgnoreCase(correctAnswer)) {
                    return true;
                }
            }
        }

        return false;
    }

    private void rewardPlayer(Player player, QuizQuestion question) {
        playerDataManager.addCorrectCount(player);

        int count = playerDataManager.getCorrectCount(player.getName());

        Bukkit.broadcastMessage(configManager.getPrefix() + ChatColor.GREEN + player.getName()
                + " 回答正确！答案是："
                + ChatColor.YELLOW + question.getAnswers().get(0));

        Bukkit.broadcastMessage(configManager.getPrefix() + ChatColor.AQUA + player.getName()
                + " 当前累计答对 "
                + ChatColor.YELLOW + count
                + ChatColor.AQUA + " 题。");

        String command = configManager.getRewardCommand()
                .replace("%player%", player.getName())
                .replace("%money%", String.valueOf(configManager.getRewardMoney()));

        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command);

        player.sendMessage(configManager.getPrefix() + ChatColor.GREEN + "你获得了 "
                + configManager.getRewardMoney()
                + " 游戏币奖励！");
    }
}