package net.darkwyvbat.dwbcore.world.gen.proxyblock;

import net.darkwyvbat.dwbcore.world.gen.proxyblock.action.PlaceBlockAction;
import net.darkwyvbat.dwbcore.world.gen.proxyblock.action.RunPoolAction;
import net.darkwyvbat.dwbcore.world.gen.proxyblock.action.SpawnEntityAction;
import net.minecraft.core.Holder;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.TagParser;
import net.minecraft.resources.Identifier;
import net.minecraft.util.random.WeightedList;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

public class ProxyBlockPoolBuilder {
    private BlockState fallback = Blocks.AIR.defaultBlockState();
    private final WeightedList.Builder<ProxyBlockAction> entries = WeightedList.builder();
    private ProxyBlockAction pendingAction;
    private int pendingWeight;

    public ProxyBlockPoolBuilder op(ProxyBlockActionOp<?>... ops) {
        List<Identifier> ids = Arrays.stream(ops).map(ProxyBlockActionOp::id).toList();
        if (pendingAction instanceof SpawnEntityAction(
                EntityType<?> entityType, Optional<CompoundTag> nbt, List<Identifier> ops1
        )) {
            List<Identifier> forAdd = new ArrayList<>(ops1);
            forAdd.addAll(ids);
            pendingAction = new SpawnEntityAction(entityType, nbt, forAdd);

        } else if (pendingAction instanceof PlaceBlockAction(
                BlockState blockState, Optional<CompoundTag> nbt, boolean copyFacing, boolean checkNeighbors,
                List<Identifier> ops1
        )) {
            List<Identifier> forAdd = new ArrayList<>(ops1);
            forAdd.addAll(ids);
            pendingAction = new PlaceBlockAction(blockState, nbt, copyFacing, checkNeighbors, forAdd);
        }
        return this;
    }

    public ProxyBlockPoolBuilder entity(EntityType<?> entity, int weight) {
        return entity(entity, null, weight);
    }

    public ProxyBlockPoolBuilder entity(EntityType<?> entity, String nbt, int weight) {
        commit();
        pendingAction = new SpawnEntityAction(entity, parseNbt(nbt), List.of());
        pendingWeight = weight;
        return this;
    }

    public ProxyBlockPoolBuilder block(Block block) {
        return block(block.defaultBlockState(), null, false, true, 1);
    }

    public ProxyBlockPoolBuilder block(BlockState state) {
        return block(state, null, false, true, 1);
    }

    public ProxyBlockPoolBuilder block(Block block, int weight) {
        return block(block.defaultBlockState(), null, false, true, weight);
    }

    public ProxyBlockPoolBuilder block(BlockState state, int weight) {
        return block(state, null, false, true, weight);
    }

    public ProxyBlockPoolBuilder block(Block block, boolean copyFacing) {
        return block(block.defaultBlockState(), null, copyFacing, true, 1);
    }

    public ProxyBlockPoolBuilder block(Block block, boolean copyFacing, int weight) {
        return block(block.defaultBlockState(), null, copyFacing, true, weight);
    }

    public ProxyBlockPoolBuilder block(Block block, String nbt, int weight) {
        return block(block.defaultBlockState(), nbt, false, true, weight);
    }

    public ProxyBlockPoolBuilder block(Block block, String nbt, boolean copyFacing, boolean checkNeighbors, int weight) {
        return block(block.defaultBlockState(), nbt, copyFacing, checkNeighbors, weight);
    }

    public ProxyBlockPoolBuilder block(BlockState blockState, String nbt, boolean copyFacing, boolean checkNeighbors, int weight) {
        commit();
        pendingAction = new PlaceBlockAction(blockState, parseNbt(nbt), copyFacing, checkNeighbors, List.of());
        pendingWeight = weight;
        return this;
    }

    public ProxyBlockPoolBuilder run(Holder<ProxyBlockPool> pool, int weight) {
        commit();
        pendingAction = new RunPoolAction(pool);
        pendingWeight = weight;
        return this;
    }

    public ProxyBlockPoolBuilder fallback(Block block) {
        return fallback(block.defaultBlockState());
    }

    public ProxyBlockPoolBuilder fallback(BlockState blockState) {
        fallback = blockState;
        return this;
    }

    private void commit() {
        if (pendingAction != null) {
            entries.add(pendingAction, pendingWeight);
            pendingAction = null;
        }
    }

    public ProxyBlockPool build() {
        commit();
        return new ProxyBlockPool(fallback, entries.build());
    }

    private static Optional<CompoundTag> parseNbt(String nbt) {
        if (nbt == null || nbt.isEmpty()) return Optional.empty();
        try {
            return Optional.of(TagParser.parseCompoundFully(nbt));
        } catch (Exception e) {
            throw new RuntimeException("ProxyBlock NBT parse failed: " + nbt, e);
        }
    }
}