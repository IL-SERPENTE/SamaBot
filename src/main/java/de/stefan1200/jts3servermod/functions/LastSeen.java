/*
 * Decompiled with CFR 0_110.
 */
package de.stefan1200.jts3servermod.functions;

import de.stefan1200.jts3servermod.BotConfigurationException;
import de.stefan1200.jts3servermod.interfaces.ClientDatabaseCache_Interface;
import de.stefan1200.jts3servermod.interfaces.HandleBotEvents;
import de.stefan1200.jts3servermod.interfaces.HandleTS3Events;
import de.stefan1200.jts3servermod.interfaces.JTS3ServerMod_Interface;
import de.stefan1200.jts3servermod.interfaces.LoadConfiguration;
import de.stefan1200.jts3serverquery.JTS3ServerQuery;
import de.stefan1200.util.ArrangedPropertiesWriter;
import java.util.BitSet;
import java.util.HashMap;
import java.util.Iterator;
import java.util.StringTokenizer;
import java.util.Vector;

/*
 * This class specifies class file version 49.0 but uses Java 6 signatures.  Assumed Java 6.
 */
public class LastSeen
implements HandleTS3Events,
LoadConfiguration,
HandleBotEvents {
    private String configPrefix = "";
    private JTS3ServerMod_Interface modClass = null;
    private JTS3ServerQuery queryLib = null;
    private ClientDatabaseCache_Interface clientCache = null;
    private boolean pluginEnabled = false;
    static final byte COMMAND_LASTSEEN_ALL = 10;
    static final byte COMMAND_LASTSEEN_SERVERGROUPS = 5;
    static final byte COMMAND_LASTSEEN_BOTADMIN = 1;
    private byte COMMAND_LASTSEEN_MODE = 0;
    private Vector<Integer> COMMAND_LASTSEEN_GROUP_LIST = new Vector<>();
    private boolean COMMAND_LASTSEEN_GROUP_LIST_IGNORE = true;

    @Override
    public void initClass(JTS3ServerMod_Interface modClass, JTS3ServerQuery queryLib, String prefix) {
        this.modClass = modClass;
        this.queryLib = queryLib;
        this.configPrefix = prefix.trim();
    }

    @Override
    public boolean multipleInstances() {
        return false;
    }

    @Override
    public void handleOnBotConnect() {
        if (!this.pluginEnabled) {
            return;
        }
        this.clientCache = this.modClass.getClientCache();
        String group = "everyone";
        if (this.COMMAND_LASTSEEN_MODE == 1) {
            group = "bot admins";
        }
        if (this.COMMAND_LASTSEEN_MODE == 5) {
            group = "specified server groups";
        }
        String msg = "Check the last seen time of a client with the command !" + this.configPrefix + ", this can be used by " + group + "!";
        this.modClass.addLogEntry(this.configPrefix, (byte)1, msg, true);
    }

    @Override
    public void handleAfterCacheUpdate() {
    }

    @Override
    public void activate() {
        if (this.clientCache == null) {
            this.clientCache = this.modClass.getClientCache();
        }
    }

    @Override
    public void disable() {
    }

    @Override
    public void unload() {
        this.COMMAND_LASTSEEN_GROUP_LIST = null;
    }

    @Override
    public void initConfig(ArrangedPropertiesWriter config) {
        config.addKey(String.valueOf(this.configPrefix) + "_user", "Who should be able to use the last seen command? Possible values: all, botadmin, servergroup\nThis command only works if the bot_clientdblist_cache is enabled!", "botadmin");
        config.addKey(String.valueOf(this.configPrefix) + "_group_list", "A comma separated list (without spaces) of server group ids.\nDepends on the given mode, this server groups can be ignored or only this server groups will be allowed to use the !lastseen command!\nThis is only needed, if user servergroup is selected!");
        config.addKey(String.valueOf(this.configPrefix) + "_group_list_mode", "Select one of the two modes for the server group list.\nignore = The selected server groups will be ignored.\nonly = Only the selected server groups are allowed to use the !lastseen command!", "ignore");
    }

    @Override
    public boolean loadConfig(ArrangedPropertiesWriter config, boolean slowMode) throws BotConfigurationException, NumberFormatException {
        String lastNumberValue = "";
        String temp = null;
        this.pluginEnabled = false;
        try {
            temp = config.getValue(String.valueOf(this.configPrefix) + "_user", "botadmin").trim();
            if (temp.equals("all")) {
                this.COMMAND_LASTSEEN_MODE = 10;
            } else if (temp.equals("botadmin")) {
                this.COMMAND_LASTSEEN_MODE = 1;
            } else if (temp.equals("servergroup")) {
                this.COMMAND_LASTSEEN_MODE = 5;
                this.COMMAND_LASTSEEN_GROUP_LIST_IGNORE = !config.getValue(String.valueOf(this.configPrefix) + "_group_list_mode", "ignore").trim().equalsIgnoreCase("only");
                temp = null;
                this.COMMAND_LASTSEEN_GROUP_LIST.clear();
                temp = config.getValue(String.valueOf(this.configPrefix) + "_group_list");
                lastNumberValue = String.valueOf(this.configPrefix) + "_group_list";
                if (temp != null && temp.length() > 0) {
                    StringTokenizer st = new StringTokenizer(temp, ",", false);
                    while (st.hasMoreTokens()) {
                        this.COMMAND_LASTSEEN_GROUP_LIST.addElement(Integer.parseInt(st.nextToken().trim()));
                    }
                }
            }
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
    public String[] botChatCommandList(HashMap<String, String> eventInfo, boolean isFullAdmin, boolean isAdmin) {
        if (!this.pluginEnabled) {
            return null;
        }
        if (isFullAdmin || isAdmin || this.isLastSeenAllowed(eventInfo.get("invokerid"))) {
            String[] commands = new String[]{"<search string>"};
            return commands;
        }
        return null;
    }

    @Override
    public String botChatCommandHelp(String command) {
        return "Shows the last online time of a client. Use * as a wildcard.\nExample: !" + this.configPrefix + " *foo*bar*";
    }

    @Override
    public boolean handleChatCommands(String clientname, HashMap<String, String> eventInfo, boolean isFullAdmin, boolean isAdmin) {
        block18 : {
            if (!this.pluginEnabled) {
                return false;
            }
            try {
                if (isFullAdmin || isAdmin || this.isLastSeenAllowed(eventInfo.get("invokerid"))) {
                    if (this.clientCache == null) {
                        this.queryLib.sendTextMessage(Integer.parseInt(eventInfo.get("invokerid")), 1, "Client database cache disabled, command disabled!");
                        break block18;
                    }
                    if (clientname.length() < 1) {
                        this.queryLib.sendTextMessage(Integer.parseInt(eventInfo.get("invokerid")), 1, "Wrong usage! Right: !" + this.configPrefix + " <clientname>\nYou can use * as wildcard!");
                        break block18;
                    }
                    if (clientname.indexOf("**") != -1) {
                        this.queryLib.sendTextMessage(Integer.parseInt(eventInfo.get("invokerid")), 1, "Wrong usage, only single wildcards are allowed!");
                        break block18;
                    }
                    if (this.clientCache.isUpdateRunning()) {
                        this.queryLib.sendTextMessage(Integer.parseInt(eventInfo.get("invokerid")), 1, "Client database cache is updating, please wait some time and try again!");
                        break block18;
                    }
                    Vector<Integer> clientSearch = this.clientCache.searchClientNickname(clientname);
                    if (clientSearch == null) {
                        this.queryLib.sendTextMessage(Integer.parseInt(eventInfo.get("invokerid")), 1, "Wrong usage, use a valid search pattern with at least 3 characters!");
                        break block18;
                    }
                    if (clientSearch.size() == 0) {
                        this.queryLib.sendTextMessage(Integer.parseInt(eventInfo.get("invokerid")), 1, "No clients found in the database!");
                        break block18;
                    }
                    if (clientSearch.size() > 10) {
                        this.queryLib.sendTextMessage(Integer.parseInt(eventInfo.get("invokerid")), 1, "Found " + Integer.toString(clientSearch.size()) + " entries in the database, please refine your search.");
                        break block18;
                    }
                    StringBuffer sb = new StringBuffer("Found " + Integer.toString(clientSearch.size()) + " entries in the database:\n");
                    Vector<HashMap<String, String>> clientList = this.modClass.getClientList();
                    boolean foundClient = false;
                    Iterator<Integer> iterator = clientSearch.iterator();
                    while (iterator.hasNext()) {
                        int clientDBID = iterator.next();
                        for (HashMap<String, String> clientOnline : clientList) {
                            if (clientDBID != Integer.parseInt(clientOnline.get("client_database_id")) || Integer.parseInt(clientOnline.get("client_type")) != 0) continue;
                            if (clientOnline.get("clid").equals(eventInfo.get("invokerid"))) {
                                sb.append("[b]" + clientOnline.get("client_nickname") + "[/b] need a mirror :)\n");
                            } else {
                                sb.append("[b]" + clientOnline.get("client_nickname") + "[/b] is currently online\n");
                            }
                            foundClient = true;
                            break;
                        }
                        if (!foundClient) {
                            try {
                                long lastOnline = (long)this.clientCache.getLastOnline(clientDBID) * 1000;
                                sb.append("[b]" + this.clientCache.getNickname(clientDBID) + "[/b] was last seen at " + this.modClass.getStringFromTimestamp(lastOnline) + "\n");
                            }
                            catch (Exception e) {
                                e.printStackTrace();
                            }
                        }
                        foundClient = false;
                    }
                    this.queryLib.sendTextMessage(Integer.parseInt(eventInfo.get("invokerid")), 1, sb.toString());
                    break block18;
                }
                this.queryLib.sendTextMessage(Integer.parseInt(eventInfo.get("invokerid")), 1, "You are not allowed to use this command!");
            }
            catch (Exception e) {
                this.modClass.addLogEntry(this.configPrefix, e, false);
            }
        }
        return true;
    }

    @Override
    public void handleClientEvents(String eventType, HashMap<String, String> eventInfo) {
    }

    @Override
    public void setListModes(BitSet listOptions) {
        listOptions.set(1);
    }

    private boolean isLastSeenAllowed(String clientID) {
        if (this.COMMAND_LASTSEEN_MODE == 10) {
            return true;
        }
        if (this.COMMAND_LASTSEEN_MODE == 5) {
            Vector<HashMap<String, String>> clientList = this.modClass.getClientList();
            if (this.COMMAND_LASTSEEN_MODE == 5 && clientList != null) {
                for (HashMap<String, String> clientInfo : clientList) {
                    if (!clientInfo.get("clid").equals(clientID)) continue;
                    boolean result = this.modClass.isGroupListed(clientInfo.get("client_servergroups"), this.COMMAND_LASTSEEN_GROUP_LIST);
                    if (this.COMMAND_LASTSEEN_GROUP_LIST_IGNORE ? !result : result) {
                        return true;
                    }
                    return false;
                }
            }
        }
        return false;
    }
}

