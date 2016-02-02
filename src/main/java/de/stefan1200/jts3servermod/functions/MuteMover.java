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
public class MuteMover
implements HandleClientList,
LoadConfiguration,
HandleBotEvents {
    private String configPrefix = "";
    private JTS3ServerMod_Interface modClass = null;
    private JTS3ServerQuery queryLib = null;
    private boolean pluginEnabled = false;
    private boolean MUTE_MOVE_BACK = true;
    private Vector<Integer> MUTE_CHANNEL_LIST = new Vector<>();
    private boolean MUTE_CHANNEL_LIST_IGNORE = true;
    private int MUTE_MOVE_CHANNELID = -1;
    private int MUTE_MOVE_DELAY = 5;
    private boolean MUTE_MOVE_MICROPHONE = false;
    private boolean MUTE_MOVE_HEADPHONE = false;
    private boolean MUTE_MOVE_MICROPHONE_HARDWARE = false;
    private boolean MUTE_MOVE_HEADPHONE_HARDWARE = false;
    private boolean MUTE_CONDITION_MODE_ALL = false;
    private Vector<Integer> MUTE_CLIENTS_MOVED = new Vector<>();
    private Vector<Integer> MUTE_CLIENTS_MOVED_CHANNEL = new Vector<>();
    private Vector<Integer> MUTE_GROUP_LIST = new Vector<>();
    private boolean MUTE_GROUP_LIST_IGNORE = true;
    private String MUTE_MESSAGE_MODE = null;
    private String MUTE_MESSAGE = null;
    //private String MUTE_FILE = null;
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
        this.MUTE_CHANNEL_LIST = null;
        this.MUTE_CLIENTS_MOVED = null;
        this.MUTE_CLIENTS_MOVED_CHANNEL = null;
        this.MUTE_GROUP_LIST = null;
    }

    @Override
    public void handleAfterCacheUpdate() {
        if (!this.pluginEnabled) {
            return;
        }
        this.channelName = this.modClass.getChannelName(this.MUTE_MOVE_CHANNELID);
        if (this.channelName == null) {
            this.modClass.addLogEntry(this.configPrefix, (byte)3, "Critical: Channel ID " + this.MUTE_MOVE_CHANNELID + " don't exists! Check value " + this.configPrefix + "_channel_id in your configuration!", true);
            this.pluginEnabled = false;
            return;
        }
        if (!this.MUTE_MESSAGE_MODE.equalsIgnoreCase("none")) {
            this.createMessage();
        }
    }

    @Override
    public void handleOnBotConnect() {
        if (!this.pluginEnabled) {
            return;
        }
        String extmsg = "";
        if ((this.MUTE_MOVE_HEADPHONE || this.MUTE_MOVE_HEADPHONE_HARDWARE) && (this.MUTE_MOVE_MICROPHONE || this.MUTE_MOVE_MICROPHONE_HARDWARE)) {
            extmsg = "headphone " + (this.MUTE_CONDITION_MODE_ALL ? "and" : "or") + " microphone";
        } else if (this.MUTE_MOVE_HEADPHONE || this.MUTE_MOVE_HEADPHONE_HARDWARE) {
            extmsg = "headphone";
        } else if (this.MUTE_MOVE_MICROPHONE || this.MUTE_MOVE_MICROPHONE_HARDWARE) {
            extmsg = "microphone";
        }
        String msg = "Clients with " + extmsg + " muted will be moved to Channel \"" + this.channelName + "\" (id: " + Integer.toString(this.MUTE_MOVE_CHANNELID) + ") after " + this.MUTE_MOVE_DELAY + " seconds" + (this.MUTE_MOVE_BACK ? " and moved back if not muted anymore!" : "");
        this.modClass.addLogEntry(this.configPrefix, (byte)1, msg, true);
    }

    @Override
    public void initConfig(ArrangedPropertiesWriter config) {
        config.addKey(String.valueOf(this.configPrefix) + "_moveback", "Move client back if not muted anymore? Set yes or no here!", "yes");
        config.addKey(String.valueOf(this.configPrefix) + "_headphone", "Enable move if headphone is muted, yes or no", "no");
        config.addKey(String.valueOf(this.configPrefix) + "_microphone", "Enable move if microphone is muted, yes or no", "no");
        config.addKey(String.valueOf(this.configPrefix) + "_headphone_hardware", "Enable move if headphone hardware is disabled, yes or no", "no");
        config.addKey(String.valueOf(this.configPrefix) + "_microphone_hardware", "Enable move if microphone hardware is disabled, yes or no\nThis also happen if someone is speaking in another TS3 client server tab.", "no");
        config.addKey(String.valueOf(this.configPrefix) + "_condition_mode", "Move the client if the client has all selected conditions or at least one? Set all or one here!", "one");
        config.addKey(String.valueOf(this.configPrefix) + "_channel_id", "Channel id to move muted clients into it");
        config.addKey(String.valueOf(this.configPrefix) + "_delay", "Idle time in seconds after the client with a specified mute status will be moved to the channel.\nHas between 0 and 10000 seconds!", "5");
        config.addKey(String.valueOf(this.configPrefix) + "_channel_list", "A comma separated list (without spaces) of channel ids.\nDepends on the given mode, this channels can be ignored or only this channels will be checked!\nIf no channels should be ignored, set no channels here and select the channel list mode ignore!");
        config.addKey(String.valueOf(this.configPrefix) + "_channel_list_mode", "Select one of the two modes for the channel list.\nignore = The selected channels will be ignored.\nonly = Only the selected channels will be checked.", "ignore");
        config.addKey(String.valueOf(this.configPrefix) + "_group_list", "A comma separated list (without spaces) of server group ids.\nDepends on the given mode, this server groups can be ignored or only this server groups will be checked!\nIf no server groups should be ignored, set no server groups here and select the group list mode ignore!");
        config.addKey(String.valueOf(this.configPrefix) + "_group_list_mode", "Select one of the two modes for the server group list.\nignore = The selected server groups will be ignored.\nonly = Only the selected server groups will be checked.", "ignore");
        config.addKey(String.valueOf(this.configPrefix) + "_message_mode", "Select the message mode, how the client should get the message.\npoke, chat or none are valid values!", "none");
        if (this.modClass.getMySQLConnection() == null) {
            config.addKey(String.valueOf(this.configPrefix) + "_file", "Path to file which contains the mute mover message", "config/server1/mutemessages.cfg");
        }
        config.addKey(String.valueOf(this.configPrefix) + "_message", "Mute Mover message, the client get this message as chat message.\nYou can use the following keywords, which will be replaced:\n%MUTE_CHANNEL_NAME% - This will be replaced with the channel name of the mute_move_channel_id\nTypical BBCode like in Teamspeak 3 Client possible. You can use \\n for a new line.", this.modClass.getMySQLConnection() != null);
    }

    @Override
    public boolean loadConfig(ArrangedPropertiesWriter config, boolean slowMode) throws BotConfigurationException, NumberFormatException {
        String lastNumberValue = "";
        String temp = null;
        this.pluginEnabled = false;
        try {
            StringTokenizer st;
            this.MUTE_MOVE_BACK = config.getValue(String.valueOf(this.configPrefix) + "_moveback", "yes").trim().equalsIgnoreCase("yes");
            this.MUTE_MOVE_HEADPHONE = config.getValue(String.valueOf(this.configPrefix) + "_headphone", "no").trim().equals("yes");
            this.MUTE_MOVE_MICROPHONE = config.getValue(String.valueOf(this.configPrefix) + "_microphone", "no").trim().equals("yes");
            this.MUTE_MOVE_HEADPHONE_HARDWARE = config.getValue(String.valueOf(this.configPrefix) + "_headphone_hardware", "no").trim().equals("yes");
            this.MUTE_MOVE_MICROPHONE_HARDWARE = config.getValue(String.valueOf(this.configPrefix) + "_microphone_hardware", "no").trim().equals("yes");
            this.MUTE_CONDITION_MODE_ALL = config.getValue(String.valueOf(this.configPrefix) + "_condition_mode", "one").trim().equals("all");
            if (!(this.MUTE_MOVE_HEADPHONE || this.MUTE_MOVE_MICROPHONE || this.MUTE_MOVE_HEADPHONE_HARDWARE || this.MUTE_MOVE_MICROPHONE_HARDWARE)) {
                throw new BotConfigurationException("Headphone and Microphone detection in config disabled!");
            }
            lastNumberValue = String.valueOf(this.configPrefix) + "_channel_id";
            temp = config.getValue(String.valueOf(this.configPrefix) + "_channel_id");
            if (temp == null) {
                throw new NumberFormatException("Config value " + lastNumberValue + " is not a number!");
            }
            this.MUTE_MOVE_CHANNELID = Integer.parseInt(temp.trim());
            lastNumberValue = String.valueOf(this.configPrefix) + "_delay";
            this.MUTE_MOVE_DELAY = Integer.parseInt(config.getValue(String.valueOf(this.configPrefix) + "_delay", "5").trim());
            if (this.MUTE_MOVE_DELAY < 0 || this.MUTE_MOVE_DELAY > 10000) {
                this.MUTE_MOVE_DELAY = 5;
            }
            temp = null;
            this.MUTE_CHANNEL_LIST.clear();
            temp = config.getValue(String.valueOf(this.configPrefix) + "_channel_list");
            lastNumberValue = String.valueOf(this.configPrefix) + "_channel_list";
            if (temp != null && temp.length() > 0) {
                st = new StringTokenizer(temp, ",", false);
                while (st.hasMoreTokens()) {
                    this.MUTE_CHANNEL_LIST.addElement(Integer.parseInt(st.nextToken().trim()));
                }
            }
            this.MUTE_CHANNEL_LIST_IGNORE = !config.getValue(String.valueOf(this.configPrefix) + "_channel_list_mode", "ignore").trim().equalsIgnoreCase("only");
            temp = null;
            this.MUTE_GROUP_LIST.clear();
            temp = config.getValue(String.valueOf(this.configPrefix) + "_group_list");
            lastNumberValue = String.valueOf(this.configPrefix) + "_group_list";
            if (temp != null && temp.length() > 0) {
                st = new StringTokenizer(temp, ",", false);
                while (st.hasMoreTokens()) {
                    this.MUTE_GROUP_LIST.addElement(Integer.parseInt(st.nextToken().trim()));
                }
            }
            this.MUTE_GROUP_LIST_IGNORE = !config.getValue(String.valueOf(this.configPrefix) + "_group_list_mode", "ignore").trim().equalsIgnoreCase("only");
            this.MUTE_MESSAGE_MODE = config.getValue(String.valueOf(this.configPrefix) + "_message_mode", "none").trim();
            if (!this.MUTE_MESSAGE_MODE.equalsIgnoreCase("none")) {
                //this.MUTE_FILE = config.getValue(String.valueOf(this.configPrefix) + "_file");
                String[] configKeys = new String[]{String.valueOf(this.configPrefix) + "_message"};
                if (!this.modClass.loadMessages(this.configPrefix, "_file", configKeys)) {
                    throw new BotConfigurationException("Mute Mover message could not be loaded!");
                }
                this.MUTE_MESSAGE = config.getValue(configKeys[0]);
                if (this.MUTE_MESSAGE == null || this.MUTE_MESSAGE.length() == 0) {
                    throw new BotConfigurationException("Mute Mover message missing in config!");
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
        listOptions.set(1);
        listOptions.set(5);
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
            if (this.MUTE_MOVE_BACK) {
                cachePos = this.MUTE_CLIENTS_MOVED.indexOf(clientID);
            }
            if (this.isClientMuted(clientInfo) && this.modClass.getIdleTime(clientInfo, this.MUTE_MOVE_CHANNELID) > (long)(this.MUTE_MOVE_DELAY * 1000)) {
                int channelID = Integer.parseInt(clientInfo.get("cid"));
                boolean result = this.modClass.isIDListed(channelID, this.MUTE_CHANNEL_LIST);
                if (channelID == this.MUTE_MOVE_CHANNELID || !(this.MUTE_CHANNEL_LIST_IGNORE ? !result : result)) continue;
                result = this.modClass.isGroupListed(clientInfo.get("client_servergroups"), this.MUTE_GROUP_LIST);
                if (!(this.MUTE_GROUP_LIST_IGNORE ? !result : result)) continue;
                try {
                    this.queryLib.moveClient(clientID, this.MUTE_MOVE_CHANNELID, null);
                    if (this.MUTE_MOVE_BACK) {
                        this.MUTE_CLIENTS_MOVED.addElement(clientID);
                        this.MUTE_CLIENTS_MOVED_CHANNEL.addElement(channelID);
                    }
                    this.modClass.addLogEntry(this.configPrefix, (byte)1, "Client status of \"" + clientInfo.get("client_nickname") + "\" (db id: " + clientInfo.get("client_database_id") + ") is muted, client was moved!", false);
                    this.modClass.sendMessageToClient(this.configPrefix, this.MUTE_MESSAGE_MODE, clientID, this.customMessage);
                    this.fel.clearException(Integer.parseInt(clientInfo.get("client_database_id")));
                }
                catch (TS3ServerQueryException sqe) {
                    if (this.fel.existsException(sqe, Integer.parseInt(clientInfo.get("client_database_id")))) continue;
                    this.fel.addException(sqe, Integer.parseInt(clientInfo.get("client_database_id")));
                    this.modClass.addLogEntry(this.configPrefix, (byte)3, "Client status of \"" + clientInfo.get("client_nickname") + "\" (db id: " + clientInfo.get("client_database_id") + ") is muted, but an error occurred while moving client!", false);
                    this.modClass.addLogEntry(this.configPrefix, sqe, false);
                }
                catch (Exception e) {
                    this.modClass.addLogEntry(this.configPrefix, (byte)3, "Client status of \"" + clientInfo.get("client_nickname") + "\" (db id: " + clientInfo.get("client_database_id") + ") is muted, but an error occurred while moving client!", false);
                    this.modClass.addLogEntry(this.configPrefix, e, false);
                }
                continue;
            }
            if (cachePos == -1 || this.isClientMuted(clientInfo)) continue;
            if (Integer.parseInt(clientInfo.get("cid")) != this.MUTE_MOVE_CHANNELID) {
                this.MUTE_CLIENTS_MOVED.removeElementAt(cachePos);
                this.MUTE_CLIENTS_MOVED_CHANNEL.removeElementAt(cachePos);
                continue;
            }
            try {
                this.queryLib.moveClient(clientID, this.MUTE_CLIENTS_MOVED_CHANNEL.elementAt(cachePos), null);
                this.MUTE_CLIENTS_MOVED.removeElementAt(cachePos);
                this.MUTE_CLIENTS_MOVED_CHANNEL.removeElementAt(cachePos);
                this.modClass.addLogEntry(this.configPrefix, (byte)1, "Client status of \"" + clientInfo.get("client_nickname") + "\" (db id: " + clientInfo.get("client_database_id") + ") is not muted anymore, client was moved back!", false);
                this.fel.clearException(Integer.parseInt(clientInfo.get("client_database_id")));
                continue;
            }
            catch (TS3ServerQueryException sqe) {
                if (this.fel.existsException(sqe, Integer.parseInt(clientInfo.get("client_database_id")))) continue;
                this.fel.addException(sqe, Integer.parseInt(clientInfo.get("client_database_id")));
                this.modClass.addLogEntry(this.configPrefix, (byte)3, "Client status of \"" + clientInfo.get("client_nickname") + "\" (db id: " + clientInfo.get("client_database_id") + ") is not muted anymore, but an error occurred while moving back client!", false);
                this.modClass.addLogEntry(this.configPrefix, sqe, false);
                continue;
            }
            catch (Exception e) {
                this.modClass.addLogEntry(this.configPrefix, e, false);
            }
        }
        if (this.MUTE_MOVE_BACK) {
            int i = 0;
            while (i < this.MUTE_CLIENTS_MOVED.size()) {
                boolean found = false;
                for (HashMap<String, String> client : clientList) {
                    if (Integer.parseInt(client.get("clid")) != this.MUTE_CLIENTS_MOVED.elementAt(i)) continue;
                    found = true;
                    break;
                }
                if (!found) {
                    this.MUTE_CLIENTS_MOVED.removeElementAt(i);
                    this.MUTE_CLIENTS_MOVED_CHANNEL.removeElementAt(i);
                }
                ++i;
            }
        }
    }

    private boolean isClientMuted(HashMap<String, String> clientInfo) {
        int expectedMuteCount = 0;
        int currentMuteCount = 0;
        if (this.MUTE_MOVE_HEADPHONE) {
            expectedMuteCount = (byte)(expectedMuteCount + 1);
            if (clientInfo.get("client_output_muted").equals("1")) {
                currentMuteCount = (byte)(currentMuteCount + 1);
            }
        }
        if (this.MUTE_MOVE_HEADPHONE_HARDWARE) {
            expectedMuteCount = (byte)(expectedMuteCount + 1);
            if (clientInfo.get("client_output_hardware").equals("0")) {
                currentMuteCount = (byte)(currentMuteCount + 1);
            }
        }
        if (this.MUTE_MOVE_MICROPHONE) {
            expectedMuteCount = (byte)(expectedMuteCount + 1);
            if (clientInfo.get("client_input_muted").equals("1")) {
                currentMuteCount = (byte)(currentMuteCount + 1);
            }
        }
        if (this.MUTE_MOVE_MICROPHONE_HARDWARE) {
            expectedMuteCount = (byte)(expectedMuteCount + 1);
            if (clientInfo.get("client_input_hardware").equals("0")) {
                currentMuteCount = (byte)(currentMuteCount + 1);
            }
        }
        return this.MUTE_CONDITION_MODE_ALL ? expectedMuteCount == currentMuteCount : currentMuteCount > 0;
    }

    private void createMessage() {
        this.customMessage = new String(this.MUTE_MESSAGE);
        this.customMessage = this.customMessage.replace("%MUTE_CHANNEL_NAME%", this.channelName);
        if (!this.modClass.isMessageLengthValid(this.MUTE_MESSAGE_MODE, this.customMessage)) {
        }
    }
}

