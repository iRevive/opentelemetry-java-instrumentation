package com.example.helloworld.client;

import java.io.IOException;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

/**
 * This class is just a HTTP Client with a trace started.
 * The spanId and the traceId are forwarded through HTTP headers
 * The server is able to reconstruct the parent span via extracted headers
 * The full trace will be reconstruct via the APM
 */
public class TracedClient {

	public static void main(String[] args) throws Exception{
		OkHttpClient client = new OkHttpClient().newBuilder().build();

		Request request = new Request.Builder()
				.url("http://localhost:8080/hello/history")
				.build();

		Response response = client.newCall(request).execute();
		if (!response.isSuccessful()) throw new IOException("Unexpected code " + response);

		System.out.println(response.body().string());
	}
}
