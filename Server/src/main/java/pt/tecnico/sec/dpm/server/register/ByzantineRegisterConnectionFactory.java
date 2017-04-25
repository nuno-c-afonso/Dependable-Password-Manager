package pt.tecnico.sec.dpm.server.register;

import java.security.KeyStore;

public interface ByzantineRegisterConnectionFactory {
	ByzantineRegisterConnection createConnection(KeyStore keystore, String Url);
}
