package net.skeagle.manhunt.vote;

import org.bukkit.entity.Player;

import java.util.*;
import java.util.stream.Collectors;

public class MHVoteManager {

    private final Map<UUID, MHVote> votes;

    public MHVoteManager() {
        votes = new HashMap<>();
    }

    public long voteCount(MHVote vote) {
        return votes.values().stream().filter(vote::equals).count();
    }

    public void setVote(Player player, MHVote vote) {
        votes.put(player.getUniqueId(), vote);
    }

    public List<UUID> getPlayersWithVote(MHVote vote) {
        return votes.keySet().stream().filter(p -> vote == votes.get(p)).collect(Collectors.toList());
    }

    public boolean hasVote(Player player) {
        return votes.containsKey(player.getUniqueId());
    }

    public void clear() {
        votes.clear();
    }
}
