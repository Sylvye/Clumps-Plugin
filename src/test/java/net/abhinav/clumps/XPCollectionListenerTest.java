package net.abhinav.clumps;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.ExperienceOrb;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerExpChangeEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

class XPCollectionListenerTest {
    private Clumps plugin;
    private Player player;
    private World world;
    private Location location;
    private XPCollectionListener listener;

    @BeforeEach
    void setUp() {
        plugin = mock(Clumps.class);
        player = mock(Player.class);
        world = mock(World.class);
        location = new Location(world, 0, 64, 0);
        when(plugin.getInstantCollectRadius()).thenReturn(1.5);
        when(player.getWorld()).thenReturn(world);
        when(player.getLocation()).thenReturn(location);
        listener = new XPCollectionListener(plugin);
    }

    private ExperienceOrb orb(int xp) {
        ExperienceOrb orb = mock(ExperienceOrb.class);
        when(orb.getUniqueId()).thenReturn(UUID.randomUUID());
        when(orb.isValid()).thenReturn(true);
        when(orb.getExperience()).thenReturn(xp);
        return orb;
    }

    private void nearby(Entity... entities) {
        when(world.getNearbyEntities(location, 1.5, 1.5, 1.5)).thenReturn(List.of(entities));
    }

    @Test
    void sourceOrbIsAwardedOnlyByPaper() {
        ExperienceOrb source = orb(7);
        nearby(source);
        PlayerExpChangeEvent event = new PlayerExpChangeEvent(player, source, 7);
        listener.onPlayerNearbyXPCollect(event);
        assertEquals(7, event.getAmount());
        verify(source, never()).remove();
        verify(player, never()).giveExp(anyInt());
    }

    @Test
    void addsNearbyXPOnceAndPreservesModifiedEventAmount() {
        ExperienceOrb source = orb(7);
        ExperienceOrb sameSource = orb(7);
        UUID sourceId = source.getUniqueId();
        when(sameSource.getUniqueId()).thenReturn(sourceId);
        ExperienceOrb first = orb(3);
        ExperienceOrb second = orb(5);
        nearby(source, sameSource, first, second, mock(Entity.class));
        PlayerExpChangeEvent event = new PlayerExpChangeEvent(player, source, 14);
        listener.onPlayerNearbyXPCollect(event);
        assertEquals(22, event.getAmount());
        verify(first).remove();
        verify(second).remove();
        verify(source, never()).remove();
        verify(sameSource, never()).remove();
        verify(player, never()).giveExp(anyInt());
    }

    @Test
    void ignoresInvalidAndDeadOrbs() {
        ExperienceOrb source = orb(7);
        ExperienceOrb invalid = orb(3);
        ExperienceOrb dead = orb(5);
        when(invalid.isValid()).thenReturn(false);
        when(dead.isDead()).thenReturn(true);
        nearby(source, invalid, dead);
        PlayerExpChangeEvent event = new PlayerExpChangeEvent(player, source, 7);
        listener.onPlayerNearbyXPCollect(event);
        assertEquals(7, event.getAmount());
        verify(invalid, never()).remove();
        verify(dead, never()).remove();
    }

    @Test
    void ignoresNullAndNonOrbSources() {
        for (Entity source : new Entity[]{null, mock(Entity.class)}) {
            PlayerExpChangeEvent event = new PlayerExpChangeEvent(player, source, 9);
            listener.onPlayerNearbyXPCollect(event);
            assertEquals(9, event.getAmount());
        }
        verifyNoInteractions(world);
    }

    @Test
    void nonPositiveRadiusDisablesCollection() {
        for (double radius : new double[]{0, -1}) {
            when(plugin.getInstantCollectRadius()).thenReturn(radius);
            ExperienceOrb source = orb(7);
            PlayerExpChangeEvent event = new PlayerExpChangeEvent(player, source, 7);
            listener.onPlayerNearbyXPCollect(event);
            assertEquals(7, event.getAmount());
            verify(source, never()).remove();
        }
        verifyNoInteractions(world);
    }
}
