package net.samagames.tsbot.redis;

import java.sql.Timestamp;
import java.util.UUID;

/**
 * Created by Rigner for project SamaBot.
 */
public class TeamSpeakLinkBean
{
    private UUID uuid;
    private String identity;
    private Timestamp link_date;
    private Timestamp first_login;
    private Timestamp last_login;

    public TeamSpeakLinkBean(UUID uuid, String identity, Timestamp link_date, Timestamp first_login, Timestamp last_login)
    {
        this.uuid = uuid;
        this.identity = identity;
        this.link_date = link_date;
        this.first_login = first_login;
        this.last_login = last_login;
    }

    public UUID getUuid()
    {
        return uuid;
    }

    public String getIdentity()
    {
        return identity;
    }

    public Timestamp getLinkDate()
    {
        return link_date;
    }

    public Timestamp getFirstLogin()
    {
        return first_login;
    }

    public Timestamp getLastLogin()
    {
        return last_login;
    }
}
