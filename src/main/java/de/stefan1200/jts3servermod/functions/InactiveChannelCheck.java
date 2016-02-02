/*
 * Decompiled with CFR 0_110.
 */
package de.stefan1200.jts3servermod.functions;

import de.stefan1200.jts3servermod.BotConfigurationException;
import de.stefan1200.jts3servermod.FunctionExceptionLog;
import de.stefan1200.jts3servermod.interfaces.HandleBotEvents;
import de.stefan1200.jts3servermod.interfaces.JTS3ServerMod_Interface;
import de.stefan1200.jts3servermod.interfaces.LoadConfiguration;
import de.stefan1200.jts3serverquery.JTS3ServerQuery;
import de.stefan1200.jts3serverquery.TS3ServerQueryException;
import de.stefan1200.util.ArrangedPropertiesWriter;
import java.util.BitSet;
import java.util.HashMap;
import java.util.StringTokenizer;
import java.util.Vector;

public class InactiveChannelCheck
implements LoadConfiguration,
HandleBotEvents {
    /*private static final byte PARENT_MODE_IGNORE = 0;
    private static final byte PARENT_MODE_ONLY = 1;
    private static final byte PARENT_MODE_PARENTONLY = 2;*/
    private String configPrefix = "";
    private JTS3ServerMod_Interface modClass = null;
    private JTS3ServerQuery queryLib = null;
    private boolean pluginEnabled = false;
    private long emptyDeleteTime = -1;
    private boolean ignorePermanent = true;
    private boolean ignoreSemiPermanent = false;
    private Vector<Integer> ignoreChannelList = new Vector<>();
    private Vector<Integer> parentChannelList = new Vector<>();
    private byte parentChannelList_mode = 0;
    private FunctionExceptionLog fel = new FunctionExceptionLog();
    private long lastCheck = 0;

    public void initClass(JTS3ServerMod_Interface modClass, JTS3ServerQuery queryLib, String prefix) {
        this.modClass = modClass;
        this.queryLib = queryLib;
        this.configPrefix = prefix.trim();
    }

    public void handleOnBotConnect() {
        if (!this.pluginEnabled) {
            return;
        }
        StringBuffer sbList = new StringBuffer();
        if (!this.ignorePermanent) {
            sbList.append("permanent");
        }
        if (!this.ignoreSemiPermanent) {
            if (sbList.length() > 0) {
                sbList.append(" and ");
            }
            sbList.append("semi-permanent");
        }
        String msg = "Delete " + sbList.toString() + " channels if empty for at least " + Long.toString(this.emptyDeleteTime / 60 / 60) + " hours!";
        this.modClass.addLogEntry(this.configPrefix, (byte)1, msg, true);
    }

    public void handleAfterCacheUpdate() {
        if (!this.pluginEnabled) {
            return;
        }
        if (System.currentTimeMillis() - this.lastCheck < 600000) {
            return;
        }
        int channelID = 0;
        int emptySeconds = 0;
        for (HashMap<String, String> channel : this.modClass.getChannelList()) {
            if (channel.get("channel_flag_default").equals("1") || this.ignorePermanent && channel.get("channel_flag_permanent").equals("1") || this.ignoreSemiPermanent && channel.get("channel_flag_semi_permanent").equals("1") || channel.get("channel_flag_permanent").equals("0") && channel.get("channel_flag_semi_permanent").equals("0") || this.modClass.isIDListed(channelID = Integer.parseInt(channel.get("cid")), this.ignoreChannelList)) continue;
            boolean result = this.parentChannelList_mode == 2 ? this.modClass.isIDListed(channelID, this.parentChannelList) : this.modClass.isIDListed(Integer.parseInt(channel.get("pid")), this.parentChannelList);
            if (!(this.parentChannelList_mode == 0 ? !result : result)) continue;
            emptySeconds = Integer.parseInt(channel.get("seconds_empty"));
            if (emptySeconds > 31536000) {
                this.modClass.addLogEntry(this.configPrefix, (byte)2, "Got bad values for channel \"" + channel.get("channel_name") + "\" (id: " + Integer.toString(channelID) + ") from TS3 server, skipping channel!", false);
                continue;
            }
            if ((long)emptySeconds <= this.emptyDeleteTime) continue;
            try {
                this.queryLib.deleteChannel(channelID, false);
                this.modClass.addLogEntry(this.configPrefix, (byte)1, "Channel \"" + channel.get("channel_name") + "\" (id: " + Integer.toString(channelID) + ") was empty for more than " + Long.toString(emptySeconds / 60 / 60) + " hours. Channel was deleted!", false);
                this.fel.clearException(channelID);
                continue;
            }
            catch (TS3ServerQueryException sqe) {
                if (this.fel.existsException(sqe, channelID)) continue;
                this.fel.addException(sqe, channelID);
                this.modClass.addLogEntry(this.configPrefix, (byte)3, "Channel \"" + channel.get("channel_name") + "\" (id: " + Integer.toString(channelID) + ") was empty for more than " + Long.toString(emptySeconds / 60 / 60) + " hours, but an error occurred while deleting channel!", false);
                this.modClass.addLogEntry(this.configPrefix, sqe, false);
                continue;
            }
            catch (Exception e) {
                this.modClass.addLogEntry(this.configPrefix, (byte)3, "Channel \"" + channel.get("channel_name") + "\" (id: " + Integer.toString(channelID) + ") was empty for more than " + Long.toString(emptySeconds / 60 / 60) + " hours, but an error occurred while deleting channel!", false);
                this.modClass.addLogEntry(this.configPrefix, e, false);
            }
        }
        this.lastCheck = System.currentTimeMillis();
    }

    public void activate() {
    }

    public void disable() {
    }

    public void unload() {
        this.ignoreChannelList = null;
        this.parentChannelList = null;
    }

    public boolean multipleInstances() {
        return true;
    }

    public void initConfig(ArrangedPropertiesWriter config) {
        config.addKey(String.valueOf(this.configPrefix) + "_emptydeletetime", "After how many hours an empty channel should be deleted? Possible values between 1 and 2200 hours.", "168");
        config.addKey(String.valueOf(this.configPrefix) + "_ignore_permanent", "Never delete permanent channels? Set yes or no here!", "yes");
        config.addKey(String.valueOf(this.configPrefix) + "_ignore_semipermanent", "Never delete semi permanent channels? Set yes or no here!", "no");
        config.addKey(String.valueOf(this.configPrefix) + "_ignore_channels", "A comma separated list (without spaces) of channel ids you like to ignore. This channels don't get deleted!");
        config.addKey(String.valueOf(this.configPrefix) + "_parentchannel_list", "A comma separated list (without spaces) of parent channel ids (use 0 for the top level).\nDepends on the given mode, all sub-channels of this channels can be ignored or only sub-channels of this channels will be checked!\nThe check parent channel only mode allows you to check the activity only on the selected parent channels (which will be reset also on activity in sub-channels) and delete the parent channel including the sub channels, if it is detected inactive.\nIf no parent channels should be ignored, set no channels here and select the channel list mode ignore!");
        config.addKey(String.valueOf(this.configPrefix) + "_parentchannel_list_mode", "Select one of the three modes for the parent channel list.\nignore = All sub-channels of the selected channels will be ignored.\nonly = Only sub-channels of the selected channels will be checked.\nparentonly = Only the parent channels will be checked (and will delete all sub channels).", "ignore");
    }

    public boolean loadConfig(ArrangedPropertiesWriter config, boolean slowMode) throws BotConfigurationException, NumberFormatException {
        String lastNumberValue = "";
        String temp = null;
        this.pluginEnabled = false;
        try {
            StringTokenizer st;
            lastNumberValue = String.valueOf(this.configPrefix) + "_emptydeletetime";
            temp = config.getValue(String.valueOf(this.configPrefix) + "_emptydeletetime");
            if (temp == null) {
                throw new NumberFormatException();
            }
            this.emptyDeleteTime = Long.parseLong(temp.trim()) * 60 * 60;
            if (this.emptyDeleteTime < 3600) {
                this.emptyDeleteTime = 3600;
            } else if (this.emptyDeleteTime > 7920000) {
                this.emptyDeleteTime = 7920000;
            }
            this.ignorePermanent = config.getValue(String.valueOf(this.configPrefix) + "_ignore_permanent", "yes").trim().equalsIgnoreCase("yes");
            this.ignoreSemiPermanent = config.getValue(String.valueOf(this.configPrefix) + "_ignore_semipermanent", "no").trim().equalsIgnoreCase("yes");
            if (this.ignorePermanent && this.ignoreSemiPermanent) {
                throw new BotConfigurationException("Ignoring permanent and semi-permanent channels are activated, disabled Inactive Channel Check!");
            }
            temp = null;
            this.ignoreChannelList.clear();
            temp = config.getValue(String.valueOf(this.configPrefix) + "_ignore_channels");
            lastNumberValue = String.valueOf(this.configPrefix) + "_ignore_channels";
            if (temp != null && temp.length() > 0) {
                st = new StringTokenizer(temp, ",", false);
                while (st.hasMoreTokens()) {
                    this.ignoreChannelList.addElement(Integer.parseInt(st.nextToken().trim()));
                }
            }
            temp = null;
            this.parentChannelList.clear();
            temp = config.getValue(String.valueOf(this.configPrefix) + "_parentchannel_list");
            lastNumberValue = String.valueOf(this.configPrefix) + "_parentchannel_list";
            if (temp != null && temp.length() > 0) {
                st = new StringTokenizer(temp, ",", false);
                while (st.hasMoreTokens()) {
                    this.parentChannelList.addElement(Integer.parseInt(st.nextToken().trim()));
                }
            }
            this.parentChannelList_mode = (temp = config.getValue(String.valueOf(this.configPrefix) + "_parentchannel_list_mode", "ignore").trim()).equalsIgnoreCase("only") ? 1 : (temp.equalsIgnoreCase("parentonly") ? (byte)2 : 0);
            this.pluginEnabled = true;
        }
        catch (NumberFormatException e) {
            NumberFormatException nfe = new NumberFormatException("Config value of \"" + lastNumberValue + "\" is not a number! Current value: " + config.getValue(lastNumberValue, "not set"));
            nfe.setStackTrace(e.getStackTrace());
            throw nfe;
        }
        return this.pluginEnabled;
    }

    public void setListModes(BitSet listOptions) {
    }
}

