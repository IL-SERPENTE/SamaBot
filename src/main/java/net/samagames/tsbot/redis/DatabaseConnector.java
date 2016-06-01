package net.samagames.tsbot.redis;

import net.samagames.tsbot.TSBot;
import net.samagames.tsbot.TSReceiver;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;

import java.sql.*;
import java.util.UUID;
import java.util.logging.Level;

/**
 * Created by Rigner for project TSBot.
 */
public class DatabaseConnector
{
    private TSBot bot;
    private JedisPool cachePool;
    private boolean continueSub;
    private Subscriber subscriber;
    private Connection connection;

    public DatabaseConnector(TSBot bot)
    {
        this.bot = bot;
        this.continueSub = true;
    }

    public boolean connect()
    {
        JedisPoolConfig config = new JedisPoolConfig();
        config.setMaxTotal(-1);
        config.setJmxEnabled(false);

        this.cachePool = new JedisPool(config, this.bot.getConfiguration().getRedisIp(), this.bot.getConfiguration().getRedisPort(), 0, this.bot.getConfiguration().getRedisPassword());
        try
        {
            this.cachePool.getResource().close();
            TSBot.LOGGER.info("Connected to redis database.");

            Class.forName("com.mysql.jdbc.Driver");
            this.connection = DriverManager.getConnection("jdbc:mysql://" + this.bot.getConfiguration().getMysqlIp() + "/" + this.bot.getConfiguration().getMysqlDatabase(), this.bot.getConfiguration().getMysqlUser(), this.bot.getConfiguration().getMysqlPassword());

            return true;
        }
        catch (Exception ex)
        {
            TSBot.LOGGER.log(Level.SEVERE, "Can't connect to the database!", ex);
            return false;
        }
    }

    public void subscribe()
    {
        this.subscriber = new Subscriber();
        new Thread(() -> {
            while (this.continueSub)
            {
                Jedis jedis = this.getConnection();
                try
                {
                    jedis.psubscribe(this.subscriber, "*");
                }
                catch (Exception e)
                {
                    e.printStackTrace();
                }

                TSBot.LOGGER.info("Disconnected from master.");
                jedis.close();
            }
        }).start();

        TSBot.LOGGER.info("Waiting for subscribing...");
        while (!this.subscriber.isSubscribed())
            try
            {
                Thread.sleep(100);
            }
            catch (InterruptedException e)
            {
                e.printStackTrace();
            }
        this.subscriber.registerReceiver(TSBot.CHANNEL, new TSReceiver(this.bot));
        TSBot.LOGGER.info("Correctly subscribed.");
    }

    public void disconnect()
    {
        this.continueSub = false;
    }

    public Jedis getConnection()
    {
        return this.cachePool.getResource();
    }

    public TeamSpeakLinkBean getLinkInfo(UUID uuid)
    {
        Statement statement = null;
        try
        {
            statement = this.connection.createStatement();
            ResultSet resultSet = statement.executeQuery("SELECT * FROM teamspeak_links WHERE uuid = (UNHEX('" + uuid.toString().replace("-", "") + "'))");
            if (resultSet.next())
                return new TeamSpeakLinkBean(uuid,
                        resultSet.getString(2),
                        resultSet.getTimestamp(3),
                        resultSet.getTimestamp(4),
                        resultSet.getTimestamp(5));
        }
        catch (Exception ex)
        {
            TSBot.LOGGER.log(Level.SEVERE, "Error while getting link for uuid " + uuid, ex);
        }
        finally
        {
            try
            {
                if (statement != null)
                    statement.close();
            }
            catch (SQLException ignored) {}
        }
        return null;
    }

    public TeamSpeakLinkBean getLinkInfo(String identity)
    {
        PreparedStatement statement = null;
        try
        {
            statement = this.connection.prepareStatement("SELECT (HEX(uuid)), identity, link_date, first_login, last_login FROM teamspeak_links WHERE identity LIKE ?");
            statement.setString(1, identity);
            ResultSet resultSet = statement.executeQuery();
            if (resultSet.next())
                return new TeamSpeakLinkBean(getUUID(resultSet.getString(1)),
                        resultSet.getString(2),
                        resultSet.getTimestamp(3),
                        resultSet.getTimestamp(4),
                        resultSet.getTimestamp(5));
        }
        catch (Exception ex)
        {
            TSBot.LOGGER.log(Level.SEVERE, "Error while getting link for identity " + identity, ex);
        }
        finally
        {
            try
            {
                if (statement != null)
                    statement.close();
            }
            catch (SQLException ignored) {}
        }
        return null;
    }

    public boolean removeLink(UUID uuid)
    {
        Statement statement = null;
        try
        {
            statement = this.connection.createStatement();
            return statement.execute("DELETE FROM teamspeak_links WHERE uuid=UNHEX(" + uuid.toString().replace("-", "") + ")");
        }
        catch (Exception ex)
        {
            TSBot.LOGGER.log(Level.SEVERE, "Error while removing link for uuid " + uuid, ex);
            return false;
        }
        finally
        {
            try
            {
                if (statement != null)
                    statement.close();
            }
            catch (SQLException ignored) {}
        }
    }

    public boolean addLink(TeamSpeakLinkBean teamSpeakLinkBean)
    {
        Statement statement = null;
        try
        {
            statement = this.connection.createStatement();
            return statement.execute("INSERT INTO teamspeak_links VALUES(UNHEX('" + teamSpeakLinkBean.getUuid().toString().replace("-", "") + "'), '" + teamSpeakLinkBean.getIdentity() + "', '" + teamSpeakLinkBean.getLinkDate() + "', '" + teamSpeakLinkBean.getFirstLogin() + "', '" + teamSpeakLinkBean.getLastLogin() + "')");
        }
        catch (Exception ex)
        {
            TSBot.LOGGER.log(Level.SEVERE, "Error while adding link for uuid " + teamSpeakLinkBean.getUuid(), ex);
            return false;
        }
        finally
        {
            try
            {
                if (statement != null)
                    statement.close();
            }
            catch (SQLException ignored) {}
        }
    }

    public int getRankForPlayer(UUID uuid)
    {
        PreparedStatement statement = null;
        try
        {
            statement = this.connection.prepareStatement("SELECT group_id FROM players WHERE uuid = UNHEX(?)");
            statement.setString(1, uuid.toString().replace("-", ""));
            ResultSet resultSet = statement.executeQuery();
            if (resultSet.next())
                return resultSet.getInt(1);
            return -1;
        }
        catch (Exception ex)
        {
            TSBot.LOGGER.log(Level.SEVERE, "Error while getting player rank from SG DB for uuid " + uuid, ex);
            return -1;
        }
        finally
        {
            try
            {
                if (statement != null)
                    statement.close();
            }
            catch (SQLException ignored) {}
        }
    }

    private static UUID getUUID(String uuid)
    {
        return UUID.fromString(uuid.toLowerCase().replaceAll("(\\w{8})(\\w{4})(\\w{4})(\\w{4})(\\w{12})", "$1-$2-$3-$4-$5"));
    }
}
