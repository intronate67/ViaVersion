package us.myles.ViaVersion.protocols.protocol1_9to1_8;

import org.bukkit.Material;
import org.spacehq.opennbt.tag.builtin.CompoundTag;
import org.spacehq.opennbt.tag.builtin.ListTag;
import org.spacehq.opennbt.tag.builtin.StringTag;
import org.spacehq.opennbt.tag.builtin.Tag;
import us.myles.ViaVersion.api.minecraft.item.Item;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class ItemRewriter {

    private static final Map<String, Integer> ENTTIY_NAME_TO_ID = new HashMap<>();
    private static final Map<Integer, String> ENTTIY_ID_TO_NAME = new HashMap<>();
    private static final Map<String, Integer> POTION_NAME_TO_ID = new HashMap<>();
    private static final Map<Integer, String> POTION_ID_TO_NAME = new HashMap<>();

    private static final Map<Integer, Integer> POTION_INDEX = new HashMap<>();

    static {
        /* Entities */
        registerEntity(1, "Item");
        registerEntity(2, "XPOrb");
        registerEntity(7, "ThrownEgg");
        registerEntity(8, "LeashKnot");
        registerEntity(9, "Painting");
        registerEntity(10, "Arrow");
        registerEntity(11, "Snowball");
        registerEntity(12, "Fireball");
        registerEntity(13, "SmallFireball");
        registerEntity(14, "ThrownEnderpearl");
        registerEntity(15, "EyeOfEnderSignal");
        registerEntity(16, "ThrownPotion");
        registerEntity(17, "ThrownExpBottle");
        registerEntity(18, "ItemFrame");
        registerEntity(19, "WitherSkull");
        registerEntity(20, "PrimedTnt");
        registerEntity(21, "FallingSand");
        registerEntity(22, "FireworksRocketEntity");
        registerEntity(30, "ArmorStand");
        registerEntity(40, "MinecartCommandBlock");
        registerEntity(41, "Boat");
        registerEntity(42, "MinecartRideable");
        registerEntity(43, "MinecartChest");
        registerEntity(44, "MinecartFurnace");
        registerEntity(45, "MinecartTNT");
        registerEntity(46, "MinecartHopper");
        registerEntity(47, "MinecartSpawner");
        registerEntity(48, "Mob");
        registerEntity(49, "Monster");
        registerEntity(50, "Creeper");
        registerEntity(51, "Skeleton");
        registerEntity(52, "Spider");
        registerEntity(53, "Giant");
        registerEntity(54, "Zombie");
        registerEntity(55, "Slime");
        registerEntity(56, "Ghast");
        registerEntity(57, "PigZombie");
        registerEntity(58, "Enderman");
        registerEntity(59, "CaveSpider");
        registerEntity(60, "Silverfish");
        registerEntity(61, "Blaze");
        registerEntity(62, "LavaSlime");
        registerEntity(63, "EnderDragon");
        registerEntity(64, "WitherBoss");
        registerEntity(65, "Bat");
        registerEntity(66, "Witch");
        registerEntity(67, "Endermite");
        registerEntity(68, "Guardian");
        registerEntity(90, "Pig");
        registerEntity(91, "Sheep");
        registerEntity(92, "Cow");
        registerEntity(93, "Chicken");
        registerEntity(94, "Squid");
        registerEntity(95, "Wolf");
        registerEntity(96, "MushroomCow");
        registerEntity(97, "SnowMan");
        registerEntity(98, "Ozelot");
        registerEntity(99, "VillagerGolem");
        registerEntity(100, "EntityHorse");
        registerEntity(101, "Rabbit");
        registerEntity(120, "Villager");
        registerEntity(200, "EnderCrystal");

        /* Potions */
        registerPotion(-1, "empty");
        registerPotion(0, "water");
        registerPotion(64, "mundane");
        registerPotion(32, "thick");
        registerPotion(16, "awkward");

        registerPotion(8198, "night_vision");
        registerPotion(8262, "long_night_vision");

        registerPotion(8206, "invisibility");
        registerPotion(8270, "long_invisibility");

        registerPotion(8203, "leaping");
        registerPotion(8267, "long_leaping");
        registerPotion(8235, "strong_leaping");

        registerPotion(8195, "fire_resistance");
        registerPotion(8259, "long_fire_resistance");

        registerPotion(8194, "swiftness");
        registerPotion(8258, "long_swiftness");
        registerPotion(8226, "strong_swiftness");

        registerPotion(8202, "slowness");
        registerPotion(8266, "long_slowness");

        registerPotion(8205, "water_breathing");
        registerPotion(8269, "long_water_breathing");

        registerPotion(8261, "healing");
        registerPotion(8229, "strong_healing");

        registerPotion(8204, "harming");
        registerPotion(8236, "strong_harming");

        registerPotion(8196, "poison");
        registerPotion(8260, "long_poison");
        registerPotion(8228, "strong_poison");

        registerPotion(8193, "regeneration");
        registerPotion(8257, "long_regeneration");
        registerPotion(8225, "strong_regeneration");

        registerPotion(8201, "strength");
        registerPotion(8265, "long_strength");
        registerPotion(8233, "strong_strength");

        registerPotion(8200, "weakness");
        registerPotion(8264, "long_weakness");

    }

    public static void toServer(Item item) {
        if (item != null) {
            if (item.getId() == Material.MONSTER_EGG.getId() && item.getData() == 0) {
                CompoundTag tag = item.getTag();
                int data = 0;
                if (tag != null && tag.get("EntityTag") instanceof CompoundTag) {
                    CompoundTag entityTag = tag.get("EntityTag");
                    if (entityTag.get("id") instanceof StringTag) {
                        StringTag id = entityTag.get("id");
                        if (ENTTIY_NAME_TO_ID.containsKey(id.getValue()))
                            data = ENTTIY_NAME_TO_ID.get(id.getValue());
                    }
                    tag.remove("EntityTag");
                }
                item.setTag(tag);
                item.setData((short) data);
            }
            if (item.getId() == Material.POTION.getId()) {
                CompoundTag tag = item.getTag();
                int data = 0;
                if (tag != null && tag.get("Potion") instanceof StringTag) {
                    StringTag potion = tag.get("Potion");
                    String potionName = potion.getValue().replace("minecraft:", "");
                    if (POTION_NAME_TO_ID.containsKey(potionName)) {
                        data = POTION_NAME_TO_ID.get(potionName);
                    }
                    tag.remove("Potion");
                }
                item.setTag(tag);
                item.setData((short) data);
            }
            //Splash potion
            if (item.getId() == 438) {
                CompoundTag tag = item.getTag();
                int data = 0;
                item.setId((short) Material.POTION.getId());
                if (tag != null && tag.get("Potion") instanceof StringTag) {
                    StringTag potion = tag.get("Potion");
                    String potionName = potion.getValue().replace("minecraft:", "");
                    if (POTION_NAME_TO_ID.containsKey(potionName)) {
                        data = POTION_NAME_TO_ID.get(potionName) + 8192;
                    }
                    tag.remove("Potion");
                }
                item.setTag(tag);
                item.setData((short) data);
            }
        }
    }

    public static void toClient(Item item) {
        if (item != null) {
            if (item.getId() == Material.MONSTER_EGG.getId() && item.getData() != 0) {
                CompoundTag tag = item.getTag();
                if (tag == null) {
                    tag = new CompoundTag("tag");
                }
                CompoundTag entityTag = new CompoundTag("EntityTag");
                if (ENTTIY_ID_TO_NAME.containsKey((int) item.getData())) {
                    StringTag id = new StringTag("id", ENTTIY_ID_TO_NAME.get((int) item.getData()));
                    entityTag.put(id);
                    tag.put(entityTag);
                }
                item.setTag(tag);
                item.setData((short) 0);
            }
            if (item.getId() == Material.POTION.getId()) {
                CompoundTag tag = item.getTag();
                if (tag == null) {
                    tag = new CompoundTag("tag");
                }
                if (item.getData() >= 16384) {
                    item.setId((short) 438); // splash id
                    item.setData((short) (item.getData() - 8192));
                }
                if (POTION_ID_TO_NAME.containsKey((int) item.getData())) {
                    String name = POTION_ID_TO_NAME.get((int) item.getData());
                    StringTag potion = new StringTag("Potion", "minecraft:" + name);
                    tag.put(potion);
                }
                item.setTag(tag);
                item.setData((short) 0);
            }
            if (item.getId() == Material.WRITTEN_BOOK.getId()) {
                CompoundTag tag = item.getTag();
                if (tag == null) {
                    tag = new CompoundTag("tag");
                }
                ListTag pages = tag.get("pages");
                if (pages == null) {
                    pages = new ListTag("pages", Collections.<Tag>singletonList(new StringTag(Protocol1_9TO1_8.fixJson(""))));
                    tag.put(pages);
                    item.setTag(tag);
                    return;
                }

                for (int i = 0; i < pages.size(); i++) {
                    if (!(pages.get(i) instanceof StringTag))
                        continue;
                    StringTag page = pages.get(i);
                    page.setValue(Protocol1_9TO1_8.fixJson(page.getValue()));
                }
                item.setTag(tag);
            }
        }
    }

    public static int getNewEffectID(int oldID) {
        if (oldID >= 16384) {
            oldID -= 8192;
        }
        return POTION_INDEX.containsKey(oldID) ? POTION_INDEX.get(oldID) : 0;
    }

    private static void registerEntity(Integer id, String name) {
        ENTTIY_ID_TO_NAME.put(id, name);
        ENTTIY_NAME_TO_ID.put(name, id);
    }

    private static void registerPotion(Integer id, String name) {
        POTION_INDEX.put(id, POTION_ID_TO_NAME.size());
        POTION_ID_TO_NAME.put(id, name);
        POTION_NAME_TO_ID.put(name, id);
    }
}
