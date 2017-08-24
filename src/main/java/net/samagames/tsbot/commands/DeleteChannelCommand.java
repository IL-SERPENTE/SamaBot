package net.samagames.tsbot.commands;

import com.github.theholywaffle.teamspeak3.api.wrapper.Client;
import net.samagames.tsbot.TSBot;
import net.samagames.tsbot.channels.BotChannel;

import java.util.List;
import java.util.stream.Collectors;

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
public class DeleteChannelCommand extends AbstractCommand
{
    public DeleteChannelCommand(TSBot tsBot)
    {
        super(tsBot);
    }

    @Override
    public boolean run(String[] args)
    {
        if (args.length < 3)
            return false;
        try
        {
            int channelId = Integer.parseInt(args[2]);
            BotChannel botChannel = this.bot.getChannelManager().getChannel(channelId);
            if (botChannel == null)
            {
                this.bot.getPubsub().respondError(args[0], "UNKNOWN");
                return true;
            }

            List<Client> clients = this.bot.getTs3Api().getClients().stream().filter(client -> client.getChannelId() == botChannel.getId()).collect(Collectors.toList());
            int[] clientIds = new int[clients.size()];
            int i = 0;
            for (Client client : clients)
                clientIds[i++] = client.getChannelId();
            this.bot.getTs3Api().moveClients(clientIds, this.bot.getConfiguration().getDefaultChannel());

            if (!this.bot.getTs3Api().deleteChannel(botChannel.getRealId()))
            {
                this.bot.getPubsub().respondError(args[0], "UNKNOWN");
                return true;
            }
            this.bot.getChannelManager().removeChannel(botChannel);
            this.bot.getPubsub().respond(args[0], "OK");
            return true;
        }
        catch (Exception ignored)
        {
            this.bot.getPubsub().respondError(args[0], ignored.getMessage());
            return true;
        }
    }
}
