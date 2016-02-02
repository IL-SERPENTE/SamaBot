/*
 * Decompiled with CFR 0_110.
 */
package de.stefan1200.jts3servermod.interfaces;

public interface ServerInfoCache_Interface {
    public String getServerName();

    public String getServerVersion();

    public String getServerPlatform();

    public long getServerUptime();

    public long getServerUptimeTimestamp();

    public long getServerCreatedAt();

    public long getServerMonthBytesDownloaded();

    public long getServerMonthBytesUploaded();

    public long getServerTotalBytesDownloaded();

    public long getServerTotalBytesUploaded();

    public int getServerMaxClients();

    public int getServerReservedSlots();

    public int getServerChannelCount();

    public int getServerClientCount();

    public int getServerClientDBCount();

    public long getServerClientConnectionsCount();

    public long getServerMinClientVersion();

    public int getServerDefaultServerGroup();

    public int getServerDefaultChannelAdminGroup();

    public int getServerDefaultChannelGroup();

    public long getServerMaxDownloadTotalBandwidth();

    public long getServerMaxUploadTotalBandwidth();

    public long getServerDownloadQuota();

    public long getServerUploadQuota();
}

