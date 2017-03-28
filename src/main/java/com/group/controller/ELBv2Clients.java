package com.group.controller;

/**
 * Created by wtang on 3/12/17.
 */
import java.io.File;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
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
import com.amazonaws.services.elasticloadbalancingv2.AmazonElasticLoadBalancingClient;
import com.amazonaws.services.elasticloadbalancingv2.model.CreateTargetGroupRequest;
import com.amazonaws.services.elasticloadbalancingv2.model.CreateTargetGroupResult;
import com.amazonaws.services.elasticloadbalancingv2.model.CreateLoadBalancerRequest;
import com.amazonaws.services.elasticloadbalancingv2.model.*;


import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import org.apache.commons.lang3.builder.ReflectionToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import org.joda.time.DateTime;

public class ELBv2Clients extends AWSClients{

    private AmazonElasticLoadBalancingClient AWSELBClient;
    private AmazonCloudWatchClient AWSCloudWatchClient;
    public ELBConfig elbConfig;
    public String elbDNSName;
    public String elbVpcId;
    public String elbArn;
    public LoadBalancerState elbState;
    public String elbTargetGroupArn;
    public String elbListenerArn;

    public ELBv2Clients(String configFilePath) {
        super();
        AWSELBClient = new AmazonElasticLoadBalancingClient(getCredentials());
        Region usaRegion = Region.getRegion(Regions.US_WEST_2);
        AWSELBClient.setRegion(usaRegion);
        AWSCloudWatchClient = new AmazonCloudWatchClient(getCredentials());
        AWSCloudWatchClient.setRegion(usaRegion);

        ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
        try {
            elbConfig = mapper.readValue(new File(configFilePath), ELBConfig.class);
            System.out.println(ReflectionToStringBuilder.toString(elbConfig, ToStringStyle.MULTI_LINE_STYLE));
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

        registerELBTargetGroup(ec2RunningInstances);

        createELBListener(elbConfig.getelbProtocol(), "forward", elbConfig.getelbPort());

        createELBRule(elbTargetGroupArn, elbListenerArn, "/db/*");

        System.out.println("CreateELBResult: " + response.toString());
        return response.getLoadBalancers().get(0).getState().toString();
    }

    /*
    * http://docs.aws.amazon.com/AWSJavaSDK/latest/javadoc/com/amazonaws/services/elasticloadbalancingv2/AmazonElasticLoadBalancing.html#createTargetGroup-com.amazonaws.services.elasticloadbalancingv2.model.CreateTargetGroupRequest-
     */

    public void createELBTargetGroup (String targetGroupName) {

        CreateTargetGroupRequest request = new CreateTargetGroupRequest()
                .withName(targetGroupName).withPort(elbConfig.getelbPort()).withProtocol(elbConfig.getelbProtocol())
                .withVpcId(elbVpcId);
        CreateTargetGroupResult response = AWSELBClient.createTargetGroup(request);
        elbTargetGroupArn = response.getTargetGroups().get(0).getTargetGroupArn();
    }

    /*
    * http://docs.aws.amazon.com/AWSJavaSDK/latest/javadoc/com/amazonaws/services/elasticloadbalancingv2/AmazonElasticLoadBalancing.html#registerTargets-com.amazonaws.services.elasticloadbalancingv2.model.RegisterTargetsRequest-
     */
    public void registerELBTargetGroup(List<String> ec2RunningInstances) {
        RegisterTargetsRequest request = new RegisterTargetsRequest()
                .withTargetGroupArn(elbTargetGroupArn);

        List<TargetDescription> targetDescriptionList = new ArrayList<TargetDescription>();

        for (String ec2 : ec2RunningInstances) {
            System.out.println(ec2);
            targetDescriptionList.add(new TargetDescription().withId(ec2).withPort(elbConfig.getInstancePort()));
        }
        request.withTargets(targetDescriptionList);

        RegisterTargetsResult response = AWSELBClient.registerTargets(request);
        System.out.print("Regeister Targets result:" + response);
    }

    /*
     * http://docs.aws.amazon.com/cli/latest/reference/elb/create-load-balancer-listeners.html
     */
    public void createELBListener(String elbProtocol, String type, Integer elbPort) {
        CreateListenerRequest request = new CreateListenerRequest().withDefaultActions(
                                            new Action().withTargetGroupArn(elbTargetGroupArn)
                                                        .withType(type))
                                                        .withLoadBalancerArn(elbArn)
                                                        .withPort(elbPort).withProtocol(elbProtocol);
        CreateListenerResult response = AWSELBClient.createListener(request);
        elbListenerArn = response.getListeners().get(0).getListenerArn();

        System.out.print("CreateLoadBalancerListenersResult: " + response);
    }

    /*
     * http://docs.aws.amazon.com/cli/latest/reference/elbv2/create-rule.html
     * example path : /img/*  /db/*
     * http://docs.aws.amazon.com/elasticloadbalancing/latest/application/load-balancer-listeners.html#listener-rules
     */
    public void createELBRule(String targetGroupArn, String listenerArn, String path) {
        CreateRuleRequest request = new CreateRuleRequest()
                .withActions(
                        new Action()
                                .withTargetGroupArn(targetGroupArn)
                                .withType("forward"))
                .withConditions(
                        new RuleCondition().withField("path-pattern")
                                .withValues(path))
                .withListenerArn(listenerArn)
                .withPriority(10);
        CreateRuleResult response = AWSELBClient.createRule(request);
        System.out.print("CreateRuleResult: " + response);
    }


    /*
     * http://docs.aws.amazon.com/cli/latest/reference/elb/delete-load-balancer.html
     */
    public void deleteELB(String ELBArn) {
        DeleteLoadBalancerRequest request = new DeleteLoadBalancerRequest()
                .withLoadBalancerArn(ELBArn);

        DeleteLoadBalancerResult response = AWSELBClient.deleteLoadBalancer(request);

        System.out.println("DeleteLoadBalancerResult: " + response);
    }


    public void listELBMetrics(String ELBArn, String ELBTargetGroup, List<String> metricList) {

        String pattern = ".*(loadbalancer/)(.*)";
        Pattern reg = Pattern.compile(pattern);
        String elbFilter = "";

        Matcher m = reg.matcher(ELBArn);
        if (m.find()) {
            elbFilter = m.group(2);
        } else {
            System.out.println("Incorrect ELB Arn");
        }

        pattern = ".*(targetgroup/.*)";
        reg = Pattern.compile(pattern);
        String tgFilter = "";

        m = reg.matcher(ELBTargetGroup);
        if (m.find()) {
            tgFilter = m.group(1);
        } else {
            System.out.println("Incorrect Target group Arn");
        }

        DimensionFilter dimensionFilterTg = new DimensionFilter().withName("TargetGroup").withValue(tgFilter);
        DimensionFilter dimensionFilterLb = new DimensionFilter().withName("LoadBalancer").withValue(elbFilter);

        List<DimensionFilter> dimensionFilterList = new ArrayList<>();
        dimensionFilterList.add(dimensionFilterTg);
        dimensionFilterList.add(dimensionFilterLb);

        ListMetricsRequest listMetricsRequest = new ListMetricsRequest()
                .withNamespace("AWS/ApplicationELB")
                .withDimensions(dimensionFilterList);

        for (String metricitem : metricList) {
            listMetricsRequest.setMetricName(metricitem);

            ListMetricsResult response;

            do {
                response = AWSCloudWatchClient.listMetrics(listMetricsRequest);

                if (response != null && response.getMetrics().size() > 0) {
                    for (Metric metric : response.getMetrics()) {
                        System.out.println(metric.getMetricName() + "(" + metric.getNamespace() + ")");

                        for (Dimension dimension : metric.getDimensions()) {
                            System.out.println(" " + dimension.getName() + ": " + dimension.getValue());
                        }
                    }
                } else {
                    System.out.print("No ELB metrics found.");
                }

                listMetricsRequest.setNextToken(response.getNextToken());
            } while (response.getNextToken() != null);
        }
    }

    public void getELBMetricStats(String ELBArn, String ELBTargetGroup, String metricName) {

        String pattern = ".*(loadbalancer/)(.*)";
        Pattern reg = Pattern.compile(pattern);
        String elbFilter = "";

        Matcher m = reg.matcher(ELBArn);
        if (m.find()) {
            elbFilter = m.group(2);
        } else {
            System.out.println("Incorrect ELB Arn");
        }

        pattern = ".*(targetgroup/.*)";
        reg = Pattern.compile(pattern);
        String tgFilter = "";

        m = reg.matcher(ELBTargetGroup);
        if (m.find()) {
            tgFilter = m.group(1);
        } else {
            System.out.println("Incorrect Target group Arn");
        }

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
        Date startTime =  new DateTime(endTime).minusMinutes(5).toDate();

        request.setMetricName(metricName);
        request.setStartTime(startTime);
        request.setEndTime(endTime);
        request.setPeriod(60);
        request.setNamespace("AWS/ApplicationELB");
        List<String> statsList = new ArrayList<String>();
        statsList.add("Sum");
        statsList.add("Average");
        statsList.add("Maximum");
        statsList.add("Minimum");

        request.setStatistics(statsList);

        GetMetricStatisticsResult result = AWSCloudWatchClient.getMetricStatistics(request);

        if (result.getDatapoints().size() > 0) {
            for (Datapoint point : result.getDatapoints()) {
                System.out.println(metricName + " at timestamp : " + point.getTimestamp() + " Sum: " + point.getSum());
                System.out.println(metricName + " at timestamp : " + point.getTimestamp() + " Average: " + point.getAverage());
                System.out.println(metricName + " at timestamp : " + point.getTimestamp() + " Maximum: " + point.getAverage());
                System.out.println(metricName + " at timestamp : " + point.getTimestamp() + " Minimum: " + point.getAverage());
            }
        }

    }


}
