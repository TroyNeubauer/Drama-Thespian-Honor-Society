package com.troy.dramaserver.net;

import java.io.File;

import javax.net.ssl.SSLException;

import io.netty.channel.ChannelInitializer;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.ssl.*;

public class ServerHandlersInit extends ChannelInitializer<SocketChannel> {

	public ServerHandlersInit() {

	}

	protected void initChannel(SocketChannel socketChannel) throws Exception {
		try {
			SslContext context = SslContextBuilder.forServer(new File("./Private/cert.pem"), new File("./Private/key.pem")).build();
			socketChannel.pipeline().addLast("ssl", context.newHandler(socketChannel.alloc()));

		} catch (SSLException e) {
			throw new RuntimeException(e);
		}
	}

}
