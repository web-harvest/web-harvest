package org.webharvest.plugin.ftp;

import org.apache.commons.net.ftp.FTPClient;
import org.testng.annotations.Test;

import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;

public class FtpClientFactoryTest {

    @Test
    public void factoryRejectsLegacyPasvAddressTrustOverride() {
        String property = FTPClient.FTP_IP_ADDRESS_FROM_PASV_RESPONSE;
        String previous = System.getProperty(property);
        try {
            System.setProperty(property, "true");

            FTPClient legacyClient = new FTPClient();
            assertTrue(legacyClient.isIpAddressFromPasvResponse(),
                    "Test must prove the legacy system property enables the unsafe behavior");

            FTPClient safeClient = FtpClientFactory.create();
            assertFalse(safeClient.isIpAddressFromPasvResponse(),
                    "WebHarvest FTP clients must ignore the server-advertised PASV address");
        } finally {
            if (previous == null) {
                System.clearProperty(property);
            } else {
                System.setProperty(property, previous);
            }
        }
    }
}
