package net.samagames.tsbot.commands;

import com.github.theholywaffle.teamspeak3.api.wrapper.Client;
import net.samagames.tsbot.TSBot;
import net.samagames.tsbot.channels.BotChannel;
import net.samagames.tsbot.database.TeamSpeakLinkBean;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.logging.Level;

/**
 * Created by Rigner for project SamaBot.
 */
public class MoveCommand extends AbstractCommand
{
    public MoveCommand(TSBot tsBot)
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
            List<UUID> result = new ArrayList<>();
            BotChannel channel = this.bot.getChannelManager().getChannel(Integer.parseInt(args[2]));
            if (channel == null)
            {
                this.bot.getPubsub().respondError(args[0], "UNKNOWN");
                return true;
            }
            for (int i = 3; i < args.length; i++)
            {
                UUID uuid = UUID.fromString(args[i]);
                TeamSpeakLinkBean bean = this.bot.getDatabaseConnector().getLinkInfo(uuid);
                if (bean == null)
                    continue ;
                Client client = this.bot.getTs3Api().getClientByUId(bean.getIdentity());
                if (client != null && this.bot.getTs3Api().moveClient(client.getId(), channel.getRealId()))
                    result.add(uuid);
            }

            String str = "";
            for (int i = 0; i < result.size(); i++)
                str += result.get(i) + (i == result.size() - 1 ? "" : ":");
            this.bot.getPubsub().respond(args[0], str);
        }
        catch (Exception ignored)
        {
            this.bot.getPubsub().respondError(args[0], ignored.getMessage());
        }
        return true;
    }
}
