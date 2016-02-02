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
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
public class BadChannelNameCheck
implements HandleBotEvents,
HandleTS3Events,
LoadConfiguration {
    private String configPrefix = "";
    private JTS3ServerMod_Interface modClass = null;
    private JTS3ServerQuery queryLib = null;
    private boolean pluginEnabled = false;
    private Vector<Integer> BADCHANNELNAME_CHANNEL_LIST = new Vector<>();
    private String BADCHANNELNAME_FILE = null;
    private byte BADCHANNELNAME_DELETE = 1;
    private boolean BADCHANNELNAME_KICK = false;
    private boolean BADCHANNELNAME_COMPLAINADD = false;
    private Vector<Integer> BADCHANNELNAME_GROUP_LIST = new Vector<>();
    private boolean BADCHANNELNAME_GROUP_LIST_IGNORE = true;
    private String BADCHANNELNAME_MESSAGE = null;
    private String BADCHANNELNAME_MESSAGE_MODE = null;
    private Vector<Pattern> BADCHANNELNAME_RULES = new Vector<>();

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
        String msg = "Bad Channel Name Check is enabled, " + Integer.toString(this.BADCHANNELNAME_RULES.size()) + " rules loaded";
        msg = this.BADCHANNELNAME_DELETE == 2 ? String.valueOf(msg) + " and matching channel names will be deleted!" : (this.BADCHANNELNAME_DELETE == 1 ? String.valueOf(msg) + " and matching channel names will be deleted, if rename is not possible!" : String.valueOf(msg) + "!");
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
        this.BADCHANNELNAME_CHANNEL_LIST = null;
        this.BADCHANNELNAME_GROUP_LIST = null;
        this.BADCHANNELNAME_RULES = null;
    }

    @Override
    public boolean multipleInstances() {
        return true;
    }

    @Override
    public void initConfig(ArrangedPropertiesWriter config) {
        config.addKey(String.valueOf(this.configPrefix) + "_kick", "Instantly kick client that creates a channel with a bad name? Set yes or no here!", "no");
        config.addKey(String.valueOf(this.configPrefix) + "_delete", "Instantly delete a channel with a bad name? Set always, onlynew or no here!\nonlynew = Try to rename channel back first. Only if that fail (maybe channel is quite new and the old name is unknown) the channel will be deleted!\nno = Try to rename channel back, but the channel will not be deleted!", "onlynew");
        config.addKey(String.valueOf(this.configPrefix) + "_channel_list", "A comma separated list (without spaces) of channel ids.\nThis channels will be ignored!");
        config.addKey(String.valueOf(this.configPrefix) + "_add_complain", "Add complain entry to the user? Set yes or no here!", "no");
        config.addKey(String.valueOf(this.configPrefix) + "_group_list", "A comma separated list (without spaces) of server group ids.\nDepends on the given mode, this server groups can be ignored or only this server groups will be checked!\nIf no server groups should be ignored, set no server groups here and select the group list mode ignore!");
        config.addKey(String.valueOf(this.configPrefix) + "_group_list_mode", "Select one of the two modes for the server group list.\nignore = The selected server groups will be ignored and can create channels with bad channel names.\nonly = Only the selected server groups will be checked.", "ignore");
        config.addKey(String.valueOf(this.configPrefix) + "_message_mode", "Select the message mode, how the client should get the message.\npoke, chat or none are valid values!\nIf client kick is activated, the message will be always used as kick message!", "poke");
        if (this.modClass.getMySQLConnection() == null) {
            config.addKey(String.valueOf(this.configPrefix) + "_file", "Path to file which contains the Bad Channel Name Check rules and the message.", "config/server1/badchannelname.cfg");
        }
        if (this.modClass.getMySQLConnection() != null) {
            config.addKey(String.valueOf(this.configPrefix) + "_message", "Set message for using a bad channel name.\nYou can use the following keywords, which will be replaced:\n%CHANNEL_NAME% - Bad Channel Name\nYou can use \\n for a new line and typical BBCode like in Teamspeak 3 Client.");
        }
    }

    @Override
    public boolean loadConfig(ArrangedPropertiesWriter config, boolean slowMode) throws BotConfigurationException, NumberFormatException {
        this.pluginEnabled = false;
        if (slowMode) {
            this.modClass.addLogEntry(this.configPrefix, (byte)3, "Slow Mode activated, Bad Channel Name Check disabled!", true);
            return false;
        }
        String lastNumberValue = "";
        String temp = null;
        try {
            StringTokenizer st;
            temp = config.getValue(String.valueOf(this.configPrefix) + "_delete", "yes").trim();
            this.BADCHANNELNAME_DELETE = temp.equalsIgnoreCase("yes") ? 2 : (temp.equalsIgnoreCase("onlynew") ? (byte)1 : 0);
            this.BADCHANNELNAME_KICK = config.getValue(String.valueOf(this.configPrefix) + "_kick", "yes").trim().equalsIgnoreCase("yes");
            this.BADCHANNELNAME_MESSAGE_MODE = config.getValue(String.valueOf(this.configPrefix) + "_message_mode", "chat").trim();
            this.BADCHANNELNAME_FILE = config.getValue(String.valueOf(this.configPrefix) + "_file");
            if (!this.loadBadChannelNameFile(config)) {
                throw new BotConfigurationException("Bad Channel Name Check message and rules could not be loaded!");
            }
            if ((this.BADCHANNELNAME_KICK || this.BADCHANNELNAME_MESSAGE_MODE.equalsIgnoreCase("chat") || this.BADCHANNELNAME_MESSAGE_MODE.equalsIgnoreCase("poke")) && !this.modClass.isMessageLengthValid(this.BADCHANNELNAME_KICK ? "kick" : this.BADCHANNELNAME_MESSAGE_MODE, this.BADCHANNELNAME_MESSAGE)) {
                this.modClass.addLogEntry(this.configPrefix, (byte)2, "Bad Nickname Check message is to long! Make sure that " + (this.BADCHANNELNAME_KICK ? "kick" : this.BADCHANNELNAME_MESSAGE_MODE) + " messages are not longer than " + Short.toString(this.modClass.getMaxMessageLength(this.BADCHANNELNAME_KICK ? "kick" : this.BADCHANNELNAME_MESSAGE_MODE)) + " characters (including spaces and BBCode)" + (this.modClass.getMySQLConnection() == null ? new StringBuilder(", check file: ").append(this.BADCHANNELNAME_FILE).toString() : ""), true);
            }
            temp = null;
            this.BADCHANNELNAME_CHANNEL_LIST.clear();
            temp = config.getValue(String.valueOf(this.configPrefix) + "_channel_list");
            lastNumberValue = String.valueOf(this.configPrefix) + "_channel_list";
            if (temp != null && temp.length() > 0) {
                st = new StringTokenizer(temp, ",", false);
                while (st.hasMoreTokens()) {
                    this.BADCHANNELNAME_CHANNEL_LIST.addElement(Integer.parseInt(st.nextToken().trim()));
                }
            }
            temp = null;
            this.BADCHANNELNAME_GROUP_LIST.clear();
            temp = config.getValue(String.valueOf(this.configPrefix) + "_group_list");
            lastNumberValue = String.valueOf(this.configPrefix) + "_group_list";
            if (temp != null && temp.length() > 0) {
                st = new StringTokenizer(temp, ",", false);
                while (st.hasMoreTokens()) {
                    this.BADCHANNELNAME_GROUP_LIST.addElement(Integer.parseInt(st.nextToken().trim()));
                }
            }
            this.BADCHANNELNAME_GROUP_LIST_IGNORE = !config.getValue(String.valueOf(this.configPrefix) + "_group_list_mode", "ignore").trim().equalsIgnoreCase("only");
            this.BADCHANNELNAME_COMPLAINADD = config.getValue(String.valueOf(this.configPrefix) + "_add_complain", "no").trim().equalsIgnoreCase("yes");
            if (this.BADCHANNELNAME_RULES.size() == 0) {
                throw new BotConfigurationException("No bad channel name rules was found! Please check configuration.");
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

    boolean loadBadChannelNameFile(ArrangedPropertiesWriter config) {
        BufferedReader br;
        String line;
        block35 : {
            if (this.modClass.getMySQLConnection() != null) {
                boolean retValue;
                MySQLConnect mysqlConnect = this.modClass.getMySQLConnection();
                this.BADCHANNELNAME_MESSAGE = config.getValue(String.valueOf(this.configPrefix) + "_message");
                retValue = false;
                PreparedStatement pst = null;
                ResultSet rs = null;
                try {
                    try {
                        mysqlConnect.connect();
                        pst = mysqlConnect.getPreparedStatement("SELECT textentry FROM jts3servermod_badchannelname WHERE instance_id = ? AND prefix = ?");
                        pst.setInt(1, this.modClass.getInstanceID());
                        pst.setString(2, this.configPrefix);
                        rs = pst.executeQuery();
                        this.BADCHANNELNAME_RULES.clear();
                        while (rs.next()) {
                            this.BADCHANNELNAME_RULES.addElement(Pattern.compile(rs.getString(1), 66));
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
                        catch (Exception var8_12) {
                            // empty catch block
                        }
                        try {
                            if (pst != null) {
                                pst.close();
                            }
                        }
                        catch (Exception var8_13) {
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
                    catch (Exception var8_16) {}
                    try {
                        if (pst != null) {
                            pst.close();
                        }
                    }
                    catch (Exception var8_17) {}
                    mysqlConnect.close();
                }
                return retValue;
            }
            if (this.BADCHANNELNAME_FILE == null) {
                this.modClass.addLogEntry(this.configPrefix, (byte)3, "Path to Bad Channel Name Check config file was not set in bot config! Check config key: " + this.configPrefix + "_file", true);
                return false;
            }
            try {
	            this.BADCHANNELNAME_FILE = this.BADCHANNELNAME_FILE.trim();
	            br = new BufferedReader(new InputStreamReader((InputStream)new FileInputStream(this.BADCHANNELNAME_FILE), this.modClass.getMessageEncoding()));
	            this.BADCHANNELNAME_RULES.clear();
	            line = br.readLine();
	            if (this.modClass.getMessageEncoding().equalsIgnoreCase("UTF-8") && line != null && line.charAt(0) == '\ufeff') {
	                line = line.substring(1);
	            }
	            if (line != null && line.equals("# JTS3ServerMod Config File")) break block35;
	            this.modClass.addLogEntry(this.configPrefix, (byte)3, "Special config file header is missing at Bad Channel Name Check config file! File path: " + this.BADCHANNELNAME_FILE, true);
	            this.modClass.addLogEntry(this.configPrefix, (byte)3, "Check if you set the right file at config key: " + this.configPrefix + "_file", true);
	            br.close();
            } catch (Exception e)
            {
            	//TG SONARQUBE
            }
            return false;
        }
        try {
            int count = 0;
            while ((line = br.readLine()) != null) {
                if (line.startsWith("#") || line.length() <= 3) continue;
                if (count == 0) {
                    this.BADCHANNELNAME_MESSAGE = line = line.replace("\\n", "\n");
                }
                if (count >= 1) {
                    this.BADCHANNELNAME_RULES.addElement(Pattern.compile(line, 66));
                }
                ++count;
            }
            br.close();
        }
        catch (FileNotFoundException fnfe) {
            this.modClass.addLogEntry(this.configPrefix, (byte)3, "Bad Channel Name Check config file you set at config key \"" + this.configPrefix + "_file\" does not exist or missing permission for reading, check file: " + this.BADCHANNELNAME_FILE, true);
            return false;
        }
        catch (Exception e) {
            this.modClass.addLogEntry(this.configPrefix, (byte)3, "Unknown error while loading Bad Channel Name Check config file! Check file you set at config key \"" + this.configPrefix + "_file\", the file path: " + this.BADCHANNELNAME_FILE, true);
            this.modClass.addLogEntry(this.configPrefix, e, true);
            return false;
        }
        return true;
    }

    @Override
    public void setListModes(BitSet listOptions) {
        listOptions.set(1);
    }

    @Override
    public String[] botChatCommandList(HashMap<String, String> eventInfo, boolean isFullAdmin, boolean isAdmin) {
        if (!this.pluginEnabled) {
            return null;
        }
        if (isFullAdmin || isAdmin) {
            String[] commands = new String[]{"check"};
            return commands;
        }
        return null;
    }

    @Override
    public String botChatCommandHelp(String command) {
        if (command.equalsIgnoreCase("check")) {
            return "Display a list of current channels with bad names (because this function don't see channel name changes, if bot was offline).";
        }
        return null;
    }

    @Override
    public boolean handleChatCommands(String msg, HashMap<String, String> eventInfo, boolean isFullAdmin, boolean isAdmin) {
        if (!this.pluginEnabled) {
            return false;
        }
        try {
            if (msg.equalsIgnoreCase("check")) {
                if (isFullAdmin || isAdmin) {
                    StringBuffer sb = new StringBuffer("");
                    int count = 0;
                    Vector<HashMap<String, String>> channelList = this.modClass.getChannelList();
                    for (HashMap<String, String> channel : channelList) {
                        try {
                            int channelID = Integer.parseInt(channel.get("cid"));
                            if (this.modClass.isIDListed(channelID, this.BADCHANNELNAME_CHANNEL_LIST)) continue;
                            for (Pattern rule : this.BADCHANNELNAME_RULES) {
                                Matcher ruleCheck = rule.matcher(channel.get("channel_name"));
                                if (!ruleCheck.matches()) continue;
                                sb.append("\n" + channel.get("channel_name") + " (id: " + channel.get("cid") + ")");
                                ++count;
                            }
                            continue;
                        }
                        catch (Exception rule) {
                            // empty catch block
                        }
                    }
                    if (count == 0) {
                        this.queryLib.sendTextMessage(Integer.parseInt(eventInfo.get("invokerid")), 1, "No bad channel names found!");
                    } else {
                        this.queryLib.sendTextMessage(Integer.parseInt(eventInfo.get("invokerid")), 1, "Found " + Integer.toString(count) + " channels with bad names!\nYou can use the following command to rename that channels: !setchannelname <channel id> <new channel name>");
                        this.queryLib.sendTextMessage(Integer.parseInt(eventInfo.get("invokerid")), 1, sb.toString());
                    }
                } else {
                    this.queryLib.sendTextMessage(Integer.parseInt(eventInfo.get("invokerid")), 1, "You are not my master!");
                }
                return true;
            }
            return false;
        }
        catch (Exception e) {
            this.modClass.addLogEntry(this.configPrefix, e, false);
            return true;
        }
    }

    @Override
    public void handleClientEvents(String eventType, HashMap<String, String> eventInfo) {
        int channelID;
        if (!this.pluginEnabled) {
            return;
        }
        if ((eventType.equals("notifychannelcreated") || eventType.equals("notifychanneledited")) && eventInfo.get("channel_name") != null && !this.modClass.isIDListed(channelID = Integer.parseInt(eventInfo.get("cid")), this.BADCHANNELNAME_CHANNEL_LIST)) {
            for (Pattern rule : this.BADCHANNELNAME_RULES) {
                String oldChannelName;
                Matcher ruleCheck = rule.matcher(eventInfo.get("channel_name"));
                if (!ruleCheck.matches()) continue;
                int clientID = Integer.parseInt(eventInfo.get("invokerid"));
                int clientType = 0;
                String sDBID = "";
                String sServerGroups = "";
                Vector<HashMap<String, String>> clientList = this.modClass.getClientList();
                for (HashMap<String, String> client : clientList) {
                    if (!client.get("clid").equals(eventInfo.get("invokerid"))) continue;
                    sDBID = client.get("client_database_id");
                    sServerGroups = client.get("client_servergroups");
                    try {
                        clientType = Integer.parseInt(client.get("client_type"));
                    }
                    catch (Exception var14_17) {}
                    break;
                }
                boolean result = this.modClass.isGroupListed(sServerGroups, this.BADCHANNELNAME_GROUP_LIST);
                if (!(this.BADCHANNELNAME_GROUP_LIST_IGNORE ? !result : result)) continue;
                boolean wasRenamed = false;
                if (this.BADCHANNELNAME_DELETE != 2 && eventType.equals("notifychanneledited") && !(oldChannelName = this.modClass.getChannelName(channelID)).equalsIgnoreCase(eventInfo.get("channel_name")) && this.queryLib.doCommand("channeledit cid=" + Integer.toString(channelID) + " channel_name=" + this.queryLib.encodeTS3String(oldChannelName)).get("id").equals("0")) {
                    this.modClass.addLogEntry(this.configPrefix, (byte)1, "Channel created by \"" + eventInfo.get("invokername") + "\" (db id: " + sDBID + ") was renamed back to last saved name \"" + oldChannelName + "\", bad channel name: " + eventInfo.get("channel_name"), false);
                    wasRenamed = true;
                }
                if (this.BADCHANNELNAME_DELETE == 2 || !wasRenamed && this.BADCHANNELNAME_DELETE == 1) {
                    try {
                        this.queryLib.deleteChannel(channelID, true);
                        this.modClass.addLogEntry(this.configPrefix, (byte)1, "Channel created by \"" + eventInfo.get("invokername") + "\" (db id: " + sDBID + ") was deleted, channel name: " + eventInfo.get("channel_name"), false);
                    }
                    catch (Exception e) {
                        this.modClass.addLogEntry(this.configPrefix, (byte)3, "Can't delete channel \"" + eventInfo.get("channel_name") + "\" (id: " + Integer.toString(channelID) + ") created by \"" + eventInfo.get("invokername") + "\" (db id: " + sDBID + ")!", false);
                        this.modClass.addLogEntry(this.configPrefix, e, false);
                    }
                }
                if (this.BADCHANNELNAME_COMPLAINADD) {
                    try {
                        this.queryLib.complainAdd(Integer.parseInt(sDBID), "Created channel with bad name: " + eventInfo.get("channel_name"));
                        this.modClass.addLogEntry(this.configPrefix, (byte)1, "Added complaint to client \"" + eventInfo.get("invokername") + "\" (db id: " + sDBID + "), reason: Created a channel with the bad name: " + eventInfo.get("channel_name"), false);
                    }
                    catch (Exception e) {
                        this.modClass.addLogEntry(this.configPrefix, (byte)3, "Error while adding complaint to client \"" + eventInfo.get("invokername") + "\" (db id: " + sDBID + "), reason: Created a channel with the bad name: " + eventInfo.get("channel_name"), false);
                        this.modClass.addLogEntry(this.configPrefix, e, false);
                    }
                }
                if (this.BADCHANNELNAME_KICK) {
                    try {
                        this.queryLib.kickClient(clientID, false, this.createMessage(eventInfo.get("channel_name")));
                        this.modClass.addLogEntry(this.configPrefix, (byte)1, "Client \"" + eventInfo.get("invokername") + "\" (db id: " + sDBID + ") was kicked, channel name \"" + eventInfo.get("channel_name") + "\" matched bad channel name rules!", false);
                    }
                    catch (Exception e) {
                        this.modClass.addLogEntry(this.configPrefix, (byte)3, "Can't kick client \"" + eventInfo.get("invokername") + "\" (db id: " + sDBID + ") for using the bad channel name: " + eventInfo.get("channel_name"), false);
                        this.modClass.addLogEntry(this.configPrefix, e, false);
                    }
                } else if ((this.BADCHANNELNAME_MESSAGE_MODE.equals("poke") || this.BADCHANNELNAME_MESSAGE_MODE.equals("chat")) && clientType == 0) {
                    this.modClass.sendMessageToClient(this.configPrefix, this.BADCHANNELNAME_MESSAGE_MODE, clientID, this.createMessage(eventInfo.get("channel_name")));
                }
                if (wasRenamed || this.BADCHANNELNAME_DELETE != 0 || this.BADCHANNELNAME_COMPLAINADD || this.BADCHANNELNAME_KICK) continue;
                this.modClass.addLogEntry(this.configPrefix, (byte)2, "Channel created by \"" + eventInfo.get("invokername") + "\" (db id: " + sDBID + "), channel name: " + eventInfo.get("channel_name"), false);
            }
        }
    }

    private String createMessage(String channelName) {
        return this.BADCHANNELNAME_MESSAGE.replace("%CHANNEL_NAME%", channelName);
    }
}

