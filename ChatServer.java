import java.io.EOFException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.text.*;
import java.util.Date;


final class ChatServer {
    private static int uniqueId = 0;
    // Data structure to hold all of the connected clients
    private final List<ClientThread> clients = new ArrayList<>();
    private final int port;            // port the server is hosted on
    private Object lock = new Object();

    /**
     * ChatServer constructor
     *
     * @param port - the port the server is being hosted on
     */
    private ChatServer(int port) {
        this.port = port;
    }

    /*
     * This is what starts the ChatServer.
     * Right now it just creates the socketServer and adds a new ClientThread to a list to be handled
     */
    private void start() {
        try {
            ServerSocket serverSocket = new ServerSocket(port);
            while (true) {
                Socket socket = serverSocket.accept();
                Runnable r = new ClientThread(socket, uniqueId++);
                Thread t = new Thread(r);
                clients.add((ClientThread) r);
                t.start();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }










    private synchronized void remove(int id)throws IOException {
        for(int i =0 ; i<clients.size();i++){
            if(clients.get(i).getId() == id){

                clients.remove(i);
            }
        }
    }





    /*
     *  > java ChatServer
     *  > java ChatServer portNumber
     *  If the port number is not specified 1500 is used
     */
    public static void main(String[] args) {
        if (args.length == 0) {
            ChatServer server = new ChatServer(1500);
            server.start();
        } else if (args.length == 1) {
            ChatServer server = new ChatServer(Integer.parseInt(args[0]));
            server.start();

        } else {
            ChatServer server = new ChatServer(1500);
            System.out.println("Created server with default values because of too many arguments!");
            server.start();
        }
    }


    /*
     * This is a private class inside of the ChatServer
     * A new thread will be created to run this every time a new client connects.
     */
    private final class ClientThread implements Runnable {
        Socket socket;                  // The socket the client is connected to
        ObjectInputStream sInput;       // Input stream to the server from the client
        ObjectOutputStream sOutput;     // Output stream to the client from the server
        String username;                // Username of the connected client
        ChatMessage cm;                 // Helper variable to manage messages
        int id;
        String broadcastUsername;

        /*
         * socket - the socket the client is connected to
         * id - id of the connection
         */
        private ClientThread(Socket socket, int id) {
            this.id = id;
            this.socket = socket;

            try {
                sOutput = new ObjectOutputStream(socket.getOutputStream());
                sInput = new ObjectInputStream(socket.getInputStream());
                username = (String) sInput.readObject();
            } catch (IOException | ClassNotFoundException e) {
                e.printStackTrace();
            }
        }

        private int getId(){
            return this.id;
        }
        private String getDate(){
            SimpleDateFormat time = new SimpleDateFormat("HH:mm:ss");
            String date = time.format(new Date());
            return date;
        }

        private void broadcast(String message) {
            synchronized (lock){
                System.out.println(getDate()+" "+username+": "+message);
                for(ClientThread t : clients){
                    t.writeMessage( getDate()+ " " +broadcastUsername+": "+message);
                }
            }

        }

        private void close() throws IOException{
            socket.close();
            sOutput.close();
            sInput.close();
            clients.remove(this);
        }
        private  void directMessage(String message, String username) {
            synchronized (lock) {
                boolean found = false;
                if(username.equals(this.username)){
                    this.writeMessage("You can not send a whisper to yourself!");
                    System.out.println("...........USER TRIED TO SEND msg TO HIMSELF");
                }
                for (ClientThread clientThread : clients) {
                    if (clientThread.username.equalsIgnoreCase(username)) {
                        clientThread.writeMessage("From " + this.username + ": " + message);
                        found = true;
                    }
                }
                if(!found){
                    this.writeMessage("The user is not online!");
                    System.out.println("........ MESSAGE FAILED INVALID USERNAME");
                }
            }
        }


        private boolean writeMessage(String message) {
            if(socket.isConnected()){
                try {
                    sOutput.writeObject(message);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                return true;
            }else{
                return false;
            }
        }



        /*
         * This is what the client thread actually runs.
         */
        @Override
        public void run() {
            // Read the username sent to you by client
            try {
                cm = (ChatMessage) sInput.readObject();
            } catch (IOException | ClassNotFoundException e) {
                e.printStackTrace();
            }
            System.out.println(username + ": Ping");


            // Send message back to the client
            try {
                sOutput.writeObject("Pong");
            } catch (IOException e) {
                e.printStackTrace();
            }

            while(socket.isConnected()){
                try {
                    try{
                        cm = (ChatMessage) sInput.readObject();
                    }catch(EOFException | SocketException e){//@todo
                        break;
                    }
                    if(cm.getType()==ChatMessage.MESSAGE && !cm.getMessage().equals("")){
                        broadcastUsername= this.username;
                        broadcast(cm.getMessage());
                    }else if(cm.getType()==ChatMessage.LIST && !cm.getMessage().equals("")){
                        String users = "";
                        for(int i =0; i<clients.size();i++){
                            users+= clients.get(i).username + ", ";
                        }
                        sOutput.writeObject(users.substring(0,users.length()-2));
                        System.out.println(getDate()+" "+username+" requested list");
                    }else if(cm.getType()==ChatMessage.LOGOUT && !cm.getMessage().equals("")){
                        System.out.println(getDate()+" "+username + " has logged out");
                        this.close();
                    }else if(cm.getType()==ChatMessage.DM && !cm.getMessage().equals("")){
                        System.out.println(getDate()+" "+this.username+" -> "+cm.getReceiver()+": "+cm.getMessage());
                        this.directMessage(cm.getMessage(), cm.getReceiver());
                    }
                } catch (IOException | ClassNotFoundException e) {
                    e.printStackTrace();
                }
            }
            try {
                this.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
