package voodoo.pack

import voodoo.data.Side
import voodoo.data.curse.CurseFile
import voodoo.data.curse.CurseManifest
import voodoo.data.curse.CurseMinecraft
import voodoo.data.curse.CurseModLoader
import voodoo.data.lock.LockPack
import voodoo.forge.Forge
import voodoo.provider.Provider
import voodoo.util.packToZip
import voodoo.util.writeJson
import java.io.File

/**
 * Created by nikky on 30/03/18.
 * @author Nikky
 */

object CursePack : AbstractPack() {
    override val label = "SK Packer"

    override fun download(modpack: LockPack, target: String?, clean: Boolean) {
        val cacheDir = directories.cacheHome
        val workspaceDir = File(".curse")
        val modpackDir = workspaceDir.resolve(with(modpack) { "$name-$version" })
        val srcFolder = modpackDir.resolve("overrides")

        if (clean) {
            logger.info("cleaning modpack directory $srcFolder")
            srcFolder.deleteRecursively()
        }
        if (!srcFolder.exists()) {
            logger.info("copying files into overrides")
            val mcDir = File(modpack.minecraftDir)
            if (mcDir.exists()) {
                mcDir.copyRecursively(srcFolder, overwrite = true)
            } else {
                logger.warn("minecraft directory $mcDir does not exist")
            }
        }

        for (file in srcFolder.walkTopDown()) {
            when {
                file.name == "_SERVER" -> file.deleteRecursively()
                file.name == "_CLIENT" -> file.renameTo(file.parentFile)
            }
        }

        val loadersFolder = modpackDir.resolve("loaders")
        logger.info("cleaning loaders $loadersFolder")
        loadersFolder.deleteRecursively()

        // download forge
        val (forgeUrl, forgeFileName, forgeLongVersion, forgeVersion) = Forge.getForgeUrl(modpack.forge.toString(), modpack.mcVersion)
//        val forgeFile = loadersFolder.resolve(forgeFileName)
//        forgeFile.download(forgeUrl, cacheDir.resolve("FORGE").resolve(forgeVersion))

        val modsFolder = srcFolder.resolve("mods")
        logger.info("cleaning mods $modsFolder")
        modsFolder.deleteRecursively()

        val curseMods = mutableListOf<CurseFile>()

        // download entries
        for (entry in modpack.entries) {
            val required = modpack.features.none { feature ->
                feature.entries.any { it == entry.name }
            }

            val provider = Provider.valueOf(entry.provider).base
            if (entry.side == Side.SERVER) continue
            if (entry.provider == Provider.CURSE.name) {
                curseMods += CurseFile(entry.projectID, entry.fileID, required).apply { println("added voodoo.data.curse file $this") }
            } else {
                val targetFolder = srcFolder.resolve(entry.folder)
                val (url, file) = provider.download(entry, targetFolder, cacheDir)
                if (!required) {
                    val optionalFile = file.parentFile.resolve(file.name + ".disabled")
                    file.renameTo(optionalFile)
                }
            }
        }

        // generate modlist
        val modListFile = modpackDir.resolve("modlist.html")
        val html = "<ul>\n" + modpack.entries.joinToString("") { entry ->
            val provider = Provider.valueOf(entry.provider).base
            if (entry.side == Side.SERVER) return@joinToString ""
            val projectPage = provider.getProjectPage(entry)
            val authors = provider.getAuthors(entry)
            val authorString = if(authors.isNotEmpty()) " (by ${authors.joinToString(", ")})" else ""

            when {
                projectPage.isNotEmpty() -> return@joinToString "<li><a href=\"$projectPage\">${entry.name} $authorString</a></li>\n"
                entry.url.isNotBlank() -> return@joinToString "<li>direct: <a href=\"${entry.url}\">${entry.name} $authorString</a></li>\n"
                else -> {
                    val source = if(entry.fileSrc.isNotBlank()) "file://"+entry.fileSrc else "unknown"
                    return@joinToString "<li>${entry.name} $authorString (source: $source)</li>\n"
                }
            }
        }  + "\n</ul>\n"



        if (modListFile.exists()) modListFile.delete()
        modListFile.createNewFile()
        modListFile.writeText(html)

        val curseManifest = CurseManifest(
                name = modpack.title,
                version = modpack.version,
                author = modpack.authors.joinToString(", "),
                minecraft = CurseMinecraft(
                        version = modpack.mcVersion,
                        modLoaders = listOf(
                                CurseModLoader(
                                        id = "forge-$forgeVersion",
                                        primary = true
                                )
                        )
                ),
                manifestType = "minecraftModpack",
                manifestVersion = 1,
                files = curseMods,
                overrides = "overrides"
        )
        val manifestFile = modpackDir.resolve("manifest.json")
        manifestFile.writeJson(curseManifest)

        val cursePackFile = workspaceDir.resolve(with(modpack) { "$name-$version.zip" })

        packToZip(modpackDir.toPath(), cursePackFile.toPath())

        logger.info("packed ${modpack.name} -> $cursePackFile")
    }

}