package com.cloud.framework.starter.id.cosid.adapter;

import com.cloud.framework.id.LongIdGenerator;
import lombok.RequiredArgsConstructor;
import me.ahoo.cosid.provider.IdGeneratorProvider;

@RequiredArgsConstructor
public class CosIdLongIdGenerator implements LongIdGenerator {
    private final IdGeneratorProvider idGeneratorProvider;

    @Override
    public Long nextId(String name) {
        return idGeneratorProvider.getRequired(name).generate();
    }
}
