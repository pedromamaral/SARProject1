/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package HTTPServer;

import HTTPFormat.*;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintStream;
//import static java.lang.Thread.NORM_PRIORITY;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Locale;
import java.util.TimeZone;

/**
 *
 * @author pedroamaral
 */
public class ConnectionThread extends Thread {
    App UserInterface;
    ServerSocket ServerSock;
    Socket client;
    DateFormat HttpDateFormat;
    
    /** Creates a new instance of httpThread */
    public ConnectionThread (App ServerUI, ServerSocket ServerSock, Socket client) {
        this.UserInterface= ServerUI;
        this.ServerSock= ServerSock;
        this.client = client;
        HttpDateFormat= new SimpleDateFormat ("EE, d MMM yyyy HH:mm:ss zz", Locale.UK);
        HttpDateFormat.setTimeZone (TimeZone.getTimeZone ("GMT"));
        setPriority ( NORM_PRIORITY - 1 );
    }
    
        /** Guess the MIME type from the file extension */
    String GuessMime (String fn) {
        String lcname = fn.toLowerCase ();
        int extenStartsAt = lcname.lastIndexOf ('.');
        if (extenStartsAt<0) {
            if (fn.equalsIgnoreCase ("makefile"))
                return "text/plain";
            return "unknown/unknown";
        }
        String exten = lcname.substring (extenStartsAt);
        // System.out.println("Ext: "+exten);
        if (exten.equalsIgnoreCase (".htm"))
            return "text/html";
        else if (exten.equalsIgnoreCase (".html"))
            return "text/html";
        else if (exten.equalsIgnoreCase (".gif"))
            return "image/gif";
        else if (exten.equalsIgnoreCase (".jpg"))
            return "image/jpeg";
        else
            return "application/octet-stream";
    }

    public void Log(String s) {
        UserInterface.Log ("" + client.getInetAddress ().getHostAddress () + ";"
                    + client.getPort () + "  " + s);
        System.out.print ("" + client.getInetAddress ().getHostAddress () +
                    ";" + client.getPort () + "  " + s);
    }
    
     @Override
    public void run( ) {

        HTTPResponse res= null;   // HTTP response object
        HTTPRequest req = null;   //HTTP request object
        PrintStream TextPrinter= null;

        try {
            /*get the input and output Streams for the TCP connection and build
              a text (ASCII) reader (TextReader) and writer (TextPrinter) */
            InputStream in = client.getInputStream( );
            BufferedReader TextReader = new BufferedReader(
                    new InputStreamReader(in, "8859_1" ));
            OutputStream out = client.getOutputStream( );
            TextPrinter = new PrintStream(out, false, "8859_1");
            
            //create an object to store the http request
            req= new HTTPRequest (UserInterface, client.getInetAddress ().getHostAddress () + ":"
                            + client.getPort (), ServerSock.getLocalPort ());  
            boolean ok= req.parse_Request (TextReader, true); //reads the input http request if everything was read ok it returnstrue
            if (ok){// if a valid request was received 
                //Create an HTTP response object. 
                res= new HTTPResponse(UserInterface,
                    client.getInetAddress ().getHostAddress () + ":" + client.getPort (),
                    App.server_name+" - "+InetAddress.getLocalHost().getHostName ()+"-"+UserInterface.server.getLocalPort ());

                //API URL received 
                if (req.UrlText.toLowerCase().endsWith ("api")) 
                {
                    while ( req.UrlText.startsWith ("/") )
                      req.UrlText = req.UrlText.substring (1);  ///remove "/" from url_txt
                      JavaRESTAPI api= new JavaRESTAPI ();
                      try {
                           Log("run JavaAPI\n");
                            api.doGet (client, req.requestHeaders, req.get_cookies(), res);
                          } catch (Exception e) {
                              res.set_error (HTTPReplyCode.BADREQ, req.version);
                          }
                }else {
                    // Get file with contents
                    String filename= UserInterface.getStaticFilesUrl() + req.UrlText + (req.UrlText.equals("/")?"index.htm":"");
                    System.out.println("Filename= "+filename);
                    File f= new File(filename);

                    if (f.exists() && f.isFile()) {
                        // Define reply contents
                        res.setCode(HTTPReplyCode.OK);
                        res.setVersion(req.version);
                        res.setFileHeaders(new File(filename), GuessMime(filename));
                        // NOTICE that only the first line of the reply is sent!
                        // No additional headers are defined!
                    } else {
                        System.out.println( "File not found" );
                        res.set_error(HTTPReplyCode.NOTFOUND, req.version);
                        // NOTICE that some code is missing in HTTPAnswer!
                    }
                }
                // Send reply
                res.send_Answer(TextPrinter, true, true);
            }
            

            in.close();
            TextPrinter.close();
            out.close();
        } catch ( IOException e ) {
            if (UserInterface.active())
                System.out.println( "I/O error " + e );
        } finally {
            try {
                client.close();
            } catch (Exception e) {
                // Ignore
                System.out.println("Error closing client"+e);
            }
            UserInterface.thread_ended();
            Log("Closed TCP connection\n");
        }
    }
    
    
}
