package space.wolv.waypoints;

import org.bukkit.Location;

import java.util.Objects;

public class Waypoint
{
    private final String name;
    private final Location location;

    public Waypoint(String name, Location location)
    {
        this.name = name;
        this.location = location;
    }

    public String getName(){ return name; }
    public Location getLocation(){ return location; }

    public double distance(Location other)
    {
        if (Objects.equals(other.getWorld(), location.getWorld()))
        {
            return location.distance(other);
        }
        return Double.MAX_VALUE;
    }
}
