package com.group.client;
/**
 * Created by wtang on 3/12/17.
 */

import java.io.File;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.builder.ReflectionToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import org.apache.log4j.Logger;
import org.joda.time.DateTime;

import com.amazonaws.regions.Region;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.cloudwatch.AmazonCloudWatchClient;
import com.amazonaws.services.cloudwatch.model.Datapoint;
import com.amazonaws.services.cloudwatch.model.Dimension;
import com.amazonaws.services.cloudwatch.model.GetMetricStatisticsRequest;
import com.amazonaws.services.cloudwatch.model.GetMetricStatisticsResult;
import com.amazonaws.services.ec2.AmazonEC2;
import com.amazonaws.services.ec2.AmazonEC2Client;
import com.amazonaws.services.ec2.model.CreateTagsRequest;
import com.amazonaws.services.ec2.model.DeleteTagsRequest;
import com.amazonaws.services.ec2.model.DeleteTagsResult;
import com.amazonaws.services.ec2.model.DescribeInstanceStatusRequest;
import com.amazonaws.services.ec2.model.DescribeInstanceStatusResult;
import com.amazonaws.services.ec2.model.DescribeInstancesRequest;
import com.amazonaws.services.ec2.model.DescribeInstancesResult;
import com.amazonaws.services.ec2.model.DescribeSecurityGroupsRequest;
import com.amazonaws.services.ec2.model.DescribeSecurityGroupsResult;
import com.amazonaws.services.ec2.model.DescribeSubnetsResult;
import com.amazonaws.services.ec2.model.DescribeTagsResult;
import com.amazonaws.services.ec2.model.Instance;
import com.amazonaws.services.ec2.model.InstanceStatus;
import com.amazonaws.services.ec2.model.InstanceType;
import com.amazonaws.services.ec2.model.LaunchSpecification;
import com.amazonaws.services.ec2.model.Reservation;
import com.amazonaws.services.ec2.model.RunInstancesRequest;
import com.amazonaws.services.ec2.model.RunInstancesResult;
import com.amazonaws.services.ec2.model.SecurityGroup;
import com.amazonaws.services.ec2.model.StartInstancesRequest;
import com.amazonaws.services.ec2.model.StartInstancesResult;
import com.amazonaws.services.ec2.model.StopInstancesRequest;
import com.amazonaws.services.ec2.model.StopInstancesResult;
import com.amazonaws.services.ec2.model.Tag;
import com.amazonaws.services.ec2.model.TagDescription;
import com.amazonaws.services.ec2.model.TerminateInstancesRequest;
import com.amazonaws.services.ec2.model.TerminateInstancesResult;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;


public class EC2Clients extends AWSClients{

    private static final Object PENDING_STATUS = "pending";
    private static Logger log = Logger.getLogger(ELBv2Clients.class);

    protected AmazonEC2 AWSEC2Client;
    public EC2Config ec2Config;
    public AmazonCloudWatchClient AWSCloudWatchClient;

    public EC2Clients(String configFilePath) {
        super();
        // http://docs.aws.amazon.com/AWSSdkDocsJava/latest/DeveloperGuide/init-ec2-client.html
        AWSEC2Client = new AmazonEC2Client(getCredentials());
        Region usaRegion = Region.getRegion(Regions.US_WEST_2);
        AWSEC2Client.setRegion(usaRegion);
        AWSCloudWatchClient = new AmazonCloudWatchClient(getCredentials());
        AWSCloudWatchClient.setRegion(usaRegion);

        ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
        try {
            ec2Config = mapper.readValue(new File(configFilePath), EC2Config.class);
            log.info(ReflectionToStringBuilder.toString(ec2Config,ToStringStyle.MULTI_LINE_STYLE));
        } catch (Exception e) {

            e.printStackTrace();
        }
    }

    public String createEC2Instance(String imageId, InstanceType instanceType, int minInstanceCount, int maxInstanceCount, String keyPairName, String securityGroupId, String subnetId) {

        /* get the subnetID of VPC, and assign to the EC2 instances. */

        RunInstancesRequest request = new RunInstancesRequest();

        request.setImageId(imageId);
        request.setInstanceType(instanceType);

        request.setKeyName(keyPairName);
        List<String> securityGroupsId = new ArrayList<String>();
        securityGroupsId.add(securityGroupId);
        //request.setSecurityGroups(securityGroups);
        request.setSecurityGroupIds(securityGroupsId);

        request.setMinCount(minInstanceCount);
        request.setMaxCount(maxInstanceCount);
        request.setSubnetId(subnetId);

        RunInstancesResult response = AWSEC2Client.runInstances(request);

        log.info("RunInstancesResult: " + response);

        return getInstanceId();
    }

    public String getInstanceId() {

        String instanceId=null;
        DescribeInstancesResult result = AWSEC2Client.describeInstances();
        for(Reservation reservation: result.getReservations()) {
            List<Instance> instances = reservation.getInstances();
            for (Instance instance : instances) {
                if (instance.getState().getName().equals(PENDING_STATUS)) {
                    instanceId = instance.getInstanceId();
                    break;
                }
            }
        }
        return instanceId;
    }

    public void startInstance(String instanceId) {

        List<String> instanceIds = new ArrayList<String>();
        instanceIds.add(instanceId);

        StartInstancesRequest request = new StartInstancesRequest();
        request.setInstanceIds(instanceIds);

        StartInstancesResult response = AWSEC2Client.startInstances(request);

        log.info("response: " + response);
    }

    public String stopInstance (String instanceId) {

        List<String> instanceIds = new ArrayList<String>();
        instanceIds.add(instanceId);

        StopInstancesRequest request = new StopInstancesRequest();
        request.setInstanceIds(instanceIds);

        StopInstancesResult response = AWSEC2Client.stopInstances(request);
        String ret = "StopInstancesId " + instanceId + ": " + response.toString();
        log.info(ret);
        return ret;
    }

    public String terminateInstance (String instanceId) {

        List<String> instanceIds = new ArrayList<String>();
        instanceIds.add(instanceId);

        TerminateInstancesRequest request = new TerminateInstancesRequest();
        request.setInstanceIds(instanceIds);

        TerminateInstancesResult response = AWSEC2Client.terminateInstances(request);
        String ret = "TerminateInstancesId " + instanceId + ": " + response.toString();
        log.info(ret);
        return ret;
    }

    public List<String> listInstances(String instanceState) {

        List<String> instanceIdList = new ArrayList<>();

        DescribeInstancesResult response = AWSEC2Client.describeInstances();

        if(response!=null && response.getReservations()!=null && !response.getReservations().isEmpty()) {
            for(Reservation reservation: response.getReservations()) {
                List<Instance> instances = reservation.getInstances();

                if(instances!=null && !instances.isEmpty()) {
                    for(Instance instance: instances) {
                        if (instance.getState().getName().equals(instanceState)){
                            instanceIdList.add(instance.getInstanceId());
                            log.info("InstanceId: " + instance.getInstanceId());
                            log.info("ImageId: " + instance.getImageId());
                            log.info("InstanceType: " + instance.getInstanceType());
                            log.info("KeyName: " + instance.getKeyName());
                            log.info("State: " + instance.getState());
                            log.info("PrivateDnsName: " + instance.getPrivateDnsName());
                            log.info("PrivateIpAddress: " + instance.getPrivateIpAddress());
                            log.info("PublicDnsName: " + instance.getPublicDnsName());
                            log.info("PublicIpAddress: " + instance.getPublicIpAddress());
                            log.info("Architecture: " + instance.getArchitecture());
                            log.info("SecurityGroups: " + instance.getSecurityGroups());
                            log.info("SubnetID: " + instance.getSubnetId());
                            log.info("VpcId: " + instance.getVpcId());


                            List<Tag> tags = instance.getTags();
                            if(tags!=null && !tags.isEmpty()) {
                                for (Tag tag : tags) {
                                    log.info("TAG: " + tag.getKey() + "-" + tag.getValue());
                                }
                            }
                        }
                        log.info("-------");
                    }
                } else {
                    log.info("No Instances Found!!!");
                }
            }
        } else {
            log.info("No Reservation List Found!!!");
        }

        return instanceIdList;
    }


    public void searchInstances(String instanceId) {

        List<String> instanceIds = new ArrayList<String>();
        instanceIds.add(instanceId);

        DescribeInstancesRequest request = new DescribeInstancesRequest();
        request.setInstanceIds(instanceIds);
        DescribeInstancesResult response = AWSEC2Client.describeInstances(request);

        if(response!=null && response.getReservations()!=null && !response.getReservations().isEmpty()) {
            for(Reservation reservation: response.getReservations()) {
                List<Instance> instances = reservation.getInstances();

                if(instances!=null && !instances.isEmpty()) {
                    for(Instance instance: instances) {
                        log.info("InstanceId: " + instance.getInstanceId());
                        log.info("ImageId: " + instance.getImageId());
                        log.info("InstanceType: " + instance.getInstanceType());
                        log.info("KeyName: " + instance.getKeyName());
                        log.info("State: " + instance.getState());
                        log.info("PrivateDnsName: " + instance.getPrivateDnsName());
                        log.info("PrivateIpAddress: " + instance.getPrivateIpAddress());
                        log.info("PublicDnsName: " + instance.getPublicDnsName());
                        log.info("PublicIpAddress: " + instance.getPublicIpAddress());
                        log.info("Architecture: " + instance.getArchitecture());
                        log.info("SecurityGroups: " + instance.getSecurityGroups());

                        List<Tag> tags = instance.getTags();
                        if(tags!=null && !tags.isEmpty()) {
                            for(Tag tag: tags) {
                                log.info("TAG: " + tag.getKey() + "-" + tag.getValue());
                            }
                        }
                        log.info("-------");
                    }
                } else {
                    log.info("No Instances Found for Instance Id: " + instanceId + "!!!");
                }
            }
        } else {
            log.info("No Reservation List Found for Instance Id: " + instanceId + "!!!");
        }
    }

    /*
     * http://docs.aws.amazon.com/cli/latest/reference/ec2/describe-instance-status.html
     * CLI: aws ec2 describe-instance-status
     */
    public void getInstanceStatus() {

        DescribeInstanceStatusResult response = AWSEC2Client.describeInstanceStatus();

        if(response!=null && response.getInstanceStatuses()!=null && !response.getInstanceStatuses().isEmpty()) {
            List<InstanceStatus> instanceStatuses = response.getInstanceStatuses();
            for(InstanceStatus instanceStatus: instanceStatuses) {
                log.info("InstanceId: " + instanceStatus.getInstanceId());
                log.info("InstanceState" + instanceStatus.getInstanceState());
                log.info("InstanceStatus: " + instanceStatus.getInstanceStatus());
                log.info("SystemStatus: " + instanceStatus.getSystemStatus());
                log.info("-------");
            }
        } else {
            log.info("No Instance Statuses Found!!!");
        }
    }

    /*
     * http://docs.aws.amazon.com/cli/latest/reference/ec2/describe-instance-status.html
     * CLI: aws ec2 describe-instance-status --instance-id i-1234567890abcdef0
     */
    public void getInstanceStatus(String instanceId) {

        List<String> instanceIds = new ArrayList<String>();
        instanceIds.add(instanceId);

        DescribeInstanceStatusRequest request = new DescribeInstanceStatusRequest();
        request.setInstanceIds(instanceIds);
        DescribeInstanceStatusResult response = AWSEC2Client.describeInstanceStatus(request);

        if(response!=null && response.getInstanceStatuses()!=null && !response.getInstanceStatuses().isEmpty()) {
            List<InstanceStatus> instanceStatuses = response.getInstanceStatuses();
            for(InstanceStatus instanceStatus: instanceStatuses) {
                log.info("InstanceId: " + instanceStatus.getInstanceId());
                log.info("InstanceState" + instanceStatus.getInstanceState());
                log.info("InstanceStatus: " + instanceStatus.getInstanceStatus());
                log.info("SystemStatus: " + instanceStatus.getSystemStatus());
                log.info("-------");
            }
        } else {
            log.info("No Instance Statuses Found!!!");
        }
    }

    public List<SecurityGroup> getSecurityGroupId(List<String> securityGroupNames) {
        DescribeSecurityGroupsRequest request = new DescribeSecurityGroupsRequest();

        request.setGroupNames(securityGroupNames);

        DescribeSecurityGroupsResult result = AWSEC2Client.describeSecurityGroups(request);

        System.out.print("The corresponding security group ID is : " + result.getSecurityGroups());
        return result.getSecurityGroups();
    }

    public List<String> listInstanceSubnetIds(String instanceState) {

        List<String> instanceSubnetIdList = new ArrayList<String>();

        DescribeInstancesResult response = AWSEC2Client.describeInstances();

        if(response!=null && response.getReservations()!=null && !response.getReservations().isEmpty()) {
            for(Reservation reservation: response.getReservations()) {
                List<Instance> instances = reservation.getInstances();

                if(instances!=null && !instances.isEmpty()) {
                    for(Instance instance: instances) {
                        if (instance.getState().getName().equals(instanceState)){
                            instanceSubnetIdList.add(instance.getSubnetId());
                            log.info("InstanceId: " + instance.getInstanceId());
                            log.info("ImageId: " + instance.getImageId());
                            log.info("InstanceType: " + instance.getInstanceType());
                            log.info("KeyName: " + instance.getKeyName());
                            log.info("State: " + instance.getState());
                            log.info("PrivateDnsName: " + instance.getPrivateDnsName());
                            log.info("PrivateIpAddress: " + instance.getPrivateIpAddress());
                            log.info("PublicDnsName: " + instance.getPublicDnsName());
                            log.info("PublicIpAddress: " + instance.getPublicIpAddress());
                            log.info("Architecture: " + instance.getArchitecture());
                            log.info("SecurityGroups: " + instance.getSecurityGroups());
                            log.info("SubnetID: " + instance.getSubnetId());

                        }
                        log.info("-------");
                    }
                } else {
                    log.info("No Instances SubnetID Found!!!");
                }
            }
        } else {
            log.info("No Reservation List Found!!!");
        }

        return instanceSubnetIdList;
    }

    public List<String> listInstanceVpcIds(String instanceState) {

        List<String> instanceVpcIdList = new ArrayList<String>();

        DescribeInstancesResult response = AWSEC2Client.describeInstances();

        if(response!=null && response.getReservations()!=null && !response.getReservations().isEmpty()) {
            for(Reservation reservation: response.getReservations()) {
                List<Instance> instances = reservation.getInstances();

                if(instances!=null && !instances.isEmpty()) {
                    for(Instance instance: instances) {
                        if (instance.getState().getName().equals(instanceState)){
                            instanceVpcIdList.add(instance.getVpcId());
                            log.info("InstanceId: " + instance.getInstanceId());
                            log.info("VpcID: " + instance.getVpcId());
                        }
                        log.info("-------");
                    }
                } else {
                    log.info("No Instances VpcID Found!!!");
                }
            }
        } else {
            log.info("No Reservation List Found!!!");
        }

        return instanceVpcIdList;
    }

    public List<String> getSubnetId() {
        DescribeSubnetsResult subnetsResult = AWSEC2Client.describeSubnets();

        List<String> res = new ArrayList<>();
        for (int i = 0; i < subnetsResult.getSubnets().size(); i++) {
            res.add(subnetsResult.getSubnets().get(i).getSubnetId());
        }

        return res;
    }

    private LaunchSpecification createLaunchSpecification(
            String ami,
            InstanceType instanceType,
            String securityGroupName,
            String keyPairName) {
        LaunchSpecification launchSpecification = new LaunchSpecification();

        launchSpecification.setImageId(ami);
        launchSpecification.setInstanceType(instanceType);

        List<String> securityGroupList = new ArrayList<String>();
        securityGroupList.add(securityGroupName);
        launchSpecification.setSecurityGroups(securityGroupList);

        launchSpecification.setKeyName(keyPairName);

        return launchSpecification;
    }


    public void assignTagToEC2(String instanceId, String key, String value) {

        List<String> instanceIds = new ArrayList<String>();
        instanceIds.add(instanceId);

        List<Tag> tags = new ArrayList<Tag>();
        Tag tag1 = new Tag();
        tag1.setKey(key);
        tag1.setValue(value);

        tags.add(tag1);

        CreateTagsRequest request = new CreateTagsRequest();
        request.setResources(instanceIds);
        request.setTags(tags);

        AWSEC2Client.createTags(request);
    }

    public void deleteTags(String instanceId) {

        List<String> instanceIds = new ArrayList<String>();
        instanceIds.add(instanceId);

        DeleteTagsRequest request = new DeleteTagsRequest();
        request.setResources(instanceIds);

        DeleteTagsResult response = AWSEC2Client.deleteTags(request);
        log.info("response: " + response);
    }

    public void listTags() {

        DescribeTagsResult response = AWSEC2Client.describeTags();

        if(response!=null && response.getTags()!=null && !response.getTags().isEmpty()) {
            List<TagDescription> tagDescriptions = response.getTags();
            for(TagDescription tagDescription: tagDescriptions) {
                log.info("ResourceId: " + tagDescription.getResourceId());
                log.info("ResourceType: " + tagDescription.getResourceType());
                log.info("Key: " + tagDescription.getKey());
                log.info("Value" + tagDescription.getValue());
                log.info("-------");
            }
        }
    }

    public Map<String, String> getEC2MetricStats(String instanceId, String metricName) {
        Map<String, String> res = new LinkedHashMap<>();

        List<Dimension> dimensionList = new ArrayList<>();

        GetMetricStatisticsRequest request = new GetMetricStatisticsRequest();
        Dimension dimension = new Dimension();
        dimension.setName("InstanceId");
        dimension.setValue(instanceId);
        dimensionList.add(dimension);

        request.setDimensions(dimensionList);

        Date endTime =  new Date();
        Date startTime =  new DateTime(endTime).minusMinutes(1).toDate();
        //To get only 1 minute stats for each metric.

        request.setMetricName(metricName);
        request.setStartTime(startTime);
        request.setEndTime(endTime);
        request.setPeriod(60);
        request.setNamespace("AWS/EC2");

        List<String> statsList = new ArrayList<>();
        statsList.add("Sum");
        statsList.add("Average");
        statsList.add("Maximum");
        statsList.add("Minimum");

        request.setStatistics(statsList);

        GetMetricStatisticsResult result = AWSCloudWatchClient.getMetricStatistics(request);

        if (result.getDatapoints().size() > 0) {
            for (Datapoint point : result.getDatapoints()) {
                res.put(metricName + " Average", point.getAverage().toString());
                res.put(metricName + " Maximum", point.getMaximum().toString());
                res.put(metricName + " Minimum", point.getMinimum().toString());

                log.info(instanceId + " " + metricName + " Average: " + point.getAverage());
                log.info(instanceId + " " + metricName + " Maximum: " + point.getAverage());
                log.info(instanceId + " " + metricName + " Minimum: " + point.getAverage());
            }
        }
        return res;
    }

}

