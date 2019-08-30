package cc.moecraft.icq.receiver.callback;

public interface Sender {
    public void submitMessage(WSCallback callback,String echo,String message);
}
