package org.proxyma.backend.admin

import org.springframework.boot.ApplicationArguments
import org.springframework.boot.ApplicationRunner
import org.springframework.boot.ExitCodeGenerator
import org.springframework.stereotype.Component

/**
 * `grant-admin <email>` / `revoke-admin <email>` operator commands.
 *
 * The runner is a no-op unless one of those commands is present. When it is,
 * `BackendApplication` starts the context without a web server and exits with
 * this runner's exit code.
 */
@Component
class AdminCommandRunner(
	private val platformAdmins: PlatformAdminService,
) : ApplicationRunner, ExitCodeGenerator {

	private var exitCode = 0

	override fun run(args: ApplicationArguments) {
		val command = args.nonOptionArgs.firstOrNull() ?: return
		if (command !in AdminCommand.NAMES) {
			return
		}

		val email = args.nonOptionArgs.getOrNull(1)
		if (email.isNullOrBlank()) {
			System.err.println("Usage: $command <email>")
			exitCode = 2
			return
		}

		try {
			val result = when (command) {
				"grant-admin" -> platformAdmins.grant(email)
				"revoke-admin" -> platformAdmins.revoke(email)
				else -> error("Unreachable")
			}
			val state = if (result.changed) "done" else "already in the requested state"
			println("$command ${result.user.email}: $state")
		} catch (ex: Exception) {
			System.err.println("$command failed: ${ex.message}")
			exitCode = 1
		}
	}

	override fun getExitCode(): Int = exitCode
}

object AdminCommand {

	val NAMES = setOf("grant-admin", "revoke-admin")

	fun isCommand(args: Array<String>): Boolean =
		args.firstOrNull { !it.startsWith("-") } in NAMES
}
