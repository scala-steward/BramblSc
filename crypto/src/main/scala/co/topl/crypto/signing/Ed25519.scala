package co.topl.crypto.signing

import co.topl.models.utility.HasLength.instances.bytesLength
import co.topl.models.utility.{Lengths, Sized}
import co.topl.models.{Bytes, Proofs, SecretKeys, VerificationKeys}

class Ed25519
    extends EllipticCurveSignatureScheme[
      SecretKeys.Ed25519,
      VerificationKeys.Ed25519,
      Proofs.Knowledge.Ed25519,
      SecretKeys.Ed25519.Length
    ] {
  private val impl = new eddsa.Ed25519
  impl.precompute()

  override val SignatureLength: Int = impl.SIGNATURE_SIZE
  override val KeyLength: Int = impl.SECRET_KEY_SIZE

  override def deriveKeyPairFromSeed(
    seed: Sized.Strict[Bytes, SecretKeys.Ed25519.Length]
  ): (SecretKeys.Ed25519, VerificationKeys.Ed25519) = {
    val secretKey = SecretKeys.Ed25519(Sized.strictUnsafe(Bytes(seed.data.toArray))(bytesLength, Lengths.`32`))
    val verificationKey = getVerificationKey(secretKey)
    secretKey -> verificationKey
  }

  override def sign(privateKey: SecretKeys.Ed25519, message: Bytes): Proofs.Knowledge.Ed25519 = {
    val sig = new Array[Byte](impl.SIGNATURE_SIZE)
    impl.sign(
      privateKey.bytes.data.toArray,
      0,
      message.toArray,
      0,
      message.toArray.length,
      sig,
      0
    )

    Proofs.Knowledge.Ed25519(Sized.strictUnsafe(Bytes(sig))(bytesLength, Lengths.`64`))
  }

  override def verify(
    signature: Proofs.Knowledge.Ed25519,
    message:   Bytes,
    publicKey: VerificationKeys.Ed25519
  ): Boolean = {
    val sigByteArray = signature.bytes.data.toArray
    val vkByteArray = publicKey.bytes.data.toArray
    val msgByteArray = message.toArray

    sigByteArray.length == impl.SIGNATURE_SIZE &&
    vkByteArray.length == impl.PUBLIC_KEY_SIZE &&
    impl.verify(
      sigByteArray,
      0,
      vkByteArray,
      0,
      msgByteArray,
      0,
      msgByteArray.length
    )
  }

  override def getVerificationKey(secretKey: SecretKeys.Ed25519): VerificationKeys.Ed25519 = {
    val pkBytes = new Array[Byte](impl.PUBLIC_KEY_SIZE)
    impl.generatePublicKey(secretKey.bytes.data.toArray, 0, pkBytes, 0)
    VerificationKeys.Ed25519(Sized.strictUnsafe(Bytes(pkBytes))(bytesLength, Lengths.`32`))
  }
}

object Ed25519 {
  val instance = new Ed25519
}