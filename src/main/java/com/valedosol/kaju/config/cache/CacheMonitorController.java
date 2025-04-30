package com.valedosol.kaju.config.cache;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.CacheManager;
import org.springframework.cache.caffeine.CaffeineCache;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.github.benmanes.caffeine.cache.stats.CacheStats;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/admin/cache")
@Tag(name = "Cache", description = "Endpoints para monitoramento de cache")
public class CacheMonitorController {

    private final CacheManager cacheManager;
    
    @Autowired
    public CacheMonitorController(CacheManager cacheManager) {
        this.cacheManager = cacheManager;
    }
    
    @GetMapping("/stats")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Estatísticas de todos os caches", description = "Retorna estatísticas para todos os caches configurados")
    public ResponseEntity<Map<String, Object>> getAllCacheStats() {
        Map<String, Object> response = new HashMap<>();
        
        cacheManager.getCacheNames().forEach(cacheName -> {
            CaffeineCache caffeineCache = (CaffeineCache) cacheManager.getCache(cacheName);
            if (caffeineCache != null) {
                com.github.benmanes.caffeine.cache.Cache<Object, Object> nativeCache = 
                    caffeineCache.getNativeCache();
                CacheStats stats = nativeCache.stats();
                
                Map<String, Object> cacheStats = new HashMap<>();
                cacheStats.put("hitCount", stats.hitCount());
                cacheStats.put("missCount", stats.missCount());
                cacheStats.put("hitRate", String.format("%.2f%%", stats.hitRate() * 100));
                cacheStats.put("evictionCount", stats.evictionCount());
                cacheStats.put("estimatedSize", nativeCache.estimatedSize());
                
                response.put(cacheName, cacheStats);
            }
        });
        
        return ResponseEntity.ok(response);
    }
    
    @GetMapping("/stats/{cacheName}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Estatísticas de um cache específico", description = "Retorna estatísticas para um cache específico")
    public ResponseEntity<Map<String, Object>> getCacheStats(@PathVariable String cacheName) {
        CaffeineCache caffeineCache = (CaffeineCache) cacheManager.getCache(cacheName);
        if (caffeineCache == null) {
            return ResponseEntity.notFound().build();
        }
        
        com.github.benmanes.caffeine.cache.Cache<Object, Object> nativeCache = 
            caffeineCache.getNativeCache();
        CacheStats stats = nativeCache.stats();
        
        Map<String, Object> response = new HashMap<>();
        response.put("hitCount", stats.hitCount());
        response.put("missCount", stats.missCount());
        response.put("hitRate", String.format("%.2f%%", stats.hitRate() * 100));
        response.put("evictionCount", stats.evictionCount());
        response.put("averageLoadPenalty", stats.averageLoadPenalty());
        response.put("estimatedSize", nativeCache.estimatedSize());
        
        return ResponseEntity.ok(response);
    }
    
    @GetMapping("/clear/{cacheName}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Limpar um cache específico", description = "Limpa todas as entradas de um cache específico")
    public ResponseEntity<String> clearCache(@PathVariable String cacheName) {
        CaffeineCache caffeineCache = (CaffeineCache) cacheManager.getCache(cacheName);
        if (caffeineCache == null) {
            return ResponseEntity.notFound().build();
        }
        
        caffeineCache.clear();
        return ResponseEntity.ok("Cache '" + cacheName + "' foi limpo com sucesso");
    }
}