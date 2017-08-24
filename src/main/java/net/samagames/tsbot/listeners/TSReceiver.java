package net.samagames.tsbot.listeners;

import net.samagames.tsbot.TSBot;
import net.samagames.tsbot.commands.*;
import net.samagames.tsbot.database.IPacketsReceiver;

import java.util.HashMap;
import java.util.Map;

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
        this.commands.put("unlink", new TSUnlinkCommand(bot));
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
