import java.io.*;
import java.net.*;
import java.text.SimpleDateFormat;
import java.util.*;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.PreparedStatement;
import java.util.logging.Level;
import java.util.logging.Logger;
/*
 * The server that can be run both as a console application or a GUI
 */

public class Server {

    // a unique ID for each connection
    private static int uniqueId;
    // an ArrayList to keep the list of the Client
    private ArrayList<ClientThread> al;
    // if I am in a GUI
    private ServerGUI sg;
    // to display time
    private SimpleDateFormat sdf;
    // the port number to listen for connection
    private int port;
    // the boolean that will be turned of to stop the server
    private boolean keepGoing;

    // JDBC members
    private Connection con;
    private Statement stmt;
    private PreparedStatement pst;

    private String usernm = null;
    private String passwd = null;

    /*
     *  server constructor that receive the port to listen to for connection as parameter
     *  in console
     */
    public Server(int port) {
        this(port, null);
    }

    public Server(int port, ServerGUI sg) {
        // GUI or not
        this.sg = sg;
        // the port
        this.port = port;
        // to display hh:mm:ss
        sdf = new SimpleDateFormat("HH:mm:ss");
        // ArrayList for the Client list
        al = new ArrayList<ClientThread>();
        // JDBC variables
        con = null;

        
        
        // Database connection
        try {
            // try to use jdbc driver
            Class.forName("com.mysql.jdbc.Driver");
        } catch (Exception e) {
            System.out.println("Driver failed to load." + e);
        }

        try {
            // get connection with database (database url, id, password)
            
            con = DriverManager.getConnection(
                    "jdbc:mysql://db4free.net:3306/chatprogram7081", "chatprogram70815", "70815chatprogram");
            System.out.println("*****************Connected with database *****************");
        } catch (Exception e) {
            System.out.println("*****************Connection Failed to database*****************");
            System.out.println(e);
        }

        // set stmt variable to execute SQL query
        try {
            stmt = con.createStatement();
            //pst = con.prepareStatement

        } catch (Exception e) {
            System.out.println(e + "stmt con.createStatement");
        }

    }

    public void start() throws SQLException {
        keepGoing = true;
        /* create socket server and wait for connection requests */
        try {
            // the socket used by the server
            ServerSocket serverSocket = new ServerSocket(port);

            // infinite loop to wait for connections
            while (keepGoing) {
                // format message saying we are waiting
                display("Server waiting for Clients on port " + port + ".");

                Socket socket = serverSocket.accept();  	// accept connection
                // if I was asked to stop
                if (!keepGoing) {
                    break;
                }
                ClientThread t = new ClientThread(socket);  // make a thread of it
                al.add(t);									// save it in the ArrayList
                t.start();
            }
            // I was asked to stop
            try {
                serverSocket.close();
                for (int i = 0; i < al.size(); ++i) {
                    ClientThread tc = al.get(i);
                    try {
                        tc.sInput.close();
                        tc.sOutput.close();
                        tc.socket.close();
                    } catch (IOException ioE) {
                        // not much I can do
                    }
                }
            } catch (Exception e) {
                display("Exception closing the server and clients: " + e);
            }
        } // something went bad
        catch (IOException e) {
            String msg = sdf.format(new Date()) + " Exception on new ServerSocket: " + e + "\n";
            display(msg);
        }
    }
    /*
     * For the GUI to stop the server
     */

    protected void stop() {
        keepGoing = false;
        // connect to myself as Client to exit statement 
        // Socket socket = serverSocket.accept();
        try {
            new Socket("localhost", port);
        } catch (Exception e) {
            // nothing I can really do
        }
    }
    /*
     * Display an event (not a message) to the console or the GUI
     */

    private void display(String msg) {
        String time = sdf.format(new Date()) + " " + msg;
        if (sg == null) {
            System.out.println(time);
        } else {
            sg.appendEvent(time + "\n");
        }
    }
    /*
     *  to broadcast a message to all Clients
     */

    private synchronized void broadcast(String message) {
        // add HH:mm:ss and \n to the message
        String time = sdf.format(new Date());
        String messageLf = time + " " + message + "\n";
        // display message on console or GUI
        if (sg == null) {
            System.out.print(messageLf);
        } else {
            sg.appendRoom(messageLf);     // append in the room window
        }
        // we loop in reverse order in case we would have to remove a Client
        // because it has disconnected
        for (int i = al.size(); --i >= 0;) {
            ClientThread ct = al.get(i);
            // try to write to the Client if it fails remove it from the list
            if (!ct.writeMsg(messageLf)) {
                al.remove(i);
                display("Disconnected Client " + ct.username + " removed from list.");
            }
        }
    }

    // for a client who logoff using the LOGOUT message
    synchronized void remove(int id) {
        // scan the array list until we found the Id
        for (int i = 0; i < al.size(); ++i) {
            ClientThread ct = al.get(i);
            // found it
            if (ct.id == id) {
                al.remove(i);
                return;
            }
        }
    }

    /*
     *  To run as a console application just open a console window and: 
     * > java Server
     * > java Server portNumber
     * If the port number is not specified 1500 is used
     */
    public static void main(String[] args) throws SQLException {
        // start server on port 1500 unless a PortNumber is specified 
        int portNumber = 1500;
        switch (args.length) {
            case 1:
                try {
                    portNumber = Integer.parseInt(args[0]);
                } catch (Exception e) {
                    System.out.println("Invalid port number.");
                    System.out.println("Usage is: > java Server [portNumber]");
                    return;
                }
            case 0:
                break;
            default:
                System.out.println("Usage is: > java Server [portNumber]");
                return;

        }
        // create a server object and start it
        Server server = new Server(portNumber);
        server.start();
    }

    /**
     * One instance of this thread will run for each client
     */
    class ClientThread extends Thread {

        // the socket where to listen/talk
        Socket socket;
        ObjectInputStream sInput;
        ObjectOutputStream sOutput;
        // my unique id (easier for deconnection)
        int id;
        // the Username of the Client
        String username;
        // the password of the client
        String password;
        // the New Username
        String newUsername;
        // the New user's password
        String newUserPassword;
        // the New user type
        int newUserType;
        // the only type of message a will receive
        ChatMessage cm;
        // the date I connect
        String date;
        
        String rmUsername;
        
         // the New Username
        String editUsername;
        // the New user's password
        String editUserPassword;
        // the New user type
        int editUserType;
        

        // Constructor
        ClientThread(Socket socket) throws SQLException {
            // a unique id
            id = ++uniqueId;
            this.socket = socket;
            boolean loop = true;
            int i = 0;
            /* Creating both Data Stream */
            System.out.println("Thread trying to create Object Input/Output Streams");
            try {
                // create output first
                sOutput = new ObjectOutputStream(socket.getOutputStream());
                sInput = new ObjectInputStream(socket.getInputStream());

                System.out.println("User is logging in...");

                //allow user 5 login attempts
                while (loop && i < 5) {
                    // read the username and password
                    username = (String) sInput.readObject();
                    password = (String) sInput.readObject();

                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        display("Thread sleep exception: " + e);
                    }
                    if (!verifyUser(username, password)) {
                        i++;
                        System.out.println("User not verified.");
                        boolean verify = false;
                        //sOutput.writeObject(verify);
                        String verified = "false";
                        sOutput.writeObject(verified);
                        sOutput.flush();
                        continue;
                    }
                    System.out.println("User verified.");
                    boolean verify = true;
                    //sOutput.writeObject(verify);
                    String verified = "true";
                    sOutput.writeObject(verified);
                    sOutput.flush();
                    loop = false;
                }

                display(username + " just connected.");
            } catch (IOException e) {
                display("Exception creating new Input/output Streams: " + e);
                return;
            } // have to catch ClassNotFoundException
            // but I read a String, I am sure it will work
            catch (ClassNotFoundException e) {
            }
            date = new Date().toString() + "\n";
        }

        // what will run forever
        public void run() {
            // to loop until LOGOUT
            boolean keepGoing = true;
            while (keepGoing) {
                // read a String (which is an object)
                try {
                    cm = (ChatMessage) sInput.readObject();
                } catch (IOException e) {
                    display(username + " Exception reading Streams: " + e);
                    break;
                } catch (ClassNotFoundException e2) {
                    break;
                }
                // the messaage part of the ChatMessage
                String message = cm.getMessage();

                // Switch on the type of message receive
                switch (cm.getType()) {
                    case ChatMessage.MESSAGE:
                        broadcast(username + ": " + message);
                        break;
                    case ChatMessage.LOGOUT:
                        display(username + " disconnected with a LOGOUT message.");
                        keepGoing = false;
                        break;
                    case ChatMessage.WHOISIN:
                        writeMsg("List of the users connected at " + sdf.format(new Date()) + "\n");
                        // scan al the users connected
                        for (int i = 0; i < al.size(); ++i) {
                            ClientThread ct = al.get(i);
                            writeMsg((i + 1) + ") " + ct.username + " since " + ct.date);
                        }
                        break;
                    case ChatMessage.USERADD:
                        try {
                            addUser();
                        } catch (SQLException ex) {
                            System.out.println("addUser() " + ex);
                            Logger.getLogger(Server.class.getName()).log(Level.SEVERE, null, ex);
                        } catch (IOException ex) {
                            System.out.println("addUser() " + ex);
                            Logger.getLogger(Server.class.getName()).log(Level.SEVERE, null, ex);
                        }

                        break;
                     case ChatMessage.USERREMOVE:
                        try {
                            rmUser();
                        } catch (SQLException ex) {
                            System.out.println("rmUser() " + ex);
                            Logger.getLogger(Server.class.getName()).log(Level.SEVERE, null, ex);
                        } catch (IOException ex) {
                            System.out.println("rmUser() " + ex);
                            Logger.getLogger(Server.class.getName()).log(Level.SEVERE, null, ex);
                        }

                        break;
                         
                     case ChatMessage.USEREDIT:
                        try {
                            editUser();
                        } catch (SQLException | IOException ex) {
                            System.out.println("editUser() " + ex);
                            Logger.getLogger(Server.class.getName()).log(Level.SEVERE, null, ex);
                        }

                        break;        
                }
            }
            // remove myself from the arrayList containing the list of the
            // connected Clients
            remove(id);
            close();
        }

        // try to close everything
        @SuppressWarnings("empty-statement")
        private void close() {
            // try to close the connection
            try {
                if (sOutput != null) {
                    sOutput.close();
                }
            } catch (Exception e) {
            }
            try {
                if (sInput != null) {
                    sInput.close();
                }
            } catch (Exception e) {
            };
            try {
                if (socket != null) {
                    socket.close();
                }
            } catch (Exception e) {
            }

            // disconnect connection and statement
            try {
                stmt.close();
                con.close();
            } catch (Exception e) {
                System.out.println("*******************Closing Failed******************");
            }
        }

        /*
         * Method to add user by admin
         */
        private void addUser() throws SQLException, IOException {
            
            // create output first
            //sOutput = new ObjectOutputStream(socket.getOutputStream());
            //sInput = new ObjectInputStream(socket.getInputStream());
            ResultSet rs = null;

            try {
                newUsername = (String) sInput.readObject();
                newUserPassword = (String) sInput.readObject();
                newUserType = (int)sInput.readObject();
            } catch (IOException ex) {
                Logger.getLogger(Server.class.getName()).log(Level.SEVERE, null, ex);
            } catch (ClassNotFoundException ex) {
                Logger.getLogger(Server.class.getName()).log(Level.SEVERE, null, ex);
            }

            String query = "SELECT * FROM users WHERE username=";
            query = query + "'" + newUsername + "'";

            rs = stmt.executeQuery(query);

            if (rs.next()) {
                // already username exist
                String verified = "false";
                sOutput.writeObject(verified);
            } else {
                // new username is valid
                String verified = "true";
                sOutput.writeObject(verified);
                // save new user data in database
                String storeQuery = "INSERT INTO users VALUES('"
                        + newUsername + "', '" + newUserPassword + "', "
                        + newUserType +")";
                stmt.execute(storeQuery);
            }
        }
        private void rmUser() throws SQLException, IOException{
            ResultSet rs = null;

            try {
                rmUsername = (String) sInput.readObject();
            } catch (IOException ex) {
                Logger.getLogger(Server.class.getName()).log(Level.SEVERE, null, ex);
            } catch (ClassNotFoundException ex) {
                Logger.getLogger(Server.class.getName()).log(Level.SEVERE, null, ex);
            }
            System.out.println("" + rmUsername);
            String query = "SELECT * FROM users WHERE username=";
            query = query + "'" + rmUsername + "'";

            rs = stmt.executeQuery(query);

            if (rs.next()) {
                // There is no user name
                String verified = "false";
                sOutput.writeObject(verified);
                
            } else {
                String verified = "true";
                sOutput.writeObject(verified);
                String storeQuery = "DELETE FROM users WHERE username= ?";
                pst = con.prepareStatement(storeQuery);
                pst.setString(1, rmUsername);
                pst.executeUpdate(); 
            }
            
        }
        
        private void editUser() throws SQLException, IOException {
            ResultSet rs = null;

            try {
                editUsername = (String) sInput.readObject();
                editUserPassword = (String) sInput.readObject();
                editUserType = (int)sInput.readObject();
            } catch (IOException ex) {
                Logger.getLogger(Server.class.getName()).log(Level.SEVERE, null, ex);
            } catch (ClassNotFoundException ex) {
                Logger.getLogger(Server.class.getName()).log(Level.SEVERE, null, ex);
            }

            String query = "SELECT * FROM users WHERE username=";
            query = query + "'" + editUsername + "'";

            rs = stmt.executeQuery(query);

            if (rs.next()) {
                // already username exist
                
                //String storeQuery = "UPDATE users password ='editUserPassword'+type ='editUserType' users WHERE username='editUsername'";
                String storeQuery = "UPDATE users SET password=?, usertype=? WHERE username=?";
                pst = con.prepareStatement(storeQuery);
                pst.setString(1, editUserPassword);
                pst.setInt(2, editUserType);
                pst.setString(3, editUsername);
                pst.executeUpdate();
                //stmt.execute(storeQuery);
                String verified = "true";
                sOutput.writeObject(verified);
                
            } else {
                String verified = "false";
                sOutput.writeObject(verified);
            }
        }
        /*
         * Write a String to the Client output stream
         */
        private boolean writeMsg(String msg) {
            // if Client is still connected send the message to it
            if (!socket.isConnected()) {
                close();
                return false;
            }
            // write the message to the stream
            try {
                sOutput.writeObject(msg);
            } // if an error occurs, do not abort just inform the user
            catch (IOException e) {
                display("Error sending message to " + username);
                display(e.toString());
            }
            return true;
        }

        
        
        private boolean verifyUser(String username, String password) throws SQLException {

            // JDBC variables
            ResultSet rs = null;
            usernm = username;
            passwd = password;

            // Compose SELECT query
            String query = "SELECT * FROM users WHERE username=";
            query = query + "'" + usernm + "'";
            query = query + "AND password='" + passwd + "'";

            // execute SQL query
            rs = stmt.executeQuery(query);

            if (rs.next()) {
                //sql check username
                if (!usernm.equals(rs.getString("username"))) {
                    System.out.println("Username is not matched");
                    return false;
                }

                //sql check password
                if (!passwd.equals(rs.getString("password"))) {
                    System.out.println("Password is not matched");
                    return false;
                }

            } else {
            	return false;
            }

            return true;
        }
    }
}
