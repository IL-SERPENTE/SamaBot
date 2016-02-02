/*
 * Decompiled with CFR 0_110.
 */
package de.stefan1200.jts3servermod;

import de.stefan1200.jts3servermod.ClientLastOnlineComparator;
import de.stefan1200.jts3servermod.JTS3ServerMod;
import de.stefan1200.jts3servermod.interfaces.ClientDatabaseCache_Interface;
import de.stefan1200.jts3serverquery.JTS3ServerQuery;
import java.util.Collections;
import java.util.HashMap;
import java.util.StringTokenizer;
import java.util.Vector;

/*
 * This class specifies class file version 49.0 but uses Java 6 signatures.  Assumed Java 6.
 */
public class ClientDatabaseCache
implements ClientDatabaseCache_Interface {
    /*private final String REQUEST_COUNT = "50";
    private final short REQUEST_DELAY = 5000;*/
    private JTS3ServerMod modClass;
    private JTS3ServerQuery queryLib;
    private boolean localCache = false;
    private Vector<String> uniqueID = new Vector<>();
    private Vector<String> nickname = new Vector<>();
    private Vector<String> description = new Vector<>();
    private Vector<String> lastIP = new Vector<>();
    private Vector<Integer> createdAt = new Vector<>();
    private Vector<Integer> lastOnline = new Vector<>();
    private Vector<Integer> databaseID = new Vector<>();
    private Vector<HashMap<String, String>> tempCache = new Vector<>();
    private HashMap<String, String> lastClient = null;
    private long lastClientTime = 0;
    private int currentPosition = 0;
    private boolean updateIsRunning = false;
    private boolean disconnectIfReady = false;

    public ClientDatabaseCache(JTS3ServerQuery queryLib, JTS3ServerMod modClass, boolean localCache) {
        this.queryLib = queryLib;
        this.modClass = modClass;
        this.localCache = localCache;
        if (localCache) {
            this.updateCache();
        }
    }

    public ClientDatabaseCache(JTS3ServerQuery queryLib, JTS3ServerMod modClass, boolean localCache, boolean disconnectIfReady) {
        this.queryLib = queryLib;
        this.modClass = modClass;
        this.disconnectIfReady = disconnectIfReady;
        this.localCache = localCache;
        if (localCache) {
            this.updateCache();
        }
    }

    private void updateCache() {
        if (!this.localCache) {
            return;
        }
        this.currentPosition = 0;
        new Thread(new Runnable(){

            public void run() {
                ClientDatabaseCache.this.modClass.addLogEntry((byte)1, "Creating client database cache...", true);
                ClientDatabaseCache.access$1(ClientDatabaseCache.this, true);
                while (ClientDatabaseCache.this.updateIsRunning) {
                    Vector<HashMap<String, String>> clientDBList;
                    block12 : {
                        try {
                            clientDBList = ClientDatabaseCache.this.queryLib.getList(5, "start=" + Integer.toString(ClientDatabaseCache.this.currentPosition) + ",duration=" + "50");
                            if (clientDBList == null || clientDBList.size() == 0) break;
                            if (!ClientDatabaseCache.this.updateIsRunning) {
                            }
                            break block12;
                        }
                        catch (Exception e) {}
                        break;
                    }
                    for (HashMap<String, String> clientInfo : clientDBList) {
                        try {
                            if (ClientDatabaseCache.this.databaseID.size() <= ClientDatabaseCache.this.currentPosition) {
                                ClientDatabaseCache.this.createdAt.addElement(Integer.parseInt(clientInfo.get("client_created")));
                                ClientDatabaseCache.this.lastOnline.addElement(Integer.parseInt(clientInfo.get("client_lastconnected")));
                                ClientDatabaseCache.this.databaseID.addElement(Integer.parseInt(clientInfo.get("cldbid")));
                                ClientDatabaseCache.this.nickname.addElement(clientInfo.get("client_nickname"));
                                ClientDatabaseCache.this.description.addElement(clientInfo.get("client_description"));
                                ClientDatabaseCache.this.lastIP.addElement(clientInfo.get("client_lastip"));
                                ClientDatabaseCache.this.uniqueID.addElement(clientInfo.get("client_unique_identifier"));
                            } else {
                                ClientDatabaseCache.this.createdAt.setElementAt(Integer.parseInt(clientInfo.get("client_created")), ClientDatabaseCache.this.currentPosition);
                                ClientDatabaseCache.this.lastOnline.setElementAt(Integer.parseInt(clientInfo.get("client_lastconnected")), ClientDatabaseCache.this.currentPosition);
                                ClientDatabaseCache.this.databaseID.setElementAt(Integer.parseInt(clientInfo.get("cldbid")), ClientDatabaseCache.this.currentPosition);
                                ClientDatabaseCache.this.nickname.setElementAt(clientInfo.get("client_nickname"), ClientDatabaseCache.this.currentPosition);
                                ClientDatabaseCache.this.description.setElementAt(clientInfo.get("client_description"), ClientDatabaseCache.this.currentPosition);
                                ClientDatabaseCache.this.lastIP.setElementAt(clientInfo.get("client_lastip"), ClientDatabaseCache.this.currentPosition);
                                ClientDatabaseCache.this.uniqueID.setElementAt(clientInfo.get("client_unique_identifier"), ClientDatabaseCache.this.currentPosition);
                            }
                        }
                        catch (NumberFormatException nfe) {
                            ClientDatabaseCache.this.modClass.addLogEntry((byte)2, "Got invalid information for client \"" + clientInfo.get("client_nickname") + "\", skipping client!", false);
                            continue;
                        }
                        ClientDatabaseCache clientDatabaseCache = ClientDatabaseCache.this;
                        ClientDatabaseCache.access$12(clientDatabaseCache, clientDatabaseCache.currentPosition + 1);
                    }
                    try {
                        Thread.sleep(5000);
                        continue;
                    }
                    catch (Exception e) {
                        break;
                    }
                }
                ClientDatabaseCache.this.updateFromTempCache();
                ClientDatabaseCache.access$1(ClientDatabaseCache.this, false);
                ClientDatabaseCache.this.modClass.addLogEntry((byte)1, "Client database cache created, " + Integer.toString(ClientDatabaseCache.this.databaseID.size()) + " clients in cache.", true);
                if (ClientDatabaseCache.this.disconnectIfReady) {
                    ClientDatabaseCache.this.queryLib.closeTS3Connection();
                }
            }
        }).start();
    }

    private void updateFromTempCache()
    {
      if (!this.localCache) {
        return;
      }
      if (!this.updateIsRunning) {
        return;
      }
      while (this.tempCache.size() > 0)
      {
        internalUpdateSingleClient(this.tempCache.elementAt(0));
        this.tempCache.removeElementAt(0);
        if (!this.updateIsRunning) {
          break;
        }
      }
    }

    void updateSingleClient(HashMap<String, String> clientInfo) {
        if (!this.localCache) {
            return;
        }
        if (this.updateIsRunning) {
            this.tempCache.addElement(clientInfo);
            return;
        }
        this.internalUpdateSingleClient(clientInfo);
    }

    private void internalUpdateSingleClient(HashMap<String, String> clientInfo) {
        try {
            if (Integer.parseInt(clientInfo.get("client_type")) != 0) {
                return;
            }
            int searchDBID = Integer.parseInt(clientInfo.get("client_database_id"));
            int index = this.databaseID.indexOf(searchDBID);
            if (index >= 0) {
                this.nickname.setElementAt(clientInfo.get("client_nickname"), index);
                this.lastOnline.setElementAt((int)(System.currentTimeMillis() / 1000), index);
                if (clientInfo.get("client_description") != null) {
                    this.description.setElementAt(clientInfo.get("client_description"), index);
                }
                if (this.uniqueID.elementAt(index).length() == 0 && clientInfo.get("client_unique_identifier") != null) {
                    this.uniqueID.setElementAt(clientInfo.get("client_unique_identifier"), index);
                }
                if (clientInfo.get("connection_client_ip") != null) {
                    this.lastIP.setElementAt(clientInfo.get("connection_client_ip"), index);
                }
            } else {
                this.nickname.addElement(clientInfo.get("client_nickname"));
                this.description.addElement(clientInfo.get("client_description") == null ? "" : clientInfo.get("client_description"));
                this.uniqueID.addElement(clientInfo.get("client_unique_identifier") == null ? "" : clientInfo.get("client_unique_identifier"));
                this.createdAt.addElement((int)(System.currentTimeMillis() / 1000));
                this.lastOnline.addElement((int)(System.currentTimeMillis() / 1000));
                this.databaseID.addElement(Integer.parseInt(clientInfo.get("client_database_id")));
                this.lastIP.addElement(clientInfo.get("connection_client_ip") == null ? "" : clientInfo.get("connection_client_ip"));
            }
        }
        catch (NumberFormatException e) {
            this.modClass.addLogEntry((byte)2, "Got invalid information for client \"" + clientInfo.get("client_nickname") + "\"!", false);
        }
    }

    void stopUpdating() {
        this.updateIsRunning = false;
    }

    @Override
    public int getClientCount() {
        if (!this.localCache) {
            return -1;
        }
        return this.databaseID.size();
    }

    @Override
    public String getLastIP(int clientDBID) {
        int pos;
        if (!this.localCache) {
            try {
                if (this.lastClient == null || this.lastClientTime < System.currentTimeMillis() - 60000 || Integer.parseInt(this.lastClient.get("client_database_id")) != clientDBID) {
                    this.lastClient = this.queryLib.getInfo(14, clientDBID);
                    this.lastClientTime = System.currentTimeMillis();
                }
                return this.lastClient.get("client_lastip");
            }
            catch (Exception var2_2) {
                // empty catch block
            }
        }
        if ((pos = this.databaseID.indexOf(clientDBID)) != -1) {
            return this.lastIP.elementAt(pos);
        }
        return null;
    }

    @Override
    public int getLastOnline(int clientDBID) {
        int pos;
        if (!this.localCache) {
            try {
                if (this.lastClient == null || this.lastClientTime < System.currentTimeMillis() - 60000 || Integer.parseInt(this.lastClient.get("client_database_id")) != clientDBID) {
                    this.lastClient = this.queryLib.getInfo(14, clientDBID);
                    this.lastClientTime = System.currentTimeMillis();
                }
                return Integer.parseInt(this.lastClient.get("client_lastconnected"));
            }
            catch (Exception var2_2) {
                // empty catch block
            }
        }
        if ((pos = this.databaseID.indexOf(clientDBID)) != -1) {
            return this.lastOnline.elementAt(pos);
        }
        return -1;
    }

    @Override
    public int getCreatedAt(int clientDBID) {
        int pos;
        if (!this.localCache) {
            try {
                if (this.lastClient == null || this.lastClientTime < System.currentTimeMillis() - 60000 || Integer.parseInt(this.lastClient.get("client_database_id")) != clientDBID) {
                    this.lastClient = this.queryLib.getInfo(14, clientDBID);
                    this.lastClientTime = System.currentTimeMillis();
                }
                return Integer.parseInt(this.lastClient.get("client_created"));
            }
            catch (Exception var2_2) {
                // empty catch block
            }
        }
        if ((pos = this.databaseID.indexOf(clientDBID)) != -1) {
            return this.createdAt.elementAt(pos);
        }
        return -1;
    }

    @Override
    public String getNickname(int clientDBID) {
        int pos;
        if (!this.localCache) {
            try {
                if (this.lastClient == null || this.lastClientTime < System.currentTimeMillis() - 60000 || Integer.parseInt(this.lastClient.get("client_database_id")) != clientDBID) {
                    this.lastClient = this.queryLib.getInfo(14, clientDBID);
                    this.lastClientTime = System.currentTimeMillis();
                }
                return this.lastClient.get("client_nickname");
            }
            catch (Exception var2_2) {
                // empty catch block
            }
        }
        if ((pos = this.databaseID.indexOf(clientDBID)) != -1) {
            return this.nickname.elementAt(pos);
        }
        return null;
    }

    @Override
    public String getUniqueID(int clientDBID) {
        int pos;
        if (!this.localCache) {
            try {
                if (this.lastClient == null || this.lastClientTime < System.currentTimeMillis() - 60000 || Integer.parseInt(this.lastClient.get("client_database_id")) != clientDBID) {
                    this.lastClient = this.queryLib.getInfo(14, clientDBID);
                    this.lastClientTime = System.currentTimeMillis();
                }
                return this.lastClient.get("client_unique_identifier");
            }
            catch (Exception var2_2) {
                // empty catch block
            }
        }
        if ((pos = this.databaseID.indexOf(clientDBID)) != -1) {
            return this.uniqueID.elementAt(pos);
        }
        return null;
    }

    @Override
    public String getDescription(int clientDBID) {
        int pos;
        if (!this.localCache) {
            try {
                if (this.lastClient == null || this.lastClientTime < System.currentTimeMillis() - 60000 || Integer.parseInt(this.lastClient.get("client_database_id")) != clientDBID) {
                    this.lastClient = this.queryLib.getInfo(14, clientDBID);
                    this.lastClientTime = System.currentTimeMillis();
                }
                return this.lastClient.get("client_description");
            }
            catch (Exception var2_2) {
                // empty catch block
            }
        }
        if ((pos = this.databaseID.indexOf(clientDBID)) != -1) {
            return this.description.elementAt(pos);
        }
        return null;
    }

    @Override
    public int getDatabaseID(String clientUniqueID) {
        int pos;
        if (!this.localCache) {
            try {
                return this.queryLib.searchClientDB(clientUniqueID, true).firstElement();
            }
            catch (Exception var2_2) {
                // empty catch block
            }
        }
        if ((pos = this.uniqueID.indexOf(clientUniqueID)) != -1) {
            return this.databaseID.elementAt(pos);
        }
        return -1;
    }

    @Override
    public boolean isCacheLocal() {
        return this.localCache;
    }

    @Override
    public boolean isUpdateRunning() {
        return this.updateIsRunning;
    }

    @Override
    public Vector<HashMap<String, Integer>> searchInactiveClients(int daysInactive, int sortOrder) {
        if (!this.localCache) {
            return null;
        }
        if (this.updateIsRunning) {
            return null;
        }
        if (daysInactive < 10) {
            return null;
        }
        long daysInactiveSeconds = System.currentTimeMillis() / 1000 - (long)(daysInactive * 86400);
        Vector<HashMap<String, Integer>> result = new Vector<HashMap<String, Integer>>();
        int i = 0;
        while (i < this.lastOnline.size()) {
            if ((long)this.lastOnline.elementAt(i).intValue() < daysInactiveSeconds) {
                HashMap<String, Integer> entry = new HashMap<String, Integer>();
                entry.put("cldbid", this.databaseID.elementAt(i));
                entry.put("lastonline", this.lastOnline.elementAt(i));
                result.addElement(entry);
            }
            ++i;
        }
        if (sortOrder >= 0) {
            ClientLastOnlineComparator cloc = new ClientLastOnlineComparator(sortOrder != 1);
            Collections.sort(result, cloc);
        }
        return result;
    }

    @Override
    public Vector<Integer> searchIPAddress(String search) {
        Vector<Integer> result;
        block16 : {
            if (!this.localCache) {
                return null;
            }
            if (this.updateIsRunning) {
                return null;
            }
            if (search == null) {
                return null;
            }
            result = new Vector<Integer>();
            String ipaddressTemp = search.replace("*", "");
            if (ipaddressTemp.length() >= 3) break block16;
            return null;
        }
        try {
            String tmp;
            boolean startsWith = search.startsWith("*");
            boolean endsWith = search.endsWith("*");
            StringTokenizer st = new StringTokenizer(search, "*", false);
            Vector<String> parts = new Vector<String>();
            while (st.hasMoreTokens()) {
                tmp = st.nextToken();
                if (tmp.length() <= 0) continue;
                parts.addElement(tmp.toLowerCase());
            }
            int pos = -1;
            int i = 0;
            while (i < this.lastIP.size()) {
                tmp = this.lastIP.elementAt(i).toLowerCase();
                pos = 0;
                int x = 0;
                while (x < parts.size()) {
                    String tmpPart = (String)parts.elementAt(x);
                    if (parts.size() == 1) {
                        if (tmp.equalsIgnoreCase(tmpPart)) {
                            result.addElement(this.databaseID.elementAt(i));
                            break;
                        }
                        if (!startsWith && !endsWith) break;
                    }
                    if (x == 0 && !startsWith) {
                        if (!tmp.startsWith(tmpPart)) break;
                        pos = tmpPart.length();
                        if (x == parts.size() - 1) {
                            result.addElement(this.databaseID.elementAt(i));
                        }
                    } else {
                        if (x == parts.size() - 1 && !endsWith) {
                            if (!tmp.endsWith(tmpPart)) break;
                            result.addElement(this.databaseID.elementAt(i));
                            break;
                        }
                        if ((pos = tmp.indexOf(tmpPart, pos)) == -1) break;
                        pos += tmpPart.length();
                        if (x == parts.size() - 1) {
                            result.addElement(this.databaseID.elementAt(i));
                        }
                    }
                    ++x;
                }
                ++i;
            }
        }
        catch (Exception ipaddressTemp) {
            // empty catch block
        }
        return result;
    }

    @Override
    public Vector<Integer> searchClientNickname(String search) {
        Vector<Integer> result;
        block22 : {
            if (search == null) {
                return null;
            }
            if (!this.localCache) {
                Vector<Integer> searchUID = null;
                try {
                    searchUID = this.queryLib.searchClientDB(search, true);
                }
                catch (Exception var3_3) {
                    // empty catch block
                }
                try {
                    if (searchUID == null || searchUID.size() == 0) {
                        return this.queryLib.searchClientDB(search.replace("*", "%"), false);
                    }
                    return searchUID;
                }
                catch (Exception var3_4) {
                    // empty catch block
                }
            }
            if (this.updateIsRunning) {
                return null;
            }
            result = new Vector<Integer>();
            int tempID = this.getDatabaseID(search);
            if (tempID != -1) {
                result.addElement(tempID);
                return result;
            }
            String clientnameTemp = search.replace("*", "");
            if (clientnameTemp.length() >= 3) break block22;
            return null;
        }
        try {
            String tmp;
            boolean startsWith = search.startsWith("*");
            boolean endsWith = search.endsWith("*");
            StringTokenizer st = new StringTokenizer(search, "*", false);
            Vector<String> parts = new Vector<String>();
            while (st.hasMoreTokens()) {
                tmp = st.nextToken();
                if (tmp.length() <= 0) continue;
                parts.addElement(tmp.toLowerCase());
            }
            int pos = -1;
            int i = 0;
            while (i < this.nickname.size()) {
                tmp = this.nickname.elementAt(i).toLowerCase();
                pos = 0;
                int x = 0;
                while (x < parts.size()) {
                    String tmpPart = (String)parts.elementAt(x);
                    if (parts.size() == 1) {
                        if (tmp.equalsIgnoreCase(tmpPart)) {
                            result.addElement(this.databaseID.elementAt(i));
                            break;
                        }
                        if (!startsWith && !endsWith) break;
                    }
                    if (x == 0 && !startsWith) {
                        if (!tmp.startsWith(tmpPart)) break;
                        pos = tmpPart.length();
                        if (x == parts.size() - 1) {
                            result.addElement(this.databaseID.elementAt(i));
                        }
                    } else {
                        if (x == parts.size() - 1 && !endsWith) {
                            if (!tmp.endsWith(tmpPart)) break;
                            result.addElement(this.databaseID.elementAt(i));
                            break;
                        }
                        if ((pos = tmp.indexOf(tmpPart, pos)) == -1) break;
                        pos += tmpPart.length();
                        if (x == parts.size() - 1) {
                            result.addElement(this.databaseID.elementAt(i));
                        }
                    }
                    ++x;
                }
                ++i;
            }
        }
        catch (Exception clientnameTemp) {
            // empty catch block
        }
        return result;
    }

    static /* synthetic */ void access$1(ClientDatabaseCache clientDatabaseCache, boolean bl) {
        clientDatabaseCache.updateIsRunning = bl;
    }

    static /* synthetic */ void access$12(ClientDatabaseCache clientDatabaseCache, int n) {
        clientDatabaseCache.currentPosition = n;
    }

}

