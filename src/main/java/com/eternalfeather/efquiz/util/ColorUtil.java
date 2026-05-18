package com.eternalfeather.efquiz.util;

import org.bukkit.ChatColor;

public class ColorUtil {

    private ColorUtil() {
    }

    public static String color(String text) {
        return ChatColor.translateAlternateColorCodes('&', text);
    }
}