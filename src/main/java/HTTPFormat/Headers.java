package HTTPFormat;

import HTTPServer.App;
import java.util.*;
import java.io.*;

/**
 *
 * @author pedroamaral
 */
public class Headers {
  
    public Properties headers;                            // Single value list
    private final App UserInterface;                              // UserInterface
    
     /**
     * Creates an empty list of headers
     * @param log log object
     */
    Headers(App _UserInterface) {
        headers = new Properties();
        this.UserInterface = _UserInterface;
    }
     /**
     * Clears the contents of the headers properties object
     */
    public void clear() {
        headers.clear();
    }
    
    /**
     * Store a header value; 
     * @param hdrName   header name
     * @param hdrVal    header value
     */
    public void setHeader(String hdrName, String hdrVal) {
        String storedHdrVal= headers.getProperty(hdrName);
        if (storedHdrVal == null) {
            headers.setProperty(hdrName, hdrVal);
        }       
    }

    /**
     * Returns the value of a property (returns the last one)
     * @param hdrName   header name
     * @return  the last header value
     */
    public String getHeaderValue(String hdrName) {
        return headers.getProperty(hdrName);
    }


    /**
     * Removes all the values of a header
     * @param hdrName   header name
     * @return true if a header was removed, false otherwise
     */
    public boolean removeHeader(String hdrName) {
        if (headers.containsKey(hdrName)) {
            headers.remove(hdrName);
            return true;
        } else
            return false;
    }
    
    /**
     * Returns an enumeration of all header names
     * @return an enumeration object
     */
    public Enumeration<Object> getAllHeaderNames() {
        return headers.keys();
    }
    
    /**
     * Parses an input stream for a sequence of headers, until an empty line is reached
     * @param in    input stream
     * @param echo  if true, echoes the headers received on screen
     * @return  true if headers were read successfully 
     * @throws IOException when a socket error occurs
     */
    public boolean readHeaders(BufferedReader TextReader, boolean echo) throws IOException {
        // Get other header parameters
        String req;
        while (((req = TextReader.readLine()) != null) && (req.length() != 0)) {
            int ix;
            if (echo) UserInterface.Log("hdr(" + req + ")\n");
            if ((ix = req.indexOf(':')) != -1) {
                String hdrName = req.substring(0, ix);
                String hdrVal = req.substring(ix + 1).trim();
                setHeader(hdrName, hdrVal);         
            } else {
                if (echo) UserInterface.Log("Invalid header\n");
                return false;
            }
        }
        return true;
    }

    /**
     * write the headers list to an output stream
     * @param pout  output stream
     * @param echo  if true, echoes the headers sent on screen
     * @throws IOException when a socket error occurs
     */
    public void writeHeaders(PrintStream TextPrinter, boolean echo) throws IOException {
        for (String hdrName : headers.stringPropertyNames()) {
                // Single value parameter
                String val = headers.getProperty(hdrName);
                TextPrinter.print(hdrName + ": " + val + "\r\n");
                if (echo) 
                    UserInterface.Log(hdrName + ": " + val + "\n");
        }
    }
}
