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

/**
 * Created by Rigner for project TSBot.
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
        config.setDebugLevel(Level.ALL);
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
        bot.run();
        bot.end();
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
