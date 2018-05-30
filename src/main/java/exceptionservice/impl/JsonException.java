package exceptionservice.impl;

import com.google.gson.JsonObject;
import exceptionservice.api.JsonExceptionService;
import org.apache.felix.scr.annotations.*;
import org.onosproject.core.CoreService;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rmqservice.api.RmqService;

/**
 * Created by root on 5/27/17.
 */

@Component(immediate = true)
@Service
public class JsonException implements JsonExceptionService {

    private final Logger log = LoggerFactory.getLogger(getClass());

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected CoreService coreService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected RmqService rmqService;


    @Activate
    protected void activate(ComponentContext context) {
        log.info("Exception Service Started");
    }

    @Deactivate
    protected void deactivate() {
        log.info("Exception Service Stopped");
    }

    @Override
    public void pathAlreadyInstalled(long pId) {
        byte[] body = null;
        JsonObject outer = new JsonObject();
        outer.addProperty("ErrorCode", "3");
        outer.addProperty("Description", "Path already installed!!");
        outer.addProperty("InstalledPathId", pId);
        body = bytesOf(outer);
        rmqService.consumerResponse(body);
    }

    @Override
    public void jsonException() {
        byte[] body = null;
        JsonObject outer = new JsonObject();
        outer.addProperty("ErrorCode", "7");
        outer.addProperty("Description", "Something wrong with command or Path already installed");
        body = bytesOf(outer);
        rmqService.consumerResponse(body);
    }

    @Override
    public void startEndTimeNotFound() {
        byte[] body = null;
        JsonObject outer = new JsonObject();
        outer.addProperty("ErrorCode", "8");
        outer.addProperty("Description", "Start or End time is not correct!!");
        body = bytesOf(outer);
        rmqService.consumerResponse(body);
    }

    @Override
    public void gatewayNotFound() {
        byte[] body = null;
        JsonObject outer = new JsonObject();
        outer.addProperty("ErrorCode", "1");
        outer.addProperty("Description", "Gateway not configured. Please check initial configuration!");
        body = bytesOf(outer);
        rmqService.consumerResponse(body);
    }

    @Override
    public void cmdNotFound() {
        byte[] body = null;
        JsonObject outer = new JsonObject();
        outer.addProperty("ErrorCode", "2");
        outer.addProperty("Description", "Command not supported. Please check command format!");
        body = bytesOf(outer);
        rmqService.consumerResponse(body);
    }

    @Override
    public void sessionOccupied() {
        byte[] body = null;
        JsonObject outer = new JsonObject();
        outer.addProperty("ErrorCode", "3");
        outer.addProperty("Description", "Session already locked for someone. " +
                "Please try again later!");
        body = bytesOf(outer);
        rmqService.consumerResponse(body);
    }

    @Override
    public void sessionIdNotFound() {
        byte[] body = null;
        JsonObject outer = new JsonObject();
        outer.addProperty("ErrorCode", "6");
        outer.addProperty("Description", "Session ID not found." +
                "Please correct session ID and try again!");
        body = bytesOf(outer);
        rmqService.consumerResponse(body);
    }

    private byte[] bytesOf(JsonObject jo) {
        return jo.toString().getBytes();
    }
}
