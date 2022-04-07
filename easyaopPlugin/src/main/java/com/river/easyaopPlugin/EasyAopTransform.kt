package com.river.easyaopPlugin

import com.android.build.api.transform.Transform
import com.android.build.api.transform.TransformInvocation
import com.android.build.gradle.internal.pipeline.TransformManager
import com.river.easyaopPlugin.collectAnno.CollectAnnoClassVisitor
import com.river.easyaopPlugin.collectLambda.CollectLambdaClassVisitor
import com.river.easyaopPlugin.entry.MethodPointCut
import com.river.easyaopPlugin.implantCode.ImplantCodeClassVisitor
import com.river.easyaopPlugin.util.TransformUtil

/**
 * 插件transform基础类，实现增量更新，接收实现类的classVisitor进行注入处理
 */
class EasyAopTransform : Transform() {
    private var methodPointCuts = mutableListOf<MethodPointCut>()

    /**
     * 处理打包流程中文件及jar包
     * @param transformInvocation TransformInvocation
     */
    override fun transform(transformInvocation: TransformInvocation) {
        val incremental = transformInvocation.isIncremental
        val inputs = transformInvocation.inputs
        val outputProvider = transformInvocation.outputProvider

        if (!incremental) {
            outputProvider.deleteAll()
        }

        //收集注解信息
        for (input in inputs) {
            for (jarInput in input.jarInputs) {
                TransformUtil.handleJarInput(jarInput, outputProvider, incremental, object: TransformUtil.HandleByteCode {
                    override fun handle(bytes: ByteArray): ByteArray {
                        return collectAnno(bytes)
                    }

                    override fun onlyMap() = true
                })
            }

            for (directoryInput in input.directoryInputs) {
                TransformUtil.handleDirInput(directoryInput, outputProvider, incremental, object: TransformUtil.HandleByteCode {
                    override fun handle(bytes: ByteArray): ByteArray {
                        return collectAnno(bytes)
                    }

                    override fun onlyMap() = true
                })
            }
        }

        //收集Lambda
        for (input in inputs) {
            for (jarInput in input.jarInputs) {
                TransformUtil.handleJarInput(jarInput, outputProvider, incremental, object: TransformUtil.HandleByteCode {
                    override fun handle(bytes: ByteArray): ByteArray {
                        return collectLambda(bytes)
                    }

                    override fun onlyMap() = true
                })
            }

            for (directoryInput in input.directoryInputs) {
                TransformUtil.handleDirInput(directoryInput, outputProvider, incremental, object: TransformUtil.HandleByteCode {
                    override fun handle(bytes: ByteArray): ByteArray {
                        return collectLambda(bytes)
                    }

                    override fun onlyMap() = true
                })
            }
        }

        //此次遍历注入代码
        for (input in inputs) {
            for (jarInput in input.jarInputs) {
                TransformUtil.handleJarInput(jarInput, outputProvider, incremental, object: TransformUtil.HandleByteCode {
                    override fun handle(bytes: ByteArray): ByteArray {
                        return handleByteCode(bytes)
                    }
                })
            }

            for (directoryInput in input.directoryInputs) {
                TransformUtil.handleDirInput(directoryInput, outputProvider, incremental, object: TransformUtil.HandleByteCode {
                    override fun handle(bytes: ByteArray): ByteArray {
                        return handleByteCode(bytes)
                    }
                })
            }
        }
    }

    /**
     * 收集注解
     * @param bytes ByteArray
     * @return ByteArray
     */
    private fun collectAnno(bytes: ByteArray): ByteArray {
        val methodPointCuts = CollectAnnoClassVisitor.collect(bytes)
        if (methodPointCuts.isNotEmpty()) {
            this.methodPointCuts.addAll(methodPointCuts)
        }
        return bytes
    }

    /**
     * 收集lambda
     * @param bytes ByteArray
     * @return ByteArray
     */
    private fun collectLambda(bytes: ByteArray): ByteArray {
        CollectLambdaClassVisitor.collect(bytes, methodPointCuts)
        return bytes
    }

    /**
     * 处理字节码
     * @param bytes ByteArray
     * @return ByteArray
     */
    private fun handleByteCode(bytes: ByteArray): ByteArray {
        return ImplantCodeClassVisitor.implantCode(bytes, methodPointCuts)
    }

    override fun getName() = "EasyAopPluginTransform"

    /**
     * 处理输入类型
     */
    override fun getInputTypes() = TransformManager.CONTENT_CLASS

    /**
     * 处理输入范围
     */
    override fun getScopes() = TransformManager.SCOPE_FULL_PROJECT

    /**
     * 支持增量更新
     * @return Boolean
     */
    override fun isIncremental() = true
}