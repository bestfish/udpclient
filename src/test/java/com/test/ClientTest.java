package com.test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import com.fish.play.client.DefaultClient;

public class ClientTest {

	/**
	 * @param args
	 */
	public static void main(String[] args) throws Exception {

		final DefaultClient client = new DefaultClient("192.168.200.205:8080");

		int threads = Runtime.getRuntime().availableProcessors() * 2;
		ExecutorService exec = Executors.newFixedThreadPool(threads);
		final CountDownLatch latch = new CountDownLatch(1000);
		for (int i = 0; i < 1000; i++) {
			final int index = i;
			exec.execute(new Runnable() {

				@Override
				public void run() {
					client.writeAndFlush("log" + index, "abcdefg");
					latch.countDown();
				}
			});
		}

		latch.await();
		exec.shutdown();
		client.destroy();
	}

}
