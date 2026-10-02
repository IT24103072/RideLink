package fare_payment_service.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    @Value("${app.rabbitmq.ride-completed-exchange}")
    private String exchangeName;

    @Value("${app.rabbitmq.ride-completed-routing-key}")
    private String routingKey;

    @Value("${app.rabbitmq.ride-completed-queue}")
    private String queueName;

    @Bean
    public TopicExchange rideEventsExchange() {
        return new TopicExchange(exchangeName);
    }

    @Bean
    public Queue rideCompletedQueue() {
        return new Queue(queueName, true); // durable
    }

    @Bean
    public Binding rideCompletedBinding(Queue rideCompletedQueue, TopicExchange rideEventsExchange) {
        return BindingBuilder.bind(rideCompletedQueue).to(rideEventsExchange).with(routingKey);
    }

    @Bean
    public MessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }
}
