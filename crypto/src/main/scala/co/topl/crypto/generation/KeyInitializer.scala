package co.topl.crypto.generation

import cats.implicits._
import co.topl.crypto.generation.mnemonic.{Entropy, EntropyFailure, Language}
import co.topl.crypto.signing._
import co.topl.models._
import scodec.bits.{BitVector, ByteVector}
import simulacrum.typeclass

import java.util.UUID

@typeclass trait KeyInitializer[SK] {
  self =>

  /**
   * Creates a random secret key
   */
  def random(): SK

  /**
   * Creates a secret key from the given seed
   */
  def fromEntropy(entropy: Entropy, password: Option[String] = None): SK

  /**
   * Creates an instance of a secret key given a byte vector
   *
   * @param bytes bytes of the secret key
   * @return
   */
  def fromBytes(bytes: ByteVector): SK

  /**
   * Create a secret key from a mnemonic string.
   *
   * @param mnemonicString The mnemonic string from which to create the key
   * @param language       The language of the mnemonic string
   * @param password       An optional password to use in creating the key.
   * @return The created secret key.
   */
  def fromMnemonicString(
                          mnemonicString: String
                        )(language: Language = Language.English, password: Option[String] = None): Either[InitializationFailure, SK] =
    Entropy
      .fromMnemonicString(mnemonicString, language)
      .map(fromEntropy(_, password))
      .leftMap(e => InitializationFailures.FailedToCreateEntropy(e))

  def fromBase58String(base58String: String): Either[InitializationFailure, SK] =
    Either.fromOption(BitVector.fromBase58(base58String), InitializationFailures.InvalidBase58String)
      .map(bits => fromBytes(bits.toByteVector))

  def fromBase16String(base16String: String): Either[InitializationFailure, SK] =
    Either.fromOption(BitVector.fromHex(base16String), InitializationFailures.InvalidBase16String)
      .map(bits => fromBytes(bits.toByteVector))
}

object KeyInitializer {

  trait Instances {

    implicit def ed25519Initializer(implicit ed25519: Ed25519): KeyInitializer[SecretKeys.Ed25519] =
      new KeyInitializer[SecretKeys.Ed25519] {

        override def random(): SecretKeys.Ed25519 =
          fromEntropy(Entropy.fromUuid(UUID.randomUUID()), password = Some(""))

        override def fromEntropy(entropy: Entropy, password: Option[String]): SecretKeys.Ed25519 =
          ed25519.deriveKeyPairFromEntropy(entropy, password)._1

        override def fromBytes(bytes: ByteVector): SecretKeys.Ed25519 = SecretKeys.Ed25519(bytes)
      }
  }

  object Instances extends Instances
}

sealed abstract class InitializationFailure

object InitializationFailures {
  case class KeyCreationError(throwable: Throwable) extends InitializationFailure
  case object InvalidBase58String extends InitializationFailure
  case object InvalidBase16String extends InitializationFailure
  case class FailedToCreateEntropy(entropyFailure: EntropyFailure) extends InitializationFailure
}
