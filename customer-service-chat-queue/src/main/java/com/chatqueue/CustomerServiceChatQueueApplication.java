package com.chatqueue;

import com.chatqueue.model.AgentStatus;
import com.chatqueue.model.enums.UserStatus;
import com.chatqueue.repository.AgentStatusRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class CustomerServiceChatQueueApplication {
	public static void main(String[] args) {
		SpringApplication.run(CustomerServiceChatQueueApplication.class, args);
	}

	// Seed demo agents
	@Bean CommandLineRunner seedAgents(AgentStatusRepository repo) {
		return args -> {
			if (repo.count() == 0) {
				AgentStatus a1 = new AgentStatus();
				a1.setAgentName("Agent A");
				a1.setStatus(UserStatus.AVAILABLE);
				a1.setChatCount(0);
				repo.save(a1);

				AgentStatus a2 = new AgentStatus();
				a2.setAgentName("Agent B");
				a2.setStatus(UserStatus.AVAILABLE);
				a2.setChatCount(0);
				repo.save(a2);
			}
		};
	}
}
