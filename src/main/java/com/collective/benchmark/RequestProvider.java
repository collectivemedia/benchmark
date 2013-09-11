package com.collective.benchmark;

import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.client.api.Request;

public interface RequestProvider {
    Request getRequest(HttpClient client);

    boolean canProvide();
}
