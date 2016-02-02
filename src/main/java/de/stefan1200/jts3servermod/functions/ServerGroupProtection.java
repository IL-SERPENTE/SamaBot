/*
 * Decompiled with CFR 0_110.
 */
package de.stefan1200.jts3servermod.functions;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.BitSet;
import java.util.HashMap;
import java.util.Iterator;
import java.util.StringTokenizer;
import java.util.Vector;

import de.stefan1200.jts3servermod.BotConfigurationException;
import de.stefan1200.jts3servermod.FunctionExceptionLog;
import de.stefan1200.jts3servermod.interfaces.HandleBotEvents;
import de.stefan1200.jts3servermod.interfaces.HandleClientList;
import de.stefan1200.jts3servermod.interfaces.HandleTS3Events;
import de.stefan1200.jts3servermod.interfaces.JTS3ServerMod_Interface;
import de.stefan1200.jts3servermod.interfaces.LoadConfiguration;
import de.stefan1200.jts3serverquery.JTS3ServerQuery;
import de.stefan1200.jts3serverquery.TS3ServerQueryException;
import de.stefan1200.util.ArrangedPropertiesWriter;
import de.stefan1200.util.MySQLConnect;

/*
 * This class specifies class file version 49.0 but uses Java 6 signatures.  Assumed Java 6.
 */
public class ServerGroupProtection
implements HandleBotEvents,
HandleTS3Events,
LoadConfiguration,
HandleClientList {
    private String configPrefix = "";
    private JTS3ServerMod_Interface modClass = null;
    private JTS3ServerQuery queryLib = null;
    private boolean pluginEnabled = false;
    private String SERVERGROUPPROTECTION_FILE = null;
    private String SERVERGROUPPROTECTION_MESSAGE_MODE = null;
    private String SERVERGROUPPROTECTION_MESSAGE = null;
    private boolean SERVERGROUPPROTECTION_ADD_MISSING_GROUPS = false;
    private boolean SERVERGROUPPROTECTION_KICK = false;
    private boolean SERVERGROUPPROTECTION_COMPLAINADD = false;
    private Vector<Integer> SERVERGROUPPROTECTION_GROUPS = new Vector<>();
    private Vector<Vector<String>> SERVERGROUPPROTECTION_CLIENTS = new Vector<>();
    private Vector<Vector<String>> SERVERGROUPPROTECTION_COMMENTS = new Vector<>();
    private Vector<Boolean> SERVERGROUPPROTECTION_ADDALLOWED = new Vector<>();
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
        StringBuffer sb = new StringBuffer();
        Iterator<Integer> iterator = this.SERVERGROUPPROTECTION_GROUPS.iterator();
        while (iterator.hasNext()) {
            int groupID = iterator.next();
            if (sb.length() != 0) {
                sb.append(", ");
            }
            sb.append(groupID);
        }
        String msg = "Server Group Protection will remove not allowed members from protected server groups (id: " + sb.toString() + ")" + (this.SERVERGROUPPROTECTION_KICK ? " and kick them" : " but they will not kicked") + (this.SERVERGROUPPROTECTION_COMPLAINADD ? " (complaint will be added)" : "");
        this.modClass.addLogEntry(this.configPrefix, (byte)1, msg, true);
    }

    @Override
    public void handleAfterCacheUpdate() {
        int i = 0;
        while (i < this.SERVERGROUPPROTECTION_GROUPS.size()) {
            this.SERVERGROUPPROTECTION_ADDALLOWED.setElementAt(this.modClass.getServerGroupType(this.SERVERGROUPPROTECTION_GROUPS.elementAt(i)) == 1, i);
            ++i;
        }
    }

    @Override
    public void activate() {
        this.checkEmptyServerGroups();
    }

    @Override
    public void disable() {
    }

    @Override
    public void unload() {
        this.SERVERGROUPPROTECTION_GROUPS = null;
        this.SERVERGROUPPROTECTION_CLIENTS = null;
        this.SERVERGROUPPROTECTION_COMMENTS = null;
        this.SERVERGROUPPROTECTION_ADDALLOWED = null;
    }

    @Override
    public boolean multipleInstances() {
        return true;
    }

    @Override
    public void initConfig(ArrangedPropertiesWriter config) {
        config.addKey(String.valueOf(this.configPrefix) + "_groups", "A comma separated list (without spaces) of server group ids, which should be protected.");
        config.addKey(String.valueOf(this.configPrefix) + "_kick", "Enable this to kick every client which using a protected server group and are not on the list of the bot, set yes or no here!", "yes");
        config.addKey(String.valueOf(this.configPrefix) + "_add_complain", "Add complaint entry to the user, set yes or no here!\nThis would only add a complaint, if the bot has to remove a server group.", "no");
        config.addKey(String.valueOf(this.configPrefix) + "_add_missing_groups", "If a client is listed in the servergroupprotection_file and miss a server group, they get added to the server group.\nThis only works for normal server groups (clients do not get added to groups like Admin Server Query)! Set yes or no here!", "yes");
        config.addKey(String.valueOf(this.configPrefix) + "_message_mode", "Select the message mode, how the client should get the message (useless if kick is enabled).\npoke, chat or none are valid values!", "poke");
        if (this.modClass.getMySQLConnection() == null) {
            config.addKey(String.valueOf(this.configPrefix) + "_file", "Path to file which contains the Server Group Protection client list and kick message.", "config/server1/servergroupprotection.cfg");
        }
        if (this.modClass.getMySQLConnection() != null) {
            config.addKey(String.valueOf(this.configPrefix) + "_message", "The kick or chat message for the server group protection.\nYou can use the following keywords, which will be replaced:\n%SERVER_GROUP_ID% - Replaced with the server group id.\n%SERVER_GROUP_NAME% - Replaced with the server group name.\nYou can use \\n for a new line and typical BBCode like in Teamspeak 3 Client.");
        }
    }

    @Override
    public boolean loadConfig(ArrangedPropertiesWriter config, boolean slowMode) throws BotConfigurationException, NumberFormatException {
        String lastNumberValue = "";
        String temp = null;
        this.pluginEnabled = false;
        try {
            temp = null;
            this.SERVERGROUPPROTECTION_ADDALLOWED.clear();
            this.SERVERGROUPPROTECTION_GROUPS.clear();
            temp = config.getValue(String.valueOf(this.configPrefix) + "_groups");
            lastNumberValue = String.valueOf(this.configPrefix) + "_groups";
            if (temp != null && temp.length() > 0) {
                int groupID = 0;
                StringTokenizer st = new StringTokenizer(temp, ",", false);
                while (st.hasMoreTokens()) {
                    groupID = Integer.parseInt(st.nextToken().trim());
                    this.SERVERGROUPPROTECTION_GROUPS.addElement(groupID);
                    this.SERVERGROUPPROTECTION_ADDALLOWED.addElement(this.modClass.getServerGroupType(groupID) == 1);
                }
            } else {
                throw new BotConfigurationException("Server Group Protection needs at least one server group set! Check config key: " + this.configPrefix + "_groups");
            }
            this.SERVERGROUPPROTECTION_COMPLAINADD = config.getValue(String.valueOf(this.configPrefix) + "_add_complain", "no").trim().equalsIgnoreCase("yes");
            this.SERVERGROUPPROTECTION_ADD_MISSING_GROUPS = config.getValue(String.valueOf(this.configPrefix) + "_add_missing_groups", "no").trim().equalsIgnoreCase("yes");
            this.SERVERGROUPPROTECTION_KICK = config.getValue(String.valueOf(this.configPrefix) + "_kick", "no").trim().equalsIgnoreCase("yes");
            if (this.SERVERGROUPPROTECTION_KICK) {
                this.SERVERGROUPPROTECTION_MESSAGE_MODE = "kick";
            }
            this.SERVERGROUPPROTECTION_MESSAGE_MODE = config.getValue(String.valueOf(this.configPrefix) + "_message_mode", "chat").trim();
            this.SERVERGROUPPROTECTION_FILE = config.getValue(String.valueOf(this.configPrefix) + "_file");
            if (!this.loadServerGroupProtectionFile(config)) {
                throw new BotConfigurationException("Server Group Protection configuration does not exists or error while loading!");
            }
            if (!this.modClass.isMessageLengthValid(this.SERVERGROUPPROTECTION_MESSAGE_MODE, this.SERVERGROUPPROTECTION_MESSAGE)) {
                this.modClass.addLogEntry(this.configPrefix, (byte)2, "Server Group Protection message is to long! Make sure that " + this.SERVERGROUPPROTECTION_MESSAGE_MODE + " messages are not longer than " + Short.toString(this.modClass.getMaxMessageLength(this.SERVERGROUPPROTECTION_MESSAGE_MODE)) + " characters (including spaces and BBCode)" + (this.modClass.getMySQLConnection() == null ? new StringBuilder(", check file: ").append(this.SERVERGROUPPROTECTION_FILE).toString() : ""), true);
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

    boolean loadServerGroupProtectionFile(ArrangedPropertiesWriter config) {
        int indexPos;
        BufferedReader br;
        String line;
        block42 : {
            if (this.modClass.getMySQLConnection() != null) {
                boolean retValue;
                MySQLConnect mysqlConnect = this.modClass.getMySQLConnection();
                this.SERVERGROUPPROTECTION_MESSAGE = config.getValue(String.valueOf(this.configPrefix) + "_message");
                retValue = false;
                PreparedStatement pst = null;
                ResultSet rs = null;
                try {
                    try {
                        mysqlConnect.connect();
                        pst = mysqlConnect.getPreparedStatement("SELECT servergroup_id, client_unique_id, comment FROM jts3servermod_servergroupprotection WHERE instance_id = ? AND prefix = ?");
                        pst.setInt(1, this.modClass.getInstanceID());
                        pst.setString(2, this.configPrefix);
                        rs = pst.executeQuery();
                        rs.last();
                        int rowCount = rs.getRow();
                        this.SERVERGROUPPROTECTION_CLIENTS.clear();
                        this.SERVERGROUPPROTECTION_COMMENTS.clear();
                        int i = 0;
                        while (i < this.SERVERGROUPPROTECTION_GROUPS.size()) {
                            this.SERVERGROUPPROTECTION_CLIENTS.addElement(new Vector<>());
                            this.SERVERGROUPPROTECTION_COMMENTS.addElement(new Vector<>());
                            ++i;
                        }
                        if (rowCount > 0) {
                            rs.beforeFirst();
                            while (rs.next()) {
                                try {
                                    indexPos = this.SERVERGROUPPROTECTION_GROUPS.indexOf(rs.getInt(1));
                                    if (indexPos == -1) continue;
                                    this.SERVERGROUPPROTECTION_CLIENTS.elementAt(indexPos).addElement(rs.getString(2).trim());
                                    this.SERVERGROUPPROTECTION_COMMENTS.elementAt(indexPos).addElement(rs.getString(3).trim());
                                    continue;
                                }
                                catch (Exception e) {
                                    // empty catch block
                                }
                            }
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
                        catch (Exception var9_18) {
                            // empty catch block
                        }
                        try {
                            if (pst != null) {
                                pst.close();
                            }
                        }
                        catch (Exception var9_19) {
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
                    catch (Exception var9_22) {}
                    try {
                        if (pst != null) {
                            pst.close();
                        }
                    }
                    catch (Exception var9_23) {}
                    mysqlConnect.close();
                }
                return retValue;
            }
            if (this.SERVERGROUPPROTECTION_FILE == null) {
                this.modClass.addLogEntry(this.configPrefix, (byte)3, "Path to Server Group Protection config file was not set in bot config! Check config key: " + this.configPrefix + "_file", true);
                return false;
            }
            try {
	            this.SERVERGROUPPROTECTION_FILE = this.SERVERGROUPPROTECTION_FILE.trim();
	            br = new BufferedReader(new InputStreamReader((InputStream)new FileInputStream(this.SERVERGROUPPROTECTION_FILE), this.modClass.getMessageEncoding()));
	            line = br.readLine();
	            if (this.modClass.getMessageEncoding().equalsIgnoreCase("UTF-8") && line != null && line.charAt(0) == '\ufeff') {
	                line = line.substring(1);
	            }
	            if (line != null && line.equals("# JTS3ServerMod Config File")) break block42;
	            this.modClass.addLogEntry(this.configPrefix, (byte)3, "Special config file header is missing at Server Group Protection config file! File path: " + this.SERVERGROUPPROTECTION_FILE, true);
	            this.modClass.addLogEntry(this.configPrefix, (byte)3, "Check if you set the right file at config key: " + this.configPrefix + "_file", true);
	            br.close();
            } catch (Exception e)
            {
            	//TG
            }
            return false;
        }
        try {
            this.SERVERGROUPPROTECTION_CLIENTS.clear();
            this.SERVERGROUPPROTECTION_COMMENTS.clear();
            int i = 0;
            while (i < this.SERVERGROUPPROTECTION_GROUPS.size()) {
                this.SERVERGROUPPROTECTION_CLIENTS.addElement(new Vector<>());
                this.SERVERGROUPPROTECTION_COMMENTS.addElement(new Vector<>());
                ++i;
            }
            int count = 0;
            while ((line = br.readLine()) != null) {
                if (line.startsWith("#") || line.length() <= 3) continue;
                if (count == 0) {
                    this.SERVERGROUPPROTECTION_MESSAGE = line = line.replace("\\n", "\n");
                }
                if (count >= 1) {
                    int pos = line.indexOf(",");
                    int pos2 = line.indexOf(" ", pos + 10);
                    if (pos == -1 || pos == 0) continue;
                    try {
                        indexPos = this.SERVERGROUPPROTECTION_GROUPS.indexOf(Integer.parseInt(line.substring(0, pos)));
                        if (indexPos == -1) continue;
                        String uidTemp = pos2 == -1 ? line.substring(pos + 1).trim() : line.substring(pos + 1, pos2).trim();
                        this.SERVERGROUPPROTECTION_CLIENTS.elementAt(indexPos).addElement(uidTemp);
                        this.SERVERGROUPPROTECTION_COMMENTS.elementAt(indexPos).addElement(line.substring(pos2));
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
            this.modClass.addLogEntry(this.configPrefix, (byte)3, "Server Group Protection config file you set at config key \"" + this.configPrefix + "_file\" does not exist or missing permission for reading, check file path: " + this.SERVERGROUPPROTECTION_FILE, true);
            return false;
        }
        catch (Exception e) {
            this.modClass.addLogEntry(this.configPrefix, (byte)3, "Unknown error while loading Server Group Protection config file! Check file you set at config key \"" + this.configPrefix + "_file\", the file path: " + this.SERVERGROUPPROTECTION_FILE, true);
            this.modClass.addLogEntry(this.configPrefix, e, true);
            return false;
        }
        return true;
    }

    private boolean saveServerGroupProtectionFile() {
        if (this.modClass.getMySQLConnection() != null) {
            boolean retValue;
            MySQLConnect mysqlConnect = this.modClass.getMySQLConnection();
            retValue = false;
            PreparedStatement pst = null;
            try {
                try {
                    mysqlConnect.connect();
                    pst = mysqlConnect.getPreparedStatement("DELETE FROM jts3servermod_servergroupprotection WHERE instance_id = " + Integer.toString(this.modClass.getInstanceID()) + " AND prefix = ?");
                    pst.setString(1, this.configPrefix);
                    pst.executeUpdate();
                    pst = mysqlConnect.getPreparedStatement("INSERT INTO jts3servermod_servergroupprotection (instance_id, prefix, servergroup_id, client_unique_id, comment) VALUES (" + Integer.toString(this.modClass.getInstanceID()) + ", ?, ?, ?, ?)");
                    if (this.SERVERGROUPPROTECTION_CLIENTS != null && this.SERVERGROUPPROTECTION_GROUPS != null) {
                        int i = 0;
                        while (i < this.SERVERGROUPPROTECTION_CLIENTS.size()) {
                            int j = 0;
                            while (j < this.SERVERGROUPPROTECTION_CLIENTS.elementAt(i).size()) {
                                pst.setString(1, this.configPrefix);
                                pst.setInt(2, this.SERVERGROUPPROTECTION_GROUPS.elementAt(i));
                                pst.setString(3, this.SERVERGROUPPROTECTION_CLIENTS.elementAt(i).elementAt(j));
                                pst.setString(4, this.SERVERGROUPPROTECTION_COMMENTS.elementAt(i).elementAt(j));
                                pst.executeUpdate();
                                ++j;
                            }
                            ++i;
                        }
                    }
                    retValue = true;
                }
                catch (Exception e) {
                    retValue = false;
                    mysqlConnect.close();
                }
            }
            finally {
                mysqlConnect.close();
            }
            return retValue;
        }
        if (this.SERVERGROUPPROTECTION_FILE == null) {
            return false;
        }
        try {
            PrintStream ps = new PrintStream(this.SERVERGROUPPROTECTION_FILE, this.modClass.getMessageEncoding());
            ps.println("# JTS3ServerMod Config File");
            ps.println("# The first line is the kick or chat message for the Server Group Protection.");
            ps.println("# You can use the following keywords, which will be replaced:");
            ps.println("# %SERVER_GROUP_ID% - Replaced with the server group id.");
            ps.println("# %SERVER_GROUP_NAME% - Replaced with the server group name.");
            ps.println("# Typical BBCode like in Teamspeak 3 Client possible.");
            if (this.SERVERGROUPPROTECTION_MESSAGE == null) {
                ps.println();
            } else {
                ps.println(this.SERVERGROUPPROTECTION_MESSAGE.replace("\n", "\\n"));
            }
            ps.println();
            ps.println("# This is the list of allowed clients in the protected server groups.");
            ps.println("# One line per client starting with the server group id, followed by a comma,");
            ps.println("# and ends with the unique id of the client.");
            ps.println("# Comments separated with a space behind the unique ids are allowed.");
            ps.println("# If a client is member of two protected groups, make two lines with the");
            ps.println("# same unique id, but different server group id.");
            ps.println("# Notice: If no clients are set for one server group, a list");
            ps.println("# with the current members of the server group will be requested from the TS3 server");
            ps.println("# and written into this file automatically!");
            if (this.SERVERGROUPPROTECTION_CLIENTS != null && this.SERVERGROUPPROTECTION_GROUPS != null) {
                int i = 0;
                while (i < this.SERVERGROUPPROTECTION_CLIENTS.size()) {
                    int j = 0;
                    while (j < this.SERVERGROUPPROTECTION_CLIENTS.elementAt(i).size()) {
                        ps.print(this.SERVERGROUPPROTECTION_GROUPS.elementAt(i));
                        ps.print(",");
                        ps.print(this.SERVERGROUPPROTECTION_CLIENTS.elementAt(i).elementAt(j));
                        ps.println(this.SERVERGROUPPROTECTION_COMMENTS.elementAt(i).elementAt(j));
                        ++j;
                    }
                    ++i;
                }
            }
            ps.flush();
            ps.close();
        }
        catch (Exception e) {
            this.modClass.addLogEntry(this.configPrefix, e, true);
            return false;
        }
        return true;
    }

    int addServerGroupProtectionEntry(int serverGroupID, String clientUniqueID, String comment, boolean save) {
        if (this.SERVERGROUPPROTECTION_CLIENTS == null || this.SERVERGROUPPROTECTION_GROUPS == null) {
            return -3;
        }
        int posGroup = this.SERVERGROUPPROTECTION_GROUPS.indexOf(serverGroupID);
        if (posGroup < 0) {
            return -2;
        }
        int posClient = this.SERVERGROUPPROTECTION_CLIENTS.elementAt(posGroup).indexOf(clientUniqueID);
        if (posClient >= 0) {
            return 0;
        }
        this.SERVERGROUPPROTECTION_CLIENTS.elementAt(posGroup).addElement(clientUniqueID);
        if (this.modClass.getMySQLConnection() == null) {
            this.SERVERGROUPPROTECTION_COMMENTS.elementAt(posGroup).addElement("   # " + comment);
        } else {
            this.SERVERGROUPPROTECTION_COMMENTS.elementAt(posGroup).addElement(comment);
        }
        if (save) {
            return this.saveServerGroupProtectionFile() ? 1 : -1;
        }
        return 1;
    }

    int removeServerGroupProtectionEntry(int serverGroupID, String clientUniqueID) {
        if (this.SERVERGROUPPROTECTION_CLIENTS == null || this.SERVERGROUPPROTECTION_GROUPS == null) {
            return -3;
        }
        int posGroup = this.SERVERGROUPPROTECTION_GROUPS.indexOf(serverGroupID);
        if (posGroup < 0) {
            return -2;
        }
        int posClient = this.SERVERGROUPPROTECTION_CLIENTS.elementAt(posGroup).indexOf(clientUniqueID);
        if (posClient < 0) {
            return 0;
        }
        this.SERVERGROUPPROTECTION_CLIENTS.elementAt(posGroup).remove(posClient);
        this.SERVERGROUPPROTECTION_COMMENTS.elementAt(posGroup).remove(posClient);
        return this.saveServerGroupProtectionFile() ? 1 : -1;
    }

    @Override
    public void setListModes(BitSet listOptions) {
        listOptions.set(1);
        listOptions.set(4);
    }

    @Override
    public String[] botChatCommandList(HashMap<String, String> eventInfo, boolean isFullAdmin, boolean isAdmin) {
        if (isFullAdmin) {
            String[] cmdList = new String[]{"add <server group id> <client unique id> [comment]", "remove <server group id> <client unique id>"};
            return cmdList;
        }
        return null;
    }

    @Override
    public String botChatCommandHelp(String command) {
        if (command.equals("add")) {
            return "Adds the unique id of a client to a protected server group (this will also saved into config).";
        }
        if (command.equals("remove")) {
            return "Removes a client unique id from a protected server group (this will also saved into config).";
        }
        return null;
    }

    /*
     * Enabled force condition propagation
     * Lifted jumps to return sites
     */
    @Override
    public boolean handleChatCommands(String msg, HashMap<String, String> eventInfo, boolean isFullAdmin, boolean isAdmin) {
        if (isFullAdmin) {
            if (msg.toLowerCase().startsWith("add ") || msg.toLowerCase().equals("add")) {
                this.handleAddCommand(msg, eventInfo);
                return true;
            } else {
                if (!msg.toLowerCase().startsWith("remove ") && !msg.toLowerCase().equals("remove")) return false;
                this.handleRemoveCommand(msg, eventInfo);
            }
            return true;
        }
        try {
            this.queryLib.sendTextMessage(Integer.parseInt(eventInfo.get("invokerid")), 1, "You are not my master! You have to be full bot admin to use this command.");
            return true;
        }
        catch (Exception e) {
            this.modClass.addLogEntry(this.configPrefix, e, false);
        }
        return true;
    }

    private void handleAddCommand(String msg, HashMap<String, String> eventInfo) {
        block24 : {
            try {
                if (msg.length() < 30) {
                    this.queryLib.sendTextMessage(Integer.parseInt(eventInfo.get("invokerid")), 1, "Wrong usage! Right: !" + this.configPrefix + " add <server group id> <client unique id> [comment]");
                    break block24;
                }
                try {
                    StringTokenizer st = new StringTokenizer(msg.substring(4), " ", false);
                    int serverGroupID = -1;
                    String clientUniqueID = null;
                    String comment = "";
                    String tmp = st.nextToken().trim();
                    try {
                        serverGroupID = Integer.parseInt(tmp);
                        tmp = st.nextToken().trim();
                        clientUniqueID = new String(tmp);
                        if (st.hasMoreTokens()) {
                            tmp = st.nextToken().trim();
                            comment = new String(tmp);
                        }
                    }
                    catch (Exception e) {
                        try {
                            clientUniqueID = new String(tmp);
                            tmp = st.nextToken().trim();
                            serverGroupID = Integer.parseInt(tmp);
                            if (st.hasMoreTokens()) {
                                tmp = st.nextToken().trim();
                                comment = new String(tmp);
                            }
                        }
                        catch (Exception var9_12) {
                            // empty catch block
                        }
                    }
                    if (clientUniqueID.length() < 20) {
                        this.queryLib.sendTextMessage(Integer.parseInt(eventInfo.get("invokerid")), 1, "Error while reading given client unique id!");
                    } else if (serverGroupID < 1) {
                        this.queryLib.sendTextMessage(Integer.parseInt(eventInfo.get("invokerid")), 1, "Error while reading given server group id!");
                    } else {
                        int retValue = this.addServerGroupProtectionEntry(serverGroupID, clientUniqueID, comment, true);
                        if (retValue == 1) {
                            this.queryLib.sendTextMessage(Integer.parseInt(eventInfo.get("invokerid")), 1, "Successfully added client to protected server group!");
                        } else if (retValue == 0) {
                            this.queryLib.sendTextMessage(Integer.parseInt(eventInfo.get("invokerid")), 1, "Client was already on that list for server group " + Integer.toString(serverGroupID) + "!");
                        } else if (retValue == -1) {
                            this.queryLib.sendTextMessage(Integer.parseInt(eventInfo.get("invokerid")), 1, "Unable to save server group protection configuration file! Check if the configuration file is write protected!");
                        } else if (retValue == -2) {
                            this.queryLib.sendTextMessage(Integer.parseInt(eventInfo.get("invokerid")), 1, "Unable to add client to protected server group! Server group protection feature must be enabled and server group " + Integer.toString(serverGroupID) + " has to be on the watch list of the server group protection feature. Please make sure that this feature is enabled and add this server group to config value servergroupprotection_groups first!");
                        } else if (retValue == -3) {
                            this.queryLib.sendTextMessage(Integer.parseInt(eventInfo.get("invokerid")), 1, "Unable to add client to protected server group! Server group protection feature is disabled, enable it first!");
                        }
                    }
                }
                catch (NumberFormatException nfe) {
                    this.queryLib.sendTextMessage(Integer.parseInt(eventInfo.get("invokerid")), 1, "Error while reading given server group id!");
                }
            }
            catch (Exception e) {
                this.modClass.addLogEntry(this.configPrefix, e, false);
            }
        }
    }

    private void handleRemoveCommand(String msg, HashMap<String, String> eventInfo) {
        block22 : {
            try {
                if (msg.length() < 33) {
                    this.queryLib.sendTextMessage(Integer.parseInt(eventInfo.get("invokerid")), 1, "Wrong usage! Right: !" + this.configPrefix + " remove <server group id> <client unique id>");
                    break block22;
                }
                try {
                    StringTokenizer st = new StringTokenizer(msg.substring(7), " ", false);
                    int serverGroupID = -1;
                    String clientUniqueID = null;
                    String tmp = st.nextToken().trim();
                    try {
                        serverGroupID = Integer.parseInt(tmp);
                        tmp = st.nextToken().trim();
                        clientUniqueID = new String(tmp);
                    }
                    catch (Exception e) {
                        try {
                            clientUniqueID = new String(tmp);
                            tmp = st.nextToken().trim();
                            serverGroupID = Integer.parseInt(tmp);
                        }
                        catch (Exception var8_11) {
                            // empty catch block
                        }
                    }
                    if (clientUniqueID.length() < 20) {
                        this.queryLib.sendTextMessage(Integer.parseInt(eventInfo.get("invokerid")), 1, "Error while reading given client unique id!");
                    } else if (serverGroupID < 1) {
                        this.queryLib.sendTextMessage(Integer.parseInt(eventInfo.get("invokerid")), 1, "Error while reading given server group id!");
                    } else {
                        int retValue = this.removeServerGroupProtectionEntry(serverGroupID, clientUniqueID);
                        if (retValue == 1) {
                            this.queryLib.sendTextMessage(Integer.parseInt(eventInfo.get("invokerid")), 1, "Successfully removed client from protected server group!");
                        } else if (retValue == 0) {
                            this.queryLib.sendTextMessage(Integer.parseInt(eventInfo.get("invokerid")), 1, "Client is already not on that list for server group " + Integer.toString(serverGroupID) + "!");
                        } else if (retValue == -1) {
                            this.queryLib.sendTextMessage(Integer.parseInt(eventInfo.get("invokerid")), 1, "Unable to save server group protection configuration file! Check if the configuration file is write protected!");
                        } else if (retValue == -2) {
                            this.queryLib.sendTextMessage(Integer.parseInt(eventInfo.get("invokerid")), 1, "Unable to removed client from protected server group! Server group protection feature must be enabled and server group " + Integer.toString(serverGroupID) + " has to be on the watch list of the server group protection feature. Please make sure that this feature is enabled and add this server group to config value servergroupprotection_groups first!");
                        } else if (retValue == -3) {
                            this.queryLib.sendTextMessage(Integer.parseInt(eventInfo.get("invokerid")), 1, "Unable to removed client from protected server group! Server group protection feature is disabled, enable it first!");
                        }
                    }
                }
                catch (NumberFormatException nfe) {
                    this.queryLib.sendTextMessage(Integer.parseInt(eventInfo.get("invokerid")), 1, "Error while reading given server group id!");
                }
            }
            catch (Exception e) {
                this.modClass.addLogEntry(this.configPrefix, e, false);
            }
        }
    }

    @Override
    public void handleClientEvents(String eventType, HashMap<String, String> eventInfo) {
    }

    @Override
    public void handleClientCheck(Vector<HashMap<String, String>> clientList) {
        if (!this.pluginEnabled) {
            return;
        }
        for (HashMap<String, String> clientInfo : clientList) {
            HashMap<String, String> response;
            int i;
            if (!clientInfo.get("client_type").equals("0")) continue;
            int clientID = Integer.parseInt(clientInfo.get("clid"));
            StringTokenizer groupTokenizer = new StringTokenizer(clientInfo.get("client_servergroups"), ",", false);
            int groupID = -1;
            String sgpMessage = "";
            Vector<Integer> clientHasGroups = new Vector<Integer>();
            while (groupTokenizer.hasMoreTokens()) {
                groupID = Integer.parseInt(groupTokenizer.nextToken());
                clientHasGroups.addElement(groupID);
                i = 0;
                while (i < this.SERVERGROUPPROTECTION_GROUPS.size()) {
                    if (groupID == this.SERVERGROUPPROTECTION_GROUPS.elementAt(i) && this.SERVERGROUPPROTECTION_CLIENTS.elementAt(i).indexOf(clientInfo.get("client_unique_identifier")) == -1) {
                        block26 : {
                            response = this.queryLib.doCommand("servergroupdelclient sgid=" + Integer.toString(groupID) + " cldbid=" + clientInfo.get("client_database_id"));
                            if (response.get("id").equals("0")) {
                                this.modClass.addLogEntry(this.configPrefix, (byte)1, "Removed client \"" + clientInfo.get("client_nickname") + "\" (db id: " + clientInfo.get("client_database_id") + ") from server group " + Integer.toString(groupID) + "!", false);
                                this.fel.clearException(Integer.parseInt(clientInfo.get("client_database_id")));
                            } else {
                                try {
                                    throw new TS3ServerQueryException("ServerGroupProtection", response.get("id"), response.get("msg"), response.get("extra_msg"), response.get("failed_permid"));
                                }
                                catch (TS3ServerQueryException sqe) {
                                    if (this.fel.existsException(sqe, Integer.parseInt(clientInfo.get("client_database_id")))) break block26;
                                    this.fel.addException(sqe, Integer.parseInt(clientInfo.get("client_database_id")));
                                    this.modClass.addLogEntry(this.configPrefix, (byte)3, "Error while removing client \"" + clientInfo.get("client_nickname") + "\" (db id: " + clientInfo.get("client_database_id") + ") from server group " + Integer.toString(groupID) + "!", false);
                                    this.modClass.addLogEntry(this.configPrefix, sqe, false);
                                }
                            }
                        }
                        if (this.SERVERGROUPPROTECTION_COMPLAINADD) {
                            try {
                                this.queryLib.complainAdd(Integer.parseInt(clientInfo.get("client_database_id")), "Not allowed server group (id: " + Integer.toString(groupID) + "): " + clientInfo.get("client_nickname"));
                                this.modClass.addLogEntry(this.configPrefix, (byte)1, "Added complaint to client \"" + clientInfo.get("client_nickname") + "\" (db id: " + clientInfo.get("client_database_id") + "), not allowed to be in the server group " + Integer.toString(groupID) + "!", false);
                                this.fel.clearException(Integer.parseInt(clientInfo.get("client_database_id")));
                            }
                            catch (TS3ServerQueryException sqe) {
                                if (!this.fel.existsException(sqe, Integer.parseInt(clientInfo.get("client_database_id")))) {
                                    this.fel.addException(sqe, Integer.parseInt(clientInfo.get("client_database_id")));
                                    this.modClass.addLogEntry(this.configPrefix, (byte)3, "Error while adding complaint to client \"" + clientInfo.get("client_nickname") + "\" (db id: " + clientInfo.get("client_database_id") + "), not allowed to be in the server group " + Integer.toString(groupID) + "!", false);
                                    this.modClass.addLogEntry(this.configPrefix, sqe, false);
                                }
                            }
                            catch (Exception e) {
                                this.modClass.addLogEntry(this.configPrefix, (byte)3, "Error while adding complaint to client \"" + clientInfo.get("client_nickname") + "\" (db id: " + clientInfo.get("client_database_id") + "), not allowed to be in the server group " + Integer.toString(groupID) + "!", false);
                                this.modClass.addLogEntry(this.configPrefix, e, false);
                            }
                        }
                        String sgName = this.modClass.getServerGroupName(groupID);
                        sgpMessage = new String(this.SERVERGROUPPROTECTION_MESSAGE);
                        sgpMessage = sgpMessage.replace("%SERVER_GROUP_ID%", Integer.toString(groupID));
                        sgpMessage = sgpMessage.replace("%SERVER_GROUP_NAME%", sgName == null ? "Unknown" : sgName);
                        if (this.SERVERGROUPPROTECTION_KICK) {
                            try {
                                this.queryLib.kickClient(clientID, false, sgpMessage);
                                this.modClass.addLogEntry(this.configPrefix, (byte)1, "Client \"" + clientInfo.get("client_nickname") + "\" (db id: " + clientInfo.get("client_database_id") + ") was kicked for being member of the protected server group " + Integer.toString(groupID) + ", unique ID is not on list!", false);
                                this.fel.clearException(Integer.parseInt(clientInfo.get("client_database_id")));
                            }
                            catch (TS3ServerQueryException sqe) {
                                if (!this.fel.existsException(sqe, Integer.parseInt(clientInfo.get("client_database_id")))) {
                                    this.fel.addException(sqe, Integer.parseInt(clientInfo.get("client_database_id")));
                                    this.modClass.addLogEntry(this.configPrefix, (byte)3, "Error while kicking client \"" + clientInfo.get("client_nickname") + "\" (db id: " + clientInfo.get("client_database_id") + ") for being member of the protected server group " + Integer.toString(groupID) + "!", false);
                                    this.modClass.addLogEntry(this.configPrefix, sqe, false);
                                }
                            }
                            catch (Exception e) {
                                this.modClass.addLogEntry(this.configPrefix, (byte)3, "Error while kicking client \"" + clientInfo.get("client_nickname") + "\" (db id: " + clientInfo.get("client_database_id") + ") for being member of the protected server group " + Integer.toString(groupID) + "!", false);
                                this.modClass.addLogEntry(this.configPrefix, e, false);
                            }
                        } else {
                            this.modClass.sendMessageToClient(this.configPrefix, this.SERVERGROUPPROTECTION_MESSAGE_MODE, clientID, sgpMessage);
                        }
                    }
                    ++i;
                }
            }
            if (!this.SERVERGROUPPROTECTION_ADD_MISSING_GROUPS) continue;
            i = 0;
            while (i < this.SERVERGROUPPROTECTION_GROUPS.size()) {
                block28 : {
                    if (this.SERVERGROUPPROTECTION_ADDALLOWED.elementAt(i).booleanValue() && this.SERVERGROUPPROTECTION_CLIENTS.elementAt(i).indexOf(clientInfo.get("client_unique_identifier")) != -1 && clientHasGroups.indexOf(this.SERVERGROUPPROTECTION_GROUPS.elementAt(i)) == -1) {
                        response = this.queryLib.doCommand("servergroupaddclient sgid=" + Integer.toString(this.SERVERGROUPPROTECTION_GROUPS.elementAt(i)) + " cldbid=" + clientInfo.get("client_database_id"));
                        if (response.get("id").equals("0")) {
                            this.modClass.addLogEntry(this.configPrefix, (byte)1, "Added client \"" + clientInfo.get("client_nickname") + "\" (db id: " + clientInfo.get("client_database_id") + ") to server group " + Integer.toString(this.SERVERGROUPPROTECTION_GROUPS.elementAt(i)) + "!", false);
                            this.fel.clearException(Integer.parseInt(clientInfo.get("client_database_id")));
                        } else {
                            try {
                                throw new TS3ServerQueryException("ServerGroupProtection", response.get("id"), response.get("msg"), response.get("extra_msg"), response.get("failed_permid"));
                            }
                            catch (TS3ServerQueryException sqe) {
                                if (this.fel.existsException(sqe, Integer.parseInt(clientInfo.get("client_database_id")))) break block28;
                                this.fel.addException(sqe, Integer.parseInt(clientInfo.get("client_database_id")));
                                this.modClass.addLogEntry(this.configPrefix, (byte)3, "Error while adding client \"" + clientInfo.get("client_nickname") + "\" (db id: " + clientInfo.get("client_database_id") + ") to server group " + Integer.toString(this.SERVERGROUPPROTECTION_GROUPS.elementAt(i)) + "!", false);
                                this.modClass.addLogEntry(this.configPrefix, sqe, false);
                            }
                        }
                    }
                }
                ++i;
            }
        }
    }

    private void checkEmptyServerGroups() {
        boolean needToSave = false;
        int i = 0;
        while (i < this.SERVERGROUPPROTECTION_GROUPS.size()) {
            if (this.SERVERGROUPPROTECTION_CLIENTS.elementAt(i).size() == 0) {
                try {
                    Vector<HashMap<String, String>> sgClientList = this.queryLib.getList(9, "sgid=" + Integer.toString(this.SERVERGROUPPROTECTION_GROUPS.elementAt(i)) + ",-names");
                    int x = 0;
                    while (x < sgClientList.size()) {
                        this.addServerGroupProtectionEntry(this.SERVERGROUPPROTECTION_GROUPS.elementAt(i), sgClientList.elementAt(x).get("client_unique_identifier"), sgClientList.elementAt(x).get("client_nickname"), false);
                        needToSave = true;
                        ++x;
                    }
                }
                catch (Exception e) {
                    this.modClass.addLogEntry(this.configPrefix, e, false);
                }
            }
            ++i;
        }
        if (needToSave) {
            this.saveServerGroupProtectionFile();
        }
    }
}

