package com.nltarzan.races.leaderboard;

import com.nltarzan.races.NltRacesPlugin;

/**
 * The only place that touches PlaceholderAPI classes, and it's only called when PlaceholderAPI is installed,
 * so the plugin still loads without it.
 */
public final class PlaceholderHook {

    private final NltRacesExpansion expansion;

    private PlaceholderHook(NltRacesExpansion expansion) {
        this.expansion = expansion;
    }

    /** Registers %nltraces_...%; returns null if PlaceholderAPI refused it. */
    public static PlaceholderHook register(NltRacesPlugin plugin) {
        NltRacesExpansion expansion = new NltRacesExpansion(plugin);
        return expansion.register() ? new PlaceholderHook(expansion) : null;
    }

    public void unregister() {
        expansion.unregister();
    }
}
