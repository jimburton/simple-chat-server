package CI646.simplechat.client;

import CI646.simplechat.server.Server.PROTOCOL;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * The client follows the Chat Protocol which is as follows.
 * When the server sends SUBMIT_NAME the client replies with the
 * desired screen name.  The server will keep sending SUBMIT_NAME
 * requests as long as the client submits screen names that are
 * already in use.  When the server sends a line beginning
 * with NAME_ACCEPTED the client is now allowed to start
 * sending the server arbitrary strings to be broadcast to all
 * chatters connected to the server.  When the server sends a
 * line beginning with MESSAGE then all characters following
 * this string should be displayed in its message area.
 */
public class Client {

    BufferedReader in;
    PrintWriter out;
    JFrame frame = new JFrame("Simple Chat Server");
    JTextField textField = new JTextField(40);
    JTextArea messageArea = new JTextArea(8, 40);
    private static final Logger LOGGER = Logger.getLogger(Client.class.getName());

    /**
     * Constructs the client by laying out the GUI and registering a
     * listener with the textfield so that pressing Return in the
     * listener sends the textfield contents to the server.  Note
     * however that the textfield is initially NOT editable, and
     * only becomes editable AFTER the client receives the NAMEACCEPTED
     * message from the server.
     */
    public Client() {

        // Layout GUI
        textField.setEditable(false);
        messageArea.setEditable(false);
        frame.getContentPane().add(textField, "North");
        frame.getContentPane().add(new JScrollPane(messageArea), "Center");
        frame.pack();

        // Add Listeners
        textField.addActionListener(new ActionListener() {
            /**
             * Responds to pressing the enter key in the textfield by sending
             * the contents of the text field to the server.    Then clear
             * the text area in preparation for the next message.
             */
            public void actionPerformed(ActionEvent e) {
                send(textField.getText());
                textField.setText("");
            }
        });
    }

    /**
     * Prompt for and return the address of the server.
     */
    private String getServerAddress() {
        return JOptionPane.showInputDialog(
                frame,
                "Enter IP Address of the Server:",
                "localhost");
    }

    /**
     * Prompt for and return the desired screen name.
     */
    private String getName() {
        return JOptionPane.showInputDialog(
                frame,
                "Choose a screen name:",
                "Screen name selection",
                JOptionPane.PLAIN_MESSAGE);
    }

    /**
     * Connects to the server then enters the processing loop.
     */
    private void run() throws IOException {

        // Make connection and initialize streams
        String serverAddress = getServerAddress();
        Socket socket = new Socket(serverAddress, 9001);
        in = new BufferedReader(new InputStreamReader(
                socket.getInputStream()));
        out = new PrintWriter(socket.getOutputStream(), true);

        // Process all messages from server, according to the protocol.
        while (true) {
            String line = in.readLine();
            LOGGER.log(Level.INFO, "{0}", line);
            if (line != null) {
                if (line.startsWith(PROTOCOL.SUBMIT_NAME.name())) {
                    send(getName());
                } else if (line.startsWith(PROTOCOL.NAME_ACCEPTED.name())) {
                    textField.setEditable(true);
                } else if (line.startsWith(PROTOCOL.MESSAGE.name())) {
                    messageArea.append(line.substring(8) + "\n");
                }
            }
        }
    }

    private void send(String msg) {
        LOGGER.log(Level.INFO, "Sending message: {0}", msg);
        out.println(msg);
    }

    private void sayGoodbye() {
        send(PROTOCOL.GOODBYE.name());
    }

    /**
     * Runs the client as an application with a closeable frame.
     */
    public static void main(String[] args) throws Exception {
        final Client client = new Client();
        client.frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        client.frame.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        client.frame.addWindowListener(new java.awt.event.WindowAdapter() {
            @Override
            public void windowClosing(java.awt.event.WindowEvent windowEvent) {
                client.sayGoodbye();
                System.exit(0);

            }
        });
        client.frame.setVisible(true);
        client.run();
    }
}