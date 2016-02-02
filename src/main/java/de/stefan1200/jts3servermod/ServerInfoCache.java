/*
 * Decompiled with CFR 0_110.
 */
package de.stefan1200.jts3servermod;

import de.stefan1200.jts3servermod.interfaces.ServerInfoCache_Interface;
import java.util.HashMap;
import java.util.Vector;

/*
 * This class specifies class file version 49.0 but uses Java 6 signatures.  Assumed Java 6.
 */
public class ServerInfoCache
implements ServerInfoCache_Interface {
    private String serverName = null;
    private String serverVersion = null;
    private String serverPlatform = null;
    private long serverUptimeSince = -1;
    private long serverCreatedAt = -1;
    private long serverDownloadQuota = -1;
    private long serverUploadQuota = -1;
    private long serverMaxDownloadTotalBandwidth = -1;
    private long serverMaxUploadTotalBandwidth = -1;
    private long serverMonthBytesDownloaded = -1;
    private long serverMonthBytesUploaded = -1;
    private long serverTotalBytesDownloaded = -1;
    private long serverTotalBytesUploaded = -1;
    private long serverClientConnectionsCount = -1;
    private long serverMinClientVersion = -1;
    private int serverMaxClients = -1;
    private int serverReservedSlots = -1;
    private int serverChannelCount = -1;
    private int serverClientCount = -1;
    private int serverDefaultServerGroup = -1;
    private int serverDefaultChannelAdminGroup = -1;
    private int serverDefaultChannelGroup = -1;
    private int serverClientDBCount = -1;

    void updateServerInfo(HashMap<String, String> serverinfo, HashMap<String, String> clientdbcount) {
        if (serverinfo == null) {
            return;
        }
        this.serverName = serverinfo.get("virtualserver_name");
        if (this.serverVersion == null) {
            this.serverVersion = serverinfo.get("virtualserver_version");
        }
        if (this.serverPlatform == null) {
            this.serverPlatform = serverinfo.get("virtualserver_platform");
        }
        try {
            if (this.serverUptimeSince == -1) {
                this.serverUptimeSince = System.currentTimeMillis() - Long.parseLong(serverinfo.get("virtualserver_uptime")) * 1000;
            }
            if (this.serverCreatedAt == -1) {
                this.serverCreatedAt = Long.parseLong(serverinfo.get("virtualserver_created")) * 1000;
            }
            this.serverMaxClients = Integer.parseInt(serverinfo.get("virtualserver_maxclients"));
            this.serverReservedSlots = Integer.parseInt(serverinfo.get("virtualserver_reserved_slots"));
            this.serverChannelCount = Integer.parseInt(serverinfo.get("virtualserver_channelsonline"));
            this.serverClientCount = Integer.parseInt(serverinfo.get("virtualserver_clientsonline")) - Integer.parseInt(serverinfo.get("virtualserver_queryclientsonline"));
            this.serverClientConnectionsCount = Long.parseLong(serverinfo.get("virtualserver_client_connections"));
            this.serverDefaultServerGroup = Integer.parseInt(serverinfo.get("virtualserver_default_server_group"));
            this.serverDefaultChannelAdminGroup = Integer.parseInt(serverinfo.get("virtualserver_default_channel_admin_group"));
            this.serverDefaultChannelGroup = Integer.parseInt(serverinfo.get("virtualserver_default_channel_group"));
            this.serverMinClientVersion = Long.parseLong(serverinfo.get("virtualserver_min_client_version"));
            this.serverMonthBytesDownloaded = Long.parseLong(serverinfo.get("virtualserver_month_bytes_downloaded"));
            this.serverMonthBytesUploaded = Long.parseLong(serverinfo.get("virtualserver_month_bytes_uploaded"));
            this.serverTotalBytesDownloaded = Long.parseLong(serverinfo.get("virtualserver_total_bytes_downloaded"));
            this.serverTotalBytesUploaded = Long.parseLong(serverinfo.get("virtualserver_total_bytes_uploaded"));
            this.serverClientDBCount = Integer.parseInt(clientdbcount.get("count"));
        }
        catch (Exception var3_3) {
            // empty catch block
        }
        try {
            this.serverDownloadQuota = Long.parseLong(serverinfo.get("virtualserver_download_quota"));
        }
        catch (Exception e) {
            this.serverDownloadQuota = -1;
        }
        try {
            this.serverUploadQuota = Long.parseLong(serverinfo.get("virtualserver_upload_quota"));
        }
        catch (Exception e) {
            this.serverUploadQuota = -1;
        }
        try {
            this.serverMaxDownloadTotalBandwidth = Long.parseLong(serverinfo.get("virtualserver_max_download_total_bandwidth"));
        }
        catch (Exception e) {
            this.serverMaxDownloadTotalBandwidth = -1;
        }
        try {
            this.serverMaxUploadTotalBandwidth = Long.parseLong(serverinfo.get("virtualserver_max_upload_total_bandwidth"));
        }
        catch (Exception e) {
            this.serverMaxUploadTotalBandwidth = -1;
        }
    }

    void updateClientCount(Vector<HashMap<String, String>> clientList) {
        int clientCountTemp = 0;
        for (HashMap<String, String> clientInfo : clientList) {
            if (!clientInfo.get("client_type").equals("0")) continue;
            ++clientCountTemp;
        }
        this.serverClientCount = clientCountTemp;
    }

    @Override
    public String getServerName() {
        return this.serverName;
    }

    @Override
    public String getServerVersion() {
        return this.serverVersion;
    }

    @Override
    public String getServerPlatform() {
        return this.serverPlatform;
    }

    @Override
    public long getServerUptime() {
        return System.currentTimeMillis() - this.serverUptimeSince;
    }

    @Override
    public long getServerUptimeTimestamp() {
        return this.serverUptimeSince;
    }

    @Override
    public long getServerCreatedAt() {
        return this.serverCreatedAt;
    }

    @Override
    public long getServerDownloadQuota() {
        return this.serverDownloadQuota;
    }

    @Override
    public long getServerUploadQuota() {
        return this.serverUploadQuota;
    }

    @Override
    public long getServerMonthBytesDownloaded() {
        return this.serverMonthBytesDownloaded;
    }

    @Override
    public long getServerMonthBytesUploaded() {
        return this.serverMonthBytesUploaded;
    }

    @Override
    public long getServerTotalBytesDownloaded() {
        return this.serverTotalBytesDownloaded;
    }

    @Override
    public long getServerTotalBytesUploaded() {
        return this.serverTotalBytesUploaded;
    }

    @Override
    public int getServerMaxClients() {
        return this.serverMaxClients;
    }

    @Override
    public int getServerReservedSlots() {
        return this.serverReservedSlots;
    }

    @Override
    public int getServerChannelCount() {
        return this.serverChannelCount;
    }

    @Override
    public int getServerClientCount() {
        return this.serverClientCount;
    }

    @Override
    public int getServerClientDBCount() {
        return this.serverClientDBCount;
    }

    @Override
    public long getServerClientConnectionsCount() {
        return this.serverClientConnectionsCount;
    }

    @Override
    public long getServerMinClientVersion() {
        return this.serverMinClientVersion;
    }

    @Override
    public int getServerDefaultServerGroup() {
        return this.serverDefaultServerGroup;
    }

    @Override
    public int getServerDefaultChannelAdminGroup() {
        return this.serverDefaultChannelAdminGroup;
    }

    @Override
    public int getServerDefaultChannelGroup() {
        return this.serverDefaultChannelGroup;
    }

    @Override
    public long getServerMaxDownloadTotalBandwidth() {
        return this.serverMaxDownloadTotalBandwidth;
    }

    @Override
    public long getServerMaxUploadTotalBandwidth() {
        return this.serverMaxUploadTotalBandwidth;
    }
}

