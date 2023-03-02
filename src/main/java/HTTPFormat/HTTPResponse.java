/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package HTTPFormat;

import HTTPServer.App;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;
/**
 *
 * @author pedroamaral
 */
public class HTTPResponse {
        /**
     * Reply code information
     */
    public HTTPReplyCode code;
    /**
     * Reply headers data
     */
    public Headers responseHeaders; // stores the HTTP headers for the response
    public ArrayList<String> setCookies;   // List to store Set-cookie header fields
    /**
     * Reply contents
     * They are stored either in a text buffer or in a file
     */
    public String text; // buffer with reply contents for dynamic API responses or server generated HTML code
    public File file;   // file used if text == null, for responses that contain a file  
    App UserInterface;
    String IdStr;  // Thread id - for logging purposes
/**
     * Creates a new instance of HTTPAnswer
     * @param _UserInterface
     * @param IdStr
     * @param server_name
     */
    public HTTPResponse(App _UserInterface, String IdStr, String server_name) {
        this.code = new HTTPReplyCode(); // code constains an instance of the HTTPReplyCode Class thar contains HTTP code values and an HTTP version field.
        this.IdStr = IdStr;
        this.UserInterface = _UserInterface;
        responseHeaders = new Headers (UserInterface);  // Headers object to store response HTTP headers  
        setCookies = new ArrayList<>(); // Array List of Strings to contain the Strings that make up the several values of the Set_Cookie Header. 
        text = null;
        file = null;
        /**
         * define Server header field name
         */
        responseHeaders.setHeader("Server", server_name);
    }

    void Log(String s) {
         UserInterface.Log (IdStr+ "  " + s );
         System.out.print (IdStr + "  " + s);
    }
   
    /* 
    Method to set the HTTP reply code of the answer
    */
    public void setCode(int _code) {
        code.setCode(_code);
    }
    /* 
    Method to set the HTTP version of the answer
    */
    public void setVersion(String v) {
        code.setVersion(v);
    }
     /* 
    Method to set an HTTP header of the answer
    */
    public void setHeader(String name, String value) {
        responseHeaders.setHeader(name, value);
    }
  /* 
    Method to add a cookie value to the list of cookies that are to be sent in Set-Cookie Headers
    */
    public void setCookie(String setcookieLine) {
        setCookies.add(setcookieLine);
    }

    /** Sets the headers needed in a reply with a static file content and fill
     * the file property with the File object of the static file to send
     * @param _f
     * @param mime_enc */
    public void setFileHeaders(File _f, String mime_enc) {
        file = _f;
        // header lines not set in 'Headers' object!
        // ...
        Log("Header fields not defined in HTTPAnswer.set_file\n");
    }

    /** Sets the headers needed in a reply with a locally generated HTML string
     * (_text object) and fill the text property with the String object 
     * containing the HTML to send
     * @param _text */
    public void setTextHeaders(String _text) {
        text = _text;
        // header lines not set in 'Headers' object!
        // ...
        Log("Header fields not defined in HTTPAnswer.set_text\n");
    }

    /**
     * Returns the current value of the answer code
     * @return 
     */
    public int getCode() {
        return code.getCode();
    }

    /**
     * Returns a string with the first line of the answer
     * @return 
     */
    public String get_first_line() {
        return code.toString();
    }

    /**
     * Returns an iterator over all header names
     * @return 
     */
    public Enumeration<Object> get_Iterator_parameter_names() {
        return responseHeaders.getAllHeaderNames();
    }

    /**
     * Returns the array list with all cookie Strings to use as values in Set-Cookie Headers
     * @return 
     */
    public ArrayList<String> getSetCookies() {
        return setCookies;
    }

    /** Sets the "Date" header field with the local date in HTTP format */
    void setDate() {
        DateFormat httpformat =
                new SimpleDateFormat("EE, d MMM yyyy HH:mm:ss zz", Locale.UK);
        httpformat.setTimeZone(TimeZone.getTimeZone("GMT"));
        responseHeaders.setHeader("Date", httpformat.format(new Date()));
    }


    /** Prepares an HTTP answer with an error code
     * @param _code
     * @param version */
    public void set_error(int _code, String version) {
        setVersion(version);
        setDate();
        code.setCode(_code);
        if (code.getCodeTxt() == null) {
            code.setCode(HTTPReplyCode.BADREQ);
        }

        if (!version.equalsIgnoreCase("HTTP/1.0")) {
            responseHeaders.setHeader("Connection", "close");
        }
        // Prepares a web page with an error description
        String txt = "<HTML>\r\n";
        txt = txt + "<HEAD><TITLE>Error " + code.getCode() + " -- " + code.getCodeTxt()
                + "</TITLE></HEAD>\r\n";
        txt = txt + "<H1> Error " + code.getCode() + " : " + code.getCodeTxt() + " </H1>\r\n";
        txt = txt + "  by " + responseHeaders.getHeaderValue("Server") + "\r\n";
        txt = txt + "</HTML>\r\n";

        // Set the header properties
        setTextHeaders(txt);
    }


    /** Sends the HTTP reply to the client using 'pout' text device
     * @param TextPrinter
     * @param send_data indicates if data is present in the response or only headers
     * @param echo
     * @throws java.io.IOException */
    public void send_Answer(PrintStream TextPrinter, boolean send_data, boolean echo) throws IOException {
        if (code.getCodeTxt() == null) {
            code.setCode(HTTPReplyCode.BADREQ);
        }
        if (echo) {
            Log("Answer: " + code.toString() + "\n");
        }
        TextPrinter.print(code.toString() + "\r\n");
        /**
         * Send all headers except set_cookie using headers Object writeHeaders
         */
        // ...

        /**
         * Check if there are cookies to send if so add the corresponding Set-Cookie Headers
         * Set-Cookies have to be sent manually using TextPrinter without using the Headers object 
         * since it uses a Properties Object to store the headers and there can be multiple Set-cookie headers 
         * and a Properties cannot have two Keys with the same value
         */
        // ...
        Log("HTTPAnswer does not yet send headers\n");
        TextPrinter.print("\r\n");

        if (send_data) {

            if (text != null) {
                TextPrinter.print(text);
            } else if (file != null) {
                try (FileInputStream fin = new FileInputStream(file)) {
                    byte [] data = new byte [fin.available()];
                    fin.read( data );  // Read the entire file to buffer 'data'
                    // IMPORTANT - Please modify this code to send a file chunk-by-chunk
                    //             to avoid having CRASHES with BIG FILES
                    Log("HTTPAnswer may fail for large files - please modify it\n");
                    TextPrinter.write(data);
                }
                catch (IOException e ) {
                  System.out.println( "I/O error opeening FileInputStream " + e );
                }
            } else if ((code.getCode() != HTTPReplyCode.NOTMODIFIED)&&(code.getCode() != HTTPReplyCode.TMPREDIRECT)) {
                Log("Internal server error sending answer\n");
            }
        }
        TextPrinter.flush();
    }
}
