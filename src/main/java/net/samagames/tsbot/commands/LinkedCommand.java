package net.samagames.tsbot.commands;

import net.samagames.tsbot.TSBot;

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
public class LinkedCommand extends AbstractCommand
{
    public LinkedCommand(TSBot tsBot)
    {
        super(tsBot);
    }

    @Override
    public boolean run(String[] args)
    {
        if (args.length != 2)
            return false;
        try
        {
            UUID uuid = UUID.fromString(args[1]);
            this.bot.getPubsub().respond(args[0], String.valueOf(this.bot.getDatabaseConnector().getLinkInfo(uuid) != null));
        }
        catch (Exception ignored)
        {
            this.bot.getPubsub().respondError(args[0], ignored.getMessage());
        }
        return true;
    }
}
