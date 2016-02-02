/*
 * Decompiled with CFR 0_110.
 */
package de.stefan1200.jts3servermod;

import de.stefan1200.jts3serverquery.TS3ServerQueryException;

import java.util.Vector;

public class FunctionExceptionLog {
    Vector<TS3ServerQueryException> exceptionLog = new Vector<TS3ServerQueryException>();
    Vector<Integer> exceptionTargetID = new Vector<Integer>();

    public void addException(TS3ServerQueryException exception) {
        this.addException(exception, -1);
    }

    public void addException(TS3ServerQueryException exception, int targetID) {
        this.exceptionLog.addElement(exception);
        this.exceptionTargetID.addElement(targetID);
    }

    public boolean existsException(TS3ServerQueryException exception) {
        return this.existsException(exception, -1);
    }

    public boolean existsException(TS3ServerQueryException exception, int targetID) {
        int i = 0;
        while (i < this.exceptionLog.size()) {
            if (exception.getErrorID() == this.exceptionLog.elementAt(i).getErrorID() && exception.getFailedPermissionID() == this.exceptionLog.elementAt(i).getFailedPermissionID() && this.exceptionTargetID.elementAt(i) == targetID) {
                return true;
            }
            ++i;
        }
        return false;
    }

    public void clearException(int targetID) {
        int i = -1;
        while ((i = this.exceptionTargetID.indexOf(targetID)) >= 0) {
            this.exceptionLog.removeElementAt(i);
            this.exceptionTargetID.removeElementAt(i);
        }
    }

    public void clearAllExceptions() {
        this.exceptionLog.clear();
        this.exceptionTargetID.clear();
    }
}

