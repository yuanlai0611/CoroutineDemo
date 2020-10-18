import kotlin.Result;
import kotlin.Unit;
import kotlin.concurrent.ThreadsKt;
import kotlin.coroutines.Continuation;
import kotlin.coroutines.EmptyCoroutineContext;
import kotlin.coroutines.SafeContinuation;
import kotlin.coroutines.intrinsics.IntrinsicsKt;
import kotlin.coroutines.jvm.internal.DebugProbesKt;
import kotlin.coroutines.jvm.internal.SuspendLambda;
import kotlin.jvm.functions.Function0;
import kotlin.jvm.functions.Function1;
import kotlin.jvm.functions.Function2;
import kotlinx.coroutines.BuildersKt;
import kotlinx.coroutines.CoroutineScope;
import kotlinx.coroutines.CoroutineStart;
import kotlinx.coroutines.Job;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class TestDecompiled {

    public static Job getUser(final long uid, CoroutineScope scope) {
        return BuildersKt.launch(scope, EmptyCoroutineContext.INSTANCE, CoroutineStart.DEFAULT, new Function2<CoroutineScope, Continuation<? super Unit>, Object>() {
            @Override
            public Object invoke(CoroutineScope scope, Continuation<? super Unit> continuation) {
                return ((AnonymousStateMachine) create(continuation)).invokeSuspend(Unit.INSTANCE);
            }

            public Continuation<?> create(Continuation<?> continuation) {
                return new AnonymousStateMachine(uid, continuation);
            }
        });
    }

    // 生成的状态机，label 为0时对应原代码 getUser(Long, CoroutineScope) 函数中的 fetchUser(Long) 函数；
    // label 为1时对应的是 showUser(Long) 函数
    public static class AnonymousStateMachine extends SuspendLambda {
        int label;
        long uid;

        public AnonymousStateMachine(long uid, Continuation<?> cont) {
            super(1);
            this.uid = uid;
        }

        @Nullable
        @Override
        protected Object invokeSuspend(@NotNull Object o) {
            switch (label) {
                case 0:
                    label = 1;
                    if (fetchUser(uid, this) == IntrinsicsKt.getCOROUTINE_SUSPENDED()) {
                        return IntrinsicsKt.getCOROUTINE_SUSPENDED();
                    }
                    break;
                case 1:
                    break;
            }
            showUser(uid);
            return Unit.INSTANCE;
        }
    }

    public static Object fetchUser(long uid, Continuation<? super Unit> completion) {
        SafeContinuation cont = new SafeContinuation(IntrinsicsKt.intercepted(completion));
        fetchUser(uid, new FetchUserLambda(cont));
        Object result = cont.getOrThrow();
        if (result == IntrinsicsKt.getCOROUTINE_SUSPENDED()) {
            DebugProbesKt.probeCoroutineSuspended(cont);
        }
        return result;
    }

    private static class FetchUserLambda implements Function1<Object, Unit> {
        Continuation<?> cont;

        public FetchUserLambda(Continuation<?> cont) {
            this.cont = cont;
        }

        public final void invoke(long it) {
            cont.resumeWith(Result.constructor-impl(Unit.INSTANCE));
        }

        @Override
        public Unit invoke(Object var) {
            invoke((long) var);
            return Unit.INSTANCE;
        }
    }

    public static Thread fetchUser(final long uid, Function1<? super Long, Unit> cb) {
        return ThreadsKt.thread(true, false, null, "work", 0, new Function0<Unit>() {
            @Override
            public Unit invoke() {
                LogKt.log("start fetch user " + uid + " info");
                try {
                    Thread.sleep(300L);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                LogKt.log("end fetch user " + uid + " info");
                ThreadsKt.thread(true, false, null, "ui", 0, new Function0<Unit>() {
                    @Override
                    public Unit invoke() {
                        cb.invoke(uid);
                        return Unit.INSTANCE;
                    }
                });
                return Unit.INSTANCE;
            }
        });
    }

    public static void showUser(long uid) {
        LogKt.log("show user " + uid + " in ui thread");
    }
}

