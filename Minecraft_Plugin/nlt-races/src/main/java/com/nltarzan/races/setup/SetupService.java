package com.nltarzan.races.setup;

import com.nltarzan.races.NltRacesPlugin;
import com.nltarzan.races.config.Messages;
import com.nltarzan.races.race.BlockPos;
import com.nltarzan.races.race.ButtonAction;
import com.nltarzan.races.race.Race;
import com.nltarzan.races.race.RaceRegistry.ButtonLink;
import com.nltarzan.races.race.Region;
import com.nltarzan.races.util.Enums;
import com.nltarzan.races.util.Locations;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.JoinConfiguration;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Right-click setup: the guided setup (/nltraces setup) and single changes (/nltraces set).
 * Each click is saved to the race file straight away.
 */
public final class SetupService {

    /**
     * Corners clicked at the same height are a floor (or water surface): the checkpoint then covers the
     * clicked layer plus this many blocks above it, so racers walking, riding or sitting low in a boat pass it.
     */
    private static final int FLAT_REGION_HEIGHT = 3;

    private final NltRacesPlugin plugin;
    private final SetupMarkers markers;
    private final Map<UUID, SetupSession> sessions = new HashMap<>();
    private final BukkitTask reminder;

    public SetupService(NltRacesPlugin plugin) {
        this.plugin = plugin;
        this.markers = new SetupMarkers(plugin);
        this.reminder = plugin.getServer().getScheduler().runTaskTimer(plugin, this::remind, 20L, 20L);
    }

    private Messages msg() {
        return plugin.getMessages();
    }

    public boolean isInSession(Player player) {
        return sessions.containsKey(player.getUniqueId());
    }

    // ---------------------------------------------------------------- starting

    /**
     * Starts the guided setup.
     *
     * @param fromStart true for "setup &lt;race&gt; start": every step, in order; false: only what's missing
     */
    public void startGuided(Player player, Race race, String label, boolean fromStart) {
        List<SetupStep> steps = SetupSteps.forRace(race);
        SetupSession session = new SetupSession(race, label, steps, true, !fromStart);
        if (!fromStart) {
            if (steps.stream().anyMatch(step -> step.current().apply(race) != null)) {
                RaceInfo.show(plugin, player, race, label);
            }
            session.index = firstMissing(session, 0);
            if (session.current() == null) {
                msg().send(player, "setup.nothing-missing", "race", race, "label", label);
                return;
            }
        }
        replaceSession(player, session);
        msg().sendRaw(player, "setup.started", "race", race,
                "format", Enums.display(race.getFormat()), "travel", RaceInfo.travelText(race));
        prompt(player, session);
    }

    /** Starts a single change, e.g. /nltraces set sprint lobby. */
    public void startSingle(Player player, Race race, String label, SetupStep step) {
        SetupSession session = new SetupSession(race, label, List.of(step), false, false);
        replaceSession(player, session);
        prompt(player, session);
    }

    private void replaceSession(Player player, SetupSession session) {
        SetupSession previous = sessions.put(player.getUniqueId(), session);
        if (previous != null) {
            markers.clearAll(player, previous, false);
        }
    }

    // ---------------------------------------------------------------- clicks

    /**
     * @param inAir the admin right-clicked the air, so {@code block} is the one they stand on
     */
    public void handleClick(Player player, Block block, boolean inAir) {
        SetupSession session = sessions.get(player.getUniqueId());
        if (session == null || !editable(player, session)) {
            return;
        }
        SetupStep step = session.current();
        switch (step.kind()) {
            case BUTTON -> {
                if (inAir) {
                    msg().send(player, "setup.button-needs-block");
                    return;
                }
                ButtonAction action = Enums.parse(ButtonAction.class, step.id().substring("button-".length()));
                ButtonLink existing = plugin.getRaces().findButton(BlockPos.of(block));
                if (existing != null && (existing.race() != session.race || existing.action() != action)) {
                    msg().send(player, "link.already-linked", "race", existing.race(),
                            "action", Enums.display(existing.action()), "label", session.label);
                    return;
                }
                apply(player, session, block, Locations.describe(block.getLocation()));
            }
            case LOCATION -> {
                if (session.isRebuildStep()) {
                    startNewSetIfNeeded(player, session, step);
                    session.createdThisVisit++;
                }
                if (markers.hasMarkers(step.id())) { // grid spots and point zones
                    markers.add(player, session, step.id(), step.id(), block.getLocation());
                }
                Location location = Locations.onTopOf(block, player.getLocation().getYaw());
                apply(player, session, location, Locations.describe(location));
            }
            case REGION -> {
                Location corner = block.getLocation();
                if (session.firstCorner == null) {
                    session.firstCorner = corner;
                    markers.add(player, session, SetupMarkers.PENDING, step.id(), corner);
                    msg().sendRaw(player, "setup.corner-set", "location", Locations.describe(corner));
                    click(player);
                    prompt(player, session);
                    refreshMarkersNextTick(player, session);
                    return;
                }
                if (!corner.getWorld().equals(session.firstCorner.getWorld())) {
                    msg().send(player, "setup.corner-other-world");
                    return;
                }
                markRegionCorners(player, session, step, session.firstCorner, corner);
                Region region = Region.of(session.firstCorner, corner);
                if (region.minY() == region.maxY()) {
                    region = new Region(region.world(), region.minX(), region.minY(), region.minZ(),
                            region.maxX(), region.minY() + FLAT_REGION_HEIGHT, region.maxZ());
                    msg().send(player, "setup.flat-region", "height", String.valueOf(FLAT_REGION_HEIGHT));
                }
                apply(player, session, region, region.describe());
            }
        }
        refreshMarkersNextTick(player, session);
    }

    /**
     * In the guided grid-spot or point-zone step, the first click of a visit throws away the ones already
     * saved and starts a new set. Clicking Next instead keeps them. (/nltraces set ... add only adds.)
     */
    private void startNewSetIfNeeded(Player player, SetupSession session, SetupStep step) {
        int existing = SetupSteps.spotCount(step, session.race);
        if (session.createdThisVisit > 0 || existing == 0) {
            return;
        }
        SetupSteps.clearSpots(step, session.race);
        markers.clear(player, session, step.id(), List.of());
        msg().sendRaw(player, "setup.set-replaced", "count", String.valueOf(existing), "step", name(step));
    }

    private void markRegionCorners(Player player, SetupSession session, SetupStep step, Location first,
                                   Location second) {
        if (!markers.hasMarkers(step.id())) {
            return;
        }
        markers.forgetPending(session);
        if (step.repeating()) {
            markers.add(player, session, step.id(), step.id(), first);
            markers.add(player, session, step.id(), step.id(), second);
        } else {
            markers.replace(player, session, step.id(), List.of(first, second));
        }
    }

    private void apply(Player player, SetupSession session, Object value, String shown) {
        SetupStep step = session.current();
        step.applier().accept(session.race, value);
        session.firstCorner = null;
        plugin.getRaces().save(session.race);
        msg().sendRaw(player, "setup.step-done", "step", name(step), "value", shown);
        click(player);

        if (!step.repeating()) {
            session.index = nextIndex(session);
        }
        continueOrFinish(player, session);
    }

    // ---------------------------------------------------------------- undo / back / next / finish

    /**
     * In the grid-spot or point-zone step, removes the last one placed; anywhere else (or with nothing placed
     * yet) it acts as Back.
     */
    public void undo(Player player, Race race, String label) {
        SetupSession session = sessionFor(player, race, label);
        if (session == null) {
            return;
        }
        SetupStep step = session.current();
        if (!session.isRebuildStep() || session.createdThisVisit == 0) {
            back(player, race, label);
            return;
        }
        int number = SetupSteps.spotCount(step, race);
        SetupSteps.removeLastSpot(step, race);
        session.createdThisVisit--;
        markers.removeLast(player, session, step.id());
        plugin.getRaces().save(race);
        msg().sendRaw(player, "setup.undone-last", "step", name(step), "number", String.valueOf(number));
        prompt(player, session);
    }

    /**
     * Goes back without changing anything; the next click replaces that step's value. Halfway through a
     * region (first corner clicked), it goes back to the first corner of the same step.
     */
    public void back(Player player, Race race, String label) {
        SetupSession session = sessionFor(player, race, label);
        if (session == null) {
            return;
        }
        if (session.firstCorner != null) {
            session.firstCorner = null;
            markers.clear(player, session, SetupMarkers.PENDING, List.of());
        } else if (session.index == 0) {
            msg().send(player, "setup.at-first-step");
            return;
        } else {
            session.index--;
        }
        msg().sendRaw(player, session.current().repeating() ? "setup.went-back-repeating" : "setup.went-back",
                "step", name(session.current()));
        prompt(player, session);
    }

    /** Leaves the current step as it is and moves on. */
    public void next(Player player, Race race, String label) {
        SetupSession session = sessionFor(player, race, label);
        if (session == null) {
            return;
        }
        msg().sendRaw(player, "setup.moved-on", "step", name(session.current()));
        if (session.firstCorner != null) {
            session.firstCorner = null;
            markers.clear(player, session, SetupMarkers.PENDING, List.of());
        }
        session.index = nextIndex(session);
        continueOrFinish(player, session);
    }

    /** Stops the setup; everything set so far is already saved. */
    public void finish(Player player, Race race, String label) {
        SetupSession session = sessionFor(player, race, label);
        if (session != null) {
            complete(player, session);
        }
    }

    /**
     * /nltraces cancel: stops whatever the admin is selecting (a single set, or the guided setup).
     * Returns false when there was nothing to cancel.
     */
    public boolean cancelSelection(Player player) {
        SetupSession session = sessions.remove(player.getUniqueId());
        if (session == null) {
            return false;
        }
        markers.clearAll(player, session, false);
        msg().actionBar(player, Component.empty());
        msg().send(player, "cancel.setup", "race", session.race);
        return true;
    }

    private void continueOrFinish(Player player, SetupSession session) {
        if (session.current() == null) {
            complete(player, session);
        } else {
            prompt(player, session);
        }
    }

    private void complete(Player player, SetupSession session) {
        sessions.remove(player.getUniqueId());
        markers.clearAll(player, session, true);
        msg().actionBar(player, Component.empty());
        if (session.guided) {
            msg().sendRaw(player, "setup.finished", "race", session.race);
            RaceInfo.showStatus(plugin, player, session.race, session.label);
        }
    }

    /** Ends a player's session without messages (logout). */
    public void cancel(Player player) {
        sessions.remove(player.getUniqueId());
    }

    /** Ends every session; used on reload, since the race objects are replaced. */
    public void cancelAll() {
        for (Map.Entry<UUID, SetupSession> entry : sessions.entrySet()) {
            Player player = plugin.getServer().getPlayer(entry.getKey());
            if (player != null) {
                markers.clearAll(player, entry.getValue(), false);
                msg().send(player, "setup.cancelled");
                msg().actionBar(player, Component.empty());
            }
        }
        sessions.clear();
    }

    public void cancelFor(Race race) {
        sessions.entrySet().removeIf(entry -> {
            if (entry.getValue().race != race) {
                return false;
            }
            Player player = plugin.getServer().getPlayer(entry.getKey());
            if (player != null) {
                markers.clearAll(player, entry.getValue(), false);
            }
            return true;
        });
    }

    public void shutdown() {
        reminder.cancel();
        cancelAll();
    }

    // ---------------------------------------------------------------- prompts

    /** Shows the current step as one block: header, instruction, current value and (guided) buttons. */
    private void prompt(Player player, SetupSession session) {
        enterStep(player, session);
        SetupStep step = session.current();
        String instructionKey = switch (step.kind()) {
            case BUTTON -> "setup.instruction.button";
            // Point zones only need a position; everything else is also saved with the direction you face.
            case LOCATION -> step.id().equals(SetupSteps.POINT_ZONE_ID)
                    ? "setup.instruction.position" : "setup.instruction.location";
            case REGION -> session.firstCorner == null ? "setup.instruction.corner-1" : "setup.instruction.corner-2";
        };
        Component instruction = msg().get(instructionKey, "step", name(step));

        String value = step.current().apply(session.race);
        Component current = value == null || session.firstCorner != null ? Component.empty()
                : msg().get(step.repeating() ? "setup.so-far" : "setup.currently", "value", value);

        if (!session.guided) {
            msg().sendRaw(player, "setup.single", "step", name(step), "instruction", instruction, "current", current);
            return;
        }
        msg().sendRaw(player, "setup.step", "number", String.valueOf(session.index + 1),
                "total", String.valueOf(session.steps.size()), "step", name(step),
                "instruction", instruction, "note", note(session, step), "current", current,
                "buttons", buttons(session));
    }

    /**
     * Runs once when a step is (re)entered: resets the placed-this-visit counter, and for grid spots and
     * point zones (in the guide and in /nltraces set ... add) shows the ones already saved as markers.
     */
    private void enterStep(Player player, SetupSession session) {
        if (session.visitIndex == session.index) {
            return;
        }
        session.visitIndex = session.index;
        session.createdThisVisit = 0;
        SetupStep step = session.current();
        if (step != null && SetupSteps.isSetOfSpots(step)) {
            markers.replace(player, session, step.id(), SetupSteps.spotBlocks(step, session.race));
        }
    }

    private Component note(SetupSession session, SetupStep step) {
        if (session.isRebuildStep() && session.createdThisVisit == 0 && SetupSteps.spotCount(step, session.race) > 0) {
            return msg().get("setup.existing-note", "step", name(step));
        }
        return step.repeating() ? msg().get("setup.repeating-note") : Component.empty();
    }

    /** [◀ Back] (or [↶ Undo] while placing grid spots / point zones) [Next ▶] [Finish ■]: each runs its command. */
    private Component buttons(SetupSession session) {
        String base = "/" + session.label + " setup " + session.race.getName() + " ";
        String first = session.isRebuildStep() && session.createdThisVisit > 0 ? "undo" : "back";
        return Component.join(JoinConfiguration.separator(Component.text("  ")),
                button(base, first), button(base, "next"), button(base, "finish"));
    }

    private Component button(String base, String action) {
        return msg().button("setup.button." + action, base + action);
    }

    /** Every second: the action-bar reminder, and markers sent again in case the client dropped them. */
    private void remind() {
        for (Map.Entry<UUID, SetupSession> entry : sessions.entrySet()) {
            Player player = plugin.getServer().getPlayer(entry.getKey());
            SetupStep step = entry.getValue().current();
            if (player != null && step != null) {
                msg().actionBar(player, msg().get("setup.actionbar", "race", entry.getValue().race,
                        "step", name(step)));
                markers.refresh(player, entry.getValue());
            }
        }
    }

    /** The server re-sends the clicked block after a cancelled click, which would hide its marker. */
    private void refreshMarkersNextTick(Player player, SetupSession session) {
        plugin.getServer().getScheduler().runTask(plugin, () -> {
            if (player.isOnline()) {
                markers.refresh(player, session);
            }
        });
    }

    private Component name(SetupStep step) {
        return msg().get("setup.names." + step.id());
    }

    private static void click(Player player) {
        player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.6f, 1.4f);
    }

    // ---------------------------------------------------------------- helpers

    /** The step after the current one; when continuing a part-finished setup, the next one not yet set. */
    private int nextIndex(SetupSession session) {
        return session.onlyMissing ? firstMissing(session, session.index + 1) : session.index + 1;
    }

    /** The first step at or after {@code from} that isn't set yet (or the end). */
    private int firstMissing(SetupSession session, int from) {
        int index = from;
        while (index < session.steps.size() && session.steps.get(index).isSet().test(session.race)) {
            index++;
        }
        return index;
    }

    private SetupSession sessionFor(Player player, Race race, String label) {
        SetupSession session = sessions.get(player.getUniqueId());
        if (session == null || session.race != race) {
            msg().send(player, "setup.not-in-setup", "race", race, "label", label);
            return null;
        }
        return session;
    }

    private boolean editable(Player player, SetupSession session) {
        if (plugin.getRaceService().isActive(session.race)) {
            msg().send(player, "general.race-active", "race", session.race);
            return false;
        }
        return true;
    }
}
