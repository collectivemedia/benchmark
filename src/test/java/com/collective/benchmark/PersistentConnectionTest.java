package com.collective.benchmark;

import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.client.api.Request;
import org.eclipse.jetty.client.api.Response;
import org.junit.Test;

import java.nio.ByteBuffer;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

import static org.mockito.Mockito.*;


/**
 * don't mock http client and use a test server for this test.
 */
public class PersistentConnectionTest {

    @Test
    public void callsGetContentAndReturnsWhenHasContentIsFalse() throws InterruptedException, ExecutionException, TimeoutException {
        HttpClient httpClient = mock(HttpClient.class);
        final Request request = mock(Request.class);
        when(request.content(any(org.eclipse.jetty.client.api.ContentProvider.class))).thenReturn(request);
        when(request.onResponseContent(any(Response.ContentListener.class))).thenReturn(request);
        when(request.onRequestSuccess(any(Request.Listener.class))).thenReturn(request);

        RequestProvider requestProvider = mock(RequestProvider.class);
        when(requestProvider.getRequest(any(HttpClient.class))).thenReturn(null).thenReturn(request);
        when(requestProvider.canProvide()).thenReturn(true);

        // exits because the recursion won't happen
        Response.ContentListener contentListener = mock(Response.ContentListener.class);
        PersistentConnection persistentConnection = new PersistentConnection(httpClient, requestProvider, new RealTimeStats(), contentListener);
        persistentConnection.start();
        verify(requestProvider, times(2)).getRequest(any(HttpClient.class));
        verify(requestProvider, times(2)).canProvide();
        // ignores the first, sends the second
        verify(request).send(any(Response.CompleteListener.class));
        verify(contentListener, never()).onContent(any(Response.class), any(ByteBuffer.class));
    }

}
