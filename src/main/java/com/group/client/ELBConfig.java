package com.group.client;

import java.util.Arrays;
import java.util.List;

/**
 * Created by wtang on 3/10/17.
 */
public class ELBConfig {
	private String name;
	private List<String> securityGroupName;
	private List<String> availabilityZone;
	private String elbProtocol;
	private String instanceProtocol;
	private Integer elbPort;
	private Integer instancePort;
	private List<String> targetGroupName;

	public String getName() {
		return name;
	}

	public List<String> getSecurityGroupName() {
		return securityGroupName;
	}

	public List<String> getAvailablityZone() {
		return availabilityZone;
	}

	public String getelbProtocol() {
		return elbProtocol;
	}

	public String getInstanceProtocol() {
		return instanceProtocol;
	}

	public Integer getelbPort() {
		return elbPort;
	}

	public Integer getInstancePort() {
		return instancePort;
	}

	public List<String> getTargetGroupName() {
		return targetGroupName;
	}

	public void setName(String name) {
		this.name = name;
	}

	public void setSecurityGroupName(String[] securityGroupName) {
		this.securityGroupName = Arrays.asList(securityGroupName);
	}

	public void setAvailablityZone(String[] availablityZone) {
		this.availabilityZone = Arrays.asList(availablityZone);
	}

	public void setelbrotocol(String elbProtocol) {
		this.elbProtocol = elbProtocol;
	}

	public void setInstanceProtocol(String instanceProtocol) {
		this.instanceProtocol = instanceProtocol;
	}

	public void setelbPort(Integer elbPort) {
		this.elbPort = elbPort;
	}

	public void setInstancePort(Integer instancePort) {
		this.instancePort = instancePort;
	}

	public void setTargetGroupName(List<String> targetGroupName) {
		this.targetGroupName = targetGroupName;
	}
}
