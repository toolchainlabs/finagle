package com.twitter.finagle.netty4.channel

import com.twitter.finagle.netty4.ssl.server.{
  Netty4ServerSslChannelInitializer,
  Netty4TlsSnoopingHandler
}
import com.twitter.finagle.param._
import com.twitter.finagle.ssl.{ClientAuth, OpportunisticTls}
import com.twitter.finagle.transport.Transport
import com.twitter.finagle.Stack
import io.netty.channel._
import java.util.logging.Level

private[netty4] object Netty4RawServerChannelInitializer {
  val ChannelLoggerHandlerKey = "channelLogger"
  val ChannelStatsHandlerKey = "channelStats"
}

/**
 * Server channel initialization logic for the part of the netty pipeline that
 * deals with raw bytes.
 *
 * @param params [[Stack.Params]] to configure the `Channel`.
 */
private[netty4] class Netty4RawServerChannelInitializer(params: Stack.Params)
    extends ChannelInitializer[Channel] {

  import Netty4RawServerChannelInitializer._

  private[this] val Logger(logger) = params[Logger]
  private[this] val Label(label) = params[Label]
  private[this] val Stats(stats) = params[Stats]

  private[this] val sharedChannelStats =
    if (!stats.isNull) {
      val sharedChannelStatsFn = params[SharedChannelStats.Param].fn
      Some(sharedChannelStatsFn(params))
    } else None

  private[this] val channelSnooper =
    if (params[Transport.Verbose].enabled)
      Some(ChannelSnooper.byteSnooper(label)(logger.log(Level.INFO, _, _)))
    else
      None

  private[this] val enableTlsSnooping: Boolean =
    // We want to make sure that we both desire opportunistic TLS and haven't
    // signaled through the client auth param that we want to verify the peer.
    if (params[OpportunisticTls.Param].level != OpportunisticTls.Desired) false
    else {
      params[Transport.ServerSsl].sslServerConfiguration match {
        case Some(config) if config.clientAuth != ClientAuth.Needed => true
        case Some(_) =>
          logger.warning(
            "Opportunistic Tls was desired but not enabled because client authorization required.")
          false

        case None =>
          logger.warning(
            "Opportunistic Tls was desired but not enabled because security configuration not specified")
          false
      }
    }

  def initChannel(ch: Channel): Unit = {
    // first => last
    // - a request flies from first to last
    // - a response flies from last to first
    //
    // ssl => channel stats => channel snooper => write timeout => read timeout => req stats => ..
    // .. => exceptions => finagle

    val pipeline = ch.pipeline

    channelSnooper.foreach(pipeline.addFirst(ChannelLoggerHandlerKey, _))

    sharedChannelStats.foreach { sharedStats =>
      val channelStatsHandler = new ChannelStatsHandler(sharedStats)
      pipeline.addFirst(ChannelStatsHandlerKey, channelStatsHandler)
    }

    // Add SSL/TLS Channel Initializer to the pipeline. If we're using snooping
    // then add that and it will handle installing the TLS handlers, if appropriate.
    if (enableTlsSnooping) {
      pipeline.addFirst(Netty4TlsSnoopingHandler.HandlerName, new Netty4TlsSnoopingHandler(params))
    } else {
      pipeline.addFirst(
        Netty4ServerSslChannelInitializer.HandlerName,
        new Netty4ServerSslChannelInitializer(params))
    }
  }
}
