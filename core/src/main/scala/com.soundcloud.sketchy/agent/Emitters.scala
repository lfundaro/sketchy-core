package com.soundcloud.sketchy.agent

import com.soundcloud.sketchy.broker.HaBroker
import com.soundcloud.sketchy.events.{ Event, SketchySignal }

/**
 * Rabbit Sink
 */
abstract class RabbitEmitterAgent(
  broker: HaBroker,
  exchange: String,
  key: String) extends Agent {

  val producer = broker.producer

  def publish(signal: SketchySignal) {
    producer.publish(exchange, key, signal.jsonPretty)
  }
}

/**
 * Emit sketchy signals back onto the message bus.
 */
class SignalEmitterAgent(
  broker: HaBroker,
  exchange: String,
  key: String) extends RabbitEmitterAgent(broker, exchange, key) {

  def on(event: Event): Seq[Event] = {
    event match {
      case signal: SketchySignal => {
        publish(signal)
        meter(signal.kind)

        signal :: Nil
      }
      case _ => Nil
    }
  }

  private val counter = prometheusCounter("kind")
  private def meter(kind: String) {
    counter.newPartial()
      .labelPair("kind", kind)
      .apply().increment()
  }
}
