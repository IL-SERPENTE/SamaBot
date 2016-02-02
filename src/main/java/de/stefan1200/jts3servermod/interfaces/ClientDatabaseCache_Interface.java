/*
 * Decompiled with CFR 0_110.
 */
package de.stefan1200.jts3servermod.interfaces;

import java.util.HashMap;
import java.util.Vector;

public interface ClientDatabaseCache_Interface {
    public int getClientCount();

    public String getLastIP(int var1);

    public int getLastOnline(int var1);

    public int getCreatedAt(int var1);

    public String getNickname(int var1);

    public String getUniqueID(int var1);

    public String getDescription(int var1);

    public int getDatabaseID(String var1);

    public boolean isCacheLocal();

    public boolean isUpdateRunning();

    public Vector<HashMap<String, Integer>> searchInactiveClients(int var1, int var2);

    public Vector<Integer> searchIPAddress(String var1);

    public Vector<Integer> searchClientNickname(String var1);
}

