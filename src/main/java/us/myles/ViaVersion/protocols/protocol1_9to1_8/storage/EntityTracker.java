package us.myles.ViaVersion.protocols.protocol1_9to1_8.storage;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import io.netty.buffer.ByteBuf;
import lombok.Getter;
import lombok.Setter;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import us.myles.ViaVersion.ViaVersionPlugin;
import us.myles.ViaVersion.api.PacketWrapper;
import us.myles.ViaVersion.api.ViaVersion;
import us.myles.ViaVersion.api.boss.BossBar;
import us.myles.ViaVersion.api.boss.BossColor;
import us.myles.ViaVersion.api.boss.BossStyle;
import us.myles.ViaVersion.api.data.StoredObject;
import us.myles.ViaVersion.api.data.UserConnection;
import us.myles.ViaVersion.api.minecraft.Position;
import us.myles.ViaVersion.api.minecraft.item.Item;
import us.myles.ViaVersion.api.minecraft.metadata.Metadata;
import us.myles.ViaVersion.api.type.Type;
import us.myles.ViaVersion.protocols.base.ProtocolInfo;

import java.util.*;
import java.util.concurrent.TimeUnit;

@Getter
public class EntityTracker extends StoredObject {
    private final Map<Integer, UUID> uuidMap = new HashMap<>();
    private final Map<Integer, EntityType> clientEntityTypes = new HashMap<>();
    private final Map<Integer, Integer> vehicleMap = new HashMap<>();
    private final Map<Integer, BossBar> bossBarMap = new HashMap<>();
    private final Set<Integer> validBlocking = new HashSet<>();
    private final Set<Integer> knownHolograms = new HashSet<>();
    private final Cache<Position, Material> blockInteractions = CacheBuilder.newBuilder().maximumSize(10).expireAfterAccess(250, TimeUnit.MILLISECONDS).build();
    @Setter
    private boolean blocking = false;
    @Setter
    private boolean autoTeam = false;
    @Setter
    private Long lastPlaceBlock = -1L;
    @Setter
    private int entityID;
    private boolean teamExists = false;

    public EntityTracker(UserConnection user) {
        super(user);
    }

    public UUID getEntityUUID(int id) {
        if (uuidMap.containsKey(id)) {
            return uuidMap.get(id);
        } else {
            UUID uuid = UUID.randomUUID();
            uuidMap.put(id, uuid);
            return uuid;
        }
    }

    public void setSecondHand(Item item) {
        setSecondHand(entityID, item);
    }

    public void setSecondHand(int entityID, Item item) {
        PacketWrapper wrapper = new PacketWrapper(0x3C, null, getUser());
        wrapper.write(Type.VAR_INT, entityID);
        wrapper.write(Type.VAR_INT, 1); // slot
        wrapper.write(Type.ITEM, item);
        try {
            wrapper.send();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void removeEntity(Integer entityID) {
        clientEntityTypes.remove(entityID);
        vehicleMap.remove(entityID);
        uuidMap.remove(entityID);
        validBlocking.remove(entityID);
        knownHolograms.remove(entityID);

        BossBar bar = bossBarMap.remove(entityID);
        if (bar != null) {
            bar.hide();
        }
    }

    public boolean interactedBlockRecently(int x, int y, int z) {
        if (blockInteractions.size() == 0)
            return false;
        Iterator<Position> it = blockInteractions.asMap().keySet().iterator();
        while (it.hasNext()) {
            Position p = it.next();
            if (p.getX() == x)
                if (p.getY() == y)
                    if (p.getZ() == z)
                        return true;
        }
        return false;
    }

    public void addBlockInteraction(Position p) {
        blockInteractions.put(p, Material.AIR);
    }

    public void handleMetadata(int entityID, List<Metadata> metadataList) {
        if (!clientEntityTypes.containsKey(entityID)) return;

        EntityType type = clientEntityTypes.get(entityID);
        for (Metadata metadata : new ArrayList<>(metadataList)) {
            // Fix: wither (crash fix)
            if (type == EntityType.WITHER) {
                if (metadata.getId() == 10) {
                    metadataList.remove(metadata);
                    //metadataList.add(new Metadata(10, NewType.Byte.getTypeID(), Type.BYTE, 0));
                }
            }
            // Fix: enderdragon (crash fix)
            if (type == EntityType.ENDER_DRAGON) {
                if (metadata.getId() == 11) {
                    metadataList.remove(metadata);
                    //   metadataList.add(new Metadata(11, NewType.Byte.getTypeID(), Type.VAR_INT, 0));
                }
            }

            if (type == EntityType.PLAYER) {
                if (metadata.getId() == 0) {
                    // Byte
                    byte data = (byte) metadata.getValue();
                    if (entityID != getEntityID() && ((ViaVersionPlugin) ViaVersion.getInstance()).isShieldBlocking()) {
                        if ((data & 0x10) == 0x10) {
                            if (validBlocking.contains(entityID)) {
                                Item shield = new Item((short) 442, (byte) 1, (short) 0, null);
                                setSecondHand(entityID, shield);
                            }
                        } else {
                            setSecondHand(entityID, null);
                        }
                    }
                }
            }
            if (type == EntityType.ARMOR_STAND && ((ViaVersionPlugin) ViaVersion.getInstance()).isHologramPatch()) {
                if (metadata.getId() == 0 && getMetaByIndex(metadataList, 10) != null) {
                    Metadata meta = getMetaByIndex(metadataList, 10); //Only happens if the armorstand is small
                    byte data = (byte) metadata.getValue();
                    // Check invisible | Check small | Check if custom name is empty | Check if custom name visible is true
                    if ((data & 0x20) == 0x20 && ((byte) meta.getValue() & 0x01) == 0x01
                            && ((String) getMetaByIndex(metadataList, 2).getValue()).length() != 0 && (boolean) getMetaByIndex(metadataList, 3).getValue()) {
                        if (!knownHolograms.contains(entityID)) {
                            knownHolograms.add(entityID);
                            try {
                                // Send movement
                                ByteBuf buf = getUser().getChannel().alloc().buffer();
                                Type.VAR_INT.write(buf, 0x25); // Relative Move Packet
                                Type.VAR_INT.write(buf, entityID);
                                buf.writeShort(0);
                                buf.writeShort((short) (128D * (((ViaVersionPlugin) ViaVersion.getInstance()).getHologramYOffset() * 32D)));
                                buf.writeShort(0);
                                buf.writeBoolean(true);
                                getUser().sendRawPacket(buf, false);
                            } catch (Exception ignored) {
                            }
                        }
                    }
                }
            }
            Player player = Bukkit.getPlayer(getUser().get(ProtocolInfo.class).getUuid());
            // Boss bar
            if (((ViaVersionPlugin) ViaVersion.getInstance()).isBossbarPatch()) {
                if (type == EntityType.ENDER_DRAGON || type == EntityType.WITHER) {
                    if (metadata.getId() == 2) {
                        BossBar bar = bossBarMap.get(entityID);
                        String title = (String) metadata.getValue();
                        title = title.isEmpty() ? (type == EntityType.ENDER_DRAGON ? "Ender Dragon" : "Wither") : title;
                        if (bar == null) {
                            bar = ViaVersion.getInstance().createBossBar(title, BossColor.PINK, BossStyle.SOLID);
                            bossBarMap.put(entityID, bar);
                            bar.addPlayer(player);
                            bar.show();
                        } else {
                            bar.setTitle(title);
                        }
                    } else if (metadata.getId() == 6 && !((ViaVersionPlugin) ViaVersion.getInstance()).isBossbarAntiflicker()) { // If anti flicker is enabled, don't update health
                        BossBar bar = bossBarMap.get(entityID);
                        // Make health range between 0 and 1
                        float maxHealth = type == EntityType.ENDER_DRAGON ? 200.0f : 300.0f;
                        float health = Math.max(0.0f, Math.min(((float) metadata.getValue()) / maxHealth, 1.0f));
                        if (bar == null) {
                            String title = type == EntityType.ENDER_DRAGON ? "Ender Dragon" : "Wither";
                            bar = ViaVersion.getInstance().createBossBar(title, health, BossColor.PINK, BossStyle.SOLID);
                            bossBarMap.put(entityID, bar);
                            bar.addPlayer(player);
                            bar.show();
                        } else {
                            bar.setHealth(health);
                        }
                    }
                }
            }
        }
    }

    public Metadata getMetaByIndex(List<Metadata> list, int index) {
        for (Metadata meta : list)
            if (index == meta.getId())
                return meta;
        return null;
    }

    public void sendTeamPacket(boolean b) {
        PacketWrapper wrapper = new PacketWrapper(0x41, null, getUser());
        wrapper.write(Type.STRING, "viaversion"); // Use viaversion as name
        if (b) {
            // add
            if (!teamExists) {
                wrapper.write(Type.BYTE, (byte) 0); // make team
                wrapper.write(Type.STRING, "viaversion");
                wrapper.write(Type.STRING, ""); // prefix
                wrapper.write(Type.STRING, ""); // suffix
                wrapper.write(Type.BYTE, (byte) 0); // friendly fire
                wrapper.write(Type.STRING, ""); // nametags
                wrapper.write(Type.STRING, "never"); // collision rule :)
                wrapper.write(Type.BYTE, (byte) 0); // color
            } else {
                wrapper.write(Type.BYTE, (byte) 3);
            }
            wrapper.write(Type.VAR_INT, 1); // player count
            wrapper.write(Type.STRING, getUser().get(ProtocolInfo.class).getUsername());
        } else {
            wrapper.write(Type.BYTE, (byte) 1); // remove team
        }
        teamExists = b;
        try {
            wrapper.send();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
