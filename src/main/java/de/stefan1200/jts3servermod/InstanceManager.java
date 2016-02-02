/*
 * Decompiled with CFR 0_110.
 */
package de.stefan1200.jts3servermod;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Properties;
import java.util.StringTokenizer;
import java.util.Vector;
import java.util.regex.Pattern;

import de.stefan1200.jts3servermod.interfaces.JTS3ServerMod_Interface;

/*
 * This class specifies class file version 49.0 but uses Java 6 signatures.  Assumed Java 6.
 */
public class InstanceManager {
    public static final String DEFAULT_LOG_FILE_PATH = "JTS3ServerMod_InstanceManager.log";
    public static final String DEFAULT_CONFIG_FILE_PATH = "config/JTS3ServerMod_InstanceManager.cfg";
    private String CONFIG_FILE_NAME = "config/JTS3ServerMod_InstanceManager.cfg";
    private String LOG_FILE_NAME = "JTS3ServerMod_InstanceManager.log";
    private Vector<String> FULL_ADMIN_UID_LIST = new Vector<String>();
    private Vector<JTS3ServerMod> instanceClass = new Vector<JTS3ServerMod>();
    private Vector<String> instanceConfigFilePath = new Vector<String>();
    private Vector<String> instanceLogFilePath = new Vector<String>();
    private Vector<String> instanceCSVLogFilePath = new Vector<String>();
    private Vector<String> instanceName = new Vector<String>();
    private Vector<Boolean> instanceDebug = new Vector<Boolean>();
    private Vector<Boolean> instanceEnabled = new Vector<Boolean>();
    private Vector<JTS3ServerMod> instanceClassReloadTemp = null;
    private Vector<String> instanceConfigFilePathReloadTemp = null;
    private Vector<String> instanceLogFilePathReloadTemp = null;
    private Vector<String> instanceCSVLogFilePathReloadTemp = null;
    private Vector<String> instanceNameReloadTemp = null;
    private Vector<Boolean> instanceDebugReloadTemp = null;
    private Vector<Boolean> instanceEnabledReloadTemp = null;
    private SimpleDateFormat sdfDebug = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    private PrintStream logFile;
    private Pattern patternBotName = Pattern.compile("[a-z0-9\\_\\-]+", 66);
    private boolean alreadyStopped = false;
    private boolean botCommandExec = false;
    private byte botUpdateCheck = 0;
    private boolean firstInit = true;

    public InstanceManager(String instanceConfig, String instanceLog) {
        String msg;
        if (instanceConfig != null) {
            this.CONFIG_FILE_NAME = instanceConfig;
        }
        if (instanceLog != null) {
            this.LOG_FILE_NAME = instanceLog;
        }
        String errorMessage = null;
        try {
            File logFileCheck = new File(this.LOG_FILE_NAME);
            if (logFileCheck.exists()) {
                File oldLogFileCheck = new File(String.valueOf(this.LOG_FILE_NAME) + ".old");
                if (oldLogFileCheck.exists()) {
                    if (oldLogFileCheck.delete()) {
                        if (!logFileCheck.renameTo(oldLogFileCheck)) {
                            errorMessage = "Unable to rename file " + this.LOG_FILE_NAME + " to " + this.LOG_FILE_NAME + ".old";
                        }
                    } else {
                        errorMessage = "Unable to delete file " + this.LOG_FILE_NAME + ".old";
                    }
                } else if (!logFileCheck.renameTo(oldLogFileCheck)) {
                    errorMessage = "Unable to rename file " + this.LOG_FILE_NAME + " to " + this.LOG_FILE_NAME + ".old";
                }
            }
        }
        catch (Exception e) {
            errorMessage = e.toString();
        }
        try {
            if (this.LOG_FILE_NAME.length() > 4) {
                this.logFile = new PrintStream(new FileOutputStream(this.LOG_FILE_NAME, true), true, "UTF-8");
                if (errorMessage != null) {
                    this.addLogEntry("LOGFILE", "Error while checking old logfile: " + errorMessage, false);
                }
            } else {
                this.logFile = null;
            }
        }
        catch (Exception e) {
            this.logFile = null;
            System.out.println("Error while creating log file: " + this.LOG_FILE_NAME);
            e.printStackTrace();
        }
        this.addLogEntry("START_MANAGER", "JTS3ServerMod 5.5.4 (11.07.2015) Instance Manager started...", true);
        if (!this.loadConfig()) {
            msg = "InstanceManager config file does not exists or is not readable, quitting now...";
            this.addLogEntry("QUIT_MANAGER", msg, true);
        } else if (this.startAllInstances() == 0) {
            msg = "No instances enabled or needed entries missing in InstanceManager config file, quitting now...";
            this.addLogEntry("QUIT_MANAGER", msg, true);
        } else {
            this.firstInit = false;
            Runtime runtime = Runtime.getRuntime();
            runtime.addShutdownHook(new Thread(new Runnable(){

                public void run() {
                    if (!InstanceManager.this.alreadyStopped) {
                        InstanceManager.this.stopAllInstances("SHUTDOWN", "Got signal from operating system, quitting now...");
                    }
                }
            }));
        }
    }

    public InstanceManager(String instanceConfig) {
        if (instanceConfig != null) {
            this.CONFIG_FILE_NAME = instanceConfig;
        }
        System.out.println("JTS3ServerMod 5.5.4 (11.07.2015) checking and updating config files...");
        if (!this.loadConfig()) {
            System.out.println("InstanceManager config file does not exists or is not readable, quitting now...");
        } else if (this.updateConfigAllInstances() == 0) {
            System.out.println("No instances updated or needed entries missing in InstanceManager config file, quitting now...");
        }
    }

    boolean isDebugModeEnabled(JTS3ServerMod_Interface botClass) {
        try {
            return this.instanceDebug.elementAt(this.instanceClass.indexOf(botClass));
        }
        catch (Exception e) {
            return false;
        }
    }

    boolean isCommandExecAllowed() {
        return this.botCommandExec;
    }

    String getConnectionLogPath(JTS3ServerMod botClass) {
        try {
            return this.instanceCSVLogFilePath.elementAt(this.instanceClass.indexOf(botClass));
        }
        catch (Exception e) {
            return null;
        }
    }

    byte getBotUpdateCheckState() {
        return this.botUpdateCheck;
    }

    void removeInstance(JTS3ServerMod_Interface instance) {
        int i = 0;
        while (i < this.instanceClass.size()) {
            if (this.instanceClass.elementAt(i) == instance) {
                this.instanceClass.setElementAt(null, i);
                break;
            }
            ++i;
        }
    }

    int isInstanceRunning(String name) {
        if (name == null || name.length() < 1) {
            return -1;
        }
        int i = 0;
        while (i < this.instanceName.size()) {
            if (this.instanceName.elementAt(i).equalsIgnoreCase(name)) {
                if (this.instanceClass.elementAt(i) == null) {
                    return 0;
                }
                return 1;
            }
            ++i;
        }
        return -1;
    }

    Vector<String> getInstanceNames() {
        Vector<String> retList = new Vector<String>();
        retList.addAll(this.instanceName);
        return retList;
    }

    private boolean startInstance(int i) {
        if (this.instanceClass.elementAt(i) == null) {
            File instanceFile = new File(this.instanceConfigFilePath.elementAt(i));
            if (instanceFile.isFile()) {
                this.addLogEntry("START_INSTANCE", "Start virtual bot instance " + this.instanceName.elementAt(i) + "...", false);
                this.instanceClass.setElementAt(new JTS3ServerMod(this, this.instanceName.elementAt(i), this.instanceConfigFilePath.elementAt(i), this.instanceLogFilePath.elementAt(i), this.FULL_ADMIN_UID_LIST), i);
                this.instanceClass.elementAt(i).runThread();
                return true;
            }
            this.addLogEntry("CHECK_INSTANCE", "Config file of virtual bot instance " + this.instanceName.elementAt(i) + " is missing! Start of this virtual bot instance skipped...", true);
        }
        return false;
    }

    boolean startInstance(String name) {
        if (name == null || name.length() < 1) {
            return false;
        }
        int i = 0;
        while (i < this.instanceName.size()) {
            if (this.instanceName.elementAt(i).equalsIgnoreCase(name)) {
                return this.startInstance(i);
            }
            ++i;
        }
        return false;
    }

    boolean stopInstance(String name) {
        if (name == null || name.length() < 1) {
            return false;
        }
        int i = 0;
        while (i < this.instanceName.size()) {
            if (this.instanceName.elementAt(i).equalsIgnoreCase(name) && this.instanceClass.elementAt(i) != null) {
                this.addLogEntry("STOP_INSTANCE", "Stop bot virtual bot instance " + this.instanceName.elementAt(i) + "...", false);
                this.instanceClass.elementAt(i).stopBotInstance(0);
                return true;
            }
            ++i;
        }
        return false;
    }

    void stopAllInstances() {
        this.stopAllInstances(null, null);
    }

    void stopAllInstances(String messageType, String message) {
        if (messageType != null && message != null) {
            this.addLogEntry(messageType, message, false);
        }
        this.addLogEntry("STOP_ALL", "Stopping all virtual bot instances and quit manager...", false);
        int i = 0;
        while (i < this.instanceClass.size()) {
            if (this.instanceClass.elementAt(i) != null) {
                this.instanceClass.elementAt(i).stopBotInstance(0);
            }
            ++i;
        }
        this.alreadyStopped = true;
        int countTimer = 0;
        while (countTimer < 10) {
            ++countTimer;
            try {
                Thread.sleep(100);
                continue;
            }
            catch (Exception var4_4) {
                // empty catch block
            }
        }
    }

    void reloadAllInstances() {
        this.addLogEntry("RELOAD_ALL", "Reload all virtual bot instances...", false);
        int i = 0;
        while (i < this.instanceClass.size()) {
            if (this.instanceClass.elementAt(i) != null) {
                this.instanceClass.elementAt(i).stopBotInstance(2);
            }
            ++i;
        }
    }

    private int startAllInstances() {
        int count = 0;
        int i = 0;
        while (i < this.instanceEnabled.size()) {
            if (this.instanceEnabled.elementAt(i).booleanValue() && this.startInstance(i)) {
                ++count;
            }
            ++i;
        }
        return count;
    }

    private int updateConfigAllInstances() {
        int count = 0;
        int i = 0;
        while (i < this.instanceEnabled.size()) {
            if (this.instanceEnabled.elementAt(i).booleanValue() && this.updateInstance(i)) {
                ++count;
            }
            ++i;
        }
        return count;
    }

    private boolean updateInstance(int i) {
        File instanceFile = new File(this.instanceConfigFilePath.elementAt(i));
        if (instanceFile.isFile()) {
            this.addLogEntry("UPDATE_INSTANCE", "Update config file of virtual bot instance " + this.instanceName.elementAt(i) + "...", false);
            new de.stefan1200.jts3servermod.JTS3ServerMod(this.instanceName.elementAt(i), this.instanceConfigFilePath.elementAt(i));
            return true;
        }
        this.addLogEntry("CHECK_INSTANCE", "Config file of virtual bot instance " + this.instanceName.elementAt(i) + " is missing! Update config file of this virtual bot instance skipped...", true);
        return false;
    }

    boolean loadConfig() {
        boolean retValue;
        File confFile;
        block16 : {
            retValue = false;
            confFile = new File(this.CONFIG_FILE_NAME);
            if (confFile.isFile()) break block16;
            return false;
        }
        try {
            Properties prop = new Properties();
            prop.load(new FileInputStream(confFile));
            this.FULL_ADMIN_UID_LIST.clear();
            String fulladminListTemp = prop.getProperty("bot_fulladmin_list");
            if (fulladminListTemp != null) {
                StringTokenizer fulladminListTokenizer = new StringTokenizer(fulladminListTemp, ",", false);
                while (fulladminListTokenizer.hasMoreTokens()) {
                    this.FULL_ADMIN_UID_LIST.addElement(fulladminListTokenizer.nextToken().trim());
                }
            }
            if (prop.getProperty("bot_command_exec", "0").equals("1")) {
                if (this.firstInit || !this.botCommandExec) {
                    this.addLogEntry("CONFIG", "Bot chat command !exec is enabled!", true);
                }
                this.botCommandExec = true;
            } else {
                if (this.firstInit || this.botCommandExec) {
                    this.addLogEntry("CONFIG", "Bot chat command !exec is disabled!", false);
                }
                this.botCommandExec = false;
            }
            try {
                this.botUpdateCheck = Byte.parseByte(prop.getProperty("bot_update_check", "0").trim());
            }
            catch (Exception e) {
                this.botUpdateCheck = 0;
            }
            this.instanceClassReloadTemp = new Vector<JTS3ServerMod>();
            this.instanceConfigFilePathReloadTemp = new Vector<String>();
            this.instanceLogFilePathReloadTemp = new Vector<String>();
            this.instanceCSVLogFilePathReloadTemp = new Vector<String>();
            this.instanceNameReloadTemp = new Vector<String>();
            this.instanceDebugReloadTemp = new Vector<Boolean>();
            this.instanceEnabledReloadTemp = new Vector<Boolean>();
            if (this.loadInstanceListFile(prop)) {
                int oldNamePos = -1;
                int i = 0;
                while (i < this.instanceNameReloadTemp.size()) {
                    oldNamePos = this.instanceName.indexOf(this.instanceNameReloadTemp.elementAt(i));
                    if (oldNamePos != -1) {
                        this.instanceClassReloadTemp.setElementAt(this.instanceClass.elementAt(oldNamePos), i);
                    }
                    ++i;
                }
                int newNamePos = -1;
                int i2 = 0;
                while (i2 < this.instanceName.size()) {
                    newNamePos = this.instanceNameReloadTemp.indexOf(this.instanceName.elementAt(i2));
                    if (newNamePos == -1) {
                        this.stopInstance(this.instanceName.elementAt(i2));
                    }
                    ++i2;
                }
                this.instanceName.clear();
                this.instanceLogFilePath.clear();
                this.instanceCSVLogFilePath.clear();
                this.instanceEnabled.clear();
                this.instanceDebug.clear();
                this.instanceConfigFilePath.clear();
                this.instanceClass.clear();
                this.instanceName.addAll(this.instanceNameReloadTemp);
                this.instanceLogFilePath.addAll(this.instanceLogFilePathReloadTemp);
                this.instanceCSVLogFilePath.addAll(this.instanceCSVLogFilePathReloadTemp);
                this.instanceEnabled.addAll(this.instanceEnabledReloadTemp);
                this.instanceDebug.addAll(this.instanceDebugReloadTemp);
                this.instanceConfigFilePath.addAll(this.instanceConfigFilePathReloadTemp);
                this.instanceClass.addAll(this.instanceClassReloadTemp);
                this.instanceNameReloadTemp = null;
                this.instanceLogFilePathReloadTemp = null;
                this.instanceCSVLogFilePathReloadTemp = null;
                this.instanceEnabledReloadTemp = null;
                this.instanceDebugReloadTemp = null;
                this.instanceConfigFilePathReloadTemp = null;
                this.instanceClassReloadTemp = null;
                retValue = true;
            }
        }
        catch (Throwable e) {
            retValue = false;
        }
        return retValue;
    }

    /*
     * Enabled force condition propagation
     * Lifted jumps to return sites
     */
    private boolean loadInstanceListFile(Properties prop) {
        try {
            int breakCount = 0;
            int i = 1;
            do {
                String enabled;
                if ((enabled = prop.getProperty(String.valueOf(Integer.toString(i)) + ".instance_enable")) != null) {
                    breakCount = 0;
                    String name = prop.getProperty(String.valueOf(Integer.toString(i)) + ".instance_name");
                    String configPath = prop.getProperty(String.valueOf(Integer.toString(i)) + ".instance_config_path");
                    String logPath = prop.getProperty(String.valueOf(Integer.toString(i)) + ".instance_logfile_path");
                    String csvLogPath = prop.getProperty(String.valueOf(Integer.toString(i)) + ".instance_csvloginlog_path");
                    String debug = prop.getProperty(String.valueOf(Integer.toString(i)) + ".instance_debug", "0");
                    if (name != null && name.length() > 0 && configPath != null && configPath.length() > 0) {
                        if (!this.patternBotName.matcher(name).matches()) {
                            this.addLogEntry("CHECK_INSTANCE", "Name of bot \"" + name + "\" is not allowed! Disable bot...", true);
                        } else {
                            this.instanceClassReloadTemp.addElement(null);
                            this.instanceConfigFilePathReloadTemp.addElement(configPath);
                            if (this.instanceNameReloadTemp.indexOf(name) == -1) {
                                this.instanceNameReloadTemp.addElement(name);
                            } else {
                                this.instanceNameReloadTemp.addElement(String.valueOf(name) + Integer.toString(i));
                            }
                            if (logPath == null || logPath.length() < 2) {
                                this.instanceLogFilePathReloadTemp.addElement(null);
                            } else {
                                this.instanceLogFilePathReloadTemp.addElement(logPath);
                            }
                            if (csvLogPath == null || csvLogPath.length() < 2) {
                                this.instanceCSVLogFilePathReloadTemp.addElement(null);
                            } else {
                                this.instanceCSVLogFilePathReloadTemp.addElement(csvLogPath);
                            }
                            if (enabled.equals("1")) {
                                File instanceFile = new File(configPath);
                                if (instanceFile.isFile()) {
                                    this.instanceEnabledReloadTemp.addElement(true);
                                } else {
                                    this.addLogEntry("CHECK_INSTANCE", "Config file of bot " + name + " is missing! Disable bot...", true);
                                    this.instanceEnabledReloadTemp.addElement(false);
                                }
                            } else {
                                this.instanceEnabledReloadTemp.addElement(false);
                            }
                            if (debug.equals("1")) {
                                this.instanceDebugReloadTemp.addElement(true);
                            } else {
                                this.instanceDebugReloadTemp.addElement(false);
                            }
                        }
                    }
                } else if (++breakCount > 10) return true;
                ++i;
            } while (true);
        }
        catch (Exception e) {
            return false;
        }
    }

    private void addLogEntry(String type, String msg, boolean outputToSystemOut) {
        try {
            if (this.logFile != null) {
                if (outputToSystemOut) {
                    System.out.println(msg);
                }
                this.logFile.println(String.valueOf(this.sdfDebug.format(new Date(System.currentTimeMillis()))) + "\t" + type.toUpperCase() + "\t" + msg);
            }
        }
        catch (Exception var4_4) {
            // empty catch block
        }
    }

}

