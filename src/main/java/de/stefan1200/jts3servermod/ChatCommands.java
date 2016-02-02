package de.stefan1200.jts3servermod;

import de.stefan1200.jts3servermod.interfaces.ClientDatabaseCache_Interface;
import de.stefan1200.jts3servermod.interfaces.ServerInfoCache_Interface;
import de.stefan1200.jts3serverquery.JTS3ServerQuery;
import de.stefan1200.jts3serverquery.TS3ServerQueryException;
import de.stefan1200.util.ArrangedPropertiesWriter;

import java.io.BufferedInputStream;
import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.StringTokenizer;
import java.util.Vector;

class ChatCommands
{
  ArrangedPropertiesWriter config;
  JTS3ServerMod modClass;
  JTS3ServerQuery queryLib;
  ClientDatabaseCache_Interface clientCache;
  InstanceManager manager;
  SimpleDateFormat sdf;
  
  ChatCommands(JTS3ServerQuery queryLib, JTS3ServerMod modClass, ClientDatabaseCache_Interface clientCache, SimpleDateFormat sdf, ArrangedPropertiesWriter config, InstanceManager manager)
  {
    this.queryLib = queryLib;
    this.modClass = modClass;
    this.clientCache = clientCache;
    this.sdf = sdf;
    this.config = config;
    this.manager = manager;
  }
  
  void handleBotQuit(String msg, HashMap<String, String> eventInfo, String instanceName, boolean isFullAdmin)
  {
    this.modClass.addLogEntry((byte)1, "Got command from " + (String)eventInfo.get("invokername") + ": " + msg, false);
    if (isFullAdmin)
    {
      try
      {
        this.queryLib.sendTextMessage(Integer.parseInt((String)eventInfo.get("invokerid")), 1, "Bye Bye, my master! Stopping all instances...");
      }
      catch (Exception e)
      {
        this.modClass.addLogEntry(e, false);
      }
      this.manager.stopAllInstances("COMMAND", "Got !botquit command from " + (String)eventInfo.get("invokername") + " (UID: " + (String)eventInfo.get("invokeruid") + ") on virtual bot instance " + instanceName);
    }
    else
    {
      try
      {
        this.queryLib.sendTextMessage(Integer.parseInt((String)eventInfo.get("invokerid")), 1, "You are not my master! You have to be full bot admin to use this command.");
      }
      catch (Exception e)
      {
        this.modClass.addLogEntry(e, false);
      }
    }
  }
  
  void handleBotReload(String msg, HashMap<String, String> eventInfo, boolean isFullAdmin, boolean isAdmin, String CONFIG_FILE_NAME)
  {
    this.modClass.addLogEntry((byte)1, "Got command from " + (String)eventInfo.get("invokername") + ": " + msg, false);
    if ((isFullAdmin) || (isAdmin))
    {
      JTS3ServerMod configCheck = new JTS3ServerMod(CONFIG_FILE_NAME);
      int configOK = configCheck.loadAndCheckConfig(true);
      if (configOK == 0)
      {
        try
        {
          this.queryLib.sendTextMessage(Integer.parseInt((String)eventInfo.get("invokerid")), 1, "Config file OK, restarting now!");
        }
        catch (Exception e)
        {
          this.modClass.addLogEntry(e, false);
        }
        this.modClass.stopBotInstance(2);
      }
      else
      {
        String errorMsg = configCheck.getErrorMessage(configOK);
        try
        {
          if (errorMsg.length() > 954) {
            errorMsg = errorMsg.substring(0, 950) + "\n[...]";
          }
          this.queryLib.sendTextMessage(Integer.parseInt((String)eventInfo.get("invokerid")), 1, "Config checked and found following errors:\n" + errorMsg);
        }
        catch (Exception e)
        {
          this.modClass.addLogEntry(e, false);
        }
      }
    }
    else
    {
      try
      {
        this.queryLib.sendTextMessage(Integer.parseInt((String)eventInfo.get("invokerid")), 1, "You are not my master!");
      }
      catch (Exception e)
      {
        this.modClass.addLogEntry(e, false);
      }
    }
  }
  
  void handleBotVersionCheck(String msg, HashMap<String, String> eventInfo, boolean isFullAdmin, boolean isAdmin)
  {
    this.modClass.addLogEntry((byte)1, "Got command from " + (String)eventInfo.get("invokername") + ": " + msg, false);
    try
    {
      if ((isFullAdmin) || (isAdmin))
      {
        StringBuffer versionInfo = new StringBuffer();
        try
        {
          HashMap<String, String> versionData = JTS3ServerMod.getVersionCheckData();
          if (versionData != null)
          {
            if ((versionData.get("final.version") != null) && (versionData.get("final.url") != null)) {
              versionInfo.append("\n[b]Latest final version:[/b] " + (String)versionData.get("final.version") + " [" + (String)versionData.get("final.build") + "]" + " - [url=" + (String)versionData.get("final.url") + "]Download[/url]");
            }
            if ((versionData.get("dev.version") != null) && (versionData.get("dev.url") != null)) {
              versionInfo.append("\n[b]Latest development version:[/b] " + (String)versionData.get("dev.version") + " [" + (String)versionData.get("dev.build") + "]" + " - [url=" + (String)versionData.get("dev.url") + "]Download[/url]");
            }
          }
        }
        catch (Exception localException1) {}
        if (versionInfo.length() == 0) {
          versionInfo.append("\nError while getting version information!");
        }
        this.queryLib.sendTextMessage(Integer.parseInt((String)eventInfo.get("invokerid")), 1, "[b]Current installed version:[/b] 5.5.4 (11.07.2015) [5504]" + versionInfo.toString());
      }
      else
      {
        this.queryLib.sendTextMessage(Integer.parseInt((String)eventInfo.get("invokerid")), 1, "You are not my master!");
      }
    }
    catch (Exception e)
    {
      this.modClass.addLogEntry(e, false);
    }
  }
  
  void handleBotFunctionList(String msg, HashMap<String, String> eventInfo, boolean isFullAdmin, boolean isAdmin)
  {
    this.modClass.addLogEntry((byte)1, "Got command from " + (String)eventInfo.get("invokername") + ": " + msg, false);
    try
    {
      if ((isFullAdmin) || (isAdmin))
      {
        String[] funcList = this.modClass.getCurrentLoadedFunctions();
        this.queryLib.sendTextMessage(Integer.parseInt((String)eventInfo.get("invokerid")), 1, 
          (funcList[0].length() == 0 ? "[b]No bot functions currently activated![/b]" : new StringBuilder("[b]The following bot functions are activated:[/b]\n").append(funcList[0]).toString()) + (
          funcList[1].length() == 0 ? "\n[b]No bot functions currently disabled![/b]" : new StringBuilder("\n[b]The following bot functions are disabled:[/b]\n").append(funcList[1]).toString()));
      }
      else
      {
        this.queryLib.sendTextMessage(Integer.parseInt((String)eventInfo.get("invokerid")), 1, "You are not my master!");
      }
    }
    catch (Exception e)
    {
      this.modClass.addLogEntry(e, false);
    }
  }
  
  void handleBotFunctionActivate(String msg, HashMap<String, String> eventInfo, boolean isFullAdmin, boolean isAdmin)
  {
    this.modClass.addLogEntry((byte)1, "Got command from " + (String)eventInfo.get("invokername") + ": " + msg, false);
    String arguments = getArguments(msg);
    try
    {
      if ((isFullAdmin) || (isAdmin))
      {
        String answermsg = "Unknown Error!";
        if (arguments.length() == 0)
        {
          answermsg = "Wrong usage! Right: !botfunctionactivate <function prefix>";
        }
        else
        {
          byte result = this.modClass.activateFunction(arguments);
          if (result == -1) {
            answermsg = "Unable to activate function \"" + arguments + "\", unknown name!";
          } else if (result == 0) {
            answermsg = "Function \"" + arguments + "\" already activated!";
          } else if (result == 1) {
            answermsg = "Function \"" + arguments + "\" activated successfully! If wanted, save bot config with !botcfgsave to make change permanent.";
          }
        }
        this.queryLib.sendTextMessage(Integer.parseInt((String)eventInfo.get("invokerid")), 1, answermsg);
      }
      else
      {
        this.queryLib.sendTextMessage(Integer.parseInt((String)eventInfo.get("invokerid")), 1, "You are not my master!");
      }
    }
    catch (Exception e)
    {
      this.modClass.addLogEntry(e, false);
    }
  }
  
  void handleBotFunctionDisable(String msg, HashMap<String, String> eventInfo, boolean isFullAdmin, boolean isAdmin)
  {
    this.modClass.addLogEntry((byte)1, "Got command from " + (String)eventInfo.get("invokername") + ": " + msg, false);
    String arguments = getArguments(msg);
    try
    {
      if ((isFullAdmin) || (isAdmin))
      {
        String answermsg = "Unknown Error!";
        if (arguments.length() == 0)
        {
          answermsg = "Wrong usage! Right: !botfunctiondisable <function prefix>";
        }
        else
        {
          byte result = this.modClass.disableFunction(arguments);
          if (result == -1) {
            answermsg = "Unable to disable function \"" + arguments + "\", unknown name!";
          } else if (result == 0) {
            answermsg = "Function \"" + arguments + "\" already disabled!";
          } else if (result == 1) {
            answermsg = "Function \"" + arguments + "\" disabled successfully! If wanted, save bot config with !botcfgsave to make change permanent.";
          }
        }
        this.queryLib.sendTextMessage(Integer.parseInt((String)eventInfo.get("invokerid")), 1, answermsg);
      }
      else
      {
        this.queryLib.sendTextMessage(Integer.parseInt((String)eventInfo.get("invokerid")), 1, "You are not my master!");
      }
    }
    catch (Exception e)
    {
      this.modClass.addLogEntry(e, false);
    }
  }
  
  void handleBotInfo(String msg, HashMap<String, String> eventInfo, long startTime, boolean isFullAdmin, boolean isAdmin)
  {
    String adminText = "You have no bot admin permissions!";
    if (isFullAdmin) {
      adminText = "You have all bot admin permissions!";
    } else if (isAdmin) {
      adminText = "You have limited bot admin permissions!";
    }
    this.modClass.addLogEntry((byte)1, "Got command from " + (String)eventInfo.get("invokername") + ": " + msg, false);
    try
    {
      this.queryLib.sendTextMessage(Integer.parseInt((String)eventInfo.get("invokerid")), 1, "On this server runs JTS3ServerMod 5.5.4 (11.07.2015) since " + 
        this.modClass.getDifferenceTime(startTime, System.currentTimeMillis()) + ".\nTry !bothelp for a list of commands! " + adminText + 
        "\nYou like this bot? Consider a [url=http://www.stefan1200.de/forum/index.php?topic=189.0]donation[/url]!");
    }
    catch (Exception e)
    {
      this.modClass.addLogEntry(e, false);
    }
  }
  
  void handleBotInstanceStop(String msg, HashMap<String, String> eventInfo, boolean isFullAdmin, String instanceName)
  {
    this.modClass.addLogEntry((byte)1, "Got command from " + (String)eventInfo.get("invokername") + ": " + msg, false);
    String arguments = getArguments(msg);
    if (isFullAdmin)
    {
      if (arguments.length() == 0)
      {
        try
        {
          this.queryLib.sendTextMessage(Integer.parseInt((String)eventInfo.get("invokerid")), 1, "Bye Bye, my master! Stopping this instance...");
        }
        catch (Exception e)
        {
          this.modClass.addLogEntry(e, false);
        }
        this.modClass.stopBotInstance(0);
      }
      else if (arguments.equalsIgnoreCase(instanceName))
      {
        try
        {
          this.queryLib.sendTextMessage(Integer.parseInt((String)eventInfo.get("invokerid")), 1, "Bye Bye, my master! Stopping this instance...");
        }
        catch (Exception e)
        {
          this.modClass.addLogEntry(e, false);
        }
        this.modClass.stopBotInstance(0);
      }
      else
      {
        try
        {
          if (this.manager.stopInstance(arguments)) {
            this.queryLib.sendTextMessage(Integer.parseInt((String)eventInfo.get("invokerid")), 1, "Instance [b]" + arguments + "[/b] stopped!");
          } else {
            this.queryLib.sendTextMessage(Integer.parseInt((String)eventInfo.get("invokerid")), 1, "Instance name [b]" + arguments + 
              "[/b] not found or not running! Get a instance name list with !botinstancelist");
          }
        }
        catch (Exception e)
        {
          this.modClass.addLogEntry(e, false);
        }
      }
    }
    else {
      try
      {
        this.queryLib.sendTextMessage(Integer.parseInt((String)eventInfo.get("invokerid")), 1, 
          "You are not my master! You have to be full bot admin to use this command.");
      }
      catch (Exception e)
      {
        this.modClass.addLogEntry(e, false);
      }
    }
  }
  
  void handleBotInstanceStart(String msg, HashMap<String, String> eventInfo, boolean isFullAdmin, String instanceName)
  {
    this.modClass.addLogEntry((byte)1, "Got command from " + (String)eventInfo.get("invokername") + ": " + msg, false);
    String arguments = getArguments(msg);
    try
    {
      if (isFullAdmin)
      {
        if (arguments.length() == 0) {
          this.queryLib.sendTextMessage(Integer.parseInt((String)eventInfo.get("invokerid")), 1, "Wrong usage! Right: !botinstancestart <name>");
        } else if (arguments.equalsIgnoreCase(instanceName)) {
          this.queryLib.sendTextMessage(Integer.parseInt((String)eventInfo.get("invokerid")), 1, "Instance [b]" + arguments + "[/b] is already running!");
        } else if (this.manager.startInstance(arguments)) {
          this.queryLib.sendTextMessage(Integer.parseInt((String)eventInfo.get("invokerid")), 1, "Instance [b]" + arguments + "[/b] started!");
        } else {
          this.queryLib.sendTextMessage(Integer.parseInt((String)eventInfo.get("invokerid")), 1, "Instance name [b]" + arguments + 
            "[/b] not found, is already running or config file missing! Get a instance name list with !botinstancelist");
        }
      }
      else {
        this.queryLib.sendTextMessage(Integer.parseInt((String)eventInfo.get("invokerid")), 1, 
          "You are not my master! You have to be full bot admin to use this command.");
      }
    }
    catch (Exception e)
    {
      this.modClass.addLogEntry(e, false);
    }
  }
  
  void handleBotInstanceList(String msg, HashMap<String, String> eventInfo, boolean isFullAdmin)
  {
    this.modClass.addLogEntry((byte)1, "Got command from " + (String)eventInfo.get("invokername") + ": " + msg, false);
    try
    {
      if (isFullAdmin)
      {
        StringBuffer instanceString = new StringBuffer();
        Vector<String> instanceNames = this.manager.getInstanceNames();
        for (String string : instanceNames)
        {
          if (instanceString.length() > 0) {
            instanceString.append("\n");
          }
          instanceString.append(string);
          instanceString.append(" - ");
          if (this.manager.isInstanceRunning(string) == 1) {
            instanceString.append("Running");
          } else {
            instanceString.append("Not Running");
          }
        }
        this.queryLib.sendTextMessage(Integer.parseInt((String)eventInfo.get("invokerid")), 1, "[b]List of instance names:[/b]\n" + instanceString.toString());
      }
      else
      {
        this.queryLib.sendTextMessage(Integer.parseInt((String)eventInfo.get("invokerid")), 1, 
          "You are not my master! You have to be full bot admin to use this command.");
      }
    }
    catch (Exception e)
    {
      this.modClass.addLogEntry(e, false);
    }
  }
  
  void handleBotInstanceListReload(String msg, HashMap<String, String> eventInfo, boolean isFullAdmin)
  {
    this.modClass.addLogEntry((byte)1, "Got command from " + (String)eventInfo.get("invokername") + ": " + msg, false);
    try
    {
      if (isFullAdmin)
      {
        if (this.manager.loadConfig()) {
          this.queryLib.sendTextMessage(Integer.parseInt((String)eventInfo.get("invokerid")), 1, "Succesfully reloaded the instance list!");
        } else {
          this.queryLib.sendTextMessage(Integer.parseInt((String)eventInfo.get("invokerid")), 1, "Error while reloading the instance list!");
        }
      }
      else {
        this.queryLib.sendTextMessage(Integer.parseInt((String)eventInfo.get("invokerid")), 1, 
          "You are not my master! You have to be full bot admin to use this command.");
      }
    }
    catch (Exception e)
    {
      this.modClass.addLogEntry(e, false);
    }
  }
  
  void handleBotReloadAll(String msg, HashMap<String, String> eventInfo, boolean isFullAdmin)
  {
    this.modClass.addLogEntry((byte)1, "Got command from " + (String)eventInfo.get("invokername") + ": " + msg, false);
    if (isFullAdmin)
    {
      try
      {
        this.queryLib.sendTextMessage(Integer.parseInt((String)eventInfo.get("invokerid")), 1, "Restarting all instances!");
      }
      catch (Exception e)
      {
        this.modClass.addLogEntry(e, false);
      }
      this.manager.reloadAllInstances();
    }
    else
    {
      try
      {
        this.queryLib.sendTextMessage(Integer.parseInt((String)eventInfo.get("invokerid")), 1, "You are not my master!");
      }
      catch (Exception e)
      {
        this.modClass.addLogEntry(e, false);
      }
    }
  }
  
  void handleBotCfgReload(String msg, HashMap<String, String> eventInfo, boolean isFullAdmin, boolean isAdmin)
  {
    this.modClass.addLogEntry((byte)1, "Got command from " + (String)eventInfo.get("invokername") + ": " + msg, false);
    String arguments = getArguments(msg);
    try
    {
      if ((isFullAdmin) || (isAdmin))
      {
        if (this.modClass.loadConfigValues())
        {
          if (arguments.length() > 1)
          {
            int result = this.modClass.reloadConfig(arguments);
            String msgTemp;
            if (result == 1)
            {
              msgTemp = "Bot function \"" + arguments + "\" reloaded config successfully!";
            }
            else
            {
              if (result == 0) {
                msgTemp = "Error while reloading config of bot function \"" + arguments + "\"!";
              } else {
                msgTemp = "Bot function \"" + arguments + "\" not found!";
              }
            }
            this.queryLib.sendTextMessage(Integer.parseInt((String)eventInfo.get("invokerid")), 1, msgTemp);
          }
          else
          {
            int[] count = this.modClass.reloadConfig(false);
            this.queryLib.sendTextMessage(Integer.parseInt((String)eventInfo.get("invokerid")), 1, 
              "All bot functions reloaded config, successfull: " + Integer.toString(count[1]) + " / with error: " + Integer.toString(count[0]));
          }
        }
        else {
          this.queryLib.sendTextMessage(Integer.parseInt((String)eventInfo.get("invokerid")), 1, "Main bot config file could not be reloaded!");
        }
      }
      else {
        this.queryLib.sendTextMessage(Integer.parseInt((String)eventInfo.get("invokerid")), 1, "You are not my master!");
      }
    }
    catch (Exception e)
    {
      this.modClass.addLogEntry(e, false);
    }
  }
  
  void handleBotCfgHelp(String msg, HashMap<String, String> eventInfo, boolean isFullAdmin, boolean isAdmin)
  {
    this.modClass.addLogEntry((byte)1, "Got command from " + (String)eventInfo.get("invokername") + ": " + msg, false);
    String arguments = getArguments(msg);
    try
    {
      if ((isFullAdmin) || (isAdmin))
      {
        if (arguments.length() == 0)
        {
          this.queryLib.sendTextMessage(Integer.parseInt((String)eventInfo.get("invokerid")), 1, "[b]List of config keys:[/b]\n");
          
          StringBuffer keyString = new StringBuffer();
          Vector<String> configKeys = this.config.getKeys();
          for (int i = 0; i < configKeys.size(); i++) {
            if ((isFullAdmin) || (!((String)configKeys.elementAt(i)).toLowerCase().startsWith("ts3_")))
            {
              if (keyString.length() != 0) {
                keyString.append(", ");
              }
              keyString.append((String)configKeys.elementAt(i));
              if (keyString.length() > 900)
              {
                this.queryLib.sendTextMessage(Integer.parseInt((String)eventInfo.get("invokerid")), 1, keyString.toString());
                keyString = new StringBuffer();
              }
            }
          }
          this.queryLib.sendTextMessage(Integer.parseInt((String)eventInfo.get("invokerid")), 1, keyString.toString());
        }
        else
        {
          String helpText = this.config.getHelpText(arguments);
          if (helpText == null)
          {
            StringBuffer keyString = new StringBuffer();
            Vector<String> configKeys = this.config.getKeys();
            for (int i = 0; i < configKeys.size(); i++) {
              if (((String)configKeys.elementAt(i)).toLowerCase().startsWith(arguments.toLowerCase()))
              {
                if (keyString.length() != 0) {
                  keyString.append(", ");
                }
                keyString.append((String)configKeys.elementAt(i));
              }
            }
            if (keyString.length() > 0)
            {
              this.queryLib.sendTextMessage(Integer.parseInt((String)eventInfo.get("invokerid")), 1, "[b]List of config keys starting with:[/b] " + arguments + 
                "\n");
              this.queryLib.sendTextMessage(Integer.parseInt((String)eventInfo.get("invokerid")), 1, keyString.toString());
            }
            else
            {
              this.queryLib.sendTextMessage(Integer.parseInt((String)eventInfo.get("invokerid")), 1, "Key [b]" + arguments + "[/b] is not valid!");
            }
          }
          else
          {
            if (helpText.length() > 1000 - arguments.length()) {
              helpText = helpText.substring(0, 997 - arguments.length()) + "...";
            }
            this.queryLib.sendTextMessage(Integer.parseInt((String)eventInfo.get("invokerid")), 1, "[b]Help of " + arguments + ":[/b]\n" + helpText);
          }
        }
      }
      else {
        this.queryLib.sendTextMessage(Integer.parseInt((String)eventInfo.get("invokerid")), 1, "You are not my master!");
      }
    }
    catch (Exception e)
    {
      this.modClass.addLogEntry(e, false);
    }
  }
  
  void handleBotCfgGet(String msg, HashMap<String, String> eventInfo, boolean isFullAdmin, boolean isAdmin)
  {
    this.modClass.addLogEntry((byte)1, "Got command from " + (String)eventInfo.get("invokername") + ": " + msg, false);
    String arguments = getArguments(msg);
    try
    {
      if ((isFullAdmin) || (isAdmin))
      {
        if (arguments.length() == 0)
        {
          this.queryLib.sendTextMessage(Integer.parseInt((String)eventInfo.get("invokerid")), 1, "Wrong usage! Right: !botcfgget <key>");
        }
        else if (arguments.length() < 3)
        {
          this.queryLib.sendTextMessage(Integer.parseInt((String)eventInfo.get("invokerid")), 1, "Wrong usage! Right: !botcfgget <key>");
        }
        else if ((!isFullAdmin) && (arguments.toLowerCase().startsWith("ts3_")))
        {
          this.queryLib.sendTextMessage(Integer.parseInt((String)eventInfo.get("invokerid")), 1, "Requesting value of key [b]" + arguments + 
            "[/b] is not allowed!");
        }
        else
        {
          String value = this.config.getValue(arguments);
          if (value == null)
          {
            if (this.config.getKeys().indexOf(arguments) == -1) {
              this.queryLib.sendTextMessage(Integer.parseInt((String)eventInfo.get("invokerid")), 1, "Key [b]" + arguments + "[/b] is not valid!");
            } else {
              this.queryLib.sendTextMessage(Integer.parseInt((String)eventInfo.get("invokerid")), 1, "No value set for key [b]" + arguments + "[/b]!");
            }
          }
          else {
            this.queryLib.sendTextMessage(Integer.parseInt((String)eventInfo.get("invokerid")), 1, 
              "Value of [b]" + arguments + "[/b]:" + (value.length() > 10 ? "\n" : " ") + value);
          }
        }
      }
      else {
        this.queryLib.sendTextMessage(Integer.parseInt((String)eventInfo.get("invokerid")), 1, "You are not my master!");
      }
    }
    catch (Exception e)
    {
      this.modClass.addLogEntry(e, false);
    }
  }
  
  void handleBotCfgSet(String msg, HashMap<String, String> eventInfo, boolean isFullAdmin, boolean isAdmin)
  {
    this.modClass.addLogEntry((byte)1, "Got command from " + (String)eventInfo.get("invokername") + ": " + msg, false);
    String arguments = getArguments(msg);
    try
    {
      if ((isFullAdmin) || (isAdmin))
      {
        if (arguments.length() == 0)
        {
          this.queryLib.sendTextMessage(Integer.parseInt((String)eventInfo.get("invokerid")), 1, "Wrong usage! Right: !botcfgset <key> = <value>");
        }
        else
        {
          int pos = arguments.indexOf("=");
          if ((arguments.length() < 3) || (pos == -1))
          {
            this.queryLib.sendTextMessage(Integer.parseInt((String)eventInfo.get("invokerid")), 1, "Wrong usage! Right: !botcfgset <key> = <value>");
          }
          else
          {
            String key = arguments.substring(0, pos).trim();
            String value = arguments.substring(pos + 1).trim();
            if (!this.config.canSaveToFile(key))
            {
              this.queryLib.sendTextMessage(
                Integer.parseInt((String)eventInfo.get("invokerid")), 
                1, 
                "Setting value for key [b]" + 
                key + 
                "[/b] is not possible, because it's write protected! Please change it directly at the file and use !botcfgreload to reload this values without restarting the bot.");
              return;
            }
            if ((!isFullAdmin) && (key.toLowerCase().startsWith("ts3_")))
            {
              this.queryLib.sendTextMessage(Integer.parseInt((String)eventInfo.get("invokerid")), 1, "Setting value for key [b]" + key + 
                "[/b] is not allowed!");
              return;
            }
            if (this.config.setValue(key, value)) {
              this.queryLib.sendTextMessage(Integer.parseInt((String)eventInfo.get("invokerid")), 1, "Successfully set key [b]" + key + "[/b] to value:" + (
                value.length() > 6 ? "\n" : " ") + value + "\nDon't forget to do [b]!botcfgsave[/b] to make this change permanent!");
            } else {
              this.queryLib.sendTextMessage(Integer.parseInt((String)eventInfo.get("invokerid")), 1, "Key [b]" + key + "[/b] is not valid!");
            }
          }
        }
      }
      else {
        this.queryLib.sendTextMessage(Integer.parseInt((String)eventInfo.get("invokerid")), 1, "You are not my master!");
      }
    }
    catch (Exception e)
    {
      this.modClass.addLogEntry(e, false);
    }
  }
  
  void handleBotCfgCheck(String msg, HashMap<String, String> eventInfo, boolean isFullAdmin, boolean isAdmin, String CONFIG_FILE_NAME)
  {
    this.modClass.addLogEntry((byte)1, "Got command from " + (String)eventInfo.get("invokername") + ": " + msg, false);
    try
    {
      if ((isFullAdmin) || (isAdmin))
      {
        JTS3ServerMod configCheck = new JTS3ServerMod(this.config, CONFIG_FILE_NAME);
        int configOK = configCheck.loadAndCheckConfig(false);
        if (configOK == 0)
        {
          this.queryLib.sendTextMessage(Integer.parseInt((String)eventInfo.get("invokerid")), 1, "Config OK!");
        }
        else
        {
          String errorMsg = configCheck.getErrorMessage(configOK);
          if (errorMsg.length() > 954) {
            errorMsg = errorMsg.substring(0, 950) + "\n[...]";
          }
          this.queryLib.sendTextMessage(Integer.parseInt((String)eventInfo.get("invokerid")), 1, "Config checked and found following errors:\n" + errorMsg);
        }
      }
      else
      {
        this.queryLib.sendTextMessage(Integer.parseInt((String)eventInfo.get("invokerid")), 1, "You are not my master!");
      }
    }
    catch (Exception e)
    {
      this.modClass.addLogEntry(e, false);
    }
  }
  
  void handleBotCfgSave(String msg, HashMap<String, String> eventInfo, boolean isFullAdmin, boolean isAdmin, String CONFIG_FILE_NAME)
  {
    this.modClass.addLogEntry((byte)1, "Got command from " + (String)eventInfo.get("invokername") + ": " + msg, false);
    try
    {
      if ((isFullAdmin) || (isAdmin))
      {
        JTS3ServerMod configCheck = new JTS3ServerMod(this.config, CONFIG_FILE_NAME);
        int configOK = configCheck.loadAndCheckConfig(false);
        if (configOK == 0)
        {
          if (this.config.save(CONFIG_FILE_NAME, "Config file of the JTS3ServerMod\nhttp://www.stefan1200.de\nThis file must be saved with the encoding ISO-8859-1!")) {
            this.queryLib.sendTextMessage(Integer.parseInt((String)eventInfo.get("invokerid")), 1, 
              "Config OK and written to disk! Do [b]!botcfgreload[/b] or [b]!botreload[/b] to see the changes!");
          } else {
            this.queryLib.sendTextMessage(Integer.parseInt((String)eventInfo.get("invokerid")), 1, 
              "Config OK, but an error occurred while writing to disk! Maybe file write protected?");
          }
        }
        else
        {
          String errorMsg = configCheck.getErrorMessage(configOK);
          if (errorMsg.length() > 954) {
            errorMsg = errorMsg.substring(0, 950) + "\n[...]";
          }
          this.queryLib.sendTextMessage(Integer.parseInt((String)eventInfo.get("invokerid")), 1, "Config checked and found following errors:\n" + errorMsg + 
            "\nNot written to disk!");
        }
      }
      else
      {
        this.queryLib.sendTextMessage(Integer.parseInt((String)eventInfo.get("invokerid")), 1, "You are not my master!");
      }
    }
    catch (Exception e)
    {
      this.modClass.addLogEntry(e, false);
    }
  }
  
  void handleClientSearch(String msg, HashMap<String, String> eventInfo, boolean isFullAdmin, boolean isAdmin)
  {
    this.modClass.addLogEntry((byte)1, "Got command from " + (String)eventInfo.get("invokername") + ": " + msg, false);
    String arguments = getArguments(msg);
    try
    {
      if ((isFullAdmin) || (isAdmin))
      {
        if (this.clientCache == null)
        {
          this.queryLib.sendTextMessage(Integer.parseInt((String)eventInfo.get("invokerid")), 1, "Client database cache disabled, command disabled!");
        }
        else if (arguments.length() == 0)
        {
          this.queryLib.sendTextMessage(Integer.parseInt((String)eventInfo.get("invokerid")), 1, 
            "Wrong usage! Right: !clientsearch <clientname or unique id>\nYou can use * as wildcard (client name only)!");
        }
        else if (arguments.indexOf("**") != -1)
        {
          this.queryLib.sendTextMessage(Integer.parseInt((String)eventInfo.get("invokerid")), 1, "Wrong usage, only single wildcards are allowed!");
        }
        else if (this.clientCache.isUpdateRunning())
        {
          this.queryLib.sendTextMessage(Integer.parseInt((String)eventInfo.get("invokerid")), 1, 
            "Client database cache is updating, please wait some time and try again!");
        }
        else
        {
          Vector<Integer> clientSearch = this.clientCache.searchClientNickname(arguments);
          if (clientSearch == null)
          {
            this.queryLib.sendTextMessage(Integer.parseInt((String)eventInfo.get("invokerid")), 1, 
              "Wrong usage, use a valid search pattern with at least 3 characters!");
          }
          else if (clientSearch.size() == 0)
          {
            this.queryLib.sendTextMessage(Integer.parseInt((String)eventInfo.get("invokerid")), 1, "No clients found in the database!");
          }
          else
          {
            boolean foundClient;
            if (((arguments.indexOf("*") == -1) && (clientSearch.size() <= 19)) || (clientSearch.size() <= 7))
            {
              StringBuffer sb = new StringBuffer("Found " + Integer.toString(clientSearch.size()) + " entries in the database:");
              
              Vector<HashMap<String, String>> clientList = this.modClass.getClientList();
              foundClient = false;
              int pos = 0;
              for (Iterator<Integer> localIterator1 = clientSearch.iterator(); localIterator1.hasNext();)
              {
                int clientDBID = ((Integer)localIterator1.next()).intValue();
                try
                {
                  long createdAt = this.clientCache.getCreatedAt(clientDBID) * 1000L;
                  String temp = "\n[b]" + this.clientCache.getNickname(clientDBID) + "[/b] ([i]DB ID:[/i] " + Integer.toString(clientDBID) + ")  [i]public unique ID:[/i] " + 
                    this.clientCache.getUniqueID(clientDBID) + "  [i]last IP:[/i] " + this.clientCache.getLastIP(clientDBID) + "  [i]created at:[/i] " + 
                    this.sdf.format(new Date(createdAt)) + "  [i]last seen at:[/i] ";
                  for (HashMap<String, String> clientOnline : clientList) {
                    if (clientDBID == Integer.parseInt((String)clientOnline.get("client_database_id")))
                    {
                      temp = temp + "currently online (id: " + (String)clientOnline.get("clid") + ")";
                      foundClient = true;
                      break;
                    }
                  }
                  if (!foundClient) {
                    try
                    {
                      long lastOnline = this.clientCache.getLastOnline(clientDBID) * 1000L;
                      temp = temp + this.sdf.format(new Date(lastOnline));
                    }
                    catch (Exception e)
                    {
                      this.modClass.addLogEntry(e, false);
                    }
                  }
                  foundClient = false;
                  
                  pos = sb.lastIndexOf("�+�");
                  if (pos == -1)
                  {
                    if (this.modClass.getUTF8Length(sb.toString() + temp) > 1024) {
                      sb.append("�+�");
                    }
                  }
                  else if (this.modClass.getUTF8Length(sb.substring(pos + 3) + temp) > 1024) {
                    sb.append("�+�");
                  }
                  sb.append(temp);
                }
                catch (Exception e)
                {
                  this.modClass.addLogEntry(e, false);
                }
              }
              this.modClass.sendMessageToClient(null, "chat", Integer.parseInt((String)eventInfo.get("invokerid")), sb.toString());
            }
            else if ((clientSearch.size() > 7) && (clientSearch.size() <= 20))
            {
              StringBuffer sb = new StringBuffer();
              
              boolean firstLine = true;
              for (Iterator<Integer> foundClient_ = clientSearch.iterator(); foundClient_.hasNext();)
              {
                int clientDBID = ((Integer)foundClient_.next()).intValue();
                if (!firstLine) {
                  sb.append(", ");
                }
                sb.append(this.clientCache.getNickname(clientDBID));
                if (firstLine) {
                  firstLine = false;
                }
              }
              this.queryLib.sendTextMessage(Integer.parseInt((String)eventInfo.get("invokerid")), 1, 
                "Found " + Integer.toString(clientSearch.size()) + " entries in the database using the search string \"" + arguments + "\", please refine your search:");
              this.queryLib.sendTextMessage(Integer.parseInt((String)eventInfo.get("invokerid")), 1, sb.toString());
            }
            else if (clientSearch.size() > 20)
            {
              this.queryLib.sendTextMessage(Integer.parseInt((String)eventInfo.get("invokerid")), 1, 
                "Found " + Integer.toString(clientSearch.size()) + " entries in the database using the search string \"" + arguments + "\", please refine your search!");
            }
          }
        }
      }
      else {
        this.queryLib.sendTextMessage(Integer.parseInt((String)eventInfo.get("invokerid")), 1, "You are not my master!");
      }
    }
    catch (Exception e)
    {
      this.modClass.addLogEntry(e, false);
    }
  }
  
  void handleSearchIP(String msg, HashMap<String, String> eventInfo, boolean isFullAdmin, boolean isAdmin)
  {
    this.modClass.addLogEntry((byte)1, "Got command from " + (String)eventInfo.get("invokername") + ": " + msg, false);
    String arguments = getArguments(msg);
    try
    {
      if ((isFullAdmin) || (isAdmin))
      {
        if ((this.clientCache == null) || (!this.clientCache.isCacheLocal()))
        {
          this.queryLib.sendTextMessage(Integer.parseInt((String)eventInfo.get("invokerid")), 1, "Client database cache disabled, command disabled!");
        }
        else if (arguments.length() == 0)
        {
          this.queryLib.sendTextMessage(Integer.parseInt((String)eventInfo.get("invokerid")), 1, 
            "Wrong usage! Right: !searchip <ip address>\nYou can use * as wildcard!");
        }
        else if (arguments.indexOf("**") != -1)
        {
          this.queryLib.sendTextMessage(Integer.parseInt((String)eventInfo.get("invokerid")), 1, "Wrong usage, only single wildcards are allowed!");
        }
        else if (this.clientCache.isUpdateRunning())
        {
          this.queryLib.sendTextMessage(Integer.parseInt((String)eventInfo.get("invokerid")), 1, 
            "Client database cache is updating, please wait some time and try again!");
        }
        else
        {
          Vector<Integer> ipSearch = this.clientCache.searchIPAddress(arguments);
          if (ipSearch == null)
          {
            this.queryLib.sendTextMessage(Integer.parseInt((String)eventInfo.get("invokerid")), 1, 
              "Wrong usage, use a valid search pattern with at least 3 characters!");
          }
          else if (ipSearch.size() == 0)
          {
            this.queryLib.sendTextMessage(Integer.parseInt((String)eventInfo.get("invokerid")), 1, "No clients found in the database!");
          }
          else
          {
            boolean foundClient;
            if (((arguments.indexOf("*") == -1) && (ipSearch.size() <= 19)) || (ipSearch.size() <= 7))
            {
              StringBuffer sb = new StringBuffer("Found " + Integer.toString(ipSearch.size()) + " entries in the database:");
              
              Vector<HashMap<String, String>> clientList = this.modClass.getClientList();
              foundClient = false;
              int pos = 0;
              for (Iterator<Integer> localIterator1 = ipSearch.iterator(); localIterator1.hasNext();)
              {
                int clientDBID = ((Integer)localIterator1.next()).intValue();
                try
                {
                  long createdAt = this.clientCache.getCreatedAt(clientDBID) * 1000L;
                  String temp = "\n[b]" + this.clientCache.getNickname(clientDBID) + "[/b] ([i]DB ID:[/i] " + Integer.toString(clientDBID) + ")  [i]public unique ID:[/i] " + 
                    this.clientCache.getUniqueID(clientDBID) + "  [i]last IP:[/i] " + this.clientCache.getLastIP(clientDBID) + "  [i]created at:[/i] " + 
                    this.sdf.format(new Date(createdAt)) + "  [i]last seen at:[/i] ";
                  for (HashMap<String, String> clientOnline : clientList) {
                    if (clientDBID == Integer.parseInt((String)clientOnline.get("client_database_id")))
                    {
                      temp = temp + "currently online (id: " + (String)clientOnline.get("clid") + ")";
                      foundClient = true;
                      break;
                    }
                  }
                  if (!foundClient) {
                    try
                    {
                      long lastOnline = this.clientCache.getLastOnline(clientDBID) * 1000L;
                      temp = temp + this.sdf.format(new Date(lastOnline));
                    }
                    catch (Exception e)
                    {
                      this.modClass.addLogEntry(e, false);
                    }
                  }
                  foundClient = false;
                  
                  pos = sb.lastIndexOf("�+�");
                  if (pos == -1)
                  {
                    if (this.modClass.getUTF8Length(sb.toString() + temp) > 1024) {
                      sb.append("�+�");
                    }
                  }
                  else if (this.modClass.getUTF8Length(sb.substring(pos + 3) + temp) > 1024) {
                    sb.append("�+�");
                  }
                  sb.append(temp);
                }
                catch (Exception e)
                {
                  this.modClass.addLogEntry(e, false);
                }
              }
              this.modClass.sendMessageToClient(null, "chat", Integer.parseInt((String)eventInfo.get("invokerid")), sb.toString());
            }
            else if ((ipSearch.size() > 7) && (ipSearch.size() <= 25))
            {
              StringBuffer sb = new StringBuffer("Found " + Integer.toString(ipSearch.size()) + " entries in the database:\n");
              
              boolean firstLine = true;
              for (Iterator<Integer> foundClient_ = ipSearch.iterator(); foundClient_.hasNext();)
              {
                int clientDBID = ((Integer)foundClient_.next()).intValue();
                if (!firstLine) {
                  sb.append(", ");
                }
                sb.append(this.clientCache.getLastIP(clientDBID));
                if (firstLine) {
                  firstLine = false;
                }
              }
              sb.append("\nPlease refine your search!");
              
              this.queryLib.sendTextMessage(Integer.parseInt((String)eventInfo.get("invokerid")), 1, sb.toString());
            }
            else if (ipSearch.size() > 25)
            {
              this.queryLib.sendTextMessage(Integer.parseInt((String)eventInfo.get("invokerid")), 1, "Found " + Integer.toString(ipSearch.size()) + 
                " entries in the database using the search string \"" + arguments + "\", please refine your search!");
            }
          }
        }
      }
      else {
        this.queryLib.sendTextMessage(Integer.parseInt((String)eventInfo.get("invokerid")), 1, "You are not my master!");
      }
    }
    catch (Exception e)
    {
      this.modClass.addLogEntry(e, false);
    }
  }
  
  void handleListInactiveClients(String msg, HashMap<String, String> eventInfo, boolean isFullAdmin, boolean isAdmin)
  {
    this.modClass.addLogEntry((byte)1, "Got command from " + (String)eventInfo.get("invokername") + ": " + msg, false);
    String arguments = getArguments(msg);
    try
    {
      if ((isFullAdmin) || (isAdmin))
      {
        if ((this.clientCache == null) || (!this.clientCache.isCacheLocal()))
        {
          this.queryLib.sendTextMessage(Integer.parseInt((String)eventInfo.get("invokerid")), 1, "Client database cache disabled, command disabled!");
        }
        else
        {
          boolean descMode = true;
          int days = 10;
          if (arguments.length() > 0) {
            try
            {
              days = Integer.parseInt(arguments);
              descMode = false;
            }
            catch (Exception e)
            {
              this.queryLib.sendTextMessage(Integer.parseInt((String)eventInfo.get("invokerid")), 1, 
                "Wrong usage! Right: !listinactiveclients [minimum days inactive]");
              return;
            }
          }
          if (days >= 10)
          {
            Vector<HashMap<String, Integer>> result = this.clientCache.searchInactiveClients(days, descMode ? 1 : 0);
            if (result == null)
            {
              this.queryLib.sendTextMessage(Integer.parseInt((String)eventInfo.get("invokerid")), 1, 
                "Please wait until the client database cache is ready!");
            }
            else
            {
              int count = 0;
              
              StringBuffer clientList = new StringBuffer();
              long currentTime = System.currentTimeMillis() / 1000L;
              for (int i = 0; i < result.size(); i++)
              {
                String temp = "\n[b]" + this.clientCache.getNickname(((result.elementAt(i)).get("cldbid")).intValue()) + "[/b] (DB ID: " + (result.elementAt(i)).get("cldbid") + " - " + 
                  (int)((currentTime - ((result.elementAt(i)).get("lastonline")).intValue()) / 86400L) + " days)";
                if (this.modClass.getUTF8Length(clientList.toString() + temp) > 1024) {
                  break;
                }
                clientList.append(temp);
                count++;
              }
              this.queryLib.sendTextMessage(Integer.parseInt((String)eventInfo.get("invokerid")), 1, "Found " + Integer.toString(result.size()) + 
                " clients which are inactive for at least " + Integer.toString(days) + " days!" + (count > 0 ? " Displaying the " + Integer.toString(count) + (descMode ? " most inactive" : " least inactive") + " clients of them!" : ""));
              
              this.queryLib.sendTextMessage(Integer.parseInt((String)eventInfo.get("invokerid")), 1, clientList.toString());
            }
          }
          else
          {
            this.queryLib.sendTextMessage(Integer.parseInt((String)eventInfo.get("invokerid")), 1, 
              "Lowest possible days to list inactive clients are 10 days!");
          }
        }
      }
      else {
        this.queryLib.sendTextMessage(Integer.parseInt((String)eventInfo.get("invokerid")), 1, "You are not my master!");
      }
    }
    catch (Exception e)
    {
      this.modClass.addLogEntry(e, false);
    }
  }
  
  @SuppressWarnings("unchecked")
void handleListInactiveChannels(String msg, HashMap<String, String> eventInfo, boolean isFullAdmin, boolean isAdmin)
  {
    this.modClass.addLogEntry((byte)1, "Got command from " + (String)eventInfo.get("invokername") + ": " + msg, false);
    String arguments = getArguments(msg);
    try
    {
      if ((isFullAdmin) || (isAdmin))
      {
        if ((this.modClass.getChannelList() == null) || (this.modClass.getChannelList().size() == 0))
        {
          this.queryLib.sendTextMessage(Integer.parseInt((String)eventInfo.get("invokerid")), 1, "Missing channel list, command disabled!");
          return;
        }
        Vector<HashMap<String, String>> channelList = (Vector<HashMap<String, String>>) this.modClass.getChannelList().clone();
        ChannelEmptyComparator cec = new ChannelEmptyComparator();
        Collections.sort(channelList, cec);
        
        long curtime = System.currentTimeMillis();
        StringBuffer sbChannelList = new StringBuffer();
        if (arguments.length() > 0)
        {
          int channelID = -1;
          try
          {
            channelID = Integer.parseInt(arguments);
          }
          catch (NumberFormatException localNumberFormatException) {}
          int count = 0;
          for (HashMap<String, String> channel : channelList) {
            if (channelID >= 0)
            {
              if (Integer.parseInt((String)channel.get("cid")) == channelID)
              {
                String timeString = "Not empty!";
                if (Integer.parseInt((String)channel.get("seconds_empty")) >= 0) {
                  timeString = "Empty since " + this.modClass.getDifferenceTime(curtime - Integer.parseInt((String)channel.get("seconds_empty")) * 1000, curtime);
                }
                sbChannelList.append("[b]" + (String)channel.get("channel_name") + "[/b] (ID: ");
                sbChannelList.append((String)channel.get("cid"));
                sbChannelList.append(") - ");
                sbChannelList.append(timeString);
                
                count++;
                break;
              }
            }
            else
            {
              if (Integer.parseInt((String)channel.get("seconds_empty")) <= 0) {
                break;
              }
              if (((String)channel.get("channel_name")).toLowerCase().indexOf(arguments.toLowerCase()) != -1)
              {
                String temp = "\n[b]" + (String)channel.get("channel_name") + "[/b] (ID: " + (String)channel.get("cid") + ") - " + this.modClass.getDifferenceTime(curtime - Integer.parseInt((String)channel.get("seconds_empty")) * 1000, curtime);
                if (this.modClass.getUTF8Length(sbChannelList.toString() + temp) > 1024) {
                  break;
                }
                sbChannelList.append(temp);
                count++;
              }
            }
          }
          if (channelID >= 0)
          {
            if (count == 0) {
              sbChannelList.append("No channel found with channel ID " + Integer.toString(channelID) + "!");
            }
          }
          else
          {
            if (count == 0) {
              sbChannelList.append("No channels found with this name!");
            }
            this.queryLib.sendTextMessage(Integer.parseInt((String)eventInfo.get("invokerid")), 1, 
              "List of empty channels (sorted by empty since time). Displaying " + Integer.toString(count) + " of " + Integer.toString(channelList.size()) + 
              " channels with the search string \"" + arguments + "\":");
          }
          this.queryLib.sendTextMessage(Integer.parseInt((String)eventInfo.get("invokerid")), 1, sbChannelList.toString());
        }
        else
        {
          int count = 0;
          
          int i = 0;
          for (int len = channelList.size(); i < len; i++)
          {
            if (Integer.parseInt(channelList.get(i).get("seconds_empty")) <= 0) {
              break;
            }
            String temp = "\n[b]" + channelList.get(i).get("channel_name") + "[/b] (ID: " + channelList.get(i).get("cid") + ") - " + this.modClass.getDifferenceTime(curtime - Integer.parseInt(channelList.get(i).get("seconds_empty")) * 1000, curtime);
            if (this.modClass.getUTF8Length(sbChannelList.toString() + temp) > 1024) {
              break;
            }
            sbChannelList.append(temp);
            count++;
          }
          if (sbChannelList.length() > 0) {
            this.queryLib.sendTextMessage(Integer.parseInt((String)eventInfo.get("invokerid")), 1, 
              "List of empty channels (sorted by empty since time). Displaying " + Integer.toString(count) + " of " + Integer.toString(channelList.size()) + " channels!");
          } else {
            sbChannelList.append("No channel is empty!");
          }
          this.queryLib.sendTextMessage(Integer.parseInt((String)eventInfo.get("invokerid")), 1, sbChannelList.toString());
        }
      }
      else
      {
        this.queryLib.sendTextMessage(Integer.parseInt((String)eventInfo.get("invokerid")), 1, "You are not my master!");
      }
    }
    catch (Exception e)
    {
      this.modClass.addLogEntry(e, false);
    }
  }
  
  void handleSetChannelGroup(String msg, HashMap<String, String> eventInfo, boolean isFullAdmin, boolean isAdmin)
  {
    this.modClass.addLogEntry((byte)1, "Got command from " + (String)eventInfo.get("invokername") + ": " + msg, false);
    String arguments = getArguments(msg);
    try
    {
      if ((isFullAdmin) || (isAdmin))
      {
        if (arguments.length() >= 5)
        {
          int clientDBIDArg = -1;
          int channelGroupID = -1;
          Vector<Integer> channelList = new Vector<Integer>();
          try
          {
            int pos = 0;
            int pos2 = arguments.indexOf(" ", pos);
            try
            {
              clientDBIDArg = Integer.parseInt(arguments.substring(pos, pos2));
            }
            catch (NumberFormatException e)
            {
              clientDBIDArg = this.modClass.getClientDBID(arguments.substring(pos, pos2));
              if (clientDBIDArg < 0) {
                throw new NumberFormatException("Got invalid client database id or unique id!");
              }
            }
            pos = pos2 + 1;
            pos2 = arguments.indexOf(" ", pos);
            channelGroupID = Integer.parseInt(arguments.substring(pos, pos2));
            
            StringTokenizer st = new StringTokenizer(arguments.substring(pos2 + 1), ",", false);
            while (st.hasMoreTokens()) {
              channelList.addElement(Integer.valueOf(Integer.parseInt(st.nextToken().trim())));
            }
          }
          catch (NumberFormatException nfe)
          {
            this.queryLib.sendTextMessage(Integer.parseInt((String)eventInfo.get("invokerid")), 1, "Error while parsing numbers, command aborted...");
            this.modClass.addLogEntry(nfe, false);
          }
          if (channelList.size() == 0)
          {
            this.queryLib.sendTextMessage(Integer.parseInt((String)eventInfo.get("invokerid")), 1, 
              "Wrong usage, no channels given! Right: !setchannelgroup <client database id or unique id> <channel group id> <channel list separated with comma>");
            return;
          }
          int count = 0;
          int errorcount = 0;
          for (Iterator<Integer> localIterator = channelList.iterator(); localIterator.hasNext();)
          {
            int channelID = ((Integer)localIterator.next()).intValue();
            
            HashMap<String, String> actionResponse = this.queryLib.doCommand("setclientchannelgroup cgid=" + Integer.toString(channelGroupID) + " cid=" + channelID + " cldbid=" + 
              Integer.toString(clientDBIDArg));
            if (((String)actionResponse.get("id")).equals("0")) {
              count++;
            } else {
              errorcount++;
            }
          }
          this.queryLib.sendTextMessage(Integer.parseInt((String)eventInfo.get("invokerid")), 1, 
            "Channel group for client database id " + Integer.toString(clientDBIDArg) + " successfully set to channel group id " + Integer.toString(channelGroupID) + " for " + Integer.toString(count) + " channels!" + (
            errorcount > 0 ? " " + Integer.toString(errorcount) + " channels could not set to default channel group!" : ""));
        }
        else
        {
          this.queryLib.sendTextMessage(Integer.parseInt((String)eventInfo.get("invokerid")), 1, 
            "Wrong usage! Right: !setchannelgroup <client database id or unique id> <channel group id> <channel list separated with comma>");
        }
      }
      else {
        this.queryLib.sendTextMessage(Integer.parseInt((String)eventInfo.get("invokerid")), 1, "You are not my master!");
      }
    }
    catch (Exception e)
    {
      this.modClass.addLogEntry(e, false);
    }
  }
  
  void handleRemoveServerGroups(String msg, HashMap<String, String> eventInfo, boolean isFullAdmin, boolean isAdmin)
  {
    this.modClass.addLogEntry((byte)1, "Got command from " + (String)eventInfo.get("invokername") + ": " + msg, false);
    String arguments = getArguments(msg);
    try
    {
      if ((isFullAdmin) || (isAdmin))
      {
        if (arguments.length() >= 1)
        {
          Vector<Integer> serverGroupList = new Vector<Integer>();
          int clientDBIDArg = -1;
          boolean doAction = false;
          Vector<HashMap<String, String>> serverGroupClientList;
          try
          {
            try
            {
              clientDBIDArg = Integer.parseInt(arguments);
            }
            catch (NumberFormatException e)
            {
              clientDBIDArg = this.modClass.getClientDBID(arguments);
              if (clientDBIDArg < 0) {
                throw new NumberFormatException("Got invalid client database id or unique id!");
              }
            }
            HashMap<String, String> sgcListResponse = this.queryLib.doCommand("servergroupsbyclientid cldbid=" + Integer.toString(clientDBIDArg));
            if (((String)sgcListResponse.get("id")).equals("0"))
            {
              ServerInfoCache_Interface serverInfoCache = this.modClass.getServerInfoCache();
              int defsgid = serverInfoCache.getServerDefaultServerGroup();
              serverGroupClientList = this.queryLib.parseRawData((String)sgcListResponse.get("response"));
              for (HashMap<String, String> hashMap : serverGroupClientList) {
                if (defsgid != Integer.parseInt((String)hashMap.get("sgid"))) {
                  serverGroupList.addElement(Integer.valueOf(Integer.parseInt((String)hashMap.get("sgid"))));
                }
              }
              doAction = true;
            }
          }
          catch (NumberFormatException nfe)
          {
            this.queryLib.sendTextMessage(Integer.parseInt((String)eventInfo.get("invokerid")), 1, "Error while parsing numbers, command aborted...");
            this.modClass.addLogEntry(nfe, false);
          }
          if (doAction)
          {
            int count = 0;
            int errorcount = 0;
            for (Iterator<Integer> serverGroupClientList_ = serverGroupList.iterator(); serverGroupClientList_.hasNext();)
            {
              int serverGroupID = ((Integer)serverGroupClientList_.next()).intValue();
              
              HashMap<String, String> actionResponse = this.queryLib.doCommand("servergroupdelclient sgid=" + serverGroupID + " cldbid=" + Integer.toString(clientDBIDArg));
              if (((String)actionResponse.get("id")).equals("0")) {
                count++;
              } else {
                errorcount++;
              }
            }
            this.queryLib.sendTextMessage(Integer.parseInt((String)eventInfo.get("invokerid")), 1, 
              "Removed " + Integer.toString(count) + " server groups of client database id " + Integer.toString(clientDBIDArg) + " successfully!" + (
              errorcount > 0 ? " " + Integer.toString(errorcount) + " server groups could not removed!" : ""));
          }
        }
        else
        {
          this.queryLib.sendTextMessage(Integer.parseInt((String)eventInfo.get("invokerid")), 1, "Wrong usage! Right: !removeservergroups <client database id or unique id>");
        }
      }
      else {
        this.queryLib.sendTextMessage(Integer.parseInt((String)eventInfo.get("invokerid")), 1, "You are not my master!");
      }
    }
    catch (Exception e)
    {
      this.modClass.addLogEntry(e, false);
    }
  }
  
  void handleRemoveChannelGroups(String msg, HashMap<String, String> eventInfo, boolean isFullAdmin, boolean isAdmin)
  {
    this.modClass.addLogEntry((byte)1, "Got command from " + (String)eventInfo.get("invokername") + ": " + msg, false);
    String arguments = getArguments(msg);
    try
    {
      if ((isFullAdmin) || (isAdmin))
      {
        if (arguments.length() >= 1)
        {
          ServerInfoCache_Interface serverInfoCache = this.modClass.getServerInfoCache();
          if (serverInfoCache == null)
          {
            this.queryLib.sendTextMessage(Integer.parseInt((String)eventInfo.get("invokerid")), 1, "Error while getting server info, command aborted...");
          }
          else
          {
            Vector<Integer> channelList = new Vector<Integer>();
            int defChannelGroupID = -1;
            int clientDBIDArg = -1;
            boolean doAction = false;
            try
            {
              defChannelGroupID = serverInfoCache.getServerDefaultChannelGroup();
              if (defChannelGroupID >= 0)
              {
                try
                {
                  clientDBIDArg = Integer.parseInt(arguments);
                }
                catch (NumberFormatException e)
                {
                  clientDBIDArg = this.modClass.getClientDBID(arguments);
                  if (clientDBIDArg < 0) {
                    throw new NumberFormatException("Got invalid client database id or unique id!");
                  }
                }
                HashMap<String, String> cgcListResponse = this.queryLib.doCommand("channelgroupclientlist cldbid=" + Integer.toString(clientDBIDArg));
                if (((String)cgcListResponse.get("id")).equals("0"))
                {
                  Vector<HashMap<String, String>> channelGroupClientList = this.queryLib.parseRawData((String)cgcListResponse.get("response"));
                  for (HashMap<String, String> hashMap : channelGroupClientList) {
                    if (Integer.parseInt((String)hashMap.get("cgid")) != defChannelGroupID) {
                      channelList.addElement(Integer.valueOf(Integer.parseInt((String)hashMap.get("cid"))));
                    }
                  }
                  doAction = true;
                }
                else
                {
                  this.queryLib.sendTextMessage(Integer.parseInt((String)eventInfo.get("invokerid")), 1, "Error while getting channel group list for the client database id " + Integer.toString(clientDBIDArg) + ", reason from TS3 server: " + (String)cgcListResponse.get("msg"));
                }
              }
              else
              {
                throw new NumberFormatException("Got invalid default channel group id!");
              }
            }
            catch (NumberFormatException nfe)
            {
              this.queryLib.sendTextMessage(Integer.parseInt((String)eventInfo.get("invokerid")), 1, "Error while parsing numbers, command aborted...");
              this.modClass.addLogEntry(nfe, false);
            }
            if (doAction)
            {
              int count = 0;
              int errorcount = 0;
              for (Iterator<Integer> tmp = channelList.iterator(); tmp.hasNext();)
              {
                int channelID = ((Integer)tmp.next()).intValue();
                
                HashMap<String, String> actionResponse = this.queryLib.doCommand("setclientchannelgroup cgid=" + Integer.toString(defChannelGroupID) + " cid=" + channelID + " cldbid=" + 
                  Integer.toString(clientDBIDArg));
                if (((String)actionResponse.get("id")).equals("0")) {
                  count++;
                } else {
                  errorcount++;
                }
              }
              this.queryLib.sendTextMessage(Integer.parseInt((String)eventInfo.get("invokerid")), 1, 
                "Channel group for client database id " + Integer.toString(clientDBIDArg) + " successfully set to default channel group for " + Integer.toString(count) + 
                " channels!" + (errorcount > 0 ? " " + Integer.toString(errorcount) + " channels could not set to default channel group!" : ""));
            }
          }
        }
        else
        {
          this.queryLib.sendTextMessage(Integer.parseInt((String)eventInfo.get("invokerid")), 1, "Wrong usage! Right: !removechannelgroups <client database id or unique id>");
        }
      }
      else {
        this.queryLib.sendTextMessage(Integer.parseInt((String)eventInfo.get("invokerid")), 1, "You are not my master!");
      }
    }
    catch (Exception e)
    {
      this.modClass.addLogEntry(e, false);
    }
  }
  
  void handleMsgChannelGroup(String msg, HashMap<String, String> eventInfo, boolean isFullAdmin, boolean isAdmin)
  {
    this.modClass.addLogEntry((byte)1, "Got command from " + (String)eventInfo.get("invokername") + ": " + msg, false);
    String arguments = getArguments(msg);
    try
    {
      if ((isFullAdmin) || (isAdmin))
      {
        if (arguments.length() >= 3)
        {
          int pos = arguments.indexOf(" ");
          if (pos == -1)
          {
            this.queryLib.sendTextMessage(Integer.parseInt((String)eventInfo.get("invokerid")), 1, 
              "Wrong usage! Right: !msgchannelgroup <channelgroup id> <message>");
          }
          else
          {
            Vector<Integer> clientListSend = new Vector<Integer>();
            String sendMessage = "Message from " + (String)eventInfo.get("invokername") + ": " + arguments.substring(pos + 1);
            boolean doAction = false;
            HashMap<String, String> hashMap;
            try
            {
              Vector<Integer> groupList = new Vector<Integer>();
              String temp = arguments.substring(0, pos);
              if ((temp != null) && (temp.length() > 0))
              {
                StringTokenizer st = new StringTokenizer(temp, ",", false);
                while (st.hasMoreTokens()) {
                  groupList.addElement(Integer.valueOf(Integer.parseInt(st.nextToken().trim())));
                }
              }
              Vector<HashMap<String, String>> clientListGroups = this.queryLib.getList(1, "-groups");
              for (Iterator<HashMap<String, String>> localIterator = clientListGroups.iterator(); localIterator.hasNext();)
              {
                hashMap = localIterator.next();
                if (groupList.indexOf(Integer.valueOf(Integer.parseInt((String)hashMap.get("client_channel_group_id")))) >= 0) {
                  if (Integer.parseInt((String)hashMap.get("client_type")) == 0) {
                    if (clientListSend.indexOf(Integer.valueOf(Integer.parseInt((String)hashMap.get("clid")))) == -1) {
                      clientListSend.addElement(Integer.valueOf(Integer.parseInt((String)hashMap.get("clid"))));
                    }
                  }
                }
              }
              doAction = true;
            }
            catch (NumberFormatException nfe)
            {
              this.queryLib.sendTextMessage(Integer.parseInt((String)eventInfo.get("invokerid")), 1, "Error while parsing numbers, command aborted...");
            }
            if (doAction)
            {
              int count = 0;
              int errorcount = 0;
              for (Iterator<Integer> hashMap_ = clientListSend.iterator(); hashMap_.hasNext();)
              {
                int clientid = ((Integer)hashMap_.next()).intValue();
                try
                {
                  this.queryLib.sendTextMessage(clientid, 1, sendMessage);
                  count++;
                }
                catch (Exception e)
                {
                  errorcount++;
                }
              }
              this.queryLib.sendTextMessage(Integer.parseInt((String)eventInfo.get("invokerid")), 1, "Message sent to " + Integer.toString(count) + 
                " clients!" + (errorcount > 0 ? " Error while sending message to " + Integer.toString(errorcount) + " clients!" : ""));
            }
          }
        }
        else
        {
          this.queryLib.sendTextMessage(Integer.parseInt((String)eventInfo.get("invokerid")), 1, 
            "Wrong usage! Right: !msgchannelgroup <channelgroup id> <message>");
        }
      }
      else {
        this.queryLib.sendTextMessage(Integer.parseInt((String)eventInfo.get("invokerid")), 1, "You are not my master!");
      }
    }
    catch (Exception e)
    {
      this.modClass.addLogEntry(e, false);
    }
  }
  
  void handleMsgServerGroup(String msg, HashMap<String, String> eventInfo, boolean isFullAdmin, boolean isAdmin)
  {
    this.modClass.addLogEntry((byte)1, "Got command from " + (String)eventInfo.get("invokername") + ": " + msg, false);
    String arguments = getArguments(msg);
    try
    {
      if ((isFullAdmin) || (isAdmin))
      {
        if (arguments.length() >= 3)
        {
          int pos = arguments.indexOf(" ");
          if (pos == -1)
          {
            this.queryLib.sendTextMessage(Integer.parseInt((String)eventInfo.get("invokerid")), 1, 
              "Wrong usage! Right: !msgservergroup <servergroup id> <message>");
          }
          else
          {
            Vector<Integer> clientListSend = new Vector<Integer>();
            String sendMessage = "Message from " + (String)eventInfo.get("invokername") + ": " + arguments.substring(pos + 1);
            boolean doAction = false;
            HashMap<String, String> hashMap;
            try
            {
              Vector<Integer> groupList = new Vector<Integer>();
              String temp = arguments.substring(0, pos);
              if ((temp != null) && (temp.length() > 0))
              {
                StringTokenizer st = new StringTokenizer(temp, ",", false);
                while (st.hasMoreTokens()) {
                  groupList.addElement(Integer.valueOf(Integer.parseInt(st.nextToken().trim())));
                }
              }
              Vector<HashMap<String, String>> clientListGroups = this.queryLib.getList(1, "-groups");
              for (Iterator<HashMap<String, String>> localIterator = clientListGroups.iterator(); localIterator.hasNext();)
              {
                hashMap = localIterator.next();
                if (this.modClass.isGroupListed((String)hashMap.get("client_servergroups"), groupList)) {
                  if (Integer.parseInt((String)hashMap.get("client_type")) == 0) {
                    if (clientListSend.indexOf(Integer.valueOf(Integer.parseInt((String)hashMap.get("clid")))) == -1) {
                      clientListSend.addElement(Integer.valueOf(Integer.parseInt((String)hashMap.get("clid"))));
                    }
                  }
                }
              }
              doAction = true;
            }
            catch (NumberFormatException nfe)
            {
              this.queryLib.sendTextMessage(Integer.parseInt((String)eventInfo.get("invokerid")), 1, "Error while parsing numbers, command aborted...");
            }
            if (doAction)
            {
              int count = 0;
              int errorcount = 0;
              for (Iterator<Integer> hashMap_ = clientListSend.iterator(); hashMap_.hasNext();)
              {
                int clientid = ((Integer)hashMap_.next()).intValue();
                try
                {
                  this.queryLib.sendTextMessage(clientid, 1, sendMessage);
                  count++;
                }
                catch (Exception e)
                {
                  errorcount++;
                }
              }
              this.queryLib.sendTextMessage(Integer.parseInt((String)eventInfo.get("invokerid")), 1, "Message sent to " + Integer.toString(count) + 
                " clients!" + (errorcount > 0 ? " Error while sending message to " + Integer.toString(errorcount) + " clients!" : ""));
            }
          }
        }
        else
        {
          this.queryLib.sendTextMessage(Integer.parseInt((String)eventInfo.get("invokerid")), 1, "Wrong usage! Right: !msgservergroup <servergroup id> <message>");
        }
      }
      else {
        this.queryLib.sendTextMessage(Integer.parseInt((String)eventInfo.get("invokerid")), 1, "You are not my master!");
      }
    }
    catch (Exception e)
    {
      this.modClass.addLogEntry(e, false);
    }
  }
  
  void handleSetChannelName(String msg, HashMap<String, String> eventInfo, boolean isFullAdmin, boolean isAdmin)
  {
    this.modClass.addLogEntry((byte)1, "Got command from " + (String)eventInfo.get("invokername") + ": " + msg, false);
    String arguments = getArguments(msg);
    try
    {
      if ((isFullAdmin) || (isAdmin))
      {
        if (arguments.length() < 3)
        {
          this.queryLib.sendTextMessage(Integer.parseInt((String)eventInfo.get("invokerid")), 1, 
            "Wrong usage! Right: !setchannelname <channel id> <new channel name>");
          return;
        }
        int pos = arguments.indexOf(" ");
        if (pos == -1)
        {
          this.queryLib.sendTextMessage(Integer.parseInt((String)eventInfo.get("invokerid")), 1, 
            "Wrong usage! Right: !setchannelname <channel id> <new channel name>");
          return;
        }
        int channelID;
        try
        {
          channelID = Integer.parseInt(arguments.substring(0, pos));
        }
        catch (Exception e)
        {
          this.queryLib.sendTextMessage(Integer.parseInt((String)eventInfo.get("invokerid")), 1, "Wrong usage! The channel id has to be a number!"); return;
        }
        String newChannelName = arguments.substring(pos + 1).trim();
        
        HashMap<String, String> actionResponse = this.queryLib.doCommand("channeledit cid=" + Integer.toString(channelID) + " channel_name=" + this.queryLib.encodeTS3String(newChannelName));
        if (((String)actionResponse.get("id")).equals("0")) {
          this.queryLib.sendTextMessage(Integer.parseInt((String)eventInfo.get("invokerid")), 1, "Channel id " + Integer.toString(channelID) + 
            " was successfully renamed to: " + newChannelName);
        } else {
          this.queryLib.sendTextMessage(Integer.parseInt((String)eventInfo.get("invokerid")), 1, "Error while rename channel id " + Integer.toString(channelID) + 
            "!");
        }
      }
      else
      {
        this.queryLib.sendTextMessage(Integer.parseInt((String)eventInfo.get("invokerid")), 1, "You are not my master!");
      }
    }
    catch (Exception e)
    {
      this.modClass.addLogEntry(e, false);
    }
  }
  
  void handleGetChannelID(String msg, HashMap<String, String> eventInfo, boolean isFullAdmin, boolean isAdmin)
  {
    this.modClass.addLogEntry((byte)1, "Got command from " + (String)eventInfo.get("invokername") + ": " + msg, false);
    String arguments = getArguments(msg);
    try
    {
      if ((isFullAdmin) || (isAdmin))
      {
        Vector<HashMap<String, String>> channelList = this.modClass.getChannelList();
        if (channelList == null)
        {
          this.queryLib.sendTextMessage(Integer.parseInt((String)eventInfo.get("invokerid")), 1, "Error while getting channel list. Command aborted!");
          return;
        }
        StringBuffer sbChannelList = new StringBuffer();
        
        int count = 0;
        if (arguments.length() == 0)
        {
          for (HashMap<String, String> channel : channelList)
          {
            String temp = "\nID: " + (String)channel.get("cid") + " - [b]" + (String)channel.get("channel_name") + "[/b]";
            if (this.modClass.getUTF8Length(sbChannelList.toString() + temp) > 1024) {
              break;
            }
            sbChannelList.append(temp);
            count++;
          }
          this.queryLib.sendTextMessage(Integer.parseInt((String)eventInfo.get("invokerid")), 1, 
            "Displaying " + Integer.toString(count) + " of " + Integer.toString(channelList.size()) + " channels:");
        }
        else
        {
          int channelID = -1;
          try
          {
            channelID = Integer.parseInt(arguments);
          }
          catch (NumberFormatException localNumberFormatException1) {}
          int countTotal = 0;
          for (HashMap<String, String> channel : channelList) {
            if (channelID >= 0)
            {
              if (Integer.parseInt((String)channel.get("cid")) == channelID)
              {
                sbChannelList.append("Channel ID: " + (String)channel.get("cid") + " - [b]" + (String)channel.get("channel_name") + "[/b]");
                count++;
                break;
              }
            }
            else if (((String)channel.get("channel_name")).toLowerCase().indexOf(arguments.toLowerCase()) != -1)
            {
              String temp = "\nID: " + (String)channel.get("cid") + " - [b]" + (String)channel.get("channel_name") + "[/b]";
              if (this.modClass.getUTF8Length(sbChannelList.toString() + temp) <= 1024)
              {
                sbChannelList.append(temp);
                count++;
              }
              countTotal++;
            }
          }
          if (channelID >= 0)
          {
            if (count == 0) {
              this.queryLib.sendTextMessage(Integer.parseInt((String)eventInfo.get("invokerid")), 1, 
                "No channel found with the channel ID " + Integer.toString(channelID) + "!");
            }
          }
          else if (count == 0) {
            this.queryLib.sendTextMessage(Integer.parseInt((String)eventInfo.get("invokerid")), 1, 
              "No channels found with the search string \"" + arguments + "\"!");
          } else {
            this.queryLib.sendTextMessage(Integer.parseInt((String)eventInfo.get("invokerid")), 1, 
              "Found the search string \"" + arguments + "\" in " + Integer.toString(countTotal) + " of " + Integer.toString(channelList.size()) + " channel names! Displaying " + Integer.toString(count) + " channels:");
          }
        }
        if (sbChannelList.length() > 0) {
          this.queryLib.sendTextMessage(Integer.parseInt((String)eventInfo.get("invokerid")), 1, sbChannelList.toString());
        }
      }
      else
      {
        this.queryLib.sendTextMessage(Integer.parseInt((String)eventInfo.get("invokerid")), 1, "You are not my master!");
      }
    }
    catch (Exception e)
    {
      this.modClass.addLogEntry(e, false);
    }
  }
  
  void handleExec(String msg, HashMap<String, String> eventInfo, boolean isFullAdmin)
  {
    this.modClass.addLogEntry((byte)1, "Got command from " + (String)eventInfo.get("invokername") + ": " + msg, false);
    try
    {
      if (msg.toLowerCase().startsWith("!execwait"))
      {
        handleExecWait(msg, eventInfo, isFullAdmin);
      }
      else
      {
        String arguments = getArguments(msg);
        if (isFullAdmin)
        {
          if (this.manager.isCommandExecAllowed())
          {
            if (arguments.length() > 0) {
              try
              {
                Runtime.getRuntime().exec(arguments);
                this.queryLib.sendTextMessage(Integer.parseInt((String)eventInfo.get("invokerid")), 1, "Command executed!");
              }
              catch (Exception e)
              {
                this.queryLib.sendTextMessage(Integer.parseInt((String)eventInfo.get("invokerid")), 1, "Error while executing command: " + e.toString());
              }
            } else {
              this.queryLib.sendTextMessage(Integer.parseInt((String)eventInfo.get("invokerid")), 1, "Wrong usage! Right: !exec <system command>");
            }
          }
          else {
            this.queryLib.sendTextMessage(Integer.parseInt((String)eventInfo.get("invokerid")), 1, "Command disabled!");
          }
        }
        else {
          this.queryLib.sendTextMessage(Integer.parseInt((String)eventInfo.get("invokerid")), 1, 
            "You are not my master! You have to be full bot admin to use this command.");
        }
      }
    }
    catch (Exception e)
    {
      this.modClass.addLogEntry(e, false);
    }
  }
  
  void handleExecWait(String msg, HashMap<String, String> eventInfo, boolean isFullAdmin)
  {
    String arguments = getArguments(msg);
    try
    {
      if (isFullAdmin)
      {
        if (this.manager.isCommandExecAllowed())
        {
          if (arguments.length() > 0) {
            try
            {
              Process proc = Runtime.getRuntime().exec(arguments);
              BufferedInputStream bisOut = new BufferedInputStream(proc.getInputStream());
              BufferedInputStream bisErr = new BufferedInputStream(proc.getErrorStream());
              int returnValue = proc.waitFor();
              
              StringBuffer output = new StringBuffer();
              while (bisOut.available() > 0) {
                output.append((char)bisOut.read());
              }
              while (bisErr.available() > 0) {
                output.append((char)bisErr.read());
              }
              if (output.length() > 1004)
              {
                output.setLength(1000);
                output.append("\n[...]");
              }
              this.queryLib.sendTextMessage(Integer.parseInt((String)eventInfo.get("invokerid")), 1, 
                "Command executed with a return value of " + Integer.toString(returnValue) + "!\n");
              this.queryLib.sendTextMessage(Integer.parseInt((String)eventInfo.get("invokerid")), 1, "Output:\n" + output.toString());
            }
            catch (Exception e)
            {
              this.queryLib.sendTextMessage(Integer.parseInt((String)eventInfo.get("invokerid")), 1, "Error while executing command: " + e.toString());
            }
          } else {
            this.queryLib.sendTextMessage(Integer.parseInt((String)eventInfo.get("invokerid")), 1, "Wrong usage! Right: !execwait <system command>");
          }
        }
        else {
          this.queryLib.sendTextMessage(Integer.parseInt((String)eventInfo.get("invokerid")), 1, "Command disabled!");
        }
      }
      else {
        this.queryLib.sendTextMessage(Integer.parseInt((String)eventInfo.get("invokerid")), 1, 
          "You are not my master! You have to be full bot admin to use this command.");
      }
    }
    catch (Exception e)
    {
      this.modClass.addLogEntry(e, false);
    }
  }
  
  void handleBotJoinChannel(String msg, HashMap<String, String> eventInfo, boolean isFullAdmin, boolean isAdmin)
  {
    this.modClass.addLogEntry((byte)1, "Got command from " + (String)eventInfo.get("invokername") + ": " + msg, false);
    String arguments = getArguments(msg);
    try
    {
      if ((isFullAdmin) || (isAdmin))
      {
        if (arguments.length() == 0)
        {
          this.queryLib.sendTextMessage(Integer.parseInt((String)eventInfo.get("invokerid")), 1, "Wrong usage! Right: !botjoinchannel <channel id>");
          return;
        }
        try
        {
          int newChannelID = Integer.parseInt(arguments);
          if (newChannelID == this.queryLib.getCurrentQueryClientChannelID())
          {
            this.queryLib.sendTextMessage(Integer.parseInt((String)eventInfo.get("invokerid")), 1, "Bot is already in the given channel!");
          }
          else
          {
            try
            {
              this.queryLib.moveClient(this.queryLib.getCurrentQueryClientID(), newChannelID, null);
              this.queryLib.sendTextMessage(Integer.parseInt((String)eventInfo.get("invokerid")), 1, "Bot was moved into the given channel!");
            }
            catch (Exception e)
            {
              this.queryLib.sendTextMessage(Integer.parseInt((String)eventInfo.get("invokerid")), 1, "Error while moving bot into the given channel!");
              this.modClass.addLogEntry(e, false);
            }
            this.queryLib.sendTextMessage(Integer.parseInt((String)eventInfo.get("invokerid")), 1, "You are not my master!");
          }
        }
        catch (Exception e)
        {
          this.queryLib.sendTextMessage(Integer.parseInt((String)eventInfo.get("invokerid")), 1, "Error while reading given channel id!");
        }
      }
    }
    catch (Exception e)
    {
      this.modClass.addLogEntry(e, false);
    }
  }
  
  void handleBotRename(String msg, HashMap<String, String> eventInfo, boolean isFullAdmin, boolean isAdmin)
  {
    this.modClass.addLogEntry((byte)1, "Got command from " + (String)eventInfo.get("invokername") + ": " + msg, false);
    String arguments = getArguments(msg);
    try
    {
      if ((isFullAdmin) || (isAdmin))
      {
        if (arguments.length() == 0)
        {
          if (this.config.getValue("bot_server_query_name") == null)
          {
            this.queryLib.sendTextMessage(Integer.parseInt((String)eventInfo.get("invokerid")), 1, "No default name set in bot config! Add a new name to command: !botrename <new name>");
            return;
          }
          if ((this.queryLib.getCurrentQueryClientName() != null) && (this.config.getValue("bot_server_query_name").equals(this.queryLib.getCurrentQueryClientName())))
          {
            this.queryLib.sendTextMessage(Integer.parseInt((String)eventInfo.get("invokerid")), 1, "Bot already has the default name! Add a new name to command: !botrename <new name>");
            return;
          }
          arguments = this.config.getValue("bot_server_query_name");
        }
        if (arguments.length() < 3)
        {
          this.queryLib.sendTextMessage(Integer.parseInt((String)eventInfo.get("invokerid")), 1, "Error while renaming bot, new name needs at least 3 characters!");
          return;
        }
        if ((this.queryLib.getCurrentQueryClientName() != null) && (arguments.equals(this.queryLib.getCurrentQueryClientName())))
        {
          this.queryLib.sendTextMessage(Integer.parseInt((String)eventInfo.get("invokerid")), 1, "Bot already has the name: " + arguments);
          return;
        }
        try
        {
          this.queryLib.setDisplayName(arguments);
          if ((this.config.getValue("bot_server_query_name") != null) && (this.config.getValue("bot_server_query_name").equals(arguments))) {
            this.queryLib.sendTextMessage(Integer.parseInt((String)eventInfo.get("invokerid")), 1, "Bot name changed back to default name: " + arguments);
          } else {
            this.queryLib.sendTextMessage(Integer.parseInt((String)eventInfo.get("invokerid")), 1, "Bot name temporary changed to: " + arguments);
          }
        }
        catch (TS3ServerQueryException sqe)
        {
          this.queryLib.sendTextMessage(Integer.parseInt((String)eventInfo.get("invokerid")), 1, "Error while renaming bot to: " + arguments + "\n" + sqe.getMessage());
        }
        catch (Exception e)
        {
          this.queryLib.sendTextMessage(Integer.parseInt((String)eventInfo.get("invokerid")), 1, "Error while renaming bot to: " + arguments + "\nError: " + e.toString());
        }
      }
      else
      {
        this.queryLib.sendTextMessage(Integer.parseInt((String)eventInfo.get("invokerid")), 1, "You are not my master!");
      }
    }
    catch (Exception e)
    {
      this.modClass.addLogEntry(e, false);
    }
  }
  
  void handleBotHelp(String msg, HashMap<String, String> eventInfo, boolean isFullAdmin, boolean isAdmin)
  {
    this.modClass.addLogEntry((byte)1, "Got command from " + (String)eventInfo.get("invokername") + ": " + msg, false);
    String arguments = getArguments(msg);
    try
    {
      if (arguments.length() == 0)
      {
        StringBuffer helpText = new StringBuffer("List of commands:\n!botinfo");
        StringBuffer helpTextAdmin = new StringBuffer();
        StringBuffer helpTextFullAdmin = new StringBuffer();
        
        helpText.append(this.modClass.getCommandList(eventInfo, isFullAdmin, isAdmin));
        if ((isFullAdmin) || (isAdmin)) {
          helpTextAdmin.append("You can also use the following admin commands:\n!botcfghelp [config key]\n!botcfgget <config key>\n!botcfgset <config key> = <config value>\n!botcfgcheck\n!botcfgreload\n!botcfgsave\n!botfunctionlist\n!botfunctionactivate <prefix>\n!botfunctiondisable <prefix>\n!botjoinchannel <channel id>\n!botreload\n!botrename <new name>\n!botversioncheck\n!clientsearch <nickname>\n!getChannelID [channel id or channel name]\n!listinactivechannels [channel id or channel name]\n!listinactiveclients [minimum days inactive]\n!msgchannelgroup <channelgroup id> <message>\n!msgservergroup <servergroup id> <message>\n!removeservergroups <client database id>\n!removechannelgroups <client database id>\n!searchip <ip address>\n!setchannelgroup <client database id> <channel group id> <channel list separated with comma>\n!setchannelname <channel id> <new channel name>");
        }
        if (isFullAdmin) {
          helpTextFullAdmin.append("You can also use the following full admin commands:\n!botinstancestart <name>\n!botinstancestop <name>\n!botinstancelist\n!botinstancelistreload\n!botreloadall\n!botquit");
        }
        if ((isFullAdmin) && (this.manager.isCommandExecAllowed())) {
          helpTextFullAdmin.append("\n!exec <system command>\n!execwait <system command>");
        }
        String lastMessage = "\n\nTo get a help about the commands, just do !bothelp <command>";
        if (isFullAdmin)
        {
          this.queryLib.sendTextMessage(Integer.parseInt((String)eventInfo.get("invokerid")), 1, helpText.toString());
          this.queryLib.sendTextMessage(Integer.parseInt((String)eventInfo.get("invokerid")), 1, helpTextAdmin.toString());
          this.queryLib.sendTextMessage(Integer.parseInt((String)eventInfo.get("invokerid")), 1, helpTextFullAdmin.toString() + lastMessage);
        }
        else if (isAdmin)
        {
          this.queryLib.sendTextMessage(Integer.parseInt((String)eventInfo.get("invokerid")), 1, helpText.toString());
          this.queryLib.sendTextMessage(Integer.parseInt((String)eventInfo.get("invokerid")), 1, helpTextAdmin.toString() + lastMessage);
        }
        else
        {
          this.queryLib.sendTextMessage(Integer.parseInt((String)eventInfo.get("invokerid")), 1, helpText.toString() + lastMessage);
        }
      }
      else
      {
        String args = arguments.toLowerCase();
        if (args.length() < 3)
        {
          this.queryLib.sendTextMessage(Integer.parseInt((String)eventInfo.get("invokerid")), 1, "Wrong usage! Right: !bothelp <command>");
        }
        else
        {
          if (args.charAt(0) == '!') {
            args = args.substring(1);
          }
          String helpText = null;
          if ((args.equals("botinfo")) || (args.equals("info"))) {
            helpText = "Shows you the running bot version and uptime. It also displays the current bot admin permission level.";
          } else if ((args.equals("botquit")) || (args.equals("quit")) || (args.equals("exit"))) {
            helpText = "Disconnect and quits all bot instances.";
          } else if ((args.equals("botreload")) || (args.equals("reconnect"))) {
            helpText = "Disconnects the current bot instance, reload bot configuration and start bot instance again. Bot configuration will be checked first!";
          } else if (args.equals("botreloadall")) {
            helpText = "Disconnects all bot instances, reload bot configuration and start all bot instances again. This command do not check bot configuration first!";
          } else if ((args.equals("botversioncheck")) || (args.equals("botversion")) || (args.equals("version"))) {
            helpText = "Displays current installed, latest final and latest development (if exists) versions of this bot.";
          } else if (args.equals("botcfghelp")) {
            helpText = "Returns informations about a config key. If no key argument given, a list of config keys will be returned.\nExample: !botcfghelp bot_channel_id";
          } else if (args.equals("botcfgget")) {
            helpText = "Returns the value of a current config key.\nExample: !botcfgget bot_channel_id";
          } else if (args.equals("botcfgset")) {
            helpText = "Set a new value for a config key. Notice: This changes will not used by the bot without saving and reloading!\nExample: !botcfgset bot_channel_id = -1";
          } else if (args.equals("botcfgcheck")) {
            helpText = "Check if current config (for example after !botcfgset) is valid.";
          } else if (args.equals("botcfgreload")) {
            helpText = "Reloads the bot configuration. You can use a function name as argument to reload only that configuration of that function.";
          } else if (args.equals("botcfgsave")) {
            helpText = "Saves current bot configuration.";
          } else if ((args.equals("botfunctionlist")) || (args.equals("functionlist"))) {
            helpText = "Get a list of currently loaded functions.";
          } else if ((args.equals("botfunctionactivate")) || (args.equals("functionon"))) {
            helpText = "Activate the given function.\nUsage: !botfunctionactivate <function prefix>";
          } else if ((args.equals("botfunctiondisable")) || (args.equals("functionoff"))) {
            helpText = "Disable the given function.\nUsage: !botfunctiondisable <function prefix>";
          } else if ((args.equals("clientsearch")) || (args.equals("clients")) || (args.equals("clientlist"))) {
            helpText = "Shows some database informations of a client. Search using the client name (* as a wildcard possible). You can also search using the complete unique id. The client database list cache needs to be enabled in the main bot configuration!\nExample: !clientsearch *foo*bar*";
          } else if (args.equals("searchip")) {
            helpText = "Shows some database informations of a client found using ip address. Use * as a wildcard. The client database list cache needs to be enabled in the main bot configuration!\nExample: !searchip 127.0.*";
          } else if ((args.equals("listinactiveclients")) || (args.equals("inactiveclients"))) {
            helpText = "List all clients which are inactive since X days. Without argument the most inactive clients will be displayed. The client database list cache needs to be enabled in the main bot configuration!\nUsage: !listinactiveclients [minimum days inactive]";
          } else if ((args.equals("listinactivechannels")) || (args.equals("emptychannels"))) {
            helpText = "List of empty channels sorted by empty since time. Optionally you can add a channel name or channel id to this command to filter the list.\nUsage: !listinactivechannels [channel id or part of the channel name]";
          } else if (args.equals("botinstancestart")) {
            helpText = "Starts a bot instance with the given name.\nUsage: !botinstancestart <name>";
          } else if (args.equals("botinstancestop")) {
            helpText = "Stops a bot instance with the given name. If no name given, the current instance will be stopped.\nUsage: !botinstancestop [name]";
          } else if (args.equals("botinstancelist")) {
            helpText = "Shows a list of all bot instances with the current status.";
          } else if (args.equals("botinstancelistreload")) {
            helpText = "Reloads the instance list from bot configuration.";
          } else if ((args.equals("setchannelname")) || (args.equals("renamechannel"))) {
            helpText = "Set a new channel name for the given channel id.\nUsage: !setchannelname <channel id> <new channel name>";
          } else if ((args.equals("getchannelid")) || (args.equals("channellist"))) {
            helpText = "Search for channel name to see the channel id or vice versa. The full channel name is not needed, just enter a part of the channel name.\nUsage: !getchannelid [channel id or part of the channel name]";
          } else if (args.equals("setchannelgroup")) {
            helpText = "Sets channel group to client to all specified channels! Separate all channels with a comma at the end of this command!\nUsage: !setchannelgroup <client database id or unique id> <channel group id> <channel list separated with comma>";
          } else if (args.equals("removeservergroups")) {
            helpText = "Removes all server groups of a client!\nUsage: !removeservergroups <client database id or unique id>";
          } else if (args.equals("removechannelgroups")) {
            helpText = "Sets all non-default channel groups of a client to the default channel group in all channels!\nUsage: !removechannelgroups <client database id or unique id>";
          } else if ((args.equals("msgchannelgroup")) || (args.equals("msgchannelgroups"))) {
            helpText = "Sends a private message to all online clients with this specified channel groups at the moment. Multiple comma separated channel groups without spaces are possible.\nUsage: !msgchannelgroup <channelgroup id> <message>\nExample: !msgchannelgroup 8,5 Hello guys!";
          } else if ((args.equals("msgservergroup")) || (args.equals("msgservergroups"))) {
            helpText = "Sends a private message to all online clients that are member of the specified server groups. Multiple comma separated server groups without spaces are possible.\nUsage: !msgservergroup <servergroup id> <message>\nExample: !msgservergroup 6,7 Hello guys!";
          } else if ((args.equals("botjoinchannel")) || (args.equals("joinchannel"))) {
            helpText = "Switch the bot into another channel.\nUsage: !botjoinchannel <channel id>";
          } else if (args.equals("botrename")) {
            helpText = "Without argument bot renames back to default client name from bot config. Specify a client name as argument to set a new temporary client name for the bot.\nUsage: !botrename [new name]";
          } else if (args.equals("exec")) {
            helpText = "Executes the specified system command. This command don't return the text output, just fire and forget! For security reasons this command needs to be enabled at the JTS3ServerMod_InstanceManager.cfg file.\nUsage: !exec <system command>";
          } else if (args.equals("execwait")) {
            helpText = "Executes the specified system command and waits for process end. Program output and return code will be send as answer. For security reasons this command needs to be enabled at the JTS3ServerMod_InstanceManager.cfg file.\nUsage: !execwait <system command>";
          }
          if (helpText == null) {
            helpText = this.modClass.getCommandHelp(args);
          }
          if (helpText == null)
          {
            this.queryLib.sendTextMessage(Integer.parseInt((String)eventInfo.get("invokerid")), 1, "No such command: [b]!" + args + "[/b]!");
          }
          else
          {
            this.queryLib.sendTextMessage(Integer.parseInt((String)eventInfo.get("invokerid")), 1, "Help of the command [b]!" + args + "[/b]:\n");
            this.queryLib.sendTextMessage(Integer.parseInt((String)eventInfo.get("invokerid")), 1, helpText);
          }
        }
      }
    }
    catch (Exception e)
    {
      this.modClass.addLogEntry(e, false);
    }
  }
  
  private String getArguments(String command)
  {
    int pos = command.indexOf(" ");
    if (pos == -1) {
      return "";
    }
    return command.substring(pos).trim();
  }
}
