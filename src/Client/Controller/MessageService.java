package Client.Controller;
import Client.gson.*;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.scene.control.Alert;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import java.io.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.TimeUnit;

class MessageService {

    private static final String HOST_ADDRESS_PROP = "server.address";
    private static final String HOST_PORT_PROP = "server.port";
    private static final String STOP_SERVER_COMMAND = "/end";

    private String hostAddress;
    private int hostPort;

    private TextArea textArea;
    private TextField textMessage;
    private Controller controller;
    private boolean needStopServerOnClosed;
    private Network network;
    private String nickname;
    private Thread timeWait;
    private int waitTime = 120;  // ожидание в секундах
    private int howManyMsgLoad = 5; // кол-во загрузки сообщений из истории

    MessageService(Controller controller, boolean needStopServerOnClosed) {
        this.textMessage = controller.textMessage;
        this.textArea = controller.textArea;
        this.controller = controller;
        this.needStopServerOnClosed = needStopServerOnClosed;
        initialize();
    }

    private void initialize() {
        readProperties();
        startConnectionToServer();
        timeWait();
    }

    private void readProperties() {
        Properties serverProperties = new Properties();
        try (InputStream inputStream = getClass().getResourceAsStream("application.properties")) {
            serverProperties.load(inputStream);
            hostAddress = serverProperties.getProperty(HOST_ADDRESS_PROP);
            hostPort = Integer.parseInt(serverProperties.getProperty(HOST_PORT_PROP));
        } catch (IOException e) {
            throw new RuntimeException("Failed to read application.properties file", e);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Invalid port value", e);
        }
    }

    private void startConnectionToServer() {
        try {
            this.network = new Network(hostAddress, hostPort, this);
        } catch (IOException e) {
            throw new ServerConnectionException("Failed to connect to server", e);
        }
    }

    private void timeWait() {
        timeWait = new Thread(() -> {
            try {
                for (int i = 1; i < waitTime; i++) {
                    System.out.println(i);
                    TimeUnit.SECONDS.sleep(1);
                }
                alertAndClose();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        });
        timeWait.start();
    }

    private void alertAndClose() {
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.setTitle("Ошибка аутентификации!");
            alert.setHeaderText("Предупреждение!");
            alert.setContentText("Превышено допустимое время на аутентификацию! \nПожалуйста, перезапустите приложение!");
            alert.showAndWait();
            controller.shutdown();
            System.exit(0);
        });
    }

    void sendMessage(String message) {
        network.send(message);
        if (!textMessage.getText().equals(""))
            ChatHistory("Я: " + textMessage.getText());
    }

    void processRetrievedMessage(String message) throws IOException {
        if (message.startsWith("/authok")) {
            timeWait.interrupt();
            nickname = message.split("\\s+")[1];
            controller.nickName = nickname;
            controller.authPanel.setVisible(false);
            controller.chatPanel.setVisible(true);
            loadChatHistory();
        } else if (controller.authPanel.isVisible()) {
            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.setTitle("Ошибка аутентификации!");
            alert.setContentText(message);
            alert.showAndWait();
        } else {
            if (message.startsWith("{") && message.endsWith("}")) {
                Message msg = Message.fromJson(message);
                ClientListMessage clientListMessage = msg.clientListMessage;
                controller.clientList.setItems(FXCollections.observableArrayList(clientListMessage.online));
            } else {
                textArea.appendText(message + System.lineSeparator());
                if (!message.equals("")) {
                    if (!message.endsWith("лайн!"))
                        ChatHistory(message);
                }
            }
        }
    }

    void close() throws IOException {
        if (needStopServerOnClosed) {
            sendMessage(STOP_SERVER_COMMAND);
        }
        network.close();
        System.exit(0);
    }

    private void ChatHistory(String messageText) {
        File file = new File("src/Client/Controller/ChatHistory.txt");
        System.out.println(file.canWrite());
        try (FileWriter writer = new FileWriter(file, true);) {
            writer.write(messageText + "\n");
        } catch (IOException e) {
            System.out.println(e.getMessage());
        }
    }

    private void loadChatHistory() throws IOException {
        textArea.appendText("Последние " + howManyMsgLoad + " сообщений:");
        BufferedReader br = new BufferedReader(new FileReader("src/Client/Controller/ChatHistory.txt"));
        List<String> listHistory = new ArrayList<>();

        String tmp;
        while ((tmp = br.readLine()) != null) {
            listHistory.add("\n" + tmp);
        }
        Collections.reverse(listHistory);

        List<String> reverseListHistory = new ArrayList<>();
        int count = 1;
        for (int i = 0; i < listHistory.size(); i++) {
            if (count <= howManyMsgLoad) {
                reverseListHistory.add(listHistory.get(i));
                count++;
            }
        }
        Collections.reverse(reverseListHistory);

        StringBuilder chatHistory = new StringBuilder();
        for (String s : reverseListHistory) {
            chatHistory.append(s);
        }
        textArea.appendText(chatHistory + "\n");

    }
}