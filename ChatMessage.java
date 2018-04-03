import java.io.Serializable;

final class ChatMessage implements Serializable {
    private static final long serialVersionUID = 6898543889087L;

    // Types of messages
    static final int MESSAGE = 0, LOGOUT = 1, DM = 2, LIST = 3, TICTACTOE = 4;

    // Here is where you should implement the chat message object.
    // Variables, Constructors, Methods, etc.
    private int type;
    private String message;
    private String receiver;
    public ChatMessage(int type, String message, String receiver){
        this.type = type;
        this.message = message;
        this.receiver = receiver;
    }

    public ChatMessage(){
        this.type = 0;
        this.message = "";
        this.receiver= "";
    }

    public int getType(){
        return this.type;
    }

    public String getMessage(){
        return this.message;
    }

    public String getReceiver(){
        return this.receiver;
    }

    public void setMessage(String msg){
        this.message = msg;
    }

    public void setReceiver(String receiver){
        this.receiver = receiver;
    }
}