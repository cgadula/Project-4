import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.net.SocketException;
import java.util.Scanner;

final class ChatClient {
    private ObjectInputStream sInput;
    private ObjectOutputStream sOutput;
    private Socket socket;

    private final String server;
    private final String username;
    private final int port;

    /* ChatClient constructor
     * @param server - the ip address of the server as a string
     * @param port - the port number the server is hosted on
     * @param username - the username of the user connecting
     */
    private ChatClient(String server, int port, String username) {
        this.server = server;
        this.port = port;
        this.username = username;
    }

    private void close() throws IOException {
        socket.close();
        sOutput.close();
        sInput.close();

    }

    /**
     * Attempts to establish a connection with the server
     * @return boolean - false if any errors occur in startup, true if successful
     */
    private boolean start() {
        // Create a socket
        try {
            socket = new Socket(server, port);
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }

        // Attempt to create output stream
        try {
            sOutput = new ObjectOutputStream(socket.getOutputStream());
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }

        // Attempt to create input stream
        try {
            sInput = new ObjectInputStream(socket.getInputStream());
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }

        // Create client thread to listen from the server for incoming messages
        Runnable r = new ListenFromServer();
        Thread t = new Thread(r);
        t.start();

        // After starting, send the clients username to the server.
        try {
            sOutput.writeObject(username);
        } catch (IOException e) {
            e.printStackTrace();
        }

        return true;
    }


    /*
     * Sends a string to the server
     * @param msg - the message to be sent
     */
    private void messageHandling() {
        while (socket.isConnected()) {
            ChatMessage logoutHandling = this.parse();
            if (logoutHandling.getType() == 1) {
                this.sendMessage(logoutHandling);
                try {
                    this.close();
                    System.out.println("You have successfully logged out!");
                    return;
                } catch (IOException e) {
                    e.printStackTrace();
                }
            } else {
                this.sendMessage(logoutHandling);
            }
        }
        try {
            this.close();
            System.out.println("Disconnected form the server.");
        } catch (IOException e) {
            System.out.println("Disconnected form the server.");
        }
    }


    private void sendMessage(ChatMessage msg)  {
        if(socket.isConnected()) {
            try {
                sOutput.writeObject(msg);
                sOutput.reset();
            } catch (IOException e) {
                System.out.println("You were disconnected from the server!");
                try {
                    this.close();
                } catch (IOException e1) {}
            }

        }

    }


    /*
     * To start the Client use one of the following command
     * > java ChatClient
     * > java ChatClient username
     * > java ChatClient username portNumber
     * > java ChatClient username portNumber serverAddress
     *
     * If the portNumber is not specified 1500 should be used
     * If the serverAddress is not specified "localHost" should be used
     * If the username is not specified "Anonymous" should be used
     */
    public static void main(String[] args) {
        // Get proper arguments and override defaults

        // Create your client and start it
         if(args.length == 1){
            ChatClient client = new ChatClient("localhost", 1500, args[0]);
             client.start();
             client.sendMessage(new ChatMessage());
             client.messageHandling();
        }else if(args.length==2){
            ChatClient client = new ChatClient("localhost", Integer.parseInt(args[1]), args[0]);
             client.start();
             client.sendMessage(new ChatMessage());
             client.messageHandling();
        }else if(args.length == 3){
            ChatClient client = new ChatClient(args[1], Integer.parseInt(args[1]), args[0]);
             client.start();
             client.sendMessage(new ChatMessage());
             client.messageHandling();
        }else{
             ChatClient client = new ChatClient("localhost", 1500, "CS 180 Student");
             client.start();
             client.sendMessage(new ChatMessage());
             client.messageHandling();
        }


        // Send an empty message to the server
    }


    /*
     * This is a private class inside of the ChatClient
     * It will be responsible for listening for messages from the ChatServer.
     * ie: When other clients send messages, the server will relay it to the client.
     */
    private final class ListenFromServer implements Runnable {
        public void run() {
            while (true){
                try {
                    String msg = (String) sInput.readObject();
                    System.out.println(msg);
                } catch (IOException | ClassNotFoundException e) {
                    System.out.println("Connection to the server has disconnected.");
                    break;
                }
        }
        }
    }

    private ChatMessage parse(){

            Scanner in = new Scanner(System.in);
            String arg = "";

            String input = in.nextLine();
            while(!input.isEmpty()) {

                if(input.contains("/msg")) {
                    arg = input.substring(0,input.indexOf(" "));
                }else{
                    arg = input;
                }
                if (arg.equalsIgnoreCase("/logout")) {
                    return new ChatMessage(1, " has logged out.", "");
                } else if (arg.equalsIgnoreCase("/list")) {
                    return new ChatMessage(3, "list" + username, username);
                } else if (arg.equalsIgnoreCase("/msg")) {
                    String[] a = input.split(" ");
                    String dmMessage = "";
                    for(int i =2 ; i<a.length;i++){
                         dmMessage+= a[i]+" ";
                    }
                    return new ChatMessage(2, dmMessage, a[1]);//handle what happens when username is not found @todo
                } /*else if (arg.equalsIgnoreCase("/ttt")) {
                 @todo
            }*/ else {
                    return new ChatMessage(0, input, "");
                }
            }
        return new ChatMessage();
    }

}
