package com.sanoli.financedash.radar.ai;

import org.springframework.stereotype.Component;
import org.springframework.web.context.annotation.RequestScope;

@Component
@RequestScope
public class RadarToolInvocationContext {

    private String usedFunction;
    private Object data;

    public void record(String usedFunction, Object data) {
        this.usedFunction = usedFunction;
        this.data = data;
    }

    public void clear() {
        this.usedFunction = null;
        this.data = null;
    }

    public String getUsedFunction() {
        return usedFunction;
    }

    public Object getData() {
        return data;
    }
}
