package net.samagames.tsbot.commands;

import net.samagames.tsbot.TSBot;

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
public class ShutdownCommand extends AbstractCommand
{
    public ShutdownCommand(TSBot bot)
    {
        super(bot);
    }

    @Override
    public boolean run(String[] args)
    {
        this.bot.setEnd(true);
        return true;
    }
}
