//public class <anonymous_for_state_machine> extends SuspendLambda<Unit> {
//
//    public <anonymous_for_state_machine>(long uid) {
//        super(1);
//        this.uid = uid;
//    }
//
//    private long uid;
//    private int label = 0;
//    private Object result;
//
//    @Nullable
//    @Override
//    protected Object invokeSuspend(@NotNull Object o) {
//        switch (label) {
//            case 0:
//                ResultKt.throwOnFailure(o);
//                label = 1;
//                if (TestKt.fetchUser(uid, this) == IntrinsicsKt.getCOROUTINE_SUSPENDED()) {
//                    return IntrinsicsKt.getCOROUTINE_SUSPENDED();
//                }
//                break;
//            case 1:
//                ResultKt.throwOnFailure(o);
//                break;
//            default:
//                throw new IllegalStateException("call to 'resume' before 'invoke' with coroutine");
//        }
//        TestKt.showUser(uid);
//        return Unit.INSTANCE;
//    }
//
//}
