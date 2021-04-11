package space.wolv.waypoints;

import cloud.commandframework.annotations.Argument;
import cloud.commandframework.annotations.CommandDescription;
import cloud.commandframework.annotations.CommandMethod;
import cloud.commandframework.annotations.CommandPermission;
import cloud.commandframework.annotations.Flag;
import cloud.commandframework.annotations.ProxiedBy;
import cloud.commandframework.annotations.specifier.Greedy;
import community.leaf.textchain.adventure.TextChain;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.checkerframework.checker.nullness.qual.NonNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

@SuppressWarnings("unused")
public class WaypointsCommand 
{
    final Waypoints waypoints;

    public WaypointsCommand(Waypoints waypoints)
    {
        this.waypoints = waypoints;
    }

    private OfflinePlayer checkTarget(Player sender, OfflinePlayer target)
    {
        if (target == null || !sender.hasPermission("waypoints.target-others"))
        {
            target = sender;
        }
        return target;
    }

    //
    // private methods
    //

    private WaypointData getData(OfflinePlayer target, Player sender)
    {
        Optional<WaypointData> dataOptional = waypoints.getWaypointData(target);
        // ensure data exists
        if (dataOptional.isEmpty())
        {
            TextChain.chain()
                .then("Error ")
                    .color(TextColor.color(0xFF6B6B))
                .then("Missing player data for " + target.getName())
                    .color(NamedTextColor.WHITE)
                .send(waypoints.adventure(sender));
            return null;
        }
        return dataOptional.get();
    }

    private String getRoundedDistanceString(Player sender, Waypoint wp)
    {
        double dist = Math.round(wp.distance(sender.getLocation()) * 100.0) / 100.0;
        ChatColor color = dist <= 100 ? ChatColor.GREEN : (dist <= 250 ? ChatColor.YELLOW : (dist <= 500 ? ChatColor.GOLD : ChatColor.RED));
        return color + String.valueOf(dist);
    }

    private void sendHeader(Player sender, String sectionTitle)
    {
        TextChain.chain()
            .then("Waypoints")
                .color(NamedTextColor.WHITE)
            .then(" >")
                .color(NamedTextColor.DARK_AQUA)
            .then("> ")
                .color(NamedTextColor.AQUA)
            .then(sectionTitle)
                .color(NamedTextColor.WHITE)
                .italic()
            .send(waypoints.adventure(sender));
    }

    private void notFound(String name, Player sender)
    {
        TextChain.chain()
            .then("Waypoint not found: ")
                .color(NamedTextColor.WHITE)
            .then(name)
                .color(NamedTextColor.RED)
            .send(waypoints.adventure(sender));
    }

    //
    // command methods
    //

    @ProxiedBy(value = "wpls", hidden = true)
    @CommandMethod("waypoints list")
    @CommandDescription("Get a list of waypoints")
    public void list(
        final @NonNull Player sender,
        @Flag(value="sort", aliases={"s"}) String sort,
        @Flag(value="player", aliases={"p"}) OfflinePlayer target
    )
    {
        target = checkTarget(sender, target);
        // get data
        WaypointData data = getData(target, sender);
        if (data == null) { return; }

        sendHeader(sender, "");
        // get list
        List<Waypoint> waypointsList;
        if (sort == null) { sort = ""; }
        switch(sort.toLowerCase())
        {
            case "name":
                waypointsList = data.getListSortedByName(sender.getLocation());
                break;
            case "distance":
            default:
                waypointsList = data.getListSortedByDistance(sender.getLocation());
                break;
        }
        if (waypointsList.isEmpty())
        {
            TextChain.chain()
                .then("You haven't set any waypoints")
                .nextLine()
                .then("Create one with /waypoint set <name>")
                .send(waypoints.adventure(sender));
            return;
        }
        ArrayList<TextChain> lines = new java.util.ArrayList<>();
        waypointsList.forEach(wp -> {
            Location loc = wp.getLocation();
            if (Objects.equals(loc.getWorld(), sender.getWorld()))
            {
                String dist = getRoundedDistanceString(sender, wp);
                // lines.add(" " + ChatColor.GRAY + wp.getName() +  ChatColor.WHITE + " (" + dist + ChatColor.WHITE + ")");
                lines.add(
                    TextChain.chain()
                        .then(" - ")
                            .color(NamedTextColor.WHITE)
                        .then(wp.getName())
                            .color(NamedTextColor.GRAY)
                            .tooltip(
                                ChatColor.WHITE + "World: " + ChatColor.AQUA + loc.getWorld().getName()
                                    + "\n" + ChatColor.WHITE +  "Location: " + ChatColor.AQUA
                                    + (int) loc.getX() + " "
                                    + (int) loc.getY() + " "
                                    + (int) loc.getZ()
                            )
                        .then(" (")
                            .color(NamedTextColor.WHITE)
                        .then(dist)
                        .then(")")
                            .color(NamedTextColor.WHITE)
                );
            }
            else
            {
                // lines.add(" " + ChatColor.DARK_GRAY + ChatColor.STRIKETHROUGH + wp.getName());
                lines.add(
                    TextChain.chain()
                        .then(" - ")
                        .then(wp.getName())
                            .color(NamedTextColor.DARK_GRAY)
                            .strikethrough()
                            .tooltip(
                                ChatColor.WHITE + "World: " + ChatColor.AQUA + loc.getWorld().getName()
                                    + "\n" + ChatColor.WHITE +  "Location: " + ChatColor.AQUA
                                    + (int) loc.getX() + " "
                                    + (int) loc.getY() + " "
                                    + (int) loc.getZ()
                            )
                );
            }
        });
        List<List<TextChain>> pages = Utils.paginate(lines, 8);
        pages.forEach(page -> page.forEach(
            line -> TextChain.chain()
                        .then(line)
                        .send(waypoints.adventure(sender))
        ));
    }

    @ProxiedBy(value = "wpset", hidden = true)
    @CommandMethod("waypoints set <name>")
    @CommandDescription("Create or Update a waypoint")
    public void set(
            final @NonNull Player sender,
            final @NonNull @Argument("name") String name,
            @Flag(value="player", aliases={"p"}) OfflinePlayer target
    )
    {
        target = checkTarget(sender, target);
        // get data
        WaypointData data = getData(target, sender);
        if (data == null) { return; }

        Location loc = sender.getLocation();
        TextChain messageChain = TextChain.chain();

        if (data.getWaypoint(name).isPresent())
        {
            data.updateWaypoint(name, loc);
            messageChain.then("Updated waypoint ");
        }
        else
        {
            data.createWaypoint(name, loc);
            messageChain.then("Created waypoint ");
        }
        messageChain.then(name)
            .color(NamedTextColor.AQUA)
            .tooltip(
                "Location: " + ChatColor.AQUA
                    + (int) loc.getX() + " "
                    + (int) loc.getY() + " "
                    + (int) loc.getZ()
            )
            .send(waypoints.adventure(sender));
    }

    @ProxiedBy(value = "wpdel", hidden = true)
    @CommandMethod("waypoints delete <name>")
    @CommandDescription("Delete a waypoint")
    public void delete(
            final @NonNull Player sender,
            final @NonNull @Argument("name") String name,
            @Flag(value="player", aliases={"p"}) OfflinePlayer target
    )
    {
        target = checkTarget(sender, target);
        // get data
        WaypointData data = getData(target, sender);
        if (data == null) { return; }

        if (data.deleteWaypoint(name))
        {
            TextChain.chain()
                .then("Deleted waypoint ")
                    .color(NamedTextColor.WHITE)
                .then(name)
                    .color(NamedTextColor.AQUA)
                .send(waypoints.adventure(sender));
        }
        else
        {
            notFound(name, sender);
        }
    }

    @ProxiedBy(value = "wpinfo", hidden = true)
    @CommandMethod("waypoints info <name>")
    @CommandDescription("Get info for a waypoint")
    public void info(
        final @NonNull Player sender,
        final @NonNull @Argument("name") String name,
        @Flag(value="player", aliases={"p"}) OfflinePlayer target
    )
    {
        target = checkTarget(sender, target);
        // get data
        WaypointData data = getData(target, sender);
        if (data == null) { return; }
        // get waypoint from data
        data.getWaypoint(name).ifPresentOrElse(wp -> {
            sendHeader(sender, "Info");
            Location loc = wp.getLocation();
            TextChain chain = TextChain.chain()
                .then(" name: ")
                    .color(NamedTextColor.WHITE)
                .then(wp.getName())
                    .color(NamedTextColor.AQUA)
                .nextLine()
                .then(" world: ")
                    .color(NamedTextColor.WHITE)
                .then(Objects.requireNonNull(loc.getWorld()).getName())
                    .color(NamedTextColor.AQUA)
                .nextLine()
                .then(" location: ")
                .then(String.valueOf((int) loc.getX()))
                    .color(NamedTextColor.AQUA)
                .then(" ")
                .then(String.valueOf((int) loc.getY()))
                    .color(NamedTextColor.AQUA)
                .then(" ")
                .then(String.valueOf((int) loc.getZ()))
                    .color(NamedTextColor.AQUA);
            if (Objects.equals(sender.getWorld(), loc.getWorld()))
            {
                chain.nextLine()
                    .then(" distance: ")
                        .color(NamedTextColor.WHITE)
                    .then(getRoundedDistanceString(sender, wp));
            }
            chain.send(waypoints.adventure(sender));
        }, () -> notFound(name, sender));
    }

    @ProxiedBy(value = "wpcopy", hidden = true)
    @CommandMethod("waypoints copy <name> <new-name>")
    @CommandDescription("copy a waypoint")
    public void copy(
        final @NonNull Player sender,
        final @NonNull @Argument("name") String name,
        final @NonNull @Argument("new-name") String newWp,
        @Flag(value="player", aliases={"p"}) OfflinePlayer target
    )
    {
        target = checkTarget(sender, target);
        WaypointData data = getData(target, sender);
        WaypointData senderData = getData(sender, sender);
        if (data == null) { return; }
        if (senderData == null) { return; }

        data.getWaypoint(name).ifPresentOrElse(wp -> {
            senderData.createWaypoint(newWp, wp.getLocation());
            Location loc = wp.getLocation();
            TextChain.chain()
                .then("Copied waypoint ")
                .then(name)
                .then(" to ")
                .then(newWp)
                    .color(NamedTextColor.AQUA)
                    .tooltip(
                        "Location: " + ChatColor.AQUA
                            + (int) loc.getX() + " "
                            + (int) loc.getY() + " "
                            + (int) loc.getZ()
                    )
                .send(waypoints.adventure(sender));
        }, () -> notFound(name, sender));
    }

    @ProxiedBy(value = "wptp", hidden = true)
    @CommandMethod("waypoints teleport <name>")
    @CommandDescription("teleport a waypoint")
    @CommandPermission("waypoints.teleport")
    public void teleport(
        final @NonNull Player sender,
        final @NonNull @Argument("name") String name,
        @Flag(value="player", aliases={"p"}) OfflinePlayer target
    )
    {
        target = checkTarget(sender, target);
        WaypointData data = getData(target, sender);
        if (data == null) { return; }
        data.getWaypoint(name).ifPresentOrElse(wp -> sender.teleport(wp.getLocation()), () -> notFound(name, sender));
    }

    @ProxiedBy(value = "setcompass", hidden = true)
    @CommandMethod("waypoints setcompass <name>")
    @CommandDescription("Set your compass to point to a waypoint")
    public void setCompass(
        final @NonNull Player sender,
        final @NonNull @Argument("name") String name,
        @Flag(value="player", aliases={"p"}) OfflinePlayer target
    )
    {
        target = checkTarget(sender, target);
        WaypointData data = getData(target, sender);
        if (data == null) { return; }
        WaypointData senderData = getData(sender, sender);
        if (senderData == null) { return; }
        data.getWaypoint(name).ifPresentOrElse(wp -> {
            senderData.setCompassTarget(wp);
            Location loc = wp.getLocation();
            TextChain.chain()
                .then("Set compass target to ")
                .then(wp.getName())
                    .color(NamedTextColor.AQUA)
                    .tooltip(
                        "Location: " + ChatColor.AQUA
                            + (int) loc.getX() + " "
                            + (int) loc.getY() + " "
                            + (int) loc.getZ()
                    )
                .send(waypoints.adventure(sender));
        }, () -> notFound(name, sender));
    }

    @ProxiedBy(value = "resetcompass", hidden = true)
    @CommandMethod("waypoints resetcompass")
    @CommandDescription("Reset your compass target")
    public void resetCompass(
        final @NonNull Player sender,
        @Flag(value="player", aliases={"p"}) OfflinePlayer target
    )
    {
        target = checkTarget(sender, target);
        WaypointData data = getData(target, sender);
        if (data == null) { return; }
        data.resetCompass();
        TextChain.chain().then("Reset compass target").send(waypoints.adventure(sender));
    }

    @ProxiedBy(value = "wphelp", hidden = true)
    @CommandMethod("waypoints help [query]")
    @CommandDescription("waypoints help page")
    public void help(final @NonNull Player sender, final @Argument("query") @Greedy String query)
    {
        waypoints.minecraftHelp.queryCommands(query == null ? "" : query, sender);
    }
}
