package co.topl.models

import scodec.bits.ByteVector

sealed trait SecretKey

//noinspection ScalaFileName
object SecretKeys {
  case class Ed25519(bytes: ByteVector) extends SecretKey
}
