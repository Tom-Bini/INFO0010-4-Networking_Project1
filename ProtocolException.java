public class ProtocolException extends Exception {

    private final String protocolMessage; // ex. "WRONG" ou "INVALID BET"

    public ProtocolException(String protocolMessage) {
        super(protocolMessage);
        this.protocolMessage = protocolMessage;
    }

    public String getProtocolMessage() {
        return protocolMessage;
    }
    
}