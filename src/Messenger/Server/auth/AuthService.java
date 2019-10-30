package Messenger.Server.auth;
import com.sun.istack.internal.Nullable;

public interface AuthService {

    void start();
    void stop();

    @Nullable
    String getNickByLoginPass(String login, String pass);

}
