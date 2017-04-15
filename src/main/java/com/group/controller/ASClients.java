package com.group.controller;

import java.io.File;
import java.util.*;
import java.lang.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.amazonaws.regions.Region;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.autoscaling.model.InstanceMonitoring;
import com.amazonaws.services.cloudwatch.AmazonCloudWatchClient;
import com.amazonaws.services.cloudwatch.model.*;
import com.amazonaws.services.autoscaling.*;
import com.amazonaws.services.autoscaling.model.*;

import org.apache.commons.lang3.builder.ReflectionToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import org.joda.time.DateTime;
import org.apache.log4j.Logger;

/**
 * Created by ahbbc on 2017/3/16.
 */

public class ASClients extends AWSClients{

    private static Logger log = Logger.getLogger(ASClients.class);
    private AmazonAutoScalingClient asClient;
    private AmazonCloudWatchClient cloudWatchClient;
    public ASConfig asConfig;

    public ASClients(String configFilePath) {
        super();
        // http://docs.aws.amazon.com/AWSSdkDocsJava/latest/DeveloperGuide/init-ec2-client.html
        asClient = new AmazonAutoScalingClient(getCredentials());
        Region usaRegion = Region.getRegion(Regions.US_WEST_2);
        asClient.setRegion(usaRegion);

        cloudWatchClient = new AmazonCloudWatchClient(getCredentials());
        cloudWatchClient.setRegion(usaRegion);

        ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
        try {
            asConfig = mapper.readValue(new File(configFilePath), ASConfig.class);
            log.info(ReflectionToStringBuilder.toString(asConfig,ToStringStyle.MULTI_LINE_STYLE));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /*
     * Create autoscaling launch configuration
     */
    public void createConfiguration(String configName, String imageID, String instantType, String securityGroup){
        //asClient.setRegion(usaRegion);
        CreateLaunchConfigurationRequest lcRequest = new CreateLaunchConfigurationRequest();
        lcRequest.setLaunchConfigurationName(configName);
        lcRequest.setImageId(imageID);
        lcRequest.setInstanceType(instantType);

        /**
         * EC2 security groups use the friendly name
         * VPC security groups use the identifier
         */
        List securityGroups = new ArrayList();
        securityGroups.add(securityGroup);
        lcRequest.setSecurityGroups(securityGroups);

        InstanceMonitoring monitoring = new InstanceMonitoring();
        monitoring.setEnabled(Boolean.FALSE);//set basic monitoring with FALSE
        lcRequest.setInstanceMonitoring(monitoring);

        asClient.createLaunchConfiguration(lcRequest);
    }

    /*
     * Create autoscaling group
     *
     * Create with Classic ELB or Application ELB
     */
    public void createASGroup(String asgName, String configName, String avZone, String elbName, List<String> tgArnList){
        //asClient.setRegion(usaRegion);
        CreateAutoScalingGroupRequest asgRequest = new CreateAutoScalingGroupRequest();
        asgRequest.setAutoScalingGroupName(asgName);
        asgRequest.setLaunchConfigurationName(configName); // as above

        List avZones = new ArrayList();
        avZones.add(avZone); // or whatever you need
        asgRequest.setAvailabilityZones(avZones);

        asgRequest.setMinSize(0);  // disabling it for the moment
        asgRequest.setMaxSize(0); //  disabling it for the moment

        List elbs = new ArrayList();
        elbs.add(elbName);

        //List arn = new ArrayList();
        //arn.add("arn:aws:elasticloadbalancing:us-west-2:899396450289:targetgroup/295JavaTargetGroupA/5d2090c7e284c3be");//attach to application elb
        //asgRequest.setLoadBalancerNames(elbs); //attach to classic elb
        asgRequest.setTargetGroupARNs(tgArnList);
        asgRequest.setHealthCheckType("ELB");
        asgRequest.setHealthCheckGracePeriod(300);
        asgRequest.setDefaultCooldown(600);

        asClient.createAutoScalingGroup(asgRequest);
    }

    /*
     * Attach target group
     */
    public void attachTargetGroup(String asgName, String targetGroupARN){
        AttachLoadBalancerTargetGroupsRequest request = new AttachLoadBalancerTargetGroupsRequest()
                .withAutoScalingGroupName(asgName)
                .withTargetGroupARNs(targetGroupARN);
        AttachLoadBalancerTargetGroupsResult response = asClient
                .attachLoadBalancerTargetGroups(request);
        log.info(response.toString());
    }

    /*
     * Set autoscaling policy
     * The adjustmentType. Valid values are ChangeInCapacity, ExactCapacity, and PercentChangeInCapacity.
     */
    public String setPolicy(String asgName, String policyName, int capacity, String adjustmentType){
        //asClient.setRegion(usaRegion);
        PutScalingPolicyRequest request = new PutScalingPolicyRequest();

        request.setAutoScalingGroupName(asgName);
        request.setPolicyName(policyName); // This scales up so I've put up at the end.
        request.setScalingAdjustment(capacity); // scale up by specific capacity
        request.setAdjustmentType(adjustmentType);

        PutScalingPolicyResult result = asClient.putScalingPolicy(request);
        String arn = result.getPolicyARN(); // You need the policy ARN in the next step so make a note of it.
        return arn;
    }

    /*
     * Set autoscaling basic alarm using CloudWatch and CPUUtilization
     * comparisonOperator: up, down
     */
    public void setBasicAlarm(String newArn, String alarmName, String asgName, String comparisonOperator){
        String upArn = newArn; // from the policy request

        // Scale Up or Down
        PutMetricAlarmRequest upRequest = new PutMetricAlarmRequest();
        upRequest.setAlarmName(alarmName);
        upRequest.setMetricName("CPUUtilization");

        List dimensions = new ArrayList();
        Dimension dimension = new Dimension();
        dimension.setName("testAS");
        dimension.setValue(asgName);
        dimensions.add(dimension);
        upRequest.setDimensions(dimensions);

        upRequest.setNamespace("AWS/EC2");
        if(comparisonOperator.equals("up")){
            upRequest.setComparisonOperator(ComparisonOperator.GreaterThanThreshold);
        }else if(comparisonOperator.equals("down")){
            upRequest.setComparisonOperator(ComparisonOperator.LessThanThreshold);
        }else{
            log.info("Please input valid scaling strategy.");
        }
        upRequest.setStatistic(Statistic.Average);

        upRequest.setUnit(StandardUnit.Percent);
        upRequest.setThreshold(60d);
        upRequest.setPeriod(300);
        upRequest.setEvaluationPeriods(2);

        List actions = new ArrayList();
        actions.add(upArn); // This is the value returned by the ScalingPolicy request
        upRequest.setAlarmActions(actions);

        cloudWatchClient.putMetricAlarm(upRequest);
    }

    /*
     * Set autoscaling advanced alarm using CloudWatch and customized metrics
     * setMetricName: CPUUtilization, DiskReadOps, DiskWriteOps, DiskReadBytes, DiskWriteBytes, NetworkIn, NetworkOut, NetworkPacketsIn, NetworkPacketsOut
     * comparisonOperator: up, down
     *
     * to be decided...
     */
    public void setAdvAlarm(String newArn, String alarmName, String setMetricName, String asgName, String comparisonOperator){
        String upArn = newArn; // from the policy request

        // Scale Up or Down with customized metrics
        PutMetricAlarmRequest upRequest = new PutMetricAlarmRequest();
        upRequest.setAlarmName(alarmName);
        upRequest.setMetricName(setMetricName);

        List dimensions = new ArrayList();
        Dimension dimension = new Dimension();
        dimension.setName("testAS");
        dimension.setValue(asgName);
        dimensions.add(dimension);
        upRequest.setDimensions(dimensions);

        upRequest.setNamespace("AWS/EC2");
        if(comparisonOperator.equals("up")){
            upRequest.setComparisonOperator(ComparisonOperator.GreaterThanThreshold);
        }else if(comparisonOperator.equals("down")){
            upRequest.setComparisonOperator(ComparisonOperator.LessThanThreshold);
        }else{
            log.info("Please input valid scaling strategy.");
        }
        upRequest.setStatistic(Statistic.Average);

        upRequest.setUnit(StandardUnit.Percent);
        upRequest.setThreshold(60d);
        upRequest.setPeriod(300);
        upRequest.setEvaluationPeriods(2);

        List actions = new ArrayList();
        actions.add(upArn); // This is the value returned by the ScalingPolicy request
        upRequest.setAlarmActions(actions);

        cloudWatchClient.putMetricAlarm(upRequest);
    }

    /*
     * List autoscaling groups
     */
    public Map<String, String> listAS() {
        Map<String, String> res  = new HashMap<String, String>();

        DescribeAutoScalingGroupsRequest request = new DescribeAutoScalingGroupsRequest();
        DescribeAutoScalingGroupsResult response = asClient.describeAutoScalingGroups(request);

        List<AutoScalingGroup> ASList = response.getAutoScalingGroups();
        for(AutoScalingGroup as : ASList){
            res.put(as.getAutoScalingGroupName(), as.getStatus());
        }
        log.info("************************************************************************************************");
        for(String key : res.keySet()){
            log.info(key);
        }
        return res;
    }

    /*
     * Delete autoscaling group
     */
    public String deleteAS(String asName) {
        DeleteAutoScalingGroupRequest request = new DeleteAutoScalingGroupRequest()
                .withAutoScalingGroupName(asName);
        DeleteAutoScalingGroupResult response = asClient
                .deleteAutoScalingGroup(request);
        String res = "DeleteASGroupResult: " + response;
        log.info(res);

        return res;
    }

}


