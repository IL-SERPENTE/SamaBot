package net.samagames.tsbot.commands;

import net.samagames.tsbot.TSBot;

/**
 * Created by Rigner for project SamaBot.
 */
public class ShutdownCommand extends AbstractCommand
{
    public ShutdownCommand(TSBot bot)
    {
        super(bot);
    }

    @Override
    public boolean run(String[] args)
    {
        this.bot.setEnd(true);
        return true;
    }
}
