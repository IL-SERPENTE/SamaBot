package net.samagames.tsbot.commands;

import net.samagames.tsbot.TSBot;

/**
 * Created by Rigner for project SamaBot.
 */
public abstract class AbstractCommand
{
    protected TSBot bot;

    protected AbstractCommand(TSBot bot)
    {
        this.bot = bot;
    }

    public abstract boolean run(String[] args);
}
