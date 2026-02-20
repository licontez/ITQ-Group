package example.docs.exception;

public class RegistryRegistrationException extends RuntimeException {
    public RegistryRegistrationException(String message) {
        super(message);
    }
}