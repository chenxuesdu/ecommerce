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
import com.amazonaws.services.elasticloadbalancingv2.AmazonElasticLoadBalancingClient;
//import com.amazonaws.services.elasticloadbalancing.model.*;
import com.amazonaws.services.elasticloadbalancingv2.model.CreateTargetGroupRequest;
import com.amazonaws.services.elasticloadbalancingv2.model.CreateTargetGroupResult;
import com.amazonaws.services.elasticloadbalancingv2.model.CreateLoadBalancerRequest;
import com.amazonaws.services.elasticloadbalancingv2.model.*;


import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import org.apache.commons.lang3.builder.ReflectionToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

public class ELBv2Clients extends AWSClients{

    private AmazonElasticLoadBalancingClient AWSELBClient;
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
    public void createELB(String elbName, List<String> securityGroups, List<String> ec2InstanceSubnetIds, List<String> ec2RunningInstances) {
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

}
