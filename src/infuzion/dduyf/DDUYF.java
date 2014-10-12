package infuzion.dduyf;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.UUID;
import java.util.logging.Logger;

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
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.plugin.java.JavaPlugin;

public class DDUYF extends JavaPlugin
  implements Listener
{
  Logger ConsoleLogger = Bukkit.getLogger();
  File Messagesfile;
  FileConfiguration messagesConfig;
  HashMap<UUID, Integer> blocks = new HashMap<UUID, Integer>();

  public void onEnable()
  {
    getServer().getPluginManager().registerEvents(this, this);
    initConfig();
    this.Messagesfile = new File(getDataFolder(), "messages.yml");
    this.messagesConfig = YamlConfiguration.loadConfiguration(this.Messagesfile);
    saveMessages();
  }

  private void initMessages() {
    this.messagesConfig.addDefault("DDUYF.ReloadSuccess", "&aConfig Successfully Reloaded!");
    this.messagesConfig.addDefault("DDUYF.RemoveDefault", "&4You cannot delete the default group!");
    this.messagesConfig.addDefault("DDUYF.NoPermission", "&4You do not have permission for this command.");
    this.messagesConfig.addDefault("DDUYF.AddGroup", "&aGroup Successfully added!");
    this.messagesConfig.addDefault("DDUYF.RemoveGroupFailed", "&4The group \"%name%\" does not exist!");
    this.messagesConfig.addDefault("DDUYF.RemoveGroupSuccess", "&aThe group has been successfully removed!");
    this.messagesConfig.options().copyDefaults(true);
  }

  private void saveMessages() {
    initMessages();
    try { messagesConfig.save(this.Messagesfile); } catch (Exception e) { e.printStackTrace(); }
  }

  private String getMessages(String message)
  {
    String ret = ChatColor.translateAlternateColorCodes('&', messagesConfig.getString(message));
    return ret;
  }

  private void initConfig() {
    FileConfiguration config = getConfig();
    config.options().header("**************************DDUYF************************** #\n***************(Don't Dig Under Your Feet)*************** #\n********************************************************* #\n****Damage is measured per half heart. 1 is 1/2 heart**** #\n****If Use Permission is enabled anyone with the node:*** #\n**************DDUYF.exempt will be exempted************** #\n********************************************************* #\n*Use /dduyf addgroup [name] instead of directly changing* #\n************************this file************************ #\n********************************************************* #\n*.Permission is the permission to be used for that group* #\n********************************************************* #\n*************Group Names are cAsE Sensitive!************* #\n********************************************************* #\n");

    ArrayList<String> t = new ArrayList<String>();
    t.add("default");
    config.addDefault("DDUYF.Options.ExemptOps", Boolean.valueOf(true));
    config.addDefault("DDUYF.Groups.Names", t);
    config.addDefault("DDUYF.Groups.default.Damage", Double.valueOf(1.0D));
    config.addDefault("DDUYF.Groups.default.Blocks", Integer.valueOf(1));
    config.addDefault("DDUYF.Groups.default.Permission", "DDUYF.Groups.default");
    config.addDefault("DDUYF.Groups.default.UsePermissions", Boolean.valueOf(false));
    config.addDefault("DDUYF.Groups.default.UseMessage", Boolean.valueOf(true));
    config.addDefault("DDUYF.Groups.default.Message", "&4 Shrapnel explodes under your feet causing you to take damage!");
    config.options().copyDefaults(true);
    saveConfig();
    reloadConfig();
  }

  private void addGroup(String name) {
    FileConfiguration config = getConfig();
    saveConfig();
    ArrayList<String> t = (ArrayList<String>)config.getStringList("DDUYF.Groups.Names");
    t.add(name);
    config.set("DDUYF.Groups.Names", t);
    config.set("DDUYF.Groups." + name + ".Damage", Double.valueOf(1.0D));
    config.set("DDUYF.Groups." + name + ".Blocks", Integer.valueOf(1));
    config.set("DDUYF.Groups." + name + ".Permission", "DDUYF.Groups." + name);
    config.set("DDUYF.Groups." + name + ".UsePermissions", Boolean.valueOf(false));
    config.set("DDUYF.Groups." + name + ".UseMessage", Boolean.valueOf(true));
    config.set("DDUYF.Groups." + name + ".Message", "&4 Shrapnel explodes under your feet causing you to take damage!");
    saveConfig();
  }

  private void removeGroup(String name, CommandSender sender) {
    FileConfiguration config = getConfig();
    ArrayList<?> r = (ArrayList<?>)config.getStringList("DDUYF.Groups.Names");
    if (!name.equals("default"))
    {
      if (r.contains(name)) {
        r.remove(name);
        config.set("DDUYF.Groups.Names", r);
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
    Boolean useperm = (Boolean)getConfig().get("DDUYF.UsePermissions");
    if ((!p.hasPermission("DDUYF.exempt")) && (useperm.booleanValue())) {
      return false;
    }
    return true;
  }

  private void addBlock(Player p)
  {
    if (!this.blocks.containsKey(p.getUniqueId()))
      this.blocks.put(p.getUniqueId(), Integer.valueOf(1));
    else if (this.blocks.containsKey(p.getUniqueId()))
      this.blocks.put(p.getUniqueId(), Integer.valueOf(((Integer)this.blocks.get(p.getUniqueId())).intValue() + 1));
  }

  private void clearBlocks(Player p)
  {
    this.blocks.put(p.getUniqueId(), Integer.valueOf(0));
  }

  private int getBlocks(Player p) {
    if (this.blocks.containsKey(p.getUniqueId())) {
      int returnu = ((Integer)this.blocks.get(p.getUniqueId())).intValue();
      return returnu;
    }return 1;
  }

  private String getPlayerGroup(Player p) {
    ArrayList<?> groups = (ArrayList<?>)getConfig().getStringList("DDUYF.Groups.Names");
    String[] group = (String[])groups.toArray(new String[groups.size()]);
    groups = null;

    int n = group.length;
    while (n > 0) {
      String groupn = getConfig().getString("DDUYF.Groups." + group[(n - 1)] + ".Permission");
      if (!groupn.isEmpty()) {
        if (p.hasPermission(groupn)) {
          return group[(n - 1)];
        }
        n--;
      } else {
        return "default";
      }
    }
    String ret = "default";
    return ret;
  }

  private void damage(Player p, int blocks) {
    String pgroup = getPlayerGroup(p);
    Boolean op = Boolean.valueOf(false);
    clearBlocks(p);
    Double d = Double.valueOf(getConfig().getDouble("DDUYF.Groups." + pgroup + ".Damage"));
    int b = getConfig().getInt("DDUYF.Groups." + pgroup + ".Blocks");
    String m = ChatColor.translateAlternateColorCodes('&', getConfig().getString("DDUYF.Groups." + pgroup + ".Message"));
    int bcur = getBlocks(p);
    if ((getConfig().getBoolean("DDUYF.Options.ExemptOp")) && (p.isOp())) {
      op = Boolean.valueOf(true);
    }
    if ((b >= bcur) && ((!p.getGameMode().equals(GameMode.CREATIVE)) || (op.booleanValue()))) {
      p.damage(d.doubleValue());
      p.sendMessage(m);
      clearBlocks(p);
    }
  }

  @EventHandler
  public void onBlockBreak(BlockBreakEvent e) {
    Block b = e.getBlock();
    Player p = e.getPlayer();
    Location pl = p.getLocation();
    Location bl = b.getLocation();
    if ((pl.getBlockX() == bl.getBlockX()) && (pl.getBlockY() - 1 == bl.getBlockY()) && (pl.getBlockZ() == bl.getBlockZ()) && (permission(p))) {
      addBlock(p);
      damage(p, getBlocks(p));
    }
  }

  public boolean onCommand(CommandSender theSender, Command cmd, String commandLabel, String[] args) {
    if ((commandLabel.equalsIgnoreCase("DDUYF")) || (commandLabel.equalsIgnoreCase("DontDigUnderYourFeet"))) {
      if (args.length >= 1) {
        if (args[0].equalsIgnoreCase("reload")) {
          if (theSender.hasPermission("DDUYF.reload")) {
            reloadConfig();
            theSender.sendMessage(getMessages("DDUYF.ReloadSuccess"));
          } else {
            theSender.sendMessage(getMessages("DDUYF.NoPermission"));
          }
        } else if (args[0].equalsIgnoreCase("addgroup")) {
          if (theSender.hasPermission("DDUYF.addgroup"))
            if (args.length > 1) {
              addGroup(args[1]);
              theSender.sendMessage(getMessages("DDUYF.AddGroup"));
            } else {
              theSender.sendMessage(ChatColor.RED + "Usage: /dduyf addgroup [groupname]");
            }
        }
        else if (args[0].equalsIgnoreCase("removegroup")) {
          if (theSender.hasPermission("DDUYF.removegroup")) {
            if (args.length > 1)
              removeGroup(args[1], theSender);
            else
              theSender.sendMessage(ChatColor.RED + "Usage: /dduyf removegroup [groupname]");
          }
          else
            theSender.sendMessage(getMessages("DDUYF.NoPermission"));
        }
        else if (theSender.hasPermission("DDUYF.access")) {
          theSender.sendMessage(ChatColor.RED + 
            "[DDUYF] Commands:\n" + 
            "/dduyf reload                Reloads configuration \n" + 
            "/dduyf addgroup [name]       Adds a group to config file \n" + 
            "/dduyf removegroup [name]    Removes a group from config file\n");
        }
        else
        {
          theSender.sendMessage(getMessages("DDUYF.NoPermission"));
        }
      } else if (theSender.hasPermission("DDUYF.access")) {
        theSender.sendMessage(ChatColor.RED + 
          "[DDUYF] Commands:\n" + 
          "/dduyf reload                Reloads configuration \n" + 
          "/dduyf addgroup [name]       Adds a group to config file \n" + 
          "/dduyf removegroup [name]    Removes a group from config file\n");
      }
      else {
        theSender.sendMessage(getMessages("DDUYF.NoPermission"));
      }
    }
    return true;
  }
}