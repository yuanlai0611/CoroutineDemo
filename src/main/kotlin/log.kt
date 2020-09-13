import kotlinx.coroutines.Job
import java.text.SimpleDateFormat
import java.util.*
import kotlin.coroutines.coroutineContext

private fun printTime() = SimpleDateFormat("HH-mm:ss.SSS").format(Date())

fun log(value: Int) {
    println("[${Thread.currentThread().name}][${printTime()}]$value")
}

fun log(value: String) {
    println("[${Thread.currentThread().name}][${printTime()}]$value")
}

private suspend inline fun Job.Key.currentJob() = coroutineContext[Job]