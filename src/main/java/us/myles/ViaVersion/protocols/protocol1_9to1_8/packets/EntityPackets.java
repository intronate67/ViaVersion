package us.myles.ViaVersion.protocols.protocol1_9to1_8.packets;

import org.bukkit.Material;
import us.myles.ViaVersion.api.PacketWrapper;
import us.myles.ViaVersion.api.ViaVersion;
import us.myles.ViaVersion.api.minecraft.item.Item;
import us.myles.ViaVersion.api.minecraft.metadata.Metadata;
import us.myles.ViaVersion.api.protocol.Protocol;
import us.myles.ViaVersion.api.remapper.PacketHandler;
import us.myles.ViaVersion.api.remapper.PacketRemapper;
import us.myles.ViaVersion.api.remapper.ValueTransformer;
import us.myles.ViaVersion.api.type.Type;
import us.myles.ViaVersion.packets.State;
import us.myles.ViaVersion.protocols.protocol1_9to1_8.ItemRewriter;
import us.myles.ViaVersion.protocols.protocol1_9to1_8.Protocol1_9TO1_8;
import us.myles.ViaVersion.protocols.protocol1_9to1_8.metadata.MetadataRewriter;
import us.myles.ViaVersion.protocols.protocol1_9to1_8.storage.EntityTracker;

import java.util.List;

public class EntityPackets {
    public static ValueTransformer<Byte, Short> toNewShort = new ValueTransformer<Byte, Short>(Type.SHORT) {
        @Override
        public Short transform(PacketWrapper wrapper, Byte inputValue) {
            return (short) (inputValue * 128);
        }
    };

    public static void register(Protocol protocol) {
        // Attach Entity Packet
        protocol.registerOutgoing(State.PLAY, 0x1B, 0x3A, new PacketRemapper() {

            @Override
            public void registerMap() {
                map(Type.INT); // 0 - Entity ID
                map(Type.INT); // 1 - Vehicle

                // Leash boolean is removed in new versions
                map(Type.BOOLEAN, new ValueTransformer<Boolean, Void>(Type.NOTHING) {
                    @Override
                    public Void transform(PacketWrapper wrapper, Boolean inputValue) throws Exception {
                        EntityTracker tracker = wrapper.user().get(EntityTracker.class);
                        if (!inputValue) {
                            int passenger = wrapper.get(Type.INT, 0);
                            int vehicle = wrapper.get(Type.INT, 1);

                            wrapper.cancel(); // Don't send current packet

                            PacketWrapper passengerPacket = wrapper.create(0x40); // Passenger Packet ID
                            if (vehicle == -1) {
                                if (!tracker.getVehicleMap().containsKey(passenger))
                                    return null; // Cancel
                                passengerPacket.write(Type.VAR_INT, tracker.getVehicleMap().remove(passenger));
                                passengerPacket.write(Type.VAR_INT_ARRAY, new Integer[]{});
                            } else {
                                passengerPacket.write(Type.VAR_INT, vehicle);
                                passengerPacket.write(Type.VAR_INT_ARRAY, new Integer[]{passenger});
                                tracker.getVehicleMap().put(passenger, vehicle);
                            }
                            passengerPacket.send(); // Send the packet
                        }
                        return null;
                    }
                });
            }
        });
        // Entity Teleport Packet
        protocol.registerOutgoing(State.PLAY, 0x18, 0x4A, new PacketRemapper() {

            @Override
            public void registerMap() {
                map(Type.VAR_INT); // 0 - Entity ID
                map(Type.INT, SpawnPackets.toNewDouble); // 1 - X - Needs to be divide by 32
                map(Type.INT, SpawnPackets.toNewDouble); // 2 - Y - Needs to be divide by 32
                map(Type.INT, SpawnPackets.toNewDouble); // 3 - Z - Needs to be divide by 32

                map(Type.BYTE); // 4 - Pitch
                map(Type.BYTE); // 5 - Yaw

                map(Type.BOOLEAN); // 6 - On Ground

                handler(new PacketHandler() {
                    @Override
                    public void handle(PacketWrapper wrapper) throws Exception {
                        int entityID = wrapper.get(Type.VAR_INT, 0);
                        if (ViaVersion.getConfig().isHologramPatch()) {
                            EntityTracker tracker = wrapper.user().get(EntityTracker.class);
                            if (tracker.getKnownHolograms().contains(entityID)) {
                                Double newValue = wrapper.get(Type.DOUBLE, 1);
                                newValue += (ViaVersion.getConfig().getHologramYOffset());
                                wrapper.set(Type.DOUBLE, 1, newValue);
                            }
                        }
                    }
                });


            }
        });
        // Entity Look Move Packet
        protocol.registerOutgoing(State.PLAY, 0x17, 0x26, new PacketRemapper() {

            @Override
            public void registerMap() {
                map(Type.VAR_INT); // 0 - Entity ID
                map(Type.BYTE, toNewShort); // 1 - X
                map(Type.BYTE, toNewShort); // 2 - Y
                map(Type.BYTE, toNewShort); // 3 - Z

                map(Type.BYTE); // 4 - Yaw
                map(Type.BYTE); // 5 - Pitch

                map(Type.BOOLEAN); // 6 - On Ground
            }
        });
        // Entity Relative Move Packet
        protocol.registerOutgoing(State.PLAY, 0x15, 0x25, new PacketRemapper() {

            @Override
            public void registerMap() {
                map(Type.VAR_INT); // 0 - Entity ID
                map(Type.BYTE, toNewShort); // 1 - X
                map(Type.BYTE, toNewShort); // 2 - Y
                map(Type.BYTE, toNewShort); // 3 - Z

                map(Type.BOOLEAN); // 4 - On Ground
            }
        });
        // Entity Equipment Packet
        protocol.registerOutgoing(State.PLAY, 0x04, 0x3C, new PacketRemapper() {

            @Override
            public void registerMap() {
                map(Type.VAR_INT); // 0 - Entity ID
                // 1 - Slot ID
                map(Type.SHORT, new ValueTransformer<Short, Integer>(Type.VAR_INT) {
                    @Override
                    public Integer transform(PacketWrapper wrapper, Short slot) {
                        return slot > 0 ? slot.intValue() + 1 : slot.intValue();
                    }
                });
                map(Type.ITEM); // 2 - Item
                // Item Rewriter
                handler(new PacketHandler() {
                    @Override
                    public void handle(PacketWrapper wrapper) throws Exception {
                        Item stack = wrapper.get(Type.ITEM, 0);
                        ItemRewriter.toClient(stack);
                    }
                });
                // Blocking
                handler(new PacketHandler() {
                    @Override
                    public void handle(PacketWrapper wrapper) throws Exception {
                        EntityTracker entityTracker = wrapper.user().get(EntityTracker.class);
                        int entityID = wrapper.get(Type.VAR_INT, 0);
                        Item stack = wrapper.get(Type.ITEM, 0);

                        if (stack != null) {
                            if (Material.getMaterial(stack.getId()) != null) {
                                if (Material.getMaterial(stack.getId()).name().endsWith("SWORD")) {
                                    entityTracker.getValidBlocking().add(entityID);
                                    return;
                                }
                            }
                        }
                        entityTracker.getValidBlocking().remove(entityID);
                    }
                });
            }
        });
        // Entity Metadata Packet
        protocol.registerOutgoing(State.PLAY, 0x1C, 0x39, new PacketRemapper() {

            @Override
            public void registerMap() {
                map(Type.VAR_INT); // 0 - Entity ID
                map(Protocol1_9TO1_8.METADATA_LIST); // 1 - Metadata List
                handler(new PacketHandler() {
                    @Override
                    public void handle(PacketWrapper wrapper) throws Exception {
                        List<Metadata> metadataList = wrapper.get(Protocol1_9TO1_8.METADATA_LIST, 0);
                        int entityID = wrapper.get(Type.VAR_INT, 0);
                        EntityTracker tracker = wrapper.user().get(EntityTracker.class);
                        if (tracker.getClientEntityTypes().containsKey(entityID)) {
                            MetadataRewriter.transform(tracker.getClientEntityTypes().get(entityID), metadataList);
                        } else if (!ViaVersion.getConfig().isUnknownEntitiesSuppressed() || ViaVersion.getInstance().isDebug()) {
                            System.out.println("Unable to find entity for metadata, entity ID: " + entityID);
                            metadataList.clear();
                        }
                    }
                });

                // Handler for meta data
                handler(new PacketHandler() {
                    @Override
                    public void handle(PacketWrapper wrapper) throws Exception {
                        List<Metadata> metadataList = wrapper.get(Protocol1_9TO1_8.METADATA_LIST, 0);
                        int entityID = wrapper.get(Type.VAR_INT, 0);
                        EntityTracker tracker = wrapper.user().get(EntityTracker.class);
                        tracker.handleMetadata(entityID, metadataList);
                    }
                });

                // Cancel packet if list empty
                handler(new PacketHandler() {
                    @Override
                    public void handle(PacketWrapper wrapper) throws Exception {
                        List<Metadata> metadataList = wrapper.get(Protocol1_9TO1_8.METADATA_LIST, 0);
                        if (metadataList.size() == 0)
                            wrapper.cancel();
                    }
                });
            }
        });

        // Entity Effect Packet
        protocol.registerOutgoing(State.PLAY, 0x1D, 0x4C, new PacketRemapper() {

            @Override
            public void registerMap() {
                map(Type.VAR_INT); // 0 - Entity ID
                map(Type.BYTE); // 1 - Effect ID
                map(Type.BYTE); // 2 - Amplifier
                map(Type.VAR_INT); // 3 - Duration
                handler(new PacketHandler() { //Handle effect indicator
                    @Override
                    public void handle(PacketWrapper wrapper) throws Exception {
                        boolean showParticles = wrapper.read(Type.BOOLEAN); //In 1.8 = true->Show particles : false->Hide particles
                        boolean newEffect = ViaVersion.getConfig().isNewEffectIndicator();
                        //0: hide, 1: shown without indictator, 2: shown with indicator, 3: hide with beacon indicator but we don't use it.
                        wrapper.write(Type.BYTE, (byte) (showParticles ? newEffect ? 2 : 1 : 0));
                    }
                });
            }
        });


        // Update Entity NBT
        protocol.registerOutgoing(State.PLAY, 0x49, 0x49, new PacketRemapper() {
            @Override
            public void registerMap() {
                handler(new PacketHandler() {
                    @Override
                    public void handle(PacketWrapper wrapper) throws Exception {
//                        int id = wrapper.read(Type.VAR_INT);
//                        CompoundTag tag = wrapper.read(Type.NBT);
//                        System.out.println(id + " - " + tag);
                        wrapper.cancel();
                    }
                });
            }
        });


        /* Packets which do not have any field remapping or handlers */

        protocol.registerOutgoing(State.PLAY, 0x20, 0x4B); // Entity Properties Packet

        protocol.registerOutgoing(State.PLAY, 0x1A, 0x1B); // Entity Status Packet
        protocol.registerOutgoing(State.PLAY, 0x16, 0x27); // Entity Look Packet
        protocol.registerOutgoing(State.PLAY, 0x14, 0x28); // Entity Packet

        protocol.registerOutgoing(State.PLAY, 0x42, 0x2C); // Combat Event Packet
        protocol.registerOutgoing(State.PLAY, 0x0A, 0x2F); // Use Bed Packet

        protocol.registerOutgoing(State.PLAY, 0x1E, 0x31); // Remove Entity Effect Packet
        protocol.registerOutgoing(State.PLAY, 0x19, 0x34); // Entity Head Look Packet
        protocol.registerOutgoing(State.PLAY, 0x12, 0x3B); // Entity Velocity Packet

        /* Incoming Packets */

        // Entity Action Packet
        protocol.registerIncoming(State.PLAY, 0x0B, 0x14, new PacketRemapper() {

            @Override
            public void registerMap() {
                map(Type.VAR_INT); // 0 - Player ID
                map(Type.VAR_INT); // 1 - Action
                map(Type.VAR_INT); // 2 - Jump
                handler(new PacketHandler() {
                    @Override
                    public void handle(PacketWrapper wrapper) throws Exception {
                        int action = wrapper.get(Type.VAR_INT, 1);
                        if (action == 6 || action == 8)
                            wrapper.cancel();
                        if (action == 7) {
                            wrapper.set(Type.VAR_INT, 1, 6);
                        }
                    }
                });
            }
        });


        // Use Entity Packet
        protocol.registerIncoming(State.PLAY, 0x02, 0x0A, new PacketRemapper() {

            @Override
            public void registerMap() {
                map(Type.VAR_INT); // 0 - Entity ID (Target)
                map(Type.VAR_INT); // 1 - Action Type
                handler(new PacketHandler() {
                    @Override
                    public void handle(PacketWrapper wrapper) throws Exception {
                        int type = wrapper.get(Type.VAR_INT, 1);
                        if (type == 2) {
                            wrapper.passthrough(Type.FLOAT); // 2 - X
                            wrapper.passthrough(Type.FLOAT); // 3 - Y
                            wrapper.passthrough(Type.FLOAT); // 4 - Z
                        }
                        if (type == 0 || type == 2) {
                            wrapper.read(Type.VAR_INT); // 2/5 - Hand
                        }
                    }
                });
            }
        });

        /* Packets which do not have any field remapping or handlers */

        protocol.registerIncoming(State.PLAY, 0x0C, 0x15); // Steer Vehicle Packet
        protocol.registerIncoming(State.PLAY, 0x18, 0x1B); // Spectate Packet

    }
}
