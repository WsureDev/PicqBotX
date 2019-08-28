package cc.moecraft.icq.receiver.callback;

public class WSReturnImpl implements WSReturn{
    private String echo;
    public WSReturnImpl(String echo){
        this.echo = echo;
    }
    @Override
    public String submitEcho(WSCallback callback, String message) {
        return echo;
    }
}
