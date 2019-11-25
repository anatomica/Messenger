package Client.Controller;
import Client.gson.ClientListMessage;
import Client.gson.Message;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.scene.control.Alert;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import java.io.*;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
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

    MessageService(Controller controller, boolean needStopServerOnClosed) throws URISyntaxException, IOException {
        this.textMessage = controller.textMessage;
        this.textArea = controller.textArea;
        this.controller = controller;
        this.needStopServerOnClosed = needStopServerOnClosed;
        initialize();
    }

    private void initialize() {
        createFile();
        // getFile();
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
            writeChatHistory("Я: " + textMessage.getText());
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
                        writeChatHistory(message);
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

    private String resource = "/Client/ChatHistory.txt";
    private URL res = getClass().getResource(resource);
    private File fileHistory;
    private String pathToHistory;

    private void createFile() {
        try {
            URI uri = MessageService.class.getProtectionDomain().getCodeSource().getLocation().toURI();
            pathToHistory = new File(uri).getParent() + "\\ChatHistory.txt";
            System.out.println(pathToHistory);
            fileHistory = new File(pathToHistory);
            if (fileHistory.createNewFile()) System.out.println("Файл истории создан!");
            else System.out.println("Файл истории ранее создан и найден!");
        } catch (IOException | URISyntaxException e) {
            System.out.println(e.getMessage());
        }
    }

    private void getFile() {
        try {
//            InputStream input = getClass().getResourceAsStream(resource);
//            OutputStream out = new FileOutputStream(file);
//
//            int read;
//            byte[] bytes = new byte[1024];
//            while ((read = input.read(bytes)) != -1) {
//                out.write(bytes, 0, read);
//            } out.close();
//        } catch (IOException ex) {
//            System.out.println(ex.getMessage());
//        }

            System.out.println(MessageService.class.getProtectionDomain().getCodeSource().getLocation().toURI().getPath());
//            System.out.println(MessageService.class.getResource("/Client/ChatHistory.txt").toExternalForm());
//            System.out.println(Objects.requireNonNull(this.getClass().getClassLoader().getResource("Client/ChatHistory.txt")).toExternalForm());
//            System.out.println(Objects.requireNonNull(getClass().getClassLoader().getResource("Client/ChatHistory.txt")).getFile());
//            Path path = Paths.get(MessageService.class.getResource(".").toURI());
//            System.out.println(path.getParent());
//            System.out.println(path.getParent().getParent());
//            System.out.println(Paths.get(MessageService.class.getResource("\\ChatHistory.txt").toURI()));

        } catch (URISyntaxException e) {
            System.out.println(e.getMessage());
        }
    }

    private void writeChatHistory(String messageText) {
        try (BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(
                new FileOutputStream(fileHistory, true), "UTF-8"))) {
            bw.write(messageText + "\n");
        } catch (IOException e) {
            System.out.println(e.getMessage());
        }
    }

    private void loadChatHistory() throws IOException {
        if (fileHistory.createNewFile()) textArea.appendText("\n");
        textArea.appendText("Последние " + howManyMsgLoad + " сообщений:");
        BufferedReader br = new BufferedReader(new InputStreamReader(
                new FileInputStream(fileHistory), "UTF-8"));
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