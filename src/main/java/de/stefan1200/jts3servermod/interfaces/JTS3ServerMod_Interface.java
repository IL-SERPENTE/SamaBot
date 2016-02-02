/*
 * Decompiled with CFR 0_110.
 */
package de.stefan1200.jts3servermod.interfaces;

import de.stefan1200.jts3servermod.interfaces.ClientDatabaseCache_Interface;
import de.stefan1200.jts3servermod.interfaces.ServerInfoCache_Interface;
import de.stefan1200.util.MySQLConnect;
import java.util.HashMap;
import java.util.TimerTask;
import java.util.Vector;

public interface JTS3ServerMod_Interface {
    public static final String VERSION = "5.5.4 (11.07.2015)";
    public static final long VERSION_BUILD = 5504;
    public static final byte ERROR_LEVEL_DEBUG = 0;
    public static final byte ERROR_LEVEL_INFO = 1;
    public static final byte ERROR_LEVEL_WARNING = 2;
    public static final byte ERROR_LEVEL_ERROR = 3;
    public static final byte ERROR_LEVEL_CRITICAL = 4;
    public static final int LIST_AWAY = 0;
    public static final int LIST_GROUPS = 1;
    public static final int LIST_INFO = 2;
    public static final int LIST_TIMES = 3;
    public static final int LIST_UID = 4;
    public static final int LIST_VOICE = 5;
    public static final int LIST_COUNTRY = 6;
    public static final int LIST_IP = 7;

    public int getUTF8Length(CharSequence var1);

    public String getMessageEncoding();

    public String getChannelName(int var1);

    public void unloadFunction(Object var1);

    public boolean loadMessages(String var1, String var2, String[] var3);

    public ServerInfoCache_Interface getServerInfoCache();

    public Vector<HashMap<String, String>> getClientList();

    public Vector<HashMap<String, String>> getChannelList();

    public MySQLConnect getMySQLConnection();

    public int getInstanceID();

    public int getDefaultChannelID();

    public ClientDatabaseCache_Interface getClientCache();

    public int getCheckInterval();

    public String getStringFromTimestamp(long var1);

    public void addTS3ChannelEvent(Object var1);

    public void addTS3ServerEvent(Object var1);

    public void addBotTimer(TimerTask var1, long var2, long var4);

    public boolean isGlobalMessageVarsEnabled();

    public String replaceGlobalMessageVars(String var1);

    public boolean sendMessageToClient(String var1, String var2, int var3, String var4);

    public short getMaxMessageLength(String var1);

    public boolean isMessageLengthValid(String var1, String var2);

    public long getIdleTime(HashMap<String, String> var1, int var2);

    public boolean isIDListed(int var1, Vector<Integer> var2);

    public String getServerGroupName(int var1);

    public int getServerGroupType(int var1);

    public int getListedGroup(String var1, Vector<Integer> var2);

    public boolean isGroupListed(String var1, Vector<Integer> var2);

    public String getVersionString(String var1);

    public String getFileSizeString(long var1, boolean var3);

    public String getDifferenceTime(long var1, long var3);

    public void addLogEntry(String var1, byte var2, String var3, boolean var4);

    public void addLogEntry(String var1, Throwable var2, boolean var3);

    public int getClientDBID(String var1);
}

