/*
 * Kontalk Android client
 * Copyright (C) 2020 Kontalk Devteam <devteam@kontalk.org>

 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.

 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.

 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.kontalk.crypto;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Date;

import org.junit.Test;
import org.bouncycastle.openpgp.PGPSecretKeyRing;
import org.bouncycastle.openpgp.operator.PBESecretKeyDecryptor;
import org.bouncycastle.openpgp.operator.PGPDigestCalculatorProvider;
import org.bouncycastle.openpgp.operator.jcajce.JcaPGPDigestCalculatorProviderBuilder;
import org.bouncycastle.openpgp.operator.jcajce.JcePBESecretKeyDecryptorBuilder;

import org.kontalk.util.ByteArrayInOutStream;
import org.kontalk.util.MessageUtils;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;


public class PGPTest {

    private static final String TEST_PRIVATE_FILENAME = "/sample_private.key";
    private static final String TEST_PRIVATE_FINGERPRINT = "9DEFD2DEFEE26B413F04A1F9BF68B5FB16CFC215";

    @Test
    public void testFormatFingerprint() throws Exception {
        String fpr = PGP.formatFingerprint("F28947756EB27311E86F6309274BE2A3BD56E37A");
        assertEquals("F289 4775 6EB2 7311 E86F  6309 274B E2A3 BD56 E37A", fpr);
    }

    @Test
    public void testBase64() throws Exception {
        PGP.registerProvider();
        final PGP.PGPDecryptedKeyPairRing createdKey = PGP.create(new Date());

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ObjectOutputStream dest = new ObjectOutputStream(out);
        PGP.serialize(createdKey, dest);

        byte[] buf = out.toByteArray();
        ObjectInputStream src = new ObjectInputStream(new ByteArrayInputStream(buf));
        final PGP.PGPDecryptedKeyPairRing serializedKey = PGP.unserialize(src);

        assertNotNull(serializedKey);

        assertEquals(serializedKey.authKey.getPrivateKey().getKeyID(),
            createdKey.authKey.getPrivateKey().getKeyID());
        assertArrayEquals(serializedKey.authKey.getPrivateKey().getPrivateKeyDataPacket().getEncoded(),
            createdKey.authKey.getPrivateKey().getPrivateKeyDataPacket().getEncoded());
        assertArrayEquals(serializedKey.authKey.getPrivateKey().getPublicKeyPacket().getEncoded(),
            createdKey.authKey.getPrivateKey().getPublicKeyPacket().getEncoded());
        assertArrayEquals(serializedKey.authKey.getPublicKey().getEncoded(),
            createdKey.authKey.getPublicKey().getEncoded());

        assertEquals(serializedKey.signKey.getPrivateKey().getKeyID(),
            createdKey.signKey.getPrivateKey().getKeyID());
        assertArrayEquals(serializedKey.signKey.getPrivateKey().getPrivateKeyDataPacket().getEncoded(),
            createdKey.signKey.getPrivateKey().getPrivateKeyDataPacket().getEncoded());
        assertArrayEquals(serializedKey.signKey.getPrivateKey().getPublicKeyPacket().getEncoded(),
            createdKey.signKey.getPrivateKey().getPublicKeyPacket().getEncoded());
        assertArrayEquals(serializedKey.signKey.getPublicKey().getEncoded(),
            createdKey.signKey.getPublicKey().getEncoded());

        assertEquals(serializedKey.encryptKey.getPrivateKey().getKeyID(),
            createdKey.encryptKey.getPrivateKey().getKeyID());
        assertArrayEquals(serializedKey.encryptKey.getPrivateKey().getPrivateKeyDataPacket().getEncoded(),
            createdKey.encryptKey.getPrivateKey().getPrivateKeyDataPacket().getEncoded());
        assertArrayEquals(serializedKey.encryptKey.getPrivateKey().getPublicKeyPacket().getEncoded(),
            createdKey.encryptKey.getPrivateKey().getPublicKeyPacket().getEncoded());
        assertArrayEquals(serializedKey.encryptKey.getPublicKey().getEncoded(),
            createdKey.encryptKey.getPublicKey().getEncoded());
    }

    @Test
    public void testChangePassphrase() throws Exception {
        PGP.registerProvider();
        String oldPassphrase = "oldtest";
        String newPassphrase = "newtest";

        InputStream in = getClass().getResourceAsStream(TEST_PRIVATE_FILENAME);
        final ByteArrayInOutStream privateKeyDataBuf = MessageUtils.readFully(in, 102400);
        byte[] privateKeyData = privateKeyDataBuf.toByteArray();
        byte[] newPrivateKeyData = PGP.changePassphrase(privateKeyData, oldPassphrase, newPassphrase);

        PGPDigestCalculatorProvider digestCalcProv = new JcaPGPDigestCalculatorProviderBuilder().build();
        PBESecretKeyDecryptor decryptor = new JcePBESecretKeyDecryptorBuilder(digestCalcProv)
            .setProvider(PGP.PROVIDER)
            .build(newPassphrase.toCharArray());

        PGPSecretKeyRing secRing = new PGPSecretKeyRing(newPrivateKeyData, PGP.sFingerprintCalculator);
        // an exception will be thrown if anything went wrong
        secRing.getSecretKey().extractPrivateKey(decryptor);
    }
}
