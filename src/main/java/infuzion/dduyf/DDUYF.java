package infuzion.dduyf;

import infuzion.dduyf.util.Metrics;
import infuzion.dduyf.util.Updater;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

public class DDUYF extends JavaPlugin implements Listener {
    File messagesFile;
    FileConfiguration messagesConfig;
    HashMap<UUID, Integer> blocks = new HashMap<UUID, Integer>();

    public void onEnable() {
        getServer().getPluginManager().registerEvents(this, this);
        initConfig();
        messagesFile = new File(getDataFolder(), "messages.yml");
        messagesConfig = YamlConfiguration.loadConfiguration(messagesFile);
        saveMessages();
        if (getConfig().getBoolean("DDUYF.Options.Auto-Update")) {
            Updater updater = new Updater(this, 85668, this.getFile(), Updater.UpdateType.DEFAULT, false);
        }

        if (getConfig().getBoolean("DDUYF.Options.Use-Metrics")) {
            try {
                Metrics metrics = new Metrics(this);
                metrics.start();
            } catch (IOException e) {
                Bukkit.getLogger().warning("Failed to submit metrics. :-(");
            }
        }
    }

    private void initMessages() {
        messagesConfig.addDefault("DDUYF.ReloadSuccess", "&aConfig Successfully Reloaded!");
        messagesConfig.addDefault("DDUYF.RemoveDefault", "&4You cannot delete the default group!");
        messagesConfig.addDefault("DDUYF.NoPermission", "&4You do not have permission for this command.");
        messagesConfig.addDefault("DDUYF.AddGroup", "&aGroup Successfully added!");
        messagesConfig.addDefault("DDUYF.RemoveGroupFailed", "&4The group \"%name%\" does not exist!");
        messagesConfig.addDefault("DDUYF.RemoveGroupSuccess", "&aThe group has been successfully removed!");
        messagesConfig.options().copyDefaults(true);
    }

    private void saveMessages() {
        initMessages();
        try {
            messagesConfig.save(messagesFile);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private String getMessages(String message) {
        return ChatColor.translateAlternateColorCodes('&', messagesConfig.getString(message));
    }

    private void initConfig() {
        FileConfiguration config = getConfig();
        config.options().header("**************************DDUYF************************** #\n***************(Don't Dig Under Your Feet)*************** #\n********************************************************* #\n****Damage is measured per half heart. 1 is 1/2 heart**** #\n****If Use Permission is enabled anyone with the node:*** #\n**************DDUYF.exempt will be exempted************** #\n********************************************************* #\n*Use /dduyf addgroup [name] instead of directly changing* #\n************************this file************************ #\n********************************************************* #\n*.Permission is the permission to be used for that group* #\n********************************************************* #\n*************Group Names are cAsE Sensitive!************* #\n********************************************************* #\n");

        ArrayList<String> t = new ArrayList<String>();
        t.add("default");
        config.addDefault("DDUYF.Options.Use-Metrics", true);
        config.addDefault("DDUYF.Options.Auto-Update", true);
        config.addDefault("DDUYF.Options.ExemptOps", true);
        config.addDefault("DDUYF.Groups.Names", t);
        config.addDefault("DDUYF.Groups.default.Damage", 1.0D);
        config.addDefault("DDUYF.Groups.default.Blocks", 1);
        config.addDefault("DDUYF.Groups.default.Permission", "dduyf.groups.default");
        config.addDefault("DDUYF.Groups.default.UsePermissions", false);
        config.addDefault("DDUYF.Groups.default.Message", "&4 Shrapnel explodes under your feet causing you to take damage!");
        config.options().copyDefaults(true);
        saveConfig();
        reloadConfig();
    }

    private void addGroup(String name) {
        FileConfiguration config = getConfig();
        saveConfig();
        ArrayList<String> t = (ArrayList<String>) config.getStringList("DDUYF.Groups.Names");
        t.add(name);
        config.set("DDUYF.Groups.Names", t);
        config.set("DDUYF.Groups." + name + ".Damage", 1.0D);
        config.set("DDUYF.Groups." + name + ".Blocks", 1);
        config.set("DDUYF.Groups." + name + ".Permission", "dduyf.groups." + name.toLowerCase());
        config.set("DDUYF.Groups." + name + ".UsePermissions", false);
        config.set("DDUYF.Groups." + name + ".UseMessage", true);
        config.set("DDUYF.Groups." + name + ".Message", "&4 Shrapnel explodes under your feet causing you to take damage!");
        saveConfig();
    }

    private void removeGroup(String name, CommandSender sender) {
        FileConfiguration config = getConfig();
        List<String> groups = config.getStringList("DDUYF.Groups.Names");
        if (!name.equals("default")) {
            if (groups.contains(name)) {
                groups.remove(name);
                config.set("DDUYF.Groups.Names", groups);
                sender.sendMessage(getMessages("DDUYF.RemoveGroupSuccess"));
            } else {
                sender.sendMessage(getMessages("DDUYF.RemoveGroupFailed").replace("%name%", name));
            }
            config.set("DDUYF.Groups." + name, null);
            saveConfig();
        } else {
            sender.sendMessage(getMessages("DDUYF.RemoveDefault"));
        }
    }

    private boolean permission(Player p) {
        boolean bool1 = !p.hasPermission("dduyf.exempt");
        boolean bool2 = getConfig().getBoolean("DDUYF.UsePermissions");
        return !(bool1 && bool2);
    }

    private void addBlock(Player p) {
        if (!blocks.containsKey(p.getUniqueId()))
            blocks.put(p.getUniqueId(), 1);
        else if (blocks.containsKey(p.getUniqueId()))
            blocks.put(p.getUniqueId(), blocks.get(p.getUniqueId()) + 1);
    }

    private void clearBlocks(Player p) {
        blocks.put(p.getUniqueId(), 0);
    }

    private int getBlocks(Player p) {
        if (blocks.containsKey(p.getUniqueId())) {
            return (blocks.get(p.getUniqueId()));
        }
        return 1;
    }

    private String getPlayerGroup(Player p) {
        List<String> groups = getConfig().getStringList("DDUYF.Groups.Names");
        String[] group = groups.toArray(new String[groups.size()]);

        int n = group.length;
        while (n > 0) {
            String groupn = getConfig().getString("DDUYF.Groups." + group[(n - 1)] + ".Permission").toLowerCase();
            if (!groupn.isEmpty()) {
                if (p.hasPermission(groupn)) {
                    return group[(n - 1)];
                }
                n--;
            } else {
                return "default";
            }
        }
        return "default";
    }

    private void damage(Player p, int blocks) {
        String pgroup = getPlayerGroup(p);
        boolean op = false;
        clearBlocks(p);
        double d = getConfig().getDouble("DDUYF.Groups." + pgroup + ".Damage");
        int b = getConfig().getInt("DDUYF.Groups." + pgroup + ".Blocks");
        String m = ChatColor.translateAlternateColorCodes('&', getConfig().getString("DDUYF.Groups." + pgroup + ".Message"));
        if ((getConfig().getBoolean("DDUYF.Options.ExemptOp")) && (p.isOp())) {
            op = true;
        }
        if ((b >= blocks) && ((!p.getGameMode().equals(GameMode.CREATIVE)) || (op))) {
            p.damage(d);
            p.sendMessage(m);
            clearBlocks(p);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    @SuppressWarnings("unused")
    public void onBlockBreak(BlockBreakEvent e) {
        if (!e.isCancelled()) {
            Block b = e.getBlock();
            Player p = e.getPlayer();
            Location pl = p.getLocation();
            Location bl = b.getLocation();
            if ((pl.getBlockX() == bl.getBlockX()) && (pl.getBlockY() - 1 == bl.getBlockY()) && (pl.getBlockZ() == bl.getBlockZ()) && (permission(p))) {
                addBlock(p);
                damage(p, getBlocks(p));
            }
        }
    }

    public boolean onCommand(CommandSender theSender, Command cmd, String commandLabel, String[] args) {
        if (cmd.getLabel().equalsIgnoreCase("dduyf")) {
            if (args.length >= 1) {
                if (args[0].equalsIgnoreCase("reload")) {
                    if (theSender.hasPermission("dduyf.reload")) {
                        reloadConfig();
                        theSender.sendMessage(getMessages("DDUYF.ReloadSuccess"));
                    } else {
                        theSender.sendMessage(getMessages("DDUYF.NoPermission"));
                    }
                } else if (args[0].equalsIgnoreCase("addgroup")) {
                    if (theSender.hasPermission("dduyf.addgroup"))
                        if (args.length > 1) {
                            addGroup(args[1]);
                            theSender.sendMessage(getMessages("DDUYF.AddGroup"));
                        } else {
                            theSender.sendMessage(ChatColor.RED + "Usage: /dduyf addgroup [groupname]");
                        }
                } else if (args[0].equalsIgnoreCase("removegroup")) {
                    if (theSender.hasPermission("dduyf.removegroup")) {
                        if (args.length > 1)
                            removeGroup(args[1], theSender);
                        else
                            theSender.sendMessage(ChatColor.RED + "Usage: /dduyf removegroup [groupname]");
                    } else
                        theSender.sendMessage(getMessages("DDUYF.NoPermission"));
                } else if (theSender.hasPermission("dduyf.access")) {
                    theSender.sendMessage(ChatColor.RED +
                            "[DDUYF] Commands:\n" +
                            "/dduyf reload                Reloads configuration \n" +
                            "/dduyf addgroup [name]       Adds a group to config file \n" +
                            "/dduyf removegroup [name]    Removes a group from config file\n");
                } else {
                    theSender.sendMessage(getMessages("DDUYF.NoPermission"));
                }
            } else if (theSender.hasPermission("dduyf.access")) {
                theSender.sendMessage(ChatColor.RED +
                        "[DDUYF] Commands:\n" +
                        "/dduyf reload                Reloads configuration \n" +
                        "/dduyf addgroup [name]       Adds a group to config file \n" +
                        "/dduyf removegroup [name]    Removes a group from config file\n");
            } else {
                theSender.sendMessage(getMessages("dduyf.NoPermission"));
            }
        }
        return true;
    }
}