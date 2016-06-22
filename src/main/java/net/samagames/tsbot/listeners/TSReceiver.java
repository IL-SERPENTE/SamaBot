package net.samagames.tsbot.listeners;

import net.samagames.tsbot.TSBot;
import net.samagames.tsbot.commands.*;
import net.samagames.tsbot.database.IPacketsReceiver;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by Rigner for project TSBot.
 */
public class TSReceiver implements IPacketsReceiver
{
    private TSBot tsBot;
    private Map<String, AbstractCommand> commands;

    public TSReceiver(TSBot bot)
    {
        this.tsBot = bot;
        this.commands = new HashMap<>();

        this.commands.put("createchannel", new CreateChannelCommand(bot));
        this.commands.put("shutdown", new ShutdownCommand(bot));
        this.commands.put("deletechannel", new DeleteChannelCommand(bot));
        this.commands.put("link", new TSLinkCommand(bot));
        this.commands.put("update", new TSUpdateCommand(bot));
        this.commands.put("move", new MoveCommand(bot));
        this.commands.put("clientchannelpermission", new ChangeClientPermissionCommand(bot));
        this.commands.put("linked", new LinkedCommand(bot));
        //TODO
    }

    @Override
    public void receive(String channel, String message)
    {
        String args[] = message.split(":");
        if (args.length < 2)
            return ;
        /*switch (args[1])
        {
            case "privatemessage":
            case "channelmessage":
                break ;
            default:
        }*/
        AbstractCommand command = this.commands.get(args[1]);
        if (command == null || !command.run(args))
        {
            TSBot.LOGGER.warning("Receiving incorrect order from " + args[0] + " : \"" + args[1] + "\" (args : \"" + String.join(":", args) + "\")");
            this.tsBot.getPubsub().respondError(args[0]);
        }
    }
}
