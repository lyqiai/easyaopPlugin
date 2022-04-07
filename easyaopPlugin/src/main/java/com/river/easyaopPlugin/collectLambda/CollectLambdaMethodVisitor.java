package com.river.easyaopPlugin.collectLambda;

import com.river.easyaopPlugin.entry.MethodPointCut;
import com.river.easyaopPlugin.util.Util;

import org.objectweb.asm.Handle;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

import java.util.List;

/**
 * @Author: River
 * @Emial: 1632958163@qq.com
 * @Create: 2022/4/1
 * @Desc: 通过劫持visitInvokeDynamicInsn指令匹配MethodPointCut方法的lambda方法，收集的lambda存储与MethodPointCut.lambdas字段
 **/
public class CollectLambdaMethodVisitor extends MethodVisitor {
    private CollectLambdaClassVisitor cv;
    private List<MethodPointCut> methodPointCuts;

    public CollectLambdaMethodVisitor(MethodVisitor methodVisitor, CollectLambdaClassVisitor cv, List<MethodPointCut> methodPointCuts) {
        super(Opcodes.ASM6, methodVisitor);
        this.cv = cv;
        this.methodPointCuts = methodPointCuts;
    }

    @Override
    public void visitInvokeDynamicInsn(String name, String descriptor, Handle bootstrapMethodHandle, Object... bootstrapMethodArguments) {
        super.visitInvokeDynamicInsn(name, descriptor, bootstrapMethodHandle, bootstrapMethodArguments);
        Type returnType = Type.getReturnType(descriptor);
        for (MethodPointCut methodPointCut : methodPointCuts) {
            boolean matcher = Util.matcherMethodDesc(methodPointCut, returnType.getInternalName(), name, descriptor);
            if (matcher) {
                for (Object bootstrapMethodArgument : bootstrapMethodArguments) {
                    if (bootstrapMethodArgument instanceof Handle) {
                        methodPointCut.getLambdas().add((Handle) bootstrapMethodArgument);
                    }
                }
            }
        }
    }
}
