package com.river.easyaopPlugin.implantCode;

import com.river.easyaopPlugin.util.ClassTypes;
import com.river.easyaopPlugin.entry.MethodPointCut;

import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.commons.AdviceAdapter;

import java.util.List;

/**
 * @Author: River
 * @Emial: 1632958163@qq.com
 * @Create: 2022/4/1
 * @Desc: 将MethodPointCut收集的操作码、操作数炽入方法体内
 **/
public class ImplantCodeMethodVisitor extends AdviceAdapter {
    private List<MethodPointCut> methodPointCuts;
    private Label handler;

    protected ImplantCodeMethodVisitor(MethodVisitor methodVisitor, int access, String name, String descriptor, List<MethodPointCut> methodPointCuts) {
        super(Opcodes.ASM6, methodVisitor, access, name, descriptor);
        this.methodPointCuts = methodPointCuts;
    }

    @Override
    protected void onMethodEnter() {
        for (MethodPointCut methodPointCut : methodPointCuts) {
            implantBeforeCode(methodPointCut);
        }
        super.onMethodEnter();
    }

    @Override
    public void visitTryCatchBlock(Label start, Label end, Label handler, String type) {
        super.visitTryCatchBlock(start, end, handler, type);
        this.handler = handler;
    }

    @Override
    public void visitLabel(Label label) {
        super.visitLabel(label);
        if (label == handler) {
            for (MethodPointCut methodPointCut : methodPointCuts) {
                if (methodPointCut.getAnnoClass().equals(ClassTypes.AfterThrowing)) {
                    implantAfterCode(methodPointCut);
                }
            }
        }
    }

    @Override
    protected void onMethodExit(int opcode) {
        for (MethodPointCut methodPointCut : methodPointCuts) {
            if (!methodPointCut.getAnnoClass().equals(ClassTypes.AfterThrowing)) {
                implantAfterCode(methodPointCut);
            }
        }
        super.onMethodExit(opcode);
    }

    private void implantBeforeCode(MethodPointCut methodPointCut) {
        if (methodPointCut.getBeforeInstructions() != null) {
            methodPointCut.getBeforeInstructions().accept(this);
        }
    }


    private void implantAfterCode(MethodPointCut methodPointCut) {
        if (methodPointCut.getAfterInstructions() != null) {
            methodPointCut.getAfterInstructions().accept(this);
        }
    }
}
