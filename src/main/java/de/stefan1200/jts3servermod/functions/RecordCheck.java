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
public class RecordCheck
implements HandleBotEvents,
HandleClientList,
LoadConfiguration {
    private String configPrefix = "";
    private JTS3ServerMod_Interface modClass = null;
    private JTS3ServerQuery queryLib = null;
    private boolean pluginEnabled = false;
    private boolean RECORD_KICK = false;
    private String RECORD_FILE = null;
    private boolean RECORD_COMPLAINADD = false;
    private String RECORD_MESSAGE_MODE = null;
    private String RECORD_MESSAGE = null;
    private Vector<Integer> RECORD_CHANNEL_LIST = new Vector<>();
    private boolean RECORD_CHANNEL_LIST_IGNORE = true;
    private Vector<Integer> RECORD_GROUP_LIST = new Vector<>();
    private boolean RECORD_GROUP_LIST_IGNORE = true;
    private Vector<Integer> RECORD_CHANNELGROUP_LIST = new Vector<>();
    private boolean RECORD_CHANNELGROUP_LIST_IGNORE = true;
    private int RECORD_MOVE_CHANNELID = -1;
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
        String msg = this.RECORD_KICK ? "Client get kicked from Server after start recording" + (this.RECORD_COMPLAINADD ? " (complaint will be added)" : "") : "Clients will be moved into Channel \"" + this.channelName + "\" (id: " + Integer.toString(this.RECORD_MOVE_CHANNELID) + ") after start recording" + (this.RECORD_COMPLAINADD ? " (complaint will be added)" : "");
        this.modClass.addLogEntry(this.configPrefix, (byte)1, msg, true);
    }

    @Override
    public void handleAfterCacheUpdate() {
        if (!this.pluginEnabled) {
            return;
        }
        if (!this.RECORD_KICK) {
            this.channelName = this.modClass.getChannelName(this.RECORD_MOVE_CHANNELID);
            if (this.channelName == null) {
                this.modClass.addLogEntry(this.configPrefix, (byte)3, "Critical: Channel ID " + this.RECORD_MOVE_CHANNELID + " don't exists! Check value " + this.configPrefix + "_channel_id in your configuration!", true);
                this.pluginEnabled = false;
                return;
            }
        }
        this.createMessage();
    }

    @Override
    public void activate() {
    }

    @Override
    public void disable() {
    }

    @Override
    public void unload() {
        this.RECORD_CHANNEL_LIST = null;
        this.RECORD_GROUP_LIST = null;
        this.RECORD_CHANNELGROUP_LIST = null;
    }

    @Override
    public void initConfig(ArrangedPropertiesWriter config) {
        config.addKey(String.valueOf(this.configPrefix) + "_action", "Select the action if a client starts recording, possible values: kick and move", "kick");
        config.addKey(String.valueOf(this.configPrefix) + "_channel_id", "If clients should be moved, set channel id to move recording clients into it");
        config.addKey(String.valueOf(this.configPrefix) + "_channel_list", "A comma separated list (without spaces) of channel ids.\nDepends on the given mode, this channels can be ignored or only this channels will be checked!\nIf no channels should be ignored, set no channels here and select the channel list mode ignore!");
        config.addKey(String.valueOf(this.configPrefix) + "_channel_list_mode", "Select one of the two modes for the channel list.\nignore = The selected channels will be ignored.\nonly = Only the selected channels will be checked.", "ignore");
        config.addKey(String.valueOf(this.configPrefix) + "_group_list", "A comma separated list (without spaces) of server group ids.\nDepends on the given mode, this server groups can be ignored or only this server groups will be checked!");
        config.addKey(String.valueOf(this.configPrefix) + "_group_list_mode", "Select one of the two modes for the server group list.\nignore = The selected server groups will be ignored and are allowed to record.\nonly = Only the selected server groups will be checked and punished, if they start recording.\nIf no server groups should be ignored, set no server groups here and select the group list mode ignore!", "ignore");
        config.addKey(String.valueOf(this.configPrefix) + "_channelgroup_list", "A comma separated list (without spaces) of channel group ids.\nDepends on the given mode, this channel groups can be ignored or only this channel groups will be checked!\nIf no channel groups should be ignored, set no channel groups here and select the group list mode ignore!");
        config.addKey(String.valueOf(this.configPrefix) + "_channelgroup_list_mode", "Select one of the two modes for the channel group list.\nignore = The selected channel groups will be ignored and are allowed to record.\nonly = Only the selected channel groups will be checked and punished, if they start recording.", "ignore");
        config.addKey(String.valueOf(this.configPrefix) + "_add_complain", "Add complain entry to the user? Set yes or no here!", "no");
        config.addKey(String.valueOf(this.configPrefix) + "_message_mode", "If clients should be moved, select the message mode, how the client should get the message.\npoke, chat or none are valid values!", "poke");
        if (this.modClass.getMySQLConnection() == null) {
            config.addKey(String.valueOf(this.configPrefix) + "_file", "Path to file which contains the record message", "config/server1/recordmessages.cfg");
        }
        config.addKey(String.valueOf(this.configPrefix) + "_message", "Set a record message. On both modes the client should see the message.\nIf clients should be moved, you can use %RECORD_CHANNEL_NAME% in the message, which will be replaced with the channel name (of the record_move_channel_id).\nTypical BBCode like in Teamspeak 3 Client possible. You can use \\n for a new line.\nIf using mode 2, a maximum of 100 characters (including space and BBCode) can be used here!", this.modClass.getMySQLConnection() != null);
    }

    @Override
    public boolean loadConfig(ArrangedPropertiesWriter config, boolean slowMode) throws BotConfigurationException, NumberFormatException {
        String lastNumberValue = "";
        String temp = null;
        this.pluginEnabled = false;
        try {
            StringTokenizer st;
            this.RECORD_KICK = config.getValue(String.valueOf(this.configPrefix) + "_action", "move").trim().equalsIgnoreCase("kick");
            this.RECORD_MESSAGE_MODE = this.RECORD_KICK ? "kick" : config.getValue(String.valueOf(this.configPrefix) + "_message_mode", "poke").trim();
            this.RECORD_FILE = config.getValue(String.valueOf(this.configPrefix) + "_file");
            String[] configKeys = new String[]{String.valueOf(this.configPrefix) + "_message"};
            if (!this.modClass.loadMessages(this.configPrefix, "_file", configKeys)) {
                throw new BotConfigurationException("Record Check message could not be loaded!");
            }
            this.RECORD_MESSAGE = config.getValue(configKeys[0]);
            if (this.RECORD_MESSAGE == null || this.RECORD_MESSAGE.length() == 0) {
                throw new BotConfigurationException("Record Check message missing in config!");
            }
            if (!this.RECORD_KICK) {
                lastNumberValue = String.valueOf(this.configPrefix) + "_channel_id";
                temp = config.getValue(String.valueOf(this.configPrefix) + "_channel_id");
                if (temp == null) {
                    throw new NumberFormatException();
                }
                this.RECORD_MOVE_CHANNELID = Integer.parseInt(temp.trim());
            } else {
                this.createMessage();
            }
            this.RECORD_COMPLAINADD = config.getValue(String.valueOf(this.configPrefix) + "_add_complain", "no").trim().equalsIgnoreCase("yes");
            temp = null;
            this.RECORD_CHANNEL_LIST.clear();
            temp = config.getValue(String.valueOf(this.configPrefix) + "_channel_list");
            lastNumberValue = String.valueOf(this.configPrefix) + "_channel_list";
            if (temp != null && temp.length() > 0) {
                st = new StringTokenizer(temp, ",", false);
                while (st.hasMoreTokens()) {
                    this.RECORD_CHANNEL_LIST.addElement(Integer.parseInt(st.nextToken().trim()));
                }
            }
            temp = null;
            this.RECORD_GROUP_LIST.clear();
            temp = config.getValue(String.valueOf(this.configPrefix) + "_group_list");
            lastNumberValue = String.valueOf(this.configPrefix) + "_group_list";
            if (temp != null && temp.length() > 0) {
                st = new StringTokenizer(temp, ",", false);
                while (st.hasMoreTokens()) {
                    this.RECORD_GROUP_LIST.addElement(Integer.parseInt(st.nextToken().trim()));
                }
            }
            temp = null;
            this.RECORD_CHANNELGROUP_LIST.clear();
            temp = config.getValue(String.valueOf(this.configPrefix) + "_channelgroup_list");
            lastNumberValue = String.valueOf(this.configPrefix) + "_channelgroup_list";
            if (temp != null && temp.length() > 0) {
                st = new StringTokenizer(temp, ",", false);
                while (st.hasMoreTokens()) {
                    this.RECORD_CHANNELGROUP_LIST.addElement(Integer.parseInt(st.nextToken().trim()));
                }
            }
            this.RECORD_CHANNEL_LIST_IGNORE = !config.getValue(String.valueOf(this.configPrefix) + "_channel_list_mode", "ignore").trim().equalsIgnoreCase("only");
            this.RECORD_GROUP_LIST_IGNORE = !config.getValue(String.valueOf(this.configPrefix) + "_group_list_mode", "ignore").trim().equalsIgnoreCase("only");
            this.RECORD_CHANNELGROUP_LIST_IGNORE = !config.getValue(String.valueOf(this.configPrefix) + "_channelgroup_list_mode", "ignore").trim().equalsIgnoreCase("only");
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
        listOptions.set(5);
    }

    @Override
    public void handleClientCheck(Vector<HashMap<String, String>> clientList) {
        if (!this.pluginEnabled) {
            return;
        }
        for (HashMap<String, String> clientInfo : clientList) {
            if (!clientInfo.get("client_type").equals("0")) continue;
            int clientID = Integer.parseInt(clientInfo.get("clid"));
            if (!clientInfo.get("client_is_recording").equals("1")) continue;
            int channelID = Integer.parseInt(clientInfo.get("cid"));
            boolean result = this.modClass.isIDListed(channelID, this.RECORD_CHANNEL_LIST);
            if (this.RECORD_KICK) {
                if (!(this.RECORD_CHANNEL_LIST_IGNORE ? !result : result)) continue;
                result = this.modClass.isGroupListed(clientInfo.get("client_servergroups"), this.RECORD_GROUP_LIST);
                if (!(this.RECORD_GROUP_LIST_IGNORE ? !result : result)) continue;
                result = this.modClass.isGroupListed(clientInfo.get("client_channel_group_id"), this.RECORD_CHANNELGROUP_LIST);
                if (!(this.RECORD_CHANNELGROUP_LIST_IGNORE ? !result : result)) continue;
                this.addComplainToUser(clientInfo);
                try {
                    this.queryLib.kickClient(clientID, false, this.customMessage);
                    this.modClass.addLogEntry(this.configPrefix, (byte)1, "Client \"" + clientInfo.get("client_nickname") + "\" (db id: " + clientInfo.get("client_database_id") + ") started recording, client was kicked!", false);
                    this.fel.clearException(Integer.parseInt(clientInfo.get("client_database_id")));
                }
                catch (TS3ServerQueryException sqe) {
                    if (this.fel.existsException(sqe, Integer.parseInt(clientInfo.get("client_database_id")))) continue;
                    this.fel.addException(sqe, Integer.parseInt(clientInfo.get("client_database_id")));
                    this.modClass.addLogEntry(this.configPrefix, (byte)3, "Client \"" + clientInfo.get("client_nickname") + "\" (db id: " + clientInfo.get("client_database_id") + ") started recording, but an error occurred while kicking client!", false);
                    this.modClass.addLogEntry(this.configPrefix, sqe, false);
                }
                catch (Exception e) {
                    this.modClass.addLogEntry(this.configPrefix, (byte)3, "Client \"" + clientInfo.get("client_nickname") + "\" (db id: " + clientInfo.get("client_database_id") + ") started recording, but an error occurred while kicking client!", false);
                    this.modClass.addLogEntry(this.configPrefix, e, false);
                }
                continue;
            }
            if (channelID == this.RECORD_MOVE_CHANNELID || !(this.RECORD_CHANNEL_LIST_IGNORE ? !result : result)) continue;
            result = this.modClass.isGroupListed(clientInfo.get("client_servergroups"), this.RECORD_GROUP_LIST);
            if (!(this.RECORD_GROUP_LIST_IGNORE ? !result : result)) continue;
            result = this.modClass.isGroupListed(clientInfo.get("client_channel_group_id"), this.RECORD_CHANNELGROUP_LIST);
            if (!(this.RECORD_CHANNELGROUP_LIST_IGNORE ? !result : result)) continue;
            this.addComplainToUser(clientInfo);
            try {
                this.queryLib.moveClient(clientID, this.RECORD_MOVE_CHANNELID, null);
                this.modClass.addLogEntry(this.configPrefix, (byte)1, "Client \"" + clientInfo.get("client_nickname") + "\" (db id: " + clientInfo.get("client_database_id") + ") started recording, client was moved!", false);
                this.modClass.sendMessageToClient(this.configPrefix, this.RECORD_MESSAGE_MODE, clientID, this.customMessage);
                this.fel.clearException(Integer.parseInt(clientInfo.get("client_database_id")));
                continue;
            }
            catch (TS3ServerQueryException sqe) {
                if (this.fel.existsException(sqe, Integer.parseInt(clientInfo.get("client_database_id")))) continue;
                this.fel.addException(sqe, Integer.parseInt(clientInfo.get("client_database_id")));
                this.modClass.addLogEntry(this.configPrefix, (byte)3, "Client \"" + clientInfo.get("client_nickname") + "\" (db id: " + clientInfo.get("client_database_id") + ") started recording, but an error occurred while moving client!", false);
                this.modClass.addLogEntry(this.configPrefix, sqe, false);
                continue;
            }
            catch (Exception e) {
                this.modClass.addLogEntry(this.configPrefix, (byte)3, "Client \"" + clientInfo.get("client_nickname") + "\" (db id: " + clientInfo.get("client_database_id") + ") started recording, but an error occurred while moving client!", false);
                this.modClass.addLogEntry(this.configPrefix, e, false);
            }
        }
    }

    private void addComplainToUser(HashMap<String, String> clientInfo) {
        if (this.RECORD_COMPLAINADD) {
            try {
                this.queryLib.complainAdd(Integer.parseInt(clientInfo.get("client_database_id")), "Recording client: " + clientInfo.get("client_nickname"));
                this.modClass.addLogEntry(this.configPrefix, (byte)1, "Added complaint to recording client \"" + clientInfo.get("client_nickname") + "\" (db id: " + clientInfo.get("client_database_id") + ")!", false);
                this.fel.clearException(Integer.parseInt(clientInfo.get("client_database_id")));
            }
            catch (TS3ServerQueryException sqe) {
                if (!this.fel.existsException(sqe, Integer.parseInt(clientInfo.get("client_database_id")))) {
                    this.fel.addException(sqe, Integer.parseInt(clientInfo.get("client_database_id")));
                    this.modClass.addLogEntry(this.configPrefix, (byte)3, "Error while adding complaint to recording client \"" + clientInfo.get("client_nickname") + "\" (db id: " + clientInfo.get("client_database_id") + ")!", false);
                    this.modClass.addLogEntry(this.configPrefix, sqe, false);
                }
            }
            catch (Exception e) {
                this.modClass.addLogEntry(this.configPrefix, (byte)3, "Error while adding complaint to recording client \"" + clientInfo.get("client_nickname") + "\" (db id: " + clientInfo.get("client_database_id") + ")!", false);
                this.modClass.addLogEntry(this.configPrefix, e, false);
            }
        }
    }

    @Override
    public boolean multipleInstances() {
        return true;
    }

    private void createMessage() {
        this.customMessage = new String(this.RECORD_MESSAGE);
        if (!this.RECORD_KICK) {
            this.customMessage = this.customMessage.replace("%RECORD_CHANNEL_NAME%", this.channelName);
        }
        if (!this.modClass.isMessageLengthValid(this.RECORD_MESSAGE_MODE, this.customMessage)) {
            this.modClass.addLogEntry(this.configPrefix, (byte)2, "Record Check message is to long! Make sure that " + this.RECORD_MESSAGE_MODE + " messages are not longer than " + Short.toString(this.modClass.getMaxMessageLength(this.RECORD_MESSAGE_MODE)) + " characters (including spaces and BBCode)" + (this.modClass.getMySQLConnection() == null ? new StringBuilder(", check file: ").append(this.RECORD_FILE).toString() : ""), true);
        }
    }
}

