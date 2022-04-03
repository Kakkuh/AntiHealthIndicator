package de.laserslime.antihealthindicator.packetadapters;

import java.util.LinkedList;
import java.util.List;

import org.bukkit.entity.Entity;
import org.bukkit.plugin.Plugin;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketEvent;
import com.comphenix.protocol.reflect.StructureModifier;
import com.comphenix.protocol.wrappers.WrappedDataWatcher;
import com.comphenix.protocol.wrappers.WrappedWatchableObject;

import de.laserslime.antihealthindicator.data.EntityDataIndex;

public class EntityMetadataAdapter extends PacketAdapter {

	public EntityMetadataAdapter(Plugin plugin) {
		super(plugin, PacketType.Play.Server.ENTITY_METADATA, PacketType.Play.Server.SPAWN_ENTITY_LIVING, PacketType.Play.Server.NAMED_ENTITY_SPAWN);
	}

	@Override
	public void onPacketSending(PacketEvent event) {
		if(event.getPacketType() != PacketType.Play.Server.ENTITY_METADATA) {
			WrappedDataWatcher watcher = event.getPacket().getDataWatcherModifier().readSafely(0).deepClone();
			watcher.remove(EntityDataIndex.HEALTH.getIndex());
			event.getPacket().getDataWatcherModifier().write(0, watcher);
		}
		StructureModifier<List<WrappedWatchableObject>> listModifier = event.getPacket().getWatchableCollectionModifier();
		List<WrappedWatchableObject> watchersold = listModifier.readSafely(0);
		if(watchersold == null) {// 1.15+ doesn't send the metadata in the spawn mob and spawn player packets
			return;
		}

		Entity entity = event.getPacket().getEntityModifier(event).readSafely(0);
		List<WrappedWatchableObject> watchersnew = new LinkedList<>(watchersold); // Create a copy to prevent ConcurrentModificationException
		for(WrappedWatchableObject current : watchersold) {
			if(plugin.getConfig().getBoolean("filters.entitydata.health.enabled", true)
					&& (EntityDataIndex.HEALTH.match(entity.getClass(), current.getIndex()) || EntityDataIndex.ABSORPTION.match(entity.getClass(), current.getIndex()))
					&& !entity.equals(event.getPlayer()) && event.getPlayer().getVehicle() != entity && (float) current.getValue() > 0f) // Only filter if health is greater than 0 to keep the player
																																			// death animation
				watchersnew.remove(current);

			if(plugin.getConfig().getBoolean("filters.entitydata.airticks.enabled", false) && EntityDataIndex.AIR_TICKS.match(entity.getClass(), current.getIndex()))
				watchersnew.remove(current);

			if(plugin.getConfig().getBoolean("filters.entitydata.xp.enabled", true) && EntityDataIndex.XP.match(entity.getClass(), current.getIndex()))
				watchersnew.remove(current);
		}
		listModifier.writeSafely(0, watchersnew);
	}
}
