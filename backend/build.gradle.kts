plugins {
	java
	jacoco
	alias(libs.plugins.spring.boot)
	alias(libs.plugins.spring.dependency.management)
	alias(libs.plugins.flyway)
	alias(libs.plugins.owasp.dependency.check)
}

group = "com.rvce"
version = "0.0.1-SNAPSHOT"

java {
	toolchain {
		languageVersion = JavaLanguageVersion.of(17)
	}
}

repositories {
	mavenCentral()
}

dependencies {
	implementation(libs.spring.boot.starter.actuator)
	implementation(libs.spring.boot.starter.data.jpa)
	implementation(libs.spring.boot.starter.data.redis)
	implementation(libs.spring.boot.starter.mail)
	implementation(libs.spring.boot.starter.security)
	implementation(libs.spring.boot.starter.validation)
	implementation(libs.spring.boot.starter.web)
	implementation("org.apache.commons:commons-csv:1.11.0")
	implementation(libs.jjwt.api)
	runtimeOnly(libs.jjwt.impl)
	runtimeOnly(libs.jjwt.jackson)
	runtimeOnly(libs.postgresql)
	implementation("org.bouncycastle:bcprov-jdk15on:1.70")
	testImplementation(libs.spring.boot.starter.test)
	testImplementation(libs.spring.security.test)
	testImplementation("com.h2database:h2")
	testRuntimeOnly(libs.junit.platform.launcher)

	// lombok - removes boilerplate code like getters, setters, constructors, etc.
	compileOnly(libs.lombok)
	annotationProcessor(libs.lombok)

	// mapstruct - for mapping between DTOs and entities
	implementation(libs.mapstruct)
	annotationProcessor(libs.mapstruct.processor)

	// flyway - runtime migration support in app
	implementation(libs.flyway.core)
	implementation(libs.flyway.database.postgresql)

	// swagger - for API documentation
	implementation(libs.springdoc.openapi.starter.webmvc.ui)
}

tasks.withType<Test> {
	useJUnitPlatform()
	javaLauncher = javaToolchains.launcherFor {
		languageVersion = JavaLanguageVersion.of(17)
	}
}

tasks.jacocoTestReport {
	dependsOn(tasks.test)
	reports {
		xml.required.set(true)
		html.required.set(true)
	}
}

tasks.test {
	finalizedBy(tasks.jacocoTestReport)
}

flyway {
	url = System.getenv("DB_URL") ?: "jdbc:postgresql://localhost:5432/scas_db?stringtype=unspecified"
	user = System.getenv("DB_USER") ?: "scas"
	password = System.getenv("DB_PASSWORD") ?: "scas_dev_password"
	locations = arrayOf("classpath:db/migration")
	validateOnMigrate = false
	outOfOrder = false
	schemas = arrayOf("public")
}
