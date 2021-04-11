package space.wolv.waypoints;

import de.leonhard.storage.Yaml;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

public class WaypointData
{
    private final OfflinePlayer player;
    private final Yaml dataFile;
    private final List<Waypoint> waypoints;

    public WaypointData(OfflinePlayer player, String playerDataDirectory)
    {
        this.player = player;
        this.dataFile = new Yaml(player.getUniqueId() + ".yml", playerDataDirectory);
        this.waypoints = dataFile.singleLayerKeySet().stream().map(this::getWaypointFromData).collect(Collectors.toList());
    }

    private Waypoint getWaypointFromData(String wpName)
    {
        double x = dataFile.getDouble(wpName + ".x");
        double y = dataFile.getDouble(wpName + ".y");
        double z = dataFile.getDouble(wpName + ".z");
        String worldName = dataFile.getString(wpName + ".world");
        Location wpLoc = new Location(Bukkit.getWorld(worldName), x, y, z);
        return new Waypoint(wpName, wpLoc);
    }

    private void saveWaypointToFile(Waypoint waypoint)
    {
        Location loc = waypoint.getLocation();
        dataFile.set(waypoint.getName() + ".x", loc.getX());
        dataFile.set(waypoint.getName() + ".y", loc.getY());
        dataFile.set(waypoint.getName() + ".z", loc.getZ());
        dataFile.set(waypoint.getName() + ".world", Objects.requireNonNull(loc.getWorld()).getName());
        dataFile.write();
    }

    public void saveAllWaypoints()
    {
        waypoints.forEach(this::saveWaypointToFile);
    }

    public Optional<Waypoint> getWaypoint(String name)
    {
        return waypoints.stream().filter(wp -> wp.getName().equalsIgnoreCase(name)).findFirst();
    }

    public void createWaypoint(String name, Location loc)
    {
        waypoints.add(new Waypoint(name, loc));
    }

    public boolean deleteWaypoint(String name)
    {
        Optional<Waypoint> waypointOptional = getWaypoint(name);
        if (waypointOptional.isEmpty())
        {
            return false;
        }
        Waypoint waypoint = waypointOptional.get();
        waypoints.remove(waypoint);
        dataFile.remove(name);
        dataFile.write();
        return true;
    }

    public void updateWaypoint(String name, Location loc)
    {
        Optional<Waypoint> waypointOptional = getWaypoint(name);
        if (waypointOptional.isEmpty())
        {
            return;
        }
        deleteWaypoint(name);
        createWaypoint(name, loc);
    }

    private void appendMissing(List<Waypoint> list)
    {
        list.addAll(
            List.copyOf(waypoints)
                .stream()
                .filter(wp -> !list.contains(wp))
                .sorted(Comparator.comparing(Waypoint::getName))
                .collect(Collectors.toList())
        );
    }

    public List<Waypoint> getListSortedByDistance(Location loc)
    {
        List<Waypoint> list = List.copyOf(waypoints)
            .stream()
            .filter(wp -> Objects.equals(wp.getLocation().getWorld(), loc.getWorld()))
            .sorted(Comparator.comparing(wp -> wp.distance(loc)))
            .collect(Collectors.toList());
        appendMissing(list);
        return list;
    }

    public List<Waypoint> getListSortedByName(Location loc)
    {
        List<Waypoint> list = List.copyOf(waypoints)
            .stream()
            .filter(wp -> Objects.equals(wp.getLocation().getWorld(), loc.getWorld()))
            .sorted(Comparator.comparing(Waypoint::getName))
            .collect(Collectors.toList());
        appendMissing(list);
        return list;
    }

    public void setCompassTarget(Waypoint waypoint)
    {
        if(player.isOnline())
        {
            Objects.requireNonNull(player.getPlayer()).setCompassTarget(waypoint.getLocation());
        }
    }

    public void resetCompass()
    {
        if(player.isOnline())
        {
            Objects.requireNonNull(player.getPlayer()).setCompassTarget(Bukkit.getWorlds().get(0).getSpawnLocation());
        }
    }
}
