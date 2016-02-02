/*
 * Decompiled with CFR 0_110.
 */
package de.stefan1200.jts3servermod.functions;

import java.util.BitSet;
import java.util.HashMap;
import java.util.StringTokenizer;
import java.util.Vector;

import de.stefan1200.jts3servermod.BotConfigurationException;
import de.stefan1200.jts3servermod.FunctionExceptionLog;
import de.stefan1200.jts3servermod.interfaces.HandleBotEvents;
import de.stefan1200.jts3servermod.interfaces.HandleClientList;
import de.stefan1200.jts3servermod.interfaces.JTS3ServerMod_Interface;
import de.stefan1200.jts3servermod.interfaces.LoadConfiguration;
import de.stefan1200.jts3serverquery.JTS3ServerQuery;
import de.stefan1200.jts3serverquery.TS3ServerQueryException;
import de.stefan1200.util.ArrangedPropertiesWriter;

/*
 * This class specifies class file version 49.0 but uses Java 6 signatures.  Assumed Java 6.
 */
public class IdleCheck
implements LoadConfiguration,
HandleClientList,
HandleBotEvents {
    private String configPrefix = "";
    private JTS3ServerMod_Interface modClass = null;
    private JTS3ServerQuery queryLib = null;
    private boolean pluginEnabled = false;
    private boolean IDLE_KICK = false;
    private Vector<Integer> IDLE_CHANNEL_LIST = new Vector<>();
    private boolean IDLE_CHANNEL_LIST_IGNORE = true;
    private Vector<Integer> IDLE_GROUP_LIST = new Vector<>();
    private boolean IDLE_GROUP_LIST_IGNORE = true;
    private int IDLE_MOVE_CHANNELID = -1;
    private String IDLE_MESSAGE_MODE = null;
    private String IDLE_MESSAGE = null;
    private String IDLE_SECOND_MESSAGE = null;
    private long IDLE_MAX_TIME = -1;
    private long IDLE_SECOND_MAX_TIME = -1;
    private String IDLE_WARN_MESSAGE_MODE = null;
    private String IDLE_WARN_MESSAGE = null;
    private long IDLE_WARN_TIME = -1;
    private int IDLE_MIN_CLIENTS = 0;
    private boolean IDLE_MIN_CLIENTS_MODE_CHANNEL = true;
    private Vector<Integer> IDLE_CLIENTS_WARN_SENT = new Vector<>();
    private Vector<Integer> idleClientsWarnSentTemp = new Vector<>();
    private boolean IDLE_MOVE_BACK = false;
    private Vector<Integer> IDLE_CLIENTS_MOVED = new Vector<>();
    private Vector<Integer> IDLE_CLIENTS_MOVED_CHANNEL = new Vector<>();
    private String IDLE_FILE = null;
    private String idleMessage = null;
    private String idleSecondMessage = null;
    private String idleWarnMessage = null;
    private String channelName = null;
    private FunctionExceptionLog fel = new FunctionExceptionLog();
    private Vector<HashMap<String, String>> clientListCacheCurrent = new Vector<>();
    private Vector<HashMap<String, String>> clientListCacheOld = new Vector<>();

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
    public void activate() {
    }

    @Override
    public void disable() {
    }

    @Override
    public void unload() {
        this.IDLE_CHANNEL_LIST = null;
        this.IDLE_GROUP_LIST = null;
        this.IDLE_CLIENTS_WARN_SENT = null;
        this.idleClientsWarnSentTemp = null;
        this.IDLE_CLIENTS_MOVED = null;
        this.IDLE_CLIENTS_MOVED_CHANNEL = null;
    }

    @Override
    public void handleAfterCacheUpdate() {
        if (!this.pluginEnabled) {
            return;
        }
        if (!this.IDLE_KICK) {
            this.channelName = this.modClass.getChannelName(this.IDLE_MOVE_CHANNELID);
            if (this.channelName == null) {
                this.modClass.addLogEntry(this.configPrefix, (byte)3, "Critical: Channel ID " + this.IDLE_MOVE_CHANNELID + " don't exists! Check value " + this.configPrefix + "_channel_id in your configuration!", true);
                this.pluginEnabled = false;
                return;
            }
        }
        if (this.modClass.getClientList() != null) {
            this.clientListCacheOld = this.clientListCacheCurrent;
            this.clientListCacheCurrent = this.modClass.getClientList();
        }
        this.createMessage();
    }

    @Override
    public void handleOnBotConnect() {
        String msg;
        if (!this.pluginEnabled) {
            return;
        }
        if (this.IDLE_WARN_TIME > 0) {
            msg = "Clients get a warning message after being idle for " + Long.toString(this.IDLE_WARN_TIME / 1000 / 60) + " minutes";
            this.modClass.addLogEntry(this.configPrefix, (byte)1, msg, true);
        }
        if (this.IDLE_KICK) {
            msg = "Clients will be kicked from server after being idle for " + Long.toString(this.IDLE_MAX_TIME / 1000 / 60) + " minutes" + (this.IDLE_MIN_CLIENTS > 0 ? new StringBuilder(" (if min ").append(Integer.toString(this.IDLE_MIN_CLIENTS)).append(" clients online)").toString() : "");
            this.modClass.addLogEntry(this.configPrefix, (byte)1, msg, true);
        } else {
            msg = "Clients will be moved into Channel \"" + this.channelName + "\" (id: " + Integer.toString(this.IDLE_MOVE_CHANNELID) + ") after being idle for " + Long.toString(this.IDLE_MAX_TIME / 1000 / 60) + " minutes" + (this.IDLE_MIN_CLIENTS > 0 ? new StringBuilder(" (if min ").append(Integer.toString(this.IDLE_MIN_CLIENTS)).append(" clients online)").toString() : "") + (this.IDLE_MOVE_BACK ? ". Clients will be moved back, if not idle anymore." : ".");
            this.modClass.addLogEntry(this.configPrefix, (byte)1, msg, true);
            if (this.IDLE_SECOND_MAX_TIME > 0) {
                msg = "Clients will be kicked from server after being idle for " + Long.toString(this.IDLE_SECOND_MAX_TIME / 1000 / 60) + " minutes" + (this.IDLE_MIN_CLIENTS > 0 ? new StringBuilder(" (if min ").append(Integer.toString(this.IDLE_MIN_CLIENTS)).append(" clients online)").toString() : "");
                this.modClass.addLogEntry(this.configPrefix, (byte)1, msg, true);
            }
        }
    }

    @Override
    public void initConfig(ArrangedPropertiesWriter config) {
        config.addKey(String.valueOf(this.configPrefix) + "_action", "Select the action if a client is idle for more than the specified maximum time, possible values: kick and move", "move");
        config.addKey(String.valueOf(this.configPrefix) + "_moveback", "If clients got moved, move client back if not idle anymore? Set yes or no here!", "no");
        config.addKey(String.valueOf(this.configPrefix) + "_channel_id", "If clients should be moved, set channel id to move idle clients into it.");
        config.addKey(String.valueOf(this.configPrefix) + "_channel_list", "A comma separated list (without spaces) of channel ids.\nDepends on the given mode, this channels can be ignored or only this channels will be checked!\nIf no channels should be ignored, set no channels here and select the channel list mode ignore!");
        config.addKey(String.valueOf(this.configPrefix) + "_channel_list_mode", "Select one of the two modes for the channel list.\nignore = The selected channels will be ignored.\nonly = Only the selected channels will be checked.", "ignore");
        config.addKey(String.valueOf(this.configPrefix) + "_group_list", "A comma separated list (without spaces) of server group ids.\nDepends on the given mode, this server groups can be ignored or only this server groups will be checked!\nIf no server groups should be ignored, set no server groups here and select the group list mode ignore!");
        config.addKey(String.valueOf(this.configPrefix) + "_group_list_mode", "Select one of the two modes for the server group list.\nignore = The selected server groups will be ignored.\nonly = Only the selected server groups will be checked.", "ignore");
        config.addKey(String.valueOf(this.configPrefix) + "_max_time", "Set the max idle time in minutes.\nIf clients should be kicked, the client will be kicked after being idle for this time.\nIf clients should be moved, the client will be moved to specified channel after being idle for this time!", "60");
        config.addKey(String.valueOf(this.configPrefix) + "_second_max_time", "If clients should be moved, set the max idle time in minutes to kick someone.\nHas to be greater than idle_max_time or -1 to disable this feature!", "-1");
        config.addKey(String.valueOf(this.configPrefix) + "_warn_time", "Set the idle warn time in minutes or set -1 to disable this feature.\nThe idle warn time has to be smaller than the max idle time", "-1");
        config.addKey(String.valueOf(this.configPrefix) + "_min_clients", "A minimum client count to activate the idle check (Query clients are not counted).\nIf less clients are in the channel or on the server, idle check does nothing.", "3");
        config.addKey(String.valueOf(this.configPrefix) + "_min_clients_mode", "Select if the minimum client count is needed in the channel or on the server.\nchannel or server are valid values!", "server");
        config.addKey(String.valueOf(this.configPrefix) + "_message_mode", "If clients should be moved, select the message mode, how the client should get the message.\npoke, chat or none are valid values!", "chat");
        config.addKey(String.valueOf(this.configPrefix) + "_warn_message_mode", "Select the message mode, how the client should get the message.\npoke or chat are valid values!", "chat");
        if (this.modClass.getMySQLConnection() == null) {
            config.addKey(String.valueOf(this.configPrefix) + "_file", "Path to file which contains the idle messages", "config/server1/idlemessages.cfg");
        }
        config.addKey(String.valueOf(this.configPrefix) + "_message", "Idle message, the client get this message as kick reason or chat message.\nYou can use the following keywords, which will be replaced:\n%IDLE_MAX_TIME% - Replaced with max idle time\n%IDLE_CHANNEL_NAME% - If clients should be moved, this will be replaced with the channel name of target idle channel\nTypical BBCode like in Teamspeak 3 Client possible. You can use \\n for a new line.", this.modClass.getMySQLConnection() != null);
        config.addKey(String.valueOf(this.configPrefix) + "_second_message", "If clients should be moved first, set the kick reason for being idle longer than the second idle max time.\nYou can use the following keywords, which will be replaced:\n%IDLE_MAX_TIME% - Replaced with max idle time\n%IDLE_SECOND_MAX_TIME% - This will be replaced with the second idle max time\nTypical BBCode like in Teamspeak 3 Client possible. You can use \\n for a new line.", this.modClass.getMySQLConnection() != null);
        config.addKey(String.valueOf(this.configPrefix) + "_warn_message", "Idle warning message, the client get this message as chat message.\nYou can use the following keywords, which will be replaced:\n%IDLE_WARN_TIME% - Replaced with idle warn time\n%IDLE_MAX_TIME% - Replaced with max idle time\n%IDLE_CHANNEL_NAME% - If clients should be moved, this will be replaced with the channel name of target idle channel", this.modClass.getMySQLConnection() != null);
    }

    @Override
    public boolean loadConfig(ArrangedPropertiesWriter config, boolean slowMode) throws BotConfigurationException, NumberFormatException {
        String lastNumberValue = "";
        String temp = null;
        this.pluginEnabled = false;
        try {
            StringTokenizer st;
            this.IDLE_KICK = config.getValue(String.valueOf(this.configPrefix) + "_action", "move").trim().equalsIgnoreCase("kick");
            lastNumberValue = String.valueOf(this.configPrefix) + "_max_time";
            temp = config.getValue(String.valueOf(this.configPrefix) + "_max_time");
            if (temp == null) {
                throw new NumberFormatException();
            }
            this.IDLE_MAX_TIME = Long.parseLong(temp.trim()) * 60 * 1000;
            lastNumberValue = String.valueOf(this.configPrefix) + "_min_clients";
            this.IDLE_MIN_CLIENTS = Integer.parseInt(config.getValue(String.valueOf(this.configPrefix) + "_min_clients", "0").trim());
            this.IDLE_MIN_CLIENTS_MODE_CHANNEL = config.getValue(String.valueOf(this.configPrefix) + "_min_clients_mode", "server").trim().equalsIgnoreCase("channel");
            this.IDLE_MESSAGE_MODE = this.IDLE_KICK ? "kick" : config.getValue(String.valueOf(this.configPrefix) + "_message_mode", "chat").trim();
            lastNumberValue = String.valueOf(this.configPrefix) + "_warn_time";
            this.IDLE_WARN_TIME = Long.parseLong(config.getValue(String.valueOf(this.configPrefix) + "_warn_time", "-1").trim());
            if (this.IDLE_WARN_TIME > 0) {
                this.IDLE_WARN_MESSAGE_MODE = config.getValue(String.valueOf(this.configPrefix) + "_warn_message_mode", "chat").trim();
            }
            this.IDLE_FILE = config.getValue(String.valueOf(this.configPrefix) + "_file");
            String[] configKeys = new String[]{String.valueOf(this.configPrefix) + "_message", String.valueOf(this.configPrefix) + "_second_message", String.valueOf(this.configPrefix) + "_warn_message"};
            if (!this.modClass.loadMessages(this.configPrefix, "_file", configKeys)) {
                throw new BotConfigurationException("Idle Check messages could not be loaded!");
            }
            this.IDLE_MESSAGE = config.getValue(configKeys[0]);
            this.IDLE_SECOND_MESSAGE = config.getValue(configKeys[1]);
            this.IDLE_WARN_MESSAGE = config.getValue(configKeys[2]);
            if (this.IDLE_MESSAGE == null || this.IDLE_MESSAGE.length() == 0) {
                throw new BotConfigurationException("Idle Check message missing in config!");
            }
            if (!this.IDLE_KICK) {
                lastNumberValue = String.valueOf(this.configPrefix) + "_channel_id";
                temp = config.getValue(String.valueOf(this.configPrefix) + "_channel_id");
                if (temp == null) {
                    throw new NumberFormatException();
                }
                this.IDLE_MOVE_CHANNELID = Integer.parseInt(temp.trim());
                this.IDLE_MOVE_BACK = config.getValue(String.valueOf(this.configPrefix) + "_moveback", "no").trim().equalsIgnoreCase("yes");
                lastNumberValue = String.valueOf(this.configPrefix) + "_second_max_time";
                this.IDLE_SECOND_MAX_TIME = Long.parseLong(config.getValue(String.valueOf(this.configPrefix) + "_second_max_time", "-1").trim());
                if (this.IDLE_SECOND_MAX_TIME > 0) {
                    this.IDLE_SECOND_MAX_TIME = this.IDLE_SECOND_MAX_TIME * 60 * 1000;
                    if (this.IDLE_SECOND_MAX_TIME <= this.IDLE_MAX_TIME) {
                        throw new BotConfigurationException(String.valueOf(this.configPrefix) + "_second_max_time must be greater than " + this.configPrefix + "_max_time!");
                    }
                    if (this.IDLE_SECOND_MESSAGE == null || this.IDLE_SECOND_MESSAGE.length() == 0) {
                        throw new BotConfigurationException("Second Idle Check message missing in config!");
                    }
                }
            }
            if (this.IDLE_WARN_TIME > 0) {
                this.IDLE_WARN_TIME = this.IDLE_WARN_TIME * 60 * 1000;
                if (this.IDLE_WARN_TIME >= this.IDLE_MAX_TIME) {
                    throw new BotConfigurationException(String.valueOf(this.configPrefix) + "_max_time must be greater than " + this.configPrefix + "_warn_time!");
                }
                if (this.IDLE_WARN_MESSAGE == null || this.IDLE_WARN_MESSAGE.length() == 0) {
                    throw new BotConfigurationException("Idle Check warn message missing in config!");
                }
            }
            this.IDLE_CLIENTS_WARN_SENT.clear();
            temp = null;
            this.IDLE_CHANNEL_LIST.clear();
            temp = config.getValue(String.valueOf(this.configPrefix) + "_channel_list");
            lastNumberValue = String.valueOf(this.configPrefix) + "_channel_list";
            if (temp != null && temp.length() > 0) {
                st = new StringTokenizer(temp, ",", false);
                while (st.hasMoreTokens()) {
                    this.IDLE_CHANNEL_LIST.addElement(Integer.parseInt(st.nextToken().trim()));
                }
            }
            temp = null;
            this.IDLE_GROUP_LIST.clear();
            temp = config.getValue(String.valueOf(this.configPrefix) + "_group_list");
            lastNumberValue = String.valueOf(this.configPrefix) + "_group_list";
            if (temp != null && temp.length() > 0) {
                st = new StringTokenizer(temp, ",", false);
                while (st.hasMoreTokens()) {
                    this.IDLE_GROUP_LIST.addElement(Integer.parseInt(st.nextToken().trim()));
                }
            }
            this.IDLE_CHANNEL_LIST_IGNORE = !config.getValue(String.valueOf(this.configPrefix) + "_channel_list_mode", "ignore").trim().equalsIgnoreCase("only");
            this.IDLE_GROUP_LIST_IGNORE = !config.getValue(String.valueOf(this.configPrefix) + "_group_list_mode", "ignore").trim().equalsIgnoreCase("only");
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
        listOptions.set(3);
        listOptions.set(5);
        listOptions.set(0);
    }

    private int getClientCountFromClientList(Vector<HashMap<String, String>> clientList, String channelID) {
        int count = 0;
        int i = 0;
        while (i < clientList.size()) {
            if (clientList.elementAt(i).get("client_type").equals("0") && (!this.IDLE_MIN_CLIENTS_MODE_CHANNEL || clientList.elementAt(i).get("cid").equals(channelID))) {
                ++count;
            }
            ++i;
        }
        return count;
    }

    @Override
    public void handleClientCheck(Vector<HashMap<String, String>> clientList) {
        if (!this.pluginEnabled) {
            return;
        }
        for (HashMap<String, String> clientInfo : clientList) {
            long idleTime;
            int cachePos;
            int clientID;
            int channelID;
            boolean result;
            if (!clientInfo.get("client_type").equals("0")) continue;
            clientID = Integer.parseInt(clientInfo.get("clid"));
            cachePos = -1;
            if (this.IDLE_MOVE_BACK) {
                cachePos = this.IDLE_CLIENTS_MOVED.indexOf(clientID);
            }
            idleTime = this.modClass.getIdleTime(clientInfo, this.IDLE_MOVE_CHANNELID);
            if (this.getClientCountFromClientList(this.clientListCacheOld, clientInfo.get("cid")) < this.IDLE_MIN_CLIENTS || this.getClientCountFromClientList(clientList, clientInfo.get("cid")) < this.IDLE_MIN_CLIENTS) continue;
            if (idleTime > this.IDLE_MAX_TIME) {
                channelID = Integer.parseInt(clientInfo.get("cid"));
                result = this.modClass.isIDListed(channelID, this.IDLE_CHANNEL_LIST);
                if (this.IDLE_KICK) {
                    if (this.IDLE_CHANNEL_LIST_IGNORE ? !result : result) {
                        result = this.modClass.isGroupListed(clientInfo.get("client_servergroups"), this.IDLE_GROUP_LIST);
                        if (this.IDLE_GROUP_LIST_IGNORE ? !result : result) {
                            this.kickClient(clientInfo, this.idleMessage, clientID);
                        }
                    }
                } else if (this.IDLE_SECOND_MAX_TIME > 0 && idleTime > this.IDLE_SECOND_MAX_TIME) {
                    if (this.IDLE_CHANNEL_LIST_IGNORE ? !result : result) {
                        result = this.modClass.isGroupListed(clientInfo.get("client_servergroups"), this.IDLE_GROUP_LIST);
                        if (this.IDLE_GROUP_LIST_IGNORE ? !result : result) {
                            this.kickClient(clientInfo, this.idleSecondMessage, clientID);
                        }
                    }
                } else if (channelID != this.IDLE_MOVE_CHANNELID && (this.IDLE_CHANNEL_LIST_IGNORE ? !result : result)) {
                    result = this.modClass.isGroupListed(clientInfo.get("client_servergroups"), this.IDLE_GROUP_LIST);
                    if (this.IDLE_GROUP_LIST_IGNORE ? !result : result) {
                        try {
                            this.queryLib.moveClient(clientID, this.IDLE_MOVE_CHANNELID, null);
                            if (this.IDLE_MOVE_BACK) {
                                this.IDLE_CLIENTS_MOVED.addElement(clientID);
                                this.IDLE_CLIENTS_MOVED_CHANNEL.addElement(channelID);
                            }
                            this.modClass.addLogEntry(this.configPrefix, (byte)1, "Client \"" + clientInfo.get("client_nickname") + "\" (db id: " + clientInfo.get("client_database_id") + ") was idle, client was moved and got a message!", false);
                            this.modClass.sendMessageToClient(this.configPrefix, this.IDLE_MESSAGE_MODE, clientID, this.idleMessage);
                            this.fel.clearException(Integer.parseInt(clientInfo.get("client_database_id")));
                        }
                        catch (TS3ServerQueryException sqe) {
                            if (!this.fel.existsException(sqe, Integer.parseInt(clientInfo.get("client_database_id")))) {
                                this.fel.addException(sqe, Integer.parseInt(clientInfo.get("client_database_id")));
                                this.modClass.addLogEntry(this.configPrefix, (byte)3, "Client status of \"" + clientInfo.get("client_nickname") + "\" (db id: " + clientInfo.get("client_database_id") + ") is idle, but an error occurred while moving client!", false);
                                this.modClass.addLogEntry(this.configPrefix, sqe, false);
                            }
                        }
                        catch (Exception e) {
                            this.modClass.addLogEntry(this.configPrefix, (byte)3, "Client status of \"" + clientInfo.get("client_nickname") + "\" (db id: " + clientInfo.get("client_database_id") + ") is idle, but an error occurred while moving client!", false);
                            this.modClass.addLogEntry(this.configPrefix, e, false);
                        }
                    }
                }
            } else if (this.IDLE_WARN_TIME > 0 && idleTime > this.IDLE_WARN_TIME) {
                if (this.IDLE_CLIENTS_WARN_SENT.indexOf(clientID) == -1) {
                    channelID = Integer.parseInt(clientInfo.get("cid"));
                    result = this.modClass.isIDListed(channelID, this.IDLE_CHANNEL_LIST);
                    if (channelID != this.IDLE_MOVE_CHANNELID && (this.IDLE_CHANNEL_LIST_IGNORE ? !result : result)) {
                        result = this.modClass.isGroupListed(clientInfo.get("client_servergroups"), this.IDLE_GROUP_LIST);
                        if (this.IDLE_GROUP_LIST_IGNORE ? !result : result) {
                            if (this.modClass.sendMessageToClient(this.configPrefix, this.IDLE_WARN_MESSAGE_MODE, clientID, this.idleWarnMessage)) {
                                this.idleClientsWarnSentTemp.addElement(clientID);
                            } else {
                                this.modClass.addLogEntry(this.configPrefix, (byte)3, "Unable to send idle warn message, maybe an invalid message mode in config file?", false);
                            }
                        }
                    }
                } else {
                    this.idleClientsWarnSentTemp.addElement(clientID);
                }
            }
            if (cachePos == -1 || idleTime >= (long)(this.modClass.getCheckInterval() * 2000) || !clientInfo.get("client_away").equals("0") || !clientInfo.get("client_output_muted").equals("0") || !clientInfo.get("client_input_muted").equals("0") || !clientInfo.get("client_output_hardware").equals("1") || !clientInfo.get("client_input_hardware").equals("1")) continue;
            if (Integer.parseInt(clientInfo.get("cid")) != this.IDLE_MOVE_CHANNELID) {
                this.IDLE_CLIENTS_MOVED.removeElementAt(cachePos);
                this.IDLE_CLIENTS_MOVED_CHANNEL.removeElementAt(cachePos);
                continue;
            }
            try {
                this.queryLib.moveClient(clientID, this.IDLE_CLIENTS_MOVED_CHANNEL.elementAt(cachePos), null);
                this.IDLE_CLIENTS_MOVED.removeElementAt(cachePos);
                this.IDLE_CLIENTS_MOVED_CHANNEL.removeElementAt(cachePos);
                this.modClass.addLogEntry(this.configPrefix, (byte)1, "Client \"" + clientInfo.get("client_nickname") + "\" (db id: " + clientInfo.get("client_database_id") + ") is not idle anymore, client was moved back!", false);
                this.fel.clearException(Integer.parseInt(clientInfo.get("client_database_id")));
                continue;
            }
            catch (TS3ServerQueryException sqe) {
                if (this.fel.existsException(sqe, Integer.parseInt(clientInfo.get("client_database_id")))) continue;
                this.fel.addException(sqe, Integer.parseInt(clientInfo.get("client_database_id")));
                this.modClass.addLogEntry(this.configPrefix, (byte)3, "Client status of \"" + clientInfo.get("client_nickname") + "\" (db id: " + clientInfo.get("client_database_id") + ") is not idle anymore, but an error occurred while moving back!", false);
                this.modClass.addLogEntry(this.configPrefix, sqe, false);
                continue;
            }
            catch (Exception e) {
                this.modClass.addLogEntry(this.configPrefix, (byte)3, "Client status of \"" + clientInfo.get("client_nickname") + "\" (db id: " + clientInfo.get("client_database_id") + ") is not idle anymore, but an error occurred while moving back!", false);
                this.modClass.addLogEntry(this.configPrefix, e, false);
            }
        }
        if (this.IDLE_WARN_TIME > 0) {
            this.IDLE_CLIENTS_WARN_SENT.clear();
            this.IDLE_CLIENTS_WARN_SENT.addAll(this.idleClientsWarnSentTemp);
            this.idleClientsWarnSentTemp.clear();
        }
        if (!this.IDLE_KICK && this.IDLE_MOVE_BACK) {
            int i = 0;
            while (i < this.IDLE_CLIENTS_MOVED.size()) {
                boolean found = false;
                for (HashMap<String, String> hashMap : clientList) {
                    if (Integer.parseInt(hashMap.get("clid")) != this.IDLE_CLIENTS_MOVED.elementAt(i)) continue;
                    found = true;
                    break;
                }
                if (!found) {
                    this.IDLE_CLIENTS_MOVED.removeElementAt(i);
                    this.IDLE_CLIENTS_MOVED_CHANNEL.removeElementAt(i);
                }
                ++i;
            }
        }
    }

    private void kickClient(HashMap<String, String> clientInfo, String kickMSG, int clientID) {
        try {
            this.queryLib.kickClient(clientID, false, kickMSG);
            this.modClass.addLogEntry(this.configPrefix, (byte)1, "Client \"" + clientInfo.get("client_nickname") + "\" (db id: " + clientInfo.get("client_database_id") + ") was idle, client was kicked!", false);
            this.fel.clearException(Integer.parseInt(clientInfo.get("client_database_id")));
        }
        catch (TS3ServerQueryException sqe) {
            if (!this.fel.existsException(sqe, Integer.parseInt(clientInfo.get("client_database_id")))) {
                this.fel.addException(sqe, Integer.parseInt(clientInfo.get("client_database_id")));
                this.modClass.addLogEntry(this.configPrefix, (byte)3, "Client status of \"" + clientInfo.get("client_nickname") + "\" (db id: " + clientInfo.get("client_database_id") + ") is idle, but an error occurred while kicking client!", false);
                this.modClass.addLogEntry(this.configPrefix, sqe, false);
            }
        }
        catch (Exception e) {
            this.modClass.addLogEntry(this.configPrefix, (byte)3, "Client status of \"" + clientInfo.get("client_nickname") + "\" (db id: " + clientInfo.get("client_database_id") + ") is idle, but an error occurred while kicking client!", false);
            this.modClass.addLogEntry(this.configPrefix, e, false);
        }
    }

    private void createMessage() {
        this.idleMessage = new String(this.IDLE_MESSAGE);
        this.idleMessage = this.idleMessage.replace("%IDLE_MAX_TIME%", Long.toString(this.IDLE_MAX_TIME / 1000 / 60));
        if (!this.IDLE_KICK) {
            this.idleMessage = this.idleMessage.replace("%IDLE_CHANNEL_NAME%", this.channelName);
        }
        if (!this.modClass.isMessageLengthValid(this.IDLE_MESSAGE_MODE, this.idleMessage)) {
            this.modClass.addLogEntry(this.configPrefix, (byte)2, "Idle Check message is to long! Make sure that " + this.IDLE_MESSAGE_MODE + " messages are not longer than " + Short.toString(this.modClass.getMaxMessageLength(this.IDLE_MESSAGE_MODE)) + " characters (including spaces and BBCode)" + (this.modClass.getMySQLConnection() == null ? new StringBuilder(", check file: ").append(this.IDLE_FILE).toString() : ""), true);
        }
        if (this.IDLE_WARN_TIME > 0) {
            this.idleWarnMessage = new String(this.IDLE_WARN_MESSAGE);
            this.idleWarnMessage = this.idleWarnMessage.replace("%IDLE_WARN_TIME%", Long.toString(this.IDLE_WARN_TIME / 1000 / 60));
            this.idleWarnMessage = this.idleWarnMessage.replace("%IDLE_MAX_TIME%", Long.toString(this.IDLE_MAX_TIME / 1000 / 60));
            if (!this.IDLE_KICK) {
                this.idleWarnMessage = this.idleWarnMessage.replace("%IDLE_CHANNEL_NAME%", this.channelName);
            }
            if (!this.modClass.isMessageLengthValid(this.IDLE_WARN_MESSAGE_MODE, this.idleWarnMessage)) {
                this.modClass.addLogEntry(this.configPrefix, (byte)2, "Idle Check warn message is to long! Make sure that " + this.IDLE_WARN_MESSAGE_MODE + " messages are not longer than " + Short.toString(this.modClass.getMaxMessageLength(this.IDLE_WARN_MESSAGE_MODE)) + " characters (including spaces and BBCode)" + (this.modClass.getMySQLConnection() == null ? new StringBuilder(", check file: ").append(this.IDLE_FILE).toString() : ""), true);
            }
        }
        if (this.IDLE_SECOND_MAX_TIME > 0) {
            this.idleSecondMessage = new String(this.IDLE_SECOND_MESSAGE);
            this.idleSecondMessage = this.idleSecondMessage.replace("%IDLE_SECOND_MAX_TIME%", Long.toString(this.IDLE_SECOND_MAX_TIME / 1000 / 60));
            this.idleSecondMessage = this.idleSecondMessage.replace("%IDLE_MAX_TIME%", Long.toString(this.IDLE_MAX_TIME / 1000 / 60));
            if (!this.modClass.isMessageLengthValid("kick", this.idleSecondMessage)) {
                this.modClass.addLogEntry(this.configPrefix, (byte)2, "Second Idle Check message is to long! Make sure that kick messages are not longer than " + Short.toString(this.modClass.getMaxMessageLength("kick")) + " characters (including spaces and BBCode)" + (this.modClass.getMySQLConnection() == null ? new StringBuilder(", check file: ").append(this.IDLE_FILE).toString() : ""), true);
            }
        }
    }
}

