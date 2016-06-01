package net.samagames.tsbot.redis;

/**
 * Created by Rigner for project TSBot.
 */
public interface IPatternReceiver
{
    void receive(String pattern, String channel, String message);
}
