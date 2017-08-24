package net.samagames.tsbot.channels;

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
public class BotChannel
{
    private int id;
    private int realId;

    public BotChannel(int id, int realId)
    {
        this.id = id;
        this.realId = realId;
    }

    public int getId()
    {
        return id;
    }

    public int getRealId()
    {
        return realId;
    }
}
