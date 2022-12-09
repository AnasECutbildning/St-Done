package me.code;

import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public class ReverseProxyHandler extends SimpleChannelInboundHandler<ByteBuf> {

    private final ReverseProxyServer server;

    public static List<Integer> PORTS = List.of(8080, 8081, 8082);

    public ReverseProxyHandler(ReverseProxyServer server) {
        this.server = server;
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, ByteBuf buf) throws Exception {
        var original = ctx.channel();

        var bootstrap = new Bootstrap();

        var random = ThreadLocalRandom.current().nextInt(0, PORTS.size());
        var port = PORTS.get(random);

        try {
            var channel = bootstrap
                    .group(server.getWorkerGroup())
                    .channel(NioSocketChannel.class)
                    .handler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        protected void initChannel(SocketChannel socketChannel) throws Exception {
                            var pipeline = socketChannel.pipeline();

                            pipeline.addLast(new ClientHandler(original));
                        }
                    })
                    .connect("localhost", port).sync().channel();

            channel.writeAndFlush(buf.copy());
        } catch (Exception e) {
            e.printStackTrace();
        }

    }
}
