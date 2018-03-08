package CI346.simplechat.server;

import javax.xml.bind.DatatypeConverter;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashSet;
import java.util.Map;
import java.util.Scanner;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static CI346.simplechat.server.Server.*;

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

            boolean wsClient = false;//is this client using the ws: protocol or plain text over sockets?

            // Create character streams for the socket.
            in = new BufferedReader(new InputStreamReader(
                    socket.getInputStream()));
            out = new PrintWriter(socket.getOutputStream(), true);

            String data = new Scanner(socket.getInputStream(),"UTF-8").useDelimiter("\\r\\n\\r\\n").next();
            Matcher get = Pattern.compile("^GET").matcher(data);
            if (get.find()) {
                wsClient = true;
                LOGGER.log(Level.INFO, "The Snark was a boojum");
                Matcher match = Pattern.compile("Sec-WebSocket-Key: (.*)").matcher(data);
                match.find();
                byte[] response = ("HTTP/1.1 101 Switching Protocols\r\n"
                        + "Connection: Upgrade\r\n"
                        + "Upgrade: websocket\r\n"
                        + "Sec-WebSocket-Accept: "
                        + DatatypeConverter
                        .printBase64Binary(
                                MessageDigest
                                        .getInstance("SHA-1")
                                        .digest((match.group(1) + "258EAFA5-E914-47DA-95CA-C5AB0DC85B11")
                                                .getBytes("UTF-8")))
                        + "\r\n\r\n")
                        .getBytes("UTF-8");

                socket.getOutputStream().write(response, 0, response.length);
            } else {
                LOGGER.log(Level.INFO, "Not a GET request");
            }

            // Request a name from this client.  Keep requesting until
            // a name is submitted that is not already used.  Note that
            // checking for the existence of a name and adding the name
            // must be done while locking the set of names.
            while (true) {
                out.println(PROTOCOL.SUBMIT_NAME.name());
                name = in.readLine();
                if (name == null) {
                    return;
                }
                LOGGER.log(Level.INFO, "Trying {0}", name);
                HashSet<String> names = getNames();
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
            out.println(PROTOCOL.NAME_ACCEPTED.name());
            putWriter(name, out);
            broadcast(toInfo(name+" entered the room"));

            // Accept messages from this client and broadcast them.
            // Ignore other clients that cannot be broadcasted to.
            while (true) {
                String input = in.readLine();
                if (input == null) {
                    return;
                } else if (input.equals(PROTOCOL.GOODBYE.name())) {
                    shutDown();
                    broadcast(toInfo(name+" left the room"));
                } else {
                    broadcast(toMessage(name, input));
                }
            }
        } catch (IOException e) {
            LOGGER.severe(e.getMessage());
        } catch (NoSuchAlgorithmException e) {
            LOGGER.severe(e.getMessage());
        }
        finally {
            // This client is going down!  Remove its name and its print
            // writer from the sets, and close its socket.
            shutDown();
        }
    }

    private void shutDown() {
        if (name != null) {
            removeName(name);
        }
        if (out != null) {
            removeWriter(out);
        }
        try {
            socket.close();
        } catch (IOException e) {
            LOGGER.severe(e.getMessage());
        }
    }

    private void broadcast(String msg) {
        LOGGER.log(Level.INFO, "Broadcasting {0}", msg);
        Map<String, PrintWriter> writers = getWriters();
        for (String name : writers.keySet()) {
            writers.get(name).println(msg);
        }
    }

    private String toMessage(String screenName, String body) {
        return String.format("%s %s: %s",
                PROTOCOL.MESSAGE.name(),
                screenName, body);
    }

    private String toInfo(String body) {
        return String.format("%s [%s]", PROTOCOL.MESSAGE.name(), body);
    }

    private byte[] decode(byte[] encoded) {
        byte[] decoded = new byte[encoded.length];
        byte[] key = byte {167, 225, 225, 210};

        for (int i = 0; i < encoded.length; i++) {
            decoded[i] = (byte)(encoded[i] ^ key[i & 0x3]);
        }
        return decoded;
    }
}
