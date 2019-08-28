package cc.moecraft.icq.receiver.callback;

public class WSCallbackImpl implements WSCallback {
    private WSReturn wsReturn;

    public WSCallbackImpl(WSReturn wsReturn){
        this.wsReturn = wsReturn;
    }

    public String askMessage(String message) {
        return wsReturn.submitEcho(this,message);
    }

    @Override
    public String tellReturnEcho(String echo) {
        return echo;
    }
}
