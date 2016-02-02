/*
 * Decompiled with CFR 0_110.
 */
package de.stefan1200.jts3servermod.interfaces;

import java.util.HashMap;

public interface HandleTS3Events {
    public String[] botChatCommandList(HashMap<String, String> var1, boolean var2, boolean var3);

    public String botChatCommandHelp(String var1);

    public boolean handleChatCommands(String var1, HashMap<String, String> var2, boolean var3, boolean var4);

    public void handleClientEvents(String var1, HashMap<String, String> var2);
}

