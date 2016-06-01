package net.samagames.tsbot;

import com.github.theholywaffle.teamspeak3.api.event.*;
import com.github.theholywaffle.teamspeak3.api.wrapper.ClientInfo;
import net.samagames.tsbot.commands.TSLinkCommand;
import net.samagames.tsbot.redis.TeamSpeakLinkBean;

/**
 * Created by Rigner for project SamaBot.
 */
public class TSListener implements TS3Listener
{
    private TSBot tsBot;

    public TSListener(TSBot tsBot)
    {
        this.tsBot = tsBot;
    }

    @Override
    public void onClientJoin(ClientJoinEvent e)
    {
        new Thread(() ->
        {
            TeamSpeakLinkBean bean = this.tsBot.getDatabaseConnector().getLinkInfo(e.getUniqueClientIdentifier());
            ClientInfo client = this.tsBot.getTs3Api().getClientInfo(e.getClientId());
            if (client == null)
                return ;
            if (bean == null)
            {
                for (int group : client.getServerGroups())
                {
                    if (group == this.tsBot.getConfiguration().getTeamspeakVipRank())
                        this.tsBot.getTs3Api().removeClientFromServerGroup(this.tsBot.getConfiguration().getTeamspeakVipRank(), client.getDatabaseId());
                    else if (group == this.tsBot.getConfiguration().getTeamspeakVipPlusRank())
                        this.tsBot.getTs3Api().removeClientFromServerGroup(this.tsBot.getConfiguration().getTeamspeakVipPlusRank(), client.getDatabaseId());
                }
                return ;
            }
            TSLinkCommand.updateRankForPlayer(this.tsBot, bean.getUuid(), client);
        }).start();
    }

    @Override
    public void onClientLeave(ClientLeaveEvent e)
    {
    }

    @Override
    public void onChannelCreate(ChannelCreateEvent e)
    {
    }

    @Override
    public void onChannelDeleted(ChannelDeletedEvent e)
    {
    }

    @Override
    public void onChannelDescriptionChanged(ChannelDescriptionEditedEvent e)
    {
    }

    @Override
    public void onChannelEdit(ChannelEditedEvent e)
    {
    }

    @Override
    public void onChannelMoved(ChannelMovedEvent e)
    {
    }

    @Override
    public void onChannelPasswordChanged(ChannelPasswordChangedEvent e)
    {
    }

    @Override
    public void onClientMoved(ClientMovedEvent e)
    {
    }

    @Override
    public void onPrivilegeKeyUsed(PrivilegeKeyUsedEvent e)
    {
    }

    @Override
    public void onServerEdit(ServerEditedEvent e)
    {
    }

    @Override
    public void onTextMessage(TextMessageEvent e)
    {
    }
}
