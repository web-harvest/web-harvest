package org.webharvest.plugin.ftp;

import org.apache.commons.net.ftp.FTPClient;
import org.testng.annotations.Test;

import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;
import static org.testng.Assert.assertEquals;

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
    @Test
    public void factoryConfiguresFiniteTransportTimeouts() {
        FTPClient client = FtpClientFactory.create();

        assertEquals(client.getConnectTimeout(), FtpClientFactory.CONNECT_TIMEOUT_MILLIS);
        assertEquals(client.getDefaultTimeout(), FtpClientFactory.CONTROL_TIMEOUT_MILLIS);
        assertEquals(client.getDataTimeout().toMillis(), (long) FtpClientFactory.DATA_TIMEOUT_MILLIS);
    }

}
