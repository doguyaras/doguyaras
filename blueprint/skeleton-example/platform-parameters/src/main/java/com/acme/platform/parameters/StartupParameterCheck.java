package com.acme.platform.parameters;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Acilis kontrolu (referans Bolum 14 "kayma tespiti"): servisin kullandigi her key katalogda var ve tipine uygun mu?
 * Tanimsiz key bir DEPLOY hatasidir; uygulama ayaga kalkmaz (fail fast). Grup okumasi group() ile yapilir:
 * kaynak kapaliysa disk snapshot'i yeterlidir, boylece control plane restart'i data plane acilisini engellemez.
 */
public final class StartupParameterCheck {
    private final SystemParameterProvider provider;

    public StartupParameterCheck(SystemParameterProvider provider) { this.provider = provider; }

    public void verify(List<RequiredParameter> required) {
        Map<String, List<RequiredParameter>> byGroup = new LinkedHashMap<>();
        for (RequiredParameter r : required) byGroup.computeIfAbsent(r.group(), g -> new ArrayList<>()).add(r);
        List<String> missing = new ArrayList<>();
        for (Map.Entry<String, List<RequiredParameter>> e : byGroup.entrySet()) {
            ParameterValues values = provider.group(e.getKey());
            for (RequiredParameter r : e.getValue()) {
                if (!values.has(r.key())) { missing.add(r.group() + "/" + r.key()); continue; }
                values.validate(r.key(), r.type());        // bozuk deger PARAMETER_VALUE_INVALID ile burada patlar
            }
        }
        if (!missing.isEmpty()) throw new ParameterNotDefinedException(missing);
    }
}
