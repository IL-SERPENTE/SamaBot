package net.samagames.tsbot.listeners;

import com.github.theholywaffle.teamspeak3.api.event.*;
import com.github.theholywaffle.teamspeak3.api.wrapper.ClientInfo;
import net.samagames.tsbot.TSBot;
import net.samagames.tsbot.commands.TSLinkCommand;
import net.samagames.tsbot.database.TeamSpeakLinkBean;

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
