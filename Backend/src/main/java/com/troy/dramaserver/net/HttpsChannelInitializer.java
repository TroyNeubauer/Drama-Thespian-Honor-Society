package com.troy.dramaserver.net;

import java.io.File;

import javax.net.ssl.SSLException;

import io.netty.channel.*;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.http.*;
import io.netty.handler.ssl.*;

public class HttpsChannelInitializer extends ChannelInitializer<SocketChannel> {

	public HttpsChannelInitializer() {

	}

	protected void initChannel(SocketChannel socketChannel) throws Exception {
		try {
			SslContext context = SslContextBuilder.forServer(new File("/etc/letsencrypt/live/dramaserver.tk/cert.pem"), new File("/etc/letsencrypt/live/dramaserver.tk/privkey.pem")).build();
			
			socketChannel.pipeline().addLast("ssl", context.newHandler(socketChannel.alloc()));
			socketChannel.pipeline().addLast(new HttpServerCodec());
			socketChannel.pipeline().addLast(new HttpObjectAggregator(1048576));
			socketChannel.pipeline().addLast(new HttpRequestHandler());
			
		} catch (SSLException e) {
			throw new RuntimeException(e);
		}
	}

}
