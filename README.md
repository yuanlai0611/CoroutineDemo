# CoroutineDemo
博客代码样例

## Kotlin 协程使用手册——挂起函数  
本文全部实例代码
### 回调与协程对比
开头先抛出一个 Android 开发中常见的例子：应用通过用户 uid 展示用户信息，步骤包括拉取用户信息和在主线程中展现信息。使用**回调的写法**如下所示：

```kotlin
typealias Callback = (Long) -> Unit

fun main() = run { getUser(123) }

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

// 用户信息UI展示函数
fun showUser(uid: Long) {
    log("show user $uid in ui thread")
}
```
运行的结果如图所示：
![回调写法结果](https://blog-1258461783.cos.ap-guangzhou.myqcloud.com/%E5%9B%9E%E8%B0%83%E5%86%99%E6%B3%95%E7%9A%84%E7%BB%93%E6%9E%9C.jpg)
接着我们使用**协程的写法**来改造上述的代码：

```kotlin 
fun main() = runBlocking<Unit> { getUser(123, this) }

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
```
运行结果如图所示：
![协程写法结果](https://blog-1258461783.cos.ap-guangzhou.myqcloud.com/%E5%8D%8F%E7%A8%8B%E5%86%99%E6%B3%95%E7%9A%84%E7%BB%93%E6%9E%9C.jpg)

*BTW: 这里需要说明，第二种写法可能比第一种写法有更多的代码，但是这个问题是另一个维度的问题，也会有一些方法来节省代码，比如模板代码生成。*

通过比较上述两种函数的运行结果可以看出它们的实现效果是一致的，但是在协程写法的 getUser 函数中 fetchUser 函数和 showUser 函数之间采用的是看起来同步的写法，这是如何做到的？在这里先抛出一个结论，协程并没有完全消除回调，而是采用另一种方式来替代回调。答案藏在编译后的代码中，可以试着将上述代码编译成字节码再反编译成 Java 代码，并在其中去寻找答案。

### 编译后的协程代码
这里将上述协程写法的代码转换成 Java 代码，得出的整体代码结构如下图所示：
![编译后的类结构](https://blog-1258461783.cos.ap-guangzhou.myqcloud.com/%E7%BC%96%E8%AF%91%E5%90%8E%E7%9A%84%E7%B1%BB%E7%BB%93%E6%9E%84.jpg)

可以看出编译生成了两个类 TestKt、TestKt\$fetchUser\$3\$1，其中 TestKt\$fetchUser\$3\$1 
类是 lambada 表达式的实现类，相当于 Java 当中的匿名内部类，这里不做深入解读，只需要关注invoke(Object) 的函数实现即可。TestKt\$fetchUser\$3\$1 的代码如下所示：

```kotlin
final class TestKt$fetchUser$3$1 extends Lambda implements Function1 {
   final Continuation $cont;
   
   public Object invoke(Object var1) {
      this.invoke(((Number)var1).longValue());
      return Unit.INSTANCE;
   }

   public final void invoke(long it) {
      Companion var3 = Result.Companion;
      Unit var4 = Unit.INSTANCE;
      boolean var5 = false;
      this.$cont.resumeWith(Result.constructor-impl(var4));
   }

   TestKt$fetchUser$3$1(Continuation var1) {
      super(1);
      this.$cont = var1;
   }
}
```
由上述的 TestKt\$fetchUser\$3\$1 类可以看出, invoke(Object) 函数中调用的是 invoke(long) 函数，最终调用的是 fetchUser 挂起函数中 Continuation 的 resumeWith(Result) 函数。所以说可以把上述类要干的事等价于原 kotlin 代码中 fetchUser(Long, Callback) 中的 `cont.resumeWith(Result.success(Unit))`

分析的重点放在 TestKt 这个类，根据调用关系由浅入深地分析，首先分析 getUser 这个函数，具体函数代码如下所示，为了方便阅读，去掉了影响理解的部分：
```kotlin
@NotNull
   public static final Job getUser(final long uid, @NotNull CoroutineScope scope) {
      Intrinsics.checkParameterIsNotNull(scope, "scope");
      return BuildersKt.launch$default(scope, (CoroutineContext)null, (CoroutineStart)null, (Function2)(new Function2((Continuation)null) {
         private CoroutineScope p$;
         Object L$0;
         int label;

         @Nullable
         public final Object invokeSuspend(@NotNull Object $result) {
            Object var3 = IntrinsicsKt.getCOROUTINE_SUSPENDED();
            CoroutineScope $this$launch;
            switch(this.label) {
            case 0:
               ResultKt.throwOnFailure($result);
               $this$launch = this.p$;
               long var10000 = uid;
               this.L$0 = $this$launch;
               this.label = 1;
               if (TestKt.fetchUser(var10000, (Continuation)this) == var3) {
                  return var3;
               }
               break;
            case 1:
               $this$launch = (CoroutineScope)this.L$0;
               ResultKt.throwOnFailure($result);
               break;
            default:
               throw new IllegalStateException("call to 'resume' before 'invoke' with coroutine");
            }

            TestKt.showUser(uid);
            return Unit.INSTANCE;
         }
        ...
      }), 3, (Object)null);
   }
```

getUser 调用了 BuildersKt.launch 函数，这个函数等价于原 kotlin 代码中的 `scope.launch` 对于协程构建器部分后面的文章会讲到，这里暂时跳过。launch\$default 的第4个参数传入的是 Function2 的匿名内部类，这里存在一处反编译的问题，因为反编译后的代码是：`new Function2((Continuation)null) {...}`，Fuction2 只是个接口，并不存在构造函数，所以 launch\$default 的第4个参数实际传入的是一个 SuspendLambda 实现类（本质上是一个 Continuation 类型）。
```kotlin
// Suspension lambdas inherit from this class
internal abstract class SuspendLambda(
    public override val arity: Int,
    completion: Continuation<Any?>?
) : ContinuationImpl(completion), FunctionBase<Any?>, SuspendFunction {
    constructor(arity: Int) : this(arity, null)

    public override fun toString(): String =
        if (completion == null)
            Reflection.renderLambdaToString(this) // this is lambda
        else
            super.toString() // this is continuation
}

```
SuspendLambda 的注释说明了被关键字 suspend 修饰的 Lambda 表达式转换成字节码以后实际上都会继承这个类。接下来继续跟进 getUser 函数，在此之前必须引出 kotlin 协程一个很重要的接口 Continuation :

```kotlin
/**
 * Interface representing a continuation after a suspension point that returns a value of type `T`.
 */
@SinceKotlin("1.3")
public interface Continuation<in T> {
    /**
     * The context of the coroutine that corresponds to this continuation.
     */
    public val context: CoroutineContext

    /**
     * Resumes the execution of the corresponding coroutine passing a successful or failed [result] as the
     * return value of the last suspension point.
     */
    public fun resumeWith(result: Result<T>)
}
```
接口的注释介绍它是挂起点处的一个续体，同时接口有两个需要完成的成员：
* context： 对应续体的协程上下文，协程上下文不是本文的重点，后续会专门介绍，可以将它理解为每个协程的局部变量
* resumeWith(Result)：从协程的挂起点处恢复，并返回响应的值

这里涉及到协程的概念，协程是一个广义的概念，并不是 kotlin 所独有的，协程的实现的核心是状态机，状态机是协程消除回调的核心所在。kotlin 中状态机都实现了实现了 Continuation 接口，在上面的 getUser 函数中其实隐藏着一个状态机，也就是刚才强调的 SuspendLambda 的实现类。还是通过上面的 getUser 函数来说明状态机的实现，同时把上面的状态机简化一下方便理解：


```kotlin
public class <anonymous_for_state_machine> extends SuspendLambda<Unit> {

    public <anonymous_for_state_machine>(long uid) {
        super(1);
        this.uid = uid;
    }

    private long uid;
    private int label = 0;
    private Object result;

    @Nullable
    @Override
    protected Object invokeSuspend(@NotNull Object o) {
        switch (label) {
            case 0:
                ResultKt.throwOnFailure(o);
                label = 1;
                if (TestKt.fetchUser(uid, this) == IntrinsicsKt.getCOROUTINE_SUSPENDED()) {
                    return IntrinsicsKt.getCOROUTINE_SUSPENDED();
                }
                break;
            case 1:
                ResultKt.throwOnFailure(o);
                break;
            default:
                throw new IllegalStateException("call to 'resume' before 'invoke' with coroutine");
        }
        TestKt.showUser(uid);
        return Unit.INSTANCE;
    }

}
```
简化后的状态机中 label 代表不同的状态，这里有两个状态 0 和 1，分别表示着初始状态和挂起状态。当协程构建器 launch 了协程之后间接调用状态机的 invokeSuspend，由于此时 label 对应着状态 0，走进 label 0 对应的流程，我们在该流程中会调用 fetchUser 函数，如果函数返回的是 COROUTINE_SUSPENDED ，则代表协程被挂起（挂起这块的细节在下一节中详细讨论），同时需要注意的是 fetchUser 函数中传入的 Continuation 就是这里的状态机，接着当 fetchUser 执行完毕之后会通过调用 Continuation 的 resumeWith 函数间接调用 invokeSuspend 恢复协程的运行，接着进入 label 1 对应的流程，这里可以看到流程中没有做其它处理就 break 出去，接着执行 TestKt.showUser(Long) 
函数。同时假如 fetchUser 执行的时候直接返回了结果，没有挂起，就会直接执行 TestKt.showUser(Long)。

### 挂起函数介绍

在学习和使用 kotlin 协程的过程中，挂起点和挂起函数等词汇的出现频率很高，kotlin 中的挂起函数需要被关键字 suspend 修饰。协程中经常使用的 delay 函数就是典型的挂起函数，当在协程中调用 delay(200) 函数时，当前协程会被挂起 200ms，200ms 后会恢复协程的运行，在当前协程被挂起的时候不会阻塞当前线程的执行。同时需要注意的是挂起函数不一定会挂起，正如上面提到的内容，getUser 的状态机中提到的，协程是否被挂起决定于 fetchUser 的返回值，根据判断返回的值是不是等于 COROUTINE_SUSPENDED 来决定是否挂起。
分析挂起函数的时候还会涉及到挂起点的概念，挂起点严格的定义是协程可能被挂起的位置。还是以 getUser 中的状态机为例子，fetchUser 为一个挂起函数，这个挂起函数就是一个挂起点，状态机中会对应两个状态，label 0 和 label 1，在 label 0 处挂起，完成相应计算工作后恢复协程调用，接着调用 lable 1对应的操作（此处无挂起），结束执行。

![挂起函数示意图](https://blog-1258461783.cos.ap-guangzhou.myqcloud.com/%E6%8C%82%E8%B5%B7%E5%87%BD%E6%95%B0%E7%A4%BA%E6%84%8F%E5%9B%BE.jpg)

### CPS 转换和 Continuation 接口
刚才在解释协程编译后的源码可能没有注意到一点，fetchUser 原本的函数是 fetchUser(Long) 但是编译后的函数变成了 fetchUser(Long, Contiuation)，它的参数列表增加了一个 Continuation 函数。（当然编译后函数参数变化的原因有很多，比如很常见的拓展方法，会把被调用类的对象作为参数传入函数中）
这就是协程说明文档所指的 CPS 转换，CPS 是 Continuation-Passing-Style 的缩写，翻译过来叫作续体传递风格，挂起函数和挂起 Lambda 表达式后面附加一个 Continuation 表达式。

```kotlin 
suspend fun fetchUser(uid: Long) 
```

经过 CPS 转换后的函数变化为：

```kotlin
fun fetchUser(uid: Long, cont: Continuation): Any? 
```

我们在上文中提到，状态机中根据判断 fetchUser 函数是否返回 COROUTINE_SUSPENDED 来决定是否挂起协程，因为 fetchUser 函数本身也可以写作有返回值的挂起函数，所以就会存在两种类型的返回值的情况，此时经过 CPS 变化后，函数的返回值编程 Any?
接着谈到为什么要进行 CPS 变化？前面说明了协程之所以可以把一个普通的回调转换成协程中同步的写法，需要借助实现了 Continuation 接口的状态机，所以这里挂起函数经过 CPS 转换的目的就是将该挂起函数的状态机传递给另一个挂起函数，有另一个挂起函数来控制状态机的状态变化，进而将原本的回调隐藏在对应状态的处理中。（这里挂起函数包括挂起 Lambda 表达式）

### 将普通回调转换成挂起函数
如何将普通的包含回调的函数转换成一个挂起函数，进而可以达到想要的同步式的写法。kotlin 协程为我们提供了一个协程内建的挂起函数 suspendCoroutine ：

```kotlin
suspend fun <T> suspendCoroutine(block: (Continuation<T>) -> Unit): T
``` 
当改函数在协会中被调用时，他只能在协程或者其它挂起函数中被调用，这里需要注意的是 suspendCoroutine 是一个挂起函数，它的 Continuation 是由外部传入的，这里唯一的参数 block，是用来处理这个 Continuation 续体的。这里以上文例子中` suspend fun fetchUser(uid: Long) ` 作为例子，内部依然是调用的回调的请求形式，我们把实现的细节隐藏在 fetchUser 的挂起函数中。当然除了 suspendCoroutine 函数之外还有 suspendCancellableCoroutine 函数，由它封装的挂起函数支持响应 CancellationException。这一点涉及到 kotlin 协程的另一个特性结构化并发并且支持 Cancel，这一点会在之后的文章中详谈。

### 总结 & 引用出处
官方对挂起函数的定义是被关键字 suspend 修饰的函数，支持挂起且不会阻塞线程，这些解释是出于描述挂起函数的功能的，但是会为学习者造成挂起函数究竟是干什么的疑惑。根据本文的探讨，我们引出了和挂起函数息息相关的 Continuation 这个接口，以及 CPS 转换规则和状态机等一系列的实现细节，通过上面的这些实现要素可以得出 suspend 修饰的挂起函数的神秘之处都隐藏在编译阶段，所以并不是没有回调，而是对同一状态机的递归调用，挂起函数的核心作用就是写法上异步 => 同步的转换。

引用出处：
[Kotlin 协程 说明文档](https://github.com/Kotlin-zh/KEEP/blob/master/proposals/coroutines.md#%E6%8C%82%E8%B5%B7%E5%87%BD%E6%95%B0)

[了解Kotlin协程实现原理这篇就够了](https://ethanhua.github.io/2018/12/24/kotlin_coroutines/)