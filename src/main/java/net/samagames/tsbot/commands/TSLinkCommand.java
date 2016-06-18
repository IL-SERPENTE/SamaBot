package net.samagames.tsbot.commands;

import com.github.theholywaffle.teamspeak3.api.wrapper.Client;
import net.samagames.tsbot.TSBot;
import net.samagames.tsbot.database.TeamSpeakLinkBean;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.UUID;

/**
 * Created by Rigner for project SamaBot.
 */
public class TSLinkCommand extends AbstractCommand
{
    public TSLinkCommand(TSBot tsBot)
    {
        super(tsBot);
    }

    @Override
    public boolean run(String[] args)
    {
        if (args.length != 4)
            return false;
        try
        {
            TeamSpeakLinkBean bean = this.bot.getDatabaseConnector().getLinkInfo(args[3]);
            if (bean != null)
            {
                this.bot.getPubsub().respondError(args[0], "ALREADY_LINKED");
                return true;
            }
            UUID uuid = UUID.fromString(args[2]);
            bean = this.bot.getDatabaseConnector().getLinkInfo(uuid);
            if (bean != null)
            {
                Client client = this.bot.getTs3Api().getClientByUId(bean.getIdentity());
                if (client != null)
                    TSLinkCommand.updateRankForPlayer(this.bot, uuid, client, true);
                if (!this.bot.getDatabaseConnector().removeLink(uuid))
                {
                    this.bot.getPubsub().respondError(args[0], "UNKNOWN");
                    return true;
                }
            }

            Client client = this.bot.getTs3Api().getClientByUId(args[3]);
            if (client == null)
            {
                this.bot.getPubsub().respondError(args[0], "INVALID_IDENTITY");
                return true;
            }

            this.bot.getDatabaseConnector().addLink(new TeamSpeakLinkBean(uuid, args[3], Timestamp.from(Instant.now()), new Timestamp(client.getCreatedDate().getTime()), new Timestamp(client.getLastConnectedDate().getTime())));

            TSLinkCommand.updateRankForPlayer(this.bot, uuid, client);
            this.bot.getPubsub().respond(args[0], "OK");
        }
        catch (Exception ignored)
        {
            this.bot.getPubsub().respondError(args[0], ignored.getMessage());
        }
        return true;
    }

    public static void updateRankForPlayer(TSBot tsBot, UUID uuid, Client client)
    {
        TSLinkCommand.updateRankForPlayer(tsBot, uuid, client, false);
    }

    public static void updateRankForPlayer(TSBot tsBot, UUID uuid, Client client, boolean removeOnly)
    {
        int rank = tsBot.getDatabaseConnector().getRankForPlayer(uuid);
        if (rank == -1)
            return ;
        boolean[] vip = {(rank == tsBot.getConfiguration().getVipRank()), false};
        boolean[] vipPlus = {(rank == tsBot.getConfiguration().getVipPlusRank()), false};
        for (int group : client.getServerGroups())
        {
            if (group == tsBot.getConfiguration().getTeamspeakVipRank())
            {
                if (removeOnly || !vip[0])
                    tsBot.getTs3Api().removeClientFromServerGroup(tsBot.getConfiguration().getTeamspeakVipRank(), client.getDatabaseId());
                else
                    vip[1] = true;
            }
            if (group == tsBot.getConfiguration().getTeamspeakVipPlusRank())
            {
                if (removeOnly || !vipPlus[0])
                    tsBot.getTs3Api().removeClientFromServerGroup(tsBot.getConfiguration().getTeamspeakVipPlusRank(), client.getDatabaseId());
                else
                    vipPlus[1] = true;
            }
        }
        if (vip[0] && !vip[1] && !removeOnly)
            tsBot.getTs3Api().addClientToServerGroup(tsBot.getConfiguration().getTeamspeakVipRank(), client.getDatabaseId());
        if (vipPlus[0] && !vipPlus[1] && !removeOnly)
            tsBot.getTs3Api().addClientToServerGroup(tsBot.getConfiguration().getTeamspeakVipPlusRank(), client.getDatabaseId());
    }
}