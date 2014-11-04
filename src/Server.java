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

//the server can be run as a CL or GUI application

public class Server {

    //a unique ID for each connection
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

                Socket socket = serverSocket.accept();      // accept connection
                // if I was asked to stop
                if (!keepGoing) {
                    break;
                }
                ClientThread t = new ClientThread(socket);  // make a thread of it
                al.add(t);                                  // save it in the ArrayList
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

    public void stop() {
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

    private synchronized void broadcast(String message, int team) {
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
            //if the clients team id is not equal to the team if of the sender
            if(ct.teamID != team){
                continue;
            }
            // try to write to the Client if it fails remove it from the list
            if (!ct.writeMsg(messageLf)) {
                al.remove(i);
                display("Disconnected Client " + ct.username + " removed from list.");
            }
        }
    }

    // for a client who logoff using the LOGOUT message
    public synchronized void remove(int id) {
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
        //team id of the client
        int teamID;
        // the New Username
        String newUsername;
        // the New user's password
        String newUserPassword;
        // the New user type
        int newUserType;
        // the New user teamid
        int newUserTeamId;
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
                    int verified = verifyUser(username, password);
                    if (verified == -1) {
                        i++;
                        System.out.println("User not verified.");
                        boolean verify = false;
                        //sOutput.writeObject(verify);
                        String verifyMessage = "false";
                        sOutput.writeObject(verifyMessage);
                        sOutput.flush();
                        continue;
                    }
                    System.out.println("User verified.");
                    boolean verify = true;
                    //get there scrum team id here
                    //save scrum team id in a variable for the CT object
                    teamID = verified;
                    String verifyMessage = "true";
                    sOutput.writeObject(verifyMessage);
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
                        broadcast(username + ": " + message, this.teamID);
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
                        } catch (SQLException ex) {
                            System.out.println("editUser() " + ex);
                            Logger.getLogger(Server.class.getName()).log(Level.SEVERE, null, ex);
                        }
                        catch (IOException ex) {
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
                //stmt.close();
                //con.close();
            } catch (Exception e) {
                System.out.println("*******************Closing Failed******************");
            }
        }

        //method to get a users permissions (usertype)
        //called when a user tries to add/edit/remove
        private int getPermissions() throws SQLException, IOException {
            ResultSet rs = null;

            String query = "SELECT * FROM users WHERE username=";
            query = query + "'" + username + "'";

            rs = stmt.executeQuery(query);

            if(rs.next()){
                int permissions = rs.getInt("usertype");
                return permissions;
            } else {
                writeMsg("FAILURE: could not get permissions for user.");
                return -1;
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
            ResultSet rss = null;

            try {
                newUsername = (String) sInput.readObject();
                newUserPassword = (String) sInput.readObject();
                newUserType = (Integer)sInput.readObject();
                newUserTeamId = (Integer)sInput.readObject();
            } catch (IOException ex) {
                Logger.getLogger(Server.class.getName()).log(Level.SEVERE, null, ex);
            } catch (ClassNotFoundException ex) {
                Logger.getLogger(Server.class.getName()).log(Level.SEVERE, null, ex);
            }

            int permissions = getPermissions();
            //if get permissions failed
            if(permissions == -1){
                return;
            }

            //if user is not scrum master or higher
            if(permissions < 1){
                writeMsg("FAILURE: You do not have permission to add a user.");
                return;
            }

            String query = "SELECT * FROM users WHERE username=";
            query = query + "'" + newUsername + "'";

            rs = stmt.executeQuery(query);
            // check if the username already exists or not
            if (rs.next()) {
                // already username exist
                writeMsg("FAILURE: Username already exists.");
            } else {

                // check if newUserTeam has already scrum master or not
                String q = "SELECT * FROM users WHERE teamid=" + newUserTeamId + " AND usertype=0";
                rss = stmt.executeQuery(q);
                // if there is, reject creating new user
                if(rss.next()) {
                    writeMsg("FAILURE: Scrum Master in the group already exists.");
                } else {
                    // else create the new user with scrum master
                    // new username is valid
                    sOutput.writeObject("true");
                    // save new user data in database
                    String storeQuery = "INSERT INTO users VALUES('"
                            + newUsername + "', '" + newUserPassword + "', "
                            + newUserType + ", " + newUserTeamId +")";
                    stmt.execute(storeQuery);
                    writeMsg("SUCCESS: New user added.");
                }
                /*
                // new username is valid
                sOutput.writeObject("true");
                // save new user data in database
                String storeQuery = "INSERT INTO users VALUES('"
                        + newUsername + "', '" + newUserPassword + "', "
                        + newUserType + ", " + newUserTeamId +")";
                stmt.execute(storeQuery);
                writeMsg("SUCCESS: New user added.");
                */
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

            int permissions = getPermissions();
            if(permissions == -1){
                return;
            }

            //if user is not scrum master or higher
            if(permissions < 1){
                writeMsg("FAILURE: You do not have permission to remove a user.");
                return;
            }

            rs = stmt.executeQuery(query);

            if (rs.next()) {
                //get the permissions of the user we want to remove
                int removePermissions = rs.getInt("usertype");
                if(removePermissions >= permissions){
                    writeMsg("FAILURE: You do not have remove this user.");
                    return;
                }
                String storeQuery = "DELETE FROM users WHERE username= ?";
                pst = con.prepareStatement(storeQuery);
                pst.setString(1, rmUsername);
                pst.executeUpdate(); 
                writeMsg("SUCCESS: User removed.");
                
            } else {
                // There is no user name
                writeMsg("FAILURE: The user you're trying to remove could not be found.");
            }
            
        }
        
        private void editUser() throws SQLException, IOException {
            ResultSet rs = null;

            try {
                editUsername = (String) sInput.readObject();
                editUserPassword = (String) sInput.readObject();
                editUserType = (Integer)sInput.readObject();
            } catch (IOException ex) {
                Logger.getLogger(Server.class.getName()).log(Level.SEVERE, null, ex);
            } catch (ClassNotFoundException ex) {
                Logger.getLogger(Server.class.getName()).log(Level.SEVERE, null, ex);
            }

            String query = "SELECT * FROM users WHERE username=";
            query = query + "'" + editUsername + "'";

            int permissions = getPermissions();
            if(permissions == -1){
                return;
            }

            //if user is not scrum master or higher
            if(permissions < 1){
                writeMsg("FAILURE: You do not have permission to remove a user.");
                return;
            }

            rs = stmt.executeQuery(query);

            if (rs.next()) {
                //get the permissions of the user we want to edit
                int removePermissions = rs.getInt("usertype");
                if(removePermissions >= permissions){
                    writeMsg("FAILURE: You do not have edit this user.");
                    return;
                }
                String storeQuery = "UPDATE users SET password=?, usertype=? WHERE username=?";
                pst = con.prepareStatement(storeQuery);
                pst.setString(1, editUserPassword);
                pst.setInt(2, editUserType);
                pst.setString(3, editUsername);
                pst.executeUpdate();
                writeMsg("SUCCESS: User edited.");
                
            } else {
                writeMsg("FAILURE: User could not be edited, user may not exist.");
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

        //check to see if a clients login information is correct
        //return 0 if false else return the team id
        private int verifyUser(String username, String password) throws SQLException {

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

            if(rs.next()) {
                //sql check username
                if (!usernm.equals(rs.getString("username"))) {
                    System.out.println("Username is not matched");
                    return -1;
                }
                System.out.println("username passed");

                //sql check password
                if (!passwd.equals(rs.getString("password"))) {
                    System.out.println("Password is not matched");
                    return -1;
                }
                System.out.println("password passed");

            } else {
                System.out.println("no match found");
                return -1;
            }
            //return the user id
            return rs.getInt("teamid");
        }
    }
}
