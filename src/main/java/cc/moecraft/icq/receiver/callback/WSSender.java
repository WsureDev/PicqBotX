package cc.moecraft.icq.receiver.callback;

public class WSSender extends Thread implements WSCallback {
    private Sender sender;

    private String response = null;
    public WSSender(Sender sender){
        this.sender = sender;
    }

    public void sendMessage(String echo,String message) {
        sender.submitMessage(this,echo,message);
        synchronized (this){
            try {
                this.wait(10000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void report(String response) {
        this.response = response;
        synchronized (this){
            this.notify();
        }
    }

    public String getResponse() {
        return response;
    }
}
