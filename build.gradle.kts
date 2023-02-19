import org.gradle.jvm.tasks.Jar
import org.jetbrains.kotlin.gradle.tasks.KotlinJvmCompile

plugins {
	id ("org.ajoberstar.grgit") version "4.1.0"
	id ("maven-publish")
	id ("org.quiltmc.loom") version "1.0.+"
	id ("org.jetbrains.kotlin.jvm") version "1.8.10"	
}

version = project.properties["mod_version"]!!
group = project.properties["maven_group"]!!
val minecraft = project.properties["minecraft"]
val quilt_mappings = project.properties["quilt_mappings"]
val quilt_loader = project.properties["quilt_loader"]
val qfapi = project.properties["qfapi"]
val qlk = project.properties["qlk"]
val carpet = project.properties["carpet"]
val malilib = project.properties["malilib"]
val minihud = project.properties["minihud"]
val mod_menu = project.properties["mod_menu"]
val placeholders = project.properties["placeholders"]

quiltflower {
	addToRuntimeClasspath.set(true)
}

repositories {
	maven {
		name = "Modrinth"
		url = uri("https://api.modrinth.com/maven")
	}
	maven {
		name = "Terraformers"
		url = uri("https://maven.terraformersmc.com/releases/")
	}
	maven {
		name = "Nucleoid's"
		url = uri("https://maven.nucleoid.xyz/")
	}
	flatDir {
		dirs = setOf(file("libs"))
	}
	maven { 
		name = "JitPack"
		url = uri("https://jitpack.io")
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
	 modImplementation   ("fi.dy.masa:malilib-fabric:$minecraft-$malilib") { isTransitive = false }
	 modImplementation   ("fi.dy.masa:minihud-fabric:$minecraft-$minihud") { isTransitive = false }
	 modImplementation   ("maven.modrinth:modmenu:$mod_menu") { isTransitive = false }
	 modImplementation   ("eu.pb4:placeholder-api:$placeholders") { isTransitive = false; include(this) }
	 modCompileOnly      ("essential-client:essential-client:+") { isTransitive = false }
	 modCompileOnly      ("chunkdebug:chunk-debug:+")

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
