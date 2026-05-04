plugins {
	java
	id("org.springframework.boot") version "4.0.6"
	id("io.spring.dependency-management") version "1.1.7"
	id("org.springdoc.openapi-gradle-plugin") version "1.9.0"
}

group = "com.koala"
version = "0.0.1-SNAPSHOT"

java {
	toolchain {
		languageVersion = JavaLanguageVersion.of(25)
	}
}

repositories {
	mavenCentral()
}

dependencies {
	implementation("org.springframework.boot:spring-boot-starter-web")
	implementation("org.springframework.boot:spring-boot-starter-validation")
	implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:3.0.3")
	testImplementation("org.springframework.boot:spring-boot-starter-test")
	testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.withType<Test> {
	useJUnitPlatform()
}

tasks.withType<JavaCompile> {
	options.compilerArgs.add("-Xlint:deprecation")
}

openApi {
	apiDocsUrl.set("http://localhost:8080/v3/api-docs.yaml")
	outputDir.set(layout.projectDirectory.dir("docs/openapi"))
	outputFileName.set("openapi.yaml")
}

val installGitHooks by tasks.registering(Exec::class) {
	description = "Points local git at the shared hooks in .githooks (auto-runs as part of build)"
	group = "git hooks"
	commandLine("git", "config", "core.hooksPath", ".githooks")
	isIgnoreExitValue = true
	onlyIf {
		val process = ProcessBuilder("git", "config", "--get", "core.hooksPath")
			.directory(rootDir)
			.redirectErrorStream(true)
			.start()
		process.waitFor()
		val current = process.inputStream.bufferedReader().readText().trim()
		current != ".githooks"
	}
}

tasks.named("build") {
	dependsOn(installGitHooks)
}
