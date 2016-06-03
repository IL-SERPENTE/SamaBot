package net.samagames.tsbot;

import redis.clients.jedis.Jedis;

import java.util.logging.Level;

/**
 * Created by Rigner for project SamaBot.
 */
public class TSSender
{
    private TSBot tsBot;

    public TSSender(TSBot tsBot)
    {
        this.tsBot = tsBot;
    }

    public void respond(String sender, String msg)
    {
        String toSend = String.join(":", sender, msg);
        Jedis jedis = null;
        try
        {
            jedis = this.tsBot.getDatabaseConnector().getConnection();
            jedis.publish(TSBot.CHANNEL_RESPONSE, toSend);
        }
        catch (Exception ex)
        {
            TSBot.LOGGER.log(Level.SEVERE, "Error getting Jedis pool", ex);
        }
        finally
        {
            if (jedis != null)
                jedis.close();
        }
    }

    public void respondError(String sender)
    {
        this.respondError(sender, null);
    }

    public void respondError(String sender, String error)
    {
        if (error != null)
            this.respond(sender, String.join(":", "ERROR", error));
        else
            this.respond(sender, "ERROR");
    }
}
