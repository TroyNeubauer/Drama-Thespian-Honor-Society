package com.troy.dramaserver.net;

import java.util.concurrent.TimeUnit;

import org.apache.logging.log4j.*;

import com.troy.dramaserver.Server;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.logging.*;

public class Net {

	private static final Logger logger = LogManager.getLogger(Net.class);

	private static final int PORT = 443;

	EventLoopGroup bossGroup = new NioEventLoopGroup(1);
	EventLoopGroup workerGroup = new NioEventLoopGroup();

	public Net(Server server) {
		try {

			ServerBootstrap b = new ServerBootstrap();
			b.group(bossGroup, workerGroup)//format:off
	                .channel(NioServerSocketChannel.class)
	                .handler(new LoggingHandler(LogLevel.INFO))
	                .childHandler(new HttpsChannelInitializer())
	                .childOption(ChannelOption.AUTO_READ, true)
	                .bind(PORT).sync();
	    } catch (InterruptedException e) {//format:on
			throw new RuntimeException(e);
		}

		NetAPIs.init(server);
	}

	public void cleanUp() {
		bossGroup.shutdownGracefully(0, 0, TimeUnit.MILLISECONDS);
		workerGroup.shutdownGracefully(0, 0, TimeUnit.MILLISECONDS);
	}

	public void join() {
		try {
			bossGroup.awaitTermination(Long.MAX_VALUE, TimeUnit.SECONDS);
		} catch (InterruptedException e) {
			logger.warn("Exception awaiting boss group");
			logger.catching(e);
		}
		try {
			workerGroup.awaitTermination(Long.MAX_VALUE, TimeUnit.SECONDS);
		} catch (InterruptedException e) {
			logger.warn("Exception awaiting worker group");
			logger.catching(e);
		}
	}

}
