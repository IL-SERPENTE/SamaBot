package net.samagames.tsbot.database;

/**
 * Created by Rigner for project TSBot.
 */
public interface IPatternReceiver
{
    void receive(String pattern, String channel, String message);
}
