package net.samagames.tsbot.database;

/**
 * Created by Rigner for project TSBot.
 */
public interface IPacketsReceiver
{
    void receive(String channel, String message);
}
