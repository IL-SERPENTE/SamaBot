package net.samagames.tsbot.redis;

/**
 * Created by Rigner for project TSBot.
 */
public interface IPacketsReceiver
{
    void receive(String channel, String message);
}
