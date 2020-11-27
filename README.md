# simple-chat-server

A lab exercise for CI646 adapted from http://cs.lmu.edu/~ray/notes/javanetexamples/.

This repository contains a simple Java socket server and a GUI client. Download the code 
and start by reading the `Server` and `ServerThread` classes. Run the application by
launching the server by running the `main` method in the `Server` class, then launching 
several clients by running the `main` method in `Client`. Send a few messages and watch the
logging output in the terminals attached to each process.


`Server` is the entry point 
for the server side of the application. It listens forever on `localhost:9001`, 
waiting for clients to connect. When a connection arrives, a new instance of `ServerThread`, 
which is a subclass of `Thread`, is spawned and started to handle the connection.

The chat protocol is made up of the messages in the `Server.PROTOCOL` `enum`. In the `run`
method of `ServerThread` the first thing that happens is to get references to the input and
output streams of the client socket, then send the message `SUBMIT_NAME` to the client. The next
text that is received by the `ServerThread` is taken to be the name, which is checked for 
uniqueness. If no other client is currently using the name, the `ServerThread` sends the message
`NAME_ACCEPTED` and broadcasts the fact that a new client has arrived to all other clients. 
Otherwise, it sends `SUBMIT_NAME` until a unique name is received.

The client first asks the user for the address of the server and a username. The `run` method 
contains a loop that listens forever for messages from the server. Once the `NAME_ACCEPTED`
message is received, any text entered into the textfield is sent to the server, which broadcasts
the message to all connected clients.

## Exercise 1

Adapt the application so that users can retrieve a list of everyone else attached to the server.
In the `Server` class, extend the protocol with a new message type called `GET_USERS`. In 
`ServerThread`, create a method called `sendUsers` that gets the list of all users from the 
`writers` `Map` then sends it to the current client only. (The usernames are the keys in this 
map, so you can use `Map.keySet`.)

In the `Client` class, add a clause to the `if` statement in the `run` method. This clause should
be triggered if the incoming text begins with the name of the `GET_USERS` message. If so, strip
off the name of the message and prepend the string `In the room with: ` to the result before
displaying it in the message area. So, when this message is received, the output will be something
like

    In the room with: [Bob, Alice]
    
With this done, you should be able to enter the message `GET_USERS` and see a list of all users.

## Exercise 2

Adapt the application so that users can send *private messages* to others. In the `Server` class, 
extend the protocol with a new message type called `PM`. In `ServerThread`, create a method called 
`sendPM` with this signature:

    private void sendPM(String user, String msg)
    
The method should work in a similar way to `broadcast` but it takes the name of a user and a 
message, then sends the message to that user only. 

Add a clause to the `if` statement in the `run` method that will be triggered if the incoming 
message begins with the name of your new message. If so, you will send the message using your 
new method, `sendPM`. To do so, you need to extract the name of the recipient from the line of 
input. You can expect that the input is in this format:

    PM USERNAME REST_OF_MESSAGE
    
Use the method [`String.split`](https://docs.oracle.com/javase/7/docs/api/java/lang/String.html#split(java.lang.String)) 
to extract the name.

In the `Client` class, add a clause to the `if` statement that will match if an incoming line
of text begins with the name of your new message type. If so, strip off the name of the message 
type and present the message to the user in a way that lets them know this is a private message:

    [NAME_OF_SENDER] REST_OF_MESSAGE
    
(You can use `split` again to pick out the name of the sender.) Now you should be able to send
a private message to a user called `Jim` by entering a line of text like so:

    PM Jim Hello Jim
    
Launch several clients to make sure that only the right user receives the message.