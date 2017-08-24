package net.samagames.tsbot;

import com.github.theholywaffle.teamspeak3.TS3Api;
import com.github.theholywaffle.teamspeak3.TS3Config;
import com.github.theholywaffle.teamspeak3.TS3Query;
import com.github.theholywaffle.teamspeak3.api.event.TS3EventType;
import net.samagames.tsbot.channels.ChannelManager;
import net.samagames.tsbot.database.DatabaseConnector;
import net.samagames.tsbot.listeners.TSListener;

import java.util.logging.Level;
import java.util.logging.Logger;

/*
 * This file is part of SamaBot.
 *
 * SamaBot is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * SamaBot is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with SamaBot.  If not, see <http://www.gnu.org/licenses/>.
 */
public class TSBot
{
    public static final Logger LOGGER = Logger.getLogger("TSBot");
    public static final String CHANNEL = "tsbot";
    public static final String CHANNEL_RESPONSE = "tsbotresponse";
    public static final String CHANNEL_GROUPCHANGE = "groupchange";

    private TSConfiguration configuration;
    private DatabaseConnector databaseConnector;
    private TS3Api ts3Api;
    private TS3Query ts3Query;
    private TSSender tsSender;
    private ChannelManager channelManager;
    private boolean end;

    public TSBot()
    {
        this.configuration = new TSConfiguration();
        this.databaseConnector = new DatabaseConnector(this);
        if (!this.configuration.reload()
                || !this.databaseConnector.connect())
            System.exit(0);
        this.databaseConnector.subscribe();

        TS3Config config = new TS3Config();
        config.setHost(this.configuration.getTeamspeakIp());
        config.setDebugLevel(Level.INFO);
        config.setFloodRate(TS3Query.FloodRate.UNLIMITED);
        config.setQueryPort(this.configuration.getTeamspeakPort());

        this.ts3Query = new TS3Query(config);
        this.ts3Query.connect();

        this.ts3Api = this.ts3Query.getApi();
        this.ts3Api.login(this.configuration.getTeamspeakUser(), this.configuration.getTeamspeakPassword());
        this.ts3Api.selectVirtualServerById(1);
        this.ts3Api.setNickname("SamaBotv2");
        this.ts3Api.registerEvent(TS3EventType.SERVER);
        this.ts3Api.addTS3Listeners(new TSListener(this));

        this.tsSender = new TSSender(this);
        this.channelManager = new ChannelManager();

        this.end = false;
    }

    private void run()
    {
        while (!this.end)
            try
            {
                synchronized (this)
                {
                    this.wait();
                }
            } catch (InterruptedException ignored) {}
    }

    private void end()
    {
        this.getChannelManager().getChannelList().forEach(botChannel -> this.ts3Api.deleteChannel(botChannel.getRealId()));
        this.databaseConnector.disconnect();
        this.ts3Query.exit();
    }

    public TSConfiguration getConfiguration()
    {
        return configuration;
    }

    public static void main(String[] args)
    {
        TSBot bot = new TSBot();
        Runtime.getRuntime().addShutdownHook(new Thread(bot::end));
        bot.run();
        System.exit(0);
    }

    public TS3Api getTs3Api()
    {
        return ts3Api;
    }

    public void setEnd(boolean end)
    {
        this.end = end;
        synchronized (this)
        {
            this.notifyAll();
        }
    }

    public DatabaseConnector getDatabaseConnector()
    {
        return databaseConnector;
    }

    public TSSender getPubsub()
    {
        return tsSender;
    }

    public ChannelManager getChannelManager()
    {
        return channelManager;
    }
}
