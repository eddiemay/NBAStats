package com.digitald4.nbastats.server;

import com.google.api.server.spi.auth.EspAuthenticator;
import com.google.api.server.spi.auth.common.User;
import com.google.api.server.spi.config.AnnotationBoolean;
import com.google.api.server.spi.config.Api;
import com.google.api.server.spi.config.ApiIssuer;
import com.google.api.server.spi.config.ApiIssuerAudience;
import com.google.api.server.spi.config.ApiMethod;
import com.google.api.server.spi.config.ApiNamespace;
import com.google.api.server.spi.config.Named;
import com.google.api.server.spi.config.Nullable;
import com.google.api.server.spi.response.UnauthorizedException;

/** The Echo API which Endpoints will be exposing. */
@Api(
		name = "echo",
		version = "v1",
		namespace =
		@ApiNamespace(
				ownerDomain = "nbastats.digitald4.com",
				ownerName = "nbastats.digitald4.com",
				packagePath = ""
		),
		issuers = {
				@ApiIssuer(
						name = "firebase",
						issuer = "https://securetoken.google.com/fantasy-predictor",
						jwksUri = "https://www.googleapis.com/robot/v1/metadata/x509/securetoken@system.gserviceaccount.com")
		}
)

public class Echo {
	/**
	 * Echoes the received message back. If n is a non-negative integer, the message is copied that
	 * many times in the returned message.
	 *
	 * Note that name is specified and will override the default name of "{class name}.{method
	 * name}". For example, the default is "echo.echo".
	 *
	 * Note that httpMethod is not specified. This will default to a reasonable HTTP method
	 * depending on the API method name. In this case, the HTTP method will default to POST.
	 */
	@ApiMethod(name = "echo")
	public Message echo(Message message, @Named("n") @Nullable Integer n) {
		return doEcho(message, n);
	}
	// [END echo_method]

	/**
	 * Echoes the received message back. If n is a non-negative integer, the message is copied that
	 * many times in the returned message.
	 *
	 * Note that name is specified and will override the default name of "{class name}.{method
	 * name}". For example, the default is "echo.echo".
	 *
	 * Note that httpMethod is not specified. This will default to a reasonable HTTP method
	 * depending on the API method name. In this case, the HTTP method will default to POST.
	 */
	@ApiMethod(name = "echo_path_parameter", path = "echo/{n}")
	public Message echoPathParameter(Message message, @Named("n") int n) {
		return doEcho(message, n);
	}

	/**
	 * Echoes the received message back. If n is a non-negative integer, the message is copied that
	 * many times in the returned message.
	 *
	 * Note that name is specified and will override the default name of "{class name}.{method
	 * name}". For example, the default is "echo.echo".
	 *
	 * Note that httpMethod is not specified. This will default to a reasonable HTTP method
	 * depending on the API method name. In this case, the HTTP method will default to POST.
	 */
	@ApiMethod(name = "echo_api_key", path = "echo_api_key", apiKeyRequired = AnnotationBoolean.TRUE)
	public Message echoApiKey(Message message, @Named("n") @Nullable Integer n) {
		return doEcho(message, n);
	}

	private Message doEcho(Message message, Integer n) {
		if (n != null && n >= 0) {
			StringBuilder sb = new StringBuilder();
			for (int i = 0; i < n; i++) {
				if (i > 0) {
					sb.append(" ");
				}
				sb.append(message.getMessage());
			}
			message.setMessage(sb.toString());
		}
		return message;
	}

	/**
	 * Gets the authenticated user's email. If the user is not authenticated, this will return an HTTP
	 * 401.
	 *
	 * Note that name is not specified. This will default to "{class name}.{method name}". For
	 * example, the default is "echo.getUserEmail".
	 *
	 * Note that httpMethod is not required here. Without httpMethod, this will default to GET due
	 * to the API method name. httpMethod is added here for example purposes.
	 */
	@ApiMethod(
			httpMethod = ApiMethod.HttpMethod.GET,
			authenticators = {EspAuthenticator.class},
			audiences = {"YOUR_OAUTH_CLIENT_ID"},
			clientIds = {"YOUR_OAUTH_CLIENT_ID"}
	)
	public Email getUserEmail(User user) throws UnauthorizedException {
		if (user == null) {
			throw new UnauthorizedException("Invalid credentials");
		}

		Email response = new Email();
		response.setEmail(user.getEmail());
		return response;
	}

	/**
	 * Gets the authenticated user's email. If the user is not authenticated, this will return an HTTP
	 * 401.
	 *
	 * Note that name is not specified. This will default to "{class name}.{method name}". For
	 * example, the default is "echo.getUserEmail".
	 *
	 * Note that httpMethod is not required here. Without httpMethod, this will default to GET due
	 * to the API method name. httpMethod is added here for example purposes.
	 */
	@ApiMethod(
			path = "firebase_user",
			httpMethod = ApiMethod.HttpMethod.GET,
			authenticators = {EspAuthenticator.class},
			issuerAudiences = {@ApiIssuerAudience(name = "firebase", audiences = {"YOUR-PROJECT-ID"})}
	)
	public Email getUserEmailFirebase(User user) throws UnauthorizedException {
		if (user == null) {
			throw new UnauthorizedException("Invalid credentials");
		}

		Email response = new Email();
		response.setEmail(user.getEmail());
		return response;
	}

	private class Message {
		private String message;

		public String getMessage() {
			return message;
		}

		public void setMessage(String message) {
			this.message = message;
		}
	}

	private class Email {
		private String email;

		public String getEmail() {
			return email;
		}

		public void setEmail(String email) {
			this.email = email;
		}
	}
}
