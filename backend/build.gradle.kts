plugins {
	java
	id("org.springframework.boot") version "3.5.13"
	id("io.spring.dependency-management") version "1.1.7"
	id("org.flywaydb.flyway") version "9.22.3"
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
	implementation("org.springframework.boot:spring-boot-starter-actuator")
	implementation("org.springframework.boot:spring-boot-starter-data-jpa")
	implementation("org.springframework.boot:spring-boot-starter-security")
	implementation("org.springframework.boot:spring-boot-starter-validation")
	implementation("org.springframework.boot:spring-boot-starter-web")
	runtimeOnly("org.postgresql:postgresql")
	testImplementation("org.springframework.boot:spring-boot-starter-test")
	testImplementation("org.springframework.security:spring-security-test")
	testRuntimeOnly("org.junit.platform:junit-platform-launcher")

	// lombok - removes boilerplate code like getters, setters, constructors, etc.
	compileOnly("org.projectlombok:lombok")
	annotationProcessor("org.projectlombok:lombok")

	// mapstruct - for mapping between DTOs and entities
	implementation("org.mapstruct:mapstruct:1.5.5.Final")
	annotationProcessor("org.mapstruct:mapstruct-processor:1.5.5.Final")

	// flyway - runtime migration support in app
	implementation("org.flywaydb:flyway-core")
	implementation("org.flywaydb:flyway-database-postgresql")

	// swagger - for API documentation
	implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:2.5.0")
}

tasks.withType<Test> {
	useJUnitPlatform()
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
