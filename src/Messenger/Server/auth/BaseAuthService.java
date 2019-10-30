package Messenger.Server.auth;
import java.util.Arrays;
import java.util.List;

public class BaseAuthService implements AuthService {

    private static class Entry {
        private String login;
        private String password;
        private String nick;

        Entry(String login, String password, String nick) {
            this.login = login;
            this.password = password;
            this.nick = nick;
        }
    }

    private final List<Entry> entries = Arrays.asList (
            new Entry("1", "1", "Оля"),
            new Entry("2", "2", "Макс"),
            new Entry("3", "3", "Ника"),
            new Entry("4", "4", "Олег"),
            new Entry("5", "5", "Влад")
    );

    @Override
    public void start() {
        System.out.println("Сервис авторизации запущен!");
    }

    @Override
    public void stop() {
        System.out.println("Сервис автоизации остановлен!");
    }

    @Override
    public String getNickByLoginPass(String login, String pass) {
        for (Entry entry : entries) {
            if (entry.login.equals(login) && entry.password.equals(pass)) {
                return entry.nick;
            }
        }
        return null;
    }
}
