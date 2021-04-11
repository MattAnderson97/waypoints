package space.wolv.waypoints;

import cloud.commandframework.annotations.AnnotationParser;
import cloud.commandframework.CommandManager;
import cloud.commandframework.arguments.parser.ParserParameters;
import cloud.commandframework.arguments.parser.StandardParameters;
import cloud.commandframework.execution.AsynchronousCommandExecutionCoordinator;
import cloud.commandframework.meta.CommandMeta;
import cloud.commandframework.minecraft.extras.MinecraftHelp;
import cloud.commandframework.paper.PaperCommandManager;
import community.leaf.textchain.adventure.TextChain;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.platform.bukkit.BukkitAudiences;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.HashMap;
import java.util.Optional;
import java.util.function.Function;

@SuppressWarnings("unused")
public class Waypoints extends JavaPlugin implements Listener
{
    private CommandManager<CommandSender> commandManager;
    private BukkitAudiences audiences;
    public MinecraftHelp<CommandSender> minecraftHelp;

    private HashMap<String, WaypointData> playerWaypointMap;
    private String playerDataFolder;

    /*
     * JavaPlugin methods
     */

    @Override
    public void onEnable()
    {
        // get command manager instance
        try
        {
            this.commandManager = new PaperCommandManager<CommandSender>(
                this,
                AsynchronousCommandExecutionCoordinator.<CommandSender>newBuilder().build(), // parse/execute commands async
                Function.identity(), // bukkit command sender
                Function.identity() // command sender again
            );
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }


        // create a bukkit audience for adventure
        audiences = BukkitAudiences.create(this);
        // setup help page and auto complete
        this.minecraftHelp = new MinecraftHelp<>(
            /* Help Prefix */ "/waypoints help",
            /* Audience mapper */ audiences::sender,
            /* Manager */ this.commandManager
        );

        // Create the annotation parser. This allows you to define commands using methods annotated with
        // @CommandMethod
        final Function<ParserParameters, CommandMeta> commandMetaFunction = p ->
            CommandMeta.simple()
                // This will allow you to decorate commands with descriptions
                .with(CommandMeta.DESCRIPTION, p.get(StandardParameters.DESCRIPTION, "No description"))
                .build();
        /* Manager */
        /* Command sender type */
        /* Mapper for command meta instances */
        AnnotationParser<CommandSender> annotationParser = new AnnotationParser<>(
                /* Manager */ this.commandManager,
                /* Command sender type */ CommandSender.class,
                /* Mapper for command meta instances */ commandMetaFunction
        );

        //
        // END FRAMEWORK SETUP
        //

        this.playerWaypointMap = new HashMap<>();
        this.playerDataFolder = getDataFolder() + File.separator + "player data";

        // register event listeners
        getServer().getPluginManager().registerEvents(this, this);

        // register commands
        annotationParser.parse(new WaypointsCommand(this));

        // setup done

        TextChain.chain()
            .then("Enabled: ")
                .color(NamedTextColor.AQUA)
            .then("Waypoints " + getDescription().getVersion())
                .color(NamedTextColor.WHITE)
            .send(adventure(Bukkit.getConsoleSender()));
    }

    @Override
    public void onDisable()
    {
        playerWaypointMap.forEach((uuid, data) -> data.saveAllWaypoints());
        playerWaypointMap.clear();
    }

    /*
     * Custom methods
     */

    public Audience adventure(CommandSender sender)
    {
        try
        {
            return (Audience) sender;
        }
        catch(ClassCastException e)
        {
            return audiences.sender(sender);
        }
    }

    public Optional<WaypointData> getWaypointData(OfflinePlayer player)
    {
        if (playerWaypointMap.containsKey(player.getUniqueId().toString()))
        {
            return Optional.of(playerWaypointMap.get(player.getUniqueId().toString()));
        }
        return Optional.empty();
    }

    public void updateWaypointData(OfflinePlayer player, WaypointData data)
    {
        playerWaypointMap.put(player.getUniqueId().toString(), data);
    }

    /*
     * event handling
     */

    @EventHandler
    public void onJoin(PlayerJoinEvent event)
    {
        Player player = event.getPlayer();
        String UUID = player.getUniqueId().toString();
        playerWaypointMap.put(UUID, new WaypointData(player, playerDataFolder));
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event)
    {
        String UUID = event.getPlayer().getUniqueId().toString();
        playerWaypointMap.get(UUID).saveAllWaypoints();
        playerWaypointMap.remove(UUID);
    }
}
