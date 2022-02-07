package y.w.twowayssl;

import static java.util.Objects.nonNull;
import static org.springframework.web.reactive.function.server.RequestPredicates.accept;
import static org.springframework.web.reactive.function.server.RouterFunctions.route;

import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.util.Optional;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLException;
import javax.net.ssl.TrustManagerFactory;
import lombok.RequiredArgsConstructor;
import nl.altindag.ssl.SSLFactory;
import nl.altindag.ssl.util.NettySslUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;
import org.springframework.http.MediaType;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.stereotype.Component;
import org.springframework.util.ResourceUtils;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;
import reactor.netty.http.client.HttpClient;
import reactor.netty.tcp.SslProvider.SslContextSpec;

@RequiredArgsConstructor
@SpringBootApplication
public class TwoWaySslApplicationClient {

    private final ServiceHandler serviceHandler;

    @Bean
    RouterFunction<ServerResponse> routes() {
        return route()
            .GET("callApi", accept(MediaType.APPLICATION_JSON), serviceHandler::callHelloAppi)
            .build();
    }

    public static void main(String[] args) {
        SpringApplication.run(TwoWaySslApplicationClient.class, args);
    }

}

@RequiredArgsConstructor
@Configuration
class WebClientConfiguration {

    private final HttpClient httpClient;

    @Bean
    WebClient webClient() {
        return WebClient
            .builder()
            .baseUrl("https://localhost:8443/hello")
            .clientConnector(new ReactorClientHttpConnector(httpClient))
            .build();
    }

}

@RequiredArgsConstructor
@Component
class ServiceHandler {

    private final WebClient webClient;

    public Mono<ServerResponse> callHelloAppi(ServerRequest request) {
        Mono<String> stringMono = webClient
            .get()
            .retrieve()
            .bodyToMono(String.class);

        return ServerResponse
            .ok()
            .body(BodyInserters.fromPublisher(stringMono, String.class));
    }
}

@Component
class HttpClientConfiguration {

    @Value("${client.ssl.one-way-authentication-enabled:false}")
    boolean oneWayAuthenticationEnabled;
    @Value("${client.ssl.two-way-authentication-enabled:false}")
    boolean twoWayAuthenticationEnabled;
    @Value("${client.ssl.key-store:}")
    String keyStorePath;
    @Value("${client.ssl.key-store-password:}")
    char[] keyStorePassword;
    @Value("${client.ssl.trust-store:}")
    String trustStorePath;
    @Value("${client.ssl.trust-store-password:}")
    char[] trustStorePassword;

    private SSLFactory sslFactory() {
        {
            SSLFactory sslFactory = null;

            if (oneWayAuthenticationEnabled) {
                sslFactory = SSLFactory.builder()
                    .withTrustMaterial(trustStorePath, trustStorePassword)
                    .withProtocols("TLSv1.3")
                    .build();
            }

            if (twoWayAuthenticationEnabled) {
                sslFactory = SSLFactory.builder()
                    .withIdentityMaterial(keyStorePath, keyStorePassword)
                    .withTrustMaterial(trustStorePath, trustStorePassword)
                    .withProtocols("TLSv1.3")
                    .build();
            }

            return sslFactory;
        }
    }

    /**
     * One two to create Netty Http Client
     *
     * @return
     * @throws SSLException
     */
    @Bean(value = "httpClient1")
    @Scope("prototype")
    public HttpClient nettyHttpClient() throws SSLException {
        SSLFactory sslFactory = sslFactory();

        reactor.netty.http.client.HttpClient httpClient = HttpClient.create();
        if (nonNull(sslFactory)) {
            SslContext sslContext = NettySslUtils.forClient(sslFactory).build();
            httpClient = httpClient.secure(sslSpec -> sslSpec.sslContext(sslContext));
        }
        return httpClient;
    }

    /**
     * Another way to create Netty Http Client
     *
     * @return
     */
    @Bean(value = "httpClient2")
    public HttpClient httpClient() {
        return HttpClient
            .create()
            .secure(spec -> {
                try {
                    KeyStore keyStore = KeyStore.getInstance("JKS");
                    keyStore.load(new FileInputStream(ResourceUtils.getFile(keyStorePath)),
                        keyStorePassword);

                    // Key Manager Factory to use this key store.
                    KeyManagerFactory keyManagerFactory = KeyManagerFactory.getInstance(
                        KeyManagerFactory.getDefaultAlgorithm());
                    keyManagerFactory.init(keyStore, keyStorePassword);

                    KeyStore trustStore = KeyStore.getInstance("JKS");
                    trustStore.load(new FileInputStream(ResourceUtils.getFile(trustStorePath)),
                        trustStorePassword);

                    // Trust Manager Factory to use the trust store
                    TrustManagerFactory trustManagerFactory = TrustManagerFactory.getInstance(
                        TrustManagerFactory.getDefaultAlgorithm());
                    trustManagerFactory.init(trustStore);

                    spec.sslContext(
                        SslContextBuilder
                            .forClient()
                            .keyManager(keyManagerFactory)
                            .trustManager(trustManagerFactory)
                            .build()
                    );
                } catch (KeyStoreException | FileNotFoundException e) {
                    e.printStackTrace();
                } catch (CertificateException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                } catch (NoSuchAlgorithmException e) {
                    e.printStackTrace();
                } catch (UnrecoverableKeyException e) {
                    e.printStackTrace();
                }
            });
    }

}

