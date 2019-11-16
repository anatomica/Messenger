package Messenger.Client.Controller;
import Messenger.Client.gson.*;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import org.apache.commons.lang.exception.ExceptionUtils;
import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;

public class Controller implements Initializable {

    @FXML
    public Button sendMessageButton;
    @FXML
    public TextField textMessage;
    @FXML
    public TextArea textArea;
    @FXML
    public MenuItem closeButton;
    @FXML
    public MenuItem clearChat;
    @FXML
    public MenuItem changeNick;
    @FXML
    public MenuItem howChangeNick;
    @FXML
    public TextField loginField;
    @FXML
    public PasswordField passField;
    @FXML
    public HBox authPanel;
    @FXML
    public VBox chatPanel;
    @FXML
    public ListView<String> clientList;
    public String nickName;

    private MessageService messageService;
    private String selectedNickname;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        try {
            this.messageService = new MessageService(this, true);
        } catch (Exception e) {
            showError(e);
        }
    }

    private void showError (Exception e) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Упс! ЧТо-то пошло не так!");
        alert.setHeaderText(e.getMessage());
        VBox dialogPaneContent = new VBox();
        Label label = new Label("Stack Trace:");
        String stackTrace = ExceptionUtils.getStackTrace(e);
        TextArea textArea = new TextArea();
        textArea.setText(stackTrace);
        dialogPaneContent.getChildren().addAll(label, textArea);
        alert.getDialogPane().setContent(dialogPaneContent);
        alert.setResizable(true);
        alert.showAndWait();
        e.printStackTrace();
    }

    @FXML
    private void closeButtonAction(){
        System.exit(0);
    }

    @FXML
    private void sendMessage (ActionEvent event) {
        sendMessageAction();
    }

    @FXML
    private void sendText (ActionEvent event) {
        sendMessageAction();
    }

    private void sendMessageAction() {
        String message = textMessage.getText();
        Message msg = buildMessage(message);
        messageService.sendMessage(msg.toJson());
        textMessage.clear();
        selectedNickname = clientList.getSelectionModel().getSelectedItem();
        if (selectedNickname == null || selectedNickname.equals("< ДЛЯ ВСЕХ >")) {
            textArea.appendText("Я: " + prepareToView(message) + System.lineSeparator());
        }
        if (!selectedNickname.equals("< ДЛЯ ВСЕХ >")) {
            textArea.appendText("Я [private] " + selectedNickname + ": " + prepareToView(message) + System.lineSeparator());
        }
    }

    private Message buildMessage(String message) {
        selectedNickname = clientList.getSelectionModel().getSelectedItem();
        if (selectedNickname == null || selectedNickname.equals("< ДЛЯ ВСЕХ >")) {
            return buildPublicMessage(message);
        }
        if (!selectedNickname.equals("< ДЛЯ ВСЕХ >")) {
            PrivateMessage msg = new PrivateMessage();
            msg.from = nickName;
            msg.to = selectedNickname;
            msg.message = message;
            return Message.createPrivate(msg);
        }
        return buildPublicMessage(message);
    }

    private Message buildPublicMessage(String message) {
        PublicMessage publicMsg = new PublicMessage();
        publicMsg.from = nickName;
        publicMsg.message = message;
        Message msg = new Message();
        msg.command = Command.PUBLIC_MESSAGE;
        msg.publicMessage = publicMsg;
        return msg;
    }

    private String prepareToView(String message) {
        return message.replaceAll("/w\\s+", "[private]: ");
    }

    public void shutdown() {
        try {
            messageService.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @FXML
    public void sendAuth (ActionEvent actionEvent) {
        String login = loginField.getText();
        String password = passField.getText();
        AuthMessage msg = new AuthMessage();
        msg.login = login;
        msg.password = password;
        Message authMsg = Message.createAuth(msg);
        messageService.sendMessage(authMsg.toJson());
    }

    public void clearChatAction(ActionEvent actionEvent) {
        textArea.clear();
    }

    public void changeNickAction(ActionEvent actionEvent) throws IOException {
        String nick = textMessage.getText();
        ChangeNick msg = new ChangeNick();
        msg.nick = nick;
        Message newNick = Message.createNick(msg);
        messageService.sendMessage(newNick.toJson());
        nickName = nick;
        textMessage.clear();
    }

    public void howChangeNickAction(ActionEvent actionEvent) {
        textArea.appendText("Для того, чтобы сменить Ник, \nвведите его в нижней панели для отправки сообщений " +
                "\nи, не нажимая отправить, \nперейдите в меню 'Правка' и нажмите 'Сменить Ник'" + System.lineSeparator());
    }
}