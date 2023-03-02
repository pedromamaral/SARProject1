package HTTPServer;

import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.scene.layout.GridPane;


import java.io.FileInputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import javax.net.ssl.*;
import java.security.*;
import java.net.UnknownHostException;
import javafx.application.Platform;


/**
 * JavaFX App
 */
public class App extends Application {
    
    public final static String server_name = "HTTP Server - SAR 2022/2023 v0.1 - by ???? ???? ????";
    public final static String HOMEFILENAME = "index.htm";
    static public SSLContext sslContext= null;
    final static int MaxAcceptLog= 10;  // Accepts up to 10 pending TCP connections
    final static int SmallTextFieldSize = 50;
    final static int MediumTextFieldSize = 100;
    ServerThread MainThread= null;
    ServerThread MainSecureThread = null; //https thread
    public ServerSocket server;
    public javax.net.ssl.SSLServerSocket serverS; // Secure TCP server
    public int n_threads= 0;
    
    //GUI objects
    ToggleButton activeButton;
    Label portLabel;
    TextField portTextField;
    Label securePortLabel;
    TextField securePortTextField;
    Label IPLabel;
    TextField IPTextField;
    Label staticFilesLabel;
    TextField staticFilesTextField;
    Button clearButton;
    Label keepAliveLabel;
    TextField keepAliveTextField;
    Label threadsLabel;
    TextField threadsTextField;
    CheckBox authCheckBox;
    TextField  UserPassTextField;
    TextArea logTextArea;
   
    
    @Override
    public void start(Stage primaryStage) {

        // create UI controls
        activeButton = new ToggleButton("Activate Server");
        activeButton.setOnAction(e -> {
            activeButtonClicked();
        });
        portLabel = new Label("HTTP Port:");
        portTextField = new TextField("20000");
        portTextField.setPrefWidth(MediumTextFieldSize);
        securePortLabel = new Label("HTTPS Port:");
        securePortTextField = new TextField("20043");
        securePortTextField.setPrefWidth(MediumTextFieldSize);
        IPLabel = new Label("IP:");
        IPTextField = new TextField();
        IPTextField.setEditable(false);
        staticFilesLabel = new Label("Static Files Location:");
        staticFilesTextField = new TextField("/home/pedroamaral/HTTPServer/html");
        staticFilesTextField.focusedProperty().addListener((observable, oldValue, newValue) -> {
            if (!newValue) {
                // The text field lost focus
                validate_static_url(staticFilesTextField);
                // validate the URL in the Text Field
            }
        });
        clearButton = new Button("Clear Logs");
        clearButton.setOnAction(e -> {
            clearButtonClicked();
        });
        keepAliveLabel = new Label("HTTP1.1 Keep-Alive:");
        keepAliveTextField = new TextField("0");
        keepAliveTextField.setPrefWidth(SmallTextFieldSize);
        threadsLabel = new Label("Active Threads:");
        threadsTextField = new TextField();
        threadsTextField.setPrefWidth(SmallTextFieldSize);
        authCheckBox = new CheckBox();
        authCheckBox.setText("Authorization");
        UserPassTextField = new TextField("UserName:Password");
        
        logTextArea = new TextArea();

        // create UI layout
        GridPane gridPane = new GridPane();
        gridPane.setHgap(10);
        gridPane.setVgap(10);
        gridPane.setPadding(new Insets(10));
        gridPane.add(portLabel, 0, 0);
        gridPane.add(portTextField, 1, 0);
        gridPane.add(securePortLabel, 2, 0);
        gridPane.add(securePortTextField, 3, 0);
        gridPane.add(IPLabel, 0, 1);
        gridPane.add(IPTextField,1,1);
        gridPane.add(staticFilesLabel, 0, 2);
        gridPane.add(staticFilesTextField, 1, 2, 3, 1);
        gridPane.add(keepAliveLabel, 0, 3);
        gridPane.add(keepAliveTextField, 1, 3);
        gridPane.add(threadsLabel, 2, 3);
        gridPane.add(threadsTextField, 3, 3);
        gridPane.add(authCheckBox, 0, 4);
        gridPane.add(UserPassTextField,1,4);
        gridPane.add(activeButton, 0, 5);
        gridPane.add(clearButton, 1, 5);
       
        ;
        
        VBox vBox = new VBox(gridPane, logTextArea);
        // Exit application on window close. 

        // create scene and set it on the stage
        Scene scene = new Scene(vBox);
        primaryStage.setScene(scene);
        primaryStage.setTitle("MyApp");
        primaryStage.show();
        
        primaryStage.setOnCloseRequest(event -> {
        Platform.exit(); // Close the application
    });
 

    }
    
        /** Logs a message on the command line and on the text area */
    public void Log (String s) {
        Platform.runLater(()-> {
          logTextArea.appendText(s);
        });
         System.out.print (s);
    }
    
        /** Cleans the text area */
    private void clearButtonClicked() {                                             
        logTextArea.setText ("");
    } 
    
    /** Start and Stop de Server */
    private void activeButtonClicked() {                                             

       if (activeButton.isSelected()) {
            // Parse UI parameters            
            int port; //HTTp port
            try {
                port= Integer.parseInt (portTextField.getText ());
            } catch (NumberFormatException e) {
                Log ("Invalid port number\n");
                activeButton.setSelected (false);
                return;
            }
            int SecurePort; // HTTPS port
            try {
                SecurePort= Integer.parseInt(securePortTextField.getText());
            } catch (NumberFormatException e) {
                Log("Invalid https port number\n");
                activeButton.setSelected(false);
                return;
            }
            // Starts http web server
            try {
                server= new ServerSocket (port, MaxAcceptLog);
            } catch (java.io.IOException e) {
                Log ("Server start failure: " + e + "\n");
                activeButton.setSelected (false);
                return;
            }
             // Starts https server
            try {
                SSLServerSocketFactory sslSrvFact = sslContext.getServerSocketFactory();
                serverS =(SSLServerSocket)sslSrvFact.createServerSocket(SecurePort);
                serverS.setNeedClientAuth(false);
            } catch (java.io.IOException e) {
                Log("Server start failure: " + e + "\n");
                activeButton.setSelected(false);
                return;
            }
            // Gets local IP
            try {
                IPTextField.setText (InetAddress.getLocalHost ().getHostAddress ());
            } catch (UnknownHostException e) {
                Log ("Failed to get local IP: "+e+"\n");
            }
            // starts main thread
            MainThread= new ServerThread ( this, server);
            MainSecureThread= new ServerThread( this, serverS);
            n_threads= 0;
            threadsTextField.setText ("0");
            MainThread.start ();
            MainSecureThread.start ();
        } else {
            // Stops web server
            try {
                if (MainThread != null) {
                    MainThread.stop_thread ();
                    MainThread= null;
                }
                if (MainSecureThread != null) {
                    MainSecureThread.stop_thread ();
                    MainThread= null;
                }    
                    
                if (server != null) {
                    server.close ();
                    server= null;
                }
                if (serverS != null) {
                    serverS.close();
                    serverS = null;
                }
            } catch (IOException e) {
                Log ("Exception closing server socket: "+e+"\n");
            }
            IPTextField.setText ("");
            n_threads= 0;
            threadsTextField.setText ("0");
        }
    } 
    
    public void validate_static_url (TextField f) {
        String str= f.getText ();
        if (str.length () == 0)
            return;
        
        char separator;
        if (str.contains("\\"))
            // Running in Windows
            separator= '\\';
        else
            // Running in Linux
            separator= '/';
        
        if (str.charAt (str.length ()-1) != separator) {
            str= str + separator;
        } else {
            while ((str.length ()>1) && (str.charAt (str.length ()-2) == separator)) {
                str= str.substring (0, str.length ()-1);
            }
        }
        f.setText (str);
    }
    
     /** Callback called when a new HTTP connection thread starts */
    public void thread_started () {
        if (MainThread != null) {
            n_threads++;
            threadsTextField.setText (Integer.toString (n_threads));
        }
    }
    
       /** Callback called when a new HTTP connection thread starts */
    public void thread_ended () {
        if (MainThread != null) {
            n_threads--;
            threadsTextField.setText (Integer.toString (n_threads));
        }
    }
    
     /** Returns the HTTP port number
     * @return Port Value HTPP */
    public int getPortHTTP () {
        try {
            return Integer.parseInt (portTextField.getText ());
        } catch (NumberFormatException e) {
            return 0;
        }
    }
    /** Returns the HTTPS port number */
    public int getPortHTTPS() {
        try {
            return Integer.parseInt(securePortTextField.getText());
        } catch (NumberFormatException e) {
            return 0;
        }
    }
    
    /** Returns the keep alive interval value */
    public int getKeepAlive () {
        String txt= keepAliveTextField.getText ();
        if (txt == null)
            return 0;
        try {
            return 1000*Integer.parseInt (txt);
        } catch (NumberFormatException e) {
            Log ("Invalid KeepAlive value: '"+txt+"'\n");
            return 0;
        }
    }
    
      /** Returns the root html directory
     * @return  */
    public String getStaticFilesUrl () {
        return staticFilesTextField.getText ();
    }
    
    /** Returns the number of active connections */
    public int active_connects () {
        if (MainThread == null && MainSecureThread==null)
            return 0;
        return n_threads;
    }

    /** Returns true if a main_thread is active */
    public boolean active () {
        return (MainThread != null || MainSecureThread !=null );
    }
    
    /** Open up the KeyStore to obtain the Trusted Certificates.
     *  KeyStore is of type "JKS". Filename is "serverAppKeys.jks"
     *  and password is "myKeys".
     */
    private static void initContext() throws Exception {
        if (sslContext != null)
            return;
        
        try {
            // MAke sure that JSSE is available
           // Security.addProvider(new com.sun.net.ssl.internal.ssl.Provider());
            
            // Create/initialize the SSLContext with key material
            char[] passphrase = "password".toCharArray(); // if the certificate was created with the password = "password"
            
            KeyStore ksKeys;
            try {
                // First initialize the key and trust material.
                ksKeys= KeyStore.getInstance("JKS");
            } catch (Exception e) {
                System.out.println("KeyStore.getInstance: "+e);
                return;
            }
            ksKeys.load(new FileInputStream("keystore"), passphrase);
            System.out.println("KsKeys has "+ksKeys.size()+" keys after load");
            
            // KeyManager's decide which key material to use.
            KeyManagerFactory kmf =
                    KeyManagerFactory.getInstance("SunX509");
            kmf.init(ksKeys, passphrase);
            System.out.println("KMfactory default alg.: "+KeyManagerFactory.getDefaultAlgorithm());
            
            
            sslContext = SSLContext.getInstance("TLSv1.2"); 
            sslContext.init(
                    kmf.getKeyManagers(), null /*tmf.getTrustManagers()*/, null);
            
        } catch (Exception e) {
            System.out.println("Failed to read keystore and trustfile.");
            e.printStackTrace();
            throw e;
        }
    }

    public static void main(String[] args) {
        try {
            initContext();
        } catch (Exception e) {
            System.out.println("Error loading security context");
        }
        launch();
    }

}