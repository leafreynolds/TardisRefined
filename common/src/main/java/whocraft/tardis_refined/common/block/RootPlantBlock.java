package whocraft.tardis_refined.common.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import whocraft.tardis_refined.common.block.shell.RootedShellBlock;
import whocraft.tardis_refined.common.blockentity.shell.RootPlantBlockEntity;
import whocraft.tardis_refined.common.capability.TardisLevelOperator;
import whocraft.tardis_refined.common.tardis.manager.TardisFlightEventManager;
import whocraft.tardis_refined.registry.BlockRegistry;
import whocraft.tardis_refined.registry.DimensionTypes;


public class RootPlantBlock extends BaseEntityBlock implements SimpleWaterloggedBlock{

    public static final DirectionProperty FACING = BlockStateProperties.FACING;
    public static final IntegerProperty AGE = BlockStateProperties.AGE_5;
    public static final BooleanProperty WATERLOGGED = BlockStateProperties.WATERLOGGED;

    public RootPlantBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(AGE, 0).setValue(WATERLOGGED, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.@NotNull Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(FACING, AGE, WATERLOGGED);
    }

    public IntegerProperty getAgeProperty() {
        return AGE;
    }

    public int getMaxAge() {
        return 5;
    }

    protected int getAge(BlockState blockState) {
        return blockState.getValue(this.getAgeProperty());
    }

    public boolean isMaxAge(BlockState blockState) {
        return blockState.getValue(this.getAgeProperty()) >= this.getMaxAge();
    }


    @Override
    public BlockState getStateForPlacement(@NotNull BlockPlaceContext context) {
        BlockState state = super.getStateForPlacement(context);
        FluidState fluidState = context.getLevel().getFluidState(context.getClickedPos());
        boolean waterlogged = fluidState.getType() == Fluids.WATER;
        return state.setValue(FACING, context.getHorizontalDirection()).setValue(this.getAgeProperty(), getAge(state)).setValue(WATERLOGGED, waterlogged);
    }

    private BlockState getStateForAging(int ageValue, Direction facing) {
        return this.defaultBlockState().setValue(this.getAgeProperty(), ageValue).setValue(FACING, facing);
    }

    @Override
    public boolean isRandomlyTicking(BlockState blockState) {
        return !this.isMaxAge(blockState);
    }

    @Override
    public void randomTick(BlockState blockState, ServerLevel serverLevel, BlockPos blockPos, RandomSource randomSource) {
        int age = this.getAge(blockState);
        Direction facing = blockState.getValue(FACING);
        if (age < this.getMaxAge()) {

            if (serverLevel.getBlockState(blockPos.below()).getBlock() == Blocks.MAGMA_BLOCK) {
                if (randomSource.nextInt(6) == 0) {
                    FluidState fluidState = serverLevel.getFluidState(blockPos);
                    boolean waterlogged = fluidState.getType() == Fluids.WATER;
                    if (age + 1 == this.getMaxAge()) {
                        serverLevel.removeBlock(blockPos, waterlogged); //Use removeBlock to allow us to keep any water source blocks since root block is now waterloggable.
                        serverLevel.setBlock(blockPos, BlockRegistry.ROOT_SHELL_BLOCK.get().defaultBlockState().setValue(RootedShellBlock.FACING, facing).setValue(WATERLOGGED, waterlogged), 3);
                    } else {
                        serverLevel.removeBlock(blockPos, waterlogged); //Use removeBlock to allow us to keep any water source blocks since root block is now waterloggable.
                        serverLevel.setBlock(blockPos, this.getStateForAging(age + 1, facing).setValue(WATERLOGGED, waterlogged), 3);
                    }

                    serverLevel.playSound(null, blockPos, SoundEvents.GROWING_PLANT_CROP, SoundSource.BLOCKS, 1, 1);
                }
            }
        }
    }

    @Override
    public @NotNull BlockState rotate(BlockState state, Rotation rot) {
        return state.setValue(FACING, rot.rotate(state.getValue(FACING)));
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos blockPos, BlockState blockState) {
        return new RootPlantBlockEntity(blockPos, blockState);
    }

    @Override
    public VoxelShape getShape(BlockState blockState, BlockGetter blockGetter, BlockPos blockPos, CollisionContext collisionContext) {

        return Block.box(5f, 0f, 5f, 11f, 5f, 11f);
    }

    @Override
    public void onPlace(BlockState blockState, Level level, BlockPos blockPos, BlockState blockState2, boolean bl) {

        if(level instanceof ServerLevel serverLevel && level.dimensionTypeId() == DimensionTypes.TARDIS){
            TardisLevelOperator.get(serverLevel).ifPresent(TardisFlightEventManager::playCloisterBell);
            level.removeBlock(blockPos, false);
            ItemEntity item = new ItemEntity(EntityType.ITEM, level);
            item.setItem(new ItemStack(BlockRegistry.ROOT_PLANT_BLOCK.get()));
            item.setPos(blockPos.getX(), blockPos.getY(), blockPos.getZ());
            level.addFreshEntity(item);
            return;
        }

        super.onPlace(blockState, level, blockPos, blockState2, bl);

        if (level.getBlockState(blockPos.below()).getBlock() == Blocks.MAGMA_BLOCK) {
            level.playSound(null, blockPos, SoundEvents.FIRE_EXTINGUISH, SoundSource.BLOCKS, 1, 1.25f);
        }
    }

    @Override
    public BlockState updateShape(BlockState blockState, Direction direction, BlockState blockState2, LevelAccessor levelAccessor, BlockPos blockPos, BlockPos blockPos2) {
        if (blockState.getValue(WATERLOGGED)){
            levelAccessor.scheduleTick(blockPos, Fluids.WATER, Fluids.WATER.getTickDelay(levelAccessor));
        }
        return super.updateShape(blockState, direction, blockState2, levelAccessor, blockPos, blockPos2);
    }

    @Override
    public FluidState getFluidState(BlockState blockState) {
        return blockState.getValue(WATERLOGGED) ? Fluids.WATER.getSource(false) : super.getFluidState(blockState);
    }
}