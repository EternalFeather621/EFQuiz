package com.eternalfeather.efquiz.command;

import com.eternalfeather.efquiz.manager.ConfigManager;
import com.eternalfeather.efquiz.manager.PlayerDataManager;
import com.eternalfeather.efquiz.manager.QuizManager;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

public class EFQuizCommand implements CommandExecutor {

    private final ConfigManager configManager;
    private final PlayerDataManager playerDataManager;
    private final QuizManager quizManager;

    /**
     * 每页排行榜显示多少名玩家
     */
    private static final int TOP_PAGE_SIZE = 10;

    public EFQuizCommand(ConfigManager configManager, PlayerDataManager playerDataManager, QuizManager quizManager) {
        this.configManager = configManager;
        this.playerDataManager = playerDataManager;
        this.quizManager = quizManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {

        if (!command.getName().equalsIgnoreCase("efquiz")) {
            return false;
        }

        /*
         * /efquiz
         * 默认显示帮助
         */
        if (args.length == 0) {
            showHelp(sender);
            return true;
        }

        /*
         * /efquiz help
         */
        if (args.length == 1 && args[0].equalsIgnoreCase("help")) {
            showHelp(sender);
            return true;
        }

        /*
         * /efquiz top
         * /efquiz top 1
         * /efquiz top 2
         */
        if (args.length >= 1 && args[0].equalsIgnoreCase("top")) {

            int page = 1;

            if (args.length >= 2) {
                try {
                    page = Integer.parseInt(args[1]);
                } catch (NumberFormatException e) {
                    sender.sendMessage(configManager.getPrefix() + ChatColor.RED + "页数必须是数字。");
                    sender.sendMessage(configManager.getPrefix() + ChatColor.YELLOW + "正确用法：/efquiz top <页数>");
                    return true;
                }
            }

            showTop(sender, page);
            return true;
        }

        /*
         * /efquiz reload
         */
        if (args.length == 1 && args[0].equalsIgnoreCase("reload")) {

            if (!sender.hasPermission("efquiz.admin")) {
                sender.sendMessage(configManager.getPrefix() + ChatColor.RED + "你没有权限使用这个命令。");
                return true;
            }

            configManager.loadConfig();
            quizManager.resetLastQuestionIndex();
            quizManager.startQuizTask();

            sender.sendMessage(configManager.getPrefix() + ChatColor.GREEN + "配置文件已重新加载。");
            sender.sendMessage(configManager.getPrefix() + ChatColor.GREEN + "当前题目数量：" + configManager.getQuestions().size());
            sender.sendMessage(configManager.getPrefix() + ChatColor.GREEN + "广播间隔：" + configManager.getIntervalSeconds() + " 秒");
            sender.sendMessage(configManager.getPrefix() + ChatColor.GREEN + "答对奖励：" + configManager.getRewardMoney() + " 游戏币");

            return true;
        }

        /*
         * 未知子命令
         */
        sender.sendMessage(configManager.getPrefix() + ChatColor.RED + "未知命令。");
        sender.sendMessage(configManager.getPrefix() + ChatColor.YELLOW + "输入 /efquiz help 查看帮助。");

        return true;
    }

    /**
     * 显示帮助
     */
    private void showHelp(CommandSender sender) {
        sender.sendMessage(ChatColor.GOLD + "========== EFQuiz 指令帮助 ==========");
        sender.sendMessage(ChatColor.YELLOW + "/efquiz help"
                + ChatColor.GRAY + " - 查看插件指令帮助");

        sender.sendMessage(ChatColor.YELLOW + "/efquiz top"
                + ChatColor.GRAY + " - 查看答题排行榜第 1 页");

        sender.sendMessage(ChatColor.YELLOW + "/efquiz top <页数>"
                + ChatColor.GRAY + " - 查看指定页数的答题排行榜");

        if (sender.hasPermission("efquiz.admin")) {
            sender.sendMessage(ChatColor.YELLOW + "/efquiz reload"
                    + ChatColor.GRAY + " - 重新加载配置文件");
        }

        sender.sendMessage(ChatColor.GOLD + "===================================");
    }

    /**
     * 分页显示排行榜
     */
    private void showTop(CommandSender sender, int page) {
        Map<String, Integer> correctCounts = playerDataManager.getCorrectCounts();

        if (correctCounts.isEmpty()) {
            sender.sendMessage(configManager.getPrefix() + ChatColor.RED + "目前还没有玩家答对过题目。");
            return;
        }

        if (page <= 0) {
            sender.sendMessage(configManager.getPrefix() + ChatColor.RED + "页数不能小于 1。");
            return;
        }

        List<Map.Entry<String, Integer>> list = new ArrayList<Map.Entry<String, Integer>>(correctCounts.entrySet());

        Collections.sort(list, new Comparator<Map.Entry<String, Integer>>() {
            @Override
            public int compare(Map.Entry<String, Integer> o1, Map.Entry<String, Integer> o2) {
                return o2.getValue().compareTo(o1.getValue());
            }
        });

        int totalPlayers = list.size();

        int totalPages = totalPlayers / TOP_PAGE_SIZE;

        if (totalPlayers % TOP_PAGE_SIZE != 0) {
            totalPages++;
        }

        if (page > totalPages) {
            sender.sendMessage(configManager.getPrefix() + ChatColor.RED + "没有这一页。");
            sender.sendMessage(configManager.getPrefix() + ChatColor.YELLOW + "当前排行榜共有 "
                    + ChatColor.WHITE + totalPages
                    + ChatColor.YELLOW + " 页。");
            return;
        }

        int startIndex = (page - 1) * TOP_PAGE_SIZE;
        int endIndex = startIndex + TOP_PAGE_SIZE;

        if (endIndex > totalPlayers) {
            endIndex = totalPlayers;
        }

        sender.sendMessage(ChatColor.GOLD + "========== EFQuiz 答题排行榜 ==========");
        sender.sendMessage(ChatColor.GRAY + "第 "
                + ChatColor.YELLOW + page
                + ChatColor.GRAY + " / "
                + ChatColor.YELLOW + totalPages
                + ChatColor.GRAY + " 页，共 "
                + ChatColor.YELLOW + totalPlayers
                + ChatColor.GRAY + " 名玩家");

        for (int i = startIndex; i < endIndex; i++) {
            Map.Entry<String, Integer> entry = list.get(i);

            int rank = i + 1;

            sender.sendMessage(ChatColor.YELLOW + "第 " + rank + " 名："
                    + ChatColor.WHITE + entry.getKey()
                    + ChatColor.GRAY + " - "
                    + ChatColor.GREEN + entry.getValue()
                    + ChatColor.GRAY + " 题");
        }

        /*
         * 这里是重点：
         * 如果发送者是玩家，就发送可以点击的上一页/下一页。
         * 如果发送者是控制台，就发送普通文字。
         */
        sendPageButtons(sender, page, totalPages);

        sender.sendMessage(ChatColor.GOLD + "===================================");
    }

    /**
     * 发送排行榜翻页按钮
     */
    private void sendPageButtons(CommandSender sender, int page, int totalPages) {

        /*
         * 控制台不能点击聊天文字，所以控制台只显示普通提示
         */
        if (!(sender instanceof Player)) {
            if (page > 1) {
                sender.sendMessage(ChatColor.GRAY + "上一页：/efquiz top " + (page - 1));
            }

            if (page < totalPages) {
                sender.sendMessage(ChatColor.GRAY + "下一页：/efquiz top " + (page + 1));
            }

            return;
        }

        Player player = (Player) sender;

        /*
         * 如果只有一页，就没有必要显示翻页按钮
         */
        if (totalPages <= 1) {
            player.sendMessage(ChatColor.DARK_GRAY + "[上一页] " + ChatColor.DARK_GRAY + "[下一页]");
            return;
        }

        StringBuilder json = new StringBuilder();

        json.append("[\"\"");

        /*
         * 上一页按钮
         */
        if (page > 1) {
            json.append(",")
                    .append("{")
                    .append("\"text\":\"[上一页]\",")
                    .append("\"color\":\"yellow\",")
                    .append("\"clickEvent\":{")
                    .append("\"action\":\"run_command\",")
                    .append("\"value\":\"/efquiz top ").append(page - 1).append("\"")
                    .append("},")
                    .append("\"hoverEvent\":{")
                    .append("\"action\":\"show_text\",")
                    .append("\"value\":\"点击查看第 ").append(page - 1).append(" 页\"")
                    .append("}")
                    .append("}");
        } else {
            json.append(",")
                    .append("{")
                    .append("\"text\":\"[上一页]\",")
                    .append("\"color\":\"dark_gray\"")
                    .append("}");
        }

        /*
         * 中间空格
         */
        json.append(",")
                .append("{")
                .append("\"text\":\"  \",")
                .append("\"color\":\"gray\"")
                .append("}");

        /*
         * 下一页按钮
         */
        if (page < totalPages) {
            json.append(",")
                    .append("{")
                    .append("\"text\":\"[下一页]\",")
                    .append("\"color\":\"yellow\",")
                    .append("\"clickEvent\":{")
                    .append("\"action\":\"run_command\",")
                    .append("\"value\":\"/efquiz top ").append(page + 1).append("\"")
                    .append("},")
                    .append("\"hoverEvent\":{")
                    .append("\"action\":\"show_text\",")
                    .append("\"value\":\"点击查看第 ").append(page + 1).append(" 页\"")
                    .append("}")
                    .append("}");
        } else {
            json.append(",")
                    .append("{")
                    .append("\"text\":\"[下一页]\",")
                    .append("\"color\":\"dark_gray\"")
                    .append("}");
        }

        json.append("]");

        /*
         * 用原版 tellraw 发送可点击文字
         */
        org.bukkit.Bukkit.dispatchCommand(
                org.bukkit.Bukkit.getConsoleSender(),
                "tellraw " + player.getName() + " " + json.toString()
        );
    }
}