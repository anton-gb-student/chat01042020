package client;

import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.ListView;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.net.Socket;
import java.net.URL;
import java.util.ResourceBundle;

public class Controller implements Initializable {
    @FXML
    public TextArea textArea;
    @FXML
    private TextField textField;
    @FXML
    private HBox authPanel;
    @FXML
    private TextField loginField;
    @FXML
    private PasswordField passwordField;
    @FXML
    private HBox msgPanel;
    @FXML
    private ListView<String> clientList;

    private Socket socket;
    private DataInputStream in;
    private DataOutputStream out;

    private final String IP_ADDRESS = "localhost";
    private final int PORT = 8189;

    private final String CHAT_TITLE_EMPTY = "Chat 2020";

    private boolean authenticated;
    private String nickname;
    private String login;         //////////////////////////

    private Stage regStage;

    public void setAuthenticated(boolean authenticated) {
        this.authenticated = authenticated;
        authPanel.setVisible(!authenticated);
        authPanel.setManaged(!authenticated);
        msgPanel.setVisible(authenticated);
        msgPanel.setManaged(authenticated);
        clientList.setVisible(authenticated);
        clientList.setManaged(authenticated);
        if (!authenticated) {
            nickname = "";
            setTitle(CHAT_TITLE_EMPTY);
            History.stop();        ////////////////
        }
        textArea.clear();
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        authenticated = false;
        Platform.runLater(() -> {
            Stage stage = (Stage) textArea.getScene().getWindow();
            stage.setOnCloseRequest(new EventHandler<WindowEvent>() {
                @Override
                public void handle(WindowEvent event) {
                    System.out.println("bue");
                    if (socket != null && !socket.isClosed()) {
                        try {
                            out.writeUTF("/end");

                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }
            });

        });
    }

    public void connect() {
        try {
            socket = new Socket(IP_ADDRESS, PORT);
            in = new DataInputStream(socket.getInputStream());
            out = new DataOutputStream(socket.getOutputStream());

            new Thread(() -> {
                try {
                    //цикл аутентификации
                    while (true) {
                        String str = in.readUTF();

                        if (str.startsWith("/authok ")) {
                            nickname = str.split(" ")[1];
                            setAuthenticated(true);

                            /////////////////
                            textArea.clear();
                            textArea.appendText(History.getLast100LinesOfHistory(login));
                            History.start(login);


                            break;
                        }
                        textArea.appendText(str + "\n");
                    }

                    setTitle(CHAT_TITLE_EMPTY + " : " + nickname);

                    //цикл работы
                    while (true) {
                        String str = in.readUTF();
                        if (str.startsWith("/")) {
                            if (str.equals("/end")) {
                                break;
                            }
                            if (str.startsWith("/clientlist ")) {
                                String[] token = str.split(" ");
                                Platform.runLater(() -> {
                                    clientList.getItems().clear();
                                    for (int i = 1; i < token.length; i++) {
                                        clientList.getItems().add(token[i]);
                                    }
                                });
                            }

                            if (str.startsWith("/yournickis ")) {
                                nickname = str.split(" ")[1];
                                setTitle(CHAT_TITLE_EMPTY + " : " + nickname);
                            }

                        } else {
                            textArea.appendText(str + "\n");
                            History.writeLine(str);          //////////////
                        }
                    }
                } catch (EOFException e) {
                    System.out.println(e.getMessage());
                } catch (IOException e) {
                    e.printStackTrace();
                } finally {
                    try {
                        socket.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    setAuthenticated(false);
                }
            }).start();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void sendMsg() {
        try {
            out.writeUTF(textField.getText());
            textField.clear();
            textField.requestFocus();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void tryToAuth() {
        if (socket == null || socket.isClosed()) {
            connect();
        }

        try {
            out.writeUTF("/auth " + loginField.getText().trim() + " " + passwordField.getText().trim());

            login = loginField.getText();          ////////////////

            passwordField.clear();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    void setTitle(String title) {
        Platform.runLater(() -> {
            ((Stage) textArea.getScene().getWindow()).setTitle(title);
        });
    }

    public void clickClientList(MouseEvent mouseEvent) {
//        System.out.println(clientList.getSelectionModel().getSelectedItem());
        String receiver = clientList.getSelectionModel().getSelectedItem();
        textField.setText("/w " + receiver + " ");
    }

    private Stage createRegWindow() {
        Stage stage = null;

        try {
            FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("/reg.fxml"));
            Parent root = fxmlLoader.load();
            stage = new Stage();
            stage.initModality(Modality.APPLICATION_MODAL);

            RegController regController = fxmlLoader.getController();
            regController.controller = this;

            stage.setTitle("Регистрация");
            stage.setScene(new Scene(root, 300, 200));

        } catch (IOException e) {
            e.printStackTrace();
        }

        return stage;
    }

    public void tryToReg() {
        if (regStage == null) {
            regStage = createRegWindow();
        }
        regStage.show();
    }

    public void tryRegistration(String login, String password, String nickname) {
        String msg = String.format("/reg %s %s %s", login, password, nickname);

        if (socket == null || socket.isClosed()) {
            connect();
        }

        try {
            out.writeUTF(msg);
        } catch (IOException e) {
            e.printStackTrace();
        }
        System.out.println("сообщение с просьбой регистрации ушла");
    }
}
