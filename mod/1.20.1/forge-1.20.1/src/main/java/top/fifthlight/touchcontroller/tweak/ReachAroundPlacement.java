package top.fifthlight.touchcontroller.tweak;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/**
 * 基岩版「四周放置」（贴边放置）。
 *
 * <p>Java 版放方块必须把准星压在某个方块的某个面上；基岩版给触屏玩家多留了一条顺手路：
 * 站在方块边缘、低头看向前方空地时，只要手里拿着方块，就替你往脚下前方那一格贴一块。</p>
 *
 * <p>判定照搬基岩版/BedrockIfy：手里是方块、准星是空气、人踩在地面实心方块上、低头超过 25 度、
 * 人贴着脚下方块朝前那条边（不超过 0.5 格）、前方那一格是空的。命中面取朝向的反面，
 * 只影响楼梯、活板门这类方块的朝向。</p>
 */
public final class ReachAroundPlacement {
    /** 低头角度阈值（度）：低于这个角度不触发，避免走路时误放。 */
    private static final float PITCH_THRESHOLD = 25.0F;

    /** 离脚下方块边缘多近才算「站在边上」（格）。 */
    private static final double EDGE_DISTANCE = 0.5D;

    private ReachAroundPlacement() {
    }

    /**
     * 当前这只手能不能四周放置，能就给出应当当作准星命中的那个方块边。
     *
     * @return 命中结果；不满足条件时返回 {@code null}
     */
    public static BlockHitResult computeTarget(LocalPlayer player) {
        if (player == null || player.isSpectator() || !player.onGround()) {
            return null;
        }
        Level level = player.level();
        BlockPos standing = player.getOnPos();
        if (!isSolid(level.getBlockState(standing))) {
            return null;
        }
        if (player.getXRot() <= PITCH_THRESHOLD) {
            return null;
        }
        if (!isNearEdge(player)) {
            return null;
        }
        Direction facing = player.getDirection();
        BlockPos target = standing.relative(facing);
        if (isSolid(level.getBlockState(target))) {
            return null;
        }
        Vec3 point = new Vec3(
                player.getX() + facing.getStepX(),
                player.getY(),
                player.getZ() + facing.getStepZ());
        return new BlockHitResult(point, facing.getOpposite(), target, false);
    }

    /**
     * 供使用流程调用：准星是空气、且这只手拿着方块时，返回贴边命中。
     */
    public static BlockHitResult find(LocalPlayer player, ItemStack heldStack) {
        if (!isBlockItem(heldStack)) {
            return null;
        }
        HitResult hit = Minecraft.getInstance().hitResult;
        if (hit != null && hit.getType() != HitResult.Type.MISS) {
            return null;
        }
        return computeTarget(player);
    }

    /** 这只手拿的是不是方块。 */
    public static boolean isBlockItem(ItemStack stack) {
        return stack != null && !stack.isEmpty() && stack.getItem() instanceof BlockItem;
    }

    private static boolean isSolid(BlockState state) {
        return !state.isAir() && !(state.getBlock() instanceof LiquidBlock);
    }

    private static boolean isNearEdge(LocalPlayer player) {
        Direction facing = player.getDirection();
        return nearEdge(player.getX() - player.blockPosition().getX(), facing.getStepX())
                || nearEdge(player.getZ() - player.blockPosition().getZ(), facing.getStepZ());
    }

    private static boolean nearEdge(double offset, int step) {
        if (step > 0) {
            return 1.0D - offset < EDGE_DISTANCE;
        }
        if (step < 0) {
            return offset < EDGE_DISTANCE;
        }
        return false;
    }
}
