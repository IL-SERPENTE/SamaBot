package net.samagames.tsbot.commands;

import com.github.theholywaffle.teamspeak3.api.wrapper.Client;
import net.samagames.tsbot.TSBot;
import net.samagames.tsbot.channels.BotChannel;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Created by Rigner for project SamaBot.
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
