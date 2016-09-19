package net.samagames.tsbot.listeners;

import com.github.theholywaffle.teamspeak3.api.event.*;
import com.github.theholywaffle.teamspeak3.api.wrapper.ClientInfo;
import net.samagames.tsbot.TSBot;
import net.samagames.tsbot.commands.TSLinkCommand;
import net.samagames.tsbot.database.TeamSpeakLinkBean;

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
            TSLinkCommand.updateRankForPlayer(this.tsBot, bean == null ? null : bean.getUuid(), client);
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
