package com.chatqueue.repository;

import com.chatqueue.model.AgentStatus;
import java.util.List;

/**
 * Response DTO for Admin Dashboard.
 * Shows current queue sizes and all agents with their status.
 */
public class AdminDashboardResponse {
    private int vipWaiting;
    private int normalWaiting;
    private List<AgentStatus> agents;

    public AdminDashboardResponse(int vipWaiting, int normalWaiting, List<AgentStatus> agents) {
        this.vipWaiting = vipWaiting;
        this.normalWaiting = normalWaiting;
        this.agents = agents;
    }

    public int getVipWaiting() {
        return vipWaiting;
    }

    public void setVipWaiting(int vipWaiting) {
        this.vipWaiting = vipWaiting;
    }

    public int getNormalWaiting() {
        return normalWaiting;
    }

    public void setNormalWaiting(int normalWaiting) {
        this.normalWaiting = normalWaiting;
    }

    public List<AgentStatus> getAgents() {
        return agents;
    }

    public void setAgents(List<AgentStatus> agents) {
        this.agents = agents;
    }
}
