package com.outlaw.bounties.model;

import org.bukkit.Location;
import java.util.UUID;

public class ActiveBounty {
    public enum State { STARTED, KILLED, CLAIMED }

    public String bountyId;
    public UUID playerUUID;
    public UUID mobUUID;
    public Location mobLocation;
    public Location npcLocation;
    public State state = State.STARTED;
}
