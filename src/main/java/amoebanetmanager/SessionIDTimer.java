package amoebanetmanager;

import com.google.common.collect.Multimap;
import costservice.CostService;
import costservice.CostServiceImpl;
import eventester.EventsService;
import eventester.EventsServiceImpl;
import initialconfigservice.InitConfigService;
import initialconfigservice.impl.InitConfigImpl;
import org.apache.felix.scr.annotations.*;
import org.onosproject.net.ConnectPoint;
import org.onosproject.net.DeviceId;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pathmanagerservice.api.BdePathService;
import pathmanagerservice.impl.BdePathServiceImpl;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimerTask;

public class SessionIDTimer extends TimerTask {

    private final Logger log = LoggerFactory.getLogger(getClass());
    protected InitConfigService initConfigService;

    public void setConfig(InitConfigService initConfigService) {
        this.initConfigService = initConfigService;
    }
    public InitConfigService getInitConfigService() {
        return initConfigService;
    }


    @Override
    public void run() {
        completeTask();
    }

    private void completeTask() {
        //assuming it takes 20 secs to complete the task
        log.info("Come after 10 seconds... Wait!!!");
        DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm");
        Date date = new Date();

        try {
            Date date1;
            String datestr = "2017-08-24 23:58";
            date1 = dateFormat.parse(datestr);
            if(date.compareTo(date1) > 0) {
                log.info("date is greater than date1");
            } else if(date.compareTo(date1) == 0) {
                log.info("date is equal");
            } else {
                log.info("date is less than date1");
            }
            if(dateFormat.format(date1).equals(dateFormat.format(date))) {
                log.info("Date and Time is EQUAL");
            } else {
                log.info("Date and Time not equal");
            }
        } catch (ParseException e) {
            e.printStackTrace();
        }

        log.info("Current data and time {}", dateFormat.format(date));
        log.info("initial config {}", getInitConfigService().gatewaysInfo());

    }


    public void testMethod(){
        log.info("called test method");
    }



}
