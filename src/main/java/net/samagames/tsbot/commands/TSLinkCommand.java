package net.samagames.tsbot.commands;

import com.github.theholywaffle.teamspeak3.api.wrapper.Client;
import net.samagames.tsbot.TSBot;
import net.samagames.tsbot.TSConfiguration;
import net.samagames.tsbot.database.TeamSpeakLinkBean;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
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
                this.bot.getPubsub().respondError(args[0], "ALREADY_LINKED");
                return true;
            }

            Client client = null;
            try
            {
                client = this.bot.getTs3Api().getClientByUId(args[3]);
            } catch (Exception ignored) {}
            if (client == null)
            {
                this.bot.getPubsub().respondError(args[0], "INVALID_IDENTITY");
                return true;
            }

            this.bot.getDatabaseConnector().addLink(new TeamSpeakLinkBean(uuid, args[3], Timestamp.from(Instant.now()), new Timestamp(client.getCreatedDate().getTime()), new Timestamp(client.getLastConnectedDate().getTime())));

            TSLinkCommand.updateRankForPlayer(this.bot, uuid, client);
            this.bot.getPubsub().respond(args[0], "LINK_OK");
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
        int rank = uuid == null ? -1 : tsBot.getDatabaseConnector().getRankForPlayer(uuid);

        TSBot.LOGGER.info("Updating ranks for player " + uuid + " (rank = " + rank + ", clid = " + client.getId() + ", dbid = " + client.getDatabaseId() + ", removemode = " + removeOnly + ")");
        List<TSConfiguration.RankPair> ranks = tsBot.getConfiguration().getRanks();
        boolean[][] rankTab = new boolean[ranks.size()][2];
        for (int i = 0; i < ranks.size(); i++)
        {
            rankTab[i][0] = ranks.get(i).getMinecraftRankIds().contains(rank);
            rankTab[i][1] = false;
        }

        boolean removed = false;
        for (int group : client.getServerGroups())
        {
            for (int i = 0; i < ranks.size(); i++)
                if (ranks.get(i).getTeamspeakRankId() == group)
                {
                    if (removeOnly || !rankTab[i][0])
                    {
                        tsBot.getTs3Api().removeClientFromServerGroup(ranks.get(i).getTeamspeakRankId(), client.getDatabaseId());
                        removed = true;
                    }
                    else
                        rankTab[i][1] = true;
                }
        }

        if (removed && uuid == null)
            tsBot.getTs3Api().sendPrivateMessage(client.getId(), "Vous possédez un grade réservé aux joueurs ayant liés leur compte Minecraft et TeamSpeak. Il vous a donc été retiré. Pour le récupérer sur cette identité, veuillez taper la commande suivante en jeu : /link ts " + client.getUniqueIdentifier());

        if (!removeOnly)
            for (int i = 0; i < ranks.size(); i++)
                if (rankTab[i][0] && !rankTab[i][1])
                    tsBot.getTs3Api().addClientToServerGroup(ranks.get(i).getTeamspeakRankId(), client.getDatabaseId());
    }
}