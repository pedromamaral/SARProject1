
package HTTPServer;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;
/**
 *
 * @author pedroamaral
 */
public class groupDB {
    private String bdname;
    private static Properties pgroups= new Properties(); // Group data
    private static SortedSet<String> set = new TreeSet<String>();  // Sorted set of group names
                
    /** Creates a new instance of groupDB */
    public groupDB (String _bdname) {
        // Put initializations here
        this.bdname= _bdname;
        try {
            FileInputStream in= new FileInputStream ( bdname );
            pgroups.load(in);
        }
        catch(Exception e) {
            System.err.println("Warning while loading groups database: "+e);            
        }
        // create set with group names
        for (Iterator<Object> it= pgroups.keySet().iterator(); it.hasNext (); ) {
            String entry= (String)(it.next());
            if (entry.endsWith(":"))
                set.add(entry.substring(0, entry.length()-1));
        }        
    }
    
    /** Saves group database to file */
    public void save_group_db() {
        try {
            File file= new File(bdname);
            if (!file.exists ())
                file.createNewFile();
            FileOutputStream out= new FileOutputStream ( file );
            for (Iterator<Object> it= pgroups.keySet().iterator(); it.hasNext (); ) {
                String entry= (String)(it.next());
                System.out.println("Wrote '"+entry+"' to DB file");
                pgroups.store(out, entry);
            }
            out.close();
        }
        catch(Exception e) {
            System.err.println("Warning while saving groups database: "+e);            
        }        
    }

    /** Returns sorted list with names */
     public SortedSet<String> get_sorted_set() {
         return set;
     }

    /** Returns a HTML table with the members of the group */
    public String table_group_html() {
        StringBuilder buf= new StringBuilder();
        
        buf.append("<table border=\"1\">\r\n<tr>\r\n<th>Grupo<th colspan=\"3\">Membros</th>\r\n</th>");
        for (Iterator<String> it= set.iterator(); it.hasNext (); ) {
            String group= (String)(it.next());
            buf.append("<tr>\r\n<td> ").append(group).append("</td> <td> ");
            buf.append(pgroups.getProperty(group+":n1","")).append(" - ");
            buf.append(JavaRESTAPI.postString2htmlString(pgroups.getProperty(group+":nam1","")));
            buf.append("</td>" + "<td> ");
            buf.append(pgroups.getProperty(group+":n2","")).append(" - ");
            buf.append(JavaRESTAPI.postString2htmlString(pgroups.getProperty(group+":nam2","")));
            buf.append("</td>" + "<td> ");
            buf.append(pgroups.getProperty(group+":n3","")).append(" - ");
            buf.append(JavaRESTAPI.postString2htmlString(pgroups.getProperty(group+":nam3","")));
            buf.append("</td>\r\n</tr>\r\n");            
        }
        buf.append("</table>\r\n");
        return buf.toString();
    }
    
    /** Store a group information */
    public void store_group(String group, boolean contar, String n1, String nam1,
            String n2, String nam2, String n3, String nam3) {
        if (group.length() == 0)
            return;
        String old= pgroups.getProperty(group+":");
        if (old == null) {
            System.out.println("Group database: new group = '"+group+"'");
            pgroups.setProperty(group+":", "0");
            set.add(group);
        } else {
            System.out.println ("Group database: updating group = '"+group+"'");
        }
        if (contar) {
            try {
                int n= Integer.parseInt(old);
                pgroups.setProperty(group+":", ""+(n+1));
            }
            catch (Exception e) {
                pgroups.setProperty(group+":", "1");
            }
        }
        pgroups.setProperty(group+":n1", n1);
        pgroups.setProperty(group+":nam1", nam1);
        pgroups.setProperty(group+":n2", n2);
        pgroups.setProperty(group+":nam2", nam2);
        pgroups.setProperty(group+":n3", n3);
        pgroups.setProperty(group+":nam3", nam3);
        DateFormat httpformat;
        httpformat= new SimpleDateFormat ("EE, d MMM yyyy HH:mm:ss zz", Locale.UK);
        httpformat.setTimeZone (TimeZone.getTimeZone ("GMT"));
        // Prepare cookie
        pgroups.setProperty(group+":lastUpdate", httpformat.format (new Date(System.currentTimeMillis() + (long)30*24*60*60*1000)));
        save_group_db();
    }
    
    /** Removes a group from the database */
    public void remove_group(String group) {
        if (group.length() == 0)
            return;
        set.remove(group);
        pgroups.remove(group+":");
        pgroups.remove(group+":n1");
        pgroups.remove(group+":nam1");
        pgroups.remove(group+":n2");
        pgroups.remove(group+":nam2");
        pgroups.remove(group+":n3");
        pgroups.remove(group+":nam3");
        pgroups.remove(group+":lastUpdate");
        save_group_db();
    }
    
    /** Get group information */
    public String get_group_info(String group, String prop) {
        if (group.length() == 0)
            return "";
        return pgroups.getProperty (group+":"+prop, "");
    }
    
}
