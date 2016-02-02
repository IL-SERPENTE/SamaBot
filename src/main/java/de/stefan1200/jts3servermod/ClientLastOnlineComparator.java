/*
 * Decompiled with CFR 0_110.
 */
package de.stefan1200.jts3servermod;

import java.util.Comparator;
import java.util.HashMap;

/*
 * This class specifies class file version 49.0 but uses Java 6 signatures.  Assumed Java 6.
 */
public class ClientLastOnlineComparator
implements Comparator<HashMap<String, Integer>> {
    private boolean reverseOrder = false;

    public ClientLastOnlineComparator(boolean reverseOrder) {
        this.reverseOrder = reverseOrder;
    }

    @Override
    public int compare(HashMap<String, Integer> o1, HashMap<String, Integer> o2) {
        int i2;
        int i1 = o1.get("lastonline");
        if (i1 < (i2 = o2.get("lastonline").intValue())) {
            return this.reverseOrder ? 1 : -1;
        }
        if (i1 > i2) {
            return this.reverseOrder ? -1 : 1;
        }
        return 0;
    }
}

