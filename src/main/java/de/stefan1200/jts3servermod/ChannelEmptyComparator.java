/*
 * Decompiled with CFR 0_110.
 */
package de.stefan1200.jts3servermod;

import java.util.Comparator;
import java.util.HashMap;

/*
 * This class specifies class file version 49.0 but uses Java 6 signatures.  Assumed Java 6.
 */
public class ChannelEmptyComparator
implements Comparator<HashMap<String, String>> {
    @Override
    public int compare(HashMap<String, String> o1, HashMap<String, String> o2) {
        int i2;
        int i1 = Integer.parseInt(o1.get("seconds_empty"));
        if (i1 < (i2 = Integer.parseInt(o2.get("seconds_empty")))) {
            return 1;
        }
        if (i1 > i2) {
            return -1;
        }
        return 0;
    }
}

