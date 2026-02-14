package net.darkwyvbat.dwbcore.debug;

import net.darkwyvbat.dwbcore.registry.DwbRegistries;
import net.darkwyvbat.dwbcore.world.gen.proxyblock.ProxyBlockPool;
import net.darkwyvbat.dwbcore.world.gen.proxyblock.ProxyBlockPoolBuilder;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.block.Blocks;

import static net.darkwyvbat.dwbcore.DwbCore.INFO;

public class TestProxPools {
    public static final ResourceKey<ProxyBlockPool> ENTITY = key("entity");
    public static final ResourceKey<ProxyBlockPool> BLOCK = key("block");
    public static final ResourceKey<ProxyBlockPool> FACING = key("facing");
    public static final ResourceKey<ProxyBlockPool> RANDOM = key("random");
    public static final ResourceKey<ProxyBlockPool> CHAIN_A = key("chain_a");
    public static final ResourceKey<ProxyBlockPool> CHAIN_B = key("chain_b");
    public static final ResourceKey<ProxyBlockPool> LOOP = key("loop");
    public static final ResourceKey<ProxyBlockPool> BLOCK_CONNECT = key("block_connect");

    public static void bootstrap(BootstrapContext<ProxyBlockPool> context) {
        context.register(ENTITY, new ProxyBlockPoolBuilder()
                .entity(EntityType.ZOMBIE, "{equipment:{mainhand:{id:dirt},head:{id:iron_helmet},chest:{id:iron_chestplate}}}", 1)
                .build()
        );
        context.register(BLOCK, new ProxyBlockPoolBuilder()
                .block(Blocks.CHEST, "{LootTable:\"minecraft:chests/ancient_city\"}", 1)
                .build()
        );
        context.register(FACING, new ProxyBlockPoolBuilder()
                .block(Blocks.FURNACE, true, 1)
                .build()
        );
        context.register(RANDOM, new ProxyBlockPoolBuilder()
                .block(Blocks.DIAMOND_BLOCK, 2)
                .block(Blocks.DIRT, 8)
                .build()
        );
        context.register(CHAIN_B, new ProxyBlockPoolBuilder()
                .block(Blocks.GOLD_BLOCK, 1)
                .build()
        );
        context.register(CHAIN_A, new ProxyBlockPoolBuilder()
                .run(context.lookup(DwbRegistries.PROXY_BLOCK_POOL).getOrThrow(CHAIN_B), 1)
                .build()
        );
        context.register(LOOP, new ProxyBlockPoolBuilder()
                .run(context.lookup(DwbRegistries.PROXY_BLOCK_POOL).getOrThrow(LOOP), 1)
                .build()
        );
        context.register(BLOCK_CONNECT, new ProxyBlockPoolBuilder()
                .block(Blocks.CHEST, null, true, true, 1)
                .build()
        );
    }

    private static ResourceKey<ProxyBlockPool> key(String i) {
        return ResourceKey.create(DwbRegistries.PROXY_BLOCK_POOL, INFO.id(i));
    }
}