package server;

import java.io.FileInputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Vector;
import java.util.logging.*;


public class Server {

    private static final Logger serverLogger = Logger.getLogger(Server.class.getName());
    private static Handler serverHandler;

    static {
        try {
            LogManager logManager = LogManager.getLogManager();
            logManager.readConfiguration(new FileInputStream("logging.properties"));
            serverHandler = new FileHandler("server.log", true);
            serverHandler.setFormatter(new SimpleFormatter());
            serverLogger.addHandler(serverHandler);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }




    private Vector<ClientHandler> clients;
    private AuthService authService;

    public AuthService getAuthService() {
        return authService;
    }

    public Server() {
        clients = new Vector<>();
//        authService = new SimpleAuthService();
        if (!SQLHandler.connect()) {
            serverLogger.log(Level.WARNING, "Не удалось подключиться к БД \n");
            throw new RuntimeException("Не удалось подключиться к БД");
        }
        authService = new DBAuthServise();


        ServerSocket server = null;
        Socket socket = null;

        try {
            server = new ServerSocket(8189);
            serverLogger.log(Level.INFO, "Сервер запущен \n");

            while (true) {
                socket = server.accept();
                serverLogger.log(Level.INFO, "Клиент подключился \n");

                new ClientHandler(socket, this);
            }

        } catch (IOException e) {
            serverLogger.log(Level.WARNING, "error: ", e);
            e.printStackTrace();
        } finally {
            SQLHandler.disconnect();

            try {
                server.close();
            } catch (IOException e) {
                serverLogger.log(Level.WARNING, "error: ", e);
                e.printStackTrace();
            }
        }
    }

    public void broadcastMsg(String nick, String msg) {
        for (ClientHandler c : clients) {
            c.sendMsg(nick + ": " + msg);
        }
    }

    public void privateMsg(ClientHandler sender, String receiver, String msg) {
        String message = String.format("[ %s ] private [ %s ] : %s",
                sender.getNick(), receiver, msg);

        if (sender.getNick().equals(receiver)) {
            sender.sendMsg(message);
            return;
        }

        for (ClientHandler c : clients) {
            if (c.getNick().equals(receiver)) {
                c.sendMsg(message);
                sender.sendMsg(message);
                return;
            }
        }

        sender.sendMsg("not found user: " + receiver);
    }


    public void subscribe(ClientHandler clientHandler) {
        clients.add(clientHandler);
        broadcastClientList();
    }

    public void unsubscribe(ClientHandler clientHandler) {
        clients.remove(clientHandler);
        broadcastClientList();
    }

    public boolean isLoginAuthorized(String login) {
        for (ClientHandler c : clients) {
            if (c.getLogin().equals(login)) {
                return true;
            }
        }
        return false;
    }

    public void broadcastClientList() {
        StringBuilder sb = new StringBuilder("/clientlist ");
        for (ClientHandler c : clients) {
            sb.append(c.getNick() + " ");
        }

        String msg = sb.toString();
        for (ClientHandler c : clients) {
            c.sendMsg(msg);
        }
    }

}
