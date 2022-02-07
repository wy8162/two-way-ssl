package y.w.twowayssl;

import static org.springframework.web.reactive.function.server.RequestPredicates.accept;
import static org.springframework.web.reactive.function.server.RouterFunctions.route;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.ServerResponse;

@Slf4j
@SpringBootApplication
public class TwoWaySslApplicationServer {

	@Bean
	RouterFunction<ServerResponse> routes() {
		return route()
			.GET("/hello", accept(MediaType.APPLICATION_JSON), r -> {
				log.info("Service was called.");

				return ServerResponse.ok().body(
					BodyInserters.fromValue("Hello, World"));
			})
			.build();

	}

	public static void main(String[] args) {
		SpringApplication.run(TwoWaySslApplicationServer.class, args);
	}

}
