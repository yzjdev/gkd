package li.gkd.app.platform

import li.gkd.app.R

import li.gkd.app.service.fixRestartAutomatorService

fun requestAutomatorRestart(): PlatformResult<Unit> {
    fixRestartAutomatorService()
    return PlatformResult.Success(Unit)
}
