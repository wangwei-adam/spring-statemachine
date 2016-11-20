/*
 * Copyright 2016 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.statemachine.buildtests.tck.redis;

import org.junit.Rule;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.keyvalue.core.KeyValueTemplate;
import org.springframework.data.redis.repository.configuration.EnableRedisRepositories;
import org.springframework.statemachine.StateMachine;
import org.springframework.statemachine.buildtests.tck.AbstractTckTests;
import org.springframework.statemachine.config.EnableStateMachineFactory;
import org.springframework.statemachine.config.StateMachineConfigurerAdapter;
import org.springframework.statemachine.config.builders.StateMachineModelConfigurer;
import org.springframework.statemachine.config.model.StateMachineModelFactory;
import org.springframework.statemachine.data.RepositoryState;
import org.springframework.statemachine.data.RepositoryStateMachineModelFactory;
import org.springframework.statemachine.data.RepositoryTransition;
import org.springframework.statemachine.data.StateRepository;
import org.springframework.statemachine.data.TransitionRepository;
import org.springframework.statemachine.data.redis.RedisRepositoryAction;
import org.springframework.statemachine.data.redis.RedisRepositoryGuard;
import org.springframework.statemachine.data.redis.RedisRepositoryState;
import org.springframework.statemachine.data.redis.RedisRepositoryTransition;
import org.springframework.statemachine.data.redis.RedisStateRepository;
import org.springframework.statemachine.data.redis.RedisTransitionRepository;

/**
 * Tck tests for machine configs build manually agains repository interfaces.
 *
 * @author Janne Valkealahti
 *
 */
public class RedisManualTckTests extends AbstractTckTests {

	@Rule
	public RedisRule redisAvailableRule = new RedisRule();

	@Override
	protected void cleanInternal() {
		AnnotationConfigApplicationContext c = new AnnotationConfigApplicationContext();
		c.register(TestConfig.class);
		c.refresh();
		KeyValueTemplate kvTemplate = c.getBean(KeyValueTemplate.class);
		kvTemplate.delete(RedisRepositoryAction.class);
		kvTemplate.delete(RedisRepositoryGuard.class);
		kvTemplate.delete(RedisRepositoryState.class);
		kvTemplate.delete(RedisRepositoryTransition.class);
		c.close();
	}

	@Override
	protected AnnotationConfigApplicationContext buildContext() {
		return new AnnotationConfigApplicationContext();
	}

	@Override
	protected StateMachine<String, String> getSimpleMachine() {
		context.register(TestConfig.class, StateMachineFactoryConfig.class);
		context.refresh();

		RedisStateRepository stateRepository = context.getBean(RedisStateRepository.class);
		RedisTransitionRepository transitionRepository = context.getBean(RedisTransitionRepository.class);

		RedisRepositoryState stateS1 = new RedisRepositoryState("S1", true);
		RedisRepositoryState stateS2 = new RedisRepositoryState("S2");
		RedisRepositoryState stateS3 = new RedisRepositoryState("S3");

		stateRepository.save(stateS1);
		stateRepository.save(stateS2);
		stateRepository.save(stateS3);


		RedisRepositoryTransition transitionS1ToS2 = new RedisRepositoryTransition(stateS1, stateS2, "E1");
		RedisRepositoryTransition transitionS2ToS3 = new RedisRepositoryTransition(stateS2, stateS3, "E2");

		transitionRepository.save(transitionS1ToS2);
		transitionRepository.save(transitionS2ToS3);

		return getStateMachineFactoryFromContext().getStateMachine();
	}

	@Override
	protected StateMachine<String, String> getSimpleSubMachine() throws Exception {
		context.register(TestConfig.class, StateMachineFactoryConfig.class);
		context.refresh();

		RedisStateRepository stateRepository = context.getBean(RedisStateRepository.class);
		RedisTransitionRepository transitionRepository = context.getBean(RedisTransitionRepository.class);

		RedisRepositoryState stateS1 = new RedisRepositoryState("S1", true);
		RedisRepositoryState stateS2 = new RedisRepositoryState("S2");
		RedisRepositoryState stateS3 = new RedisRepositoryState("S3");

		RedisRepositoryState stateS21 = new RedisRepositoryState("S21", true);
		stateS21.setParentState(stateS2);
		RedisRepositoryState stateS22 = new RedisRepositoryState("S22");
		stateS22.setParentState(stateS2);

		stateRepository.save(stateS1);
		stateRepository.save(stateS2);
		stateRepository.save(stateS3);
		stateRepository.save(stateS21);
		stateRepository.save(stateS22);

		RedisRepositoryTransition transitionS1ToS2 = new RedisRepositoryTransition(stateS1, stateS2, "E1");
		RedisRepositoryTransition transitionS2ToS3 = new RedisRepositoryTransition(stateS21, stateS22, "E2");
		RedisRepositoryTransition transitionS21ToS22 = new RedisRepositoryTransition(stateS2, stateS3, "E3");

		transitionRepository.save(transitionS1ToS2);
		transitionRepository.save(transitionS2ToS3);
		transitionRepository.save(transitionS21ToS22);

		return getStateMachineFactoryFromContext().getStateMachine();
	}

	@Configuration
	@EnableStateMachineFactory
	public static class StateMachineFactoryConfig extends StateMachineConfigurerAdapter<String, String> {

		@Autowired
		private StateRepository<? extends RepositoryState> stateRepository;

		@Autowired
		private TransitionRepository<? extends RepositoryTransition> transitionRepository;

		@Override
		public void configure(StateMachineModelConfigurer<String, String> model) throws Exception {
			model
				.withModel()
					.factory(modelFactory());
		}

		@Bean
		public StateMachineModelFactory<String, String> modelFactory() {
			return new RepositoryStateMachineModelFactory(stateRepository, transitionRepository);
		}
	}

	@EnableAutoConfiguration
	@EntityScan(basePackages = {"org.springframework.statemachine.data.redis"})
	@EnableRedisRepositories(basePackages = {"org.springframework.statemachine.data.redis"})
	static class TestConfig {
	}
}
