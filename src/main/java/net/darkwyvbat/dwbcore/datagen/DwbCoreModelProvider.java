package net.darkwyvbat.dwbcore.datagen;

import net.darkwyvbat.dwbcore.world.block.DwbBlocks;
import net.darkwyvbat.dwbcore.world.block.ProxyBlock;
import net.fabricmc.fabric.api.client.datagen.v1.provider.FabricModelProvider;
import net.fabricmc.fabric.api.datagen.v1.FabricPackOutput;
import net.minecraft.client.data.models.BlockModelGenerators;
import net.minecraft.client.data.models.ItemModelGenerators;
import net.minecraft.client.data.models.blockstates.MultiVariantGenerator;
import net.minecraft.client.data.models.blockstates.PropertyDispatch;
import net.minecraft.client.data.models.model.ModelTemplates;
import net.minecraft.client.data.models.model.TextureMapping;
import net.minecraft.client.data.models.model.TextureSlot;
import net.minecraft.client.renderer.block.model.Variant;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;

import java.util.EnumMap;
import java.util.Map;

import static net.darkwyvbat.dwbcore.DwbCore.INFO;

public class DwbCoreModelProvider extends FabricModelProvider {

    public DwbCoreModelProvider(FabricPackOutput output) {
        super(output);
    }

    @Override
    public void generateBlockStateModels(BlockModelGenerators blockModelGenerators) {
        registerProxyBlock(blockModelGenerators);
    }

    @Override
    public void generateItemModels(ItemModelGenerators itemModelGenerators) {

    }

    public void registerProxyBlock(BlockModelGenerators blockStateModelGenerator) {
        Map<ProxyBlock.Appearance, Identifier> appearanceModels = new EnumMap<>(ProxyBlock.Appearance.class);
        String id = BuiltInRegistries.BLOCK.getKey(DwbBlocks.PROXY_BLOCK).getPath();
        for (ProxyBlock.Appearance appearance : ProxyBlock.Appearance.values()) {
            String appearanceName = appearance.getSerializedName();
            Identifier sideTexture = INFO.id("block/" + id + "/" + appearanceName + "_side");
            Identifier frontTexture = INFO.id("block/" + id + "/" + appearanceName + "_front");
            Identifier model = INFO.id("block/" + id + "/" + appearanceName);
            TextureMapping textureMapping = new TextureMapping().put(TextureSlot.SIDE, sideTexture).put(TextureSlot.FRONT, frontTexture).put(TextureSlot.TOP, sideTexture);
            ModelTemplates.CUBE_ORIENTABLE.create(model, textureMapping, blockStateModelGenerator.modelOutput);
            appearanceModels.put(appearance, model);
        }
        blockStateModelGenerator.blockStateOutput.accept(MultiVariantGenerator.dispatch(DwbBlocks.PROXY_BLOCK)
                .with(PropertyDispatch.initial(ProxyBlock.APPEARANCE_PROPERTY, ProxyBlock.FACING_PROPERTY).generate((a, d) -> {
                            Variant variant = BlockModelGenerators.plainModel(appearanceModels.get(a));
                            return BlockModelGenerators.variant(switch (d) {
                                case NORTH -> variant;
                                case EAST -> variant.with(BlockModelGenerators.Y_ROT_90);
                                case SOUTH -> variant.with(BlockModelGenerators.Y_ROT_180);
                                case WEST -> variant.with(BlockModelGenerators.Y_ROT_270);
                                case UP -> variant.with(BlockModelGenerators.X_ROT_270);
                                case DOWN -> variant.with(BlockModelGenerators.X_ROT_90);
                            });
                        })
                )
        );

        blockStateModelGenerator.registerSimpleItemModel(DwbBlocks.PROXY_BLOCK, appearanceModels.get(ProxyBlock.Appearance.GENERIC));
    }
}