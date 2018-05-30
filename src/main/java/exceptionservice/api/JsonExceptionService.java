package exceptionservice.api;

/**
 * Created by root on 5/27/17.
 */
public interface JsonExceptionService {

    public void jsonException();
    public void gatewayNotFound();
    public void cmdNotFound();
    public void sessionOccupied();
    public void pathAlreadyInstalled(long pId);
    public void sessionIdNotFound();
    public void startEndTimeNotFound();
}
