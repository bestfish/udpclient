package com.fish.play.client;

import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.DatagramPacket;
import io.netty.channel.socket.nio.NioDatagramChannel;
import io.netty.util.CharsetUtil;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.InetSocketAddress;
import java.util.List;
import java.util.Random;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.fish.play.client.handler.DefaultClientHandler;
import com.fish.play.client.util.AddrUtil;

public class DefaultClient {
	private static final Log log = LogFactory.getLog(DefaultClient.class);
	private static final String SEPARATOR = ":";
	private EventLoopGroup group;
	private String serverAddresses;
	private List<InetSocketAddress> inetSockets;
	private Random random = new Random();

	public DefaultClient() {
	}

	public DefaultClient(String serverAddresses) {
		this.serverAddresses = serverAddresses;
		setupEnv();
	}

	private void setupEnv() {
		try {
			inetSockets = AddrUtil.getAddresses(serverAddresses);
			group = new NioEventLoopGroup();
		} catch (Exception e) {
			log.error("UDP client初始化异常," + e);
			throw new RuntimeException("UDP client初始化异常");
		}
	}

	private Channel getChannel() throws InterruptedException {
		Bootstrap bootstrap = new Bootstrap();
		bootstrap.group(group).channel(NioDatagramChannel.class).option(ChannelOption.SO_BROADCAST, true);
		return bootstrap.handler(new DefaultClientHandler()).bind(0).sync().channel();
	}

	/**
	 * 该方法不关注响应
	 * 
	 * @param message
	 *            待发送消息
	 */
	public void writeAndFlush(String key, String message) {
		if (message == null) {
			return;
		}

		Channel channel = null;
		try {
			channel = getChannel();
			DatagramPacket packet = new DatagramPacket(Unpooled.copiedBuffer(concatKeyValue(key, message),
					CharsetUtil.UTF_8), pickBalanceOne());
			channel.writeAndFlush(packet).sync();
		} catch (Exception e) {
			log.error("UDP包发送异常, " + e);
		} finally {
			if (channel != null) {
				channel.close();
			}
		}

	}

	private String concatKeyValue(String key, String value) {
		return key + SEPARATOR + value;
	}

	public void writeAndFlush(String key, Throwable t) {
		if (t == null) {
			return;
		}

		StringWriter writer = new StringWriter();
		PrintWriter printWriter = new PrintWriter(writer);
		t.printStackTrace(printWriter);
		printWriter.close();
		String message = writer.toString();
		writeAndFlush(key, message);
	}

	/**
	 * @deprecated
	 * @param message
	 * @param timeoutMillis
	 */
	public void writeAndFlushAndReturnAll(String message, long timeoutMillis) {
		if (message == null) {
			return;
		}

		Channel channel = null;
		try {
			channel = getChannel();
			DatagramPacket packet = new DatagramPacket(Unpooled.copiedBuffer(message, CharsetUtil.UTF_8),
					pickBalanceOne());
			channel.writeAndFlush(packet).sync();

			if (!channel.closeFuture().await(timeoutMillis)) {
				log.error("receive time out!");
			}

		} catch (Exception e) {
			log.error("UDP包发送异常, " + e);
		}

	}

	private InetSocketAddress pickBalanceOne() {
		int index = random.nextInt(inetSockets.size());
		return inetSockets.get(index);
	}

	public void destroy() {
		if (group != null) {
			group.shutdownGracefully();
		}
	}

	public String getServerAddresses() {
		return serverAddresses;
	}

	public void setServerAddresses(String serverAddresses) {
		this.serverAddresses = serverAddresses;
		setupEnv();
	}

}
