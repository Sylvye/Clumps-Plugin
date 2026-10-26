package net.abhinav.clumps;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.ExperienceOrb;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerExpChangeEvent;

import java.util.List;
import java.util.stream.Collectors;

public class XPCollectionListener implements Listener {

    private final Clumps plugin;

    public XPCollectionListener(Clumps plugin) {
        this.plugin = plugin;
    }

    public void register() {
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    @EventHandler
    public void onPlayerNearbyXPCollect(PlayerExpChangeEvent event) {
        if (!(event.getSource() instanceof ExperienceOrb sourceOrb)) return;

        Player player = event.getPlayer();
        World world = player.getWorld();
        Location playerLocation = player.getLocation();
        double instantCollectRadius = plugin.getInstantCollectRadius();

        // Only collect XP if radius is set to something greater than zero
        if (instantCollectRadius <= 0) return;

        // Find nearby orbs and collect their XP
        List<ExperienceOrb> nearbyOrbs = world.getNearbyEntities(playerLocation, instantCollectRadius, instantCollectRadius, instantCollectRadius)
                .stream()
                .filter(entity -> entity instanceof ExperienceOrb)
                .map(entity -> (ExperienceOrb) entity)
                .collect(Collectors.toList());

        int totalXP = event.getAmount();
        for (ExperienceOrb orb : nearbyOrbs) {
            // Paper handles the source orb after this event, including its XP award.
            if (orb.getUniqueId().equals(sourceOrb.getUniqueId()) || !orb.isValid() || orb.isDead()) continue;

            totalXP += orb.getExperience();
            orb.remove();
        }
        event.setAmount(totalXP);
    }
}
