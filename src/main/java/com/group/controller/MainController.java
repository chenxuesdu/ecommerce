package com.group.controller;

import com.amazonaws.services.ec2.model.SecurityGroup;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@RestController
public class MainController {
	@RequestMapping("/")
	public String home() {
		return "Welcome to our ecommerce system! Weiyu";
	}

	@RequestMapping("/launchELB")
	public Map<String, String> launchELB() {
		String ec2ConfigFilePath = "/Users/wtang/Documents/295/ecommerce/src/main/java/com/group/controller/ec2Config.yaml";
		String elbConfigFilePath = "/Users/wtang/Documents/295/ecommerce/src/main/java/com/group/controller/elbConfig.yaml";

		ELBClients elbClients = new ELBClients(elbConfigFilePath);

		EC2Clients ec2Instance = new EC2Clients(ec2ConfigFilePath);

            /*
            String instanceId = ec2Instance.createEC2Instance(
                    ec2Instance.ec2Config.getImageID(),
                    ec2Instance.ec2Config.getInstanceType(),
                    ec2Instance.ec2Config.getMinInstanceCount(),
                    ec2Instance.ec2Config.getMaxInstanceCount(),
                    ec2Instance.ec2Config.getKeyName(),
                    ec2Instance.ec2Config.getSecurityGroupName());

            System.out.println("The newly created ec2 instance has an ID: " + instanceId);
            System.out.println(ec2Instance.getInstanceId());
            System.out.println("**********************");
            */

		List<String> ec2List = ec2Instance.listInstances("stopped");

		for (String inst : ec2List) {
			ec2Instance.startInstance(inst);
			//ec2Instance.stopInstance(inst);
		}

		try {
			System.out.println("Sleep 20 seconds waiting for all instances coming up ......");
			Thread.sleep(20000);
		} catch (Exception e) {
			System.out.print(e);
		}

		List<String> ec2InstanceSubnetIds = ec2Instance.listInstanceSubnetIds("running");
		for (String s : ec2InstanceSubnetIds)   System.out.println(s);

		List<SecurityGroup> securityGroups = ec2Instance.getSecurityGroupId(elbClients.elbConfig.getSecurityGroupName());
		List<String> securityGroupId = new ArrayList<String>();
		for (SecurityGroup s : securityGroups) {
			securityGroupId.add(s.getGroupId());
		}

		List<String> ec2InstanceVpcIds = ec2Instance.listInstanceVpcIds("running");
		for (String s : ec2InstanceVpcIds)   System.out.println(s);

		elbClients.createELB(elbClients.elbConfig.getName(), securityGroupId, ec2InstanceSubnetIds);

		//Since there is no web service on ec2 instance yet, just health check @ tcp port 22.
		elbClients.createOrUpdateHealthCheck(elbClients.elbConfig.getName());

		List<String> ec2RunningInstances = ec2Instance.listInstances("running");

		elbClients.registerInstancesToELB(elbClients.elbConfig.getName(), ec2RunningInstances);

		elbClients.enableCrossZoneLoadBalancing();

		elbClients.listELB();

		try {
			System.out.println("Sleep 20 seconds waiting for all instances joining ELB ......");
			Thread.sleep(20000);
		} catch (Exception e) {
			System.out.print(e);
		}

		Map<String, String> instanceElbState = elbClients.getInstanceHealth(elbClients.elbConfig.getName(), ec2List);

		System.out.print(instanceElbState);

		return instanceElbState;
	}

}
