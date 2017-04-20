package com.group.controller;

/**
 * Created by wtang on 3/12/17.
 */
import java.io.File;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.Date;
import java.sql.Timestamp;
import java.time.LocalTime;

import com.amazonaws.regions.Region;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.cloudwatch.AmazonCloudWatchClient;
import com.amazonaws.services.cloudwatch.model.*;
//import com.amazonaws.services.elasticloadbalancing.model.*;
import com.amazonaws.services.elasticloadbalancingv2.AmazonElasticLoadBalancingClient;
import com.amazonaws.services.elasticloadbalancingv2.model.CreateLoadBalancerResult;
import com.amazonaws.services.elasticloadbalancingv2.model.CreateTargetGroupRequest;
import com.amazonaws.services.elasticloadbalancingv2.model.CreateTargetGroupResult;
import com.amazonaws.services.elasticloadbalancingv2.model.CreateLoadBalancerRequest;
import com.amazonaws.services.elasticloadbalancingv2.model.*;


import com.amazonaws.services.elasticloadbalancingv2.model.DeleteLoadBalancerRequest;
import com.amazonaws.services.elasticloadbalancingv2.model.DeleteLoadBalancerResult;
import com.amazonaws.services.elasticloadbalancingv2.model.DescribeLoadBalancersRequest;
import com.amazonaws.services.elasticloadbalancingv2.model.DescribeLoadBalancersResult;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import org.apache.commons.lang3.builder.ReflectionToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import org.apache.log4j.Logger;
import org.joda.time.DateTime;

public class ELBv2Clients extends AWSClients{
    private static Logger log = Logger.getLogger(ELBv2Clients.class);

    private AmazonElasticLoadBalancingClient AWSELBClient;
    private AmazonCloudWatchClient AWSCloudWatchClient;
    public ELBConfig elbConfig;
    public String elbDNSName;
    public String elbVpcId;
    public String elbArn;
    public LoadBalancerState elbState;
    public List<String> elbTargetGroupArn = new ArrayList<>();
    public List<String>  elbListenerArn = new ArrayList<>();

    public ELBv2Clients(String configFilePath) {
        super();
        AWSELBClient = new AmazonElasticLoadBalancingClient(getCredentials());
        Region usaRegion = Region.getRegion(Regions.US_WEST_2);
        AWSELBClient.setRegion(usaRegion);
        AWSCloudWatchClient = new AmazonCloudWatchClient(getCredentials());
        AWSCloudWatchClient.setRegion(usaRegion);
        elbArn = "";
        ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
        try {
            elbConfig = mapper.readValue(new File(configFilePath), ELBConfig.class);
            log.info(ReflectionToStringBuilder.toString(elbConfig, ToStringStyle.MULTI_LINE_STYLE));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /*
     * http://docs.aws.amazon.com/cli/latest/reference/elb/create-load-balancer.html
     */
    public String createELB(String elbName, List<String> securityGroups, List<String> ec2InstanceSubnetIds, List<String> ec2RunningInstances) {
        CreateLoadBalancerRequest request = new CreateLoadBalancerRequest()
                                                .withName(elbName)
                                                .withSubnets(ec2InstanceSubnetIds)
                                                .withSecurityGroups(securityGroups);

        CreateLoadBalancerResult response = AWSELBClient.createLoadBalancer(request);
        elbDNSName = response.getLoadBalancers().get(0).getDNSName();
        elbVpcId = response.getLoadBalancers().get(0).getVpcId();
        elbArn = response.getLoadBalancers().get(0).getLoadBalancerArn();
        elbState = response.getLoadBalancers().get(0).getState();

        createELBTargetGroup(elbConfig.getTargetGroupName());

        int half = ec2RunningInstances.size()/2;

        List<String> ec2InstanceList1 = new ArrayList<>();
        for (int i = 0; i < half ; i++) {
            ec2InstanceList1.add(ec2RunningInstances.get(i));
        }

        List<String> ec2InstanceList2 = new ArrayList<>();
        for (int i = half; i < ec2RunningInstances.size(); i++) {
            ec2InstanceList2.add(ec2RunningInstances.get(i));
        }

        registerELBTargets(elbTargetGroupArn.get(0), ec2InstanceList1);
        registerELBTargets(elbTargetGroupArn.get(1), ec2InstanceList2);

        createELBListener(elbConfig.getelbProtocol(), "forward", elbConfig.getelbPort(), elbTargetGroupArn.get(0));
        //createELBListener(elbConfig.getelbProtocol(), "forward", elbConfig.getelbPort()+8000, elbTargetGroupArn.get(1));


        createELBRule(elbTargetGroupArn.get(0), elbListenerArn.get(0), "/web/*", 10);
        //createELBRule(elbTargetGroupArn.get(1), elbListenerArn.get(1), "/web/*", 11);
        createELBRule(elbTargetGroupArn.get(1), elbListenerArn.get(0), "/web/*", 11);



        log.info("CreateELBResult: " + response.toString());
        return response.getLoadBalancers().get(0).getState().getCode();
    }

    /*
    * http://docs.aws.amazon.com/AWSJavaSDK/latest/javadoc/com/amazonaws/services/elasticloadbalancingv2/AmazonElasticLoadBalancing.html#createTargetGroup-com.amazonaws.services.elasticloadbalancingv2.model.CreateTargetGroupRequest-
     */

    public void createELBTargetGroup (List<String> targetGroupName) {

        for (String tn:targetGroupName) {
            CreateTargetGroupRequest request = new CreateTargetGroupRequest()
                    .withName(tn).withPort(elbConfig.getelbPort()).withProtocol(elbConfig.getelbProtocol())
                    .withVpcId(elbVpcId);
            CreateTargetGroupResult response = AWSELBClient.createTargetGroup(request);
            elbTargetGroupArn.add(response.getTargetGroups().get(0).getTargetGroupArn());
        }
    }

    /*
    * http://docs.aws.amazon.com/AWSJavaSDK/latest/javadoc/com/amazonaws/services/elasticloadbalancingv2/AmazonElasticLoadBalancing.html#registerTargets-com.amazonaws.services.elasticloadbalancingv2.model.RegisterTargetsRequest-
     */
    public void registerELBTargets(String targetGroupArn, List<String> ec2RunningInstances) {
        RegisterTargetsRequest request = new RegisterTargetsRequest()
                .withTargetGroupArn(targetGroupArn);

        List<TargetDescription> targetDescriptionList = new ArrayList<TargetDescription>();

        for (String ec2 : ec2RunningInstances) {
            log.info(ec2);
            targetDescriptionList.add(new TargetDescription().withId(ec2).withPort(elbConfig.getInstancePort()));
        }
        request.withTargets(targetDescriptionList);

        RegisterTargetsResult response = AWSELBClient.registerTargets(request);
        System.out.print("Register Targets result:" + response);
    }

    /*
     * http://docs.aws.amazon.com/cli/latest/reference/elb/create-load-balancer-listeners.html
     */
    public void createELBListener(String elbProtocol, String type, Integer elbPort, String targetGroupArn) {
        CreateListenerRequest request = new CreateListenerRequest().withDefaultActions(
                                            new Action().withTargetGroupArn(targetGroupArn)
                                                        .withType(type))
                                                        .withLoadBalancerArn(elbArn)
                                                        .withPort(elbPort).withProtocol(elbProtocol);
        CreateListenerResult response = AWSELBClient.createListener(request);
        elbListenerArn.add( response.getListeners().get(0).getListenerArn());

        System.out.print("CreateLoadBalancerListenersResult: " + response);
    }

    /*
     * http://docs.aws.amazon.com/cli/latest/reference/elbv2/create-rule.html
     * example path : /img/*  /db/*
     * http://docs.aws.amazon.com/elasticloadbalancing/latest/application/load-balancer-listeners.html#listener-rules
     */
    public void createELBRule(String targetGroupArn, String listenerArn, String path, int priority) {
        CreateRuleRequest request = new CreateRuleRequest()
                .withActions(
                        new Action()
                                .withTargetGroupArn(targetGroupArn)
                                .withType("forward"))
                .withConditions(
                        new RuleCondition().withField("path-pattern")
                                .withValues(path))
                .withListenerArn(listenerArn)
                .withPriority(priority);
        CreateRuleResult response = AWSELBClient.createRule(request);
        System.out.print("CreateRuleResult: " + response);
    }


    /*
     * http://docs.aws.amazon.com/cli/latest/reference/elb/delete-load-balancer.html
     */
    public String deleteELB(String ELBArn) {
        DeleteLoadBalancerRequest request = new DeleteLoadBalancerRequest()
                .withLoadBalancerArn(ELBArn);

        DeleteLoadBalancerResult response = AWSELBClient.deleteLoadBalancer(request);
        String ret = "DeleteLoadBalancerResult: " + response;
        log.info(ret);
        return ret;
    }

    public List<String> describeTargetGroup(String ELBArn) {
        DescribeTargetGroupsRequest request = new DescribeTargetGroupsRequest()
                .withLoadBalancerArn(ELBArn);

        List<String> tgList = new ArrayList<>();
        DescribeTargetGroupsResult response = AWSELBClient.describeTargetGroups(request);
        log.info("DeleteLoadBalancerResult: " + response.toString());

        for (TargetGroup tg : response.getTargetGroups())
            tgList.add(tg.getTargetGroupArn());
        return tgList;
    }

    public String deleteTargetGroup(String TGArn) {
        DeleteTargetGroupRequest request = new DeleteTargetGroupRequest()
                .withTargetGroupArn(TGArn);

        DeleteTargetGroupResult response = AWSELBClient.deleteTargetGroup(request);
        String ret = "DeleteLoadBalancerResult: " + response.toString();
        log.info(ret);
        return ret;
    }
    /*
    * http://docs.aws.amazon.com/AWSJavaSDK/latest/javadoc/com/amazonaws/services/cloudwatch/AmazonCloudWatchClient.html#listMetrics-com.amazonaws.services.cloudwatch.model.ListMetricsRequest-
     */

    public void listELBMetrics(String ELBArn, String ELBTargetGroup, List<String> metricList) {
        String elbFilter = trimELBArn(ELBArn);
        String tgFilter = trimTgArn(ELBTargetGroup);

        DimensionFilter dimensionFilterTg = new DimensionFilter().withName("TargetGroup").withValue(tgFilter);
        DimensionFilter dimensionFilterLb = new DimensionFilter().withName("LoadBalancer").withValue(elbFilter);

        List<DimensionFilter> dimensionFilterList = new ArrayList<>();
        dimensionFilterList.add(dimensionFilterTg);
        dimensionFilterList.add(dimensionFilterLb);

        ListMetricsRequest listMetricsRequest = new ListMetricsRequest()
                .withNamespace("AWS/ApplicationELB")
                .withDimensions(dimensionFilterList);

        for (String m : metricList) {
            listMetricsRequest.setMetricName(m);
            ListMetricsResult response;

            do {
                response = AWSCloudWatchClient.listMetrics(listMetricsRequest);
                if (response != null && response.getMetrics().size() > 0) {
                    for (Metric metric : response.getMetrics()) {
                        log.info(metric.getMetricName() + "(" + metric.getNamespace() + ")");

                        for (Dimension dimension : metric.getDimensions()) {
                            log.info(" " + dimension.getName() + ": " + dimension.getValue());
                        }
                    }
                } else {
                    System.out.print("No ELB metrics found.");
                }

                listMetricsRequest.setNextToken(response.getNextToken());
            } while (response.getNextToken() != null);
        }
    }

    /*
    *http://docs.aws.amazon.com/AWSJavaSDK/latest/javadoc/com/amazonaws/services/cloudwatch/AmazonCloudWatchClient.html#getMetricStatistics-com.amazonaws.services.cloudwatch.model.GetMetricStatisticsRequest-
     */
    public Map<String, String> getELBMetricStats(String ELBArn, String ELBTargetGroup, String metricName) {
        Map<String, String> res = new LinkedHashMap<> ();
        String elbFilter = trimELBArn(ELBArn);
        String tgFilter = trimTgArn(ELBTargetGroup);
        List<Dimension> dimensionList = new ArrayList<>();

        GetMetricStatisticsRequest request = new GetMetricStatisticsRequest();
        Dimension dimension = new Dimension();
        dimension.setName("LoadBalancer");
        dimension.setValue(elbFilter);
        dimensionList.add(dimension);

        dimension = new Dimension();
        dimension.setName("TargetGroup");
        dimension.setValue(tgFilter);
        dimensionList.add(dimension);

        request.setDimensions(dimensionList);

        Date endTime =  new Date();
        Date startTime =  new DateTime(endTime).minusMinutes(1).toDate();
        //To get only 1 minute stats for each metric.

        request.setMetricName(metricName);
        request.setStartTime(startTime);
        request.setEndTime(endTime);
        request.setPeriod(60);
        request.setNamespace("AWS/ApplicationELB");

        List<String> statsList = new ArrayList<>();
        statsList.add("Sum");
        statsList.add("Average");
        statsList.add("Maximum");
        statsList.add("Minimum");

        request.setStatistics(statsList);

        GetMetricStatisticsResult result = AWSCloudWatchClient.getMetricStatistics(request);

        if (result.getDatapoints().size() > 0) {
            for (Datapoint point : result.getDatapoints()) {
                res.put(metricName + " sum", point.getSum().toString());
                res.put(metricName + " Average", point.getAverage().toString());
                res.put(metricName + " Maximum", point.getMaximum().toString());
                res.put(metricName + " Minimum", point.getMinimum().toString());

                log.info(tgFilter + " " + metricName + " at timestamp : " + point.getTimestamp() + " Sum: " + point.getSum());
                log.info(tgFilter + " " + metricName + " at timestamp : " + point.getTimestamp() + " Average: " + point.getAverage());
                log.info(tgFilter + " " + metricName + " at timestamp : " + point.getTimestamp() + " Maximum: " + point.getAverage());
                log.info(tgFilter + " " + metricName + " at timestamp : " + point.getTimestamp() + " Minimum: " + point.getAverage());
            }
        }
        return res;
    }

    /*
    * http://docs.aws.amazon.com/AWSJavaSDK/latest/javadoc/com/amazonaws/services/elasticloadbalancingv2/AmazonElasticLoadBalancing.html#describeLoadBalancers-com.amazonaws.services.elasticloadbalancingv2.model.DescribeLoadBalancersRequest-
    */
    public String getELBState(String elbArn) {
        DescribeLoadBalancersRequest request = new DescribeLoadBalancersRequest()
                .withLoadBalancerArns(elbArn);
        DescribeLoadBalancersResult response = AWSELBClient.describeLoadBalancers(request);

        String state = response.getLoadBalancers().get(0).getState().getCode();
        log.info("ELB:" + elbArn + " state:" + state);
        return state;
    }

    public String getELBDNSName(String elbArn) {
        DescribeLoadBalancersRequest request = new DescribeLoadBalancersRequest()
                .withLoadBalancerArns(elbArn);
        DescribeLoadBalancersResult response = AWSELBClient.describeLoadBalancers(request);

        String elbDNSName = response.getLoadBalancers().get(0).getDNSName();
        log.info("ELB:" + elbArn + " DNS name:" + elbDNSName);
        return elbDNSName;
    }

    public Map<String, String> searchELB(String ELBArn) {
        Map<String, String> ret  = new LinkedHashMap<> ();
        DescribeLoadBalancersRequest request = new DescribeLoadBalancersRequest();
        List<String> ELBs = new ArrayList<>();
        ELBs.add(ELBArn);

        request.setLoadBalancerArns(ELBs);
        DescribeLoadBalancersResult response = AWSELBClient.describeLoadBalancers(request );

        List<LoadBalancer> ELBList = response.getLoadBalancers();
        printELBDescription(ELBList, ret);
        return ret;
    }

    public Map<String, String> listELB() {
        Map<String, String> ret  = new LinkedHashMap<>();

        DescribeLoadBalancersRequest request = new DescribeLoadBalancersRequest();
        DescribeLoadBalancersResult response = AWSELBClient.describeLoadBalancers(request);

        List<LoadBalancer> ELBList = response.getLoadBalancers();

        printELBDescription(ELBList, ret);
        return ret;
    }

    private void printELBDescription(List<LoadBalancer> ELBList, Map<String, String> ret) {
        for(LoadBalancer elb: ELBList) {
            log.info("LoadBalancerName: " + elb.getLoadBalancerName());
            ret.put("LoadBalancerName", elb.getLoadBalancerName());
            log.info("LoadBalancerARN: " + elb.getLoadBalancerArn());
            ret.put("LoadBalancerARN", elb.getLoadBalancerArn());
            log.info("LoadBalancerDNSName: " + elb.getDNSName());
            ret.put("LoadBalancerDNSName", elb.getDNSName());
            log.info("Scheme: " + elb.getScheme());
            ret.put("Scheme", elb.getScheme());
            log.info("Created Time: " + elb.getCreatedTime().toString());
            ret.put("CreatedTime", elb.getCreatedTime().toString());
            log.info("LoadBalancerState: " + elb.getState());
            ret.put("LoadBalancerState", elb.getScheme());
            log.info("LoadBalancerType: " +  elb.getType());
            ret.put("LoadBalancerType", elb.getType());
            log.info("Load BalancerVpcID: " + elb.getVpcId());
            ret.put("LoadBalancerVpcID", elb.getVpcId());

            log.info("AvailabilityZones: ");
            int i = 0;
            for(AvailabilityZone az: elb.getAvailabilityZones()) {
                log.info("\tAvailabilityZone: " + az.getZoneName());
                ret.put("AvailabilityZone" + i , az.getZoneName());
                i ++;
            }

            i = 0;
            log.info("SecurityGroups: ");
            for(String securityGroup: elb.getSecurityGroups()) {
                log.info("\tName: " + securityGroup);
                ret.put("SecurityGroup" + i , securityGroup);
                i ++;
            }
        }
    }

    private String trimELBArn (String elbArn) {
        String pattern = ".*(loadbalancer/)(.*)";
        Pattern reg = Pattern.compile(pattern);

        Matcher m = reg.matcher(elbArn);
        if (m.find()) {
            return m.group(2);
        } else {
            log.error("Incorrect ELB Arn");
        }
        return "";
    }

    private String trimTgArn (String elbTargetGroupArn) {
        String pattern = ".*(targetgroup/.*)";
        Pattern reg = Pattern.compile(pattern);

        Matcher m = reg.matcher(elbTargetGroupArn);
        if (m.find()) {
            return m.group(1);
        } else {
            log.error("Incorrect Target group Arn");
        }
        return "";
    }
}
