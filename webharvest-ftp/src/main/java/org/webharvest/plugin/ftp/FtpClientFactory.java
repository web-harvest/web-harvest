package org.webharvest.plugin.ftp;

import org.apache.commons.net.ftp.FTPClient;

/**
 * Creates FTP clients with the safe PASV response address policy.
 *
 * <p>Never trust an address advertised by an FTP server in its PASV response.
 * Commons Net 3.9.0+ defaults to this policy, but setting it explicitly also
 * prevents a legacy system property from re-enabling the unsafe behavior.</p>
 */
final class FtpClientFactory {

    private FtpClientFactory() {
    }

    static FTPClient create() {
        FTPClient client = new FTPClient();
        client.setIpAddressFromPasvResponse(false);
        return client;
    }
}
