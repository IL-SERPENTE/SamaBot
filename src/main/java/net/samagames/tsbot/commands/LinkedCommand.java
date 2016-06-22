package net.samagames.tsbot.commands;

import net.samagames.tsbot.TSBot;

import java.util.UUID;

/**
 * Created by Rigner for project SamaBot.
 */
public class LinkedCommand extends AbstractCommand
{
    public LinkedCommand(TSBot tsBot)
    {
        super(tsBot);
    }

    @Override
    public boolean run(String[] args)
    {
        if (args.length != 2)
            return false;
        try
        {
            UUID uuid = UUID.fromString(args[1]);
            this.bot.getPubsub().respond(args[0], String.valueOf(this.bot.getDatabaseConnector().getLinkInfo(uuid) != null));
        }
        catch (Exception ignored)
        {
            this.bot.getPubsub().respondError(args[0], ignored.getMessage());
        }
        return true;
    }
}
