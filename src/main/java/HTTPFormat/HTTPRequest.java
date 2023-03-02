/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package HTTPFormat;

import HTTPServer.App;
import java.util.Properties;
import java.io.BufferedReader;
import java.io.IOException;
import java.util.StringTokenizer;

/**
 *
 * @author pedroamaral 
 * 
 * Class that stores all information about a HTTP request
 * Incomplete Version 22/23
 */
public class HTTPRequest {
    
    public String method;   // stores the HTTP Method of the request
    public String UrlText;  // stores the url of the request
    public String version;  // stores the HTTP version of the request
    public Headers requestHeaders; // stores the HTTP headers of the request
    public Properties cookies; //stores cookies received in the Cookie Headers
    private final App UserInterface;  // log object UserInterface
    public String text;     //store possible contents in an HTTP request (for example POST contents)
    private final String idStr;    // idString for logging purposes
    public int LocalPort;  // local HTTP server port
    
    /** 
     * Creates a new instance of HTTPQuery
     * @param _UserInterface   log object
     * @param id    log id
     * @param LocalPort local HTTP server port
     */
    public HTTPRequest (App _UserInterface, String id, int LocalPort) {
        // initializes everything to null
        this.requestHeaders= new Headers (_UserInterface);
        this.UserInterface= _UserInterface;
        this.idStr= id;
        this.LocalPort= LocalPort;
        this.UrlText= null;
        this.method= null;
        this.version= null;
        this.text= null;
        this.cookies = new Properties();
    }

    public void Log(String s) {
         UserInterface.Log (idStr+ "  " + s );
         System.out.print (idStr + "  " + s);
    }
    
     /**
     * Get a header property value
     * @param hdrName   header name
     * @return          header value
     */
    public String getHeaderValue(String hdrName) {
        return requestHeaders.getHeaderValue(hdrName);
    }
    
    /**
     * Set a header property value
     * @param hdrName   header name
     * @param hdrVal    header value
     */
    public void setHeader(String hdrName, String hdrVal) {
        requestHeaders.setHeader(hdrName, hdrVal);
    }

    
    /** Returns the Cookie Properties object */
    public Properties get_cookies () {
        return this.cookies;
    }
    
    
    /**
     * Remove a header property name
     * @param hdrName   header name
     * @return true if successful
     */
    public boolean removeHeader(String hdrName) {
        return requestHeaders.removeHeader(hdrName);
    }
    
    
    /** Parses a new HTTP query from an input steam
     * @param TextReader   input stream Buffered Reader
     * @param echo  if true, echoes the received message to the screen
     * @return HTTPReplyCode.OK when successful, or HTTPReplyCode.BADREQ in case of error
     * @throws java.io.IOException 
     */
    public boolean parse_Request (BufferedReader TextReader, boolean echo) throws IOException {
        // Get first line
        String request = TextReader.readLine( );  	// Reads the first line
        if (request == null) {
            if (echo) Log ("Invalid request Connection closed\n");
            return false;
        }
        Log("Request: " + request + "\n");
        StringTokenizer st= new StringTokenizer(request);
        if (st.countTokens() != 3) {
           if (echo) Log ("Invalid request received: " + request + "\n");
           return false;  // Invalid request
        } 
        method= st.nextToken();    // Store HTTP method
        UrlText= st.nextToken();    // Store URL
        version= st.nextToken();  // Store HTTP version
     
        // read the remaining headers inside readHeaders method of headers object   
        requestHeaders.readHeaders(TextReader, echo);
        
        // check if the Content-Length size is different than zero. If true read the body of the request (that can contain POST data)
        int clength= 0;
        try {
            String len= requestHeaders.getHeaderValue("Content-Length");
            if (len != null)
                clength= Integer.parseInt (len);
            else if (!TextReader.ready ())
                clength= 0;
        } catch (NumberFormatException e) {
            if (echo) Log ("Bad request\n");
            return false;
        }
        if (clength>0) {
            // Length is not 0 - read data to string
            String str= new String ();
            char [] cbuf= new char [clength];
            //the content is not formed by line ended with \n so it need to be read char by char
            int n, cnt= 0;
            while ((cnt<clength) && ((n= TextReader.read (cbuf)) > 0)) {
                str= str + new String (cbuf);
                cnt += n;
            }
            if (cnt != clength) {
                Log ("Read request with "+cnt+" data bytes and Content-Length = "+clength+" bytes\n");
                return false;
            }
            text= str;
            if (echo)
                Log ("Contents('"+text+"')\n");
        }

        return true;
    }    
    
}
