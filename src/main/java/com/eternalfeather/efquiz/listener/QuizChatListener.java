package com.eternalfeather.efquiz.listener;

import com.eternalfeather.efquiz.manager.QuizManager;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;

public class QuizChatListener implements Listener {

    private final QuizManager quizManager;

    public QuizChatListener(QuizManager quizManager) {
        this.quizManager = quizManager;
    }

    @EventHandler
    public void onPlayerChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();
        String message = event.getMessage();

        boolean correct = quizManager.submitAnswer(player, message);

        if (correct) {
            event.setCancelled(true);
        }
    }
}