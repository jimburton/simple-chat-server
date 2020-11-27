package CI646.simplechat.server;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * A handler thread class.  Handlers are spawned from the listening
 * loop and are responsible for a dealing with a single client
 * and broadcasting its messages.
 */
public class ServerThread extends Thread {
    private String name;
    private Socket socket;
    private BufferedReader in;
    private PrintWriter out;
    private static final Logger LOGGER = Logger.getLogger( Server.class.getName());

    public ServerThread(Socket socket) {
        this.socket = socket;
    }

    /**
     * Services this thread's client by repeatedly requesting a
     * screen name until a unique one has been submitted, then
     * acknowledges the name and registers the output stream for
     * the client in a global set, then repeatedly gets inputs and
     * broadcasts them.
     */
    public void run() {
        try {
            // Create character streams for the socket.
            in = new BufferedReader(new InputStreamReader(
                    socket.getInputStream()));
            out = new PrintWriter(socket.getOutputStream(), true);

            // Request a name from this client.  Keep requesting until
            // a name is submitted that is not already used.  Note that
            // checking for the existence of a name and adding the name
            // must be done while locking the set of names.
            while (true) {
                out.println(Server.PROTOCOL.SUBMIT_NAME.name());
                name = in.readLine();
                if (name == null) {
                    return;
                }
                LOGGER.log(Level.INFO, "Trying {0}", name);
                HashSet<String> names = Server.getNames();
                synchronized (names) {
                    if (!names.contains(name)) {
                        LOGGER.log(Level.INFO, "Accepting {0}", name);
                        names.add(name);
                        break;
                    }
                }
            }

            // Now that a successful name has been chosen, add the
            // socket's print writer to the set of all writers so
            // this client can receive broadcast messages.
            out.println(Server.PROTOCOL.NAME_ACCEPTED.name());
            Server.putWriter(name, out);
            broadcast(toInfo(name+" entered the room"));

            // Accept messages from this client and broadcast them.
            // Ignore other clients that cannot be broadcasted to.
            while (true) {
                String input = in.readLine();
                if (input == null) {
                    return;
                } else if (input.startsWith(Server.PROTOCOL.PM.name())) {
                    LOGGER.log(Level.INFO, "Sending PM to {0}", getPMName(input));
                    sendPM(getPMName(input), input);
                } else if (input.startsWith(Server.PROTOCOL.GET_USERS.name())) {
                    LOGGER.log(Level.INFO, "Sending users");
                    sendUsers();
                } else if (input.equals(Server.PROTOCOL.GOODBYE.name())) {
                    shutDown();
                    broadcast(toInfo(name+" left the room"));
                } else {
                    broadcast(toMessage(name, input));
                }
            }
        } catch (IOException e) {
            LOGGER.severe(e.getMessage());
        } finally {
            // This client is going down!  Remove its name and its print
            // writer from the sets, and close its socket.
            shutDown();
        }
    }

    private void shutDown() {
        if (name != null) {
            Server.removeName(name);
        }
        if (out != null) {
            Server.removeWriter(out);
        }
        try {
            socket.close();
        } catch (IOException e) {
            LOGGER.severe(e.getMessage());
        }
    }

    private void sendUsers() {
        LOGGER.log(Level.INFO, "Sending users");
        out.println(Server.PROTOCOL.GET_USERS+" "+ Arrays.toString(Server.getWriters().keySet().toArray()));
    }

    private void broadcast(String msg) {
        LOGGER.log(Level.INFO, "Broadcasting {0}", msg);
        Map<String, PrintWriter> writers = Server.getWriters();
        for (String name : writers.keySet()) {
            writers.get(name).println(msg);
        }
    }

    private void sendPM(String user, String msg) {
        LOGGER.log(Level.INFO, "Sending PM to {0} : {1}", new Object[]{user, msg});
        LOGGER.log(Level.INFO, "Has key? {0}", Server.getWriters().containsKey(user));
        Server.getWriters().get(user).println(msg);
    }

    public static String toMessage(String screenName, String body) {
        return String.format("%s %s: %s",
                Server.PROTOCOL.MESSAGE.name(),
                screenName, body);
    }

    public static String toInfo(String body) {
        return String.format("%s [%s]", Server.PROTOCOL.MESSAGE.name(), body);
    }

    public static String getPMName(String line) {
        return line.split(" ")[1];
    }
}
