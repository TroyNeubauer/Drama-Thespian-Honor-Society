package com.troy.dramaserver.net;

import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.FullHttpRequest;

public class Http {
	public static HttpResponceBuilder respond(ChannelHandlerContext ctx, FullHttpRequest request) {
		return new HttpResponceBuilder(ctx, request);
	}
}
