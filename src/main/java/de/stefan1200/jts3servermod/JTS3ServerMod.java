package de.stefan1200.jts3servermod;

import de.stefan1200.jts3servermod.interfaces.ClientDatabaseCache_Interface;
import de.stefan1200.jts3servermod.interfaces.HandleBotEvents;
import de.stefan1200.jts3servermod.interfaces.HandleClientList;
import de.stefan1200.jts3servermod.interfaces.HandleTS3Events;
import de.stefan1200.jts3servermod.interfaces.JTS3ServerMod_Interface;
import de.stefan1200.jts3servermod.interfaces.LoadConfiguration;
import de.stefan1200.jts3servermod.interfaces.ServerInfoCache_Interface;
import de.stefan1200.jts3serverquery.JTS3ServerQuery;
import de.stefan1200.jts3serverquery.TS3ServerQueryException;
import de.stefan1200.jts3serverquery.TeamspeakActionListener;
import de.stefan1200.util.ArrangedPropertiesWriter;
import de.stefan1200.util.MySQLConnect;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.net.URL;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.BitSet;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.StringTokenizer;
import java.util.Timer;
import java.util.TimerTask;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class JTS3ServerMod
  implements TeamspeakActionListener, JTS3ServerMod_Interface
{
  static final String[] LIST_OPTIONS = { "-away", "-groups", "-info", "-times", "-uid", "-voice", "-country", "-ip" };
  static final String[] ERROR_LEVEL_NAMES = { "DEBUG", "INFO", "WARNING", "ERROR", "CRITICAL", "STATUS" };
  static final byte ERROR_LEVEL_STATUS = 5;
  static final String VERSIONCHECK_URL = "http://www.stefan1200.de/versioncheck/JTS3ServerMod.version2";
  private int reloadState = 2;
  private int reconnectTime = 65000;
  private String CONFIG_FILE_NAME = "server_bot.cfg";
  private String TS3_ADDRESS;
  private int TS3_QUERY_PORT = 10011;
  private String TS3_LOGIN;
  private String TS3_PASSWORD;
  private int TS3_VIRTUALSERVER_ID;
  private int TS3_VIRTUALSERVER_PORT;
  private int BOT_CHANNEL_ID;
  private boolean SLOW_MODE = false;
  private int CHECK_INTERVAL;
  private String MESSAGE_ENCODING;
  private String SERVER_QUERY_NAME;
  private String SERVER_QUERY_NAME_2;
  private boolean RECONNECT_FOREVER;
  private boolean CLIENT_DATABASE_CACHE = false;
  private boolean GLOBAL_MESSAGE_VARS = false;
  private Vector<String> FULL_ADMIN_UID_LIST = new Vector<String>();
  private Vector<String> ADMIN_UID_LIST = new Vector<String>();
  private String CSVLOGGER_FILE;
  private int DEFAULT_CHANNEL_ID = -1;
  private String lastNumberValueAtConfigLoad = null;
  private String lastExceptionAtConfigLoad = null;
  private boolean updateCache = false;
  private boolean DEBUG = false;
  private byte botUpdateCheck = 0;
  private SimpleDateFormat sdf;
  private SimpleDateFormat sdfDebug = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
  private long startTime = 0L;
  private String instanceName;
  private String listArguments = null;
  private String lastActionString = "";
  private Vector<HashMap<String, String>> permissionListCache = null;
  private Vector<HashMap<String, String>> serverGroupListCache = null;
  private Vector<HashMap<String, String>> channelListCache = null;
  private Vector<HashMap<String, String>> clientList = null;
  private Vector<HandleBotEvents> classList_ts3EventServer = new Vector<HandleBotEvents>();
  private Vector<HandleBotEvents> classList_ts3EventChannel = new Vector<HandleBotEvents>();
  private Vector<String> functionList_Name = new Vector<String>();
  private Vector<String> functionList_Prefix = new Vector<String>();
  private Vector<Boolean> functionList_Enabled = new Vector<Boolean>();
  private Vector<Boolean> functionList_Internal = new Vector<Boolean>();
  private Vector<HandleBotEvents> functionList_Class = new Vector<HandleBotEvents>();
  private ArrangedPropertiesWriter config = null;
  private JTS3ServerQuery queryLib;
  private InstanceManager manager;
  private ClientDatabaseCache clientCache = null;
  private ServerInfoCache serverInfoCache = null;
  private ChatCommands chatCommands = null;
  private PrintStream logFile = null;
  private PrintStream csvLogFile = null;
  private byte botLogLevel = 1;
  private Timer botTimer;
  private TimerTask timerCheck;
  private TimerTask timerReconnect;
  private TimerTask timerUpdateCache;
  private BitSet listOptions = new BitSet(10);
  private Pattern patternFunctionPrefix = Pattern.compile("[a-z0-9\\_\\-]+", 66);
  
  public JTS3ServerMod(String configFile)
  {
    this.config = new ArrangedPropertiesWriter();
    initVars();
    this.CONFIG_FILE_NAME = configFile;
    initConfig();
    this.reloadState = 3;
  }
  
  public JTS3ServerMod(ArrangedPropertiesWriter apw, String configFile)
  {
    initVars();
    this.config = apw;
    this.CONFIG_FILE_NAME = configFile;
    this.reloadState = 3;
  }
  
  public JTS3ServerMod(InstanceManager manager, String instanceName, String configFile, String logFilePath, Vector<String> fullBotAdminList)
  {
    this.CONFIG_FILE_NAME = configFile;
    this.manager = manager;
    this.instanceName = instanceName;
    this.FULL_ADMIN_UID_LIST = fullBotAdminList;
    this.config = new ArrangedPropertiesWriter();
    if (logFilePath != null)
    {
      String errorMessage = null;
      try
      {
        File logFileCheck = new File(logFilePath);
        if (logFileCheck.exists())
        {
          File oldLogFileCheck = new File(logFilePath + ".old");
          if (oldLogFileCheck.exists())
          {
            if (oldLogFileCheck.delete())
            {
              if (!logFileCheck.renameTo(oldLogFileCheck)) {
                errorMessage = "Unable to rename file " + logFilePath + " to " + logFilePath + ".old";
              }
            }
            else {
              errorMessage = "Unable to delete file " + logFilePath + ".old";
            }
          }
          else if (!logFileCheck.renameTo(oldLogFileCheck)) {
            errorMessage = "Unable to rename file " + logFilePath + " to " + logFilePath + ".old";
          }
        }
      }
      catch (Exception e)
      {
        errorMessage = e.toString();
      }
      try
      {
        this.logFile = new PrintStream(new FileOutputStream(logFilePath, true), true, "UTF-8");
        if (errorMessage != null) {
          addLogEntry((byte)3, "Error while checking old logfile: " + errorMessage, false);
        }
      }
      catch (Exception e)
      {
        this.logFile = null;
        System.out.println("Error while creating log file: " + logFilePath);
        e.printStackTrace();
      }
    }
    initConfig();
    this.queryLib = new JTS3ServerQuery(instanceName);
  }
  
  public JTS3ServerMod(String instanceName, String configFile)
  {
    initVars();
    this.CONFIG_FILE_NAME = configFile;
    this.instanceName = instanceName;
    this.config = new ArrangedPropertiesWriter();
    initConfig();
    
    this.reloadState = 3;
    int configOK = loadAndCheckConfig(true);
    if (configOK == 0)
    {
      if (this.config.save(this.CONFIG_FILE_NAME, "Config file of the JTS3ServerMod 5.5.4 (11.07.2015)\nhttp://www.stefan1200.de\nThis file must be saved with the encoding ISO-8859-1!")) {
        System.out.println(instanceName + ": Config OK and written updated file to disk!");
      } else {
        System.out.println(instanceName + ": Config OK, but an error occurred while writing to disk! Maybe file write protected?");
      }
    }
    else
    {
      String errorMsg = getErrorMessage(configOK);
      
      System.out.println(instanceName + ": Config checked and found following errors:\n" + errorMsg + "\nNot written to disk!");
    }
  }
  
  static void showHelp()
  {
    System.out.println("JTS3ServerMod 5.5.4 (11.07.2015)");
    System.out.println("-help\t\tShows this help message");
    System.out.println("-version\tShows installed and latest version of JTS3ServerMod");
    System.out.println("-config <path>\tSet a different config file for the instance manager, default config/JTS3ServerMod_InstanceManager.cfg");
    System.out.println("-updateconfig\tCheck bot config file and write updated config file to disk");
    System.out.println("-log <path>\tSet a different path for the instance manager logfile, default JTS3ServerMod_InstanceManager.log");
  }
  
  public static void main(String[] args)
  {
    String configFilePath = null;
    String logFilePath = null;
    if ((args != null) && (args.length > 0)) {
      for (int i = 0; i < args.length; i++) {
        if ((args[i].equalsIgnoreCase("-config")) || (args[i].equalsIgnoreCase("/config")) || (args[i].equalsIgnoreCase("--config")))
        {
          if (args.length > i + 1)
          {
            configFilePath = args[(i + 1)];
            i++;
          }
        }
        else if ((args[i].equalsIgnoreCase("-log")) || (args[i].equalsIgnoreCase("/log")) || (args[i].equalsIgnoreCase("--log")))
        {
          if (args.length > i + 1)
          {
            logFilePath = args[(i + 1)];
            i++;
          }
        }
        else if ((args[i].equalsIgnoreCase("-help")) || (args[i].equalsIgnoreCase("/help")) || (args[i].equalsIgnoreCase("--help")))
        {
          showHelp();
          System.exit(0);
        }
        else if ((args[i].equalsIgnoreCase("-version")) || (args[i].equalsIgnoreCase("/version")) || (args[i].equalsIgnoreCase("--version")))
        {
          showVersionCheck();
          System.exit(0);
        }
        else if ((args[i].equalsIgnoreCase("-versioncheck")) || (args[i].equalsIgnoreCase("/versioncheck")) || (args[i].equalsIgnoreCase("--versioncheck")))
        {
          showVersionCheck();
          System.exit(0);
        }
        else if ((args[i].equalsIgnoreCase("-updateconfig")) || (args[i].equalsIgnoreCase("/updateconfig")) || (args[i].equalsIgnoreCase("--updateconfig")))
        {
          new InstanceManager(configFilePath);
          System.exit(0);
        }
      }
    }
    new InstanceManager(configFilePath, logFilePath);
  }
  
  static void showVersionCheck()
  {
    System.out.println("JTS3ServerMod");
    System.out.println("Current installed version:");
    System.out.println("5.5.4 (11.07.2015) [5504]");
    
    HashMap<String, String> versionData = getVersionCheckData();
    if (versionData != null)
    {
      if (versionData.get("final.version") != null)
      {
        System.out.println("Latest final version:");
        System.out.println((String)versionData.get("final.version") + " [" + (String)versionData.get("final.build") + "]");
      }
      if (versionData.get("dev.version") != null)
      {
        System.out.println("Latest development version:");
        System.out.println((String)versionData.get("dev.version") + " [" + (String)versionData.get("dev.build") + "]");
      }
    }
  }
  
  static HashMap<String, String> getVersionCheckData()
  {
    HashMap<String, String> versionData = new HashMap<String, String>();
    try
    {
      URL versionCheckUrl = new URL("http://www.stefan1200.de/versioncheck/JTS3ServerMod.version2");
      BufferedReader versionCheckStream = new BufferedReader(new InputStreamReader(versionCheckUrl.openStream()));
      String tmp;
      while ((tmp = versionCheckStream.readLine()) != null)
      {
        StringTokenizer st = new StringTokenizer(tmp, ";", false);
        String modeLine = st.nextToken();
        if (modeLine.equalsIgnoreCase("final"))
        {
          versionData.put("final.build", st.nextToken());
          versionData.put("final.version", st.nextToken());
          versionData.put("final.url", st.nextToken());
        }
        if (modeLine.equalsIgnoreCase("development"))
        {
          versionData.put("dev.build", st.nextToken());
          versionData.put("dev.version", st.nextToken());
          versionData.put("dev.url", st.nextToken());
        }
      }
      versionCheckStream.close();
    }
    catch (Exception localException) {}
    return versionData;
  }
  
  private String getPermissionName(int permissionID)
  {
    if (this.permissionListCache == null) {
      return null;
    }
    String sPermissionID = Integer.toString(permissionID);
    String retValue = null;
    for (HashMap<String, String> permission : this.permissionListCache) {
      if (((String)permission.get("permid")).equals(sPermissionID))
      {
        retValue = (String)permission.get("permname");
        break;
      }
    }
    return retValue;
  }
  
  public String getMessageEncoding()
  {
    return this.MESSAGE_ENCODING;
  }
  
  public String getChannelName(int channelID)
  {
    if (this.channelListCache == null) {
      return null;
    }
    String sChannelID = Integer.toString(channelID);
    String retValue = null;
    for (HashMap<String, String> channel : this.channelListCache) {
      if (((String)channel.get("cid")).equals(sChannelID))
      {
        retValue = (String)channel.get("channel_name");
        break;
      }
    }
    return retValue;
  }
  
  private void initConfig()
  {
    this.config.addKey("ts3_server_address", "Teamspeak 3 server address");
    this.config.addKey("ts3_server_query_port", "Teamspeak 3 server query port, default is 10011", "10011");
    this.config.addKey("ts3_server_query_login", "Teamspeak 3 server query admin account name");
    this.config.addKey("ts3_server_query_password", "Teamspeak 3 server query admin password");
    this.config.addKey("ts3_virtualserver_id", "Teamspeak 3 virtual server ID or -1 to use ts3_virtualserver_port");
    this.config.addKey("ts3_virtualserver_port", "Teamspeak 3 virtual server port, only needed if ts3_virtualserver_id is set to -1");
    this.config.addSeparator();
    this.config.addKey("bot_channel_id", "Channel id, the bot will join into it after connecting. If not wanted, use a negative number like -1.\nDon't set the default channel here, because the bot is already in the default channel after connecting.", "-1");
    this.config.addKey("bot_slowmode", "Activate the slow mode of the bot, 0 = disable, 1 = enable.\nIf slow mode is activated, the bot connects slower to the server\nand disables some bot features to reduce the amount of needed commands.\nThis feature may allow you to use the bot without whitelist the bot IP address.\nSlow mode disables the bad channel name check, channel notify, client auto move, client database cache,\nserver group notify, welcome message and do not allow the bot check interval to be lower than 3 seconds.", "0");
    this.config.addKey("bot_check_interval", "Check every X seconds, default is 1. Values between 1 and 30 are allowed.\nIf slow mode is activated, 3 is the lowest possible value.", "1");
    this.config.addKey("bot_messages_encoding", "A different encoding of the messages config files.\nDefault is UTF-8 which should be good for all EU and US languages.\nChange this only if you know what you are doing!\nFor English or German language you can also use the encoding ISO-8859-1\nA list of all valid ones: http://java.sun.com/j2se/1.5.0/docs/guide/intl/encoding.doc.html", "UTF-8");
    this.config.addKey("bot_clientdblist_cache", "This enables the client database list cache. This cache is needed for commands like !lastseen. 1 = Enable, 0 = Disable", "1");
    this.config.addKey("bot_global_message_vars", "This enables the global message variables. See readme file for a list of variables. 1 = Enable, 0 = Disable\nIf enabled, you can use all server variables in all messages. If not needed, disable this to save performance.", "0");
    this.config.addKey("bot_server_query_name", "Server Query name, this will be displayed as name of the connection.", "JTS3ServerMod");
    this.config.addKey("bot_server_query_name_2", "Second Server Query name, this will be displayed as name of the connection.\nThis name will be used, if the first name is already in use.", "MyJTS3ServerMod");
    this.config.addKey("bot_date_pattern", "Change the date pattern, which will be used to format a date in chat functions and welcome message.\nTo get help how to make such a pattern, look here: http://java.sun.com/j2se/1.5.0/docs/api/java/text/SimpleDateFormat.html", "yyyy-MM-dd HH:mm:ss");
    this.config.addKey("bot_connect_forever", "Should the bot try to connect forever if the Teamspeak server or the bot is offline? 0 = disable, 1 = enable", "0");
    this.config.addKey("bot_update_check", "Every time a bot full admin connects to the TS3 server it will be checked if an update for the JTS3ServerMod is available.\nIf an update is available, a chat message will be sent to the bot full admin.\n0 = disable, 1 = final versions, 2 = final and test versions", "0");
    this.config.addKey("bot_log_level", "Minimum log level, how much details you want to be written to the bot log files? Default is 1.\nHigher log levels will be also written, as an example: You set log level to 2, level 2, 3 and critical errors will be written to bot log file.\n0 = Debug\n1 = Information (recommended and default)\n2 = Warning (recommended for smaller log files)\n3 = Error (experts only)", "1");
    this.config.addKey("bot_admin_list", "A comma separated list (without spaces) of unique user ids, which should be able to use bot admin commands.\nThe unique user ids looks like this: mBbHRXwDAG7R19Rv3PorhMwbZW4=");
    this.config.addSeparator();
    this.config.addKey("bot_functions", "Set a comma separated list (without spaces) of needed bot functions here.\nEach function needs the function class and the function name, both separated with a colon.\nAll possible function classes are listed below, that class is case sensitive!\nThat function name you choose is important and has to be unique. It will be used as prefix for the configuration key names and chat commands.\nDon't use spaces in the function names, only use letters, numbers, minus and underscore!\nMost functions allow multiple usage, that allows you to set as many welcome messages or idle check rules, as you want.\nDon't forget that you have to put all settings of the functions in this file.\nHint: Start the bot with the argument -updateconfig after adding bot functions, that writes the configuration for all functions into this file!\nWhole command: java -jar JTS3ServerMod.jar -updateconfig\nNotice: This -updateconfig will also delete all lines of removed or renamed functions in this config file!\nFor more information about the functions read documents/ConfigHelp.html or documents/ConfigHelp_deutsch.html!\nExample: IdleCheck:idle,IdleCheck:idle_guest,MuteMover:mute,WelcomeMessage:welcome,WelcomeMessage:welcome_guest\nThis example gives you the following:\n- Two IdleCheck with the name idle and idle_guest\n- One MuteMover with the name mute\n- Two WelcomeMessage with the name welcome and welcome_guest\n\nFunction list (use only once!):\nAutoMove - Move connecting clients of a specified server group to a specified channel\nLastSeen - Chat command to check the last online time of a client (client database list cache must be enabled!)\n\nFunction list (multiple use possible):\nAdvertising - Send messages to channel or server chat every X minutes\nAwayMover - Move the client as soon as away status is set for longer than X seconds\nBadChannelNameCheck - Checking for bad channel names, can delete the channel and punish the client\nBadNicknameCheck - Checking for bad nicknames and can punish the client\nChannelNotify - Notify specified server groups about clients joining a specified channel\nIdleCheck - Move or kick an idle client, can also send an idle warning message\nInactiveChannelCheck - Delete channels if empty for more than X hours\nMuteMover - Move the client as soon as the specified mute status is set for longer than X seconds\nRecordCheck - Move or kick a recording client (of course only the record function of the Teamspeak client is detected)\nServerGroupNotify - Notify specified server groups about clients of specified server groups connecting to the TS3 server\nServerGroupProtection - Make sure that only specified clients are members of the specified server groups\nWelcomeMessage - Sends a message to new connected clients");
    
    this.config.addKey("bot_functions_disabled", "Set a comma separated list (without spaces) of needed but disabled bot functions here.\nSame format as bot_functions!\nAll functions you set here are not activated at bot start, but you can switch on functions using chat commands.");
    this.config.addSeparator();
    this.config.addSeparator();
  }
  
  private void initVars()
  {
    if (this.config != null) {
      this.config.removeAllValues();
    }
    if (this.manager != null) {
      this.DEBUG = this.manager.isDebugModeEnabled(this);
    } else {
      this.DEBUG = false;
    }
    if (this.queryLib != null)
    {
      this.queryLib.DEBUG_COMMLOG_PATH = ("JTS3ServerQuery-communication" + (this.instanceName == null ? "" : new StringBuilder("_").append(this.instanceName).toString()) + ".log");
      this.queryLib.DEBUG_ERRLOG_PATH = ("JTS3ServerQuery-error" + (this.instanceName == null ? "" : new StringBuilder("_").append(this.instanceName).toString()) + ".log");
      this.queryLib.DEBUG = this.DEBUG;
    }
    this.TS3_ADDRESS = null;
    this.TS3_QUERY_PORT = 10011;
    this.TS3_LOGIN = null;
    this.TS3_PASSWORD = null;
    this.TS3_VIRTUALSERVER_ID = -1;
    this.TS3_VIRTUALSERVER_PORT = -1;
    this.BOT_CHANNEL_ID = -1;
    this.botLogLevel = 1;
    
    this.SLOW_MODE = false;
    this.CHECK_INTERVAL = 1;
    this.MESSAGE_ENCODING = "UTF-8";
    this.SERVER_QUERY_NAME = null;
    this.SERVER_QUERY_NAME_2 = null;
    this.RECONNECT_FOREVER = false;
    this.ADMIN_UID_LIST = new Vector<String>();
    
    this.DEFAULT_CHANNEL_ID = -1;
    this.lastNumberValueAtConfigLoad = null;
    this.updateCache = false;
    this.botUpdateCheck = 0;
    this.permissionListCache = null;
    this.clientCache = null;
    this.serverInfoCache = null;
    this.serverGroupListCache = null;
    this.lastActionString = "";
    
    this.listArguments = null;
    this.listOptions.clear();
    
    this.botTimer = new Timer(true);
  }
  
  int loadAndCheckConfig(boolean fromFile)
  {
    int errorCode = loadConfig(fromFile);
    if (errorCode != 0) {
      return errorCode;
    }
    return 0;
  }
  
  String getErrorMessage(int errorCode)
  {
    if (errorCode == 20)
    {
      if (this.lastNumberValueAtConfigLoad != null) {
        return "Critical: An expected number value in config is not a number or is completely missing! Look at value " + this.lastNumberValueAtConfigLoad + " in config file: " + this.CONFIG_FILE_NAME;
      }
      return "Critical: An expected number value in config is not a number or is completely missing! Config file: " + this.CONFIG_FILE_NAME;
    }
    if (errorCode == 23) {
      return "Critical: Teamspeak 3 server address, loginname or password missing in config file! Config file: " + this.CONFIG_FILE_NAME;
    }
    if (errorCode == 31) {
      return "Critical: Server query name in config file need at least 3 characters! Config file: " + this.CONFIG_FILE_NAME;
    }
    if (errorCode == 32) {
      return "Critical: Config file missing or is not readable, check: " + this.CONFIG_FILE_NAME;
    }
    if (errorCode == 40) {
      return "Critical: Unexpected error occurred in bot configuration! Config file: " + this.CONFIG_FILE_NAME + ((this.lastExceptionAtConfigLoad != null) && (this.logFile == null) ? "\n" + this.lastExceptionAtConfigLoad : "");
    }
    return "Unknown";
  }
  
  private void loadFunctions()
  {
    int duplicateClassCount = 0;
    boolean loadBotClass = false;
    int configKeyCount = 0;
    for (int i = 0; i < this.functionList_Prefix.size(); i++) {
      try
      {
        Class<?> newFunction = Class.forName("de.stefan1200.jts3servermod.functions." + (String)this.functionList_Name.elementAt(i));
        if (HandleBotEvents.class.isAssignableFrom(newFunction))
        {
          loadBotClass = false;
          duplicateClassCount = Collections.frequency(this.functionList_Name, this.functionList_Name.elementAt(i));
          Object functionClass = (HandleBotEvents)newFunction.newInstance();
          if (duplicateClassCount > 1)
          {
            if (((HandleBotEvents)functionClass).multipleInstances())
            {
              loadBotClass = true;
            }
            else
            {
              addLogEntry((byte)2, "Function class \"" + (String)this.functionList_Name.elementAt(i) + "\" can only used once per virtual bot instance! Skipping all of this.", getMySQLConnection() == null);
              removeDuplicateFunctions((String)this.functionList_Name.elementAt(i));
              i--;
            }
          }
          else {
            loadBotClass = true;
          }
          if (loadBotClass)
          {
            ((HandleBotEvents)functionClass).initClass(this, this.queryLib, (String)this.functionList_Prefix.elementAt(i));
            this.functionList_Class.setElementAt((HandleBotEvents)functionClass, i);
            if (LoadConfiguration.class.isAssignableFrom(newFunction))
            {
              configKeyCount = this.config.getKeyCount();
              ((LoadConfiguration)functionClass).initConfig(this.config);
              if (configKeyCount < this.config.getKeyCount()) {
                this.config.addSeparator();
              }
            }
            addLogEntry((byte)1, "Successfully loaded function: " + (String)this.functionList_Name.elementAt(i) + " / " + (String)this.functionList_Prefix.elementAt(i), false);
          }
        }
        else
        {
          addLogEntry((byte)3, "Can't load function class \"" + (String)this.functionList_Name.elementAt(i) + "\", it's not a valid bot function! Skipping all of this.", false);
          removeDuplicateFunctions((String)this.functionList_Name.elementAt(i));
          i--;
        }
      }
      catch (Exception e)
      {
        addLogEntry((byte)3, "Error loading function, skipping: " + (String)this.functionList_Name.elementAt(i) + " / " + (String)this.functionList_Prefix.elementAt(i), getMySQLConnection() == null);
        addLogEntry(e, false);
        this.functionList_Name.removeElementAt(i);
        this.functionList_Prefix.removeElementAt(i);
        this.functionList_Enabled.removeElementAt(i);
        this.functionList_Internal.removeElementAt(i);
        this.functionList_Class.removeElementAt(i);
        i--;
      }
    }
  }
  
  int[] reloadConfig(boolean preconnect)
  {
    String adminListTemp = this.config.getValue("bot_admin_list");
    if (adminListTemp != null)
    {
      this.ADMIN_UID_LIST.clear();
      StringTokenizer adminListTokenizer = new StringTokenizer(adminListTemp, ",", false);
      while (adminListTokenizer.hasMoreTokens()) {
        this.ADMIN_UID_LIST.addElement(adminListTokenizer.nextToken().trim());
      }
    }
    try
    {
      this.sdf = new SimpleDateFormat(this.config.getValue("bot_date_pattern", "yyyy-MM-dd HH:mm:ss"));
    }
    catch (Exception eSDF)
    {
      addLogEntry((byte)3, "Date pattern from config value of bot_date_pattern is invalid, using now the default date pattern!", getMySQLConnection() == null);
      addLogEntry(eSDF, false);
    }
    this.GLOBAL_MESSAGE_VARS = this.config.getValue("bot_global_message_vars", "0").equals("1");
    
    int[] count = new int[2];
    for (int i = 0; i < this.functionList_Class.size(); i++) {
      if (LoadConfiguration.class.isAssignableFrom(((HandleBotEvents)this.functionList_Class.elementAt(i)).getClass())) {
        try
        {
          if (((LoadConfiguration)this.functionList_Class.elementAt(i)).loadConfig(this.config, this.SLOW_MODE))
          {
            if ((((Boolean)this.functionList_Enabled.elementAt(i)).booleanValue()) && (!preconnect))
            {
              ((HandleBotEvents)this.functionList_Class.elementAt(i)).disable();
              ((HandleBotEvents)this.functionList_Class.elementAt(i)).activate();
            }
            count[1] += 1;
          }
          else
          {
            count[0] += 1;
          }
        }
        catch (Exception e)
        {
          count[0] += 1;
          addLogEntry((String)this.functionList_Prefix.elementAt(i), e, true);
        }
      }
    }
    return count;
  }
  
  int reloadConfig(String prefix)
  {
    for (int i = 0; i < this.functionList_Prefix.size(); i++) {
      if (((String)this.functionList_Prefix.elementAt(i)).equalsIgnoreCase(prefix))
      {
        if (!LoadConfiguration.class.isAssignableFrom(((HandleBotEvents)this.functionList_Class.elementAt(i)).getClass())) {
          break;
        }
        try
        {
          boolean result = ((LoadConfiguration)this.functionList_Class.elementAt(i)).loadConfig(this.config, this.SLOW_MODE);
          if (result) {
            if (((Boolean)this.functionList_Enabled.elementAt(i)).booleanValue())
            {
              ((HandleBotEvents)this.functionList_Class.elementAt(i)).disable();
              ((HandleBotEvents)this.functionList_Class.elementAt(i)).activate();
            }
          }
          return result ? 1 : 0;
        }
        catch (Exception e)
        {
          addLogEntry(e, true);
        }
      }
    }
    return -1;
  }
  
  String[] getCurrentLoadedFunctions()
  {
    StringBuffer sbTempEnabled = new StringBuffer();
    StringBuffer sbTempDisabled = new StringBuffer();
    for (int i = 0; i < this.functionList_Prefix.size(); i++) {
      if (((Boolean)this.functionList_Enabled.elementAt(i)).booleanValue())
      {
        if (sbTempEnabled.length() > 0) {
          sbTempEnabled.append("\n");
        }
        sbTempEnabled.append((String)this.functionList_Prefix.elementAt(i));
        sbTempEnabled.append(" / ");
        sbTempEnabled.append((String)this.functionList_Name.elementAt(i));
      }
      else
      {
        if (sbTempDisabled.length() > 0) {
          sbTempDisabled.append("\n");
        }
        sbTempDisabled.append((String)this.functionList_Prefix.elementAt(i));
        sbTempDisabled.append(" / ");
        sbTempDisabled.append((String)this.functionList_Name.elementAt(i));
      }
    }
    String[] retValue = new String[2];
    retValue[0] = sbTempEnabled.toString();
    retValue[1] = sbTempDisabled.toString();
    return retValue;
  }
  
  byte activateFunction(String prefix)
  {
    for (int i = 0; i < this.functionList_Prefix.size(); i++) {
      if (((String)this.functionList_Prefix.elementAt(i)).equalsIgnoreCase(prefix))
      {
        if (((Boolean)this.functionList_Enabled.elementAt(i)).booleanValue()) {
          return 0;
        }
        try
        {
          ((HandleBotEvents)this.functionList_Class.elementAt(i)).activate();
          this.functionList_Enabled.setElementAt(Boolean.valueOf(true), i);
          createBotFunctionsConfig();
          addLogEntry((byte)1, "Function activated: " + (String)this.functionList_Name.elementAt(i) + " / " + (String)this.functionList_Prefix.elementAt(i), false);
          return 1;
        }
        catch (Exception e)
        {
          addLogEntry(e, false);
        }
      }
    }
    return -1;
  }
  
  byte disableFunction(String prefix)
  {
    for (int i = 0; i < this.functionList_Prefix.size(); i++) {
      if (((String)this.functionList_Prefix.elementAt(i)).equalsIgnoreCase(prefix))
      {
        if (!((Boolean)this.functionList_Enabled.elementAt(i)).booleanValue()) {
          return 0;
        }
        try
        {
          ((HandleBotEvents)this.functionList_Class.elementAt(i)).disable();
          this.functionList_Enabled.setElementAt(Boolean.valueOf(false), i);
          createBotFunctionsConfig();
          addLogEntry((byte)1, "Function disabled: " + (String)this.functionList_Name.elementAt(i) + " / " + (String)this.functionList_Prefix.elementAt(i), false);
          return 1;
        }
        catch (Exception e)
        {
          addLogEntry(e, false);
        }
      }
    }
    return -1;
  }
  
  void createBotFunctionsConfig()
  {
    StringBuffer sbBotFunctions = new StringBuffer();
    StringBuffer sbBotFunctionsDisabled = new StringBuffer();
    for (int i = 0; i < this.functionList_Prefix.size(); i++) {
      if (((Boolean)this.functionList_Internal.elementAt(i)).booleanValue()) {
        if (((Boolean)this.functionList_Enabled.elementAt(i)).booleanValue())
        {
          if (sbBotFunctions.length() > 0) {
            sbBotFunctions.append(",");
          }
          sbBotFunctions.append((String)this.functionList_Name.elementAt(i));
          sbBotFunctions.append(":");
          sbBotFunctions.append((String)this.functionList_Prefix.elementAt(i));
        }
        else
        {
          if (sbBotFunctionsDisabled.length() > 0) {
            sbBotFunctionsDisabled.append(",");
          }
          sbBotFunctionsDisabled.append((String)this.functionList_Name.elementAt(i));
          sbBotFunctionsDisabled.append(":");
          sbBotFunctionsDisabled.append((String)this.functionList_Prefix.elementAt(i));
        }
      }
    }
    this.config.setValue("bot_functions", sbBotFunctions.toString());
    this.config.setValue("bot_functions_disabled", sbBotFunctionsDisabled.toString());
  }
  
  String getCommandList(HashMap<String, String> eventInfo, boolean isFullAdmin, boolean isAdmin)
  {
    StringBuffer sbTemp = new StringBuffer();
    for (int i = 0; i < this.functionList_Class.size(); i++) {
      if (((Boolean)this.functionList_Enabled.elementAt(i)).booleanValue()) {
        if (HandleTS3Events.class.isAssignableFrom(((HandleBotEvents)this.functionList_Class.elementAt(i)).getClass())) {
          try
          {
            String sPrefix = (String)this.functionList_Prefix.elementAt(i);
            String[] commands = ((HandleTS3Events)this.functionList_Class.elementAt(i)).botChatCommandList(eventInfo, isFullAdmin, isAdmin);
            if (commands != null)
            {
              String[] arrayOfString1;
              int j = (arrayOfString1 = commands).length;
              for (int k = 0; k < j; k++)
              {
                String command = arrayOfString1[k];
                if (command.length() == 0) {
                  sbTemp.append("\n!" + sPrefix);
                } else {
                  sbTemp.append("\n!" + sPrefix + " " + command);
                }
              }
            }
          }
          catch (Exception e)
          {
            addLogEntry(e, true);
          }
        }
      }
    }
    return sbTemp.toString();
  }
  
  String getCommandHelp(String args)
  {
    String commandArgs = "";
    int pos = args.indexOf(" ");
    String sPrefix;
    if (pos == -1)
    {
      sPrefix = args;
    }
    else
    {
      sPrefix = args.substring(0, pos);
      commandArgs = args.substring(pos + 1);
    }
    for (int i = 0; i < this.functionList_Prefix.size(); i++) {
      if (((Boolean)this.functionList_Enabled.elementAt(i)).booleanValue()) {
        if (((String)this.functionList_Prefix.elementAt(i)).equalsIgnoreCase(sPrefix)) {
          if (HandleTS3Events.class.isAssignableFrom(((HandleBotEvents)this.functionList_Class.elementAt(i)).getClass())) {
            try
            {
              return ((HandleTS3Events)this.functionList_Class.elementAt(i)).botChatCommandHelp(commandArgs);
            }
            catch (Exception e)
            {
              addLogEntry(e, false);
            }
          }
        }
      }
    }
    return null;
  }
  
  private void removeDuplicateFunctions(String className)
  {
    for (int i = 0; i < this.functionList_Prefix.size(); i++) {
      if (((String)this.functionList_Name.elementAt(i)).equals(className))
      {
        this.functionList_Name.removeElementAt(i);
        this.functionList_Prefix.removeElementAt(i);
        this.functionList_Enabled.removeElementAt(i);
        this.functionList_Internal.removeElementAt(i);
        this.functionList_Class.removeElementAt(i);
        i--;
      }
    }
  }
  
  private void unloadAllFunctions()
  {
    for (int i = 0; i < this.functionList_Class.size(); i++)
    {
      if (this.functionList_Class.elementAt(i) != null) {
        ((HandleBotEvents)this.functionList_Class.elementAt(0)).disable();
      }
      if (this.functionList_Class.elementAt(i) != null) {
        ((HandleBotEvents)this.functionList_Class.elementAt(0)).unload();
      }
    }
    this.functionList_Name.clear();
    this.functionList_Prefix.clear();
    this.functionList_Enabled.clear();
    this.functionList_Internal.clear();
    this.functionList_Class.clear();
    this.classList_ts3EventChannel.clear();
    this.classList_ts3EventServer.clear();
    
    addLogEntry((byte)1, "Unloaded all functions!", false);
  }
  
  public void unloadFunction(Object c)
  {
    int pos = this.functionList_Class.indexOf(c);
    if (pos != -1)
    {
      addLogEntry((byte)1, "Successfully unloaded function: " + (String)this.functionList_Name.elementAt(pos) + " / " + (String)this.functionList_Prefix.elementAt(pos), false);
      ((HandleBotEvents)c).disable();
      ((HandleBotEvents)c).unload();
      this.functionList_Name.removeElementAt(pos);
      this.functionList_Prefix.removeElementAt(pos);
      this.functionList_Enabled.removeElementAt(pos);
      this.functionList_Internal.removeElementAt(pos);
      this.functionList_Class.removeElementAt(pos);
      this.classList_ts3EventChannel.removeElement(c);
      this.classList_ts3EventServer.removeElement(c);
    }
    else
    {
      addLogEntry((byte)2, "Unloading function not possible, function not found!", false);
    }
  }
  
  private String getFunctionPrefix(Object c)
  {
    int pos = this.functionList_Class.indexOf(c);
    if (pos != -1) {
      return (String)this.functionList_Prefix.elementAt(pos);
    }
    return null;
  }
  
  boolean loadConfigValues()
  {
    return this.config.loadValues(this.CONFIG_FILE_NAME);
  }
  
  private int loadConfig(boolean fromFile)
  {
    String lastNumberValue = null;
    if (fromFile) {
      if (!this.config.loadValues(this.CONFIG_FILE_NAME)) {
        return 32;
      }
    }
    try
    {
      if (this.DEBUG)
      {
        this.botLogLevel = 0;
      }
      else
      {
        lastNumberValue = "bot_log_level";
        this.botLogLevel = Byte.parseByte(this.config.getValue("bot_log_level", "1").trim());
        if (this.botLogLevel > 3) {
          this.botLogLevel = 3;
        }
        addLogEntry((byte)5, "Activate log level: " + ERROR_LEVEL_NAMES[this.botLogLevel], false);
      }
      if (this.manager != null)
      {
        this.CSVLOGGER_FILE = this.manager.getConnectionLogPath(this);
        this.botUpdateCheck = this.manager.getBotUpdateCheckState();
      }
      else
      {
        this.csvLogFile = null;
        this.botUpdateCheck = 0;
      }
      String temp = this.config.getValue("bot_slowmode", "0");
      if (temp.equals("1")) {
        this.SLOW_MODE = true;
      } else {
        this.SLOW_MODE = false;
      }
      lastNumberValue = "bot_check_interval";
      this.CHECK_INTERVAL = Integer.parseInt(this.config.getValue("bot_check_interval", "1").trim());
      if (this.CHECK_INTERVAL > 30) {
        this.CHECK_INTERVAL = 30;
      }
      if (this.SLOW_MODE)
      {
        if (this.CHECK_INTERVAL < 3) {
          this.CHECK_INTERVAL = 3;
        }
      }
      else if (this.CHECK_INTERVAL < 1) {
        this.CHECK_INTERVAL = 1;
      }
      this.SERVER_QUERY_NAME = this.config.getValue("bot_server_query_name", "JTS3ServerMod");
      this.SERVER_QUERY_NAME_2 = this.config.getValue("bot_server_query_name_2");
      this.MESSAGE_ENCODING = this.config.getValue("bot_messages_encoding", "UTF-8");
      if (this.SERVER_QUERY_NAME.length() < 3) {
        return 31;
      }
      temp = this.config.getValue("bot_clientdblist_cache", "0").trim();
      if (temp.equals("1"))
      {
        if (this.SLOW_MODE) {
          this.CLIENT_DATABASE_CACHE = false;
        } else {
          this.CLIENT_DATABASE_CACHE = true;
        }
      }
      else {
        this.CLIENT_DATABASE_CACHE = false;
      }
      temp = this.config.getValue("bot_connect_forever", "0").trim();
      if (temp.equals("1")) {
        this.RECONNECT_FOREVER = true;
      } else {
        this.RECONNECT_FOREVER = false;
      }
      this.TS3_ADDRESS = this.config.getValue("ts3_server_address");
      lastNumberValue = "ts3_server_query_port";
      temp = this.config.getValue("ts3_server_query_port");
      if (temp == null) {
        throw new NumberFormatException();
      }
      this.TS3_QUERY_PORT = Integer.parseInt(temp.trim());
      this.TS3_LOGIN = this.config.getValue("ts3_server_query_login");
      this.TS3_PASSWORD = this.config.getValue("ts3_server_query_password");
      lastNumberValue = "ts3_virtualserver_id";
      temp = this.config.getValue("ts3_virtualserver_id");
      if (temp == null) {
        throw new NumberFormatException();
      }
      this.TS3_VIRTUALSERVER_ID = Integer.parseInt(temp.trim());
      lastNumberValue = "bot_channel_id";
      this.BOT_CHANNEL_ID = Integer.parseInt(this.config.getValue("bot_channel_id", "-1").trim());
      if (this.TS3_ADDRESS.equalsIgnoreCase("ts3.server.net"))
      {
        addLogEntry((byte)3, " ", getMySQLConnection() == null);
        addLogEntry((byte)3, "**********************************************************************************", getMySQLConnection() == null);
        addLogEntry((byte)3, "It seems you never touched the config, check file " + this.CONFIG_FILE_NAME, getMySQLConnection() == null);
        addLogEntry((byte)3, "**********************************************************************************", getMySQLConnection() == null);
        addLogEntry((byte)3, " ", getMySQLConnection() == null);
      }
      if (this.TS3_VIRTUALSERVER_ID < 1)
      {
        lastNumberValue = "ts3_virtualserver_port";
        temp = this.config.getValue("ts3_virtualserver_port");
        if (temp == null) {
          throw new NumberFormatException();
        }
        this.TS3_VIRTUALSERVER_PORT = Integer.parseInt(temp.trim());
      }
      if ((this.TS3_ADDRESS == null) || (this.TS3_LOGIN == null) || (this.TS3_PASSWORD == null)) {
        return 23;
      }
      temp = this.config.getValue("bot_functions", "").trim();
      if ((temp != null) && (temp.length() > 0))
      {
        String sFunctionTemp = null;
        String[] functionTemp = null;
        StringTokenizer st = new StringTokenizer(temp, ",", false);
        while (st.hasMoreTokens())
        {
          sFunctionTemp = st.nextToken().trim();
          functionTemp = sFunctionTemp.split(":");
          if (functionTemp.length == 2)
          {
            if (this.functionList_Prefix.indexOf(functionTemp[1]) == -1)
            {
              Matcher ruleCheck = this.patternFunctionPrefix.matcher(functionTemp[1]);
              if (ruleCheck.matches())
              {
                this.functionList_Name.addElement(functionTemp[0]);
                this.functionList_Prefix.addElement(functionTemp[1]);
                this.functionList_Enabled.addElement(Boolean.valueOf(true));
                this.functionList_Internal.addElement(Boolean.valueOf(true));
                this.functionList_Class.addElement(null);
              }
              else
              {
                addLogEntry((byte)3, "Your chosen function name is not valid. Don't use spaces, only use letters, numbers, underscore and minus. Skipping this one: " + functionTemp[1], getMySQLConnection() == null);
              }
            }
            else
            {
              addLogEntry((byte)3, "Your chosen function name is not unique, skipping this one: " + functionTemp[1], getMySQLConnection() == null);
            }
          }
          else {
            addLogEntry((byte)3, "This wrong: \"" + sFunctionTemp + "\". Skipping this one. Right format is function class and function name separated with a colon!", getMySQLConnection() == null);
          }
        }
      }
      temp = this.config.getValue("bot_functions_disabled", "").trim();
      if ((temp != null) && (temp.length() > 0))
      {
        String sFunctionTemp = null;
        String[] functionTemp = null;
        StringTokenizer st = new StringTokenizer(temp, ",", false);
        while (st.hasMoreTokens())
        {
          sFunctionTemp = st.nextToken().trim();
          functionTemp = sFunctionTemp.split(":");
          if (functionTemp.length == 2)
          {
            if (this.functionList_Prefix.indexOf(functionTemp[1]) == -1)
            {
              Matcher ruleCheck = this.patternFunctionPrefix.matcher(functionTemp[1]);
              if (ruleCheck.matches())
              {
                this.functionList_Name.addElement(functionTemp[0]);
                this.functionList_Prefix.addElement(functionTemp[1]);
                this.functionList_Enabled.addElement(Boolean.valueOf(false));
                this.functionList_Internal.addElement(Boolean.valueOf(true));
                this.functionList_Class.addElement(null);
              }
              else
              {
                addLogEntry((byte)3, "Your chosen function name is not valid. Don't use spaces, only use letters, numbers, underscore and minus. Skipping this one: " + functionTemp[1], getMySQLConnection() == null);
              }
            }
            else
            {
              addLogEntry((byte)3, "Your chosen function name is not unique, skipping this one: " + functionTemp[1], getMySQLConnection() == null);
            }
          }
          else {
            addLogEntry((byte)3, "This wrong: \"" + sFunctionTemp + "\". Skipping this one. Right format is function class and function name separated with a colon!", getMySQLConnection() == null);
          }
        }
      }
      loadFunctions();
      if (fromFile) {
        if (!this.config.loadValues(this.CONFIG_FILE_NAME)) {
          return 32;
        }
      }
      reloadConfig(true);
    }
    catch (NumberFormatException nfe)
    {
      addLogEntry(nfe, false);
      this.lastNumberValueAtConfigLoad = new String(lastNumberValue);
      
      return 20;
    }
    catch (Exception e1)
    {
      addLogEntry(e1, false);
      this.lastExceptionAtConfigLoad = getStackTrace(e1);
      
      return 40;
    }
    return 0;
  }
  
  public boolean loadMessages(String configPrefix, String configKey_Path, String[] configValues)
  {
    String path = this.config.getValue(configPrefix + configKey_Path);
    if (path == null)
    {
      addLogEntry(configPrefix, (byte)3, "Path to messages was not set in bot config! Check config key: " + configPrefix + configKey_Path, getMySQLConnection() == null);
      return false;
    }
    path = path.trim();
    try
    {
      BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(path), this.MESSAGE_ENCODING));
      
      String line = br.readLine();
      if ((this.MESSAGE_ENCODING.equalsIgnoreCase("UTF-8")) && (line != null) && (line.charAt(0) == 65279)) {
        line = line.substring(1);
      }
      if ((line == null) || (!line.equals("# JTS3ServerMod Config File")))
      {
        addLogEntry(configPrefix, (byte)3, "Special config file header is missing at the config file! File path: " + path, getMySQLConnection() == null);
        addLogEntry(configPrefix, (byte)3, "Check if you set the right file at config key: " + configPrefix + configKey_Path, getMySQLConnection() == null);
        br.close();
        return false;
      }
      int count = 0;
      while ((line = br.readLine()) != null) {
        if (!line.startsWith("#")) {
          if (line.length() > 3)
          {
            line = line.replace("\\n", "\n");
            
            this.config.setValue(configValues[count], line);
            
            count++;
            if (count >= configValues.length) {
              break;
            }
          }
        }
      }
      br.close();
    }
    catch (FileNotFoundException fnfe)
    {
      addLogEntry(configPrefix, (byte)3, "The file you set at config key \"" + configPrefix + configKey_Path + "\" does not exist or missing permission for reading, check file path: " + path, getMySQLConnection() == null);
      return false;
    }
    catch (Exception e)
    {
      addLogEntry(configPrefix, (byte)3, "Unknown error while loading messages, check file you set at config key \"" + configPrefix + configKey_Path + "\", the file path: " + path, getMySQLConnection() == null);
      addLogEntry(configPrefix, e, false);
      return false;
    }
    return true;
  }
  
  private void outputStartMSG()
  {
    if (this.CSVLOGGER_FILE != null)
    {
      String msg = "Server connection log is activated and will be written into the " + (this.CSVLOGGER_FILE.equals("sql") ? "MySQL database" : new StringBuilder("file: ").append(this.CSVLOGGER_FILE).toString());
      addLogEntry((byte)1, msg, true);
    }
    for (int i = 0; i < this.functionList_Class.size(); i++) {
      if (((Boolean)this.functionList_Enabled.elementAt(i)).booleanValue()) {
        if (HandleBotEvents.class.isAssignableFrom(((HandleBotEvents)this.functionList_Class.elementAt(i)).getClass())) {
          try
          {
            ((HandleBotEvents)this.functionList_Class.elementAt(i)).handleOnBotConnect();
            ((HandleBotEvents)this.functionList_Class.elementAt(i)).activate();
          }
          catch (Exception e)
          {
            addLogEntry(e, true);
          }
        }
      }
    }
  }
  
  private void setListOptions()
  {
    this.listOptions.clear();
    if ((this.CSVLOGGER_FILE != null) || (this.clientCache != null))
    {
      this.listOptions.set(4);
      this.listOptions.set(7);
    }
    for (int i = 0; i < this.functionList_Class.size(); i++) {
      if (LoadConfiguration.class.isAssignableFrom(((HandleBotEvents)this.functionList_Class.elementAt(i)).getClass())) {
        try
        {
          ((LoadConfiguration)this.functionList_Class.elementAt(i)).setListModes(this.listOptions);
        }
        catch (Exception e)
        {
          addLogEntry(e, true);
        }
      }
    }
  }
  
  private void createListArguments()
  {
    this.listArguments = "";
    for (int i = this.listOptions.nextSetBit(0); i >= 0; i = this.listOptions.nextSetBit(i + 1)) {
      if (this.listArguments.equals("")) {
        this.listArguments += LIST_OPTIONS[i];
      } else {
        this.listArguments = (this.listArguments + "," + LIST_OPTIONS[i]);
      }
    }
  }
  
  void runThread()
  {
    Thread t = new Thread(new Runnable()
    {
      public void run()
      {
        do
        {
          if (JTS3ServerMod.this.reloadState == 2)
          {
            JTS3ServerMod.this.initVars();
            JTS3ServerMod.this.runMod();
          }
          try
          {
            Thread.sleep(100L);
          }
          catch (Exception localException) {}
        } while (JTS3ServerMod.this.reloadState != 0);
        JTS3ServerMod.this.addLogEntry((byte)5, "Virtual bot instance \"" + JTS3ServerMod.this.instanceName + "\" stopped", JTS3ServerMod.this.getMySQLConnection() == null);
      }
    });
    t.setName(this.instanceName);
    t.start();
  }
  
  private void runMod()
  {
    this.reloadState = 3;
    this.startTime = System.currentTimeMillis();
    addLogEntry((byte)5, "Virtual bot instance \"" + this.instanceName + "\" starts now" + (this.DEBUG ? " - Debug mode activated!" : ""), getMySQLConnection() == null);
    int configOK = loadAndCheckConfig(true);
    this.reloadState = 1;
    if (configOK != 0)
    {
      addLogEntry((byte)4, getErrorMessage(configOK), getMySQLConnection() == null);
      stopBotInstance(0);
      return;
    }
    if (this.SLOW_MODE) {
      addLogEntry((byte)1, "Slow mode activated (using less commands at once)", getMySQLConnection() == null);
    }
    try
    {
      this.queryLib.connectTS3Query(this.TS3_ADDRESS, this.TS3_QUERY_PORT);
      addLogEntry((byte)1, "Successful connected to " + this.TS3_ADDRESS + "!", getMySQLConnection() == null);
    }
    catch (Exception e)
    {
      addLogEntry((byte)4, "Unable to connect to Teamspeak 3 server at " + this.TS3_ADDRESS + "!", getMySQLConnection() == null);
      addLogEntry(e, getMySQLConnection() == null);
      
      reconnectBot(false);
      return;
    }
    try
    {
      this.queryLib.loginTS3(this.TS3_LOGIN, this.TS3_PASSWORD);
      addLogEntry((byte)1, "Login as \"" + this.TS3_LOGIN + "\" successful!", getMySQLConnection() == null);
    }
    catch (Exception e)
    {
      addLogEntry((byte)4, "Unable to login as \"" + this.TS3_LOGIN + "\"! Maybe this IP address is banned for some minutes on that server!", getMySQLConnection() == null);
      addLogEntry(e, getMySQLConnection() == null);
      
      stopBotInstance(0);
      return;
    }
    try
    {
      this.permissionListCache = this.queryLib.getList(6);
    }
    catch (Exception e)
    {
      addLogEntry((byte)2, "Unable to receive permission list! If wanted, set permission b_serverinstance_permission_list to server group Guest Server Query.", getMySQLConnection() == null);
      addLogEntry(e, false);
    }
    if (this.SLOW_MODE)
    {
      try
      {
        Thread.sleep(3000L);
      }
      catch (Exception localException1) {}
      if (this.botTimer == null) {
        return;
      }
    }
    if (this.TS3_VIRTUALSERVER_ID >= 1) {
      try
      {
        this.queryLib.selectVirtualServer(this.TS3_VIRTUALSERVER_ID, false);
        addLogEntry((byte)1, "Successful selected virtual server " + Integer.toString(this.TS3_VIRTUALSERVER_ID) + "!", getMySQLConnection() == null);
      }
      catch (Exception e)
      {
        addLogEntry((byte)4, "Unable to select virtual server " + Integer.toString(this.TS3_VIRTUALSERVER_ID) + "!", getMySQLConnection() == null);
        addLogEntry(e, getMySQLConnection() == null);
        
        reconnectBot(false);
        return;
      }
    } else {
      try
      {
        this.queryLib.selectVirtualServer(this.TS3_VIRTUALSERVER_PORT, true);
        addLogEntry((byte)1, "Successful selected virtual server on port " + Integer.toString(this.TS3_VIRTUALSERVER_PORT) + "!", getMySQLConnection() == null);
      }
      catch (Exception e)
      {
        addLogEntry((byte)4, "Unable to select virtual server on port " + Integer.toString(this.TS3_VIRTUALSERVER_PORT) + "!", getMySQLConnection() == null);
        addLogEntry(e, getMySQLConnection() == null);
        
        reconnectBot(false);
        return;
      }
    }
    try
    {
      this.queryLib.setDisplayName(this.SERVER_QUERY_NAME);
    }
    catch (Exception e)
    {
      addLogEntry((byte)2, "Unable to change name to \"" + this.SERVER_QUERY_NAME + "\"!", getMySQLConnection() == null);
      addLogEntry(e, getMySQLConnection() == null);
      if ((this.SERVER_QUERY_NAME_2 != null) && (this.SERVER_QUERY_NAME_2.length() >= 3))
      {
        addLogEntry((byte)1, "Try using \"" + this.SERVER_QUERY_NAME_2 + "\"...", getMySQLConnection() == null);
        try
        {
          this.queryLib.setDisplayName(this.SERVER_QUERY_NAME_2);
        }
        catch (Exception e2)
        {
          addLogEntry((byte)2, "Unable to change name to \"" + this.SERVER_QUERY_NAME_2 + "\"!", getMySQLConnection() == null);
          addLogEntry(e, getMySQLConnection() == null);
        }
      }
    }
    if (this.SLOW_MODE)
    {
      try
      {
        Thread.sleep(3000L);
      }
      catch (Exception localException2) {}
      if (this.botTimer == null) {
        return;
      }
    }
    if (this.BOT_CHANNEL_ID > 0)
    {
      if (this.queryLib.getCurrentQueryClientID() == -1) {
        reconnectBot(false);
      }
      try
      {
        this.queryLib.moveClient(this.queryLib.getCurrentQueryClientID(), this.BOT_CHANNEL_ID, null);
      }
      catch (Exception e)
      {
        addLogEntry((byte)3, "Unable to switch channel (own client ID: " + Integer.toString(this.queryLib.getCurrentQueryClientID()) + ")!", getMySQLConnection() == null);
        addLogEntry((byte)3, "Notice: If channel ID " + Integer.toString(this.BOT_CHANNEL_ID) + " is the default channel, use -1 in bot config!", getMySQLConnection() == null);
        addLogEntry(e, getMySQLConnection() == null);
      }
    }
    if (this.SLOW_MODE)
    {
      try
      {
        Thread.sleep(3000L);
      }
      catch (Exception localException3) {}
      if (this.botTimer == null) {
        return;
      }
    }
    this.serverInfoCache = new ServerInfoCache();
    this.queryLib.setTeamspeakActionListener(this);
    try
    {
      this.queryLib.addEventNotify(4, 0);
    }
    catch (Exception e)
    {
      StringBuffer sbNameList = new StringBuffer();
      if (this.CSVLOGGER_FILE != null) {
        sbNameList.append("Connection Log");
      }
      if ((this.botUpdateCheck == 1) || (this.botUpdateCheck == 2))
      {
        sbNameList.append("Bot Update Check");
        this.botUpdateCheck = 0;
      }
      String tempName = null;
      while (this.classList_ts3EventServer.size() > 0)
      {
        tempName = getFunctionPrefix(this.classList_ts3EventServer.elementAt(0));
        if (tempName != null)
        {
          if (sbNameList.length() > 0) {
            sbNameList.append(", ");
          }
          sbNameList.append(tempName);
        }
        unloadFunction(this.classList_ts3EventServer.elementAt(0));
      }
      addLogEntry((byte)3, "Unable to see joining clients! Following functions disabled: " + sbNameList.toString(), getMySQLConnection() == null);
      addLogEntry(e, getMySQLConnection() == null);
      
      this.CSVLOGGER_FILE = null;
      if (this.csvLogFile != null)
      {
        this.csvLogFile.close();
        this.csvLogFile = null;
      }
    }
    try
    {
      this.queryLib.addEventNotify(5, 0);
    }
    catch (Exception e)
    {
      StringBuffer sbNameList = new StringBuffer();
      String tempName = null;
      while (this.classList_ts3EventChannel.size() > 0)
      {
        tempName = getFunctionPrefix(this.classList_ts3EventChannel.elementAt(0));
        if (tempName != null)
        {
          if (sbNameList.length() > 0) {
            sbNameList.append(", ");
          }
          sbNameList.append(tempName);
        }
        unloadFunction(this.classList_ts3EventChannel.elementAt(0));
      }
      addLogEntry((byte)3, "Unable to receive channel join events! Following functions disabled: " + sbNameList.toString(), getMySQLConnection() == null);
      addLogEntry(e, getMySQLConnection() == null);
    }
    try
    {
      this.queryLib.addEventNotify(2, 0);
    }
    catch (Exception e)
    {
      addLogEntry((byte)3, "Unable to receive channel chat messages!", getMySQLConnection() == null);
      addLogEntry(e, getMySQLConnection() == null);
    }
    try
    {
      this.queryLib.addEventNotify(3, 0);
    }
    catch (Exception e)
    {
      addLogEntry((byte)3, "Unable to receive private chat messages!", getMySQLConnection() == null);
      addLogEntry(e, getMySQLConnection() == null);
    }
    if (this.SLOW_MODE)
    {
      try
      {
        Thread.sleep(3000L);
      }
      catch (Exception localException4) {}
      if (this.botTimer == null) {
        return;
      }
    }
    try
    {
      this.queryLib.addEventNotify(1, 0);
    }
    catch (Exception e)
    {
      addLogEntry((byte)3, "Unable to receive server chat messages!", getMySQLConnection() == null);
      addLogEntry(e, getMySQLConnection() == null);
    }
    if (!handleUpdateCache())
    {
      reconnectBot(false);
      return;
    }
    if (this.SLOW_MODE)
    {
      try
      {
        Thread.sleep(3000L);
      }
      catch (Exception localException5) {}
      if (this.botTimer == null) {
        return;
      }
    }
    this.clientCache = new ClientDatabaseCache(this.queryLib, this, this.CLIENT_DATABASE_CACHE);
    
    outputStartMSG();
    setListOptions();
    createListArguments();
    
    this.chatCommands = new ChatCommands(this.queryLib, this, this.clientCache, this.sdf, this.config, this.manager);
    
    this.timerCheck = new TimerTask()
    {
      public void run()
      {
        JTS3ServerMod.this.runCheck();
      }
    };
    this.botTimer.schedule(this.timerCheck, this.CHECK_INTERVAL * 1000);
    
    this.timerUpdateCache = new TimerTask()
    {
      public void run()
      {
        JTS3ServerMod.this.updateCache = true;
      }
    };
    this.botTimer.schedule(this.timerUpdateCache, 60000L, 60000L);
    
    addLogEntry((byte)1, "Bot started and connected successful, write !botinfo in server chat to get an answer!", getMySQLConnection() == null);
  }
  
  private void reconnectBot(boolean force)
  {
    if ((this.RECONNECT_FOREVER) || (force))
    {
      prepareStopBot();
      
      addLogEntry((byte)1, "Reconnecting in " + Integer.toString(this.reconnectTime / 1000) + " seconds...", getMySQLConnection() == null);
      
      this.timerReconnect = new TimerTask()
      {
        public void run()
        {
          if (JTS3ServerMod.this.botTimer != null) {
            JTS3ServerMod.this.botTimer.cancel();
          }
          JTS3ServerMod.this.botTimer = null;
          JTS3ServerMod.this.reloadState = 2;
        }
      };
      this.botTimer.schedule(this.timerReconnect, this.reconnectTime);
      return;
    }
    stopBotInstance(0);
  }
  
  private void prepareStopBot()
  {
    unloadAllFunctions();
    if (this.timerCheck != null) {
      this.timerCheck.cancel();
    }
    this.timerCheck = null;
    if (this.clientCache != null) {
      this.clientCache.stopUpdating();
    }
    this.clientCache = null;
    if (this.timerUpdateCache != null) {
      this.timerUpdateCache.cancel();
    }
    this.timerUpdateCache = null;
    try
    {
      this.queryLib.removeTeamspeakActionListener();
    }
    catch (Exception localException) {}
    this.queryLib.closeTS3Connection();
  }
  
  void stopBotInstance(int mode)
  {
    if (mode == 2) {
      addLogEntry((byte)5, "Virtual bot instance \"" + this.instanceName + "\" restarts", getMySQLConnection() == null);
    }
    prepareStopBot();
    if (this.timerReconnect != null) {
      this.timerReconnect.cancel();
    }
    this.timerReconnect = null;
    if (this.botTimer != null) {
      this.botTimer.cancel();
    }
    this.botTimer = null;
    this.reloadState = mode;
    if (mode == 0)
    {
      if (this.csvLogFile != null)
      {
        this.csvLogFile.close();
        this.csvLogFile = null;
      }
      this.manager.removeInstance(this);
    }
  }
  
  @SuppressWarnings("unused")
  private void checkTS3Clients()
  {
    if (!this.queryLib.isConnected())
    {
      String msg = "Connection to Teamspeak 3 server lost...";
      addLogEntry((byte)4, msg, true);
      
      reconnectBot(true);
      return;
    }
    try
    {
      this.clientList = this.queryLib.getList(1, this.listArguments);
    }
    catch (Exception e)
    {
      addLogEntry((byte)4, "Error while getting client list!", getMySQLConnection() == null);
      addLogEntry(e, true);
      
      reconnectBot(true);
      return;
    }
    if (this.serverInfoCache != null) {
      this.serverInfoCache.updateClientCount(this.clientList);
    }
    if (this.updateCache) {
      handleUpdateCache();
    }
    for (int i = 0; i < this.functionList_Class.size(); i++) {
      if (((Boolean)this.functionList_Enabled.elementAt(i)).booleanValue()) {
        if (HandleClientList.class.isAssignableFrom(((HandleBotEvents)this.functionList_Class.elementAt(i)).getClass())) {
          try
          {
            ((HandleClientList)this.functionList_Class.elementAt(i)).handleClientCheck(this.clientList);
          }
          catch (Exception e)
          {
            addLogEntry(e, true);
          }
        }
      }
    }
  }
  
  private boolean handleUpdateCache()
  {
    this.updateCache = false;
    try
    {
      this.channelListCache = this.queryLib.getList(2, "-flags,-secondsempty");
    }
    catch (Exception e)
    {
      addLogEntry((byte)3, "Error while getting channel list!", getMySQLConnection() == null);
      addLogEntry(e, getMySQLConnection() == null);
      return false;
    }
    for (HashMap<String, String> channel : this.channelListCache) {
      if (((String)channel.get("channel_flag_default")).equals("1")) {
        this.DEFAULT_CHANNEL_ID = Integer.parseInt((String)channel.get("cid"));
      }
    }
    try
    {
      this.serverGroupListCache = this.queryLib.getList(4);
    }
    catch (Exception e)
    {
      addLogEntry((byte)3, "Error while getting server group list!", getMySQLConnection() == null);
      addLogEntry(e, getMySQLConnection() == null);
      return false;
    }
    if (this.serverInfoCache != null) {
      try
      {
        this.serverInfoCache.updateServerInfo(this.queryLib.getInfo(11, -1), (HashMap<String, String>)this.queryLib.getList(5, "duration=1,-count").firstElement());
      }
      catch (Exception e)
      {
        addLogEntry((byte)3, "Error while getting server info!", getMySQLConnection() == null);
        addLogEntry(e, getMySQLConnection() == null);
        return false;
      }
    }
    for (int i = 0; i < this.functionList_Class.size(); i++) {
      if (((Boolean)this.functionList_Enabled.elementAt(i)).booleanValue()) {
        if (HandleBotEvents.class.isAssignableFrom(((HandleBotEvents)this.functionList_Class.elementAt(i)).getClass())) {
          try
          {
            ((HandleBotEvents)this.functionList_Class.elementAt(i)).handleAfterCacheUpdate();
          }
          catch (Exception e)
          {
            addLogEntry(e, false);
          }
        }
      }
    }
    return true;
  }
  
  public ServerInfoCache_Interface getServerInfoCache()
  {
    return this.serverInfoCache;
  }
  
  public Vector<HashMap<String, String>> getClientList()
  {
    return this.clientList;
  }
  
  public Vector<HashMap<String, String>> getChannelList()
  {
    return this.channelListCache;
  }
  
  public MySQLConnect getMySQLConnection()
  {
    return null;
  }
  
  public int getInstanceID()
  {
    return -1;
  }
  
  public int getDefaultChannelID()
  {
    return this.DEFAULT_CHANNEL_ID;
  }
  
  public ClientDatabaseCache_Interface getClientCache()
  {
    return this.clientCache;
  }
  
  public int getCheckInterval()
  {
    return this.CHECK_INTERVAL;
  }
  
  public String getStringFromTimestamp(long timestamp)
  {
    return this.sdf.format(new Date(timestamp));
  }
  
  public void addTS3ChannelEvent(Object obj)
  {
    if (obj != null) {
      this.classList_ts3EventChannel.addElement((HandleBotEvents)obj);
    }
  }
  
  public void addTS3ServerEvent(Object obj)
  {
    if (obj != null) {
      this.classList_ts3EventServer.addElement((HandleBotEvents)obj);
    }
  }
  
  public void addBotTimer(TimerTask task, long firstStart, long interval)
  {
    if (interval > 0L) {
      this.botTimer.schedule(task, firstStart, interval);
    } else {
      this.botTimer.schedule(task, firstStart);
    }
  }
  
  public boolean isGlobalMessageVarsEnabled()
  {
    return this.GLOBAL_MESSAGE_VARS;
  }
  
  public String replaceGlobalMessageVars(String message)
  {
    String newMessage = new String(message);
    if (this.serverInfoCache != null)
    {
      if (this.serverInfoCache.getServerName() != null) {
        newMessage = newMessage.replace("%SERVER_NAME%", this.serverInfoCache.getServerName());
      }
      if (this.serverInfoCache.getServerPlatform() != null) {
        newMessage = newMessage.replace("%SERVER_PLATFORM%", this.serverInfoCache.getServerPlatform());
      }
      if (this.serverInfoCache.getServerVersion() != null) {
        newMessage = newMessage.replace("%SERVER_VERSION%", getVersionString(this.serverInfoCache.getServerVersion()));
      }
      newMessage = newMessage.replace("%SERVER_UPTIME%", getDifferenceTime(this.serverInfoCache.getServerUptimeTimestamp(), System.currentTimeMillis()));
      newMessage = newMessage.replace("%SERVER_CREATED_DATE%", getStringFromTimestamp(this.serverInfoCache.getServerCreatedAt()));
      newMessage = newMessage.replace("%SERVER_UPTIME_DATE%", getStringFromTimestamp(this.serverInfoCache.getServerUptimeTimestamp()));
      newMessage = newMessage.replace("%SERVER_UPLOAD_QUOTA%", Long.toString(this.serverInfoCache.getServerUploadQuota()));
      newMessage = newMessage.replace("%SERVER_DOWNLOAD_QUOTA%", Long.toString(this.serverInfoCache.getServerDownloadQuota()));
      newMessage = newMessage.replace("%SERVER_MONTH_BYTES_UPLOADED%", getFileSizeString(this.serverInfoCache.getServerMonthBytesUploaded(), false));
      newMessage = newMessage.replace("%SERVER_MONTH_BYTES_DOWNLOADED%", getFileSizeString(this.serverInfoCache.getServerMonthBytesDownloaded(), false));
      newMessage = newMessage.replace("%SERVER_TOTAL_BYTES_UPLOADED%", getFileSizeString(this.serverInfoCache.getServerTotalBytesUploaded(), false));
      newMessage = newMessage.replace("%SERVER_TOTAL_BYTES_DOWNLOADED%", getFileSizeString(this.serverInfoCache.getServerTotalBytesDownloaded(), false));
      newMessage = newMessage.replace("%SERVER_MAX_CLIENTS%", Integer.toString(this.serverInfoCache.getServerMaxClients()));
      newMessage = newMessage.replace("%SERVER_RESERVED_SLOTS%", Integer.toString(this.serverInfoCache.getServerReservedSlots()));
      newMessage = newMessage.replace("%SERVER_CHANNEL_COUNT%", Integer.toString(this.serverInfoCache.getServerChannelCount()));
      newMessage = newMessage.replace("%SERVER_CLIENT_COUNT%", Integer.toString(this.serverInfoCache.getServerClientCount()));
      newMessage = newMessage.replace("%SERVER_CLIENT_CONNECTIONS_COUNT%", Long.toString(this.serverInfoCache.getServerClientConnectionsCount()));
      newMessage = newMessage.replace("%SERVER_CLIENT_DB_COUNT%", Integer.toString(this.serverInfoCache.getServerClientDBCount()));
    }
    return newMessage;
  }
  
  private Vector<String> splitMessage(String configPrefix, String message)
  {
    Vector<String> messages = new Vector<String>();
    int pos = -3;
    int pos2 = -1;
    try
    {
      do
      {
        pos2 = message.indexOf("�+�", pos + 3);
        if (pos2 > 0) {
          messages.addElement(message.substring(pos + 3, pos2));
        } else {
          messages.addElement(message.substring(pos + 3));
        }
        pos = pos2;
      } while (pos2 != -1);
    }
    catch (Exception e)
    {
      messages.clear();
      messages.addElement(message);
      addLogEntry(configPrefix, e, false);
    }
    return messages;
  }
  
  public boolean sendMessageToClient(String configPrefix, String mode, int clientID, String message)
  {
    if (mode == null) {
      return false;
    }
    if (message == null) {
      return false;
    }
    if (this.GLOBAL_MESSAGE_VARS) {
      message = replaceGlobalMessageVars(message);
    }
    boolean retValue = false;
    
    Vector<String> messages = splitMessage(configPrefix, message);
    if (mode.equalsIgnoreCase("poke")) {
      try
      {
        for (int i = 0; i < messages.size(); i++) {
          this.queryLib.pokeClient(clientID, (String)messages.elementAt(i));
        }
        retValue = true;
      }
      catch (Exception e)
      {
        addLogEntry(configPrefix, (byte)3, "Error while poking Client ID: " + Integer.toString(clientID), false);
        addLogEntry(configPrefix, e, false);
      }
    } else if (mode.equalsIgnoreCase("chat")) {
      try
      {
        for (int i = 0; i < messages.size(); i++) {
          this.queryLib.sendTextMessage(clientID, 1, (String)messages.elementAt(i));
        }
        retValue = true;
      }
      catch (Exception e)
      {
        addLogEntry(configPrefix, (byte)3, "Error while sending chat message to Client ID: " + Integer.toString(clientID), false);
        addLogEntry(configPrefix, e, false);
      }
    }
    return retValue;
  }
  
  public int getUTF8Length(CharSequence sequence)
  {
    int count = 0;
    int i = 0;
    for (int len = sequence.length(); i < len; i++)
    {
      char ch = sequence.charAt(i);
      if (ch <= '')
      {
        count++;
      }
      else if (ch <= '?')
      {
        count += 2;
      }
      else if (Character.isHighSurrogate(ch))
      {
        count += 4;
        i++;
      }
      else
      {
        count += 3;
      }
    }
    return count;
  }
  
  public short getMaxMessageLength(String type)
  {
    if (type == null) {
      return Short.MAX_VALUE;
    }
    if (type.equalsIgnoreCase("chat")) {
      return 1024;
    }
    if (type.equalsIgnoreCase("poke")) {
      return 100;
    }
    if (type.equalsIgnoreCase("kick")) {
      return 80;
    }
    return Short.MAX_VALUE;
  }
  
  public boolean isMessageLengthValid(String type, String message)
  {
    Vector<String> messages = splitMessage(null, message);
    
    boolean chatMessage = type.equalsIgnoreCase("chat");
    for (int i = 0; i < messages.size(); i++) {
      if ((chatMessage ? getUTF8Length((CharSequence)messages.elementAt(i)) : ((String)messages.elementAt(i)).length()) > getMaxMessageLength(type)) {
        return false;
      }
    }
    return true;
  }
  
  public long getIdleTime(HashMap<String, String> clientInfo, int ignoreInChannel)
  {
    long currentIdleTime = Long.MAX_VALUE;
    try
    {
      currentIdleTime = Long.parseLong((String)clientInfo.get("client_idle_time"));
    }
    catch (NumberFormatException nfe)
    {
      if (!((String)clientInfo.get("cid")).equals(Integer.toString(ignoreInChannel))) {
        addLogEntry((byte)2, "TS3 Server sends wrong client_idle_time for client " + (String)clientInfo.get("client_nickname") + " (id: " + (String)clientInfo.get("clid") + ")", false);
      }
    }
    return currentIdleTime;
  }
  
  public boolean isIDListed(int searchID, Vector<Integer> list)
  {
    for (Iterator<Integer> localIterator = list.iterator(); localIterator.hasNext();)
    {
      int listID = ((Integer)localIterator.next()).intValue();
      if (searchID == listID) {
        return true;
      }
    }
    return false;
  }
  
  public String getServerGroupName(int groupID)
  {
    if ((this.serverGroupListCache == null) || (groupID < 0)) {
      return null;
    }
    for (HashMap<String, String> serverGroupInfo : this.serverGroupListCache) {
      if (((String)serverGroupInfo.get("sgid")).equals(Integer.toString(groupID))) {
        return (String)serverGroupInfo.get("name");
      }
    }
    return null;
  }
  
  public int getServerGroupType(int groupID)
  {
    if ((this.serverGroupListCache == null) || (groupID < 0)) {
      return -1;
    }
    for (HashMap<String, String> serverGroupInfo : this.serverGroupListCache) {
      if (((String)serverGroupInfo.get("sgid")).equals(Integer.toString(groupID))) {
        return Integer.parseInt((String)serverGroupInfo.get("type"));
      }
    }
    return -1;
  }
  
  @Override
  public int getListedGroup(String groupIDs, Vector<Integer> list) {
      StringTokenizer groupTokenizer = new StringTokenizer(groupIDs, ",", false);
      while (groupTokenizer.hasMoreTokens()) {
          int groupID = Integer.parseInt(groupTokenizer.nextToken());
          Iterator<Integer> iterator = list.iterator();
          while (iterator.hasNext()) {
              int gID = iterator.next();
              if (groupID != gID) continue;
              return groupID;
          }
      }
      return -1;
  }
  
  @Override
  public boolean isGroupListed(String groupIDs, Vector<Integer> list) {
      StringTokenizer groupTokenizer = new StringTokenizer(groupIDs, ",", false);
      while (groupTokenizer.hasMoreTokens()) {
          int groupID = Integer.parseInt(groupTokenizer.nextToken());
          Iterator<Integer> iterator = list.iterator();
          while (iterator.hasNext()) {
              int gID = iterator.next();
              if (groupID != gID) continue;
              return true;
          }
      }
      return false;
  }
  
  boolean isGroupListed(String groupIDs, int searchGroupID)
  {
    StringTokenizer groupTokenizer = new StringTokenizer(groupIDs, ",", false);
    while (groupTokenizer.hasMoreTokens())
    {
      int groupID = Integer.parseInt(groupTokenizer.nextToken());
      if (groupID == searchGroupID) {
        return true;
      }
    }
    return false;
  }
  
  private void handleChatMessage(HashMap<String, String> eventInfo)
  {
    if (Integer.parseInt((String)eventInfo.get("invokerid")) == this.queryLib.getCurrentQueryClientID()) {
      return;
    }
    String msg = ((String)eventInfo.get("msg")).trim();
    msg = msg.replace('�', ' ');
    if (!msg.startsWith("!")) {
      return;
    }
    boolean isFullAdmin = false;
    for (String fulladminUID : this.FULL_ADMIN_UID_LIST) {
      if (fulladminUID.equals(eventInfo.get("invokeruid")))
      {
        isFullAdmin = true;
        break;
      }
    }
    boolean isAdmin = false;
    if (!isFullAdmin) {
      for (String adminUID : this.ADMIN_UID_LIST) {
        if (adminUID.equals(eventInfo.get("invokeruid")))
        {
          isAdmin = true;
          break;
        }
      }
    }
    if ((msg.toLowerCase().equalsIgnoreCase("!botquit")) || (msg.equalsIgnoreCase("!quit")) || (msg.equalsIgnoreCase("!exit"))) {
      this.chatCommands.handleBotQuit(msg, eventInfo, this.instanceName, isFullAdmin);
    } else if (msg.toLowerCase().startsWith("!botinstancestop")) {
      this.chatCommands.handleBotInstanceStop(msg, eventInfo, isFullAdmin, this.instanceName);
    } else if (msg.toLowerCase().startsWith("!botinstancestart")) {
      this.chatCommands.handleBotInstanceStart(msg, eventInfo, isFullAdmin, this.instanceName);
    } else if (msg.equalsIgnoreCase("!botinstancelist")) {
      this.chatCommands.handleBotInstanceList(msg, eventInfo, isFullAdmin);
    } else if (msg.equalsIgnoreCase("!botinstancelistreload")) {
      this.chatCommands.handleBotInstanceListReload(msg, eventInfo, isFullAdmin);
    } else if ((msg.equalsIgnoreCase("!botreload")) || (msg.equalsIgnoreCase("!reconnect"))) {
      this.chatCommands.handleBotReload(msg, eventInfo, isFullAdmin, isAdmin, this.CONFIG_FILE_NAME);
    } else if (msg.equalsIgnoreCase("!botreloadall")) {
      this.chatCommands.handleBotReloadAll(msg, eventInfo, isFullAdmin);
    } else if ((msg.equalsIgnoreCase("!botversion")) || (msg.equalsIgnoreCase("!botversioncheck")) || (msg.equalsIgnoreCase("!version"))) {
      this.chatCommands.handleBotVersionCheck(msg, eventInfo, isFullAdmin, isAdmin);
    } else if ((msg.equalsIgnoreCase("!botfunctionlist")) || (msg.equalsIgnoreCase("!functionlist"))) {
      this.chatCommands.handleBotFunctionList(msg, eventInfo, isFullAdmin, isAdmin);
    } else if ((msg.toLowerCase().startsWith("!botfunctionactivate")) || (msg.toLowerCase().startsWith("!functionon"))) {
      this.chatCommands.handleBotFunctionActivate(msg, eventInfo, isFullAdmin, isAdmin);
    } else if ((msg.toLowerCase().startsWith("!botfunctiondisable")) || (msg.toLowerCase().startsWith("!functionoff"))) {
      this.chatCommands.handleBotFunctionDisable(msg, eventInfo, isFullAdmin, isAdmin);
    } else if (msg.toLowerCase().startsWith("!botcfgreload")) {
      this.chatCommands.handleBotCfgReload(msg, eventInfo, isFullAdmin, isAdmin);
    } else if (msg.toLowerCase().startsWith("!botcfghelp")) {
      this.chatCommands.handleBotCfgHelp(msg, eventInfo, isFullAdmin, isAdmin);
    } else if (msg.toLowerCase().startsWith("!botcfgget")) {
      this.chatCommands.handleBotCfgGet(msg, eventInfo, isFullAdmin, isAdmin);
    } else if (msg.toLowerCase().startsWith("!botcfgset")) {
      this.chatCommands.handleBotCfgSet(msg, eventInfo, isFullAdmin, isAdmin);
    } else if (msg.equalsIgnoreCase("!botcfgcheck")) {
      this.chatCommands.handleBotCfgCheck(msg, eventInfo, isFullAdmin, isAdmin, this.CONFIG_FILE_NAME);
    } else if (msg.equalsIgnoreCase("!botcfgsave")) {
      this.chatCommands.handleBotCfgSave(msg, eventInfo, isFullAdmin, isAdmin, this.CONFIG_FILE_NAME);
    } else if ((msg.toLowerCase().startsWith("!clientsearch")) || (msg.toLowerCase().startsWith("!clients")) || (msg.toLowerCase().startsWith("!clientlist"))) {
      this.chatCommands.handleClientSearch(msg, eventInfo, isFullAdmin, isAdmin);
    } else if (msg.toLowerCase().startsWith("!searchip")) {
      this.chatCommands.handleSearchIP(msg, eventInfo, isFullAdmin, isAdmin);
    } else if ((msg.toLowerCase().startsWith("!listinactiveclients")) || (msg.toLowerCase().startsWith("!inactiveclients"))) {
      this.chatCommands.handleListInactiveClients(msg, eventInfo, isFullAdmin, isAdmin);
    } else if ((msg.toLowerCase().startsWith("!listinactivechannels")) || (msg.toLowerCase().startsWith("!emptychannels"))) {
      this.chatCommands.handleListInactiveChannels(msg, eventInfo, isFullAdmin, isAdmin);
    } else if (msg.toLowerCase().startsWith("!setchannelgroup")) {
      this.chatCommands.handleSetChannelGroup(msg, eventInfo, isFullAdmin, isAdmin);
    } else if (msg.toLowerCase().startsWith("!removeservergroups")) {
      this.chatCommands.handleRemoveServerGroups(msg, eventInfo, isFullAdmin, isAdmin);
    } else if (msg.toLowerCase().startsWith("!removechannelgroups")) {
      this.chatCommands.handleRemoveChannelGroups(msg, eventInfo, isFullAdmin, isAdmin);
    } else if (msg.toLowerCase().startsWith("!msgchannelgroup")) {
      this.chatCommands.handleMsgChannelGroup(msg, eventInfo, isFullAdmin, isAdmin);
    } else if (msg.toLowerCase().startsWith("!msgservergroup")) {
      this.chatCommands.handleMsgServerGroup(msg, eventInfo, isFullAdmin, isAdmin);
    } else if ((msg.toLowerCase().startsWith("!botjoinchannel")) || (msg.toLowerCase().startsWith("!joinchannel"))) {
      this.chatCommands.handleBotJoinChannel(msg, eventInfo, isFullAdmin, isAdmin);
    } else if (msg.toLowerCase().startsWith("!botrename")) {
      this.chatCommands.handleBotRename(msg, eventInfo, isFullAdmin, isAdmin);
    } else if ((msg.toLowerCase().startsWith("!setchannelname")) || (msg.toLowerCase().startsWith("!renamechannel"))) {
      this.chatCommands.handleSetChannelName(msg, eventInfo, isFullAdmin, isAdmin);
    } else if ((msg.toLowerCase().startsWith("!getchannelid")) || (msg.toLowerCase().startsWith("!channellist"))) {
      this.chatCommands.handleGetChannelID(msg, eventInfo, isFullAdmin, isAdmin);
    } else if (msg.toLowerCase().startsWith("!exec")) {
      this.chatCommands.handleExec(msg, eventInfo, isFullAdmin);
    } else if ((msg.equalsIgnoreCase("!botinfo")) || (msg.equalsIgnoreCase("!info"))) {
      this.chatCommands.handleBotInfo(msg, eventInfo, this.startTime, isFullAdmin, isAdmin);
    } else if ((msg.toLowerCase().startsWith("!bothelp")) || (msg.toLowerCase().startsWith("!h"))) {
      this.chatCommands.handleBotHelp(msg, eventInfo, isFullAdmin, isAdmin);
    }
    for (int i = 0; i < this.functionList_Class.size(); i++) {
      if (((Boolean)this.functionList_Enabled.elementAt(i)).booleanValue()) {
        if (HandleTS3Events.class.isAssignableFrom(((HandleBotEvents)this.functionList_Class.elementAt(i)).getClass())) {
          try
          {
            String sPrefix = "!" + ((String)this.functionList_Prefix.elementAt(i)).toLowerCase();
            if ((msg.toLowerCase().startsWith(sPrefix + " ")) || (msg.toLowerCase().equals(sPrefix)))
            {
              addLogEntry((byte)1, "Got command from " + (String)eventInfo.get("invokername") + ": " + msg, false);
              if (((HandleTS3Events)this.functionList_Class.elementAt(i)).handleChatCommands(msg.length() > sPrefix.length() ? msg.substring(sPrefix.length() + 1) : "", eventInfo, isFullAdmin, isAdmin)) {
                break;
              }
              this.queryLib.sendTextMessage(Integer.parseInt((String)eventInfo.get("invokerid")), 1, "No such command: [b]" + msg + "[/b]");
            }
          }
          catch (Exception e)
          {
            addLogEntry(e, true);
          }
        }
      }
    }
  }
  
  private void handleCSVLog(HashMap<String, String> eventInfo)
  {
    if (this.CSVLOGGER_FILE == null) {
      return;
    }
    try
    {
      if (this.csvLogFile == null) {
        this.csvLogFile = new PrintStream(new FileOutputStream(this.CSVLOGGER_FILE, true), false, "UTF-8");
      }
      this.csvLogFile.print(this.sdfDebug.format(new Date(System.currentTimeMillis())));
      this.csvLogFile.print(";");
      this.csvLogFile.print((String)eventInfo.get("client_unique_identifier"));
      this.csvLogFile.print(";");
      this.csvLogFile.print(eventInfo.get("connection_client_ip") != null ? (String)eventInfo.get("connection_client_ip") : "***");
      this.csvLogFile.print(";");
      this.csvLogFile.println((String)eventInfo.get("client_nickname"));
      
      this.csvLogFile.flush();
    }
    catch (Exception e)
    {
      addLogEntry(e, false);
    }
  }
  
  private void handleUpdateCheck(HashMap<String, String> eventInfo)
  {
    if ((this.botUpdateCheck != 1) && (this.botUpdateCheck != 2)) {
      return;
    }
    boolean isFullAdmin = false;
    for (String fulladminUID : this.FULL_ADMIN_UID_LIST) {
      if (fulladminUID.equals(eventInfo.get("client_unique_identifier")))
      {
        isFullAdmin = true;
        break;
      }
    }
    if (!isFullAdmin) {
      return;
    }
    StringBuffer versionInfo = new StringBuffer();
    try
    {
    	HashMap<String, String> versionData = getVersionCheckData();
      if (versionData != null)
      {
        long devBuild = versionData.get("dev.build") == null ? 0L : Long.parseLong(versionData.get("dev.build"));
        long finalBuild = versionData.get("final.build") == null ? 0L : Long.parseLong(versionData.get("final.build"));
        if (5504L < finalBuild) {
          if ((versionData.get("final.version") != null) && (versionData.get("final.url") != null)) {
            versionInfo.append("\n[b]Latest final version:[/b] " + versionData.get("final.version") + " [" + versionData.get("final.build") + "]" + " - [url=" + versionData.get("final.url") + "]Download[/url]");
          }
        }
        if ((this.botUpdateCheck == 2) && (5504L < devBuild)) {
          if ((versionData.get("dev.version") != null) && (versionData.get("dev.url") != null)) {
            versionInfo.append("\n[b]Latest development version:[/b] " + versionData.get("dev.version") + " [" + versionData.get("dev.build") + "]" + " - [url=" + versionData.get("dev.url") + "]Download[/url]");
          }
        }
      }
    }
    catch (Exception e)
    {
      addLogEntry(e, false);
    }
    if (versionInfo.length() > 0) {
      try
      {
        this.queryLib.sendTextMessage(Integer.parseInt((String)eventInfo.get("clid")), 1, "New JTS3ServerMod version is available!\n[b]Current installed version:[/b] 5.5.4 (11.07.2015) [5504]" + 
          versionInfo.toString());
      }
      catch (Exception e)
      {
        addLogEntry(e, false);
      }
    }
  }
  
  public String getVersionString(String version)
  {
    String searchString = " [Build: ";
    int pos1 = version.indexOf(searchString);
    int pos2 = version.indexOf("]", pos1 + searchString.length());
    try
    {
      long lTime = Long.parseLong(version.substring(pos1 + searchString.length(), pos2)) * 1000L;
      return version.substring(0, pos1) + " (" + this.sdf.format(new Date(lTime)) + ")";
    }
    catch (Exception e) {}
    return version;
  }
  
  public String getFileSizeString(long size, boolean base1000)
  {
    int base = base1000 ? 1000 : 1024;
    
    NumberFormat nf = NumberFormat.getInstance();
    nf.setMinimumFractionDigits(1);
    nf.setMaximumFractionDigits(2);
    String retValue;
    if (size > base * base * base)
    {
      double value = size / base / base / base;
      retValue = nf.format(value) + " " + (base1000 ? "GB" : "GiB");
    }
    else
    {
      if (size > base * base)
      {
        double value = size / base / base;
        retValue = nf.format(value) + " " + (base1000 ? "MB" : "MiB");
      }
      else
      {
        if (size > base)
        {
          double value = size / base;
          retValue = nf.format(value) + " " + (base1000 ? "kB" : "KiB");
        }
        else
        {
          retValue = size + " byte";
        }
      }
    }
    return retValue;
  }
  
  public void teamspeakActionPerformed(String eventType, HashMap<String, String> eventInfo)
  {
    try
    {
      if (eventType.equals("notifytextmessage"))
      {
        handleChatMessage(eventInfo);
      }
      else
      {
        if ((eventType + eventInfo.toString()).equals(this.lastActionString)) {
          return;
        }
        this.lastActionString = (eventType + eventInfo.toString());
        if (eventType.equals("notifyclientleftview")) {
          if (this.clientCache != null)
          {
            Vector<HashMap<String, String>> clientListCache = this.clientList;
            for (HashMap<String, String> clientInfo : clientListCache) {
              if (((String)clientInfo.get("clid")).equals(eventInfo.get("clid"))) {
                if (Integer.parseInt((String)clientInfo.get("client_type")) == 0)
                {
                  this.clientCache.updateSingleClient(clientInfo);
                  break;
                }
              }
            }
          }
        }
        if (eventType.equals("notifycliententerview")) {
          if (Integer.parseInt((String)eventInfo.get("client_type")) == 0)
          {
            HashMap<String, String> clientInfo = this.queryLib.getInfo(13, Integer.parseInt((String)eventInfo.get("clid")));
            if (clientInfo != null) {
              eventInfo.put("connection_client_ip", (String)clientInfo.get("connection_client_ip"));
            }
            handleCSVLog(eventInfo);
            if (this.clientCache != null) {
              this.clientCache.updateSingleClient(eventInfo);
            }
            handleUpdateCheck(eventInfo);
          }
        }
        for (int i = 0; i < this.functionList_Class.size(); i++) {
          if (((Boolean)this.functionList_Enabled.elementAt(i)).booleanValue()) {
            if (HandleTS3Events.class.isAssignableFrom(((HandleBotEvents)this.functionList_Class.elementAt(i)).getClass())) {
              try
              {
                ((HandleTS3Events)this.functionList_Class.elementAt(i)).handleClientEvents(eventType, eventInfo);
              }
              catch (Exception e)
              {
                addLogEntry(e, true);
              }
            }
          }
        }
      }
    }
    catch (Throwable e)
    {
      addLogEntry(e, false);
    }
  }
  
  /* Error */
  private void runCheck()
  {
    // Byte code:
    //   0: aload_0
    //   1: invokespecial 2001	de/stefan1200/jts3servermod/JTS3ServerMod:checkTS3Clients	()V
    //   4: goto +331 -> 335
    //   7: astore_1
    //   8: aload_0
    //   9: iconst_2
    //   10: ldc_w 2003
    //   13: iconst_1
    //   14: invokevirtual 321	de/stefan1200/jts3servermod/JTS3ServerMod:addLogEntry	(BLjava/lang/String;Z)V
    //   17: aload_0
    //   18: aload_1
    //   19: iconst_0
    //   20: invokevirtual 826	de/stefan1200/jts3servermod/JTS3ServerMod:addLogEntry	(Ljava/lang/Throwable;Z)V
    //   23: aload_0
    //   24: getfield 1304	de/stefan1200/jts3servermod/JTS3ServerMod:timerCheck	Ljava/util/TimerTask;
    //   27: ifnull +347 -> 374
    //   30: aload_0
    //   31: new 2005	de/stefan1200/jts3servermod/JTS3ServerMod$5
    //   34: dup
    //   35: aload_0
    //   36: invokespecial 2007	de/stefan1200/jts3servermod/JTS3ServerMod$5:<init>	(Lde/stefan1200/jts3servermod/JTS3ServerMod;)V
    //   39: putfield 1304	de/stefan1200/jts3servermod/JTS3ServerMod:timerCheck	Ljava/util/TimerTask;
    //   42: aload_0
    //   43: getfield 732	de/stefan1200/jts3servermod/JTS3ServerMod:botTimer	Ljava/util/Timer;
    //   46: aload_0
    //   47: getfield 1304	de/stefan1200/jts3servermod/JTS3ServerMod:timerCheck	Ljava/util/TimerTask;
    //   50: aload_0
    //   51: getfield 716	de/stefan1200/jts3servermod/JTS3ServerMod:CHECK_INTERVAL	I
    //   54: sipush 1000
    //   57: imul
    //   58: i2l
    //   59: invokevirtual 1306	java/util/Timer:schedule	(Ljava/util/TimerTask;J)V
    //   62: goto +312 -> 374
    //   65: astore_1
    //   66: aload_0
    //   67: iconst_4
    //   68: ldc_w 2008
    //   71: iconst_1
    //   72: invokevirtual 321	de/stefan1200/jts3servermod/JTS3ServerMod:addLogEntry	(BLjava/lang/String;Z)V
    //   75: aload_0
    //   76: iconst_4
    //   77: ldc_w 2010
    //   80: iconst_1
    //   81: invokevirtual 321	de/stefan1200/jts3servermod/JTS3ServerMod:addLogEntry	(BLjava/lang/String;Z)V
    //   84: aload_0
    //   85: aload_1
    //   86: iconst_0
    //   87: invokevirtual 826	de/stefan1200/jts3servermod/JTS3ServerMod:addLogEntry	(Ljava/lang/Throwable;Z)V
    //   90: aload_0
    //   91: getfield 263	de/stefan1200/jts3servermod/JTS3ServerMod:manager	Lde/stefan1200/jts3servermod/InstanceManager;
    //   94: ldc -126
    //   96: new 274	java/lang/StringBuilder
    //   99: dup
    //   100: ldc_w 2012
    //   103: invokespecial 280	java/lang/StringBuilder:<init>	(Ljava/lang/String;)V
    //   106: aload_0
    //   107: getfield 265	de/stefan1200/jts3servermod/JTS3ServerMod:instanceName	Ljava/lang/String;
    //   110: invokevirtual 283	java/lang/StringBuilder:append	(Ljava/lang/String;)Ljava/lang/StringBuilder;
    //   113: invokevirtual 287	java/lang/StringBuilder:toString	()Ljava/lang/String;
    //   116: invokevirtual 2014	de/stefan1200/jts3servermod/InstanceManager:stopAllInstances	(Ljava/lang/String;Ljava/lang/String;)V
    //   119: aload_0
    //   120: getfield 1304	de/stefan1200/jts3servermod/JTS3ServerMod:timerCheck	Ljava/util/TimerTask;
    //   123: ifnull +35 -> 158
    //   126: aload_0
    //   127: new 2005	de/stefan1200/jts3servermod/JTS3ServerMod$5
    //   130: dup
    //   131: aload_0
    //   132: invokespecial 2007	de/stefan1200/jts3servermod/JTS3ServerMod$5:<init>	(Lde/stefan1200/jts3servermod/JTS3ServerMod;)V
    //   135: putfield 1304	de/stefan1200/jts3servermod/JTS3ServerMod:timerCheck	Ljava/util/TimerTask;
    //   138: aload_0
    //   139: getfield 732	de/stefan1200/jts3servermod/JTS3ServerMod:botTimer	Ljava/util/Timer;
    //   142: aload_0
    //   143: getfield 1304	de/stefan1200/jts3servermod/JTS3ServerMod:timerCheck	Ljava/util/TimerTask;
    //   146: aload_0
    //   147: getfield 716	de/stefan1200/jts3servermod/JTS3ServerMod:CHECK_INTERVAL	I
    //   150: sipush 1000
    //   153: imul
    //   154: i2l
    //   155: invokevirtual 1306	java/util/Timer:schedule	(Ljava/util/TimerTask;J)V
    //   158: return
    //   159: astore_1
    //   160: aload_0
    //   161: iconst_4
    //   162: ldc_w 2017
    //   165: iconst_1
    //   166: invokevirtual 321	de/stefan1200/jts3servermod/JTS3ServerMod:addLogEntry	(BLjava/lang/String;Z)V
    //   169: aload_0
    //   170: aload_1
    //   171: iconst_0
    //   172: invokevirtual 826	de/stefan1200/jts3servermod/JTS3ServerMod:addLogEntry	(Ljava/lang/Throwable;Z)V
    //   175: aload_0
    //   176: getfield 263	de/stefan1200/jts3servermod/JTS3ServerMod:manager	Lde/stefan1200/jts3servermod/InstanceManager;
    //   179: ldc -126
    //   181: new 274	java/lang/StringBuilder
    //   184: dup
    //   185: ldc_w 2012
    //   188: invokespecial 280	java/lang/StringBuilder:<init>	(Ljava/lang/String;)V
    //   191: aload_0
    //   192: getfield 265	de/stefan1200/jts3servermod/JTS3ServerMod:instanceName	Ljava/lang/String;
    //   195: invokevirtual 283	java/lang/StringBuilder:append	(Ljava/lang/String;)Ljava/lang/StringBuilder;
    //   198: invokevirtual 287	java/lang/StringBuilder:toString	()Ljava/lang/String;
    //   201: invokevirtual 2014	de/stefan1200/jts3servermod/InstanceManager:stopAllInstances	(Ljava/lang/String;Ljava/lang/String;)V
    //   204: aload_0
    //   205: getfield 1304	de/stefan1200/jts3servermod/JTS3ServerMod:timerCheck	Ljava/util/TimerTask;
    //   208: ifnull +35 -> 243
    //   211: aload_0
    //   212: new 2005	de/stefan1200/jts3servermod/JTS3ServerMod$5
    //   215: dup
    //   216: aload_0
    //   217: invokespecial 2007	de/stefan1200/jts3servermod/JTS3ServerMod$5:<init>	(Lde/stefan1200/jts3servermod/JTS3ServerMod;)V
    //   220: putfield 1304	de/stefan1200/jts3servermod/JTS3ServerMod:timerCheck	Ljava/util/TimerTask;
    //   223: aload_0
    //   224: getfield 732	de/stefan1200/jts3servermod/JTS3ServerMod:botTimer	Ljava/util/Timer;
    //   227: aload_0
    //   228: getfield 1304	de/stefan1200/jts3servermod/JTS3ServerMod:timerCheck	Ljava/util/TimerTask;
    //   231: aload_0
    //   232: getfield 716	de/stefan1200/jts3servermod/JTS3ServerMod:CHECK_INTERVAL	I
    //   235: sipush 1000
    //   238: imul
    //   239: i2l
    //   240: invokevirtual 1306	java/util/Timer:schedule	(Ljava/util/TimerTask;J)V
    //   243: return
    //   244: astore_1
    //   245: aload_0
    //   246: aload_1
    //   247: iconst_0
    //   248: invokevirtual 826	de/stefan1200/jts3servermod/JTS3ServerMod:addLogEntry	(Ljava/lang/Throwable;Z)V
    //   251: aload_0
    //   252: getfield 1304	de/stefan1200/jts3servermod/JTS3ServerMod:timerCheck	Ljava/util/TimerTask;
    //   255: ifnull +119 -> 374
    //   258: aload_0
    //   259: new 2005	de/stefan1200/jts3servermod/JTS3ServerMod$5
    //   262: dup
    //   263: aload_0
    //   264: invokespecial 2007	de/stefan1200/jts3servermod/JTS3ServerMod$5:<init>	(Lde/stefan1200/jts3servermod/JTS3ServerMod;)V
    //   267: putfield 1304	de/stefan1200/jts3servermod/JTS3ServerMod:timerCheck	Ljava/util/TimerTask;
    //   270: aload_0
    //   271: getfield 732	de/stefan1200/jts3servermod/JTS3ServerMod:botTimer	Ljava/util/Timer;
    //   274: aload_0
    //   275: getfield 1304	de/stefan1200/jts3servermod/JTS3ServerMod:timerCheck	Ljava/util/TimerTask;
    //   278: aload_0
    //   279: getfield 716	de/stefan1200/jts3servermod/JTS3ServerMod:CHECK_INTERVAL	I
    //   282: sipush 1000
    //   285: imul
    //   286: i2l
    //   287: invokevirtual 1306	java/util/Timer:schedule	(Ljava/util/TimerTask;J)V
    //   290: goto +84 -> 374
    //   293: astore_2
    //   294: aload_0
    //   295: getfield 1304	de/stefan1200/jts3servermod/JTS3ServerMod:timerCheck	Ljava/util/TimerTask;
    //   298: ifnull +35 -> 333
    //   301: aload_0
    //   302: new 2005	de/stefan1200/jts3servermod/JTS3ServerMod$5
    //   305: dup
    //   306: aload_0
    //   307: invokespecial 2007	de/stefan1200/jts3servermod/JTS3ServerMod$5:<init>	(Lde/stefan1200/jts3servermod/JTS3ServerMod;)V
    //   310: putfield 1304	de/stefan1200/jts3servermod/JTS3ServerMod:timerCheck	Ljava/util/TimerTask;
    //   313: aload_0
    //   314: getfield 732	de/stefan1200/jts3servermod/JTS3ServerMod:botTimer	Ljava/util/Timer;
    //   317: aload_0
    //   318: getfield 1304	de/stefan1200/jts3servermod/JTS3ServerMod:timerCheck	Ljava/util/TimerTask;
    //   321: aload_0
    //   322: getfield 716	de/stefan1200/jts3servermod/JTS3ServerMod:CHECK_INTERVAL	I
    //   325: sipush 1000
    //   328: imul
    //   329: i2l
    //   330: invokevirtual 1306	java/util/Timer:schedule	(Ljava/util/TimerTask;J)V
    //   333: aload_2
    //   334: athrow
    //   335: aload_0
    //   336: getfield 1304	de/stefan1200/jts3servermod/JTS3ServerMod:timerCheck	Ljava/util/TimerTask;
    //   339: ifnull +35 -> 374
    //   342: aload_0
    //   343: new 2005	de/stefan1200/jts3servermod/JTS3ServerMod$5
    //   346: dup
    //   347: aload_0
    //   348: invokespecial 2007	de/stefan1200/jts3servermod/JTS3ServerMod$5:<init>	(Lde/stefan1200/jts3servermod/JTS3ServerMod;)V
    //   351: putfield 1304	de/stefan1200/jts3servermod/JTS3ServerMod:timerCheck	Ljava/util/TimerTask;
    //   354: aload_0
    //   355: getfield 732	de/stefan1200/jts3servermod/JTS3ServerMod:botTimer	Ljava/util/Timer;
    //   358: aload_0
    //   359: getfield 1304	de/stefan1200/jts3servermod/JTS3ServerMod:timerCheck	Ljava/util/TimerTask;
    //   362: aload_0
    //   363: getfield 716	de/stefan1200/jts3servermod/JTS3ServerMod:CHECK_INTERVAL	I
    //   366: sipush 1000
    //   369: imul
    //   370: i2l
    //   371: invokevirtual 1306	java/util/Timer:schedule	(Ljava/util/TimerTask;J)V
    //   374: return
    // Line number table:
    //   Java source line #2677	-> byte code offset #0
    //   Java source line #2678	-> byte code offset #4
    //   Java source line #2679	-> byte code offset #7
    //   Java source line #2681	-> byte code offset #8
    //   Java source line #2682	-> byte code offset #17
    //   Java source line #2705	-> byte code offset #23
    //   Java source line #2707	-> byte code offset #30
    //   Java source line #2714	-> byte code offset #42
    //   Java source line #2684	-> byte code offset #65
    //   Java source line #2686	-> byte code offset #66
    //   Java source line #2687	-> byte code offset #75
    //   Java source line #2688	-> byte code offset #84
    //   Java source line #2689	-> byte code offset #90
    //   Java source line #2705	-> byte code offset #119
    //   Java source line #2707	-> byte code offset #126
    //   Java source line #2714	-> byte code offset #138
    //   Java source line #2690	-> byte code offset #158
    //   Java source line #2692	-> byte code offset #159
    //   Java source line #2694	-> byte code offset #160
    //   Java source line #2695	-> byte code offset #169
    //   Java source line #2696	-> byte code offset #175
    //   Java source line #2705	-> byte code offset #204
    //   Java source line #2707	-> byte code offset #211
    //   Java source line #2714	-> byte code offset #223
    //   Java source line #2697	-> byte code offset #243
    //   Java source line #2699	-> byte code offset #244
    //   Java source line #2701	-> byte code offset #245
    //   Java source line #2705	-> byte code offset #251
    //   Java source line #2707	-> byte code offset #258
    //   Java source line #2714	-> byte code offset #270
    //   Java source line #2704	-> byte code offset #293
    //   Java source line #2705	-> byte code offset #294
    //   Java source line #2707	-> byte code offset #301
    //   Java source line #2714	-> byte code offset #313
    //   Java source line #2716	-> byte code offset #333
    //   Java source line #2705	-> byte code offset #335
    //   Java source line #2707	-> byte code offset #342
    //   Java source line #2714	-> byte code offset #354
    //   Java source line #2717	-> byte code offset #374
    // Local variable table:
    //   start	length	slot	name	signature
    //   0	375	0	this	JTS3ServerMod
    //   7	12	1	e0	NullPointerException
    //   65	21	1	ome	OutOfMemoryError
    //   159	12	1	vme	VirtualMachineError
    //   244	3	1	ex	Throwable
    //   293	41	2	localObject	Object
    // Exception table:
    //   from	to	target	type
    //   0	4	7	java/lang/NullPointerException
    //   0	4	65	java/lang/OutOfMemoryError
    //   0	4	159	java/lang/VirtualMachineError
    //   0	4	244	java/lang/Throwable
    //   0	23	293	finally
    //   65	119	293	finally
    //   159	204	293	finally
    //   244	251	293	finally
  }
  
  public String getDifferenceTime(long from, long to)
  {
    long difference = to - from;
    int days = (int)(difference / 86400000L);
    int hours = (int)(difference / 3600000L % 24L);
    int minutes = (int)(difference / 60000L % 60L);
    int seconds = (int)(difference / 1000L % 60L);
    
    NumberFormat nf = NumberFormat.getInstance();
    nf.setMinimumIntegerDigits(2);
    nf.setMaximumIntegerDigits(2);
    
    StringBuffer timeString = new StringBuffer();
    if (days > 0)
    {
      timeString.append(days);
      timeString.append(" days and ");
    }
    if ((days > 0) || (hours > 0))
    {
      timeString.append(hours);
      timeString.append(":");
      timeString.append(nf.format(minutes));
      timeString.append(":");
      timeString.append(nf.format(seconds));
      timeString.append(" hours");
    }
    else if (minutes > 0)
    {
      timeString.append(minutes);
      timeString.append(":");
      timeString.append(nf.format(seconds));
      timeString.append(" minutes");
    }
    else
    {
      timeString.append(seconds);
      timeString.append(" seconds");
    }
    return timeString.toString();
  }
  
  void addLogEntry(byte type, String msg, boolean outputToSystemOut)
  {
    addLogEntry(null, type, msg, outputToSystemOut);
  }
  
  public void addLogEntry(String functionName, byte type, String msg, boolean outputToSystemOut)
  {
    try
    {
      if ((outputToSystemOut) || (this.DEBUG)) {
        System.out.println(this.instanceName + (functionName == null ? "" : new StringBuilder(" / ").append(functionName).toString()) + ": " + msg);
      }
      if (type < this.botLogLevel) {
        return;
      }
      if (this.logFile != null) {
        this.logFile.println(this.sdfDebug.format(new Date(System.currentTimeMillis())) + "\t" + (functionName == null ? "JTS3ServerMod" : new StringBuilder("Function ").append(functionName).toString()) + "\t" + ERROR_LEVEL_NAMES[type] + "\t" + msg);
      }
    }
    catch (Exception localException) {}
  }
  
  void addLogEntry(Throwable e, boolean outputToSystemOut)
  {
    addLogEntry(null, e, outputToSystemOut);
  }
  
  public void addLogEntry(String functionName, Throwable e, boolean outputToSystemOut)
  {
    try
    {
      if ((outputToSystemOut) || (this.DEBUG)) {
        System.out.println(this.instanceName + (functionName == null ? "" : new StringBuilder(" / ").append(functionName).toString()) + ": " + e.toString());
      }
      if (this.logFile != null)
      {
        this.logFile.println(this.sdfDebug.format(new Date(System.currentTimeMillis())) + "\t" + (functionName == null ? "JTS3ServerMod" : new StringBuilder("Function ").append(functionName).toString()) + "\t" + "EXCEPTION" + "\t" + "Bot Version: " + "5.5.4 (11.07.2015)");
        e.printStackTrace(this.logFile);
      }
      if ((e instanceof TS3ServerQueryException)) {
        if (((TS3ServerQueryException)e).getFailedPermissionID() >= 0)
        {
          String permissionName = getPermissionName(((TS3ServerQueryException)e).getFailedPermissionID());
          if (permissionName != null)
          {
            String permissionMsg = "Missing permission or not enough power: " + permissionName;
            if (outputToSystemOut) {
              System.out.println(this.instanceName + (functionName == null ? "" : new StringBuilder(" / ").append(functionName).toString()) + ": " + permissionMsg);
            }
            if (this.logFile != null) {
              this.logFile.println(this.sdfDebug.format(new Date(System.currentTimeMillis())) + "\t" + (functionName == null ? "JTS3ServerMod" : new StringBuilder("Function ").append(functionName).toString()) + "\tPERMISSION_ERROR\t" + permissionMsg);
            }
          }
        }
      }
    }
    catch (Exception localException) {}
  }
  
  public int getClientDBID(String uniqueID)
  {
    if ((uniqueID == null) || (uniqueID.length() < 25)) {
      return -1;
    }
    if (this.clientCache != null) {
      return this.clientCache.getDatabaseID(uniqueID);
    }
    try
    {
      HashMap<String, String> response = this.queryLib.doCommand("clientgetdbidfromuid cluid=" + uniqueID);
      if (!((String)response.get("id")).equals("0")) {
        return -1;
      }
      response = this.queryLib.parseLine((String)response.get("response"));
      return Integer.parseInt((String)response.get("cldbid"));
    }
    catch (Exception e) {}
    return -1;
  }
  
  static String getStackTrace(Throwable aThrowable)
  {
    Writer result = new StringWriter();
    PrintWriter printWriter = new PrintWriter(result);
    aThrowable.printStackTrace(printWriter);
    return result.toString();
  }
}
