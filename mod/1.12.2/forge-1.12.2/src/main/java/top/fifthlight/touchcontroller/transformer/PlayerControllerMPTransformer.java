package top.fifthlight.touchcontroller.transformer;

import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

public class PlayerControllerMPTransformer extends TouchControllerClassVisitor {
    public PlayerControllerMPTransformer(ClassVisitor classVisitor) {
        super(Opcodes.ASM5, classVisitor);
    }

    @Override
    public String getClassName() {
        return "net.minecraft.client.multiplayer.PlayerControllerMP";
    }

    @Override
    public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
        boolean isProcessRightClickBlock = "processRightClickBlock".equals(name) || "func_187099_a".equals(mapSelfMethodName(name, desc));
        boolean isProcessRightClick = "processRightClick".equals(name) || "func_187101_a".equals(mapSelfMethodName(name, desc));

        if (isProcessRightClickBlock || isProcessRightClick) {
            return new MethodVisitor(Opcodes.ASM5, super.visitMethod(access, name, desc, signature, exceptions)) {
                // Call PlayerControllerMPHelper.beforeUsingItem at the head of method
                @Override
                public void visitCode() {
                    if (isProcessRightClickBlock) {
                        visitVarInsn(Opcodes.ALOAD, 0);
                        visitVarInsn(Opcodes.ALOAD, 1);
                        visitVarInsn(Opcodes.ALOAD, 6);
                    } else {
                        visitVarInsn(Opcodes.ALOAD, 0);
                        visitVarInsn(Opcodes.ALOAD, 1);
                        visitVarInsn(Opcodes.ALOAD, 3);
                    }
                    super.visitMethodInsn(Opcodes.INVOKESTATIC, "top/fifthlight/touchcontroller/helper/PlayerControllerMPHelper", "beforeUsingItem", "(Lnet/minecraft/client/multiplayer/PlayerControllerMP;Lnet/minecraft/entity/player/EntityPlayer;Lnet/minecraft/util/EnumHand;)V", false);
                    super.visitCode();
                }

                // Call PlayerControllerMPHelper.afterUsingItem before every return;
                @Override
                public void visitInsn(int opcode) {
                    if (opcode == Opcodes.ARETURN) {
                        visitVarInsn(Opcodes.ALOAD, 0);
                        visitVarInsn(Opcodes.ALOAD, 1);
                        super.visitMethodInsn(Opcodes.INVOKESTATIC, "top/fifthlight/touchcontroller/helper/PlayerControllerMPHelper", "afterUsingItem", "(Lnet/minecraft/client/multiplayer/PlayerControllerMP;Lnet/minecraft/entity/player/EntityPlayer;)V", false);
                    }
                    super.visitInsn(opcode);
                }
            };
        } else {
            return super.visitMethod(access, name, desc, signature, exceptions);
        }
    }
}
