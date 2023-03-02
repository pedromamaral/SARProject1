/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package HTTPFormat;

/**
 *
 * @author pedroamaral
 */
public class HTTPReplyCode {
    
    public static final int NOTDEFINED= -1;
    public static final int OK= 200;
    public static final int NOTMODIFIED= 304;
    public static final int TMPREDIRECT= 307;
    public static final int BADREQ= 400;
    public static final int UNAUTHORIZED= 401;
    public static final int NOTFOUND= 404;
    public static final int PROXYAUTHENTIC=407;
    public static final int NOTIMPLEMENTED= 501;

    
    private int code;
    private String codeTxt;
    private String version;
    
    
    /** Default constructor */
    public HTTPReplyCode() {
        code= NOTDEFINED;
        codeTxt= null;
        version= "HTTP/1.1";
    }
    
    public HTTPReplyCode(int code, String version) {
        this.code= code;
        this.codeTxt= HTTPReplyCode.codeText(code);
        this.version= version;
    }
    
    public HTTPReplyCode(HTTPReplyCode _code) {
        code= _code.code;
        codeTxt= (_code.codeTxt != null) ? _code.codeTxt : null;
        version= (_code.version != null) ? _code.version : null;
    }

    public int getCode() { return code; }
    public String getCodeTxt() { return codeTxt; }
    public String getVersion() { return version; }

    public void setCode(int code) { 
        this.code= code; 
        this.codeTxt= codeText(code); 
    }
    /** Overwrites default code text
     * @param codeTxt */
    public void setCodeTxt(String codeTxt) { 
        this.codeTxt= codeTxt; 
    }
    public void setVersion(String version) {
        this.version= version;
    }

    public boolean isError() {
        return (code >= 400);
    }

    public boolean isUndef () {
        return code == -1;
    }

    @Override
    public String toString() {
        return version+" "+code+" "+codeTxt;
    }
    
    /** Auxiliary function that returns the default code text */
    public static String codeText (int code) {
        switch (code) {
            case OK:
                return "OK";
            case NOTMODIFIED:
                return "Not Modified";
            case TMPREDIRECT:
                return "Temporary Redirect";
            case BADREQ:
                return "Bad Request";
            case NOTFOUND:
                return "File Not Found";
            case UNAUTHORIZED:
                return "Unauthorized";
            case NOTIMPLEMENTED:
                return "Not Implemented";
            case PROXYAUTHENTIC:
                return "Proxy Authentication Required";
            default:
                return null;
        }
    }
    

    
}
