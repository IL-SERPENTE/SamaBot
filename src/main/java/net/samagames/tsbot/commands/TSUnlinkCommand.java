package net.samagames.tsbot.commands;

import com.github.theholywaffle.teamspeak3.api.wrapper.Client;
import net.samagames.tsbot.TSBot;
import net.samagames.tsbot.database.TeamSpeakLinkBean;

import java.util.UUID;

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
public class TSUnlinkCommand extends AbstractCommand
{
    public TSUnlinkCommand(TSBot tsBot)
    {
        super(tsBot);
    }

    @Override
    public boolean run(String[] args)
    {
        if (args.length != 3)
            return false;
        try
        {
            UUID uuid = UUID.fromString(args[2]);
            TeamSpeakLinkBean bean = this.bot.getDatabaseConnector().getLinkInfo(uuid);
            if (bean == null)
            {
                this.bot.getPubsub().respondError(args[0], "NOT_LINKED");
                return true;
            }
            Client client = null;
            try
            {
                client = this.bot.getTs3Api().getClientByUId(bean.getIdentity());
            } catch (Exception ignored) {}
            if (!this.bot.getDatabaseConnector().removeLink(uuid))
            {
                this.bot.getPubsub().respondError(args[0], "UNKNOWN");
                return true;
            }
            if (client != null)
                TSLinkCommand.updateRankForPlayer(this.bot, uuid, client, true);
            this.bot.getPubsub().respond(args[0], "UNLINK_OK");
            return true;
        }
        catch (Exception ignored)
        {
            this.bot.getPubsub().respondError(args[0], ignored.getMessage());
        }
        return true;
    }
}
