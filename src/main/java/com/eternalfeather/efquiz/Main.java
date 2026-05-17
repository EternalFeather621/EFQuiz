package com.eternalfeather.efquiz;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

public class Main extends JavaPlugin implements Listener {

    private final Random random = new Random();

    private BukkitTask quizTask;

    private String prefix;
    private int intervalSeconds;
    private int rewardMoney;
    private String rewardCommand;
    private boolean caseSensitive;
    private File playerDataFile;
    private FileConfiguration playerDataConfig;

    private List<QuizQuestion> questions = new ArrayList<QuizQuestion>();
    private Map<String, Integer> correctCounts = new LinkedHashMap<String, Integer>();

    private int lastQuestionIndex = -1;

    private volatile QuizQuestion currentQuestion;
    private volatile boolean questionAnswered = false;

    @Override
    public void onEnable() {
        saveDefaultConfig();

        loadPluginConfig();
        loadPlayerData();

        Bukkit.getPluginManager().registerEvents(this, this);

        startQuizTask();

        getLogger().info("EFQuiz enabled!");
    }

    @Override
    public void onDisable() {
        if (quizTask != null) {
            quizTask.cancel();
        }

        savePlayerData();

        getLogger().info("EFQuiz disabled!");
    }

    private void loadPluginConfig() {
        reloadConfig();

        prefix = color(getConfig().getString("settings.prefix", "&6[EF答题]&r "));
        intervalSeconds = getConfig().getInt("settings.interval-seconds", 300);
        rewardMoney = getConfig().getInt("settings.reward-money", 100);
        rewardCommand = getConfig().getString("settings.reward-command", "eco give %player% %money%");
        caseSensitive = getConfig().getBoolean("settings.case-sensitive", false);

        if (intervalSeconds < 10) {
            intervalSeconds = 10;
        }

        questions.clear();

        List<Map<?, ?>> questionMapList = getConfig().getMapList("questions");

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

        lastQuestionIndex = -1;

        getLogger().info("Loaded " + questions.size() + " quiz questions.");
    }

    private void loadPlayerData() {
        playerDataFile = new File(getDataFolder(), "playerdata.yml");

        if (!playerDataFile.exists()) {
            try {
                getDataFolder().mkdirs();
                playerDataFile.createNewFile();
            } catch (IOException e) {
                getLogger().warning("Could not create playerdata.yml!");
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

        getLogger().info("Loaded " + correctCounts.size() + " player quiz records.");
    }

    private void savePlayerData() {
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
            getLogger().warning("Could not save playerdata.yml!");
            e.printStackTrace();
        }
    }

    private void addCorrectCount(Player player) {
        String playerName = player.getName();

        int oldCount = 0;

        if (correctCounts.containsKey(playerName)) {
            oldCount = correctCounts.get(playerName);
        }

        correctCounts.put(playerName, oldCount + 1);

        savePlayerData();
    }

    private void startQuizTask() {
        if (quizTask != null) {
            quizTask.cancel();
        }

        long delayTicks = 20L * 10L;
        long intervalTicks = 20L * intervalSeconds;

        quizTask = Bukkit.getScheduler().runTaskTimer(this, new Runnable() {
            @Override
            public void run() {
                broadcastRandomQuestion();
            }
        }, delayTicks, intervalTicks);
    }

    private void broadcastRandomQuestion() {
        if (questions.isEmpty()) {
            Bukkit.broadcastMessage(prefix + ChatColor.RED + "题库为空，请在 config.yml 中添加题目。");
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

        Bukkit.broadcastMessage(prefix + ChatColor.YELLOW + "题目：" + ChatColor.WHITE + question.getQuestion());
        Bukkit.broadcastMessage(prefix + ChatColor.GRAY + "请直接在聊天框输入答案。");
    }

    @EventHandler
    public void onPlayerChat(AsyncPlayerChatEvent event) {
        final QuizQuestion question = currentQuestion;

        if (question == null) {
            return;
        }

        if (questionAnswered) {
            return;
        }

        final Player player = event.getPlayer();
        String message = event.getMessage().trim();

        if (!isCorrectAnswer(message, question)) {
            return;
        }

        questionAnswered = true;
        event.setCancelled(true);

        Bukkit.getScheduler().runTask(this, new Runnable() {
            @Override
            public void run() {
                rewardPlayer(player, question);
            }
        });
    }

    private boolean isCorrectAnswer(String playerAnswer, QuizQuestion question) {
        for (String answer : question.getAnswers()) {
            String correctAnswer = answer.trim();

            if (caseSensitive) {
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
        addCorrectCount(player);

        int count = correctCounts.get(player.getName());

        Bukkit.broadcastMessage(prefix + ChatColor.GREEN + player.getName()
                + " 回答正确！答案是："
                + ChatColor.YELLOW + question.getAnswers().get(0));

        Bukkit.broadcastMessage(prefix + ChatColor.AQUA + player.getName()
                + " 当前累计答对 "
                + ChatColor.YELLOW + count
                + ChatColor.AQUA + " 题。");

        String command = rewardCommand
                .replace("%player%", player.getName())
                .replace("%money%", String.valueOf(rewardMoney));

        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command);

        player.sendMessage(prefix + ChatColor.GREEN + "你获得了 " + rewardMoney + " 游戏币奖励！");
    }

    private void showTop(CommandSender sender) {
        if (correctCounts.isEmpty()) {
            sender.sendMessage(prefix + ChatColor.RED + "目前还没有玩家答对过题目。");
            return;
        }

        List<Map.Entry<String, Integer>> list = new ArrayList<Map.Entry<String, Integer>>(correctCounts.entrySet());

        Collections.sort(list, new Comparator<Map.Entry<String, Integer>>() {
            @Override
            public int compare(Map.Entry<String, Integer> o1, Map.Entry<String, Integer> o2) {
                return o2.getValue().compareTo(o1.getValue());
            }
        });

        sender.sendMessage(ChatColor.GOLD + "========== EF答题排行榜 ==========");

        int max = Math.min(10, list.size());

        for (int i = 0; i < max; i++) {
            Map.Entry<String, Integer> entry = list.get(i);

            sender.sendMessage(ChatColor.YELLOW + "第 " + (i + 1) + " 名："
                    + ChatColor.WHITE + entry.getKey()
                    + ChatColor.GRAY + " - "
                    + ChatColor.GREEN + entry.getValue()
                    + ChatColor.GRAY + " 题");
        }
    }

    private String color(String text) {
        return ChatColor.translateAlternateColorCodes('&', text);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {

        if (!command.getName().equalsIgnoreCase("efquiz")) {
            return false;
        }

        if (args.length == 1 && args[0].equalsIgnoreCase("top")) {
            showTop(sender);
            return true;
        }

        if (args.length == 1 && args[0].equalsIgnoreCase("reload")) {

            if (!sender.hasPermission("efquiz.admin")) {
                sender.sendMessage(prefix + ChatColor.RED + "你没有权限使用这个命令。");
                return true;
            }

            loadPluginConfig();
            startQuizTask();

            sender.sendMessage(prefix + ChatColor.GREEN + "配置文件已重新加载。");
            sender.sendMessage(prefix + ChatColor.GREEN + "当前题目数量：" + questions.size());
            sender.sendMessage(prefix + ChatColor.GREEN + "广播间隔：" + intervalSeconds + " 秒");
            sender.sendMessage(prefix + ChatColor.GREEN + "答对奖励：" + rewardMoney + " 游戏币");

            return true;
        }

        sender.sendMessage(ChatColor.GOLD + "EF答题 插件命令：");
        sender.sendMessage(ChatColor.YELLOW + "/efquiz top " + ChatColor.GRAY + "- 查看答题排行榜");
        sender.sendMessage(ChatColor.YELLOW + "/efquiz reload " + ChatColor.GRAY + "- 重新加载配置文件");

        return true;
    }

    private static class QuizQuestion {

        private final String question;
        private final List<String> answers;

        public QuizQuestion(String question, List<String> answers) {
            this.question = question;
            this.answers = answers;
        }

        public String getQuestion() {
            return question;
        }

        public List<String> getAnswers() {
            return answers;
        }
    }
}