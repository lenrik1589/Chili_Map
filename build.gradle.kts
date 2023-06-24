import org.gradle.jvm.tasks.Jar
import org.jetbrains.kotlin.gradle.tasks.KotlinJvmCompile

plugins {
	id ("maven-publish")
	id ("org.ajoberstar.grgit") version "4.1.0"
	id ("org.jetbrains.kotlin.jvm") version "1.8.10"
	id ("org.quiltmc.loom") version "1.2.+"
}

version = project.properties["mod_version"]!!
group = project.properties["maven_group"]!!
val minecraft by project.properties
val quilt_mappings by project.properties
val quilt_loader by project.properties
val qfapi by project.properties
val qlk by project.properties
val carpet by project.properties
val malilib by project.properties
val minihud by project.properties
val mod_menu by project.properties
val placeholders by project.properties
val essential by project.properties
val chunk_debug by project.properties

quiltflower {
	addToRuntimeClasspath.set(true)
}

repositories {
	maven {
		name = "Modrinth"
		url = uri("https://api.modrinth.com/maven")
		content {
			includeGroup("maven.modrinth")
		}
	}
	flatDir {
		dirs = setOf(file("libs"))
	}
	maven { 
		name = "JitPack"
		url = uri("https://jitpack.io")
	}
	maven {
		url = uri("https://cursemaven.com")
		content {
			includeGroup("curse.maven")
		}
	}
	mavenLocal()
	mavenCentral()
}

dependencies {
	minecraft           ("com.mojang:minecraft:$minecraft")
	mappings            ("org.quiltmc:quilt-mappings:$minecraft+build.$quilt_mappings:intermediary-v2")
	modImplementation   ("org.quiltmc:quilt-loader:$quilt_loader")
	modImplementation   ("org.quiltmc.quilted-fabric-api:quilted-fabric-api:$qfapi-$minecraft")
	modImplementation   ("org.quiltmc.quilt-kotlin-libraries:quilt-kotlin-libraries:$qlk") {
		exclude(group = "org.quiltmc", module = "qsl" )
	}
	implementation      ("com.github.LlamaLad7:MixinExtras:0.1.1")
	annotationProcessor ("com.github.LlamaLad7:MixinExtras:0.1.1")
	modImplementation   ("com.github.gnembon:fabric-carpet:$carpet") { isTransitive = false }
	modImplementation   ("curse.maven:malilib-303119:$malilib") { isTransitive = false }
	modImplementation   ("curse.maven:minihud-fabric-244260:$minihud") { isTransitive = false }
	modImplementation   ("maven.modrinth:modmenu:$mod_menu") { isTransitive = false }
	modImplementation   ("maven.modrinth:placeholder-api:$placeholders+$minecraft") { include(this) }
	modImplementation   ("maven.modrinth:essentialclient:$essential")
	implementation      ("com.github.senseiwells:Arucas:+")
	modImplementation   ("maven.modrinth:chunkdebug:$chunk_debug")

}

tasks.withType<ProcessResources> {
	filteringCharset = "UTF-8"
	duplicatesStrategy = DuplicatesStrategy.EXCLUDE
}

tasks.withType<JavaCompile> {
	// ensure that the encoding is set to UTF-8, no matter what the system default is
	// this fixes some edge cases with special characters not displaying correctly
	// see http://yodaconditions.net/blog/fix-for-java-file-encoding-problems-with-gradle.html
	// If Javadoc is generated, this must be specified in that task too.
	options.encoding = "UTF-8"
}

tasks.withType<Jar> {
	from("LICENSE") {
		rename { "${this}_${project.properties["archivesBaseName"]}" }
	}
}

tasks.withType<KotlinJvmCompile> {
	kotlinOptions {
		freeCompilerArgs += "-opt-in=kotlin.RequiresOptIn"
		jvmTarget = "17"
	}
}
