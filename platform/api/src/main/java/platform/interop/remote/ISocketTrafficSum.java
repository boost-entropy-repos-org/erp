package platform.interop.remote;

public interface ISocketTrafficSum {

    void incrementIn(long in);
    
    void incrementOut(long out);
}
