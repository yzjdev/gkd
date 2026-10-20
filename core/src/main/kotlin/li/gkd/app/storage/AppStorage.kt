package li.gkd.app.storage

import li.gkd.app.R

import li.gkd.app.util.AndroidStorage

/** Android 进程在读取任何应用状态前确定存储根目录。 */
fun appStorage(): AppStorageLayout = AndroidStorage.storage
