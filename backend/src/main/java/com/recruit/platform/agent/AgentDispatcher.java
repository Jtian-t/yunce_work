package com.recruit.platform.agent;

public interface AgentDispatcher {

    void dispatch(AgentJob job, String payloadJson);
}
