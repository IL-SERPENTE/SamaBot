package net.samagames.tsbot.redis;

import net.samagames.tsbot.TSBot;
import redis.clients.jedis.JedisPubSub;

import java.util.HashMap;
import java.util.HashSet;
import java.util.logging.Level;

/**
 * This file is a part of the SamaGames project
 * This code is absolutely confidential.
 * Created by .....
 * (C) Copyright Elydra Network 2015
 * All rights reserved.
 */
class Subscriber extends JedisPubSub
{
    private final HashMap<String, HashSet<IPacketsReceiver>> packetsReceivers = new HashMap<>();
    private final HashMap<String, HashSet<IPatternReceiver>> patternsReceivers = new HashMap<>();

    public void registerReceiver(String channel, IPacketsReceiver receiver)
    {
        HashSet<IPacketsReceiver> receivers = packetsReceivers.get(channel);
        if (receivers == null)
            receivers = new HashSet<>();
        receivers.add(receiver);
        this.subscribe(channel);
        packetsReceivers.put(channel, receivers);
    }

    public void registerPattern(String pattern, IPatternReceiver receiver)
    {
        HashSet<IPatternReceiver> receivers = patternsReceivers.get(pattern);
        if (receivers == null)
            receivers = new HashSet<>();
        receivers.add(receiver);
        this.psubscribe(pattern);
        patternsReceivers.put(pattern, receivers);
    }

    @Override
    public void onMessage(String channel, String message)
    {
        try
        {
            HashSet<IPacketsReceiver> receivers = packetsReceivers.get(channel);
            if (receivers != null)
                receivers.forEach((IPacketsReceiver receiver) -> receiver.receive(channel, message));
            else
                TSBot.LOGGER.log(Level.WARNING, "{PubSub} Received message on a channel, but no packetsReceivers were found. (channel: " + channel + ", message: " + message + ")");
        } catch (Exception ignored)
        {
            ignored.printStackTrace();
        }

    }

    @Override
    public void onPMessage(String pattern, String channel, String message)
    {
        try
        {
            HashSet<IPatternReceiver> receivers = patternsReceivers.get(pattern);
            if (receivers != null)
                receivers.forEach((IPatternReceiver receiver) -> receiver.receive(pattern, channel, message));
        } catch (Exception ignored)
        {
            ignored.printStackTrace();
        }
    }
}
