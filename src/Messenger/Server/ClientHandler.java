package Messenger.Server;

import Messenger.Server.gson.*;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;

public class ClientHandler {

    private MyServer myServer;
    private String clientName;
    private Socket socket;
    private DataInputStream in;
    private DataOutputStream out;

    ClientHandler(Socket socket, MyServer myServer) {
        try {
            this.socket = socket;
            this.myServer = myServer;
            this.in = new DataInputStream(socket.getInputStream());
            this.out = new DataOutputStream(socket.getOutputStream());

            new Thread(() -> {
                try {
                    while (true) {
                        if (authentication()) {
                            break;
                        }
                    }
                    readMessages();
                } catch (IOException e) {
                    e.printStackTrace();
                } finally {
                    closeConnection();
                }
            }).start();
        } catch (IOException e) {
            throw new RuntimeException("Ошибка создания подключения к клиенту!", e);
        }
    }

    private void readMessages() throws IOException {
        while (true) {
            String clientMessage = in.readUTF();
            System.out.printf("Сообщение: '%s' от клиента: %s%n", clientMessage, clientName);
            Message m = Message.fromJson(clientMessage);
            switch (m.command) {
                case PUBLIC_MESSAGE:
                    PublicMessage publicMessage = m.publicMessage;
                    myServer.broadcastMessage(publicMessage.from + ": " + publicMessage.message, this);
                    break;
                case PRIVATE_MESSAGE:
                    PrivateMessage privateMessage = m.privateMessage;
                    myServer.privateMessage(privateMessage.from + " [private]: " + privateMessage.message, privateMessage.to, ClientHandler.this);
                    break;
                case END:
                    return;
            }
        }
    }

    private void closeConnection() {
        myServer.unsubscribe(this);
        myServer.broadcastMessage(clientName + " is offline");
        try {
            socket.close();
        } catch (IOException e) {
            System.err.println("Failed to close socket!");
            e.printStackTrace();
        }
    }

    // "/auth login password"
    private boolean authentication() throws IOException {
        String clientMessage = in.readUTF();
        Message message = Message.fromJson(clientMessage);
        if (message.command == Command.AUTH_MESSAGE) {
            AuthMessage authMessage = message.authMessage;
            String login = authMessage.login;
            String password = authMessage.password;
            String nick = myServer.getAuthService().getNickByLoginPass(login, password);
            if (nick == null) {
                sendMessage("Неверные логин/пароль!");
                return false;
            }
            if (myServer.isNickBusy(nick)) {
                sendMessage("Учетная запись уже используется!");
                return false;
            }
            sendMessage("/authok " + nick);
            clientName = nick;
            myServer.broadcastMessage(clientName + " is online");
            myServer.subscribe(this);
        }
        return true;
    }

    public void sendMessage(String message)  {
        try {
            out.writeUTF(message);
        } catch (IOException e) {
            System.err.println("Ошибка отправки сообщения пользователю: " + clientName + " : " + message);
            e.printStackTrace();
        }
    }

    String getClientName() {
        return clientName;
    }
}
