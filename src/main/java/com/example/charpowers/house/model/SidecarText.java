package com.example.charpowers.house.model;

import java.util.List;

// The optional little text file beside a house file: <name>.txt with  level=25  and  name=Big Barn.
public record SidecarText(int level, String name) {
    public static SidecarText parse(List<String> lines) {
        int level = 1;
        String name = null;
        for (String line : lines) {
            int eq = line.indexOf('=');
            if (eq < 0 || line.startsWith("#")) continue;
            String key = line.substring(0, eq).trim().toLowerCase(java.util.Locale.ROOT), value = line.substring(eq + 1).trim();
            if (key.equals("level")) {
                try {
                    level = Math.max(1, Math.min(100, Integer.parseInt(value)));
                } catch (NumberFormatException ignored) {
                    // the default it is
                }
            } else if (key.equals("name") && !value.isEmpty()) {
                name = value.length() > 40 ? value.substring(0, 40) : value;
            }
        }
        return new SidecarText(level, name);
    }
}
