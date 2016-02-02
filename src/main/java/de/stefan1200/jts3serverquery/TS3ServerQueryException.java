/*
 * Decompiled with CFR 0_110.
 */
package de.stefan1200.jts3serverquery;

public class TS3ServerQueryException
extends Exception {
	private static final long serialVersionUID = -5953692078338193135L;
	String apiMethodName;
    int errorID;
    String errorMessage;
    String extraErrorMessage;
    int failedPermissionID;

    public TS3ServerQueryException(String apiMethodName, String errorID, String errorMessage, String extraErrorMessage, String failedPermissionID) {
        super("ServerQuery Error " + errorID + ": " + errorMessage + (extraErrorMessage != null ? new StringBuilder(" - ").append(extraErrorMessage).toString() : "") + (failedPermissionID != null ? new StringBuilder(" - Permission ID: ").append(failedPermissionID).toString() : ""));
        this.apiMethodName = apiMethodName;
        try {
            this.errorID = Integer.parseInt(errorID);
        }
        catch (NumberFormatException nfe) {
            this.errorID = -1;
        }
        this.errorMessage = errorMessage;
        this.extraErrorMessage = extraErrorMessage;
        try {
            this.failedPermissionID = Integer.parseInt(failedPermissionID);
        }
        catch (NumberFormatException nfe) {
            this.failedPermissionID = -1;
        }
    }

    public String getApiMethodName() {
        return this.apiMethodName;
    }

    public int getErrorID() {
        return this.errorID;
    }

    public String getErrorMessage() {
        return this.errorMessage;
    }

    public String getExtraErrorMessage() {
        return this.extraErrorMessage;
    }

    public int getFailedPermissionID() {
        return this.failedPermissionID;
    }
}

