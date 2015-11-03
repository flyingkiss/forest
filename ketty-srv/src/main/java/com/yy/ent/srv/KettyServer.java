package com.yy.ent.srv;

import com.yy.ent.mvc.ioc.KettyIOC;
import com.yy.ent.srv.core.HttpServerInitializer;
import com.yy.ent.srv.core.KettyServerInitializer;
import com.yy.ent.srv.core.ServerContext;
import com.yy.ent.srv.uitl.ServerType;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import io.netty.util.concurrent.DefaultEventExecutorGroup;
import io.netty.util.concurrent.DefaultThreadFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created with IntelliJ IDEA.
 * User: Dempe
 * Date: 2015/10/15
 * Time: 16:41
 * To change this template use File | Settings | File Templates.
 */
public class KettyServer {

    private static final Logger LOGGER = LoggerFactory.getLogger(KettyServer.class);

    private EventLoopGroup bossGroup;

    private EventLoopGroup workerGroup;

    private ServerBootstrap b;

    private DefaultEventExecutorGroup executorGroup;

    private KettyIOC cherry;

    private ServerContext context = new ServerContext();


    public KettyServer() {
        this(ServerType.KETTY_SERVER);
    }

    public KettyServer(ServerType serverType) {
        executorGroup = new DefaultEventExecutorGroup(4, new DefaultThreadFactory("decode-worker-thread-pool"));
        ChannelInitializer channelInitializer;
        switch (serverType) {
            case KETTY_SERVER:
                channelInitializer = new KettyServerInitializer(context);
                break;
            case HTTP_SERVER:
                channelInitializer = new HttpServerInitializer(context);
                break;
            default:
                channelInitializer = new KettyServerInitializer(context);
        }
        init(channelInitializer);
    }

    public void start(int port) {
        try {
            ChannelFuture f = b.bind(port).sync();
            f.channel().closeFuture().sync();
        } catch (InterruptedException e) {
            LOGGER.info("server start:{}", port);
        } finally {
            stop();
        }
    }


    private void init(ChannelInitializer channelInitializer) {
        bossGroup = new NioEventLoopGroup();
        workerGroup = new NioEventLoopGroup(Runtime.getRuntime().availableProcessors() * 2);
        b = new ServerBootstrap();
        b.group(bossGroup, workerGroup)
                .channel(NioServerSocketChannel.class)
                .option(ChannelOption.TCP_NODELAY, true)
                .option(ChannelOption.SO_KEEPALIVE, true)
                .handler(new LoggingHandler(LogLevel.INFO))
                .childHandler(channelInitializer);
    }


    public void stop() {
        if (bossGroup != null)
            bossGroup.shutdownGracefully();
        if (workerGroup != null)
            workerGroup.shutdownGracefully();
    }

    public KettyServer stopWithJVMShutdown() {
        Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {
            @Override
            public void run() {
                stop();
            }
        }));
        return this;
    }

    public KettyServer initMVC() throws Exception {
        cherry = new KettyIOC();
        return this;
    }
}
