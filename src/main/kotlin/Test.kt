import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlin.concurrent.thread
import kotlin.coroutines.suspendCoroutine

// 博客系列章节一：挂起函数 样例

typealias Callback = (Long) -> Unit

fun main() = runBlocking<Unit> { getUser(123, this) }

//fun main() = run { getUser(123) }

// 展示用户信息函数
fun getUser(uid: Long) {
    fetchUser(uid) {
        showUser(uid)
    }
}

// 拉取用户信息函数
fun fetchUser(uid: Long, cb: Callback) = thread(name = "work") {
    log("start fetch user $uid info")
    Thread.sleep(300)
    log("end fetch user $uid info")
    thread(name = "ui") {
        cb(uid)
    }
}

//
// 用户信息UI展示函数
fun showUser(uid: Long) {
    log("show user $uid in ui thread")
}

fun getUser(uid: Long, scope: CoroutineScope) =
        scope.launch {
            // 这里没有回调
            fetchUser(uid)
            // 使用上述的UI展示函数
            showUser(uid)
        }

suspend fun fetchUser(uid: Long) = suspendCoroutine<Unit> { cont ->
    // 使用上面的回调函数改造成挂起函数
    fetchUser(uid) {
        cont.resumeWith(Result.success(Unit))
    }
}




