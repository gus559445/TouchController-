package top.fifthlight.touchcontroller.mixin;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.AttackIndicatorStatus;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import org.koin.java.KoinJavaComponent;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import top.fifthlight.touchcontroller.common.event.RenderEvents;
import top.fifthlight.touchcontroller.common.layout.CrosshairStatus;
import top.fifthlight.touchcontroller.common.model.ControllerHudModel;

@Mixin(Gui.class)
public abstract class InGameHudMixin {
    @Shadow
    @Final
    protected static ResourceLocation GUI_ICONS_LOCATION;

    @Shadow
    protected int screenWidth;

    @Shadow
    protected int screenHeight;

    @Shadow
    @Final
    protected Minecraft minecraft;

    @Inject(
            method = "renderCrosshair",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/gui/GuiGraphics;blit(Lnet/minecraft/resources/ResourceLocation;IIIIII)V",
                    ordinal = 0
            ),
            cancellable = true
    )
    private void renderCrosshair(GuiGraphics context, CallbackInfo callbackInfo) {
        boolean shouldRender = RenderEvents.INSTANCE.shouldRenderCrosshair();
        if (!shouldRender) {
            // 指示器画在**触控圈**上：原版准星已经被 TouchController 隐藏，玩家真正瞄的是手指
            // 下面那个圆环，画在屏幕中心既看不到、也不对应玩家瞄的东西。
            int x = screenWidth / 2;
            int y = screenHeight / 2;
            CrosshairStatus status = ((ControllerHudModel) KoinJavaComponent.get(ControllerHudModel.class))
                    .getResult().getCrosshairStatus();
            if (status != null) {
                x = (int) (status.getPosition().getX() * screenWidth);
                y = (int) (status.getPosition().getY() * screenHeight);
            }

            // 攻击冷却：准星下面那个横条 / 冷却好了换成刀图标。
            // 不读 options.attackIndicator()：MCPE 风格模组（mcpemotion 的 HideAttackIndicatorMixin）
            // 会在渲染期间把它临时改成 HIDDEN，照原版判断的话这里永远画不出来。
            float attackCooldownProgress = this.minecraft.player.getAttackStrengthScale(0.0f);
            boolean renderFullTexture = false;
            if (this.minecraft.crosshairPickEntity != null && this.minecraft.crosshairPickEntity instanceof LivingEntity && attackCooldownProgress >= 1.0f) {
                renderFullTexture = this.minecraft.player.getCurrentItemAttackStrengthDelay() > 5.0f && this.minecraft.crosshairPickEntity.isAlive();
            }
            if (renderFullTexture) {
                context.blit(GUI_ICONS_LOCATION, x - 8, y - 8, 68, 94, 16, 16);
            } else if (attackCooldownProgress < 1.0f) {
                int progress = (int) (attackCooldownProgress * 17.0f);
                context.blit(GUI_ICONS_LOCATION, x - 8, y - 2, 36, 94, 16, 4);
                context.blit(GUI_ICONS_LOCATION, x - 8, y - 2, 52, 94, progress, 4);
            }

            // 蓄力进度（弓 / 弩 / 三叉戟）：原版没有独立进度条，这里在同一个位置补一条。
            ItemStack useItem = this.minecraft.player.getUseItem();
            int useDuration = useItem.isEmpty() ? 0 : useItem.getUseDuration();
            if (this.minecraft.player.isUsingItem() && useDuration > 0) {
                float charge = 1.0f - (float) this.minecraft.player.getUseItemRemainingTicks() / (float) useDuration;
                if (charge > 0.0f) {
                    charge = Math.min(charge, 1.0f);
                    int barY = y + 8;
                    context.fill(x - 8, barY, x + 8, barY + 3, 0x80000000);
                    context.fill(x - 8, barY, x - 8 + (int) (16 * charge), barY + 3, 0xFFFFFFFF);
                }
            }
            RenderSystem.defaultBlendFunc();
            callbackInfo.cancel();
        }
    }

    @Inject(
            method = "renderHotbar",
            at = @At(
                    value = "INVOKE",
                    target = "Lcom/mojang/blaze3d/vertex/PoseStack;popPose()V",
                    ordinal = 0
            )
    )
    private void renderHotbar(float tickDelta, GuiGraphics context, CallbackInfo ci) {
        var player = minecraft.player;
        if (player != null) {
            var controllerHudModel = (ControllerHudModel) KoinJavaComponent.get(ControllerHudModel.class);
            var inventory = controllerHudModel.getResult().getInventory();
            var slots = inventory.getSlots();
            var x = (screenWidth - 182) / 2 + 1;
            var y = screenHeight - 22 + 1;
            for (int i = 0; i < 9; i++) {
                var stack = player.getInventory().getItem(i);
                if (stack.isEmpty()) {
                    continue;
                }
                var slot = slots[i];
                var progress = slot.getProgress();
                var height = (int) (16 * progress);
                context.fill(x + 20 * i + 2, y + 18 - height, x + 20 * i + 18, y + 18, 0xFF00BB00);
            }
        }
    }
}