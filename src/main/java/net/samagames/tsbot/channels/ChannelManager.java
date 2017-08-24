package net.samagames.tsbot.channels;

import java.util.ArrayList;
import java.util.List;

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
public class ChannelManager
{
    private static int ids = 0;
    private List<BotChannel> channelList;

    public ChannelManager()
    {
        this.channelList = new ArrayList<>();
    }

    public List<BotChannel> getChannelList()
    {
        return channelList;
    }

    public BotChannel getChannel(int id)
    {
        return channelList.stream().filter(botChannel -> botChannel.getId() == id).findFirst().orElse(null);
    }

    public void removeChannel(BotChannel botChannel)
    {
        this.channelList.remove(botChannel);
    }

    public BotChannel createChannel(int realId)
    {
        BotChannel botChannel = new BotChannel(ids++, realId);
        this.channelList.add(botChannel);
        return botChannel;
    }
}
