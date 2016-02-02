/*
 * Decompiled with CFR 0_110.
 */
package de.stefan1200.jts3servermod.functions;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.BitSet;
import java.util.HashMap;
import java.util.StringTokenizer;

import de.stefan1200.jts3servermod.BotConfigurationException;
import de.stefan1200.jts3servermod.interfaces.HandleBotEvents;
import de.stefan1200.jts3servermod.interfaces.HandleTS3Events;
import de.stefan1200.jts3servermod.interfaces.JTS3ServerMod_Interface;
import de.stefan1200.jts3servermod.interfaces.LoadConfiguration;
import de.stefan1200.jts3serverquery.JTS3ServerQuery;
import de.stefan1200.util.ArrangedPropertiesWriter;
import de.stefan1200.util.MySQLConnect;

/*
 * This class specifies class file version 49.0 but uses Java 6 signatures.  Assumed Java 6.
 */
public class AutoMove
implements HandleBotEvents,
HandleTS3Events,
LoadConfiguration {
    private String configPrefix = "";
    private JTS3ServerMod_Interface modClass = null;
    private JTS3ServerQuery queryLib = null;
    private boolean pluginEnabled = false;
    private HashMap<Integer, Integer> AUTOMOVE_SGCHANNEL_LIST = new HashMap<>();
    private String AUTOMOVE_MESSAGE_MODE = null;
    private String AUTOMOVE_MESSAGE = null;
    private String AUTOMOVE_FILE = null;

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
        String msg = "Auto Move is enabled, default channels for " + Integer.toString(this.AUTOMOVE_SGCHANNEL_LIST.size()) + " server groups set";
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
        this.AUTOMOVE_SGCHANNEL_LIST = null;
    }

    @Override
    public boolean multipleInstances() {
        return false;
    }

    @Override
    public void initConfig(ArrangedPropertiesWriter config) {
        config.addKey(String.valueOf(this.configPrefix) + "_message_mode", "Select the message mode, how the clients should get the message.\npoke, chat or none are valid values!", "chat");
        if (this.modClass.getMySQLConnection() == null) {
            config.addKey(String.valueOf(this.configPrefix) + "_file", "Path to file which contains the auto move configuration and message.", "config/server1/automove.cfg");
        }
        if (this.modClass.getMySQLConnection() != null) {
            config.addKey(String.valueOf(this.configPrefix) + "_message", "Set the chat message for the auto move function.\nYou can use \\n for a new line and typical BBCode like in Teamspeak 3 Client.");
        }
    }

    @Override
    public boolean loadConfig(ArrangedPropertiesWriter config, boolean slowMode) throws BotConfigurationException, NumberFormatException {
        this.pluginEnabled = false;
        if (slowMode) {
            this.modClass.addLogEntry(this.configPrefix, (byte)3, "Slow Mode activated, Auto Move disabled!", true);
            return false;
        }
        this.AUTOMOVE_MESSAGE_MODE = config.getValue(String.valueOf(this.configPrefix) + "_message_mode", "none").trim();
        this.AUTOMOVE_FILE = config.getValue(String.valueOf(this.configPrefix) + "_file");
        if (!this.loadAutoMoveFile(config)) {
            throw new BotConfigurationException("Auto Move message and configuration could not be loaded!");
        }
        if (this.AUTOMOVE_SGCHANNEL_LIST.size() == 0) {
            throw new BotConfigurationException("Auto Move needs at least one server group set!" + (this.AUTOMOVE_FILE == null ? "" : new StringBuilder(" Config file: ").append(this.AUTOMOVE_FILE).toString()));
        }
        if (this.AUTOMOVE_MESSAGE_MODE.equalsIgnoreCase("chat") || this.AUTOMOVE_MESSAGE_MODE.equalsIgnoreCase("poke")) {
            if (this.AUTOMOVE_MESSAGE == null || this.AUTOMOVE_MESSAGE.length() == 0) {
                throw new BotConfigurationException("Auto Move message could not be loaded!");
            }
            if (!this.modClass.isMessageLengthValid(this.AUTOMOVE_MESSAGE_MODE, this.AUTOMOVE_MESSAGE)) {
                this.modClass.addLogEntry(this.configPrefix, (byte)2, "Auto Move message is to long! Make sure that " + this.AUTOMOVE_MESSAGE_MODE + " messages are not longer than " + Short.toString(this.modClass.getMaxMessageLength(this.AUTOMOVE_MESSAGE_MODE)) + " characters (including spaces and BBCode)" + (this.modClass.getMySQLConnection() == null ? new StringBuilder(", check file: ").append(this.AUTOMOVE_FILE).toString() : ""), true);
            }
        }
        this.modClass.addTS3ServerEvent(this);
        this.pluginEnabled = true;
        return this.pluginEnabled;
    }

    private boolean loadAutoMoveFile(ArrangedPropertiesWriter config) {
        BufferedReader br;
        String line;
        block37 : {
            if (this.modClass.getMySQLConnection() != null) {
                boolean retValue;
                MySQLConnect mysqlConnect = this.modClass.getMySQLConnection();
                this.AUTOMOVE_MESSAGE = config.getValue(String.valueOf(this.configPrefix) + "_message");
                retValue = false;
                PreparedStatement pst = null;
                ResultSet rs = null;
                try {
                    try {
                        mysqlConnect.connect();
                        pst = mysqlConnect.getPreparedStatement("SELECT servergroup_id, channel_id FROM jts3servermod_automove WHERE instance_id = ? AND prefix = ?");
                        pst.setInt(1, this.modClass.getInstanceID());
                        pst.setString(2, this.configPrefix);
                        rs = pst.executeQuery();
                        this.AUTOMOVE_SGCHANNEL_LIST.clear();
                        while (rs.next()) {
                            this.AUTOMOVE_SGCHANNEL_LIST.put(rs.getInt(1), rs.getInt(2));
                        }
                        retValue = true;
                    }
                    catch (Exception e) {
                        retValue = false;
                        try {
                            if (rs != null) {
                                rs.close();
                            }
                        }
                        catch (Exception var8_14) {
                            // empty catch block
                        }
                        try {
                            if (pst != null) {
                                pst.close();
                            }
                        }
                        catch (Exception var8_15) {
                            // empty catch block
                        }
                        mysqlConnect.close();
                    }
                }
                finally {
                    try {
                        if (rs != null) {
                            rs.close();
                        }
                    }
                    catch (Exception var8_18) {}
                    try {
                        if (pst != null) {
                            pst.close();
                        }
                    }
                    catch (Exception var8_19) {}
                    mysqlConnect.close();
                }
                return retValue;
            }
            if (this.AUTOMOVE_FILE == null) {
                this.modClass.addLogEntry(this.configPrefix, (byte)3, "Path to Auto Move config file was not set in bot config! Check config key: " + this.configPrefix + "_file", true);
                return false;
            }
            try {
	            this.AUTOMOVE_FILE = this.AUTOMOVE_FILE.trim();
	            br = new BufferedReader(new InputStreamReader((InputStream)new FileInputStream(this.AUTOMOVE_FILE), this.modClass.getMessageEncoding()));
	            line = br.readLine();
	            if (this.modClass.getMessageEncoding().equalsIgnoreCase("UTF-8") && line != null && line.charAt(0) == '\ufeff') {
	                line = line.substring(1);
	            }
	            if (line != null && line.equals("# JTS3ServerMod Config File")) break block37;
	            this.modClass.addLogEntry(this.configPrefix, (byte)3, "Special config file header is missing at Auto Move config file! File path: " + this.AUTOMOVE_FILE, true);
	            this.modClass.addLogEntry(this.configPrefix, (byte)3, "Check if you set the right file at config key: " + this.configPrefix + "_file", true);
	            br.close();
            } catch (Exception e)
            {
            	//TG SONARQUBE
            }
            return false;
        }
        try {
            this.AUTOMOVE_SGCHANNEL_LIST.clear();
            int count = 0;
            while ((line = br.readLine()) != null) {
                if (line.startsWith("#") || line.length() < 3) continue;
                if (count == 0) {
                    this.AUTOMOVE_MESSAGE = line = line.replace("\\n", "\n");
                }
                if (count >= 1) {
                    int pos = line.indexOf(",");
                    if (pos == -1 || pos == 0) continue;
                    try {
                        this.AUTOMOVE_SGCHANNEL_LIST.put(Integer.parseInt(line.substring(0, pos).trim()), Integer.parseInt(line.substring(pos + 1).trim()));
                    }
                    catch (Exception e) {
                        continue;
                    }
                }
                ++count;
            }
            br.close();
        }
        catch (FileNotFoundException fnfe) {
            this.modClass.addLogEntry(this.configPrefix, (byte)3, "Auto Move config file you set at config key \"" + this.configPrefix + "_file\" does not exist or missing permission for reading, check file path: " + this.AUTOMOVE_FILE, true);
            return false;
        }
        catch (Exception e) {
            this.modClass.addLogEntry(this.configPrefix, (byte)3, "Unknown error while loading Auto Move config file! Check file you set at config key \"" + this.configPrefix + "_file\", the file path: " + this.AUTOMOVE_FILE, true);
            this.modClass.addLogEntry(this.configPrefix, e, true);
            return false;
        }
        return true;
    }

    @Override
    public void setListModes(BitSet listOptions) {
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
        if (eventType.equals("notifycliententerview") && Integer.parseInt(eventInfo.get("client_type")) == 0 && Integer.parseInt(eventInfo.get("ctid")) == this.modClass.getDefaultChannelID()) {
            int clientID = Integer.parseInt(eventInfo.get("clid"));
            int channelID = this.getTargetChannel(eventInfo.get("client_servergroups"), this.AUTOMOVE_SGCHANNEL_LIST);
            if (channelID > 0) {
                String channelName = this.modClass.getChannelName(channelID);
                try {
                    this.queryLib.moveClient(clientID, channelID, null);
                    this.modClass.addLogEntry(this.configPrefix, (byte)1, "Client \"" + eventInfo.get("client_nickname") + "\" (db id: " + eventInfo.get("client_database_id") + ") has connected, moved to channel: " + (channelName != null ? new StringBuilder(String.valueOf(channelName)).append("(id: ").append(Integer.toString(channelID)).append(")").toString() : "Unknown"), false);
                    this.modClass.sendMessageToClient(this.configPrefix, this.AUTOMOVE_MESSAGE_MODE, clientID, this.AUTOMOVE_MESSAGE.replace("%CHANNEL_NAME%", channelName != null ? channelName : "Unknown"));
                }
                catch (Exception e) {
                    this.modClass.addLogEntry(this.configPrefix, (byte)1, "Client \"" + eventInfo.get("client_nickname") + "\" (db id: " + eventInfo.get("client_database_id") + ") has connected, but an error occurred while moving to channel: " + (channelName != null ? new StringBuilder(String.valueOf(channelName)).append("(id: ").append(Integer.toString(channelID)).append(")").toString() : "Unknown"), false);
                    this.modClass.addLogEntry(this.configPrefix, e, false);
                }
            }
        }
    }

    private int getTargetChannel(String groupIDs, HashMap<Integer, Integer> list) {
        StringTokenizer groupTokenizer = new StringTokenizer(groupIDs, ",", false);
        Integer channelID = null;
        while (groupTokenizer.hasMoreTokens()) {
            int groupID = Integer.parseInt(groupTokenizer.nextToken());
            channelID = list.get(groupID);
            if (channelID == null) continue;
            return channelID;
        }
        return -1;
    }
}

