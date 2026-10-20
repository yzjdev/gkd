package li.gkd.app.service

import li.gkd.app.R

class HttpTileService : BaseTileService() {
    override val activeFlow get() = HttpService.isRunning

    override fun onTileClick() {
        if (HttpService.isRunning.value) {
            HttpService.stop()
        } else {
            HttpService.start()
        }
    }
}
