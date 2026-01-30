var telegramVersion = "9.2.1"

plugins {
	java
	id("org.springframework.boot") version "4.0.2"
	id("io.spring.dependency-management") version "1.1.7"
}

group = "org.bot"
version = "0.0.1"

java {
    sourceCompatibility = JavaVersion.VERSION_25
}

tasks.jar {
    enabled = false
}

tasks.bootJar {
    enabled = true
}

configurations {
	compileOnly {
		extendsFrom(configurations.annotationProcessor.get())
	}
}

repositories {
	mavenCentral()
	maven { url = uri("https://repo.spring.io/milestone") }
	maven { url = uri("https://repo.spring.io/snapshot") }
}

dependencies {
	implementation("org.springframework.boot:spring-boot-starter-web")
	implementation("org.springframework.boot:spring-boot-starter-actuator")

	implementation("org.telegram:telegrambots-meta:$telegramVersion")
	implementation("org.telegram:telegrambots-longpolling:$telegramVersion")
	implementation("org.telegram:telegrambots-client:$telegramVersion")
	implementation("com.alibaba:fastjson:2.0.57")
	compileOnly("org.projectlombok:lombok")

	annotationProcessor("org.springframework.boot:spring-boot-configuration-processor")
	annotationProcessor("org.projectlombok:lombok")

	testImplementation("org.springframework.boot:spring-boot-starter-test")
	testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.test {
	useJUnitPlatform()
}
