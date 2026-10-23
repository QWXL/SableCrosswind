package cn.chumenta.crosswind.registry;

import cn.chumenta.crosswind.Crosswind;
import cn.chumenta.crosswind.instrument.WindInstruments;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class CrosswindRegistry {
    private CrosswindRegistry() {
    }

    public static final DeferredRegister.Blocks BLOCKS =
            DeferredRegister.createBlocks(Crosswind.MOD_ID);

    public static final DeferredRegister.Items ITEMS =
            DeferredRegister.createItems(Crosswind.MOD_ID);

    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, Crosswind.MOD_ID);

    public static final DeferredRegister<net.minecraft.world.item.CreativeModeTab> CREATIVE_TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, Crosswind.MOD_ID);

    private static BlockBehaviour.Properties instrumentProps() {
        return BlockBehaviour.Properties.of()
                .strength(1.5F, 6.0F)
                .sound(SoundType.COPPER)

                .noOcclusion();
    }

    private static net.minecraft.world.level.block.Block windVaneBlock() {
        return WIND_VANE.get();
    }

    private static net.minecraft.world.level.block.Block anemometerBlock() {
        return ANEMOMETER.get();
    }

    private static net.minecraft.world.level.block.Block weatherStationBlock() {
        return WEATHER_STATION.get();
    }

    public static final DeferredBlock<WindInstruments.InstrumentBlock> WIND_VANE =
            BLOCKS.register("wind_vane",
                    () -> new WindInstruments.PoleInstrumentBlock(
                            instrumentProps(), cn.chumenta.crosswind.instrument.InstrumentShapes.WIND_VANE));

    public static final DeferredBlock<WindInstruments.InstrumentBlock> ANEMOMETER =
            BLOCKS.register("anemometer",
                    () -> new WindInstruments.PoleInstrumentBlock(
                            instrumentProps(), cn.chumenta.crosswind.instrument.InstrumentShapes.ANEMOMETER));

    public static final DeferredBlock<WindInstruments.InstrumentBlock> WEATHER_STATION =
            BLOCKS.register("weather_station",

                    () -> new WindInstruments.PoleInstrumentBlock(
                            instrumentProps().strength(2.5F, 12.0F),
                            cn.chumenta.crosswind.instrument.InstrumentShapes.WEATHER_STATION,

                            true));

    public static final DeferredBlock<WindInstruments.InstrumentBlock> INSTRUMENT_ROD =
            BLOCKS.register("instrument_rod",
                    () -> new WindInstruments.RodBlock(
                            instrumentProps().strength(1.0F, 4.0F),
                            cn.chumenta.crosswind.instrument.InstrumentShapes.ROD));

    public static final DeferredBlock<cn.chumenta.crosswind.rat.RatBlock> RAT =
            BLOCKS.register("rat",
                    () -> new cn.chumenta.crosswind.rat.RatBlock(
                            instrumentProps().strength(2.0F, 6.0F)));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<WindInstruments.WindVaneBlockEntity>> WIND_VANE_BE =
            BLOCK_ENTITIES.register("wind_vane",
                    () -> BlockEntityType.Builder.of(WindInstruments.WindVaneBlockEntity::new,
                            windVaneBlock()).build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<WindInstruments.AnemometerBlockEntity>> ANEMOMETER_BE =
            BLOCK_ENTITIES.register("anemometer",
                    () -> BlockEntityType.Builder.of(WindInstruments.AnemometerBlockEntity::new,
                            anemometerBlock()).build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<WindInstruments.WeatherStationBlockEntity>> WEATHER_STATION_BE =
            BLOCK_ENTITIES.register("weather_station",
                    () -> BlockEntityType.Builder.of(WindInstruments.WeatherStationBlockEntity::new,
                            weatherStationBlock()).build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<cn.chumenta.crosswind.rat.RatBlockEntity>> RAT_BE =
            BLOCK_ENTITIES.register("rat",
                    () -> BlockEntityType.Builder.of(
                            cn.chumenta.crosswind.rat.RatBlockEntity::create,
                            RAT.get()).build(null));

    private static DeferredItem<net.minecraft.world.item.BlockItem> poleItem(
            final String name,
            final DeferredBlock<WindInstruments.InstrumentBlock> block) {
        return ITEMS.register(name, () -> new cn.chumenta.crosswind.instrument.PoleBlockItem(
                block.get(), new net.minecraft.world.item.Item.Properties()));
    }

    public static final DeferredItem<net.minecraft.world.item.BlockItem> WIND_VANE_ITEM =
            poleItem("wind_vane", WIND_VANE);

    public static final DeferredItem<net.minecraft.world.item.BlockItem> ANEMOMETER_ITEM =
            poleItem("anemometer", ANEMOMETER);

    public static final DeferredItem<net.minecraft.world.item.BlockItem> WEATHER_STATION_ITEM =
            ITEMS.registerSimpleBlockItem("weather_station", WEATHER_STATION);

    public static final DeferredItem<net.minecraft.world.item.BlockItem> RAT_ITEM =
            ITEMS.registerSimpleBlockItem("rat", RAT);

    public static final DeferredRegister<net.minecraft.world.item.ArmorMaterial> ARMOR_MATERIALS =
            DeferredRegister.create(net.minecraft.core.registries.Registries.ARMOR_MATERIAL,
                    Crosswind.MOD_ID);

    public static final DeferredHolder<net.minecraft.world.item.ArmorMaterial,
            net.minecraft.world.item.ArmorMaterial> NON_SLIP_ARMOR_MATERIAL =
            ARMOR_MATERIALS.register("non_slip_boots", () -> new net.minecraft.world.item.ArmorMaterial(

                    java.util.Map.of(net.minecraft.world.item.ArmorItem.Type.BOOTS, 1),
                    15,
                    net.minecraft.sounds.SoundEvents.ARMOR_EQUIP_LEATHER,
                    () -> net.minecraft.world.item.crafting.Ingredient.of(
                            net.minecraft.world.item.Items.LEATHER),
                    java.util.List.of(new net.minecraft.world.item.ArmorMaterial.Layer(
                            Crosswind.id("non_slip_boots"))),
                    0.0F,
                    0.0F));

    public static final DeferredItem<cn.chumenta.crosswind.item.NonSlipBootsItem> NON_SLIP_BOOTS_ITEM =
            ITEMS.register("non_slip_boots",
                    () -> new cn.chumenta.crosswind.item.NonSlipBootsItem(
                            new net.minecraft.world.item.Item.Properties()

                                    .durability(net.minecraft.world.item.ArmorItem.Type.BOOTS
                                            .getDurability(5))));

    public static final DeferredItem<net.minecraft.world.item.BlockItem> INSTRUMENT_ROD_ITEM =
            poleItem("instrument_rod", INSTRUMENT_ROD);

    public static final DeferredRegister<net.minecraft.core.particles.ParticleType<?>> PARTICLE_TYPES =
            DeferredRegister.create(net.minecraft.core.registries.Registries.PARTICLE_TYPE, Crosswind.MOD_ID);

    public static final DeferredHolder<net.minecraft.core.particles.ParticleType<?>,
            net.minecraft.core.particles.SimpleParticleType> WIND_STREAK =
            PARTICLE_TYPES.register("wind_streak",
                    () -> new net.minecraft.core.particles.SimpleParticleType(false));

    public static void registerParticles(final net.neoforged.bus.api.IEventBus modBus) {
        PARTICLE_TYPES.register(modBus);
    }

    public static final DeferredHolder<net.minecraft.world.item.CreativeModeTab, net.minecraft.world.item.CreativeModeTab> TAB =
            CREATIVE_TABS.register("main", () -> net.minecraft.world.item.CreativeModeTab.builder()
                    .title(net.minecraft.network.chat.Component.translatable("itemGroup.crosswind.main"))
                    .icon(() -> new net.minecraft.world.item.ItemStack(WIND_VANE_ITEM.get()))
                    .displayItems((params, output) -> {
                        output.accept(WIND_VANE_ITEM.get());
                        output.accept(ANEMOMETER_ITEM.get());
                        output.accept(WEATHER_STATION_ITEM.get());
                        output.accept(INSTRUMENT_ROD_ITEM.get());
                        output.accept(RAT_ITEM.get());
                        output.accept(NON_SLIP_BOOTS_ITEM.get());
                    })
                    .build());

    public static void register(final net.neoforged.bus.api.IEventBus modBus) {
        BLOCKS.register(modBus);
        ITEMS.register(modBus);
        ARMOR_MATERIALS.register(modBus);
        BLOCK_ENTITIES.register(modBus);
        CREATIVE_TABS.register(modBus);
    }
}
