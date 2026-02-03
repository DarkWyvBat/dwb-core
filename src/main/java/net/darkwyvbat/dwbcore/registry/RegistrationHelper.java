package net.darkwyvbat.dwbcore.registry;

import net.darkwyvbat.dwbcore.world.gen.proxyblock.ProxyBlockAction;
import net.darkwyvbat.dwbcore.world.gen.proxyblock.ProxyBlockActionType;
import net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder;
import net.fabricmc.fabric.api.object.builder.v1.world.poi.PoiHelper;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.village.poi.PoiType;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.configurations.FeatureConfiguration;

import java.util.Arrays;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.UnaryOperator;

public final class RegistrationHelper {

    public static Block registerBlock(Identifier id, Function<BlockBehaviour.Properties, Block> function, BlockBehaviour.Properties properties) {
        ResourceKey<Block> key = ResourceKey.create(Registries.BLOCK, id);
        return Registry.register(BuiltInRegistries.BLOCK, key, function.apply(properties.setId(key)));
    }

    public static <T extends BlockEntity> BlockEntityType<T> registerBlockEntity(Identifier id, FabricBlockEntityTypeBuilder<T> builder) {
        return Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, id, builder.build());
    }

    public static <T extends Entity> EntityType<T> registerEntity(Identifier id, EntityType.Builder<T> builder) {
        ResourceKey<EntityType<?>> key = ResourceKey.create(Registries.ENTITY_TYPE, id);
        return Registry.register(BuiltInRegistries.ENTITY_TYPE, key, builder.build(key));
    }

    public static Item registerBlockItem(Block block) {
        return registerBlockItem(block, BlockItem::new, new Item.Properties());
    }

    public static Item registerBlockItem(Block block, Item.Properties properties) {
        return registerBlockItem(block, BlockItem::new, properties);
    }

    public static <T extends Item> T registerBlockItem(Block block, BiFunction<Block, Item.Properties, T> factory) {
        return registerBlockItem(block, factory, new Item.Properties());
    }

    public static <T extends Item> T registerBlockItem(Block block, BiFunction<Block, Item.Properties, T> factory, Item.Properties properties) {
        ResourceKey<Item> key = ResourceKey.create(Registries.ITEM, BuiltInRegistries.BLOCK.getKey(block));
        properties.setId(key);
        properties.useBlockDescriptionPrefix();
        T item = factory.apply(block, properties);
        Registry.register(BuiltInRegistries.ITEM, key, item);
        if (item instanceof BlockItem blockItem)
            blockItem.registerBlocks(Item.BY_BLOCK, item);
        return item;
    }

    public static Item registerItem(Identifier id, Item.Properties properties) {
        return registerItem(id, Item::new, properties);
    }

    public static Item registerItem(Identifier id, Function<Item.Properties, Item> itemFactory, Item.Properties properties) {
        ResourceKey<Item> key = ResourceKey.create(Registries.ITEM, id);
        return Registry.register(BuiltInRegistries.ITEM, key, itemFactory.apply(properties.setId(key)));
    }

    public static Item registerItem(Identifier id, Function<Item.Properties, Item> itemFactory) {
        return registerItem(id, itemFactory, new Item.Properties());
    }

    public static ResourceKey<PoiType> registerPoi(Identifier id, int ticketCount, int searchDistance, Block... blocks) {
        PoiHelper.register(id, ticketCount, searchDistance, blocks);
        return ResourceKey.create(Registries.POINT_OF_INTEREST_TYPE, id);
    }

    public static ResourceKey<PoiType> registerPoi(Identifier id, int ticketCount, int searchDistance, BlockState... states) {
        PoiHelper.register(id, ticketCount, searchDistance, Arrays.asList(states));
        return ResourceKey.create(Registries.POINT_OF_INTEREST_TYPE, id);
    }

    public static ResourceKey<PoiType> registerPoi(Identifier id, int ticketCount, int searchDistance, Block block, Predicate<BlockState> statePredicate) {
        return registerPoi(id, ticketCount, searchDistance, block.getStateDefinition().getPossibleStates().stream().filter(statePredicate).toArray(BlockState[]::new));
    }

    public static <T> DataComponentType<T> registerDataComponent(Identifier id, UnaryOperator<DataComponentType.Builder<T>> unaryOperator) {
        return Registry.register(BuiltInRegistries.DATA_COMPONENT_TYPE, id, unaryOperator.apply(DataComponentType.builder()).build());
    }

    public static SoundEvent registerSoundEvent(Identifier id) {
        return Registry.register(BuiltInRegistries.SOUND_EVENT, id, SoundEvent.createVariableRangeEvent(id));
    }

    public static Holder.Reference<SoundEvent> registerSoundEventRef(Identifier id) {
        return Registry.registerForHolder(BuiltInRegistries.SOUND_EVENT, id, SoundEvent.createVariableRangeEvent(id));
    }

    public static Holder.Reference<MobEffect> registerMobEffectRef(Identifier id, MobEffect mobEffect) {
        return Registry.registerForHolder(BuiltInRegistries.MOB_EFFECT, id, mobEffect);
    }

    public static <A extends ProxyBlockAction> ProxyBlockActionType<A> registerPbActionType(Identifier id, ProxyBlockActionType<A> type) {
        return Registry.register(DwbRegistries.PROXY_BLOCK_ACTION_TYPE, id, type);
    }

    public static <C extends FeatureConfiguration, F extends Feature<C>> F registerFeature(Identifier id, F feature) {
        return Registry.register(BuiltInRegistries.FEATURE, id, feature);
    }
}