package net.samagames.tsbot;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.stream.JsonReader;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;

/**
 * Created by Rigner for project TSBot.
 */
public class TSConfiguration
{
    private String redisIp;
    private short redisPort;
    private String redisPassword;

    private String teamspeakIp;
    private short teamspeakPort;
    private String teamspeakUser;
    private String teamspeakPassword;

    private String mysqlIp;
    private String mysqlUser;
    private String mysqlPassword;
    private String mysqlDatabase;

    private int defaultChannel;
    private List<RankPair> ranks;

    public boolean reload()
    {
        try {
            File file = new File("config.json");
            if (!file.exists())
            {
                File file2 = new File("config.json.example");
                if (file2.exists())
                    if (!file2.renameTo(file))
                        throw new FileNotFoundException();
            }
            JsonObject rootJson = new JsonParser().parse(new JsonReader(new FileReader(file))).getAsJsonObject();

            String newRedisIp = rootJson.get("redis-ip").getAsString();
            short newRedisPort = rootJson.get("redis-port").getAsShort();
            String newRedisPassword = rootJson.get("redis-password").getAsString();
            String teamspeakIp = rootJson.get("teamspeak-ip").getAsString();
            short teamspeakPort = rootJson.get("teamspeak-port").getAsShort();
            String teamspeakUser = rootJson.get("teamspeak-user").getAsString();
            String teamspeakPassword = rootJson.get("teamspeak-password").getAsString();

            String mysqlIp = rootJson.get("mysql-ip").getAsString();
            String mysqlUser = rootJson.get("mysql-user").getAsString();
            String mysqlPassword = rootJson.get("mysql-password").getAsString();
            String mysqlDatabase = rootJson.get("mysql-database").getAsString();

            int defaultChannel = rootJson.get("default-channel").getAsInt();
            List<RankPair> ranks = new ArrayList<>();
            rootJson.get("ranks").getAsJsonArray().forEach(element ->
            {
                String[] strings = element.getAsString().split(", ");
                ranks.add(new RankPair(Integer.parseInt(strings[0]), Integer.parseInt(strings[1])));
            });

            this.redisIp = newRedisIp;
            this.redisPort = newRedisPort;
            this.redisPassword = newRedisPassword;
            this.teamspeakIp = teamspeakIp;
            this.teamspeakPort = teamspeakPort;
            this.teamspeakUser = teamspeakUser;
            this.teamspeakPassword = teamspeakPassword;
            this.mysqlIp = mysqlIp;
            this.mysqlUser = mysqlUser;
            this.mysqlPassword = mysqlPassword;
            this.mysqlDatabase = mysqlDatabase;
            this.defaultChannel = defaultChannel;
            this.ranks = ranks;

            TSBot.LOGGER.info("Configuration successfully loaded");

            return true;
        } catch (FileNotFoundException ex) {
            TSBot.LOGGER.log(Level.SEVERE, "Could not find config file", ex);
        } catch (Exception ex) {
            TSBot.LOGGER.log(Level.SEVERE, "Error loading config file", ex);
        }
        return false;
    }

    public int getRedisPort()
    {
        return redisPort;
    }

    public String getRedisIp()
    {
        return redisIp;
    }

    public String getRedisPassword()
    {
        return redisPassword;
    }

    public String getTeamspeakIp()
    {
        return teamspeakIp;
    }

    public short getTeamspeakPort()
    {
        return teamspeakPort;
    }

    public String getTeamspeakUser()
    {
        return teamspeakUser;
    }

    public String getTeamspeakPassword()
    {
        return teamspeakPassword;
    }

    public String getMysqlDatabase()
    {
        return mysqlDatabase;
    }

    public String getMysqlIp()
    {
        return mysqlIp;
    }

    public String getMysqlUser()
    {
        return mysqlUser;
    }

    public String getMysqlPassword()
    {
        return mysqlPassword;
    }

    public int getDefaultChannel()
    {
        return defaultChannel;
    }

    public List<RankPair> getRanks()
    {
        return ranks;
    }

    public static class RankPair
    {
        private int teamspeakRankId;
        private int minecraftRankId;

        public RankPair(int teamspeakRankId, int minecraftRankId)
        {
            this.teamspeakRankId = teamspeakRankId;
            this.minecraftRankId = minecraftRankId;
        }

        public int getMinecraftRankId()
        {
            return this.minecraftRankId;
        }

        public int getTeamspeakRankId()
        {
            return this.teamspeakRankId;
        }
    }
}
