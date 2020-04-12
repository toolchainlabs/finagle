package com.twitter.finagle.postgresql.machine

import com.twitter.finagle.postgresql.Messages
import com.twitter.finagle.postgresql.Response

class SimpleQueryMachine(query: String) extends StateMachine[SimpleQueryMachine.State, Response] {

  override def start: StateMachine.TransitionResult[SimpleQueryMachine.State, Response] = ???

  override def receive(state: SimpleQueryMachine.State, msg: Messages.BackendMessage): StateMachine.TransitionResult[SimpleQueryMachine.State, Response] = ???
}

object SimpleQueryMachine {
  sealed trait State
}
