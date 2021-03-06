package voodoo

import com.fasterxml.jackson.module.kotlin.readValue
import com.github.kittinunf.fuel.httpGet
import com.github.kittinunf.result.Result
import com.xenomachina.argparser.ArgParser
import com.xenomachina.argparser.mainBody
import mu.KLogging
import org.apache.commons.codec.digest.DigestUtils
import voodoo.data.sk.SKPack
import voodoo.data.sk.task.TaskIf
import voodoo.mmc.MMCUtil.selectFeatures
import voodoo.mmc.data.MultiMCPack
import voodoo.mmc.data.PackComponent
import voodoo.util.*
import java.io.File


/**
 * Created by nikky on 01/04/18.
 * @author Nikky
 */

object Hex : KLogging() {
    private val directories = Directories.get(moduleName = "hex")

    @JvmStatic
    fun main(vararg args: String) = mainBody {
        val arguments = Arguments(ArgParser(args))

        arguments.run {

            install(instanceId, instanceDir, minecraftDir)
        }

    }

    fun File.sha1Hex(): String? = DigestUtils.sha1Hex(this.inputStream())

    fun install(instanceId: String, instanceDir: File, minecraftDir: File) {

        val urlFile = instanceDir.resolve("voodoo.url.txt")
        val packUrl = urlFile.readText()
        val (_, _, result) = packUrl.httpGet()
//                .header("User-Agent" to useragent)
                .responseString()

        val modpack: SKPack = when (result) {
            is Result.Success -> {
                jsonMapper.readValue(result.value)
            }
            is Result.Failure -> {
                logger.error("${result.error} could not retrieve pack")
                return
            }
        }

        val oldpackFile = instanceDir.resolve("voodoo.modpack.json")
        val oldpack: SKPack? = if (!oldpackFile.exists())
            null
        else {
            val pack: SKPack = jsonMapper.readValue(oldpackFile)
            logger.info("loaded old pack ${pack.name} ${pack.version}")
            pack
        }

        if (oldpack != null) {
            if (oldpack.version == modpack.version) {
                logger.info("no update required ?")
//                return //TODO: make dialog close continue when no update is required ?
            }
        }

        val forgePrefix = "net.minecraftforge:forge:"
        val (_, _, forgeVersion) = modpack.versionManifest.libraries.find {
            it.name.startsWith(forgePrefix)
        }?.name.let { it ?: "::" }.split(':')

        logger.info("forge version is $forgeVersion")

        // read user input
        val featureJson = instanceDir.resolve("voodoo.features.json")
        val defaults = if (featureJson.exists()) {
            featureJson.readJson()
        } else {
            mapOf<String, Boolean>()
        }
        val features = selectFeatures(modpack.features, defaults, modpack.title.blankOr
                ?: modpack.name, modpack.version)
        featureJson.writeJson(features)

        val objectsUrl = packUrl.substringBeforeLast('/') + "/" + modpack.objectsLocation

        val oldTaskList = oldpack?.tasks?.toMutableList() ?: mutableListOf()

        // iterate new tasks
        for (task in modpack.tasks) {

            val oldTask = oldTaskList.find { it.to == task.to }

            val whenTask = task.`when`
            if (whenTask != null) {
                val download = when (whenTask.`if`) {
                    TaskIf.requireAny -> {
                        whenTask.features.any { features[it] ?: false }
                    }
                }
                if (!download) {
                    logger.info("${whenTask.features} is disabled, skipping download")
                    continue
                }
            }


            val url = if (task.location.startsWith("http")) {
                task.location
            } else {
                "$objectsUrl/${task.location}"
            }
            val target = minecraftDir.resolve(task.to)
            val chunkedHash = task.hash.chunked(6).joinToString("/")
            val cacheFolder = directories.cacheHome.resolve(chunkedHash)

            if (target.exists()) {
                if (oldTask != null) {
                    // file exists already and existed in the last version

                    if (task.userFile && oldTask.userFile) {
                        logger.info("task ${task.to} is a userfile, will not be modified")
                        oldTask.let { oldTaskList.remove(it) }
                        continue
                    }
                    if (oldTask.hash == task.hash && target.isFile && target.sha1Hex() == task.hash) {
                        logger.info("task ${task.to} file did not change and sha1 hash matches")
                        oldTask.let { oldTaskList.remove(it) }
                        continue
                    } else {
                        // mismatching hash.. override file
                        logger.info("task ${task.to} mismatching hash.. reset and override file")
                        oldTask.let { oldTaskList.remove(it) }
                        target.delete()
                        target.parentFile.mkdirs()
                        target.download(url, cacheFolder)
                    }
                } else {
                    // file exists but was not in the last version.. reset to make sure
                    logger.info("task ${task.to} exists but was not in the last version.. reset to make sure")
                    target.delete()
                    target.parentFile.mkdirs()
                    target.download(url, cacheFolder)
                }
            } else {
                // new file
                logger.info("task ${task.to} creating new file")
                target.parentFile.mkdirs()
                target.download(url, cacheFolder)

                oldTask?.let { oldTaskList.remove(it) }
            }

            if (target.exists()) {
                val sha1 = target.sha1Hex()
                if (sha1 != task.hash) {
                    logger.error("hashes do not match for task ${task.to}")
                    logger.error(sha1)
                    logger.error(task.hash)
                } else {
                    logger.trace("task ${task.to} validated")
                }
            } else {
                logger.error("file $target was not created")
            }
        }

        // iterate old
        oldTaskList.forEach { task ->
            val target = minecraftDir.resolve(task.to)
            logger.info("deleting $target")
            target.delete()
        }


        // set minecraft and forge versions
        val mmcPackPath = instanceDir.resolve("mmc-pack.json")
        val mmcPack = if (mmcPackPath.exists()) {
            mmcPackPath.readJson()
        } else MultiMCPack()
        mmcPack.components = listOf(
                PackComponent(
                        uid = "net.minecraft",
                        version = modpack.gameVersion,
                        important = true
                ),
                PackComponent(
                        uid = "net.minecraftforge",
                        version = forgeVersion.substringAfter("${modpack.gameVersion}-"),
                        important = true
                )
        ) + mmcPack.components
        mmcPackPath.writeJson(mmcPack)

        oldpackFile.createNewFile()
        oldpackFile.writeJson(modpack)
    }

    private class Arguments(parser: ArgParser) {
        val instanceId by parser.storing("--id",
                help = "\$INST_ID - ID of the instance")

        val instanceDir by parser.storing("--inst",
                help = "\$INST_DIR - absolute path of the instance") { File(this) }

        val minecraftDir by parser.storing("--mc",
                help = "\$INST_MC_DIR - absolute path of minecraft") { File(this) }
    }
}