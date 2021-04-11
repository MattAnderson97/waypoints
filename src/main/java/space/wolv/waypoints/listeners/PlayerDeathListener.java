package space.wolv.waypoints.listeners;

import community.leaf.textchain.adventure.TextChain;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import space.wolv.waypoints.Utils;
import space.wolv.waypoints.Waypoints;

public class PlayerDeathListener implements Listener
{
    private final Waypoints waypoints;

    public PlayerDeathListener(Waypoints waypoints)
    {
        this.waypoints = waypoints;
    }

    @EventHandler
    public void onDeath(PlayerDeathEvent event)
    {
        Player player = event.getEntity();
        Location loc = player.getLocation();
        waypoints.getWaypointData(player).ifPresent(data -> data.createWaypoint("death", loc));
        waypoints.addRecentlyDied(player);
    }

    @EventHandler
    public void onRespawn(PlayerRespawnEvent event)
    {
        Player player = event.getPlayer();
        if (waypoints.checkRecentlyDied(player))
        {
            waypoints.getWaypointData(player).ifPresent(data -> {
                Utils.sendSeparator(waypoints.adventure(player));
                TextChain.chain()
                        .then("Oh dear, you died!")
                        .nextLine()
                        .nextLine()
                        .then("A waypoint has been created at your death location")
                        .nextLine()
                        .nextLine()
                        .then("You can check the waypoint with")
                        .then(" /waypoints info death")
                            .color(NamedTextColor.AQUA)
                            .command("/waypoints info death")
                        .nextLine()
                        .then("and you can copy it with ")
                            .color(NamedTextColor.WHITE)
                        .then("/waypoints copy death <name>")
                            .color(NamedTextColor.AQUA)
                            .suggest("/waypoints copy death ")
                        .send(waypoints.adventure(player));
                Utils.sendSeparator(waypoints.adventure(player));
                waypoints.removeRecentlyDied(player);
            });
        }
    }
}
