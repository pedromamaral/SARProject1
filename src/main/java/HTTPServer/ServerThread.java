/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package HTTPServer;
import java.net.ServerSocket;

/**
 * Server Thread
 * @author pedroamaral
 */
public class ServerThread extends Thread {
    
        App UserInterface;
        ServerSocket ss;
        volatile boolean active;
        
        ServerThread ( App ui, ServerSocket ss ) {
            this.UserInterface= ui;
            this.ss= ss;
        }
        
        public void wake_up () {
            this.interrupt ();
        }
        
        public void stop_thread () {
            active= false;
            UserInterface.thread_ended();
            this.interrupt ();
        }
        
        @Override
        public void run () {
            System.out.println (
                    "\n******************** "+App.server_name+" started ********************\n");
            active= true;
            while ( active ) {
                try {
                    ConnectionThread conn = new ConnectionThread ( UserInterface, ss, ss.accept () );
                    conn.start ( );
                    UserInterface.thread_started ();
                } catch (java.io.IOException e) {
                    UserInterface.Log ("IO exception: "+ e + "\n");
                    active= false;
                }
            }
        }
}
