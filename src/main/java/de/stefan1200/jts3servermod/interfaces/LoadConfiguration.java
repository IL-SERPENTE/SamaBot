/*
 * Decompiled with CFR 0_110.
 */
package de.stefan1200.jts3servermod.interfaces;

import de.stefan1200.jts3servermod.BotConfigurationException;
import de.stefan1200.util.ArrangedPropertiesWriter;
import java.util.BitSet;

public interface LoadConfiguration {
    public void initConfig(ArrangedPropertiesWriter var1);

    public boolean loadConfig(ArrangedPropertiesWriter var1, boolean var2) throws BotConfigurationException, NumberFormatException;

    public void setListModes(BitSet var1);
}

