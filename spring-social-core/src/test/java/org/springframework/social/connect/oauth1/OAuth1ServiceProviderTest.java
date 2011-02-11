package org.springframework.social.connect.oauth1;

import static org.junit.Assert.*;

import java.util.List;

import org.junit.Test;
import org.springframework.social.connect.ServiceProviderConnection;
import org.springframework.social.connect.support.ConnectionRepository;
import org.springframework.social.connect.test.StubConnectionRepository;
import org.springframework.social.oauth1.AuthorizedRequestToken;
import org.springframework.social.oauth1.OAuth10aOperations;
import org.springframework.social.oauth1.OAuthToken;

public class OAuth1ServiceProviderTest {

	private ConnectionRepository connectionRepository = new StubConnectionRepository();

	private OAuth10aServiceProvider<TestApi> serviceProvider = new TestServiceProvider("54321", "65432", connectionRepository);

	@Test
	public void connectFlow() {
		// preconditions
		Long accountId = 1L;
		assertEquals(false, serviceProvider.isConnected(accountId));
		assertEquals(0, serviceProvider.getConnections(accountId).size());
		
		// oauth 1 dance
		OAuth10aOperations oauthClient = serviceProvider.getOAuth10aOperations();
		OAuthToken requestToken = oauthClient.fetchNewRequestToken("http://localhost:8080/me");
		String authorizeUrl = oauthClient.buildAuthorizeUrl(requestToken.getValue());
		assertEquals("http://springsource.org/oauth/authorize?request_token=12345", authorizeUrl);
		OAuthToken accessToken = oauthClient.exchangeForAccessToken(new AuthorizedRequestToken(requestToken, "verifier"));

		// connect
		ServiceProviderConnection<TestApi> connection = serviceProvider.connect(accountId, accessToken);
		TestApi api = connection.getServiceApi();
		assertEquals("Hello Keith!", api.testOperation("Keith"));

		TestApiImpl impl = (TestApiImpl) api;
		assertEquals("54321", impl.getConsumerKey());
		assertEquals("65432", impl.getConsumerSecret());
		assertEquals("34567", impl.getAccessToken());
		assertEquals("45678", impl.getSecret());
		
		// additional postconditions
		assertEquals(true, serviceProvider.isConnected(accountId));
		List<ServiceProviderConnection<TestApi>> connections = serviceProvider.getConnections(accountId);
		assertEquals(1, connections.size());
		assertEquals("Hello Keith!", connections.get(0).getServiceApi().testOperation("Keith"));		

	}
	
	@Test
	public void equals() {
		Long accountId = 1L;
		OAuthToken accessToken = new OAuthToken("12345", "23456");
		ServiceProviderConnection<TestApi> connection = serviceProvider.connect(accountId, accessToken);
		ServiceProviderConnection<TestApi> sameConnection = serviceProvider.getConnections(1L).get(0);
		assertEquals(connection, sameConnection);		
	}
	
	@Test
	public void duplicateConnection() {
		Long accountId = 1L;
		OAuthToken accessToken = new OAuthToken("12345", "23456");
		serviceProvider.connect(accountId, accessToken);
		try {
			serviceProvider.connect(accountId, accessToken);
			fail("Should have failed on duplicate connection");
		} catch (IllegalArgumentException e) {
			
		}
	}
	
	@Test
	public void disconnect() {
		Long accountId = 1L;
		OAuthToken accessToken = new OAuthToken("12345", "23456");
		ServiceProviderConnection<TestApi> connection = serviceProvider.connect(accountId, accessToken);
		TestApi api = connection.getServiceApi();
		assertEquals("Hello Keith!", api.testOperation("Keith"));

		// disconnect
		connection.disconnect();
		assertEquals(false, serviceProvider.isConnected(accountId));
		assertEquals(0, serviceProvider.getConnections(accountId).size());

		try {
			connection.getServiceApi();
			fail("Should be disconnected");
		} catch (IllegalStateException e) {
			
		}
		
		try {
			connection.disconnect();
			fail("Should already be disconnected");
		} catch (IllegalStateException e) {
			
		}		
	}
	
	static class TestServiceProvider extends AbstractOAuth10aServiceProvider<TestApi> {

		public TestServiceProvider(String consumerKey, String consumerSecret, ConnectionRepository connectionRepository) {
			super("test", connectionRepository, consumerKey, consumerSecret, new StubOAuth1Operations());
		}

		protected TestApi getApi(String consumerKey, String consumerSecret, String accessToken, String secret) {
			return new TestApiImpl(consumerKey, consumerSecret, accessToken, secret);
		}

	}
	
	interface TestApi {
		String testOperation(String arg);
	}
	
	static class TestApiImpl implements TestApi {

		private String consumerKey;
		
		private String consumerSecret;
		
		private String accessToken;
		
		private String secret;
		
		public TestApiImpl(String consumerKey, String consumerSecret, String accessToken, String secret) {
			this.consumerKey = consumerKey;
			this.consumerSecret = consumerSecret;
			this.accessToken = accessToken;
			this.secret = secret;
		}

		public String getConsumerKey() {
			return consumerKey;
		}

		public String getConsumerSecret() {
			return consumerSecret;
		}

		public String getAccessToken() {
			return accessToken;
		}

		public String getSecret() {
			return secret;
		}

		public String testOperation(String arg) {
			return "Hello " + arg + "!";
		}
		
	}
}
