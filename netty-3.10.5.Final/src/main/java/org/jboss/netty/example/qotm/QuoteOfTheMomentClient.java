/*
 * Copyright 2012 The Netty Project
 *
 * The Netty Project licenses this file to you under the Apache License,
 * version 2.0 (the "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at:
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */
package org.jboss.netty.example.qotm;

import org.jboss.netty.bootstrap.ConnectionlessBootstrap;
import org.jboss.netty.channel.ChannelPipeline;
import org.jboss.netty.channel.ChannelPipelineFactory;
import org.jboss.netty.channel.Channels;
import org.jboss.netty.channel.FixedReceiveBufferSizePredictorFactory;
import org.jboss.netty.channel.socket.DatagramChannel;
import org.jboss.netty.channel.socket.nio.NioDatagramChannelFactory;
import org.jboss.netty.handler.codec.string.StringDecoder;
import org.jboss.netty.handler.codec.string.StringEncoder;
import org.jboss.netty.util.CharsetUtil;

import java.net.InetSocketAddress;
import java.util.concurrent.Executors;

/**
 * A UDP broadcast client that asks for a quote of the moment (QOTM) to {@link QuoteOfTheMomentServer}.
 *
 * Inspired by <a href="http://docs.oracle.com/javase/tutorial/networking/datagrams/clientServer.html">the official
 * Java tutorial</a>.
 */
public final class QuoteOfTheMomentClient {

    static final int PORT = Integer.parseInt(System.getProperty("port", "7686"));

    public static void main(String[] args) throws Exception {
        ConnectionlessBootstrap b = new ConnectionlessBootstrap(
                new NioDatagramChannelFactory(Executors.newCachedThreadPool()));

        try {
            // Configure the pipeline factory.
            b.setPipelineFactory(new ChannelPipelineFactory() {
                public ChannelPipeline getPipeline() {
                    return Channels.pipeline(
                            new StringEncoder(CharsetUtil.ISO_8859_1),
                            new StringDecoder(CharsetUtil.ISO_8859_1),
                            new QuoteOfTheMomentClientHandler());
                }
            });

            // Enable broadcast
            b.setOption("broadcast", "true");

            // Allow packets as large as up to 1024 bytes (default is 768).
            // You could increase or decrease this value to avoid truncated packets
            // or to improve memory footprint respectively.
            //
            // Please also note that a large UDP packet might be truncated or
            // dropped by your router no matter how you configured this option.
            // In UDP, a packet is truncated or dropped if it is larger than a
            // certain size, depending on router configuration.  IPv4 routers
            // truncate and IPv6 routers drop a large packet.  That's why it is
            // safe to send small packets in UDP.
            b.setOption(
                    "receiveBufferSizePredictorFactory",
                    new FixedReceiveBufferSizePredictorFactory(1024));

            DatagramChannel c = (DatagramChannel) b.bind(new InetSocketAddress(0));

            // Broadcast the QOTM request to port 8080.
            c.write("QOTM?", new InetSocketAddress("255.255.255.255", PORT));

            // QuoteOfTheMomentClientHandler will close the DatagramChannel when a
            // response is received.  If the channel is not closed within 5 seconds,
            // print an error message and quit.
            if (!c.getCloseFuture().await(5000)) {
                System.err.println("QOTM request timed out.");
                c.close().sync();
            }
        } finally {
            b.releaseExternalResources();
        }
    }
}
