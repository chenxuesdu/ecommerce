package com.group.controller;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.lang.*;

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

/**
 * Created by ahbbc on 2017/3/16.
 */

public class ASClients extends AWSClients{

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
            System.out.println(ReflectionToStringBuilder.toString(asConfig,ToStringStyle.MULTI_LINE_STYLE));
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
        monitoring.setEnabled(Boolean.FALSE);
        lcRequest.setInstanceMonitoring(monitoring);

        asClient.createLaunchConfiguration(lcRequest);
    }

    /*
     * Create autoscaling group
     */
    public void createASGroup(String asgName, String configName, String avZone, String elbName){
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
        asgRequest.setLoadBalancerNames(elbs);

        asgRequest.setHealthCheckType("ELB");
        asgRequest.setHealthCheckGracePeriod(300);
        asgRequest.setDefaultCooldown(600);

        asClient.createAutoScalingGroup(asgRequest);
    }

    /*
     * Set autoscaling policy
     */
    public String setPolicy(String asgName, String policyName){
        //asClient.setRegion(usaRegion);
        PutScalingPolicyRequest request = new PutScalingPolicyRequest();

        request.setAutoScalingGroupName(asgName);
        request.setPolicyName(policyName); // This scales up so I've put up at the end.
        request.setScalingAdjustment(1); // scale up by one
        request.setAdjustmentType("ChangeInCapacity");

        PutScalingPolicyResult result = asClient.putScalingPolicy(request);
        String arn = result.getPolicyARN(); // You need the policy ARN in the next step so make a note of it.
        return arn;
    }

    /*
     * Set autoscaling alarm using CloudWatch
     */
    public void setAlarm(String newArn, String alarmName, String asgName){
        //cloudWatchClient = new AmazonCloudWatchClient(bawsc);
        //cloudWatchClient.setRegion(usaRegion);
        String upArn = newArn; // from the policy request

        // Scale Up
        PutMetricAlarmRequest upRequest = new PutMetricAlarmRequest();
        upRequest.setAlarmName(alarmName);
        upRequest.setMetricName("CPUUtilization");

        List dimensions = new ArrayList();
        Dimension dimension = new Dimension();
        dimension.setName("AutoScalingGroupName");
        dimension.setValue(asgName);
        dimensions.add(dimension);
        upRequest.setDimensions(dimensions);

        upRequest.setNamespace("AWS/EC2");
        upRequest.setComparisonOperator(ComparisonOperator.GreaterThanThreshold);
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

}


