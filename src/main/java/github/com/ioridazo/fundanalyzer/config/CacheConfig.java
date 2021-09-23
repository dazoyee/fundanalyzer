package github.com.ioridazo.fundanalyzer.config;

import com.github.benmanes.caffeine.cache.Cache;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.binder.cache.CaffeineCacheMetrics;
import org.jetbrains.annotations.NotNull;
import org.springframework.boot.autoconfigure.cache.CacheProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.Collections;
import java.util.List;

@EnableConfigurationProperties(CacheProperties.class)
@EnableCaching
@Configuration
public class CacheConfig {
// https://qiita.com/koya3jp/items/d632e95ebc57ee07695c

    // CaffeineCacheConfigurationの実装をcopy
    @Bean
    public CaffeineCacheManager cacheManager(
            final MeterRegistry meterRegistry, final CacheProperties cacheProperties) {
        final CaffeineCacheManager cacheManager = createCacheManager(meterRegistry, cacheProperties);
        final List<String> cacheNames = cacheProperties.getCacheNames();

        if (!CollectionUtils.isEmpty(cacheNames)) {
            cacheManager.setCacheNames(cacheNames);
        }

        return cacheManager;
    }

    // ここで独自CaffeineCacheManagerを生成
    private CaffeineCacheManager createCacheManager(
            final MeterRegistry meterRegistry, final CacheProperties cacheProperties) {
        final CaffeineCacheManager cacheManager = new InstrumentedCaffeineCacheManager(meterRegistry);
        setCacheBuilder(cacheManager, cacheProperties);
        return cacheManager;
    }

    private void setCacheBuilder(final CaffeineCacheManager cacheManager, final CacheProperties cacheProperties) {
        final String specification = cacheProperties.getCaffeine().getSpec();

        if (StringUtils.hasText(specification)) {
            cacheManager.setCacheSpecification(specification);
        }
    }

    public static class InstrumentedCaffeineCacheManager extends CaffeineCacheManager {

        private final MeterRegistry meterRegistry;

        public InstrumentedCaffeineCacheManager(final MeterRegistry meterRegistry) {
            this.meterRegistry = meterRegistry;
        }

        @NotNull
        @Override
        protected Cache<Object, Object> createNativeCaffeineCache(@NotNull final String name) {
            final Cache<Object, Object> nativeCache = super.createNativeCaffeineCache(name);
            CaffeineCacheMetrics.monitor(meterRegistry, nativeCache, name, Collections.emptyList());
            return nativeCache;
        }
    }
}
