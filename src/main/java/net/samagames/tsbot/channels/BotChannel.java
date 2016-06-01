package net.samagames.tsbot.channels;

/**
 * Created by Rigner for project SamaBot.
 */
public class BotChannel
{
    private int id;
    private int realId;

    public BotChannel(int id, int realId)
    {
        this.id = id;
        this.realId = realId;
    }

    public int getId()
    {
        return id;
    }

    public int getRealId()
    {
        return realId;
    }
}
