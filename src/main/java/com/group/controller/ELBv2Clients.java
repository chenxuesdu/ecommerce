package com.group.controller;

/**
 * Created by wtang on 3/12/17.
 */
import java.io.File;
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
import com.amazonaws.services.elasticloadbalancing.model.*;
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
import org.joda.time.DateTime;

public class ELBv2Clients extends AWSClients{

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

        List<String> ec2InstanceList1 = new ArrayList<>();
        ec2InstanceList1.add(ec2RunningInstances.get(0));
        List<String> ec2InstanceList2 = new ArrayList<>();
        ec2InstanceList2.add(ec2RunningInstances.get(1));

        registerELBTargetGroup(elbTargetGroupArn.get(0), ec2InstanceList1);
        registerELBTargetGroup(elbTargetGroupArn.get(1), ec2InstanceList2);

        createELBListener(elbConfig.getelbProtocol(), "forward", elbConfig.getelbPort(), elbTargetGroupArn.get(0));
        createELBListener(elbConfig.getelbProtocol(), "forward", elbConfig.getelbPort()+8000, elbTargetGroupArn.get(1));


        createELBRule(elbTargetGroupArn.get(0), elbListenerArn.get(0), "/db/*", 10);
        createELBRule(elbTargetGroupArn.get(1), elbListenerArn.get(1), "/web/*", 11);


        System.out.println("CreateELBResult: " + response.toString());
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
    public void registerELBTargetGroup(String targetGroupArn, List<String> ec2RunningInstances) {
        RegisterTargetsRequest request = new RegisterTargetsRequest()
                .withTargetGroupArn(targetGroupArn);

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
    public void deleteELB(String ELBArn) {
        DeleteLoadBalancerRequest request = new DeleteLoadBalancerRequest()
                .withLoadBalancerArn(ELBArn);

        DeleteLoadBalancerResult response = AWSELBClient.deleteLoadBalancer(request);

        System.out.println("DeleteLoadBalancerResult: " + response);
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

    /*
    *http://docs.aws.amazon.com/AWSJavaSDK/latest/javadoc/com/amazonaws/services/cloudwatch/AmazonCloudWatchClient.html#getMetricStatistics-com.amazonaws.services.cloudwatch.model.GetMetricStatisticsRequest-
     */
    public Map<String, String> getELBMetricStats(String ELBArn, String ELBTargetGroup, String metricName) {
        Map<String, String> res = new HashMap<> ();
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
                res.put(tgFilter + " " + metricName+" sum", point.getSum().toString());
                res.put(tgFilter + " " + metricName+" Average", point.getAverage().toString());
                res.put(tgFilter + " " + metricName+" Maximum", point.getMaximum().toString());
                res.put(tgFilter + " " + metricName+" Minimum", point.getMinimum().toString());

                System.out.println(tgFilter + " " + metricName + " at timestamp : " + point.getTimestamp() + " Sum: " + point.getSum());
                System.out.println(tgFilter + " " + metricName + " at timestamp : " + point.getTimestamp() + " Average: " + point.getAverage());
                System.out.println(tgFilter + " " + metricName + " at timestamp : " + point.getTimestamp() + " Maximum: " + point.getAverage());
                System.out.println(tgFilter + " " + metricName + " at timestamp : " + point.getTimestamp() + " Minimum: " + point.getAverage());
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
        System.out.println("ELB:" + elbArn + " state:" + state);
        return state;
    }

    public String getELBDNSName(String elbArn) {
        DescribeLoadBalancersRequest request = new DescribeLoadBalancersRequest()
                .withLoadBalancerArns(elbArn);
        DescribeLoadBalancersResult response = AWSELBClient.describeLoadBalancers(request);

        String elbDNSName = response.getLoadBalancers().get(0).getDNSName();
        System.out.println("ELB:" + elbArn + " DNS name:" + elbDNSName);
        return elbDNSName;
    }

    private String trimELBArn (String elbArn) {
        String pattern = ".*(loadbalancer/)(.*)";
        Pattern reg = Pattern.compile(pattern);

        Matcher m = reg.matcher(elbArn);
        if (m.find()) {
            return m.group(2);
        } else {
            System.out.println("Incorrect ELB Arn");
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
            System.out.println("Incorrect Target group Arn");
        }
        return "";
    }
}
