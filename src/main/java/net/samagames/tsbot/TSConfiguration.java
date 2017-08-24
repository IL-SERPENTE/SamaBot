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
                List<Integer> ids = new ArrayList<>();
                for (int i = 1; i < strings.length; i++)
                    ids.add(Integer.parseInt(strings[i]));
                ranks.add(new RankPair(Integer.parseInt(strings[0]), ids));
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
        private List<Integer> minecraftRankIds;

        public RankPair(int teamspeakRankId, List<Integer> minecraftRankIds)
        {
            this.teamspeakRankId = teamspeakRankId;
            this.minecraftRankIds = minecraftRankIds;
        }

        public List<Integer> getMinecraftRankIds()
        {
            return this.minecraftRankIds;
        }

        public int getTeamspeakRankId()
        {
            return this.teamspeakRankId;
        }
    }
}
