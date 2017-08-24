package net.samagames.tsbot;

import redis.clients.jedis.Jedis;

import java.util.logging.Level;

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
