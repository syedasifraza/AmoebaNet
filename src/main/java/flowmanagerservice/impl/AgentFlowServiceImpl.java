package flowmanagerservice.impl;

import flowmanagerservice.api.AgentFlowService;
import initialconfigservice.InitConfigService;
import org.apache.felix.scr.annotations.*;
import org.onlab.packet.IpAddress;
import org.onlab.packet.IpPrefix;
import org.onlab.packet.VlanId;
import org.onlab.util.ItemNotFoundException;
import org.onosproject.core.ApplicationId;
import org.onosproject.net.DeviceId;
import org.onosproject.net.PortNumber;
import org.onosproject.net.flow.*;
import org.onosproject.net.meter.*;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.Iterator;
import java.util.Set;
import java.util.stream.StreamSupport;

@Component(immediate = true)
@Service
public class AgentFlowServiceImpl implements AgentFlowService {

    public static final int PRIORITY = 500;
    public static final int TIME_OUT = 120;

    private final Logger log = LoggerFactory.getLogger(getClass());

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected FlowRuleService flowRuleService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected InitConfigService initConfigService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected MeterService meterService;


    ApplicationId appId;
    @Activate
    protected void activate(ComponentContext context) {
        appId = initConfigService.getAppId();
        log.info("Flow Manager Service Activated");

    }

    @Deactivate
    protected void deactivate() {

        log.info("Flow Manager Stopped");
        meterService.getAllMeters().iterator().forEachRemaining(n->{
            log.info("In meter delete");
            if(n.appId().equals(appId)) {
                log.info("if conddition of meter delete is true+");
                Meter tmpMeter1 = meterService.getMeter(n.deviceId(), n.id());
                MeterRequest meterRequest = meterToMeterRequest(tmpMeter1, "REMOVE");
                meterService.withdraw(meterRequest, tmpMeter1.id());
            }
        });
    }

    @Override
    public void installFlows(DeviceId deviceId, PortNumber inPort, PortNumber outPort,
                             String srcIP, String dstIP, String srcPort, String dstPort,
                             Double rate, Set<Long> fId, String trafficType, String vlanId,
                             String oscarsVlanId) {

        log.info("\n Device IDs {}, srcIP {}, dstIP {}, srcPort {}, dstPort {}, rate {}",
                deviceId, srcIP, dstIP, srcPort, dstPort, rate);

        Long fId1, fId2;
        MeterId meterId1 = null;
        MeterId meterId2 = null;
        log.info("before if condition");
        log.info("rate value {}", rate.longValue());
        if(trafficType.equals("1")) {
            log.info("If condition true");
            Band band = DefaultBand.builder()
                    .ofType(Band.Type.DROP)
                    .burstSize(rate.longValue())
                    .withRate(rate.longValue())
                    .build();
            log.info("before MeterRequest");

            MeterRequest meterRequest = DefaultMeterRequest.builder()
                    .forDevice(deviceId)
                    .fromApp(appId)
                    .burst()
                    .withUnit(Meter.Unit.KB_PER_SEC)
                    .withBands(Collections.singleton(band))
                    .add();
            //= MeterId.meterId(10);

            //= MeterId.meterId(20);

            try {
                log.info("before meter add");
                meterId1 = meterService.submit(meterRequest).id();
                log.info("First meter added meterId1 {}", meterId1);
                log.info("Start 2nd meter adding");
                meterId2 = meterService.submit(meterRequest).id();


                log.info("second meter added meterid2 {}", meterId2);
            } catch (Exception e) {log.info("add meters command");
                e.printStackTrace();
                return;
            }


            //log.info("Meter Id {}", meterId);
        /*try {
            TimeUnit.SECONDS.sleep(3);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }*/
        } else {
            log.info("In else");
            meterId1 = MeterId.meterId(1);
            meterId2 = MeterId.meterId(2);
        }

        log.info("After else");
        fId1 = pushFlows(deviceId, inPort, outPort,
                srcIP, dstIP, meterId1, trafficType,
                vlanId, oscarsVlanId, 1);
        fId2 = pushFlows(deviceId, outPort, inPort,
                dstIP, srcIP, meterId2, trafficType,
                vlanId, oscarsVlanId, 2);

        //log.info("Meters {}", meter.appId());
        fId.add(fId1);
        fId.add(fId2);
        fId.add(meterId1.id());
        fId.add(meterId2.id());

    }

    @Override
    public void removeFlowsByAppId() {

        flowRuleService.removeFlowRulesById(appId);


    }

    @Override
    public void removePathId(DeviceId deviceId, Set<Long> flowId) {

        long fId1, fId2, meterId1, meterId2;
        final Iterator<Long> flows = flowId.iterator();
        final Iterable<FlowEntry> flowEntries =
                flowRuleService.getFlowEntries(deviceId);

        fId1 = flows.next();
        fId2 = flows.next();
        meterId1 = flows.next();
        meterId2 = flows.next();

        if (!flowEntries.iterator().hasNext()) {
            throw new ItemNotFoundException("Device not found");
        }

        StreamSupport.stream(flowEntries.spliterator(), false)
                .filter(entry -> entry.id().value() == fId1)
                .forEach(flowRuleService::removeFlowRules);

        StreamSupport.stream(flowEntries.spliterator(), false)
                .filter(entry -> entry.id().value() == fId2)
                .forEach(flowRuleService::removeFlowRules);

        MeterId mid1 = MeterId.meterId(meterId1);
        final Meter tmpMeter1 = meterService.getMeter(deviceId, mid1);
        if (tmpMeter1 != null) {
            final MeterRequest meterRequest = meterToMeterRequest(tmpMeter1, "REMOVE");
            meterService.withdraw(meterRequest, tmpMeter1.id());
        }

        MeterId mid2 = MeterId.meterId(meterId2);
        final Meter tmpMeter2 = meterService.getMeter(deviceId, mid2);
        if (tmpMeter2 != null) {
            final MeterRequest meterRequest = meterToMeterRequest(tmpMeter2, "REMOVE");
            meterService.withdraw(meterRequest, tmpMeter2.id());


        }

    }

    private MeterRequest meterToMeterRequest(Meter meter, String operation) {
        MeterRequest.Builder builder;
        MeterRequest meterRequest;

        if (meter == null) {
            return null;
        }

        if (meter.isBurst()) {
            builder = DefaultMeterRequest.builder()
                    .fromApp(meter.appId())
                    .forDevice(meter.deviceId())
                    .withUnit(meter.unit())
                    .withBands(meter.bands())
                    .burst();
        } else {
            builder = DefaultMeterRequest.builder()
                    .fromApp(meter.appId())
                    .forDevice(meter.deviceId())
                    .withUnit(meter.unit())
                    .withBands(meter.bands());
        }

        switch (operation) {
            case "ADD":
                meterRequest = builder.add();
                break;
            case "REMOVE":
                meterRequest = builder.remove();
                break;
            default:
                log.warn("Invalid operation {}.", operation);
                return null;
        }

        return meterRequest;
    }

    public Long pushFlows(DeviceId deviceId, PortNumber inPort, PortNumber outPort,
                          String srcIP, String dstIP, MeterId meterId, String trafficType,
                          String vlanId, String oscarsVlanId, int flowInorOut) {
        log.info("vlan Id {}, oscarsId {}", vlanId, oscarsVlanId);
        TrafficSelector.Builder sbuilder;
        sbuilder = DefaultTrafficSelector.builder();

        TrafficTreatment treatment;
        if(trafficType.equals("1") && oscarsVlanId.equals("")) {
            log.info("If traffic treatment");
            treatment = DefaultTrafficTreatment.builder()
                    .meter(meterId)
                    .setQueue(3)
                    .setOutput(outPort)
                    .build();
            sbuilder.matchVlanId(VlanId.vlanId(vlanId))
                    .matchEthType((short) 0x800)
                    .matchInPort(inPort);
        } else if(trafficType.equals("1") && !oscarsVlanId.equals("")) {
            String temp;
            if(flowInorOut == 1) {
                temp = oscarsVlanId;
                sbuilder.matchVlanId(VlanId.vlanId(vlanId))
                        .matchEthType((short) 0x800)
                        .matchInPort(inPort);
            } else {
                temp = vlanId;
                sbuilder.matchVlanId(VlanId.vlanId(oscarsVlanId))
                        .matchEthType((short) 0x800)
                        .matchInPort(inPort);
            }
            treatment = DefaultTrafficTreatment.builder()
                    .popVlan()
                    .pushVlan()
                    .setVlanId((VlanId.vlanId(temp)))
                    .meter(meterId)
                    .setQueue(3)
                    .setOutput(outPort)
                    .build();
        } else if(trafficType.equals("0") && !oscarsVlanId.equals("")) {
            log.info("In else of treatment");
            String temp;
            if(flowInorOut == 1) {
                temp = oscarsVlanId;
                sbuilder.matchVlanId(VlanId.vlanId(vlanId))
                        .matchEthType((short) 0x800)
                        .matchInPort(inPort);
            } else {
                temp = vlanId;
                sbuilder.matchVlanId(VlanId.vlanId(oscarsVlanId))
                        .matchEthType((short) 0x800)
                        .matchInPort(inPort);
            }
            treatment = DefaultTrafficTreatment.builder()
                    .popVlan()
                    .pushVlan()
                    .setVlanId((VlanId.vlanId(temp)))
                    .setQueue(0)
                    .setOutput(outPort)
                    .build();
        } else {
            treatment = DefaultTrafficTreatment.builder()
                    .setQueue(0)
                    .setOutput(outPort)
                    .build();
            sbuilder.matchVlanId(VlanId.vlanId(vlanId))
                    .matchEthType((short) 0x800)
                    .matchInPort(inPort);
        }

        FlowRuleOperations.Builder rules = FlowRuleOperations.builder();

        if(srcIP != null) {
            sbuilder.matchIPSrc(IpPrefix.valueOf((IpAddress.valueOf(srcIP)), 32));
        }

        if(dstIP != null) {
            sbuilder.matchIPDst(IpPrefix.valueOf((IpAddress.valueOf(dstIP)), 32));
        }

        FlowRule addRule = DefaultFlowRule.builder()
                .forDevice(deviceId)
                .withSelector(sbuilder.build())
                .withTreatment(treatment)
                .withPriority(PRIORITY)
                .makePermanent()
                .fromApp(appId)
                .build();
        rules.add(addRule);
        flowRuleService.apply(rules.build());

        log.info("Flow id {} @ device Id {}", addRule.id().toString(), deviceId);
        return addRule.id().value();
    }
}
