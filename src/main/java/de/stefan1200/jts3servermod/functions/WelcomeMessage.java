/*
 * Decompiled with CFR 0_110.
 */
package de.stefan1200.jts3servermod.functions;

import java.util.BitSet;
import java.util.HashMap;
import java.util.Iterator;
import java.util.StringTokenizer;
import java.util.Vector;

import de.stefan1200.jts3servermod.BotConfigurationException;
import de.stefan1200.jts3servermod.interfaces.HandleBotEvents;
import de.stefan1200.jts3servermod.interfaces.HandleTS3Events;
import de.stefan1200.jts3servermod.interfaces.JTS3ServerMod_Interface;
import de.stefan1200.jts3servermod.interfaces.LoadConfiguration;
import de.stefan1200.jts3serverquery.JTS3ServerQuery;
import de.stefan1200.util.ArrangedPropertiesWriter;

/*
 * This class specifies class file version 49.0 but uses Java 6 signatures.  Assumed Java 6.
 */
public class WelcomeMessage
implements HandleBotEvents,
HandleTS3Events,
LoadConfiguration {
    private String configPrefix;
    private JTS3ServerMod_Interface modClass = null;
    private JTS3ServerQuery queryLib = null;
    private boolean pluginEnabled = false;
    private String WELCOMEMESSAGE_FILE = null;
    private Vector<Integer> WELCOMEMESSAGE_GROUP_LIST = new Vector<>();
    private boolean WELCOMEMESSAGE_GROUP_LIST_IGNORE = true;
    private Vector<String> WELCOMEMESSAGE_SHOWONCONNECTION = new Vector<>();
    private String WELCOMEMESSAGE_MESSAGE_MODE = null;
    private String WELCOMEMESSAGE_MESSAGE = null;

    @Override
    public void initClass(JTS3ServerMod_Interface modClass, JTS3ServerQuery queryLib, String prefix) {
        this.modClass = modClass;
        this.queryLib = queryLib;
        this.configPrefix = prefix.trim();
    }

    @Override
    public boolean multipleInstances() {
        return true;
    }

    @Override
    public void handleOnBotConnect() {
        String msg;
        if (!this.pluginEnabled) {
            return;
        }
        if (this.WELCOMEMESSAGE_GROUP_LIST.size() == 0 && this.WELCOMEMESSAGE_GROUP_LIST_IGNORE) {
            msg = "All new connecting clients get the welcome message" + (this.WELCOMEMESSAGE_FILE == null ? "!" : new StringBuilder(" from file ").append(this.WELCOMEMESSAGE_FILE).toString());
        } else {
            StringBuffer groupTmp = new StringBuffer();
            Iterator<Integer> iterator = this.WELCOMEMESSAGE_GROUP_LIST.iterator();
            while (iterator.hasNext()) {
                int groupID = iterator.next();
                if (groupTmp.length() != 0) {
                    groupTmp.append(", ");
                }
                groupTmp.append(groupID);
            }
            msg = "New connecting clients which are" + (this.WELCOMEMESSAGE_GROUP_LIST_IGNORE ? " not " : " ") + "members of selected server groups (id: " + groupTmp.toString() + ") get the welcome message" + (this.WELCOMEMESSAGE_FILE == null ? "!" : new StringBuilder(" from file ").append(this.WELCOMEMESSAGE_FILE).toString());
        }
        this.modClass.addLogEntry(this.configPrefix, (byte)1, msg, true);
    }

    @Override
    public void handleAfterCacheUpdate() {
    }

    @Override
    public void activate() {
    }

    @Override
    public void disable() {
    }

    @Override
    public void unload() {
        this.WELCOMEMESSAGE_GROUP_LIST = null;
        this.WELCOMEMESSAGE_SHOWONCONNECTION = null;
    }

    @Override
    public void initConfig(ArrangedPropertiesWriter config) {
        config.addKey(String.valueOf(this.configPrefix) + "_group_list", "A comma separated list (without spaces) of server group ids.\nDepends on the given mode, this server groups can be ignored or only this server groups get this welcome message!\nIf no server groups should be ignored, set no server groups here and select the group list mode ignore!");
        config.addKey(String.valueOf(this.configPrefix) + "_group_list_mode", "Select one of the two modes for the server group list.\nignore = The selected server groups will be ignored and don't get this welcome message.\nonly = Only the selected server groups get this welcome message.", "ignore");
        config.addKey(String.valueOf(this.configPrefix) + "_showonconnectionnumber", "A comma separated list (without spaces) of the total connection count (it's the same value as %CLIENT_TOTALCONNECTIONS% at the welcome message),\nwhen this message should be send to the client. Set this to -1 to show it every time.\nRanges like 1-100, -20 (means 20 or less) or 100- (means 100 or more) are also possible.", "-1");
        config.addKey(String.valueOf(this.configPrefix) + "_message_mode", "Select the message mode, how the client should get the message.\npoke or chat are valid values!", "chat");
        if (this.modClass.getMySQLConnection() == null) {
            config.addKey(String.valueOf(this.configPrefix) + "_file", "Path to file which contains the welcome message.", "config/server1/welcomemessages.cfg");
        }
        config.addKey(String.valueOf(this.configPrefix) + "_message", "Set welcome message. You can use \\n for a new line and typical BBCode like in Teamspeak 3 Client.\nYou can use the following keywords, which will be replaced:\n%CLIENT_ID% - Current client ID on the server\n%CLIENT_DATABASE_ID% - Client database ID\n%CLIENT_UNIQUE_ID% - Unique client ID\n%CLIENT_COUNTRY% - Short name of the client country, detected by the TS3 server\n%CLIENT_NICKNAME% - Nickname of the client\n%CLIENT_VERSION% - Client version\n%CLIENT_PLATFORM% - Client platform (Windows, Linux, ...)\n%CLIENT_CREATED% - Date and time of the first connection of the client\n%CLIENT_TOTALCONNECTIONS% - Total connection count of the client\n%CLIENT_UNREAD_MESSAGES% - Offline message count\n%CLIENT_MONTH_BYTES_UPLOADED% - Uploaded data in current month (filetransfer and avatar)\n%CLIENT_MONTH_BYTES_DOWNLOADED% - Downloaded data in current month (filetransfer and avatar)\n%CLIENT_TOTAL_BYTES_UPLOADED% - Uploaded data all times (filetransfer and avatar)\n%CLIENT_TOTAL_BYTES_DOWNLOADED% - Downloaded data all times (filetransfer and avatar)\nThis welcome message will be used for specified server groups you defined at welcomemessage_groups.", this.modClass.getMySQLConnection() != null);
    }

    @Override
    public boolean loadConfig(ArrangedPropertiesWriter config, boolean slowMode) throws BotConfigurationException, NumberFormatException {
        this.pluginEnabled = false;
        if (slowMode) {
            this.modClass.addLogEntry(this.configPrefix, (byte)3, "Slow Mode activated, Welcome Message disabled!", true);
            return false;
        }
        String lastNumberValue = "";
        String temp = null;
        try {
            StringTokenizer st;
            this.WELCOMEMESSAGE_GROUP_LIST.clear();
            temp = config.getValue(String.valueOf(this.configPrefix) + "_group_list", "");
            lastNumberValue = String.valueOf(this.configPrefix) + "_group_list";
            if (temp != null && temp.length() > 0) {
                st = new StringTokenizer(temp, ",", false);
                while (st.hasMoreTokens()) {
                    this.WELCOMEMESSAGE_GROUP_LIST.addElement(Integer.parseInt(st.nextToken().trim()));
                }
            }
            this.WELCOMEMESSAGE_GROUP_LIST_IGNORE = !config.getValue(String.valueOf(this.configPrefix) + "_group_list_mode", "ignore").trim().equalsIgnoreCase("only");
            this.WELCOMEMESSAGE_SHOWONCONNECTION.clear();
            temp = config.getValue(String.valueOf(this.configPrefix) + "_showonconnectionnumber", "-1");
            lastNumberValue = String.valueOf(this.configPrefix) + "_showonconnectionnumber";
            if (temp != null && temp.length() > 0) {
                st = new StringTokenizer(temp, ",", false);
                while (st.hasMoreTokens()) {
                    this.WELCOMEMESSAGE_SHOWONCONNECTION.addElement(st.nextToken().trim());
                }
            }
            this.WELCOMEMESSAGE_MESSAGE_MODE = config.getValue(String.valueOf(this.configPrefix) + "_message_mode", "chat").trim();
            this.WELCOMEMESSAGE_FILE = config.getValue(String.valueOf(this.configPrefix) + "_file");
            String[] configKeys = new String[]{String.valueOf(this.configPrefix) + "_message"};
            if (!this.modClass.loadMessages(this.configPrefix, "_file", configKeys)) {
                throw new BotConfigurationException("Welcome message could not be loaded!");
            }
            this.WELCOMEMESSAGE_MESSAGE = config.getValue(configKeys[0]);
            if (this.WELCOMEMESSAGE_MESSAGE == null || this.WELCOMEMESSAGE_MESSAGE.length() == 0) {
                throw new BotConfigurationException("Welcome message could not be loaded!");
            }
            if (!this.modClass.isMessageLengthValid(this.WELCOMEMESSAGE_MESSAGE_MODE, this.WELCOMEMESSAGE_MESSAGE)) {
                this.modClass.addLogEntry(this.configPrefix, (byte)2, "Welcome Message is to long! Make sure that " + this.WELCOMEMESSAGE_MESSAGE_MODE + " messages are not longer than " + Short.toString(this.modClass.getMaxMessageLength(this.WELCOMEMESSAGE_MESSAGE_MODE)) + " characters (including spaces and BBCode)" + (this.modClass.getMySQLConnection() == null ? new StringBuilder(", check file: ").append(this.WELCOMEMESSAGE_FILE).toString() : ""), true);
            }
            this.modClass.addTS3ServerEvent(this);
            this.pluginEnabled = true;
        }
        catch (NumberFormatException e) {
            NumberFormatException nfe = new NumberFormatException("Config value of \"" + lastNumberValue + "\" is not a number! Current value: " + config.getValue(lastNumberValue, "not set"));
            nfe.setStackTrace(e.getStackTrace());
            throw nfe;
        }
        return this.pluginEnabled;
    }

    @Override
    public void setListModes(BitSet listOptions) {
    }

    @Override
    public String[] botChatCommandList(HashMap<String, String> eventInfo, boolean isFullAdmin, boolean isAdmin) {
        if (!this.pluginEnabled) {
            return null;
        }
        if (isFullAdmin || isAdmin) {
            String[] commands = new String[]{"test"};
            return commands;
        }
        return null;
    }

    @Override
    public String botChatCommandHelp(String command) {
        if (command.equalsIgnoreCase("test")) {
            return "Sends you a test message with all placeholders of the welcome message function.";
        }
        return null;
    }

    @Override
    public boolean handleChatCommands(String msg, HashMap<String, String> eventInfo, boolean isFullAdmin, boolean isAdmin) {
        if (!this.pluginEnabled) {
            return false;
        }
        if (msg.equalsIgnoreCase("test")) {
            try {
                if (isFullAdmin || isAdmin) {
                    String welcomeDebugClient = this.createWelcomeMessage("CLIENT_ID: %CLIENT_ID%\nCLIENT_DATABASE_ID: %CLIENT_DATABASE_ID%\nCLIENT_UNIQUE_ID: %CLIENT_UNIQUE_ID%\nCLIENT_COUNTRY: %CLIENT_COUNTRY%\nCLIENT_NICKNAME: %CLIENT_NICKNAME%\nCLIENT_VERSION: %CLIENT_VERSION%\nCLIENT_PLATFORM: %CLIENT_PLATFORM%\nCLIENT_IP: %CLIENT_IP%\nCLIENT_CREATED: %CLIENT_CREATED%\nCLIENT_TOTALCONNECTIONS: %CLIENT_TOTALCONNECTIONS%\nCLIENT_MONTH_BYTES_UPLOADED: %CLIENT_MONTH_BYTES_UPLOADED%\nCLIENT_MONTH_BYTES_DOWNLOADED: %CLIENT_MONTH_BYTES_DOWNLOADED%\nCLIENT_TOTAL_BYTES_UPLOADED: %CLIENT_TOTAL_BYTES_UPLOADED%\nCLIENT_TOTAL_BYTES_DOWNLOADED: %CLIENT_TOTAL_BYTES_DOWNLOADED%", Integer.parseInt(eventInfo.get("invokerid")), true);
                    this.modClass.sendMessageToClient(this.configPrefix, "chat", Integer.parseInt(eventInfo.get("invokerid")), welcomeDebugClient);
                    String welcomeDebugServer = this.createWelcomeMessage("The following server variables will only be parsed, if global message variables are activated at the general bot settings!\n\nSERVER_NAME: %SERVER_NAME%\nSERVER_PLATFORM: %SERVER_PLATFORM%\nSERVER_VERSION: %SERVER_VERSION%\nSERVER_UPTIME: %SERVER_UPTIME%\nSERVER_UPTIME_DATE: %SERVER_UPTIME_DATE%\nSERVER_CREATED_DATE: %SERVER_CREATED_DATE%\nSERVER_UPLOAD_QUOTA: %SERVER_UPLOAD_QUOTA%\nSERVER_DOWNLOAD_QUOTA: %SERVER_DOWNLOAD_QUOTA%\nSERVER_MONTH_BYTES_UPLOADED: %SERVER_MONTH_BYTES_UPLOADED%\nSERVER_MONTH_BYTES_DOWNLOADED: %SERVER_MONTH_BYTES_DOWNLOADED%\nSERVER_TOTAL_BYTES_UPLOADED: %SERVER_TOTAL_BYTES_UPLOADED%\nSERVER_TOTAL_BYTES_DOWNLOADED: %SERVER_TOTAL_BYTES_DOWNLOADED%\nSERVER_MAX_CLIENTS: %SERVER_MAX_CLIENTS%\nSERVER_RESERVED_SLOTS: %SERVER_RESERVED_SLOTS%\nSERVER_CHANNEL_COUNT: %SERVER_CHANNEL_COUNT%\nSERVER_CLIENT_COUNT: %SERVER_CLIENT_COUNT%\nSERVER_CLIENT_CONNECTIONS_COUNT: %SERVER_CLIENT_CONNECTIONS_COUNT%\nSERVER_CLIENT_DB_COUNT: %SERVER_CLIENT_DB_COUNT%", Integer.parseInt(eventInfo.get("invokerid")), true);
                    this.modClass.sendMessageToClient(this.configPrefix, "chat", Integer.parseInt(eventInfo.get("invokerid")), welcomeDebugServer);
                } else {
                    this.queryLib.sendTextMessage(Integer.parseInt(eventInfo.get("invokerid")), 1, "You are not my master!");
                }
            }
            catch (Exception e) {
                this.modClass.addLogEntry(this.configPrefix, e, false);
            }
            return true;
        }
        return false;
    }

    @Override
    public void handleClientEvents(String eventType, HashMap<String, String> eventInfo) {
        if (!this.pluginEnabled) {
            return;
        }
        if (eventType.equals("notifycliententerview") && Integer.parseInt(eventInfo.get("client_type")) == 0 && this.WELCOMEMESSAGE_MESSAGE != null) {
            String welcomeMessage;
            int clientID;
            boolean result = this.modClass.isGroupListed(eventInfo.get("client_servergroups"), this.WELCOMEMESSAGE_GROUP_LIST);
            if ((this.WELCOMEMESSAGE_GROUP_LIST_IGNORE ? !result : result) && (welcomeMessage = this.createWelcomeMessage(this.WELCOMEMESSAGE_MESSAGE, clientID = Integer.parseInt(eventInfo.get("clid")), false)) != null) {
                this.modClass.sendMessageToClient(this.configPrefix, this.WELCOMEMESSAGE_MESSAGE_MODE, clientID, welcomeMessage);
            }
        }
    }

    /*
     * Enabled aggressive block sorting
     * Enabled unnecessary exception pruning
     * Enabled aggressive exception aggregation
     */
    private String createWelcomeMessage(String template, int clientID, boolean chatCommand) {
        HashMap<String, String> clientInfo;
        try {
            clientInfo = this.queryLib.getInfo(13, clientID);
            if (clientInfo == null) {
                throw new NullPointerException("Invalid client information returned!");
            }
        }
        catch (Exception e) {
            this.modClass.addLogEntry(this.configPrefix, (byte)3, "Error while requesting information of client id " + Integer.toString(clientID) + " from TS3 server!", false);
            this.modClass.addLogEntry(this.configPrefix, e, false);
            return null;
        }
        String welcomeMessage = null;
        try {
            if (this.WELCOMEMESSAGE_SHOWONCONNECTION.indexOf("-1") == -1 && !chatCommand) {
                if (clientInfo.get("client_totalconnections") == null) {
                    this.modClass.addLogEntry(this.configPrefix, new NullPointerException("Got no client_totalconnections value from TS3 server!"), false);
                    return null;
                }
                for (int i = 0; i < this.WELCOMEMESSAGE_SHOWONCONNECTION.size(); ++i) {
                    int currentValue = Integer.parseInt(clientInfo.get("client_totalconnections"));
                    String value = this.WELCOMEMESSAGE_SHOWONCONNECTION.elementAt(i);
                    int pos = value.indexOf("-");
                    if (pos != -1) {
                        int firstNumber = 0;
                        int secondNumber = Integer.MAX_VALUE;
                        if (pos > 0) {
                            firstNumber = Integer.parseInt(value.substring(0, pos).trim());
                        }
                        if (pos < value.length() - 1) {
                            secondNumber = Integer.parseInt(value.substring(pos + 1).trim());
                        }
                        if (currentValue < firstNumber) return null;
                        if (currentValue <= secondNumber) continue;
                        return null;
                    }
                    if (currentValue == Integer.parseInt(value)) continue;
                    return null;
                }
            }
        }
        catch (Exception e) {
            this.modClass.addLogEntry(this.configPrefix, e, false);
        }
        try {
            welcomeMessage = template;
            if (clientInfo.get("cid") != null) {
                welcomeMessage = welcomeMessage.replace("%CLIENT_ID%", clientInfo.get("cid"));
            }
            if (clientInfo.get("client_database_id") != null) {
                welcomeMessage = welcomeMessage.replace("%CLIENT_DATABASE_ID%", clientInfo.get("client_database_id"));
            }
            if (clientInfo.get("client_unique_identifier") != null) {
                welcomeMessage = welcomeMessage.replace("%CLIENT_UNIQUE_ID%", clientInfo.get("client_unique_identifier"));
            }
            if (clientInfo.get("client_country") != null) {
                welcomeMessage = welcomeMessage.replace("%CLIENT_COUNTRY%", clientInfo.get("client_country"));
            }
            if (clientInfo.get("client_nickname") != null) {
                welcomeMessage = welcomeMessage.replace("%CLIENT_NICKNAME%", clientInfo.get("client_nickname"));
            }
            if (clientInfo.get("client_version") != null) {
                welcomeMessage = welcomeMessage.replace("%CLIENT_VERSION%", this.modClass.getVersionString(clientInfo.get("client_version")));
            }
            if (clientInfo.get("client_platform") != null) {
                welcomeMessage = welcomeMessage.replace("%CLIENT_PLATFORM%", clientInfo.get("client_platform"));
            }
            if (clientInfo.get("connection_client_ip") != null) {
                welcomeMessage = welcomeMessage.replace("%CLIENT_IP%", clientInfo.get("connection_client_ip"));
            }
            if (clientInfo.get("client_created") != null) {
                welcomeMessage = welcomeMessage.replace("%CLIENT_CREATED%", this.modClass.getStringFromTimestamp(Long.parseLong(clientInfo.get("client_created")) * 1000));
            }
            if (clientInfo.get("client_totalconnections") != null) {
                welcomeMessage = welcomeMessage.replace("%CLIENT_TOTALCONNECTIONS%", clientInfo.get("client_totalconnections"));
            }
            if (clientInfo.get("client_month_bytes_uploaded") != null) {
                welcomeMessage = welcomeMessage.replace("%CLIENT_MONTH_BYTES_UPLOADED%", this.modClass.getFileSizeString(Long.parseLong(clientInfo.get("client_month_bytes_uploaded")), false));
            }
            if (clientInfo.get("client_month_bytes_downloaded") != null) {
                welcomeMessage = welcomeMessage.replace("%CLIENT_MONTH_BYTES_DOWNLOADED%", this.modClass.getFileSizeString(Long.parseLong(clientInfo.get("client_month_bytes_downloaded")), false));
            }
            if (clientInfo.get("client_total_bytes_uploaded") != null) {
                welcomeMessage = welcomeMessage.replace("%CLIENT_TOTAL_BYTES_UPLOADED%", this.modClass.getFileSizeString(Long.parseLong(clientInfo.get("client_total_bytes_uploaded")), false));
            }
            if (clientInfo.get("client_total_bytes_downloaded") == null) return welcomeMessage;
            return welcomeMessage.replace("%CLIENT_TOTAL_BYTES_DOWNLOADED%", this.modClass.getFileSizeString(Long.parseLong(clientInfo.get("client_total_bytes_downloaded")), false));
        }
        catch (Exception e) {
            this.modClass.addLogEntry(this.configPrefix, e, false);
            return null;
        }
    }
}

