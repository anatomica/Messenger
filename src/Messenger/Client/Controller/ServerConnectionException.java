package Messenger.Client.Controller;

class ServerConnectionException extends RuntimeException {

    ServerConnectionException(String message, Throwable cause) {
        super(message, cause);
    }
}
