package net.samagames.tsbot.commands;

import com.github.theholywaffle.teamspeak3.api.ChannelProperty;
import net.samagames.tsbot.TSBot;
import net.samagames.tsbot.channels.BotChannel;

import java.util.HashMap;
import java.util.Map;

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
public class CreateChannelCommand extends AbstractCommand
{
    public CreateChannelCommand(TSBot bot)
    {
        super(bot);
    }

    @Override
    public boolean run(String[] args)
    {
        if (args.length < 3)
            return false;
        Map<ChannelProperty, String> properties = new HashMap<>();
        try
        {
            for (int i = 3; i < args.length; i++)
                if (args[i].contains("="))
                {
                    String[] split = args[i].split("=");
                    properties.put(ChannelProperty.valueOf(split[0]), split[1]);
                }
            int channelId = bot.getTs3Api().createChannel(args[2], properties);
            if (channelId == -1)
            {
                this.bot.getPubsub().respondError(args[0], "UNKNOWN");
                return true;
            }
            for (int i = 3; i < args.length; i++)
                if (args[i].contains("-"))
                {
                    String[] split = args[i].split("-");
                    this.bot.getTs3Api().addChannelPermission(channelId, split[0], Integer.parseInt(split[1]));
                }
            this.bot.getTs3Api().moveQuery(this.bot.getConfiguration().getDefaultChannel());

            BotChannel botChannel = this.bot.getChannelManager().createChannel(channelId);
            this.bot.getPubsub().respond(args[0], String.valueOf(botChannel.getId()));
            return true;
        }
        catch (Exception ignored)
        {
            this.bot.getPubsub().respondError(args[0], ignored.getMessage());
            return true;
        }
    }
}
