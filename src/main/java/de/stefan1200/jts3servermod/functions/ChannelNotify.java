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
public class ChannelNotify
implements HandleBotEvents,
HandleTS3Events,
LoadConfiguration {
    private String configPrefix = "";
    private JTS3ServerMod_Interface modClass = null;
    private boolean pluginEnabled = false;
    private Vector<Integer> CHANNELNOTIFY_GROUP_LIST = new Vector<>();
    private boolean CHANNELNOTIFY_GROUP_LIST_IGNORE = true;
    private Vector<Integer> CHANNELNOTIFY_GROUPTARGETS = new Vector<>();
    private String CHANNELNOTIFY_MESSAGE_MODE = null;
    private String CHANNELNOTIFY_MESSAGENOTIFIED_MODE = null;
    private String CHANNELNOTIFY_FILE = null;
    private String CHANNELNOTIFY_MESSAGE = null;
    private String CHANNELNOTIFY_MESSAGENOTIFIED = null;
    private String CHANNELNOTIFY_MESSAGENOTNOTIFIED = null;
    private int CHANNELNOTIFY_CHANNELID = -1;
    private Vector<Integer> CHANNELNOTIFY_CHANNEL_LIST = new Vector<>();
    private boolean CHANNELNOTIFY_CHANNEL_LIST_IGNORE = true;
    private String channelName = null;

    @Override
    public void initClass(JTS3ServerMod_Interface modClass, JTS3ServerQuery queryLib, String prefix) {
        this.modClass = modClass;
        this.configPrefix = prefix.trim();
    }

    @Override
    public void handleOnBotConnect() {
        if (!this.pluginEnabled) {
            return;
        }
        StringBuffer groupTmp = new StringBuffer();
        Iterator<Integer> iterator = this.CHANNELNOTIFY_GROUPTARGETS.iterator();
        while (iterator.hasNext()) {
            int groupID = iterator.next();
            if (groupTmp.length() != 0) {
                groupTmp.append(", ");
            }
            groupTmp.append(groupID);
        }
        String msg = "Watching for new clients in channel \"" + this.channelName + "\" (id: " + Integer.toString(this.CHANNELNOTIFY_CHANNELID) + "), sending message to all online clients of server group ids: " + groupTmp.toString();
        this.modClass.addLogEntry(this.configPrefix, (byte)1, msg, true);
    }

    @Override
    public void handleAfterCacheUpdate() {
        if (!this.pluginEnabled) {
            return;
        }
        this.channelName = this.modClass.getChannelName(this.CHANNELNOTIFY_CHANNELID);
        if (this.channelName == null) {
            this.modClass.addLogEntry(this.configPrefix, (byte)3, "Critical: Channel ID " + this.CHANNELNOTIFY_CHANNELID + " don't exists! Check value " + this.configPrefix + "_channel_id in your configuration!", true);
            this.pluginEnabled = false;
            return;
        }
    }

    @Override
    public void activate() {
    }

    @Override
    public void disable() {
    }

    @Override
    public void unload() {
        this.CHANNELNOTIFY_GROUPTARGETS = null;
        this.CHANNELNOTIFY_CHANNEL_LIST = null;
        this.CHANNELNOTIFY_GROUP_LIST = null;
    }

    @Override
    public boolean multipleInstances() {
        return true;
    }

    @Override
    public void initConfig(ArrangedPropertiesWriter config) {
        config.addKey(String.valueOf(this.configPrefix) + "_channel_id", "Channel id that should be watched for new clients. You can only set one channel id here!");
        config.addKey(String.valueOf(this.configPrefix) + "_group_list", "A comma separated list (without spaces) of server group ids.\nDepends on the given mode, this server groups can be ignored or only this server groups will be watched!\nIf no server groups should be ignored, set no server groups here and select the group list mode ignore!");
        config.addKey(String.valueOf(this.configPrefix) + "_group_list_mode", "Select one of the two modes for the server group list.\nignore = The selected server groups will be ignored.\nonly = Send a notify message only if the selected server groups join the channel.", "ignore");
        config.addKey(String.valueOf(this.configPrefix) + "_grouptargets", "A comma separated list (without spaces) of server group ids, which should be notified about new clients in the specified channel.");
        config.addKey(String.valueOf(this.configPrefix) + "_channel_list", "A comma separated list (without spaces) of channel ids.\nDepends on the given mode, target clients in this channels can be ignored or only clients in this channels receive the notify message!\nIf no channels should be ignored, set no channels here and select the channel list mode ignore!");
        config.addKey(String.valueOf(this.configPrefix) + "_channel_list_mode", "Select one of the two modes for the channel list.\nignore = Clients in the selected channels will be ignored.\nonly = Only clients in the selected channels receive the notify message.", "ignore");
        config.addKey(String.valueOf(this.configPrefix) + "_message_mode", "Select the message mode, how the notified clients should get the message.\npoke or chat are valid values!", "poke");
        config.addKey(String.valueOf(this.configPrefix) + "_messagenotified_mode", "Select the message mode, how the clients (who joined the channel) should get the message.\npoke, chat or none are valid values!", "none");
        if (this.modClass.getMySQLConnection() == null) {
            config.addKey(String.valueOf(this.configPrefix) + "_file", "Path to file which contains the channel notify message", "config/server1/channelnotifymessages.cfg");
        }
        config.addKey(String.valueOf(this.configPrefix) + "_message", "Channel Notify message, specified clients get this message as chat or poke message.\nYou can use the following keywords, which will be replaced:\n%CLIENT_NAME% - Client Name\n%CLIENT_DBID% - Client Database ID\n%CLIENT_UNIQUEID% - Client Unique ID\n%CHANNEL_NAME% - Watched Channel Name\nTypical BBCode like in Teamspeak 3 Client possible. You can use \\n for a new line.", this.modClass.getMySQLConnection() != null);
        config.addKey(String.valueOf(this.configPrefix) + "_messagenotified", "Channel Notify message for the watched client.\nIf enabled, clients joining the watched channel get this message as chat or poke message.\nYou can use the following keywords, which will be replaced:\n%CLIENT_COUNT% - Number of target group clients who get informed about this client\n%CLIENT_NAMES% - List of names of target group clients who get informed about this client\n%CHANNEL_NAME% - Watched Channel Name\nTypical BBCode like in Teamspeak 3 Client possible. You can use \\n for a new line.", this.modClass.getMySQLConnection() != null);
        config.addKey(String.valueOf(this.configPrefix) + "_messagenotnotified", "Channel Notify message for the watched client, if no target group client is online.\nIf enabled, clients joining the watched channel get this message as chat or poke message.\nYou can use the following keywords, which will be replaced:\n%CHANNEL_NAME% - Watched Channel Name\nTypical BBCode like in Teamspeak 3 Client possible. You can use \\n for a new line.", this.modClass.getMySQLConnection() != null);
    }

    @Override
    public boolean loadConfig(ArrangedPropertiesWriter config, boolean slowMode) throws BotConfigurationException, NumberFormatException {
        this.pluginEnabled = false;
        if (slowMode) {
            this.modClass.addLogEntry(this.configPrefix, (byte)3, "Slow Mode activated, Channel Notify disabled!", true);
            return false;
        }
        String lastNumberValue = "";
        String temp = null;
        try {
            StringTokenizer st;
            this.CHANNELNOTIFY_FILE = config.getValue(String.valueOf(this.configPrefix) + "_file");
            this.CHANNELNOTIFY_MESSAGE_MODE = config.getValue(String.valueOf(this.configPrefix) + "_message_mode", "chat").trim();
            this.CHANNELNOTIFY_MESSAGENOTIFIED_MODE = config.getValue(String.valueOf(this.configPrefix) + "_messagenotified_mode", "chat").trim();
            String[] configKeys = new String[]{String.valueOf(this.configPrefix) + "_message", String.valueOf(this.configPrefix) + "_messagenotified", String.valueOf(this.configPrefix) + "_messagenotnotified"};
            if (!this.modClass.loadMessages(this.configPrefix, "_file", configKeys)) {
                throw new BotConfigurationException("Channel Notify messages could not be loaded!");
            }
            this.CHANNELNOTIFY_MESSAGE = config.getValue(configKeys[0]);
            if (this.CHANNELNOTIFY_MESSAGE == null || this.CHANNELNOTIFY_MESSAGE.length() == 0) {
                throw new BotConfigurationException("Channel Notify message missing in config!");
            }
            if (this.CHANNELNOTIFY_MESSAGENOTIFIED_MODE.equalsIgnoreCase("poke") || this.CHANNELNOTIFY_MESSAGENOTIFIED_MODE.equalsIgnoreCase("chat")) {
                this.CHANNELNOTIFY_MESSAGENOTIFIED = config.getValue(configKeys[1]);
                if (this.CHANNELNOTIFY_MESSAGENOTIFIED == null || this.CHANNELNOTIFY_MESSAGENOTIFIED.length() == 0) {
                    throw new BotConfigurationException("Channel Notify message for the watched client missing in config!");
                }
                this.CHANNELNOTIFY_MESSAGENOTNOTIFIED = config.getValue(configKeys[2]);
                if (this.CHANNELNOTIFY_MESSAGENOTNOTIFIED == null || this.CHANNELNOTIFY_MESSAGENOTNOTIFIED.length() == 0) {
                    throw new BotConfigurationException("Channel Notify message for the watched client (if no target group client is online) missing in config!");
                }
            }
            lastNumberValue = String.valueOf(this.configPrefix) + "_channel_id";
            temp = config.getValue(String.valueOf(this.configPrefix) + "_channel_id");
            if (temp == null) {
                throw new NumberFormatException();
            }
            this.CHANNELNOTIFY_CHANNELID = Integer.parseInt(temp.trim());
            temp = null;
            this.CHANNELNOTIFY_GROUPTARGETS.clear();
            temp = config.getValue(String.valueOf(this.configPrefix) + "_grouptargets");
            lastNumberValue = String.valueOf(this.configPrefix) + "_grouptargets";
            if (temp != null && temp.length() > 0) {
                st = new StringTokenizer(temp, ",", false);
                while (st.hasMoreTokens()) {
                    this.CHANNELNOTIFY_GROUPTARGETS.addElement(Integer.parseInt(st.nextToken().trim()));
                }
            }
            temp = null;
            this.CHANNELNOTIFY_CHANNEL_LIST.clear();
            temp = config.getValue(String.valueOf(this.configPrefix) + "_channel_list");
            lastNumberValue = String.valueOf(this.configPrefix) + "_channel_list";
            if (temp != null && temp.length() > 0) {
                st = new StringTokenizer(temp, ",", false);
                while (st.hasMoreTokens()) {
                    this.CHANNELNOTIFY_CHANNEL_LIST.addElement(Integer.parseInt(st.nextToken().trim()));
                }
            }
            this.CHANNELNOTIFY_CHANNEL_LIST_IGNORE = !config.getValue(String.valueOf(this.configPrefix) + "_channel_list_mode", "ignore").trim().equalsIgnoreCase("only");
            temp = null;
            this.CHANNELNOTIFY_GROUP_LIST.clear();
            temp = config.getValue(String.valueOf(this.configPrefix) + "_group_list");
            lastNumberValue = String.valueOf(this.configPrefix) + "_group_list";
            if (temp != null && temp.length() > 0) {
                st = new StringTokenizer(temp, ",", false);
                while (st.hasMoreTokens()) {
                    this.CHANNELNOTIFY_GROUP_LIST.addElement(Integer.parseInt(st.nextToken().trim()));
                }
            }
            this.CHANNELNOTIFY_GROUP_LIST_IGNORE = !config.getValue(String.valueOf(this.configPrefix) + "_group_list_mode", "ignore").trim().equalsIgnoreCase("only");
            if (this.CHANNELNOTIFY_GROUPTARGETS.size() == 0) {
                throw new BotConfigurationException("No Channel Notify targets was defined! Please check configuration.");
            }
            this.modClass.addTS3ChannelEvent(this);
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
        listOptions.set(1);
        listOptions.set(4);
    }

    @Override
    public String[] botChatCommandList(HashMap<String, String> eventInfo, boolean isFullAdmin, boolean isAdmin) {
        return null;
    }

    @Override
    public String botChatCommandHelp(String command) {
        return null;
    }

    @Override
    public boolean handleChatCommands(String msg, HashMap<String, String> eventInfo, boolean isFullAdmin, boolean isAdmin) {
        return false;
    }

    @Override
    public void handleClientEvents(String eventType, HashMap<String, String> eventInfo) {
        if (!this.pluginEnabled) {
            return;
        }
        if (eventType.equals("notifycliententerview") || eventType.equals("notifyclientmoved")) {
            if (Integer.parseInt(eventInfo.get("ctid")) != this.CHANNELNOTIFY_CHANNELID) {
                return;
            }
            Vector<HashMap<String, String>> clientList = this.modClass.getClientList();
            if (clientList == null) {
                return;
            }
            HashMap<String, String> clientEvent = null;
            if (eventType.equals("notifycliententerview")) {
                clientEvent = eventInfo;
            } else if (eventType.equals("notifyclientmoved")) {
                clientEvent = new HashMap<>();
                for (HashMap<String, String> clientEventTemp : clientList) {
                    if (!clientEventTemp.get("clid").equals(eventInfo.get("clid"))) continue;
                    clientEvent = clientEventTemp;
                    break;
                }
            }
            if (clientEvent != null && clientEvent.get("client_type").equals("0") && this.modClass.getListedGroup((String)clientEvent.get("client_servergroups"), this.CHANNELNOTIFY_GROUPTARGETS) == -1) {
                boolean result = this.modClass.isGroupListed((String)clientEvent.get("client_servergroups"), this.CHANNELNOTIFY_GROUP_LIST);
                if (this.CHANNELNOTIFY_GROUP_LIST_IGNORE ? !result : result) {
                    Vector<String> targetClients = new Vector<String>();
                    Vector<String> targetClients_UID = new Vector<String>();
                    String cnMessage = new String(this.CHANNELNOTIFY_MESSAGE);
                    cnMessage = cnMessage.replace("%CLIENT_NAME%", (CharSequence)clientEvent.get("client_nickname"));
                    cnMessage = cnMessage.replace("%CLIENT_NAME_CLICKABLE%", "[URL=client://0/" + (String)clientEvent.get("client_unique_identifier") + "]" + (String)clientEvent.get("client_nickname") + "[/URL]");
                    cnMessage = cnMessage.replace("%CLIENT_DBID%", (CharSequence)clientEvent.get("client_database_id"));
                    cnMessage = cnMessage.replace("%CLIENT_UNIQUEID%", (CharSequence)clientEvent.get("client_unique_identifier"));
                    cnMessage = cnMessage.replace("%CHANNEL_NAME%", this.channelName);
                    if (!this.modClass.isMessageLengthValid(this.CHANNELNOTIFY_MESSAGE_MODE, cnMessage = cnMessage.replace("%CHANNEL_NAME_CLICKABLE%", "[URL=channelid://" + Integer.toString(this.CHANNELNOTIFY_CHANNELID) + "]" + this.channelName + "[/URL]"))) {
                        this.modClass.addLogEntry(this.configPrefix, (byte)2, "Channel Notify message is to long! Make sure that " + this.CHANNELNOTIFY_MESSAGE_MODE + " messages are not longer than " + Short.toString(this.modClass.getMaxMessageLength(this.CHANNELNOTIFY_MESSAGE_MODE)) + " characters (including spaces and BBCode)" + (this.modClass.getMySQLConnection() == null ? new StringBuilder(", check file: ").append(this.CHANNELNOTIFY_FILE).toString() : "!"), true);
                    }
                    int clientID = -1;
                    for (HashMap<String, String> clientInfo : clientList) {
                        if (!clientInfo.get("client_type").equals("0") || !this.modClass.isGroupListed(clientInfo.get("client_servergroups"), this.CHANNELNOTIFY_GROUPTARGETS)) continue;
                        result = this.modClass.isIDListed(Integer.parseInt(clientInfo.get("cid")), this.CHANNELNOTIFY_CHANNEL_LIST);
                        if (!(this.CHANNELNOTIFY_CHANNEL_LIST_IGNORE ? !result : result) || !this.modClass.sendMessageToClient(this.configPrefix, this.CHANNELNOTIFY_MESSAGE_MODE, clientID = Integer.parseInt(clientInfo.get("clid")), cnMessage)) continue;
                        targetClients.addElement(clientInfo.get("client_nickname"));
                        targetClients_UID.addElement(clientInfo.get("client_unique_identifier"));
                    }
                    if (this.CHANNELNOTIFY_MESSAGENOTIFIED_MODE.equalsIgnoreCase("poke") || this.CHANNELNOTIFY_MESSAGENOTIFIED_MODE.equalsIgnoreCase("chat")) {
                        clientID = Integer.parseInt((String)clientEvent.get("clid"));
                        if (targetClients.size() == 0) {
                            String cnMessageNotNotified = new String(this.CHANNELNOTIFY_MESSAGENOTNOTIFIED);
                            if (!this.modClass.isMessageLengthValid(this.CHANNELNOTIFY_MESSAGENOTIFIED_MODE, cnMessageNotNotified = cnMessageNotNotified.replace("%CHANNEL_NAME%", this.channelName))) {
                                this.modClass.addLogEntry(this.configPrefix, (byte)2, "Channel Notify message for the watched client (if no target group client is online) is to long! Make sure that " + this.CHANNELNOTIFY_MESSAGENOTIFIED_MODE + " messages are not longer than " + Short.toString(this.modClass.getMaxMessageLength(this.CHANNELNOTIFY_MESSAGENOTIFIED_MODE)) + " characters (including spaces and BBCode)" + (this.modClass.getMySQLConnection() == null ? new StringBuilder(", check file: ").append(this.CHANNELNOTIFY_FILE).toString() : "!"), true);
                            }
                            this.modClass.sendMessageToClient(this.configPrefix, this.CHANNELNOTIFY_MESSAGENOTIFIED_MODE, clientID, cnMessageNotNotified);
                        } else {
                            StringBuffer sb = new StringBuffer();
                            StringBuffer sbURL = new StringBuffer();
                            int i = 0;
                            while (i < targetClients.size()) {
                                if (i > 0) {
                                    sb.append(", ");
                                    sbURL.append(", ");
                                }
                                sb.append((String)targetClients.elementAt(i));
                                sbURL.append("[URL=client://0/" + (String)targetClients_UID.elementAt(i) + "]" + (String)targetClients.elementAt(i) + "[/URL]");
                                ++i;
                            }
                            String cnMessageNotified = new String(this.CHANNELNOTIFY_MESSAGENOTIFIED);
                            cnMessageNotified = cnMessageNotified.replace("%CLIENT_COUNT%", Integer.toString(targetClients.size()));
                            cnMessageNotified = cnMessageNotified.replace("%CLIENT_NAMES%", sb.toString());
                            cnMessageNotified = cnMessageNotified.replace("%CLIENT_NAMES_CLICKABLE%", sbURL.toString());
                            if (!this.modClass.isMessageLengthValid(this.CHANNELNOTIFY_MESSAGENOTIFIED_MODE, cnMessageNotified = cnMessageNotified.replace("%CHANNEL_NAME%", this.channelName))) {
                                this.modClass.addLogEntry(this.configPrefix, (byte)2, "Channel Notify message for the watched client is to long! Make sure that " + this.CHANNELNOTIFY_MESSAGENOTIFIED_MODE + " messages are not longer than " + Short.toString(this.modClass.getMaxMessageLength(this.CHANNELNOTIFY_MESSAGENOTIFIED_MODE)) + " characters (including spaces and BBCode)" + (this.modClass.getMySQLConnection() == null ? new StringBuilder(", check file: ").append(this.CHANNELNOTIFY_FILE).toString() : "!"), true);
                            }
                            this.modClass.sendMessageToClient(this.configPrefix, this.CHANNELNOTIFY_MESSAGENOTIFIED_MODE, clientID, cnMessageNotified);
                        }
                    }
                }
            }
        }
    }
}

