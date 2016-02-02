/*
 * Decompiled with CFR 0_110.
 */
package de.stefan1200.jts3servermod.interfaces;

import de.stefan1200.jts3servermod.interfaces.JTS3ServerMod_Interface;
import de.stefan1200.jts3serverquery.JTS3ServerQuery;

public interface HandleBotEvents {
    public void initClass(JTS3ServerMod_Interface var1, JTS3ServerQuery var2, String var3);

    public void handleOnBotConnect();

    public void handleAfterCacheUpdate();

    public void activate();

    public void disable();

    public void unload();

    public boolean multipleInstances();
}

