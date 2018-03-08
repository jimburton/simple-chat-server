package CI346.simplechat.server;

import java.io.PrintWriter;
import java.net.ServerSocket;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * A multithreaded chat room server.  When a client connects the
 * server requests a screen name by sending the client the
 * SUBMIT_NAME message, and keeps requesting a name until
 * a unique one is received.  After a client submits a unique
 * name, the server acknowledges with NAME_ACCEPTED.  Then
 * all messages from that client will be broadcast to all other
 * clients that have submitted a unique screen name.  The
 * broadcast messages are prefixed with MESSAGE.
 *
 */
public class Server {

    private static final int PORT = 9001;
    private static HashSet<String> names = new HashSet<>();
    private static Map<String, PrintWriter> writers = new HashMap<>();
    private static final Logger LOGGER = Logger.getLogger( Server.class.getName());

    /** The messages that make up our chat protocol **/
    public enum PROTOCOL {
        SUBMIT_NAME
        , NAME_ACCEPTED
        , MESSAGE
        , GOODBYE
    }

    public static void main(String[] args) throws Exception {
        LOGGER.log(Level.INFO, "The chat server is running.");
        ServerSocket listener = new ServerSocket(PORT);
        try {
            while (true) {
                new ServerThread(listener.accept()).start();
            }
        } finally {
            listener.close();
        }
    }

    /**
     * The set of all names of clients in the chat room.  Maintained
     * so that we can check that new clients are not registering name
     * already in use.
     */
    public static HashSet<String> getNames() {
        return names;
    }

    public static void removeName(String name) {
        names.remove(name);
    }

    public static Map<String, PrintWriter> getWriters() {
        return writers;
    }

    public static void putWriter(String name, PrintWriter out) {
        writers.put(name, out);
    }

    public static void removeWriter(PrintWriter out) {
        writers.remove(out);
    }
}