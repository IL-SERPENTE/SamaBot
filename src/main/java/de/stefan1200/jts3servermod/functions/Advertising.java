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
import java.util.TimerTask;
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
import de.stefan1200.util.MySQLConnect;

/*
 * This class specifies class file version 49.0 but uses Java 6 signatures.  Assumed Java 6.
 */
public class Advertising
implements HandleBotEvents,
LoadConfiguration,
HandleClientList {
    private String configPrefix = "";
    private JTS3ServerMod_Interface modClass = null;
    private JTS3ServerQuery queryLib = null;
    private boolean pluginEnabled = false;
    private boolean ADVERTISING_TARGET_CHANNEL = false;
    private int ADVERTISING_CHANNELID = -1;
    private int ADVERTISING_INTERVAL = -1;
    private String ADVERTISING_FILE = null;
    private Vector<String> ADVERTISING_MESSAGES = new Vector<String>();
    private String channelName = null;
    private TimerTask timerAdvertising;
    private boolean advertiseNow = false;
    private short currentAdvertiseMessage = 0;
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
        String msg = this.ADVERTISING_TARGET_CHANNEL ? "Advertising will be send to channel \"" + this.channelName + "\" (id: " + Integer.toString(this.ADVERTISING_CHANNELID) + ") every " + Integer.toString(this.ADVERTISING_INTERVAL) + " minutes (" + this.ADVERTISING_MESSAGES.size() + " messages found)" : "Advertising will be send to virtual server every " + Integer.toString(this.ADVERTISING_INTERVAL) + " minutes (" + this.ADVERTISING_MESSAGES.size() + " messages found)";
        this.modClass.addLogEntry(this.configPrefix, (byte)1, msg, true);
    }

    @Override
    public void handleAfterCacheUpdate() {
        if (!this.pluginEnabled) {
            return;
        }
        if (this.ADVERTISING_TARGET_CHANNEL) {
            this.channelName = this.modClass.getChannelName(this.ADVERTISING_CHANNELID);
            if (this.channelName == null) {
                this.modClass.addLogEntry(this.configPrefix, (byte)3, "Critical: Channel ID " + this.ADVERTISING_CHANNELID + " don't exists! Check value " + this.configPrefix + "_channel_id in your configuration!", true);
                this.pluginEnabled = false;
            }
        }
    }

    @Override
    public void activate() {
        if (!this.pluginEnabled) {
            return;
        }
        if (this.timerAdvertising != null) {
            this.timerAdvertising.cancel();
        }
        this.timerAdvertising = null;
        this.timerAdvertising = new TimerTask(){

            public void run() {
                Advertising.access$0(Advertising.this, true);
            }
        };
        this.modClass.addBotTimer(this.timerAdvertising, this.ADVERTISING_INTERVAL * 60 * 1000, this.ADVERTISING_INTERVAL * 60 * 1000);
    }

    @Override
    public void disable() {
        if (this.timerAdvertising != null) {
            this.timerAdvertising.cancel();
        }
        this.timerAdvertising = null;
        this.advertiseNow = false;
    }

    @Override
    public void unload() {
        this.ADVERTISING_MESSAGES = null;
    }

    @Override
    public boolean multipleInstances() {
        return true;
    }

    @Override
    public void initConfig(ArrangedPropertiesWriter config) {
        config.addKey(String.valueOf(this.configPrefix) + "_mode", "Is the target of the advertising messages a channel or a server? Possible values (just write one of the both words!): channel or server", "server");
        config.addKey(String.valueOf(this.configPrefix) + "_channel_id", "If a channel is the target, set channel id to write advertising message into it");
        config.addKey(String.valueOf(this.configPrefix) + "_repeat_time", "Advertise every X minutes", "30");
        if (this.modClass.getMySQLConnection() == null) {
            config.addKey(String.valueOf(this.configPrefix) + "_file", "Path to file which contains the advertising messages.", "config/server1/advertising.cfg");
        }
    }

    @Override
    public boolean loadConfig(ArrangedPropertiesWriter config, boolean slowMode) throws BotConfigurationException, NumberFormatException {
        String lastNumberValue = "";
        String temp = null;
        this.pluginEnabled = false;
        try {
            this.ADVERTISING_TARGET_CHANNEL = config.getValue(String.valueOf(this.configPrefix) + "_mode", "server").trim().equalsIgnoreCase("channel");
            this.ADVERTISING_FILE = config.getValue(String.valueOf(this.configPrefix) + "_file");
            if (this.ADVERTISING_TARGET_CHANNEL) {
                lastNumberValue = String.valueOf(this.configPrefix) + "_channel_id";
                temp = config.getValue(String.valueOf(this.configPrefix) + "_channel_id");
                if (temp == null) {
                    throw new NumberFormatException();
                }
                this.ADVERTISING_CHANNELID = Integer.parseInt(temp.trim());
            }
            if (!this.loadAdvertisingMessages()) {
                throw new BotConfigurationException("Advertising messages does not exists or error while loading!");
            }
            if (this.ADVERTISING_MESSAGES.size() < 1) {
                throw new BotConfigurationException("No Advertising messages found!");
            }
            lastNumberValue = String.valueOf(this.configPrefix) + "_repeat_time";
            temp = config.getValue(String.valueOf(this.configPrefix) + "_repeat_time");
            if (temp == null) {
                throw new NumberFormatException();
            }
            this.ADVERTISING_INTERVAL = Integer.parseInt(temp.trim());
            if (this.ADVERTISING_INTERVAL < 1) {
                this.ADVERTISING_INTERVAL = 1;
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

    boolean loadAdvertisingMessages() {
        BufferedReader br;
        String line;
        block36 : {
            if (this.modClass.getMySQLConnection() != null) {
                boolean retValue;
                MySQLConnect mysqlConnect = this.modClass.getMySQLConnection();
                retValue = false;
                PreparedStatement pst = null;
                ResultSet rs = null;
                try {
                    try {
                        mysqlConnect.connect();
                        pst = mysqlConnect.getPreparedStatement("SELECT textentry FROM jts3servermod_advertising WHERE instance_id = ? AND prefix = ?");
                        pst.setInt(1, this.modClass.getInstanceID());
                        pst.setString(2, this.configPrefix);
                        rs = pst.executeQuery();
                        int warningCount = 0;
                        this.ADVERTISING_MESSAGES.clear();
                        while (rs.next()) {
                            this.ADVERTISING_MESSAGES.addElement(rs.getString(1));
                            if (rs.getString(1).length() <= this.modClass.getMaxMessageLength("chat")) continue;
                            ++warningCount;
                        }
                        retValue = true;
                        this.currentAdvertiseMessage = 0;
                        if (warningCount > 0) {
                            this.modClass.addLogEntry(this.configPrefix, (byte)2, String.valueOf(Integer.toString(warningCount)) + " advertising messages are to long! Make sure that chat messages are not longer than " + Short.toString(this.modClass.getMaxMessageLength("chat")) + " characters (including spaces and BBCode).", true);
                        }
                    }
                    catch (Exception e) {
                        retValue = false;
                        try {
                            if (rs != null) {
                                rs.close();
                            }
                        }
                        catch (Exception var7_12) {
                            // empty catch block
                        }
                        try {
                            if (pst != null) {
                                pst.close();
                            }
                        }
                        catch (Exception var7_13) {
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
                    catch (Exception var7_16) {}
                    try {
                        if (pst != null) {
                            pst.close();
                        }
                    }
                    catch (Exception var7_17) {}
                    mysqlConnect.close();
                }
                return retValue;
            }
            if (this.ADVERTISING_FILE == null) {
                this.modClass.addLogEntry(this.configPrefix, (byte)3, "Path to advertising config file was not set in bot config!", true);
                return false;
            }
            try {
	            this.ADVERTISING_FILE = this.ADVERTISING_FILE.trim();
	            br = new BufferedReader(new InputStreamReader((InputStream)new FileInputStream(this.ADVERTISING_FILE), this.modClass.getMessageEncoding()));
	            this.ADVERTISING_MESSAGES.clear();
	            line = br.readLine();
	            if (this.modClass.getMessageEncoding().equalsIgnoreCase("UTF-8") && line != null && line.charAt(0) == '\ufeff') {
	                line = line.substring(1);
	            }
	            if (line != null && line.equals("# JTS3ServerMod Config File")) break block36;
	            this.modClass.addLogEntry(this.configPrefix, (byte)3, "Special config file header is missing at advertising config file!", true);
	            this.modClass.addLogEntry(this.configPrefix, (byte)3, "Check if you set the right file: " + this.ADVERTISING_FILE, true);
	            br.close();
            } catch (Exception e)
            {
            	//TG SONARQUBE
            }
            return false;
        }
        try {
            int warningCount = 0;
            while ((line = br.readLine()) != null) {
                if (line.startsWith("#") || line.length() <= 3) continue;
                if ((line = line.replace("\\n", "\n")).length() > this.modClass.getMaxMessageLength("chat")) {
                    ++warningCount;
                }
                this.ADVERTISING_MESSAGES.addElement(line);
            }
            br.close();
            if (warningCount > 0) {
                this.modClass.addLogEntry(this.configPrefix, (byte)2, String.valueOf(Integer.toString(warningCount)) + " advertising messages are to long! Make sure that chat messages are not longer than " + Short.toString(this.modClass.getMaxMessageLength("chat")) + " characters (including spaces and BBCode), check file: " + this.ADVERTISING_FILE, true);
            }
            this.currentAdvertiseMessage = 0;
        }
        catch (FileNotFoundException fnfe) {
            this.modClass.addLogEntry(this.configPrefix, (byte)3, "Advertising config file does not exist or missing permission for reading, check file: " + this.ADVERTISING_FILE, true);
            return false;
        }
        catch (Exception e) {
            this.modClass.addLogEntry(this.configPrefix, (byte)3, "Unknown error while loading advertising config file, check file: " + this.ADVERTISING_FILE, true);
            this.modClass.addLogEntry(this.configPrefix, e, true);
            return false;
        }
        return true;
    }

    @Override
    public void setListModes(BitSet listOptions) {
    }

    @Override
    public void handleClientCheck(Vector<HashMap<String, String>> clientList) {
        if (!this.pluginEnabled) {
            return;
        }
        if (this.advertiseNow && this.ADVERTISING_MESSAGES.size() > 0) {
            this.advertiseNow = false;
            if (this.currentAdvertiseMessage == this.ADVERTISING_MESSAGES.size()) {
                this.currentAdvertiseMessage = 0;
            }
            String message = this.ADVERTISING_MESSAGES.elementAt(this.currentAdvertiseMessage);
            if (this.modClass.isGlobalMessageVarsEnabled()) {
                message = this.modClass.replaceGlobalMessageVars(message);
            }
            if (this.ADVERTISING_TARGET_CHANNEL) {
                try {
                    this.queryLib.sendTextMessage(this.ADVERTISING_CHANNELID, 2, message);
                    this.currentAdvertiseMessage = (short)(this.currentAdvertiseMessage + 1);
                    this.fel.clearAllExceptions();
                }
                catch (TS3ServerQueryException sqe) {
                    if (!this.fel.existsException(sqe)) {
                        this.fel.addException(sqe);
                        this.modClass.addLogEntry(this.configPrefix, sqe, false);
                    }
                }
                catch (Exception e) {
                    this.modClass.addLogEntry(this.configPrefix, e, false);
                }
            } else {
                try {
                    this.queryLib.sendTextMessage(this.queryLib.getCurrentQueryClientServerID(), 3, message);
                    this.currentAdvertiseMessage = (short)(this.currentAdvertiseMessage + 1);
                    this.fel.clearAllExceptions();
                }
                catch (TS3ServerQueryException sqe) {
                    if (!this.fel.existsException(sqe)) {
                        this.fel.addException(sqe);
                        this.modClass.addLogEntry(this.configPrefix, sqe, false);
                    }
                }
                catch (Exception e) {
                    this.modClass.addLogEntry(this.configPrefix, e, false);
                }
            }
        }
    }

    static /* synthetic */ void access$0(Advertising advertising, boolean bl) {
        advertising.advertiseNow = bl;
    }

}

