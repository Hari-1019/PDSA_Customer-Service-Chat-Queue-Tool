package com.chatqueue.dto;

import com.chatqueue.model.AgentStatus;
import java.util.List;

public record AdminDashboardResponse(
        int vipWaiting,
        int normalWaiting,
        List<AgentStatus> agents
) {}
