package org.proxyma.backend

import org.proxyma.backend.admin.AdminCommand
import org.springframework.boot.SpringApplication
import org.springframework.boot.WebApplicationType
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.ConfigurationPropertiesScan
import kotlin.system.exitProcess

@SpringBootApplication
@ConfigurationPropertiesScan
class BackendApplication

fun main(args: Array<String>) {
	val application = SpringApplication(BackendApplication::class.java)
	val adminCommand = AdminCommand.isCommand(args)
	if (adminCommand) {
		// Operator commands need the database and Flyway, not a listening port.
		application.setWebApplicationType(WebApplicationType.NONE)
	}

	val context = application.run(*args)
	if (adminCommand) {
		exitProcess(SpringApplication.exit(context))
	}
}
