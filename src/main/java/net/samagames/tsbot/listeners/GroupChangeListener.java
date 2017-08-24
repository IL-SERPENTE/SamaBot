package net.samagames.tsbot.listeners;

import com.github.theholywaffle.teamspeak3.api.wrapper.Client;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.samagames.tsbot.TSBot;
import net.samagames.tsbot.commands.TSLinkCommand;
import net.samagames.tsbot.database.IPacketsReceiver;
import net.samagames.tsbot.database.TeamSpeakLinkBean;

import java.util.UUID;
import java.util.logging.Level;

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
public class GroupChangeListener implements IPacketsReceiver
{
    private TSBot tsBot;
    private Gson gson;

    public GroupChangeListener(TSBot tsBot)
    {
        this.tsBot = tsBot;
        this.gson = new GsonBuilder().create();
    }

    @Override
    public void receive(String channel, String message)
    {
        try
        {
            GroupChangePacket packetObj = gson.fromJson(message, GroupChangePacket.class);
            if (packetObj == null || packetObj.playerUUID == null)
                return ;
            TeamSpeakLinkBean bean = this.tsBot.getDatabaseConnector().getLinkInfo(packetObj.playerUUID);
            if (bean == null)
                return ;
            Client client = this.tsBot.getTs3Api().getClientByUId(bean.getIdentity());
            if (client != null)
                TSLinkCommand.updateRankForPlayer(this.tsBot, packetObj.playerUUID, client);
        }
        catch (Exception ex)
        {
            TSBot.LOGGER.log(Level.SEVERE, "Error changing player rank after group change (message : " + message + ")", ex);
        }
    }

    @SuppressWarnings("unused")
    private class GroupChangePacket
    {
        private String type;
        private UUID playerUUID;
        private String target;
    }
}
