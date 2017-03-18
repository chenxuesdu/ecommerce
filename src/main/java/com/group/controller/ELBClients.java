package com.group.controller;

/**
 * Created by wtang on 3/12/17.
 */
import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.amazonaws.regions.Region;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.elasticloadbalancing.AmazonElasticLoadBalancingClient;
import com.amazonaws.services.elasticloadbalancing.model.*;
//import com.amazonaws.services.elasticloadbalancingv2.model.CreateTargetGroupRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import org.apache.commons.lang3.builder.ReflectionToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

public class ELBClients extends AWSClients{

    private AmazonElasticLoadBalancingClient AWSELBClient;
    public ELBConfig elbConfig;

    public ELBClients(String configFilePath) {
        super();
        AWSELBClient = new AmazonElasticLoadBalancingClient(getCredentials());
        Region usaRegion = Region.getRegion(Regions.US_WEST_2);
        AWSELBClient.setRegion(usaRegion);

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
    public void createELB(String elbName, List<String> securityGroups, List<String> ec2InstanceSubnetIds) {
        CreateLoadBalancerRequest request = new CreateLoadBalancerRequest();
        request.setLoadBalancerName(elbName);
        List<Listener> listeners = new ArrayList<Listener>();
        listeners.add(new Listener(elbConfig.getelbProtocol(), elbConfig.getelbPort(), elbConfig.getInstancePort()));
        //listeners.add(new Listener("HTTP", 80, 80));
        //listeners.add(new Listener("HTTPS", 443, 443));
        System.out.print(elbConfig.getAvailablityZone());
        request.withAvailabilityZones(elbConfig.getAvailablityZone());
        //request.setSubnets(ec2InstanceSubnetIds);
        /* either use Subnet or Availability Zone. */

        request.setListeners(listeners);

        //CreateTargetGroupRequest targetGroupRequest = new CreateTargetGroupRequest()
        //        .withName("my-targets").withPort(80).withProtocol("HTTP")
        //          .withVpcId("vpc-3ac0fb5f");


        request.setSecurityGroups(securityGroups);

        CreateLoadBalancerResult response = AWSELBClient.createLoadBalancer(request);

        System.out.println("CreateELBResult: " + response);
    }

    /*
     * http://docs.aws.amazon.com/AWSJavaSDK/latest/javadoc/com/amazonaws/services/elasticloadbalancing/model/LoadBalancerAttributes.html
     */

    public void enableCrossZoneLoadBalancing() {
        ModifyLoadBalancerAttributesRequest request = new ModifyLoadBalancerAttributesRequest();

        CrossZoneLoadBalancing crossZoneLoadBalancing = new CrossZoneLoadBalancing();
        crossZoneLoadBalancing.setEnabled(true);

        LoadBalancerAttributes loadBalancerAttributes = new LoadBalancerAttributes();
        loadBalancerAttributes.setCrossZoneLoadBalancing(crossZoneLoadBalancing);

        request.withLoadBalancerAttributes(loadBalancerAttributes);
        request.withLoadBalancerName(elbConfig.getName());

        ModifyLoadBalancerAttributesResult response = AWSELBClient.modifyLoadBalancerAttributes(request);

        System.out.println("EnableCrossZoneLoadBalancing result: " + response);

    }

    /*
     * http://docs.aws.amazon.com/cli/latest/reference/elb/apply-security-groups-to-load-balancer.html
     */
    public void applySecurityGroupELB(String ELBName, List<String> securityGroups) {
        ApplySecurityGroupsToLoadBalancerRequest request = new ApplySecurityGroupsToLoadBalancerRequest();
        request.setLoadBalancerName(ELBName);
        request.setSecurityGroups(securityGroups);

        ApplySecurityGroupsToLoadBalancerResult response = AWSELBClient.applySecurityGroupsToLoadBalancer(request);

        System.out.println("ApplySecurityGroupsToELBResult: " + response);
    }
    /*
     * http://docs.aws.amazon.com/cli/latest/reference/elb/create-load-balancer-listeners.html
     */
    public void createELBListener(String elbProtocol, String instanceProtocol, Integer instancePort, Integer elbPort) {
        CreateLoadBalancerListenersRequest request = new CreateLoadBalancerListenersRequest();

        List<Listener> listeners = new ArrayList<Listener>();
        Listener listener = new Listener();
        listener.setInstanceProtocol(instanceProtocol);
        listener.setProtocol(elbProtocol);
        listener.setInstancePort(instancePort);
        listener.setLoadBalancerPort(elbPort);
        listeners.add(listener);

        request.setListeners(listeners);

        CreateLoadBalancerListenersResult response = AWSELBClient.createLoadBalancerListeners(request);

        System.out.println("CreateLoadBalancerListenersResult: " + response);
    }

    /*
     * http://docs.aws.amazon.com/cli/latest/reference/elb/delete-load-balancer.html
     */
    public void deleteELB(String ELBName) {
        DeleteLoadBalancerRequest request = new DeleteLoadBalancerRequest();
        request.setLoadBalancerName(ELBName);

        DeleteLoadBalancerResult response = AWSELBClient.deleteLoadBalancer(request);

        System.out.println("DeleteLoadBalancerResult: " + response);
    }

    /*
     * http://docs.aws.amazon.com/cli/latest/reference/elb/describe-load-balancers.html
     */
    public void listELB() {
        DescribeLoadBalancersResult response = AWSELBClient.describeLoadBalancers();

        List<LoadBalancerDescription> ELBDescriptionList = response.getLoadBalancerDescriptions();

        printELBDescription(ELBDescriptionList);
    }

    public List<String> getELBDNSName() {
        DescribeLoadBalancersResult response = AWSELBClient.describeLoadBalancers();
        List<LoadBalancerDescription> ELBDescriptionList = response.getLoadBalancerDescriptions();
        List<String> elbDNSNameList = new ArrayList<String>();

        for(LoadBalancerDescription ELBDescription: ELBDescriptionList) {
            elbDNSNameList.add(ELBDescription.getDNSName());
        }
        return elbDNSNameList;
    }
    /*
     * http://docs.aws.amazon.com/cli/latest/reference/elb/describe-load-balancers.html
     */
    public void searchELB(String ELBName) {

        DescribeLoadBalancersRequest request = new DescribeLoadBalancersRequest();

        List<String> ELBs = new ArrayList<String>();
        ELBs.add(ELBName);
        request.setLoadBalancerNames(ELBs);

        DescribeLoadBalancersResult response = AWSELBClient.describeLoadBalancers(request );

        List<LoadBalancerDescription> ELBDescriptionList = response.getLoadBalancerDescriptions();

        printELBDescription(ELBDescriptionList);
    }

    private void printELBDescription(List<LoadBalancerDescription> ELBDescriptionList) {
        for(LoadBalancerDescription ELBDescription: ELBDescriptionList) {
            System.out.println(ELBDescription.getLoadBalancerName());
            System.out.println(ELBDescription.getDNSName());

            System.out.println("HealthCheck: ");
            System.out.println("\tTarget: " + ELBDescription.getHealthCheck().getTarget());
            System.out.println("\tInterval: " + ELBDescription.getHealthCheck().getInterval());
            System.out.println("\tHealthyThreshold: " + ELBDescription.getHealthCheck().getHealthyThreshold());
            System.out.println("\tTimeout: " + ELBDescription.getHealthCheck().getTimeout());
            System.out.println("\tUnhealthyThreshold: " + ELBDescription.getHealthCheck().getUnhealthyThreshold());
            
            System.out.println("Instances: ");
            for(Instance instance: ELBDescription.getInstances()) {
                System.out.println("\tInstanceId: " + instance.getInstanceId());
            }

            System.out.println("Listener Descriptions:");
            for(ListenerDescription listenerDescription: ELBDescription.getListenerDescriptions()) {
                System.out.println("\tProtocol: " + listenerDescription.getListener().getProtocol());
                System.out.println("\tInstanceProtocol: " + listenerDescription.getListener().getInstanceProtocol());
                System.out.println("\tLoadBalancerPort: " + listenerDescription.getListener().getLoadBalancerPort());
                System.out.println("\tInstancePort: " + listenerDescription.getListener().getInstancePort());
            }

            System.out.println("SecurityGroups: ");
            for(String securityGroup: ELBDescription.getSecurityGroups()) {
                System.out.println("\tName: " + securityGroup);
            }
        }
    }

    /*
     * http://docs.aws.amazon.com/AWSJavaSDK/latest/javadoc/com/amazonaws/services/elasticloadbalancing/model/HealthCheck.html
     * 
     * http://docs.aws.amazon.com/cli/latest/reference/elb/configure-health-check.html
     */
    public void createOrUpdateHealthCheck(String ELBName) {
        HealthCheck healthCheck = new HealthCheck();
        healthCheck.withHealthyThreshold(2);
        healthCheck.withInterval(30);
        healthCheck.withTarget("TCP:22");
        healthCheck.withTimeout(5);
        healthCheck.withUnhealthyThreshold(2);

        ConfigureHealthCheckRequest request = new ConfigureHealthCheckRequest();
        request.setHealthCheck(healthCheck);
        request.setLoadBalancerName(ELBName);

        ConfigureHealthCheckResult response = AWSELBClient.configureHealthCheck(request);

        System.out.println("ConfigureHealthCheckResult: " + response);
    }

    /*
     * http://docs.aws.amazon.com/cli/latest/reference/elb/describe-instance-health.html
     */
    public Map<String, String> getInstanceHealth(String ELBName, List<String> instanceIds) {
        DescribeInstanceHealthRequest request = new DescribeInstanceHealthRequest();
        request.setLoadBalancerName(ELBName);
        request.setInstances(getELBInstanceList(instanceIds));

        Map<String, String> instanceElbState = new HashMap<String, String>();

        DescribeInstanceHealthResult response = AWSELBClient.describeInstanceHealth(request);

        System.out.println("DescribeInstanceHealthResult: " + response);
        List<InstanceState> instanceStates = response.getInstanceStates();
        for(InstanceState instanceState: instanceStates) {
            System.out.println("InstanceId: " + instanceState.getInstanceId());
            System.out.println("Description: " + instanceState.getDescription());
            System.out.println("State: " + instanceState.getState());
            System.out.println("ReasonCode: " + instanceState.getReasonCode());
            instanceElbState.put(instanceState.getInstanceId(), instanceState.getState());
        }

        return instanceElbState;
    }

    /*
     * http://docs.aws.amazon.com/cli/latest/reference/elb/delete-load-balancer-listeners.html
     */
    public void deleteELBListener(String ELBName, Integer ELBPort) {
        DeleteLoadBalancerListenersRequest request = new DeleteLoadBalancerListenersRequest();
        request.setLoadBalancerName(ELBName);

        List<Integer> ELBPorts = new ArrayList<Integer>();
        ELBPorts.add(ELBPort);
        request.setLoadBalancerPorts(ELBPorts);

        DeleteLoadBalancerListenersResult response = AWSELBClient.deleteLoadBalancerListeners(request);

        System.out.println("DeleteLoadBalancerListenersResult: " + response);
    }

    /*
     * http://docs.aws.amazon.com/cli/latest/reference/elb/register-instances-with-load-balancer.html
     */
    public void registerInstancesToELB(String ELBName, List<String> instanceIds) {
        RegisterInstancesWithLoadBalancerRequest request = new RegisterInstancesWithLoadBalancerRequest();
        request.setLoadBalancerName(ELBName);
        request.setInstances(getELBInstanceList(instanceIds));

        RegisterInstancesWithLoadBalancerResult response = AWSELBClient.registerInstancesWithLoadBalancer(request);

        System.out.println("RegisterInstancesWithLoadBalancerResult: " + response);
    }

    /*
     * http://docs.aws.amazon.com/cli/latest/reference/elb/deregister-instances-from-load-balancer.html
     */
    public void deregisterInstancesFromELB(String ELBName, List<String> instanceIds) {
        DeregisterInstancesFromLoadBalancerRequest request = new DeregisterInstancesFromLoadBalancerRequest();
        request.setLoadBalancerName(ELBName);
        request.setInstances(getELBInstanceList(instanceIds));

        DeregisterInstancesFromLoadBalancerResult response = AWSELBClient.deregisterInstancesFromLoadBalancer(request);

        System.out.println("DeregisterInstancesFromLoadBalancerResult: " + response);
    }

    /*
     * http://docs.aws.amazon.com/cli/latest/reference/elb/add-tags.html
     */
    public void addTags(String ELBName, String key, String value) {
        List<String> ELBNames = new ArrayList<>();
        ELBNames.add(ELBName);

        Tag tag = new Tag();
        tag.setKey(key);
        tag.setValue(value);

        List<Tag> tags = new ArrayList<Tag>();
        tags.add(tag);

        AddTagsRequest request = new AddTagsRequest();
        request.setLoadBalancerNames(ELBNames);
        request.setTags(tags);

        AddTagsResult response = AWSELBClient.addTags(request );

        System.out.println("AddTagsResult: " + response);
    }

    /*
     * http://docs.aws.amazon.com/cli/latest/reference/elb/describe-tags.html
     */
    public void listTags(String ELBName) {
        List<String> ELBNames = new ArrayList<>();
        ELBNames.add(ELBName);

        DescribeTagsRequest request = new DescribeTagsRequest();
        request.setLoadBalancerNames(ELBNames);

        DescribeTagsResult response = AWSELBClient.describeTags(request );

        List<TagDescription> tagDescriptionList = response.getTagDescriptions();
        for(TagDescription tagDescription: tagDescriptionList) {
            System.out.println(tagDescription.getLoadBalancerName());
            System.out.println("Tags: ");
            for(Tag tag: tagDescription.getTags()) {
                System.out.println("\t" + tag.getKey() + "-" + tag.getValue());
            }
        }
    }

    /*
     * http://docs.aws.amazon.com/cli/latest/reference/elb/remove-tags.html
     */
    public void deleteTags(String ELBName, String key) {
        List<String> ELBNames = new ArrayList<>();
        ELBNames.add(ELBName);

        TagKeyOnly tagKeyOnly = new TagKeyOnly();
        tagKeyOnly.setKey(key);

        List<TagKeyOnly> tagKeyOnlyList = new ArrayList<TagKeyOnly>();
        tagKeyOnlyList.add(tagKeyOnly );

        RemoveTagsRequest request = new RemoveTagsRequest();
        request.setLoadBalancerNames(ELBNames);
        request.setTags(tagKeyOnlyList);

        RemoveTagsResult response = AWSELBClient.removeTags(request);

        System.out.println("RemoveTagsResult: " + response);
    }

    private List<Instance> getELBInstanceList(List<String> instanceIds) {

        List<Instance> instanceList = new ArrayList<Instance>();

        if(instanceIds!=null && !instanceIds.isEmpty()) {
            for(String instanceId: instanceIds){
                instanceList.add(new Instance(instanceId));
            }
        }

        return instanceList;
    }
}
