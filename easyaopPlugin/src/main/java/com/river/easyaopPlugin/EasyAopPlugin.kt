package com.river.easyaopPlugin

import com.android.build.gradle.AppExtension
import org.gradle.api.Plugin
import org.gradle.api.Project

/**
 * 插件入口
 */
class EasyAopPlugin : Plugin<Project> {
    override fun apply(project: Project) {
        val appExtension = project.extensions.findByType(AppExtension::class.java)
        appExtension?.registerTransform(EasyAopTransform())
    }
}