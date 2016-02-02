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
public class ServerGroupNotify
implements HandleBotEvents,
HandleTS3Events,
LoadConfiguration {
    private String configPrefix = "";
    private JTS3ServerMod_Interface modClass = null;
    private boolean pluginEnabled = false;
    private Vector<Integer> SERVERGROUPNOTIFY_GROUPS = new Vector<>();
    private Vector<Integer> SERVERGROUPNOTIFY_GROUPTARGETS = new Vector<>();
    private String SERVERGROUPNOTIFY_MESSAGE_MODE = null;
    private String SERVERGROUPNOTIFY_MESSAGENOTIFIED_MODE = null;
    private String SERVERGROUPNOTIFY_FILE = null;
    private String SERVERGROUPNOTIFY_MESSAGE = null;
    private String SERVERGROUPNOTIFY_MESSAGENOTIFIED = null;
    private String SERVERGROUPNOTIFY_MESSAGENOTNOTIFIED = null;
    private Vector<Integer> SERVERGROUPNOTIFY_CHANNEL_LIST = new Vector<>();
    private boolean SERVERGROUPNOTIFY_CHANNEL_LIST_IGNORE = true;

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
        Iterator<Integer> iterator = this.SERVERGROUPNOTIFY_GROUPS.iterator();
        while (iterator.hasNext()) {
            int groupID = iterator.next();
            if (groupTmp.length() != 0) {
                groupTmp.append(", ");
            }
            groupTmp.append(groupID);
        }
        StringBuffer groupTmp2 = new StringBuffer();
        Iterator<Integer> iterator2 = this.SERVERGROUPNOTIFY_GROUPTARGETS.iterator();
        while (iterator2.hasNext()) {
            int groupID = iterator2.next();
            if (groupTmp2.length() != 0) {
                groupTmp2.append(", ");
            }
            groupTmp2.append(groupID);
        }
        String msg = "Watching for new connecting clients of selected server groups (id: " + groupTmp.toString() + "), sending message to all online clients of server group ids: " + groupTmp2.toString();
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
        this.SERVERGROUPNOTIFY_GROUPS = null;
        this.SERVERGROUPNOTIFY_GROUPTARGETS = null;
        this.SERVERGROUPNOTIFY_CHANNEL_LIST = null;
    }

    @Override
    public boolean multipleInstances() {
        return true;
    }

    @Override
    public void initConfig(ArrangedPropertiesWriter config) {
        config.addKey(String.valueOf(this.configPrefix) + "_groups", "A comma separated list (without spaces) of server group ids, which should be watched on joining.");
        config.addKey(String.valueOf(this.configPrefix) + "_grouptargets", "A comma separated list (without spaces) of server group ids, which should be notified about joining clients.");
        config.addKey(String.valueOf(this.configPrefix) + "_channel_list", "A comma separated list (without spaces) of channel ids.\nDepends on the given mode, target clients in this channels can be ignored or only clients in this channels receive the notify message!\nIf no channels should be ignored, set no channels here and select the channel list mode ignore!");
        config.addKey(String.valueOf(this.configPrefix) + "_channel_list_mode", "Select one of the two modes for the channel list.\nignore = Clients in the selected channels will be ignored.\nonly = Only clients in the selected channels receive the notify message.", "ignore");
        config.addKey(String.valueOf(this.configPrefix) + "_message_mode", "Select the message mode, how the notified clients should get the message.\npoke or chat are valid values!", "poke");
        config.addKey(String.valueOf(this.configPrefix) + "_messagenotified_mode", "Select the message mode, how the clients (who joined the server) should get the message.\npoke, chat or none are valid values!", "none");
        if (this.modClass.getMySQLConnection() == null) {
            config.addKey(String.valueOf(this.configPrefix) + "_file", "Path to file which contains the server group notify message", "config/server1/servergroupnotifymessages.cfg");
        }
        config.addKey(String.valueOf(this.configPrefix) + "_message", "Server Group Notify message, specified clients get this message as chat or poke message.\nYou can use the following keywords, which will be replaced:\n%SERVER_GROUP_ID% - Server Group ID\n%SERVER_GROUP_NAME% - Server Group Name\n%CLIENT_NAME% - Client Name\n%CLIENT_DBID% - Client Database ID\n%CLIENT_UNIQUEID% - Client Unique ID\nTypical BBCode like in Teamspeak 3 Client possible. You can use \\n for a new line.", this.modClass.getMySQLConnection() != null);
        config.addKey(String.valueOf(this.configPrefix) + "_messagenotified", "Server Group Notify message for the watched client.\nIf enabled, clients with a watched server group connecting the server get this message as chat or poke message.\nYou can use the following keywords, which will be replaced:\n%CLIENT_COUNT% - Number of target group clients who get informed about this client\n%CLIENT_NAMES% - List of names of target group clients who get informed about this client\nTypical BBCode like in Teamspeak 3 Client possible. You can use \\n for a new line.", this.modClass.getMySQLConnection() != null);
        config.addKey(String.valueOf(this.configPrefix) + "_messagenotnotified", "Server Group Notify message for the watched client, if no target group client is online\nIf enabled, clients with a watched server group connecting the server get this message as chat or poke message.\nTypical BBCode like in Teamspeak 3 Client possible. You can use \\n for a new line.", this.modClass.getMySQLConnection() != null);
    }

    @Override
    public boolean loadConfig(ArrangedPropertiesWriter config, boolean slowMode) throws BotConfigurationException, NumberFormatException {
        this.pluginEnabled = false;
        if (slowMode) {
            this.modClass.addLogEntry(this.configPrefix, (byte)3, "Slow Mode activated, Server Group Notify disabled!", true);
            return false;
        }
        String lastNumberValue = "";
        String temp = null;
        try {
            StringTokenizer st;
            this.SERVERGROUPNOTIFY_FILE = config.getValue(String.valueOf(this.configPrefix) + "_file");
            this.SERVERGROUPNOTIFY_MESSAGE_MODE = config.getValue(String.valueOf(this.configPrefix) + "_message_mode", "chat").trim();
            this.SERVERGROUPNOTIFY_MESSAGENOTIFIED_MODE = config.getValue(String.valueOf(this.configPrefix) + "_messagenotified_mode", "chat").trim();
            String[] configKeys = new String[]{String.valueOf(this.configPrefix) + "_message", String.valueOf(this.configPrefix) + "_messagenotified", String.valueOf(this.configPrefix) + "_messagenotnotified"};
            if (!this.modClass.loadMessages(this.configPrefix, "_file", configKeys)) {
                throw new BotConfigurationException("Server Group Notify messages could not be loaded!");
            }
            this.SERVERGROUPNOTIFY_MESSAGE = config.getValue(configKeys[0]);
            if (this.SERVERGROUPNOTIFY_MESSAGE == null || this.SERVERGROUPNOTIFY_MESSAGE.length() == 0) {
                throw new BotConfigurationException("Server Group Notify message could not be loaded!");
            }
            if (this.SERVERGROUPNOTIFY_MESSAGENOTIFIED_MODE.equalsIgnoreCase("poke") || this.SERVERGROUPNOTIFY_MESSAGENOTIFIED_MODE.equalsIgnoreCase("chat")) {
                this.SERVERGROUPNOTIFY_MESSAGENOTIFIED = config.getValue(configKeys[1]);
                if (this.SERVERGROUPNOTIFY_MESSAGENOTIFIED == null || this.SERVERGROUPNOTIFY_MESSAGENOTIFIED.length() == 0) {
                    throw new BotConfigurationException("Server Group Notify message for the watched client missing in config!");
                }
                this.SERVERGROUPNOTIFY_MESSAGENOTNOTIFIED = config.getValue(configKeys[2]);
                if (this.SERVERGROUPNOTIFY_MESSAGENOTNOTIFIED == null || this.SERVERGROUPNOTIFY_MESSAGENOTNOTIFIED.length() == 0) {
                    throw new BotConfigurationException("Server Group Notify message for the watched client (if no target group client is online) missing in config!");
                }
            }
            temp = null;
            this.SERVERGROUPNOTIFY_GROUPS.clear();
            temp = config.getValue(String.valueOf(this.configPrefix) + "_groups");
            lastNumberValue = String.valueOf(this.configPrefix) + "_groups";
            if (temp != null && temp.length() > 0) {
                st = new StringTokenizer(temp, ",", false);
                while (st.hasMoreTokens()) {
                    this.SERVERGROUPNOTIFY_GROUPS.addElement(Integer.parseInt(st.nextToken().trim()));
                }
            }
            temp = null;
            this.SERVERGROUPNOTIFY_GROUPTARGETS.clear();
            temp = config.getValue(String.valueOf(this.configPrefix) + "_grouptargets");
            lastNumberValue = String.valueOf(this.configPrefix) + "_grouptargets";
            if (temp != null && temp.length() > 0) {
                st = new StringTokenizer(temp, ",", false);
                while (st.hasMoreTokens()) {
                    this.SERVERGROUPNOTIFY_GROUPTARGETS.addElement(Integer.parseInt(st.nextToken().trim()));
                }
            }
            temp = null;
            this.SERVERGROUPNOTIFY_CHANNEL_LIST.clear();
            temp = config.getValue(String.valueOf(this.configPrefix) + "_channel_list");
            lastNumberValue = String.valueOf(this.configPrefix) + "_channel_list";
            if (temp != null && temp.length() > 0) {
                st = new StringTokenizer(temp, ",", false);
                while (st.hasMoreTokens()) {
                    this.SERVERGROUPNOTIFY_CHANNEL_LIST.addElement(Integer.parseInt(st.nextToken().trim()));
                }
            }
            this.SERVERGROUPNOTIFY_CHANNEL_LIST_IGNORE = !config.getValue(String.valueOf(this.configPrefix) + "_channel_list_mode", "ignore").trim().equalsIgnoreCase("only");
            if (this.SERVERGROUPNOTIFY_GROUPS.size() == 0 || this.SERVERGROUPNOTIFY_GROUPTARGETS.size() == 0) {
                throw new BotConfigurationException("No server groups in bot configuration set. Check config values of " + this.configPrefix + "_groups and " + this.configPrefix + "_grouptargets");
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
        Vector<HashMap<String, String>> clientList;
        int groupID;
        if (!this.pluginEnabled) {
            return;
        }
        if (eventType.equals("notifycliententerview") && eventInfo.get("client_type").equals("0") && (clientList = this.modClass.getClientList()) != null && (groupID = this.modClass.getListedGroup(eventInfo.get("client_servergroups"), this.SERVERGROUPNOTIFY_GROUPS)) > 0) {
            Vector<String> targetClients = new Vector<String>();
            Vector<String> targetClients_UID = new Vector<String>();
            String sgName = this.modClass.getServerGroupName(groupID);
            String sgnMessage = new String(this.SERVERGROUPNOTIFY_MESSAGE);
            sgnMessage = sgnMessage.replace("%SERVER_GROUP_ID%", Integer.toString(groupID));
            sgnMessage = sgnMessage.replace("%SERVER_GROUP_NAME%", sgName == null ? "Unknown" : sgName);
            sgnMessage = sgnMessage.replace("%CLIENT_NAME%", eventInfo.get("client_nickname"));
            sgnMessage = sgnMessage.replace("%CLIENT_NAME_CLICKABLE%", "[URL=client://0/" + eventInfo.get("client_unique_identifier") + "]" + eventInfo.get("client_nickname") + "[/URL]");
            sgnMessage = sgnMessage.replace("%CLIENT_DBID%", eventInfo.get("client_database_id"));
            if (!this.modClass.isMessageLengthValid(this.SERVERGROUPNOTIFY_MESSAGE_MODE, sgnMessage = sgnMessage.replace("%CLIENT_UNIQUEID%", eventInfo.get("client_unique_identifier")))) {
                this.modClass.addLogEntry(this.configPrefix, (byte)2, "Server Group Notify message is to long! Make sure that " + this.SERVERGROUPNOTIFY_MESSAGE_MODE + " messages are not longer than " + Short.toString(this.modClass.getMaxMessageLength(this.SERVERGROUPNOTIFY_MESSAGE_MODE)) + " characters (including spaces and BBCode), check file: " + this.SERVERGROUPNOTIFY_FILE, true);
            }
            int clientID = -1;
            for (HashMap<String, String> clientInfo : clientList) {
                if (!clientInfo.get("client_type").equals("0") || !this.modClass.isGroupListed(clientInfo.get("client_servergroups"), this.SERVERGROUPNOTIFY_GROUPTARGETS)) continue;
                boolean result = this.modClass.isIDListed(Integer.parseInt(clientInfo.get("cid")), this.SERVERGROUPNOTIFY_CHANNEL_LIST);
                if (!(this.SERVERGROUPNOTIFY_CHANNEL_LIST_IGNORE ? !result : result) || !this.modClass.sendMessageToClient(this.configPrefix, this.SERVERGROUPNOTIFY_MESSAGE_MODE, clientID = Integer.parseInt(clientInfo.get("clid")), sgnMessage)) continue;
                targetClients.addElement(clientInfo.get("client_nickname"));
                targetClients_UID.addElement(clientInfo.get("client_unique_identifier"));
            }
            if (this.SERVERGROUPNOTIFY_MESSAGENOTIFIED_MODE.equalsIgnoreCase("poke") || this.SERVERGROUPNOTIFY_MESSAGENOTIFIED_MODE.equalsIgnoreCase("chat")) {
                clientID = Integer.parseInt(eventInfo.get("clid"));
                if (targetClients.size() == 0) {
                    if (!this.modClass.isMessageLengthValid(this.SERVERGROUPNOTIFY_MESSAGENOTIFIED_MODE, this.SERVERGROUPNOTIFY_MESSAGENOTNOTIFIED)) {
                        this.modClass.addLogEntry(this.configPrefix, (byte)2, "Server Group Notify message for the watched client (if no target group client is online) is to long! Make sure that " + this.SERVERGROUPNOTIFY_MESSAGENOTIFIED_MODE + " messages are not longer than " + Short.toString(this.modClass.getMaxMessageLength(this.SERVERGROUPNOTIFY_MESSAGENOTIFIED_MODE)) + " characters (including spaces and BBCode)" + (this.modClass.getMySQLConnection() == null ? new StringBuilder(", check file: ").append(this.SERVERGROUPNOTIFY_FILE).toString() : "!"), true);
                    }
                    this.modClass.sendMessageToClient(this.configPrefix, this.SERVERGROUPNOTIFY_MESSAGENOTIFIED_MODE, clientID, this.SERVERGROUPNOTIFY_MESSAGENOTNOTIFIED);
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
                    String cnMessageNotified = new String(this.SERVERGROUPNOTIFY_MESSAGENOTIFIED);
                    cnMessageNotified = cnMessageNotified.replace("%CLIENT_COUNT%", Integer.toString(targetClients.size()));
                    cnMessageNotified = cnMessageNotified.replace("%CLIENT_NAMES%", sb.toString());
                    if (!this.modClass.isMessageLengthValid(this.SERVERGROUPNOTIFY_MESSAGENOTIFIED_MODE, cnMessageNotified = cnMessageNotified.replace("%CLIENT_NAMES_CLICKABLE%", sbURL.toString()))) {
                        this.modClass.addLogEntry(this.configPrefix, (byte)2, "Server Group Notify message for the watched client is to long! Make sure that " + this.SERVERGROUPNOTIFY_MESSAGENOTIFIED_MODE + " messages are not longer than " + Short.toString(this.modClass.getMaxMessageLength(this.SERVERGROUPNOTIFY_MESSAGENOTIFIED_MODE)) + " characters (including spaces and BBCode)" + (this.modClass.getMySQLConnection() == null ? new StringBuilder(", check file: ").append(this.SERVERGROUPNOTIFY_FILE).toString() : "!"), true);
                    }
                    this.modClass.sendMessageToClient(this.configPrefix, this.SERVERGROUPNOTIFY_MESSAGENOTIFIED_MODE, clientID, cnMessageNotified);
                }
            }
        }
    }
}

