package net.samagames.tsbot.commands;

import com.github.theholywaffle.teamspeak3.api.wrapper.Client;
import net.samagames.tsbot.TSBot;
import net.samagames.tsbot.channels.BotChannel;
import net.samagames.tsbot.database.TeamSpeakLinkBean;

import java.util.*;

/**
 * Created by Rigner for project SamaBot.
 */
public class ChangeClientPermissionCommand extends AbstractCommand
{
    public ChangeClientPermissionCommand(TSBot tsBot)
    {
        super(tsBot);
    }

    @Override
    public boolean run(String[] args)
    {
        if (args.length < 4)
            return false;
        try
        {
            int channelId = Integer.parseInt(args[2]);
            BotChannel channel = this.bot.getChannelManager().getChannel(channelId);
            if (channel == null)
            {
                this.bot.getPubsub().respondError(args[0], "UNKNOWN");
                return true;
            }
            Map<String, Integer> permissions = new HashMap<>();
            List<TeamSpeakLinkBean> beans = new ArrayList<>();

            for (int i = 3; i < args.length; i++)
            {
                if (args[i].contains("="))
                {
                    String[] split = args[i].split("=");
                    permissions.put(split[0], Integer.parseInt(split[1]));
                }
                else
                {
                    TeamSpeakLinkBean bean = this.bot.getDatabaseConnector().getLinkInfo(UUID.fromString(args[i]));
                    if (bean != null)
                        beans.add(bean);
                }
            }
            List<UUID> results = new ArrayList<>();
            beans.forEach(bean -> results.add(bean.getUuid()));
            beans.forEach(bean -> {
                Client client = this.bot.getTs3Api().getClientByUId(bean.getIdentity());
                if (client == null)
                {
                    results.remove(bean.getUuid());
                    return ;
                }
                permissions.forEach((perm, level) -> this.bot.getTs3Api().addChannelClientPermission(channel.getRealId(), client.getDatabaseId(), perm, level));
            });

            String str = "";
            for (int i = 0; i < results.size(); i++)
                str += results.get(i) + (i == results.size() - 1 ? "" : ":");
            this.bot.getPubsub().respond(args[0], str);
        }
        catch (Exception ignored)
        {
            this.bot.getPubsub().respondError(args[0], ignored.getMessage());
        }
        return true;
    }
}
