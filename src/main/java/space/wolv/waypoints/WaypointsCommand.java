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

    private void sendHeader(Player sender)
    {
        TextChain.chain()
            .then("Waypoints")
                .color(NamedTextColor.WHITE)
            .then(" >")
                .color(NamedTextColor.DARK_AQUA)
            .then(">")
                .color(NamedTextColor.AQUA)
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

    @ProxiedBy("wpls")
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
        if (data == null)
        {
            return;
        }
        sendHeader(sender);
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

    @ProxiedBy("wpset")
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
        if (data == null)
        {
            return;
        }
        // check if waypoint already exists
        Optional<Waypoint> wpOptional = data.getWaypoint(name);
        if (wpOptional.isPresent())
        {
            data.updateWaypoint(name, sender.getLocation());
            data.getWaypoint(name).ifPresent(wp -> {
                Location loc = wp.getLocation();
                TextChain.chain()
                    .then("Updated waypoint ")
                        .color(NamedTextColor.WHITE)
                    .then(wp.getName())
                        .color(NamedTextColor.AQUA)
                        .tooltip(
                            "Location: " + ChatColor.AQUA
                                + (int) loc.getX() + " "
                                + (int) loc.getY() + " "
                                + (int) loc.getZ()
                        )
                    .send(waypoints.adventure(sender));
            });
        }
        else
        {
            data.createWaypoint(name, sender.getLocation());
            data.getWaypoint(name).ifPresent(wp -> {
                Location loc = wp.getLocation();
                TextChain.chain()
                    .then("Created waypoint ")
                        .color(NamedTextColor.WHITE)
                    .then(wp.getName())
                        .color(NamedTextColor.AQUA)
                        .tooltip(
                            "Location: " + ChatColor.AQUA
                                + (int) loc.getX() + " "
                                + (int) loc.getY() + " "
                                + (int) loc.getZ()
                        )
                    .send(waypoints.adventure(sender));
            });
        }
        waypoints.updateWaypointData(target, data);
    }

    @ProxiedBy("wpdel")
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
        if (data == null)
        {
            return;
        }
        if (data.deleteWaypoint(name))
        {
            TextChain.chain()
                .then("Deleted waypoint")
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

    @ProxiedBy("wpinfo")
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
        if (data == null)
        {
            return;
        }
        // get waypoint from data
        Optional<Waypoint> wpOptional = data.getWaypoint(name);
        if (wpOptional.isPresent()){
            Waypoint wp = wpOptional.get();
            sendHeader(sender);
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
        }
        else
        {
            notFound(name, sender);
        }
    }

    @ProxiedBy("wpcopy")
    @CommandMethod("waypoints copy <name>")
    @CommandDescription("copy a waypoint")
    public void copy(
        final @NonNull Player sender,
        final @NonNull @Argument("name") String name,
        @Flag(value="player", aliases={"p"}) OfflinePlayer target
    )
    {
        target = checkTarget(sender, target);
    }

    @ProxiedBy("wpshare")
    @CommandMethod("waypoints share <name> <target>")
    @CommandDescription("share a waypoint")
    public void share(
        final @NonNull Player sender,
        final @NonNull @Argument("name") String name,
        final @NonNull @Argument("target") Player target,
        @Flag(value="player", aliases={"p"}) OfflinePlayer owner
    )
    {
        owner = checkTarget(sender, owner);
    }

    @ProxiedBy("wpteleport")
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
    }

    @ProxiedBy("wphelp")
    @CommandMethod("waypoints help [query]")
    @CommandDescription("waypoints help page")
    public void help(final @NonNull Player sender, final @Argument("query") @Greedy String query)
    {
        waypoints.minecraftHelp.queryCommands(query == null ? "" : query, sender);
    }
}
