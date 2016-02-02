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
import de.stefan1200.jts3servermod.FunctionExceptionLog;
import de.stefan1200.jts3servermod.interfaces.HandleBotEvents;
import de.stefan1200.jts3servermod.interfaces.HandleClientList;
import de.stefan1200.jts3servermod.interfaces.JTS3ServerMod_Interface;
import de.stefan1200.jts3servermod.interfaces.LoadConfiguration;
import de.stefan1200.jts3serverquery.JTS3ServerQuery;
import de.stefan1200.jts3serverquery.TS3ServerQueryException;
import de.stefan1200.util.ArrangedPropertiesWriter;
import de.stefan1200.util.MySQLConnect;

/*
 * This class specifies class file version 49.0 but uses Java 6 signatures.  Assumed Java 6.
 */
public class BadNicknameCheck
implements HandleBotEvents,
HandleClientList,
LoadConfiguration {
    private String configPrefix = "";
    private JTS3ServerMod_Interface modClass = null;
    private JTS3ServerQuery queryLib = null;
    private boolean pluginEnabled = false;
    private boolean BADNICKNAME_KICK = true;
    private String BADNICKNAME_FILE = null;
    private boolean BADNICKNAME_COMPLAINADD = false;
    private Vector<Integer> BADNICKNAME_GROUP_LIST = new Vector<Integer>();
    private boolean BADNICKNAME_GROUP_LIST_IGNORE = true;
    private String BADNICKNAME_MESSAGE = null;
    private String BADNICKNAME_MESSAGE_MODE = null;
    private Vector<Pattern> BADNICKNAME_RULES = new Vector<Pattern>();
    private Vector<String> BADNICKNAME_CACHE = new Vector<String>();
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
        String msg = "Bad Nickname Check is enabled, " + Integer.toString(this.BADNICKNAME_RULES.size()) + " rules loaded";
        if (this.BADNICKNAME_COMPLAINADD && this.BADNICKNAME_KICK) {
            msg = String.valueOf(msg) + " (client will be kicked and a complaint will be added)";
        } else if (this.BADNICKNAME_KICK) {
            msg = String.valueOf(msg) + " (client will be kicked)";
        } else if (this.BADNICKNAME_COMPLAINADD) {
            msg = String.valueOf(msg) + " (complaint will be added)";
        }
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
        this.BADNICKNAME_GROUP_LIST = null;
        this.BADNICKNAME_RULES = null;
    }

    @Override
    public boolean multipleInstances() {
        return true;
    }

    @Override
    public void initConfig(ArrangedPropertiesWriter config) {
        config.addKey(String.valueOf(this.configPrefix) + "_kick", "Kick client with a bad nickname? Set yes or no here!", "yes");
        config.addKey(String.valueOf(this.configPrefix) + "_add_complain", "Add complain entry to the user? Set yes or no here!", "no");
        config.addKey(String.valueOf(this.configPrefix) + "_group_list", "A comma separated list (without spaces) of server group ids.\nDepends on the given mode, this server groups can be ignored or only this server groups will be checked!\nIf no server groups should be ignored, set no server groups here and select the group list mode ignore!");
        config.addKey(String.valueOf(this.configPrefix) + "_group_list_mode", "Select one of the two modes for the server group list.\nignore = The selected server groups will be ignored and can have bad nicknames.\nonly = Only the selected server groups will be checked.", "ignore");
        config.addKey(String.valueOf(this.configPrefix) + "_message_mode", "Select the message mode, how the client should get the message.\npoke or chat are valid values!\nIf client kick is activated, the message will be always used as kick message!", "poke");
        if (this.modClass.getMySQLConnection() == null) {
            config.addKey(String.valueOf(this.configPrefix) + "_file", "Path to file which contains the bad nickname message and check rules.", "config/server1/badnickname.cfg");
        }
        if (this.modClass.getMySQLConnection() != null) {
            config.addKey(String.valueOf(this.configPrefix) + "_message", "Set kick message for using a bad nickname.\nYou can use the following keywords, which will be replaced:\n%CLIENT_NAME% - Client Name\nYou can use \\n for a new line and typical BBCode like in Teamspeak 3 Client.");
        }
    }

    @Override
    public boolean loadConfig(ArrangedPropertiesWriter config, boolean slowMode) throws BotConfigurationException, NumberFormatException {
        String lastNumberValue = "";
        String temp = null;
        this.pluginEnabled = false;
        try {
            this.BADNICKNAME_KICK = config.getValue(String.valueOf(this.configPrefix) + "_kick", "yes").trim().equalsIgnoreCase("yes");
            this.BADNICKNAME_MESSAGE_MODE = config.getValue(String.valueOf(this.configPrefix) + "_message_mode", "chat").trim();
            this.BADNICKNAME_FILE = config.getValue(String.valueOf(this.configPrefix) + "_file");
            if (!this.loadBadNicknameFile(config)) {
                throw new BotConfigurationException("Bad Nickname Check message and configuration could not be loaded!");
            }
            if (this.BADNICKNAME_MESSAGE == null || this.BADNICKNAME_MESSAGE.length() == 0) {
                throw new BotConfigurationException("Bad Nickname Check message could not be loaded!");
            }
            if (!this.modClass.isMessageLengthValid(this.BADNICKNAME_KICK ? "kick" : this.BADNICKNAME_MESSAGE_MODE, this.BADNICKNAME_MESSAGE)) {
                this.modClass.addLogEntry(this.configPrefix, (byte)2, "Bad Nickname Check message is to long! Make sure that " + (this.BADNICKNAME_KICK ? "kick" : this.BADNICKNAME_MESSAGE_MODE) + " messages are not longer than " + Short.toString(this.modClass.getMaxMessageLength(this.BADNICKNAME_KICK ? "kick" : this.BADNICKNAME_MESSAGE_MODE)) + " characters (including spaces and BBCode)" + (this.modClass.getMySQLConnection() == null ? new StringBuilder(", check file: ").append(this.BADNICKNAME_FILE).toString() : ""), true);
            }
            temp = null;
            this.BADNICKNAME_GROUP_LIST.clear();
            temp = config.getValue(String.valueOf(this.configPrefix) + "_group_list");
            lastNumberValue = String.valueOf(this.configPrefix) + "_group_list";
            if (temp != null && temp.length() > 0) {
                StringTokenizer st = new StringTokenizer(temp, ",", false);
                while (st.hasMoreTokens()) {
                    this.BADNICKNAME_GROUP_LIST.addElement(Integer.parseInt(st.nextToken().trim()));
                }
            }
            this.BADNICKNAME_GROUP_LIST_IGNORE = !config.getValue(String.valueOf(this.configPrefix) + "_group_list_mode", "ignore").trim().equalsIgnoreCase("only");
            this.BADNICKNAME_COMPLAINADD = config.getValue(String.valueOf(this.configPrefix) + "_add_complain", "no").trim().equalsIgnoreCase("yes");
            if (this.BADNICKNAME_RULES.size() == 0) {
                throw new BotConfigurationException("No bad nickname check rules was found! Config file: " + this.BADNICKNAME_FILE);
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

    boolean loadBadNicknameFile(ArrangedPropertiesWriter config) {
        BufferedReader br;
        String line;
        block35 : {
            if (this.modClass.getMySQLConnection() != null) {
                boolean retValue;
                MySQLConnect mysqlConnect = this.modClass.getMySQLConnection();
                this.BADNICKNAME_MESSAGE = config.getValue(String.valueOf(this.configPrefix) + "_message");
                retValue = false;
                PreparedStatement pst = null;
                ResultSet rs = null;
                try {
                    try {
                        mysqlConnect.connect();
                        pst = mysqlConnect.getPreparedStatement("SELECT textentry FROM jts3servermod_badnickname WHERE instance_id = ? AND prefix = ?");
                        pst.setInt(1, this.modClass.getInstanceID());
                        pst.setString(2, this.configPrefix);
                        rs = pst.executeQuery();
                        this.BADNICKNAME_RULES.clear();
                        while (rs.next()) {
                            this.BADNICKNAME_RULES.addElement(Pattern.compile(rs.getString(1), 66));
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
            if (this.BADNICKNAME_FILE == null) {
                this.modClass.addLogEntry(this.configPrefix, (byte)3, "Path to Bad Nickname Check config file was not set in bot config! Check config key: " + this.configPrefix + "_file", true);
                return false;
            }
            try {
	            this.BADNICKNAME_FILE = this.BADNICKNAME_FILE.trim();
	            br = new BufferedReader(new InputStreamReader((InputStream)new FileInputStream(this.BADNICKNAME_FILE), this.modClass.getMessageEncoding()));
	            this.BADNICKNAME_RULES.clear();
	            line = br.readLine();
	            if (this.modClass.getMessageEncoding().equalsIgnoreCase("UTF-8") && line != null && line.charAt(0) == '\ufeff') {
	                line = line.substring(1);
	            }
	            if (line != null && line.equals("# JTS3ServerMod Config File")) break block35;
	            this.modClass.addLogEntry(this.configPrefix, (byte)3, "Special config file header is missing at Bad Nickname Check config file! File path: " + this.BADNICKNAME_FILE, true);
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
                    this.BADNICKNAME_MESSAGE = line = line.replace("\\n", "\n");
                }
                if (count >= 1) {
                    this.BADNICKNAME_RULES.addElement(Pattern.compile(line, 66));
                }
                ++count;
            }
            br.close();
        }
        catch (FileNotFoundException fnfe) {
            this.modClass.addLogEntry(this.configPrefix, (byte)3, "Bad Nickname Check config file you set at config key \"" + this.configPrefix + "_file\" does not exist or missing permission for reading, check file path: " + this.BADNICKNAME_FILE, true);
            return false;
        }
        catch (Exception e) {
            this.modClass.addLogEntry(this.configPrefix, (byte)3, "Unknown error while loading Bad Nickname Check config file! Check file you set at config key \"" + this.configPrefix + "_file\", the file path: " + this.BADNICKNAME_FILE, true);
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
    public void handleClientCheck(Vector<HashMap<String, String>> clientList) {
        if (!this.pluginEnabled) {
            return;
        }
        for (HashMap<String, String> clientInfo : clientList) {
            if (!clientInfo.get("client_type").equals("0") || !this.BADNICKNAME_KICK && this.BADNICKNAME_CACHE.indexOf(clientInfo.get("client_nickname")) >= 0) continue;
            int clientID = Integer.parseInt(clientInfo.get("clid"));
            boolean result = this.modClass.isGroupListed(clientInfo.get("client_servergroups"), this.BADNICKNAME_GROUP_LIST);
            if (!(this.BADNICKNAME_GROUP_LIST_IGNORE ? !result : result)) continue;
            for (Pattern rule : this.BADNICKNAME_RULES) {
                Matcher ruleCheck = rule.matcher(clientInfo.get("client_nickname"));
                if (!ruleCheck.matches()) continue;
                if (this.BADNICKNAME_COMPLAINADD) {
                    try {
                        this.queryLib.complainAdd(Integer.parseInt(clientInfo.get("client_database_id")), "Bad Nickname: " + clientInfo.get("client_nickname"));
                        this.modClass.addLogEntry(this.configPrefix, (byte)1, "Added complaint to client with the bad nickname \"" + clientInfo.get("client_nickname") + "\" (db id: " + clientInfo.get("client_database_id") + ")!", false);
                        this.fel.clearException(Integer.parseInt(clientInfo.get("client_database_id")));
                    }
                    catch (TS3ServerQueryException sqe) {
                        if (!this.fel.existsException(sqe, Integer.parseInt(clientInfo.get("client_database_id")))) {
                            this.fel.addException(sqe, Integer.parseInt(clientInfo.get("client_database_id")));
                            this.modClass.addLogEntry(this.configPrefix, (byte)3, "Error while adding complaint to client with the bad nickname \"" + clientInfo.get("client_nickname") + "\" (db id: " + clientInfo.get("client_database_id") + ")!", false);
                            this.modClass.addLogEntry(this.configPrefix, sqe, false);
                        }
                    }
                    catch (Exception e) {
                        this.modClass.addLogEntry(this.configPrefix, (byte)3, "Error while adding complaint to client with the bad nickname \"" + clientInfo.get("client_nickname") + "\" (db id: " + clientInfo.get("client_database_id") + ")!", false);
                        this.modClass.addLogEntry(this.configPrefix, e, false);
                    }
                }
                if (this.BADNICKNAME_KICK) {
                    try {
                        this.queryLib.kickClient(clientID, false, this.createMessage(clientInfo.get("client_nickname")));
                        this.modClass.addLogEntry(this.configPrefix, (byte)1, "Client \"" + clientInfo.get("client_nickname") + "\" (db id: " + clientInfo.get("client_database_id") + ") was kicked, nickname matched bad nickname rules!", false);
                        this.fel.clearException(Integer.parseInt(clientInfo.get("client_database_id")));
                    }
                    catch (TS3ServerQueryException sqe) {
                        if (this.fel.existsException(sqe, Integer.parseInt(clientInfo.get("client_database_id")))) continue;
                        this.fel.addException(sqe, Integer.parseInt(clientInfo.get("client_database_id")));
                        this.modClass.addLogEntry(this.configPrefix, (byte)3, "Nickname of the client \"" + clientInfo.get("client_nickname") + "\" (db id: " + clientInfo.get("client_database_id") + ") match a bad nickname rule, but kick is not possible because of an error!", false);
                        this.modClass.addLogEntry(this.configPrefix, sqe, false);
                    }
                    catch (Exception e) {
                        this.modClass.addLogEntry(this.configPrefix, (byte)3, "Nickname of the client \"" + clientInfo.get("client_nickname") + "\" (db id: " + clientInfo.get("client_database_id") + ") match a bad nickname rule, but kick is not possible because of an error!", false);
                        this.modClass.addLogEntry(this.configPrefix, e, false);
                    }
                    continue;
                }
                this.BADNICKNAME_CACHE.addElement(clientInfo.get("client_nickname"));
                this.modClass.sendMessageToClient(this.configPrefix, this.BADNICKNAME_MESSAGE_MODE, clientID, this.createMessage(clientInfo.get("client_nickname")));
            }
        }
        if (!this.BADNICKNAME_KICK) {
            int i = 0;
            while (i < this.BADNICKNAME_CACHE.size()) {
                boolean found = false;
                for (HashMap<String, String> client : clientList) {
                    if (!client.get("client_nickname").equals(this.BADNICKNAME_CACHE.elementAt(i))) continue;
                    found = true;
                    break;
                }
                if (!found) {
                    this.BADNICKNAME_CACHE.removeElementAt(i);
                }
                ++i;
            }
        }
    }

    private String createMessage(String clientName) {
        return this.BADNICKNAME_MESSAGE.replace("%CLIENT_NAME%", clientName);
    }
}

