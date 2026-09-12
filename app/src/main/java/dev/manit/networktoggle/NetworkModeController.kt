package dev.manit.networktoggle

import android.content.Context
import android.content.Intent
import android.provider.Settings

class NetworkModeController(context: Context) {
    private val appContext = context.applicationContext
    private val backends: List<NetworkBackend> = listOf(
        PublicTelephonyBackend(appContext),
        ShizukuBackend(appContext)
    )

    fun read(): ModeState {
        val target = CurrentDataSubscription.resolve(appContext).getOrElse {
            return ModeState(NetworkMode.UNKNOWN, message = it.message ?: "No mobile-data SIM")
        }
        val errors = mutableListOf<String>()
        for (backend in backends) {
            if (!backend.isAvailable()) continue
            backend.readUserAllowedTypes(target)
                .onSuccess { mask ->
                    if (!NetworkMasks.isSafeBase(mask)) {
                        errors += "${backend.name}: policy is empty or does not include LTE"
                    } else {
                        return ModeState(NetworkMasks.mode(mask), mask, backend.name, target = target)
                    }
                }
                .onFailure { errors += "${backend.name}: ${it.message}" }
        }
        return failure(errors, target)
    }

    fun toggle(): ModeState {
        val target = CurrentDataSubscription.resolve(appContext).getOrElse {
            return ModeState(NetworkMode.UNKNOWN, message = it.message ?: "No mobile-data SIM")
        }
        val errors = mutableListOf<String>()
        for (backend in backends) {
            if (!backend.isAvailable()) continue
            val beforeResult = backend.readUserAllowedTypes(target)
            if (beforeResult.isFailure) {
                errors += "${backend.name} read: ${beforeResult.exceptionOrNull()?.message}"
                continue
            }
            val before = beforeResult.getOrThrow()
            if (!NetworkMasks.isSafeBase(before)) {
                errors += "${backend.name}: refusing an empty policy or one without LTE"
                continue
            }

            val freshTarget = CurrentDataSubscription.resolve(appContext).getOrNull()
            if (freshTarget != target) {
                return ModeState(NetworkMode.UNKNOWN, message = "Mobile-data SIM changed. Tap again.")
            }

            val requested = NetworkMasks.with5g(before, NetworkMasks.mode(before) != NetworkMode.FIVE_G)
            val writeResult = backend.writeUserAllowedTypes(target, requested)
            if (writeResult.isFailure) {
                errors += "${backend.name} write: ${writeResult.exceptionOrNull()?.message}"
                continue
            }

            Thread.sleep(350)
            val afterResult = backend.readUserAllowedTypes(target)
            if (afterResult.isFailure) {
                errors += "${backend.name} verify: ${afterResult.exceptionOrNull()?.message}"
                continue
            }
            val after = afterResult.getOrThrow()
            if (after != requested) {
                errors += "${backend.name}: exact verification failed ($before -> $after, expected $requested)"
                continue
            }

            val currentTarget = CurrentDataSubscription.resolve(appContext).getOrNull()
            if (currentTarget != target) {
                return ModeState(
                    NetworkMode.UNKNOWN,
                    message = "The previous data SIM was changed and verified, but mobile data moved to another SIM. Tap again."
                )
            }
            return ModeState(
                NetworkMasks.mode(after), after, backend.name,
                "Verified $before -> $after", target
            )
        }
        return failure(errors, target)
    }

    private fun failure(errors: List<String>, target: CurrentDataSubscription.Target) = ModeState(
        NetworkMode.UNKNOWN,
        message = errors.joinToString("; ").ifBlank {
            "Start Shizuku and grant this app permission"
        },
        target = target
    )

    fun settingsIntent(): Intent = Intent(Settings.ACTION_NETWORK_OPERATOR_SETTINGS)
        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
}
