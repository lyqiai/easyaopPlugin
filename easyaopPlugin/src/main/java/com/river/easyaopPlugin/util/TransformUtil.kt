package com.river.easyaopPlugin.util

import com.android.build.api.transform.*
import org.apache.commons.codec.digest.DigestUtils
import org.apache.commons.io.FileUtils
import org.apache.commons.io.IOUtils
import java.io.File
import java.io.FileOutputStream
import java.util.jar.JarEntry
import java.util.jar.JarFile
import java.util.jar.JarOutputStream
import java.util.zip.ZipEntry

/**
 * @Author: River
 * @Emial: 1632958163@qq.com
 * @Create: 2021/11/9
 **/
object TransformUtil {
    /**
     * 处理JAR包
     * @param jarInput JarInput
     * @param outputProvider TransformOutputProvider
     * @param incremental Boolean
     * @param callback HandleJarInputCallback
     */
    fun handleJarInput(
        jarInput: JarInput,
        outputProvider: TransformOutputProvider,
        incremental: Boolean,
        callback: HandleByteCode
    ) {
        if (incremental) {
            when (jarInput.status) {
                Status.NOTCHANGED -> {
                }
                Status.ADDED, Status.CHANGED -> {
                    handleJarInputInsert(jarInput, outputProvider, callback)
                }
                Status.REMOVED -> {
                    val dest = outputProvider.getContentLocation(
                        jarInput.name,
                        jarInput.contentTypes,
                        jarInput.scopes,
                        Format.JAR
                    )
                    if (dest.exists())
                        FileUtils.forceDelete(dest)
                }
            }
        } else {
            handleJarInputInsert(jarInput, outputProvider, callback)
        }
    }

    /**
     * 处理新增的jar
     * @param jarInput JarInput
     * @param outputProvider TransformOutputProvider
     */
    private fun handleJarInputInsert(jarInput: JarInput, outputProvider: TransformOutputProvider, callback: HandleByteCode) {
        if (!jarInput.file.absolutePath.endsWith(".jar")) return

        if (callback.onlyMap()) {
            val jarFile = JarFile(jarInput.file)
            val enumeration = jarFile.entries()
            while (enumeration.hasMoreElements()) {
                val jarEntry = enumeration.nextElement() as JarEntry
                val entryName = jarEntry.name
                val zipEntry = ZipEntry(entryName)
                val inputStream = jarFile.getInputStream(zipEntry)

                if (filterFile(entryName)) {
                    callback.handle(inputStream.readBytes())
                }

                inputStream.close()
            }
            jarFile.close()
        } else {
            var jarName = jarInput.name
            val md5Name = DigestUtils.md5Hex(jarInput.file.absolutePath)
            if (jarName.endsWith(".jar")) {
                jarName = jarName.substring(0, jarName.length - 4)
            }
            val jarFile = JarFile(jarInput.file)
            val enumeration = jarFile.entries()
            val tempFile = File("${jarInput.file.parent}${File.separator}temp.jar")
            if (tempFile.exists()) {
                tempFile.delete()
            }
            val jarOutputStream = JarOutputStream(FileOutputStream(tempFile))
            while (enumeration.hasMoreElements()) {
                val jarEntry = enumeration.nextElement() as JarEntry
                val entryName = jarEntry.name
                val zipEntry = ZipEntry(entryName)
                val inputStream = jarFile.getInputStream(zipEntry)
                if (filterFile(entryName)) {
                    jarOutputStream.putNextEntry(zipEntry)
                    jarOutputStream.write(callback.handle(inputStream.readBytes()))
                } else {
                    jarOutputStream.putNextEntry(zipEntry)
                    jarOutputStream.write(IOUtils.toByteArray(inputStream))
                }
                jarOutputStream.closeEntry()
            }

            jarOutputStream.close()
            jarFile.close()
            val dest = outputProvider.getContentLocation(
                jarName + md5Name,
                jarInput.contentTypes,
                jarInput.scopes,
                Format.JAR
            )
            FileUtils.copyFile(tempFile, dest)
            tempFile.delete()
        }
    }

    /**
     * 处理文件
     * @param directoryInput DirectoryInput
     * @param outputProvider TransformOutputProvider
     * @param incremental Boolean
     */
    fun handleDirInput(
        directoryInput: DirectoryInput,
        outputProvider: TransformOutputProvider,
        incremental: Boolean,
        callback: HandleByteCode
    ) {
        if (incremental) {
            if (callback.onlyMap()) {
                directoryInput.changedFiles.forEach { changeFile ->
                    when (changeFile.value) {
                        Status.NOTCHANGED -> { }
                        Status.REMOVED -> { }
                        Status.ADDED, Status.CHANGED -> {
                            if (filterFile(changeFile.key.name)) {
                                callback.handle(changeFile.key.readBytes())
                            }
                        }
                    }
                }
            } else {
                val dest = outputProvider.getContentLocation(
                    directoryInput.name,
                    directoryInput.contentTypes,
                    directoryInput.scopes,
                    Format.DIRECTORY
                )
                FileUtils.forceMkdir(dest)
                val srcDirPath = directoryInput.file.absolutePath
                val destDirPath = dest.absolutePath
                directoryInput.changedFiles.forEach { changeFile ->
                    val destFile = File(changeFile.key.absolutePath.replace(srcDirPath, destDirPath))
                    when (changeFile.value) {
                        Status.NOTCHANGED -> {
                        }
                        Status.REMOVED -> {
                            if (destFile.exists())
                                destFile.delete()
                        }
                        Status.ADDED, Status.CHANGED -> {
                            try {
                                FileUtils.touch(destFile)
                            } catch (e: Exception) {

                            }
                            if (filterFile(changeFile.key.name)) {
                                FileUtils.writeByteArrayToFile(
                                    destFile,
                                    callback.handle(changeFile.key.readBytes())
                                )
                            } else {
                                if (changeFile.key.isFile) {
                                    FileUtils.touch(destFile)
                                    FileUtils.copyFile(changeFile.key, destFile)
                                }
                            }
                        }
                    }
                }
            }
        } else {
            handleFullDirInput(directoryInput, outputProvider, callback)
        }
    }

    /**
     * 全量处理文件
     * @param dirInput DirectoryInput
     * @param outputProvider TransformOutputProvider
     */
    private fun handleFullDirInput(
        dirInput: DirectoryInput,
        outputProvider: TransformOutputProvider,
        callback: HandleByteCode
    ) {
        if (callback.onlyMap()) {
            if (dirInput.file.isDirectory) {
                com.android.utils.FileUtils.getAllFiles(dirInput.file).forEach { file ->
                    if (filterFile(file.name)) {
                        callback.handle(file.readBytes())
                    }
                }
            }
        } else {
            if (dirInput.file.isDirectory) {
                com.android.utils.FileUtils.getAllFiles(dirInput.file).forEach { file ->
                    if (filterFile(file.name)) {
                        FileUtils.writeByteArrayToFile(file, callback.handle(file.readBytes()))
                    }
                }
            }

            val dest = outputProvider.getContentLocation(
                dirInput.name,
                dirInput.contentTypes,
                dirInput.scopes,
                Format.DIRECTORY
            )

            FileUtils.copyDirectory(dirInput.file, dest)
        }
    }


    /**
     * 定义过滤文件规则
     * @param fileName String
     * @return Boolean
     */
    private fun filterFile(fileName: String): Boolean {
        return fileName.endsWith(".class") &&
                !fileName.contains("R\$") &&
                !fileName.endsWith("/R") &&
                !fileName.startsWith("android/support") &&
                !fileName.startsWith("androidx/") &&
                !fileName.startsWith("com/google") &&
                !fileName.startsWith("kotlin") &&
                !fileName.startsWith("kotlinx") &&
                !fileName.startsWith("org/intellij") &&
                !fileName.startsWith("org/jetbrains") &&
                fileName != "R.class" &&
                fileName != "R2.class" &&
                fileName != "BuildConfig.class"
    }

    interface HandleByteCode {
        fun handle(bytes: ByteArray): ByteArray

        fun onlyMap(): Boolean = false
    }
}