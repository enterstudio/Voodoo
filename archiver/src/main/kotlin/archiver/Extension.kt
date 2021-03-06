package archiver

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory
import com.fasterxml.jackson.module.kotlin.KotlinModule
import java.io.File

/**
 * Created by nikky on 02/02/18.
 * @author Nikky
 */

fun Any.writeToFile(file: File) {
    val mapper = ObjectMapper(YAMLFactory()) // Enable YAML parsing
    mapper.registerModule(KotlinModule()) // Enable Kotlin support

    file.bufferedWriter().use {
        mapper.writeValue(it, this)
    }
}