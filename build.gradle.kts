import kotlinx.metadata.isExpect
import kotlinx.metadata.klib.KlibModuleMetadata
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinMetadataTarget
import org.jetbrains.kotlin.konan.library.resolverByName
import org.jetbrains.kotlin.util.DummyLogger

buildscript {
    dependencies {
        classpath("org.jetbrains.kotlinx:kotlinx-metadata-klib:0.0.5")
    }
}

plugins {
    kotlin("multiplatform") version "1.9.24"
}

group = "org.example"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

kotlin {
    jvmToolchain(21)

    macosArm64()
    macosX64()
    linuxArm64()
    linuxX64()
}

val srcset2output = project.objects.mapProperty(String::class, FileCollection::class)

val findExpects = project.tasks.create("findExpects") {
    doLast {
        srcset2output.get().forEach { srcSetInfo ->
            val metadataDir = srcSetInfo.value.singleFile
            if (!metadataDir.exists()) return@forEach

            val lib = resolverByName(emptyList(), logger = DummyLogger).resolve(metadataDir.absolutePath)
            val meta = KlibModuleMetadata.read(object : KlibModuleMetadata.MetadataLibraryProvider {
                override val moduleHeaderData: ByteArray
                    get() = lib.moduleHeaderData

                override fun packageMetadata(fqName: String, partName: String): ByteArray {
                    return lib.packageMetadata(fqName, partName)
                }

                override fun packageMetadataParts(fqName: String): Set<String> {
                    return lib.packageMetadataParts(fqName)
                }
            })
            meta.fragments.forEach { frag ->
                frag.classes.forEach { klass ->
                    if (klass.isExpect) {
                        project.logger.warn("An expect was found: ${klass.name} from SourceSet{${srcSetInfo.key}}")
                    }
                }
                frag.pkg?.functions?.forEach { fn ->
                    if (fn.isExpect) {
                        project.logger.warn("An expect was found: ${fn.name} from from SourceSet{${srcSetInfo.key}}")
                    }
                }
            }
        }
    }
}

kotlin.targets.configureEach {
    if (this is KotlinMetadataTarget) {
        compilations.configureEach {
            findExpects.dependsOn(output)
            srcset2output.put(defaultSourceSet.name, output.classesDirs)
        }
    }
}
