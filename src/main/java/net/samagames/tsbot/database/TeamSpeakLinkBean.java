package net.samagames.tsbot.database;

import java.sql.Timestamp;
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
