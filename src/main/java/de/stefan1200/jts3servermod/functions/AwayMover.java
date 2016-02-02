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
public class AwayMover
implements HandleBotEvents,
LoadConfiguration,
HandleClientList {
    private String configPrefix = "";
    private JTS3ServerMod_Interface modClass = null;
    private JTS3ServerQuery queryLib = null;
    private boolean pluginEnabled = false;
    private boolean AWAY_MOVE_BACK = true;
    private Vector<Integer> AWAY_CHANNEL_LIST = new Vector<>();
    private boolean AWAY_CHANNEL_LIST_IGNORE = true;
    private int AWAY_MOVE_CHANNELID = -1;
    private int AWAY_MOVE_DELAY = 5;
    private Vector<Integer> AWAY_CLIENTS_MOVED = new Vector<>();
    private Vector<Integer> AWAY_CLIENTS_MOVED_CHANNEL = new Vector<>();
    private Vector<Integer> AWAY_GROUP_LIST = new Vector<>();
    private boolean AWAY_GROUP_LIST_IGNORE = true;
    private String AWAY_MESSAGE_MODE = null;
    private String AWAY_MESSAGE = null;
    private String AWAY_FILE = null;
    private String customMessage = null;
    private String channelName = null;
    private FunctionExceptionLog fel = new FunctionExceptionLog();

    @Override
    public void initClass(JTS3ServerMod_Interface modClass, JTS3ServerQuery queryLib, String prefix) {
        this.modClass = modClass;
        this.queryLib = queryLib;
        this.configPrefix = prefix.trim();
    }

    @Override
    public void handleOnBotConnect() {
        if (!this.pluginEnabled) {
            return;
        }
        String msg = "Clients with away status will be moved to Channel \"" + this.channelName + "\" (id: " + Integer.toString(this.AWAY_MOVE_CHANNELID) + ") after " + this.AWAY_MOVE_DELAY + " seconds" + (this.AWAY_MOVE_BACK ? " and moved back if not away anymore!" : "");
        this.modClass.addLogEntry(this.configPrefix, (byte)1, msg, true);
    }

    @Override
    public void handleAfterCacheUpdate() {
        if (!this.pluginEnabled) {
            return;
        }
        this.channelName = this.modClass.getChannelName(this.AWAY_MOVE_CHANNELID);
        if (this.channelName == null) {
            this.modClass.addLogEntry(this.configPrefix, (byte)3, "Critical: Channel ID " + this.AWAY_MOVE_CHANNELID + " don't exists! Check value " + this.configPrefix + "_channel_id in your configuration!", true);
            this.pluginEnabled = false;
            return;
        }
        if (!this.AWAY_MESSAGE_MODE.equalsIgnoreCase("none")) {
            this.createMessage();
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
        this.AWAY_CHANNEL_LIST = null;
        this.AWAY_CLIENTS_MOVED = null;
        this.AWAY_CLIENTS_MOVED_CHANNEL = null;
        this.AWAY_GROUP_LIST = null;
    }

    @Override
    public boolean multipleInstances() {
        return true;
    }

    @Override
    public void initConfig(ArrangedPropertiesWriter config) {
        config.addKey(String.valueOf(this.configPrefix) + "_moveback", "Move client back if not away anymore? Set yes or no here!", "yes");
        config.addKey(String.valueOf(this.configPrefix) + "_channel_id", "Channel id to move away clients into it");
        config.addKey(String.valueOf(this.configPrefix) + "_delay", "Idle time in seconds after the client with away status will be moved to the channel.\nHas between 0 and 10000 seconds!", "5");
        config.addKey(String.valueOf(this.configPrefix) + "_channel_list", "A comma separated list (without spaces) of channel ids.\nDepends on the given mode, this channels can be ignored or only this channels will be checked!\nIf no channels should be ignored, set no channels here and select the channel list mode ignore!");
        config.addKey(String.valueOf(this.configPrefix) + "_channel_list_mode", "Select one of the two modes for the channel list.\nignore = The selected channels will be ignored.\nonly = Only the selected channels will be checked.", "ignore");
        config.addKey(String.valueOf(this.configPrefix) + "_group_list", "A comma separated list (without spaces) of server group ids.\nDepends on the given mode, this server groups can be ignored or only this server groups will be checked!\nIf no server groups should be ignored, set no server groups here and select the group list mode ignore!");
        config.addKey(String.valueOf(this.configPrefix) + "_group_list_mode", "Select one of the two modes for the server group list.\nignore = The selected server groups will be ignored.\nonly = Only the selected server groups will be checked.", "ignore");
        config.addKey(String.valueOf(this.configPrefix) + "_message_mode", "Select the message mode, how the client should get the message.\npoke, chat or none are valid values!", "none");
        if (this.modClass.getMySQLConnection() == null) {
            config.addKey(String.valueOf(this.configPrefix) + "_file", "Path to file which contains the away mover message", "config/server1/awaymessages.cfg");
        }
        config.addKey(String.valueOf(this.configPrefix) + "_message", "Away Mover message, the client get this message as chat message.\nYou can use the following keywords, which will be replaced:\n%AWAY_CHANNEL_NAME% - This will be replaced with the channel name of the away_move_channel_id\nTypical BBCode like in Teamspeak 3 Client possible. You can use \\n for a new line.", this.modClass.getMySQLConnection() != null);
    }

    @Override
    public boolean loadConfig(ArrangedPropertiesWriter config, boolean slowMode) throws BotConfigurationException, NumberFormatException {
        String lastNumberValue = "";
        String temp = null;
        this.pluginEnabled = false;
        try {
            StringTokenizer st;
            this.AWAY_MOVE_BACK = config.getValue(String.valueOf(this.configPrefix) + "_moveback", "yes").trim().equalsIgnoreCase("yes");
            lastNumberValue = String.valueOf(this.configPrefix) + "_channel_id";
            temp = config.getValue(String.valueOf(this.configPrefix) + "_channel_id");
            if (temp == null) {
                throw new NumberFormatException();
            }
            this.AWAY_MOVE_CHANNELID = Integer.parseInt(temp.trim());
            lastNumberValue = String.valueOf(this.configPrefix) + "_delay";
            this.AWAY_MOVE_DELAY = Integer.parseInt(config.getValue(String.valueOf(this.configPrefix) + "_delay", "5").trim());
            if (this.AWAY_MOVE_DELAY < 0 || this.AWAY_MOVE_DELAY > 10000) {
                this.AWAY_MOVE_DELAY = 5;
            }
            temp = null;
            this.AWAY_CHANNEL_LIST.clear();
            temp = config.getValue(String.valueOf(this.configPrefix) + "_channel_list");
            lastNumberValue = String.valueOf(this.configPrefix) + "_channel_list";
            if (temp != null && temp.length() > 0) {
                st = new StringTokenizer(temp, ",", false);
                while (st.hasMoreTokens()) {
                    this.AWAY_CHANNEL_LIST.addElement(Integer.parseInt(st.nextToken().trim()));
                }
            }
            this.AWAY_CHANNEL_LIST_IGNORE = !config.getValue(String.valueOf(this.configPrefix) + "_channel_list_mode", "ignore").trim().equalsIgnoreCase("only");
            temp = null;
            this.AWAY_GROUP_LIST.clear();
            temp = config.getValue(String.valueOf(this.configPrefix) + "_group_list");
            lastNumberValue = String.valueOf(this.configPrefix) + "_group_list";
            if (temp != null && temp.length() > 0) {
                st = new StringTokenizer(temp, ",", false);
                while (st.hasMoreTokens()) {
                    this.AWAY_GROUP_LIST.addElement(Integer.parseInt(st.nextToken().trim()));
                }
            }
            this.AWAY_GROUP_LIST_IGNORE = !config.getValue(String.valueOf(this.configPrefix) + "_group_list_mode", "ignore").trim().equalsIgnoreCase("only");
            this.AWAY_MESSAGE_MODE = config.getValue(String.valueOf(this.configPrefix) + "_message_mode", "none").trim();
            if (!this.AWAY_MESSAGE_MODE.equalsIgnoreCase("none")) {
                this.AWAY_FILE = config.getValue(String.valueOf(this.configPrefix) + "_file");
                String[] configKeys = new String[]{String.valueOf(this.configPrefix) + "_message"};
                if (!this.modClass.loadMessages(this.configPrefix, "_file", configKeys)) {
                    throw new BotConfigurationException("Away mover message could not be loaded!");
                }
                this.AWAY_MESSAGE = config.getValue(configKeys[0]);
                if (this.AWAY_MESSAGE == null || this.AWAY_MESSAGE.length() == 0) {
                    throw new BotConfigurationException("Away Mover message missing in config!");
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
    public void setListModes(BitSet listOptions) {
        listOptions.set(0);
        listOptions.set(1);
        listOptions.set(3);
    }

    @Override
    public void handleClientCheck(Vector<HashMap<String, String>> clientList) {
        if (!this.pluginEnabled) {
            return;
        }
        for (HashMap<String, String> clientInfo : clientList) {
            if (!clientInfo.get("client_type").equals("0")) continue;
            int clientID = Integer.parseInt(clientInfo.get("clid"));
            int cachePos = -1;
            if (this.AWAY_MOVE_BACK) {
                cachePos = this.AWAY_CLIENTS_MOVED.indexOf(clientID);
            }
            if (clientInfo.get("client_away").equals("1") && this.modClass.getIdleTime(clientInfo, this.AWAY_MOVE_CHANNELID) > (long)(this.AWAY_MOVE_DELAY * 1000)) {
                int channelID = Integer.parseInt(clientInfo.get("cid"));
                boolean result = this.modClass.isIDListed(channelID, this.AWAY_CHANNEL_LIST);
                if (channelID == this.AWAY_MOVE_CHANNELID || !(this.AWAY_CHANNEL_LIST_IGNORE ? !result : result)) continue;
                result = this.modClass.isGroupListed(clientInfo.get("client_servergroups"), this.AWAY_GROUP_LIST);
                if (!(this.AWAY_GROUP_LIST_IGNORE ? !result : result)) continue;
                try {
                    this.queryLib.moveClient(clientID, this.AWAY_MOVE_CHANNELID, null);
                    if (this.AWAY_MOVE_BACK) {
                        this.AWAY_CLIENTS_MOVED.addElement(clientID);
                        this.AWAY_CLIENTS_MOVED_CHANNEL.addElement(channelID);
                    }
                    this.modClass.addLogEntry(this.configPrefix, (byte)1, "Client status of \"" + clientInfo.get("client_nickname") + "\" (db id: " + clientInfo.get("client_database_id") + ") is away, client was moved!", false);
                    this.modClass.sendMessageToClient(this.configPrefix, this.AWAY_MESSAGE_MODE, clientID, this.customMessage);
                    this.fel.clearException(Integer.parseInt(clientInfo.get("client_database_id")));
                }
                catch (TS3ServerQueryException sqe) {
                    if (this.fel.existsException(sqe, Integer.parseInt(clientInfo.get("client_database_id")))) continue;
                    this.fel.addException(sqe, Integer.parseInt(clientInfo.get("client_database_id")));
                    this.modClass.addLogEntry(this.configPrefix, (byte)3, "Client status of \"" + clientInfo.get("client_nickname") + "\" (db id: " + clientInfo.get("client_database_id") + ") is away, but an error occurred while moving client!", false);
                    this.modClass.addLogEntry(this.configPrefix, sqe, false);
                }
                catch (Exception e) {
                    this.modClass.addLogEntry(this.configPrefix, (byte)3, "Client status of \"" + clientInfo.get("client_nickname") + "\" (db id: " + clientInfo.get("client_database_id") + ") is away, but an error occurred while moving client!", false);
                    this.modClass.addLogEntry(this.configPrefix, e, false);
                }
                continue;
            }
            if (cachePos == -1 || !clientInfo.get("client_away").equals("0")) continue;
            if (Integer.parseInt(clientInfo.get("cid")) != this.AWAY_MOVE_CHANNELID) {
                this.AWAY_CLIENTS_MOVED.removeElementAt(cachePos);
                this.AWAY_CLIENTS_MOVED_CHANNEL.removeElementAt(cachePos);
                continue;
            }
            try {
                this.queryLib.moveClient(clientID, this.AWAY_CLIENTS_MOVED_CHANNEL.elementAt(cachePos), null);
                this.AWAY_CLIENTS_MOVED.removeElementAt(cachePos);
                this.AWAY_CLIENTS_MOVED_CHANNEL.removeElementAt(cachePos);
                this.modClass.addLogEntry(this.configPrefix, (byte)1, "Client status of \"" + clientInfo.get("client_nickname") + "\" (db id: " + clientInfo.get("client_database_id") + ") is not away anymore, client was moved back!", false);
                this.fel.clearException(Integer.parseInt(clientInfo.get("client_database_id")));
                continue;
            }
            catch (TS3ServerQueryException sqe) {
                if (this.fel.existsException(sqe, Integer.parseInt(clientInfo.get("client_database_id")))) continue;
                this.fel.addException(sqe, Integer.parseInt(clientInfo.get("client_database_id")));
                this.modClass.addLogEntry(this.configPrefix, (byte)3, "Client status of \"" + clientInfo.get("client_nickname") + "\" (db id: " + clientInfo.get("client_database_id") + ") is not away anymore, but an error occurred while moving back client!", false);
                this.modClass.addLogEntry(this.configPrefix, sqe, false);
                continue;
            }
            catch (Exception e) {
                this.modClass.addLogEntry(this.configPrefix, (byte)3, "Client status of \"" + clientInfo.get("client_nickname") + "\" (db id: " + clientInfo.get("client_database_id") + ") is not away anymore, but an error occurred while moving back client!", false);
                this.modClass.addLogEntry(this.configPrefix, e, false);
            }
        }
        if (this.AWAY_MOVE_BACK) {
            int i = 0;
            while (i < this.AWAY_CLIENTS_MOVED.size()) {
                boolean found = false;
                for (HashMap<String, String> hashMap : clientList) {
                    if (Integer.parseInt(hashMap.get("clid")) != this.AWAY_CLIENTS_MOVED.elementAt(i)) continue;
                    found = true;
                    break;
                }
                if (!found) {
                    this.AWAY_CLIENTS_MOVED.removeElementAt(i);
                    this.AWAY_CLIENTS_MOVED_CHANNEL.removeElementAt(i);
                }
                ++i;
            }
        }
    }

    private void createMessage() {
        this.customMessage = new String(this.AWAY_MESSAGE);
        this.customMessage = this.customMessage.replace("%AWAY_CHANNEL_NAME%", this.channelName);
        if (!this.modClass.isMessageLengthValid(this.AWAY_MESSAGE_MODE, this.customMessage)) {
            this.modClass.addLogEntry(this.configPrefix, (byte)2, "Away Mover message is to long! Make sure that " + this.AWAY_MESSAGE_MODE + " messages are not longer than " + Short.toString(this.modClass.getMaxMessageLength(this.AWAY_MESSAGE_MODE)) + " characters (including spaces and BBCode)" + (this.modClass.getMySQLConnection() == null ? new StringBuilder(", check file: ").append(this.AWAY_FILE).toString() : ""), true);
        }
    }
}

