package com.technovision.ironchest.blocks;

import com.technovision.ironchest.blocks.blockentities.CrystalChestBlockEntity;
import com.technovision.ironchest.registry.ModBlockEntityType;
import net.fabricmc.fabric.api.object.builder.v1.block.FabricBlockSettings;
import net.fabricmc.fabric.api.tool.attribute.v1.FabricToolTags;
import net.minecraft.block.Material;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.sound.BlockSoundGroup;
import net.minecraft.world.BlockView;

public class CrystalChestBlock extends GenericIronChestBlock {

    public CrystalChestBlock() {
        super(FabricBlockSettings.of(Material.STONE)
                        .hardness(3.0F)
                        .resistance(3.0F)
                        .sounds(BlockSoundGroup.STONE)
                        .breakByTool(FabricToolTags.PICKAXES, 0)
                        .requiresTool(),
                ExtraChestTypes.CRYSTAL,
                () -> ModBlockEntityType.CRYSTAL_CHEST);
    }

    @Override
    public BlockEntity createBlockEntity(BlockView world) {
        return new CrystalChestBlockEntity();
    }
}
