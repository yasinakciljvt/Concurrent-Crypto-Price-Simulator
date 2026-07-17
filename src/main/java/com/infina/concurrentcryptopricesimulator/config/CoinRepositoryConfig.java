package com.infina.concurrentcryptopricesimulator.config;

import com.infina.concurrentcryptopricesimulator.repository.DefaultCoinRepositories;
import com.infina.concurrentcryptopricesimulator.repository.InMemoryCoinRepository;
import com.infina.concurrentcryptopricesimulator.state.SafeCoinState;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class CoinRepositoryConfig {
	@Bean
	public InMemoryCoinRepository <SafeCoinState> safeCoinRepository(){
		return DefaultCoinRepositories.createSafe();
	}
}
